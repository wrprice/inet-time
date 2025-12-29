package io.github.wrprice.inettime;

import static io.github.wrprice.inettime.InternetTimeUnit.*;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.Objects.requireNonNull;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalTime; // for Javadoc reference
import java.time.temporal.*;
import java.util.Locale;

/// Temporal fields to access or set time-of-day values in terms of the
/// [Internet Time units][InternetTimeUnit].  These field values require knowledge of a UTC
/// offset, so [LocalTime], [LocalDateTime], date-only, and other "local" [Temporal] values *are
/// not supported*.
///
/// A primary use of these Field objects in application code is to retrieve *Internet Time* values
/// from "normal" [Temporal] and [TemporalAccessor] types found in the standard API, for which
/// there are no [convenience][InternetTime#getBeat()] [methods][InternetTime#getCentibeatOfDay()].
/// For example, `offsetDateTime.get(InternetTimeField.BEAT_OF_DAY)`.  The fields also provide
/// access to the range of valid values.
///
/// **Implementation notes:**  
/// This is a final, immutable and thread-safe `enum`.
///
/// @author William R. Price
public enum InternetTimeField implements TemporalField {
  /// The **.beat** of the day, or the number of *millidays* since midnight.
  ///
  /// Counts the *.beat* within the day, from 0 to (1,000 - 1).
  BEAT_OF_DAY("BeatOfDay", BEATS, 999),

  /// The [centibeats][InternetTimeUnit#CENTIBEATS] elapsed since midnight.
  ///
  /// Counts the *centibeats* within the day, from 0 to (100,000 - 1).
  CENTIBEAT_OF_DAY("CentibeatOfDay", CENTIBEATS, 999_99),
  ; // end of enum instances

  private static final long MAX_MILLI_OF_DAY =
      ChronoField.MILLI_OF_DAY.range().getMaximum();

  private static final long INET_UTC_OFFSET_MILLIS =
      SECONDS.toMillis(InternetTime.ZONE.getTotalSeconds());

  private final String name;
  private final InternetTimeUnit unit;
  private final ValueRange range;

  private InternetTimeField(String name, InternetTimeUnit unit, int valueRangeMax) {
    this.name = name;
    this.unit = unit;
    this.range = ValueRange.of(0, valueRangeMax);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public String getDisplayName(Locale locale) {
    requireNonNull(locale, "Locale");
    return toString(); // FUTURE: localization
  }

  /// {@return an [InternetTimeUnit]}
  @Override
  public TemporalUnit getBaseUnit() {
    return unit;
  }

  /// {@return [ChronoUnit#DAYS]}  All *Internet Time*-specific units are relative to a single day.
  @Override
  public TemporalUnit getRangeUnit() {
    return DAYS;
  }

  /// {@return a value from 0 to the upper range of the field (see enum instance documentation)}
  @Override
  public ValueRange range() {
    return range;
  }

  /// {@return mirrors [TemporalUnit#isDateBased()] from the [base unit][#getBaseUnit()]}
  @Override
  public boolean isDateBased() {
    return getBaseUnit().isDateBased();
  }

  /// {@return mirrors [TemporalUnit#isTimeBased()] from the [base unit][#getBaseUnit()]}
  @Override
  public boolean isTimeBased() {
    return getBaseUnit().isTimeBased();
  }

  @Override
  public boolean isSupportedBy(TemporalAccessor temporal) {
    return temporal instanceof InternetTime
        || (null != temporal
            && ChronoField.MILLI_OF_DAY.isSupportedBy(temporal)
            && ChronoField.OFFSET_SECONDS.isSupportedBy(temporal));
  }

  private void throwIfNotSupported(TemporalAccessor temporal) {
    if (!isSupportedBy(requireNonNull(temporal, "temporal"))) {
      throw new UnsupportedTemporalTypeException(temporal.getClass() + " does not support " + this);
    }
  }

  /// {@return equivalent to [#range()]}  *Internet Time* field ranges do not vary by date nor
  /// based on any other time component.
  ///
  /// @param temporal ignored, except to check for compatibility and `null` (per the interface spec)
  @Override
  public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
    throwIfNotSupported(temporal);
    return range(); // does not vary
  }

  @Override
  public long getFrom(TemporalAccessor temporal) {
    if (temporal instanceof InternetTime it) {
      return switch (this) {
        case BEAT_OF_DAY -> it.getBeat();
        case CENTIBEAT_OF_DAY -> it.getCentibeatOfDay();
      };
    }
    throwIfNotSupported(temporal);
    final long utcOffsetSecs = ChronoField.OFFSET_SECONDS.getFrom(temporal);
    return unit.fromMillis(toNormalizedMilliOfDay(temporal, utcOffsetSecs));
  }

  // Internal support for InternetTime#ofInstant(Instant)
  long getFrom(LocalDateTime temporal, int utcOffsetSecs) {
    return unit.fromMillis(toNormalizedMilliOfDay(temporal, utcOffsetSecs));
  }

  @Override
  public <R extends Temporal> R adjustInto(R temporal, long newValue) {
    if (!range().isValidValue(newValue)) {
      throw new DateTimeException("Value out of range for " + this + ": " + newValue);
    }
    if (temporal instanceof InternetTime it) {
      @SuppressWarnings("unchecked")
      R r = (R) it.with(this, newValue);
      return r;
    }
    throwIfNotSupported(temporal);
    final long utcOffsetSecs = ChronoField.OFFSET_SECONDS.getFrom(temporal);
    final long millisOfDay = toOffsetMilliOfDay(unit.toMillis(newValue), utcOffsetSecs);
    return ChronoField.MILLI_OF_DAY.adjustInto(temporal, millisOfDay);
  }

  private long toNormalizedMilliOfDay(TemporalAccessor temporal, long utcOffsetSecs) {
    long milliOfDay = ChronoField.MILLI_OF_DAY.getFrom(temporal);
    milliOfDay -= SECONDS.toMillis(utcOffsetSecs); // removes offset from UTC --> UTC
    milliOfDay += INET_UTC_OFFSET_MILLIS; // UTC --> INet Time
    return wrapMilliOfDay(milliOfDay);
  }

  private long toOffsetMilliOfDay(long normalizedMillisOfDay, long utcOffsetSecs) {
    long milliOfDay = normalizedMillisOfDay;
    milliOfDay -= INET_UTC_OFFSET_MILLIS; // INet Time --> UTC
    milliOfDay += SECONDS.toMillis(utcOffsetSecs); // UTC --> add desired offset
    return wrapMilliOfDay(milliOfDay);
  }

  private long wrapMilliOfDay(long value) {
    if (value < 0) {
      value += MAX_MILLI_OF_DAY;
    } else if (value > MAX_MILLI_OF_DAY) {
      value -= MAX_MILLI_OF_DAY;
    }
    return value;
  }
}
