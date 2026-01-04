/*
 * Copyright 2025 William R. Price - All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.wrprice.inettime;

import static io.github.wrprice.inettime.InternetTimeField.*;
import static io.github.wrprice.inettime.InternetTimeUnit.*;
import static java.time.temporal.ChronoField.*;
import static java.util.Objects.requireNonNull;

import java.io.Serial;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.*;
import java.time.chrono.IsoChronology;
import java.time.format.*;
import java.time.temporal.*;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

/// Represents a global instant according to the *Swatch&reg; Internet Time* standard:
/// a date-time with a *fixed offset* from <abbr title="Coordinated Universal Time">UTC</abbr>
/// in the ISO calendar.  An example is: `2025-12-28 @123.45`.  As implied by the fixed offset,
/// <abbr title="Daylight Saving Time">DST</abbr> *does not apply*.
///
/// This implementation's time-of-day precision is limited to the granularity of a single
/// [centibeat][InternetTimeUnit#CENTIBEATS] measured from midnight, and the zone offset is not
/// stored because a constant, fixed UTC offset is implied.  In that way, this class is somewhat
/// of a hybrid between an [Instant] and an [OffsetDateTime], with similar functionality.
///
/// ## Factory methods
///
/// - `public static` methods starting with `of` create **exact** values in *Internet Time*.
/// - `public static` methods starting with `now` create values **at or before** the current time
///   given by the system default or a provided [Clock].
/// - [#from(TemporalAccessor)] attempts to derive an *Internet Time* value **equal to or before**
///   the provided value.
/// - `public static` methods starting with `parse` attempt to obtain an *Internet Time* value
///   **equal to or before** the date-time represented as text, given a corresponding formatter.
///
/// Attempting to "round-trip" an `InternetTime` value from an inexact method back to the original
/// type may not result in an equivalent value due to alignment and precision differences.
///
/// ## Convenience methods
///
/// ### Alignment
///
/// - [#toNearestSecond()] creates an `OffsetDateTime` at the nearest whole second boundary (where
///   all smaller units, such as millisecond-of-second, etc.) are zero(0).  The nearest second can
///   occur *before or after* the original `InternetTime` value on the time line.
/// - [#toStartOf(InternetTimeField, Temporal)] aligns a date-time value of another type to the
///   first millisecond-of-day of its equivalent *Internet Time* value, at the resolution of the
///   specified [InternetTimeField] (effectively, the current *.beat* or centibeat).
///
/// ### Conversion
///
/// Various `public` instance methods beginning with `to` and named after common `java.time` API
/// value types generate those value types from the value of the original `InternetTime` instance.
/// Converted values are aligned to the beginning millisecond-of-day of the original value's
/// centibeat-of-day.
///
/// - [#toInstant()]
/// - [#toLocalDate()]
/// - [#toLocalDateTime()]
/// - [#toOffsetDateTime()]
/// - [#toOffsetTime()]
///
/// The `static` methods [#timeOfBeat(int)] and [#timeOfBeat(int, int)] produce [OffsetTime]
/// values for various *.beat* values that can then be converted to hours, minutes, seconds, etc.,
/// in non-*Internet Time* zones with different offsets.  These values are useful for time display
/// purposes where the date context (if any) is handled separately.
///
/// ## Formatting & Parsing
///
/// Immutable, thread-safe formatters (which also support parsing operations) in a select number of
/// predefined formats are available as `static final` fields on this class.  To build your own
/// custom format involving *.beats*, see the [#beatFormatter] method.
///
/// -----
///
/// **Implementation notes:**  
/// This class is immutable and thread-safe.
///
/// This is a [value-based][vbc] class; programmers should treat instances that are
/// [equal][#equals(Object)] as interchangeable and should not use instances for synchronization,
/// or unpredictable behavior may occur. For example, in a future release, synchronization may fail.
/// The `equals` method should be used for equivalence comparisons.
///
///[vbc]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/doc-files/ValueBased.html
///
/// @author William R. Price
public final /*value*/ class InternetTime
    implements Temporal, Comparable<InternetTime>, Serializable {

  /// Fixed-offset zone based on the location of Swatch's headquarters in Biel, Switzerland.
  /// Can also be used as a [ZoneId].
  public static final ZoneOffset ZONE = ZoneOffset.ofHours(+1);

  /// Used to access array elements of type `DateTimeFormatter` with `volatile` semantics.
  /// @see #TIME_FORMATTERS_BY_STYLE
  private static final VarHandle DATETIMEFORMAT_ARRAY_VARHANDLE =
      MethodHandles.arrayElementVarHandle(DateTimeFormatter[].class);

  /// Holds reusable time-only formatters for each [FormatStyle#ordinal()].
  /// @see #beatFormatter(FormatStyle)
  private static final DateTimeFormatter[] TIME_FORMATTERS_BY_STYLE =
      new DateTimeFormatter[FormatStyle.values().length];

  /// ISO-style local date-time formatter and parser with Internet Time *.beats*
  /// but without any zone or offset: `2025-12-31 @567`
  ///
  /// The format consists of:
  ///
  /// - The [ISO_LOCAL_DATE][DateTimeFormatter#ISO_LOCAL_DATE]
  /// - A space (ASCII 0x20)
  /// - [beatFormatter(MEDIUM)][#beatFormatter(FormatStyle)], which is:
  ///     - The `@` symbol
  ///     - Three digits, zero-padded, for the number of whole *.beats* since Internet Time midnight
  public static final DateTimeFormatter LOCAL_DATE_BEATS =
      joinFormats(DateTimeFormatter.ISO_LOCAL_DATE, beatFormatter(FormatStyle.MEDIUM));

  /// ISO-style local date-time formatter and parser with Internet Time fractional *.beats*
  /// but without any zone or offset: `2025-12-31 @567.89`
  ///
  /// The format consists of:
  ///
  /// - The [ISO_LOCAL_DATE][DateTimeFormatter#ISO_LOCAL_DATE]
  /// - A space (ASCII 0x20)
  /// - [beatFormatter(FULL)][#beatFormatter(FormatStyle)], which is:
  ///     - The `@` symbol
  ///     - Three digits, zero-padded, for the number of whole *.beats* since Internet Time midnight
  ///     - The `.` symbol
  ///     - Two digits, zero-padded, for the fractional part of the current *.beat*.
  public static final DateTimeFormatter LOCAL_DATE_CENTIBEATS =
      joinFormats(DateTimeFormatter.ISO_LOCAL_DATE, beatFormatter(FormatStyle.FULL));

  /// ISO-style date-time formatter and parser with Internet Time *.beats*
  /// and an ISO-8601 extended offset date from UTC: `2025-12-31+01:00 @567`
  ///
  /// The format consists of:
  ///
  /// - The [ISO_OFFSET_DATE][DateTimeFormatter#ISO_OFFSET_DATE]
  /// - A space (ASCII 0x20)
  /// - [beatFormatter(MEDIUM)][#beatFormatter(FormatStyle)], which is:
  ///     - The `@` symbol
  ///     - Three digits, zero-padded, for the number of whole *.beats* since Internet Time midnight
  public static final DateTimeFormatter OFFSET_DATE_BEATS =
      joinFormats(DateTimeFormatter.ISO_OFFSET_DATE, beatFormatter(FormatStyle.MEDIUM));

  /// ISO-style date-time formatter and parser with Internet Time fractional *.beats*
  /// and an ISO-8601 extended offset date from UTC: `2025-12-31+01:00 @567.89`
  ///
  /// The format consists of:
  ///
  /// - The [ISO_OFFSET_DATE][DateTimeFormatter#ISO_OFFSET_DATE]
  /// - A space (ASCII 0x20)
  /// - [beatFormatter(FULL)][#beatFormatter(FormatStyle)], which is:
  ///     - The `@` symbol
  ///     - Three digits, zero-padded, for the number of whole *.beats* since Internet Time midnight
  ///     - The `.` symbol
  ///     - Two digits, zero-padded, for the fractional part of the current *.beat*.
  public static final DateTimeFormatter OFFSET_DATE_CENTIBEATS =
      joinFormats(DateTimeFormatter.ISO_OFFSET_DATE, beatFormatter(FormatStyle.FULL));

  /// Late 1990s format seemingly preferred by Swatch&reg;, though not officially documented
  /// as a standard: `d31.12.2025 @567.89`
  ///
  /// NOTE: Some historical examples used two(2)-digit years, which was short-sighted given that
  /// *Y2K* was right around the corner and on many technologists' minds.  This format is configured
  /// to always use four(4) digits for the year.  The date is local to *Internet Time* (UTC+1);
  /// no UTC offset is shown.
  private static final DateTimeFormatter RETRO_DATE_CENTIBEATS =
      new DateTimeFormatterBuilder()
          .optionalStart()
          .appendLiteral('d')
          .optionalEnd()
          .appendValue(DAY_OF_MONTH, 2) // FUTURE: INET_DAY_OF_MONTH?
          .appendLiteral('.')
          .appendValue(MONTH_OF_YEAR, 2) // FUTURE: INET_MONTH_OF_YEAR?
          .appendLiteral('.')
          .appendValue(YEAR, 4) // FUTURE: INET_YEAR?
          .appendLiteral(' ')
          .append(beatFormatter(FormatStyle.FULL))
          .toFormatter()
          .withZone(ZONE); // zone override will throw if values used have the incorrect zone

  @Serial
  private static final long serialVersionUID = 202512271724L;

  /// Date relative to the Internet Time's fixed zone.
  /// @serial
  private final LocalDate date;

  /// Local time in centibeats. Valid values are 0 to 99,999 (inclusive).
  /// @serial
  private final int centibeatOfDay;

  private InternetTime(LocalDateTime dateTimeInCorrectOffset) {
    this(
        dateTimeInCorrectOffset.toLocalDate(),
        (int) CENTIBEAT_OF_DAY.getFrom(dateTimeInCorrectOffset, ZONE.getTotalSeconds()));
  }

  private InternetTime(LocalDate date, int centibeats) {
    this.date = date;
    this.centibeatOfDay = centibeats;
  }

  // <editor-fold desc="Factory Methods" defaultstate="collapsed"> -------------------------------

  /// Obtains the current date-time from the system clock in *Internet Time*. The offset is always
  /// UTC+1, regardless of the system's default time-zone.
  ///
  /// Using this method will prevent the ability to use an alternate clock for testing because the
  /// clock is hard-coded.
  ///
  /// @return the current date and time from the system clock
  public static InternetTime now() {
    return now(Clock.systemUTC());
  }

  /// Obtains the current date-time from a provided clock. The offset is always UTC+1, computed
  /// from the offset of the time-zone in the clock.
  ///
  /// Using this method allows using an alternate clock for testing.
  ///
  /// @param clock the clock to use
  /// @return the current date and time according to the clock
  public static InternetTime now(Clock clock) {
    return ofInstant(requireNonNull(clock, "clock").instant());
  }

  /// Obtains an instance of `InternetTime` from a temporal object.  A `TemporalAccessor` represents
  /// an arbitrary set of date and time information.  This factory first attempts to translate
  /// `java.time` date-time types supporting time-zone offset information, falling back to an
  /// `Instant` if necessary.
  ///
  /// **Local** date-time values, date-only, and time-only values are not supported, because a
  /// representation in *Internet Time* requires determining the offset relative to UTC+1.
  ///
  /// Attempting to "round-trip" the returned [InternetTime] [back to][#toInstant()] an `Instant`
  /// will often result in a *different value* (+/- 864ms) because of the precision limitations of
  /// `InternetTime`.
  ///
  /// @param accessor the temporal object to convert
  /// @return a date-time aligned to *Internet Time*
  /// @throws DateTimeException if unable to convert to an `InternetTime`
  public static InternetTime from(TemporalAccessor accessor) {
    requireNonNull(accessor, "accessor");
    return switch (accessor) {
      case InternetTime it -> it;
      case OffsetDateTime dt -> new InternetTime(dt.withOffsetSameInstant(ZONE).toLocalDateTime());
      case ZonedDateTime dt -> new InternetTime(dt.withZoneSameInstant(ZONE).toLocalDateTime());
      case TemporalAccessor ta when
          DateTimeFormatter.class.getPackage().equals(ta.getClass().getPackage()) -> fromParsed(ta);
      default -> {
        try {
          yield ofInstant(Instant.from(accessor));
        } catch (DateTimeException dte) {
          throw cannotDeriveFrom(accessor);
        }
      }
    };
  }

  private static DateTimeException cannotDeriveFrom(TemporalAccessor accessor) {
    var typeName = accessor.getClass().getName();
    return new DateTimeException("Cannot derive InternetTime from " + typeName + ": " + accessor);
  }

  /// Obtains an instance of `InternetTime` from an `Instant`.
  ///
  /// Attempting to "round-trip" the returned [InternetTime] [back to][#toInstant()] an `Instant`
  /// will often result in a *different value* (+/- 864ms) because of the precision limitations of
  /// `InternetTime`.
  ///
  /// @param instant the instant to create the date-time from
  /// @return the date and time in *Internet Time*
  static InternetTime ofInstant(Instant instant) {
    return new InternetTime(LocalDateTime.ofInstant(requireNonNull(instant, "instant"), ZONE));
  }

  /// Obtains an instance of `InternetTime` from a year, month, day, *.beat*, fractional hundredths
  /// of a *.beat*, and offset.
  ///
  /// This method may be best suited for writing test cases.  Non-test code may prefer other
  /// methods like [#ofInstant(Instant)] and [#from(TemporalAccessor)].  For non-test code using
  /// the concepts of [BEAT_OF_DAY][InternetTimeField#BEAT_OF_DAY] and
  /// [CENTIBEAT_OF_BEAT][InternetTimeField#CENTIBEAT_OF_BEAT] directly, see:
  /// [#of(LocalDate, int, int, ZoneOffset)].
  ///
  /// @param year the year
  /// @param month the month of the year, from 1 (January) to 12 (December)
  /// @param dayOfMonth the day of the month, from 1 to 31 (depending on the month)
  /// @param beat the number of *.beats* since *Internet Time* midnight, from 0 to 999
  /// @param hundredthsOfBeat of a *.beat*, the fractional part since the `beat` began, from 0 to 99
  /// @param offset the zone offset of the date (the `beat` and `hundredths` are not affected)
  /// @return the date and time in *Internet Time*
  /// @throws DateTimeException if any field value is out of range or not valid for the month & year
  public static InternetTime of(
      int year, int month, int dayOfMonth, int beat, int hundredthsOfBeat, ZoneOffset offset) {
    return of(LocalDate.of(year, month, dayOfMonth), beat, hundredthsOfBeat, offset);
  }

  /// Obtains an instance of `InternetTime` from a year, month, day, *.beat*, fractional hundredths
  /// of a *.beat*, and offset.
  ///
  /// This method may be best suited for writing test cases.  Non-test code may prefer other
  /// methods like [#ofInstant(Instant)] and [#from(TemporalAccessor)].  For non-test code using
  /// the concepts of [BEAT_OF_DAY][InternetTimeField#BEAT_OF_DAY] and
  /// [CENTIBEAT_OF_BEAT][InternetTimeField#CENTIBEAT_OF_BEAT] directly, see:
  /// [#of(LocalDate, int, int, ZoneOffset)].
  ///
  /// @param year the year
  /// @param month the month of the year
  /// @param dayOfMonth the day of the month, from 1 to 31 (depending on the month)
  /// @param beat the number of *.beats* since *Internet Time* midnight, from 0 to 999
  /// @param hundredthsOfBeat of a *.beat*, the fractional part since the `beat` began, from 0 to 99
  /// @param offset the zone offset of the date (the `beat` and `hundredths` are not affected)
  /// @return the date and time in *Internet Time*
  /// @throws DateTimeException if any field value is out of range or not valid for the month & year
  public static InternetTime of(
      int year, Month month, int dayOfMonth, int beat, int hundredthsOfBeat, ZoneOffset offset) {
    return of(LocalDate.of(year, month, dayOfMonth), beat, hundredthsOfBeat, offset);
  }

  /// Obtains an instance of `InternetTime` from a `LocalDate`, *.beat*, fractional hundredths
  /// of a *.beat*, and offset.
  ///
  /// This method is best suited for code operating natively in [beats][InternetTimeUnit#BEATS] and
  /// [centibeats][InternetTimeUnit#CENTIBEATS] but with other `java.time` API types for other
  /// components like dates.
  ///
  /// @param date the local date
  /// @param beat the number of *.beats* since *Internet Time* midnight, from 0 to 999
  /// @param hundredthsOfBeat of a *.beat*, the fractional part since the `beat` began, from 0 to 99
  /// @param offset the zone offset of the local date (the `beat` and `hundredths` are not affected)
  /// @return the date and time in *Internet Time*
  /// @throws DateTimeException if any field value is out of range
  public static InternetTime of(LocalDate date, int beat, int hundredthsOfBeat, ZoneOffset offset) {
    date = normalizeLocalDate(date, offset, beat, hundredthsOfBeat);
    return new InternetTime(date, CENTIBEATS_PER_BEAT * beat + hundredthsOfBeat);
  }

  private static LocalDate normalizeLocalDate(
      LocalDate date, ZoneOffset dateOffset, int beat, int hundredthsOfBeat) {
    int adjustMs = 1000 * (dateOffset.getTotalSeconds() - ZONE.getTotalSeconds());
    if (adjustMs != 0) {
      long msOfDay = beatsToMillisOfDay(beat, hundredthsOfBeat);
      long adjusted = msOfDay + adjustMs;
      if (adjusted < 0) {
        date = date.plusDays(1); // date in Biel will be the next day
      } else if (adjusted >= MILLIS_PER_DAY) {
        date = date.minusDays(1); // date in Biel will be the prior day
      }
    }
    return date;
  }

  // </editor-fold>

  // <editor-fold desc="Formatting & Parsing Convenience Methods" defaultstate="collapsed"> ------

  /// Obtains an instance of `InternetTime` from a text string using a specific formatter.
  ///
  /// Formatters that only support *local* date-time values will be assumed to occur in the
  /// [Internet Time zone][#ZONE] (UTC+1).  Formatters that only support dates without a time
  /// component are not supported.
  ///
  /// @param text to be parsed
  /// @param formatter the formatter to use
  /// @return the parsed Internet Time
  /// @throws DateTimeException if an error occurs during printing
  public static InternetTime parse(CharSequence text, DateTimeFormatter formatter) {
    return requireNonNull(formatter, "formatter").parse(text, InternetTime::fromParsed);
  }

  /// Formats this Internet Time value using the provided formatter.
  ///
  /// If the format outputs a date, it will be in the [Internet Time zone][#ZONE] (UTC+1).
  /// Formatters that output a zone and/or offset will output the same zone.  If the formatter
  /// outputs a time, the value will be at the start of the current [beat][#getBeat()] and
  /// [fractional beat][#getCentibeatOfBeat()].
  ///
  /// @param formatter the formatter to use
  /// @return the formatted Internet Time string
  /// @throws DateTimeException if an error occurs during printing
  public String format(DateTimeFormatter formatter) {
    return requireNonNull(formatter, "formatter").format(this);
  }

  /// Obtains a formatter that renders the time as **.beats**, for example: `@321.98`.
  /// The provided `FormatStyle` determines the granularity and whether to prefix the
  /// value with the `@` symbol traditionally used by *Swatch&reg;*.
  ///
  /// | `FormatStyle` | Example |
  /// |---------------|---------|
  /// | `SHORT`       |  457    |
  /// | `MEDIUM`      | @457    |
  /// | `LONG`        |  457.89 |
  /// | `FULL`        | @457.89 |
  ///
  /// The returned formatters *only* render the time, not the date.  There are static fields on the
  /// [InternetTime] class for some common formats.  A [DateTimeFormatterBuilder] can join the
  /// output of this method with a preferred date representation or pattern to create custom
  /// formats.
  ///
  /// Because the *.beat* time notation is the same in all time zones for a given instant, when
  /// joining with a date format, it can be ambiguous whether the rendered date is the standard
  /// *Internet Time* date or the end-user's local date.  Including the UTC offset, such as with
  /// [DateTimeFormatter#ISO_OFFSET_DATE], is one possible solution (see: [#OFFSET_DATE_BEATS]).
  ///
  /// @param style `SHORT` and `MEDIUM` display whole *.beat* values without or with a leading `@`
  ///     symbol, respectively.  `LONG` and `FULL` are similar but add fractional centibeats.
  /// @return formatter for time (only) displayed as *.beats*
  public static DateTimeFormatter beatFormatter(FormatStyle style) {
    int ordinal = requireNonNull(style, "style").ordinal();
    return (DATETIMEFORMAT_ARRAY_VARHANDLE.getVolatile(TIME_FORMATTERS_BY_STYLE, ordinal)
        instanceof DateTimeFormatter f) ? f : makeTimeFormat(style);
  }

  private static DateTimeFormatter makeTimeFormat(FormatStyle style) {
    var fmt = appendTimeFormat(new DateTimeFormatterBuilder(), style).toFormatter();
    DATETIMEFORMAT_ARRAY_VARHANDLE.setVolatile(TIME_FORMATTERS_BY_STYLE, style.ordinal(), fmt);
    return fmt;
  }

  private static DateTimeFormatterBuilder appendTimeFormat(
      DateTimeFormatterBuilder builder, FormatStyle style) {
    final UnaryOperator<DateTimeFormatterBuilder> centibeats =
        b -> b.optionalStart()
            .appendLiteral('.')
            .appendFraction(CENTIBEAT_OF_BEAT, 2, 2, false) // non-i18n decimal
            .optionalEnd();
    return switch (style) {
      case SHORT -> builder.appendValue(BEAT_OF_DAY, 3);
      case MEDIUM -> builder.appendLiteral("@").appendValue(BEAT_OF_DAY, 3);
      case LONG -> centibeats.apply(appendTimeFormat(builder, FormatStyle.SHORT));
      case FULL -> centibeats.apply(appendTimeFormat(builder, FormatStyle.MEDIUM));
    };
  }

  private static DateTimeFormatter joinFormats(
      DateTimeFormatter dateFormat, DateTimeFormatter timeFormat) {
    return new DateTimeFormatterBuilder()
        .append(dateFormat)
        .appendLiteral(' ')
        .append(timeFormat)
        .toFormatter();
  }

  private static InternetTime fromParsed(TemporalAccessor ta) {
    LocalDate date;
    if (ta.isSupported(DAY_OF_MONTH) && ta.isSupported(MONTH_OF_YEAR) && ta.isSupported(YEAR)) {
      date = LocalDate.of(ta.get(YEAR), ta.get(MONTH_OF_YEAR), ta.get(DAY_OF_MONTH));
    } else {
      date = LocalDate.EPOCH;
    }
    var offset = ta.isSupported(OFFSET_SECONDS)
        ? ZoneOffset.ofTotalSeconds(ta.get(OFFSET_SECONDS))
        : ZONE;
    int centibeats;
    if (ta.isSupported(CENTIBEAT_OF_DAY)) {
      centibeats = (int) ta.getLong(CENTIBEAT_OF_DAY);
    } else if (ta.isSupported(BEAT_OF_DAY)) {
      centibeats = (int) ta.getLong(BEAT_OF_DAY) * CENTIBEATS_PER_BEAT;
      if (ta.isSupported(CENTIBEAT_OF_BEAT)) {
        centibeats += (int) ta.getLong(CENTIBEAT_OF_BEAT);
      }
    } else if (ta.isSupported(MILLI_OF_DAY)) {
      centibeats =
          (int) CENTIBEATS.fromMillis(
              InternetTimeField.toNormalizedMilliOfDay(ta, offset.getTotalSeconds()));
    } else if (ta.isSupported(INSTANT_SECONDS)) {
      long instantSecs = INSTANT_SECONDS.getFrom(ta);
      long msPart =
          ta.isSupported(NANO_OF_SECOND)
              ? TimeUnit.NANOSECONDS.toMillis(NANO_OF_SECOND.getFrom(ta))
              : 0;
      return ofInstant(Instant.ofEpochMilli(TimeUnit.SECONDS.toMillis(instantSecs) + msPart));
    } else {
      throw cannotDeriveFrom(ta);
    }
    date = normalizeLocalDate(date, offset, centibeats / CENTIBEATS_PER_BEAT, 0 /* fractional */);
    return new InternetTime(date, centibeats);
  }

  // </editor-fold>

  // <editor-fold desc="Simple Instance Methods" defaultstate="collapsed"> -----------------------

  @Override
  public int hashCode() {
    return date.hashCode() * 31 + Integer.hashCode(centibeatOfDay);
  }

  /// Checks if this date-time is equal to another `InternetTime` date-time. Other types return
  /// `false`.
  ///
  /// @param other the object to check, `null` returns `false`
  /// @return `true` if `other` is an `InternetTime` representing the same instant on the time-line
  @Override
  public boolean equals(Object other) {
    return other instanceof InternetTime it
        && centibeatOfDay == it.centibeatOfDay
        && date.equals(it.date);
  }

  /// Compares this date-time to another date-time.  The comparison is based on the local date,
  /// then the cumulative centibeats elapsed since midnight.  This comparison is consistent with
  /// `equals()`.
  ///
  /// @param other the other date-time to compare to
  /// @return the comparator value: negative if less, positive if greater
  @Override
  public int compareTo(InternetTime other) {
    requireNonNull(other);
    int val = date.compareTo(other.date);
    if (0 == val) {
      val = Integer.compare(centibeatOfDay, other.centibeatOfDay);
    }
    return val;
  }

  /// Outputs this date-time as a `String` suitable for debugging.
  ///
  /// The format of the output is not specified and is subject to change between revisions.
  /// For stable, deterministic values use [#format].
  ///
  /// @return a string representation of this `InternetTime` date and time
  @Override
  public String toString() {
    return RETRO_DATE_CENTIBEATS.format(this);
  }

  /// {@return the year field as a primitive `int` value}
  ///
  /// The year returned by this method is proleptic as per `get(YEAR)`. To obtain the year-of-era,
  /// use `get(YEAR_OF_ERA)`.
  public int getYear() {
    return date.getYear();
  }

  /// {@return the month-of-year field using the `Month` enum}
  ///
  /// The enum provides the primitive [int value][Month#getValue], if required.
  public Month getMonth() {
    return date.getMonth();
  }

  /// {@return the day-of-month field as a primitive `int` value, from 1 to 31}
  public int getDayOfMonth() {
    return date.getDayOfMonth();
  }

  /// {@return the day-of-year field as a primitive `int` value, from 1 to 365 (366 in leap years)}
  public int getDayOfYear() {
    return date.getDayOfYear();
  }

  /// {@return the day-of-week field using the `DayOfWeek` enum}
  ///
  /// The enum provides the primitive [int value][DayOfWeek#getValue], if required.
  public DayOfWeek getDayOfWeek() {
    return date.getDayOfWeek();
  }

  /// {@return the beat-of-day field as a primitive `int` value, from 0 to 999}
  public int getBeat() {
    return centibeatOfDay / CENTIBEATS_PER_BEAT;
  }

  /// {@return the centibeat-of-day field as a primitive `int` value, from 0 to 99,999}
  public int getCentibeatOfDay() {
    return centibeatOfDay;
  }

  /// {@return the centibeat-of-beat field as a primitive `int` value, from 0 to 99}
  public int getCentibeatOfBeat() {
    return centibeatOfDay % CENTIBEATS_PER_BEAT;
  }

  private long millisecondOfDay() {
    return CENTIBEATS.toMillis(centibeatOfDay);
  }

  // </editor-fold>

  // <editor-fold desc="Type Conversion Methods" defaultstate="collapsed"> -----------------------

  /// Converts this date-time to an `Instant`
  /// @return an `Instant` representing the same instant on the time-line
  public Instant toInstant() {
    var dateEpochSecond = toLocalDate().toEpochSecond(LocalTime.MIDNIGHT, ZONE);
    return Instant.ofEpochMilli(millisecondOfDay() + TimeUnit.SECONDS.toMillis(dateEpochSecond));
  }

  /// Converts this date-time to an `OffsetDateTime`.
  ///
  /// The produced time is always in the time-zone offset UTC+1, but it can be
  /// [separately converted][OffsetDateTime#withOffsetSameInstant] to a date-time in another
  /// zone given the other zone's offset.
  ///
  /// @return an `OffsetDateTime` representing the same instant on the time-line
  public OffsetDateTime toOffsetDateTime() {
    return OffsetDateTime.of(toLocalDate(), toLocalTime(), ZONE);
  }

  /// Converts this date-time to a `ZonedDateTime`.
  ///
  /// The produced time is always in the time-zone offset UTC+1, but it can be
  /// [separately converted][ZonedDateTime#withZoneSameInstant] to a date-time in another
  /// zone given the other zone's [ZoneId].
  ///
  /// @return a `ZonedDateTime` representing the same instant on the time-line
  public ZonedDateTime toZonedDateTime() {
    return ZonedDateTime.of(toLocalDate(), toLocalTime(), ZONE);
  }

  /// Gets the local date-time in the *Internet Time* zone.
  /// @return a `LocalDateTime` representing the date-time in the *Internet Time* UTC+1 offset
  public LocalDateTime toLocalDateTime() {
    return LocalDateTime.of(toLocalDate(), toLocalTime());
  }

  /// Gets the local date in the *Internet Time* zone.
  /// @return a `LocalDate` representing the date in the *Internet Time* UTC+1 offset
  public LocalDate toLocalDate() {
    return date;
  }

  private LocalTime toLocalTime() {
    return LocalTime.ofNanoOfDay(TimeUnit.MILLISECONDS.toNanos(millisecondOfDay()));
  }

  /// Gets the *Internet Time* ISO time-of-day without the date.
  ///
  /// The produced time is always in the time-zone offset UTC+1, but it can be
  /// [separately converted][OffsetTime#withOffsetSameInstant(ZoneOffset)] to the time in another
  /// zone given the other zone's offset.  These values are useful for time-only display purposes
  /// where the contextual date (if any) is handled separately.
  ///
  /// @return an `OffsetTime` representing the time-of-day in the *Internet Time* UTC+1 offset
  public OffsetTime toOffsetTime() {
    return OffsetTime.of(toLocalTime(), ZONE);
  }

  /// Equivalent to `timeOfBeat(beats, 0)`.
  /// @see #timeOfBeat(int, int)
  ///
  /// @param beats time of day in whole *.beats*, from 0 to 999, inclusive
  /// @return a standard hour, minute, second, and millisecond representation of the time
  ///     in the UTC+1 time-zone
  /// @throws DateTimeException if input value is out of range
  public static OffsetTime timeOfBeat(int beats) {
    return timeOfBeat(beats, 0);
  }

  /// Produces an `OffsetTime` value for the given *.beat* and fractional portion.
  ///
  /// The produced time is always in the time-zone offset UTC+1, but it can be
  /// [separately converted][OffsetTime#withOffsetSameInstant(ZoneOffset)] to the time in another
  /// zone given the other zone's offset.  These values are useful for time-only display purposes
  /// where the contextual date (if any) is handled separately.
  ///
  /// @param beats time of day in whole *.beats*, from 0 to 999, inclusive
  /// @param hundredthsOfBeat partial *.beat*, in centibeats elapsed since the beginning of `beats`,
  ///     from 0 to 99, inclusive
  /// @return a standard hour, minute, second, and millisecond representation of the time
  ///     in the UTC+1 time-zone
  /// @throws DateTimeException if any input values are out of range
  public static OffsetTime timeOfBeat(int beats, int hundredthsOfBeat) {
    long msOfDay = beatsToMillisOfDay(beats, hundredthsOfBeat);
    int totalSec = (int) (msOfDay / 1000);
    int hr = totalSec / 3600;
    int min = (totalSec % 3600) / 60;
    int sec = totalSec % 60;
    int ms = (int) (msOfDay % 1000);
    return OffsetTime.of(hr, min, sec, (int) TimeUnit.MILLISECONDS.toNanos(ms), ZONE);
  }

  private static long beatsToMillisOfDay(int beats, int centibeatsOfBeat) {
    BEAT_OF_DAY.range().checkValidIntValue(beats, BEAT_OF_DAY);
    CENTIBEAT_OF_BEAT.range().checkValidIntValue(centibeatsOfBeat, CENTIBEAT_OF_BEAT);
    return BEATS.toMillis(beats) + CENTIBEATS.toMillis(centibeatsOfBeat);
  }

  // </editor-fold>

  // <editor-fold desc="Alignment Methods" defaultstate="collapsed"> -----------------------------

  /// Aligns a date-time value of another type to the first millisecond-of-day of its equivalent
  /// *Internet Time* value.  Supported `Temporal` types must at minimum allow querying and
  /// adjusting the [millisecond-of-day][ChronoField#MILLI_OF_DAY] field. Date-only values, such
  /// as `LocalDate`, are not supported.
  ///
  /// The alignment resolution, to the *.beat* or *centibeat*, is selected by specifying one of
  /// the [InternetTimeField] enum instances.  For the purposes of this operation, the field
  /// [InternetTimeField#CENTIBEAT_OF_BEAT] aligns to the same millisecond as
  /// [InternetTimeField#CENTIBEAT_OF_DAY].
  ///
  /// @param <R> type of `Temporal` value to align and return
  /// @param field resolution to use for alignment
  /// @param temporal the date-time value from which to derive the aligned result
  /// @return a value at or just before `temporal`, and of the same type, aligned to the beginning
  ///     millisecond-of-day at the given field resolution of *Internet Time*
  /// @throws UnsupportedTemporalTypeException if the `temporal` value type is not supported
  public static <R extends Temporal> R toStartOf(InternetTimeField field, R temporal) {
    requireNonNull(field, "field");
    TemporalLongToObj<Temporal> factory;
    long millis;
    switch (requireNonNull(temporal, "temporal")) {
      case InternetTime it when
          0 == it.getCentibeatOfBeat() || CENTIBEATS.equals(field.getBaseUnit()) -> {
        return temporal; // under these conditions, already aligned to the requested field
      }
      case Instant i -> {
        var odt = i.atOffset(ZONE);
        millis = field.truncate(odt.getLong(MILLI_OF_DAY));
        millis = (millis % 1000)
            + TimeUnit.SECONDS.toMillis(odt.with(MILLI_OF_DAY, millis).toEpochSecond());
        factory = (__, val) -> Instant.ofEpochMilli(val);
      }
      default -> {
        if (!temporal.isSupported(MILLI_OF_DAY)) {
          throw field.exceptionForUnsupported(temporal);
        }
        millis = field.truncate(temporal.getLong(MILLI_OF_DAY));
        factory = MILLI_OF_DAY::adjustInto;
      }
    }
    @SuppressWarnings("unchecked")
    R r = (R) factory.apply(temporal, millis);
    return r;
  }

  /// Creates an `OffsetDateTime` at the nearest whole-second boundary. All smaller units, such as
  /// millisecond-of-second, etc., will be zero(0).
  ///
  /// Within a day, only 0.8% of `InternetTime` values align naturally on a whole-second boundary.
  /// The nearest second can occur *before or after* this `InternetTime` value on the time line.
  ///
  /// @return an `OffsetDateTime` near the current instant represented by this `InternetTime`, but
  ///     aligned on a whole-second (second-of-day) boundary
  public OffsetDateTime toNearestSecond() {
    var odt = toOffsetDateTime();
    int remainder = odt.get(MILLI_OF_SECOND);
    int adjustment = (Integer.signum(499 - remainder) >>> 31) * 1000 - remainder;
    return odt.plusNanos(TimeUnit.MILLISECONDS.toNanos(adjustment));
  }

  // </editor-fold>

  // <editor-fold desc="TemporalAccessor Interface Methods" defaultstate="collapsed"> ------------

  @Override
  public boolean isSupported(TemporalField field) {
    return switch (field) {
      case null -> false;
      case InternetTimeField __ -> true;
      case ChronoField cf -> OffsetDateTime.MIN.isSupported(cf); // this should behave like an ODT
      default -> field.isSupportedBy(this);
    };
  }

  private void checkUnsupportedField(ChronoField field) {
    if (!isSupported(field)) {
      throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
    }
  }

  @Override
  public ValueRange range(TemporalField field) {
    if (field instanceof ChronoField cf) {
      checkUnsupportedField(cf);
      return switch (cf) {
        case INSTANT_SECONDS, OFFSET_SECONDS -> cf.range();
        default -> date.isSupported(cf) ? date.range(cf) : LocalTime.MIDNIGHT.range(cf);
      };
    }
    return requireNonNull(field, "field").rangeRefinedBy(this);
  }

  @Override
  public <R> R query(TemporalQuery<R> query) {
    // NOTE: Object reference equivalence is intended!  See TemporalAccessor#query API spec
    Object answer;
    if (query == TemporalQueries.chronology()) {
      answer = IsoChronology.INSTANCE;
    } else if (query == TemporalQueries.precision()) {
      answer = CENTIBEATS;
    } else if (query == TemporalQueries.offset()) {
      // NOTE: *not* overriding default for ZoneId query, see TemporalQueries#zoneId for why
      answer = ZONE;
    } else {
      return Temporal.super.query(query); // REQUIRED by API spec
    }
    @SuppressWarnings("unchecked")
    R r = (R) answer;
    return r;
  }

  @Override
  public long getLong(TemporalField field) {
    return switch (field) {
      case InternetTimeField itf -> getLong(itf);
      case ChronoField cf -> getLong(cf);
      default -> field.getFrom(this);
    };
  }

  long getLong(InternetTimeField field) {
    return switch (field) {
      case BEAT_OF_DAY -> getBeat();
      case CENTIBEAT_OF_DAY -> getCentibeatOfDay();
      case CENTIBEAT_OF_BEAT -> getCentibeatOfBeat();
    };
  }

  private long getLong(ChronoField field) {
    return switch (field) {
      case NANO_OF_DAY -> millisecondOfDay() * 1_000_000;

      case MICRO_OF_DAY -> millisecondOfDay() * 1000;

      case OFFSET_SECONDS -> ZONE.getTotalSeconds();

      case INSTANT_SECONDS -> date.toEpochSecond(LocalTime.MIDNIGHT, ZONE) + getLong(SECOND_OF_DAY);

      case MILLI_OF_DAY -> millisecondOfDay();

      case SECOND_OF_DAY -> millisecondOfDay() / 1000;

      case MINUTE_OF_DAY -> millisecondOfDay() / 60_000;

      case HOUR_OF_DAY -> millisecondOfDay() / (60 * 60_000);

      case CLOCK_HOUR_OF_DAY -> { var hr = getLong(HOUR_OF_DAY); yield 0 == hr ? 24 : hr; }

      case HOUR_OF_AMPM -> getLong(HOUR_OF_DAY) % 12;

      case CLOCK_HOUR_OF_AMPM -> { var hr = getLong(HOUR_OF_AMPM); yield 0 == hr ? 12 : hr; }

      case AMPM_OF_DAY ->
          TimeUnit.MILLISECONDS.toNanos(millisecondOfDay()) >= LocalTime.NOON.toNanoOfDay() ? 1 : 0;

      case NANO_OF_SECOND -> millisecondOfDay() % 1000 * 1_000_000;

      case MICRO_OF_SECOND -> millisecondOfDay() % 1000 * 1000;

      case MILLI_OF_SECOND ->  millisecondOfDay() % 1000;

      case SECOND_OF_MINUTE -> millisecondOfDay() % 60_000 / 1000;

      case MINUTE_OF_HOUR -> millisecondOfDay() % (60 * 60_000) / 60_000;

      // anything that's not a time field:
      default -> date.getLong(field);
    };
  }

  // </editor-fold>

  // <editor-fold desc="Temporal Interface Methods" defaultstate="collapsed"> --------------------

  @Override
  public boolean isSupported(TemporalUnit unit) {
    return switch (unit) {
      case null -> false;
      case InternetTimeUnit __ -> true;
      case ChronoUnit cu ->
        cu.ordinal() >= ChronoUnit.MILLIS.ordinal() // does not support finer resolution than ms
        && OffsetDateTime.MIN.isSupported(cu); // otherwise this should behave like OffsetDateTime
      default -> unit.isSupportedBy(this);
    };
  }

  private void checkUnsupportedUnit(ChronoUnit unit) {
    if (!isSupported(unit)) {
      throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
    }
  }

  @Override
  public long until(Temporal endExclusive, TemporalUnit unit) {
    requireNonNull(endExclusive, "endExclusive");
    requireNonNull(unit, "unit");
    OffsetDateTime endODT;
    try {
      endODT = switch (endExclusive) {
        case ZonedDateTime zdt -> zdt.toOffsetDateTime();
        case InternetTime it -> it.toOffsetDateTime();
        default -> OffsetDateTime.from(endExclusive);
      };
    } catch (DateTimeException __) {
      var type = endExclusive.getClass().getName();
      throw new DateTimeException(
          "InternetTime is not compatible with " + type + ": " + endExclusive);
    }
    if (unit instanceof ChronoUnit cu) {
      checkUnsupportedUnit(cu);
      return toOffsetDateTime().until(endODT, unit);
    }
    return unit.between(this, endODT);
  }

  @Override
  public InternetTime with(TemporalAdjuster adjuster) {
    return (InternetTime) Temporal.super.with(adjuster);
  }

  @Override
  public InternetTime with(TemporalField field, long newValue) {
    if (field instanceof InternetTimeField itf) {
      int itfValue = itf.range().checkValidIntValue(newValue, itf);
      return switch (itf) {
        case BEAT_OF_DAY -> new InternetTime(date, itfValue * CENTIBEATS_PER_BEAT);
        case CENTIBEAT_OF_DAY -> new InternetTime(date, itfValue);
        case CENTIBEAT_OF_BEAT ->
          new InternetTime(date, getCentibeatOfDay() - getCentibeatOfBeat() + itfValue);
      };
    }
    if (field instanceof ChronoField cf) {
      checkUnsupportedField(cf);
      // FUTURE: this could be better optimized, but I'm lazy for now
      return new InternetTime(toOffsetDateTime().with(field, newValue).toLocalDateTime());
    }
    return field.adjustInto(this, newValue);
  }

  @Override
  public InternetTime minus(TemporalAmount amount) {
    return (InternetTime) Temporal.super.minus(amount);
  }

  @Override
  public InternetTime minus(long amount, TemporalUnit unit) {
    return (InternetTime) Temporal.super.minus(amount, unit);
  }

  @Override
  public InternetTime plus(TemporalAmount amount) {
    return (InternetTime) Temporal.super.plus(amount);
  }

  @Override
  public InternetTime plus(long amount, TemporalUnit unit) {
    if (unit instanceof InternetTimeUnit itu) {
      amount = itu.toMillis(amount);
      unit = ChronoUnit.MILLIS;
      // fall through
    }
    if (unit instanceof ChronoUnit cu) {
      checkUnsupportedUnit(cu);
      // FUTURE: this could be better optimized, but I'm lazy for now
      return new InternetTime(toOffsetDateTime().plus(amount, unit).toLocalDateTime());
    }
    return unit.addTo(this, amount);
  }

  // </editor-fold>

  @FunctionalInterface
  private interface TemporalLongToObj<T> {
    T apply(Temporal temporal, long value);
  }
}
