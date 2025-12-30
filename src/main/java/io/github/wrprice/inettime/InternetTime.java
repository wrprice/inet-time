package io.github.wrprice.inettime;

import static io.github.wrprice.inettime.InternetTimeField.*;
import static io.github.wrprice.inettime.InternetTimeUnit.*;
import static io.github.wrprice.inettime.PrivateField.CENTIBEAT_OF_BEAT;
import static java.time.temporal.ChronoField.*;
import static java.util.Objects.requireNonNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.*;
import java.time.temporal.*;
import java.util.concurrent.TimeUnit;

/// Represents a global instant according to the *Swatch&reg; Internet Time* standard:
/// a date-time with a *fixed offset* from <abbr title="Coordinated Universal Time">UTC</abbr>
/// in the ISO calendar.  An example is: `2025-12-28@123.45`.  As implied by the fixed offset,
/// <abbr title="Daylight Saving Time">DST</abbr> *does not apply*.
///
/// The goal is to provide similar functionality as Java's [OffsetDateTime]; however, unlike
/// `OffsetDateTime`, this implementation's time-of-day resolution is limited to the granularity of
/// a single [centibeat][InternetTimeUnit#CENTIBEATS] and the zone offset is not stored because a
/// constant, fixed offset is implied.
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
  public static final ZoneOffset ZONE = ZoneOffset.ofHours(+1);

  /// This Temporal type should have functionality and behavior similar to OffsetDateTime.
  private static final Temporal ANALOG = OffsetDateTime.MIN;

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

  public static InternetTime now() {
    return now(Clock.systemUTC());
  }

  public static InternetTime now(Clock clock) {
    return ofInstant(requireNonNull(clock, "clock").instant());
  }

  public static InternetTime from(TemporalAccessor accessor) {
    requireNonNull(accessor, "accessor");
    return switch (accessor) {
      case InternetTime it -> it;
      case OffsetDateTime dt -> new InternetTime(dt.withOffsetSameInstant(ZONE).toLocalDateTime());
      case ZonedDateTime dt -> new InternetTime(dt.withZoneSameInstant(ZONE).toLocalDateTime());
      default -> {
        try {
          yield ofInstant(Instant.from(accessor));
        } catch (DateTimeException dte) {
          var typeName = accessor.getClass().getName();
          throw new DateTimeException(
              "Cannot derive InternetTime from " + typeName + ": " + accessor);
        }
      }
    };
  }

  public static InternetTime ofInstant(Instant instant) {
    return new InternetTime(LocalDateTime.ofInstant(requireNonNull(instant, "instant"), ZONE));
  }

  public static InternetTime of(
      int year, int month, int dayOfMonth, int beat, int hundredthsOfBeat, ZoneOffset zone) {
    return of(LocalDate.of(year, month, dayOfMonth), beat, hundredthsOfBeat, zone);
  }

  public static InternetTime of(
      int year, Month month, int dayOfMonth, int beat, int hundredthsOfBeat, ZoneOffset zone) {
    return of(LocalDate.of(year, month, dayOfMonth), beat, hundredthsOfBeat, zone);
  }

  public static InternetTime of(LocalDate date, int beat, int hundredthsOfBeat, ZoneOffset offset) {
    long msOfDay = beatsToMillisOfDay(beat, hundredthsOfBeat);
    int adjustMs = 1000 * (offset.getTotalSeconds() - ZONE.getTotalSeconds());
    long adjusted = msOfDay + adjustMs;
    if (adjusted < 0) {
      date = date.plusDays(1); // date in Biel will be the next day
    } else if (adjusted >= MILLIS_PER_DAY) {
      date = date.minusDays(1); // date in Biel will be the prior day
    }
    return new InternetTime(date, CENTIBEATS_PER_BEAT * beat + hundredthsOfBeat);
  }

  // </editor-fold>

  // <editor-fold desc="Simple Instance Methods" defaultstate="collapsed"> -----------------------

  @Override
  public int hashCode() {
    return date.hashCode() * 31 + Integer.hashCode(centibeatOfDay);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof InternetTime it
        && centibeatOfDay == it.centibeatOfDay
        && date.equals(it.date);
  }

  @Override
  public int compareTo(InternetTime other) {
    requireNonNull(other);
    int val = date.compareTo(other.date);
    if (0 == val) {
      val = Integer.compare(centibeatOfDay, other.centibeatOfDay);
    }
    return val;
  }

  @Override
  public String toString() {
    int partialBeats = getCentibeatOfBeat();
    if (0 == partialBeats) {
      return String.format("%s@%03d", toLocalDate(), getBeat());
    } else {
      return String.format("%s@%03d.%02d", toLocalDate(), getBeat(), partialBeats);
    }
  }

  public int getYear() {
    return date.getYear();
  }

  public Month getMonth() {
    return date.getMonth();
  }

  public int getDayOfMonth() {
    return date.getDayOfMonth();
  }

  public int getDayOfYear() {
    return date.getDayOfYear();
  }

  public DayOfWeek getDayOfWeek() {
    return date.getDayOfWeek();
  }

  public int getBeat() {
    return centibeatOfDay / CENTIBEATS_PER_BEAT;
  }

  public int getCentibeatOfDay() {
    return centibeatOfDay;
  }

  public int getCentibeatOfBeat() {
    return centibeatOfDay % CENTIBEATS_PER_BEAT;
  }

  private long millisecondOfDay() {
    return CENTIBEATS.toMillis(centibeatOfDay);
  }

  // </editor-fold>

  // <editor-fold desc="Type Conversion Methods" defaultstate="collapsed"> -----------------------

  public Instant toInstant() {
    var dateEpochSecond = toLocalDate().toEpochSecond(LocalTime.MIDNIGHT, ZONE);
    return Instant.ofEpochMilli(millisecondOfDay() + TimeUnit.SECONDS.toMillis(dateEpochSecond));
  }

  public OffsetDateTime toOffsetDateTime() {
    return OffsetDateTime.of(toLocalDate(), toLocalTime(), ZONE);
  }

  public ZonedDateTime toZonedDateTime() {
    return ZonedDateTime.of(toLocalDate(), toLocalTime(), ZONE);
  }

  public LocalDateTime toLocalDateTime() {
    return LocalDateTime.of(toLocalDate(), toLocalTime());
  }

  public LocalDate toLocalDate() {
    return date;
  }

  private LocalTime toLocalTime() {
    return LocalTime.ofNanoOfDay(TimeUnit.MILLISECONDS.toNanos(millisecondOfDay()));
  }

  public OffsetTime toOffsetTime() {
    return OffsetTime.of(toLocalTime(), ZONE);
  }

  public static OffsetTime timeOfBeat(int beats) {
    return timeOfBeat(beats, 0);
  }

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

  public static <R extends Temporal> R toStartOf(InternetTimeField field, R temporal) {
    requireNonNull(field, "field");
    TemporalLongToObj<Temporal> factory;
    long millis;
    switch (requireNonNull(temporal, "temporal")) {
      case InternetTime it when 0 == it.getCentibeatOfBeat() || CENTIBEAT_OF_DAY.equals(field) -> {
        return temporal; // under these conditions, already aligned to the requested field
      }
      case Instant i -> {
        long epochMillis = i.toEpochMilli();
        long millisOfDay = epochMillis % MILLIS_PER_DAY;
        millis = epochMillis - millisOfDay + field.truncate(millisOfDay);
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

  public OffsetDateTime toNearestSecond() {
    var odt = toOffsetDateTime();
    long msOfDay = odt.get(MILLI_OF_DAY);
    long remainder = msOfDay % 1000;
    msOfDay += (Long.signum(499 - remainder) >>> 31) * 1000 - remainder;
    if (msOfDay >= MILLIS_PER_DAY) {
      odt = odt.plusSeconds(1);
      msOfDay = 0;
    }
    return odt.with(MILLI_OF_DAY, msOfDay);
  }

  // </editor-fold>

  // <editor-fold desc="TemporalAccessor Interface Methods" defaultstate="collapsed"> ------------

  @Override
  public boolean isSupported(TemporalField field) {
    return switch (field) {
      case null -> false;
      case InternetTimeField __ -> true;
      case ChronoField cf -> ANALOG.isSupported(cf);
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
    return Temporal.super.query(query);
  }

  @Override
  public long getLong(TemporalField field) {
    return switch (field) {
      case InternetTimeField itf -> getFieldAsLong(itf);
      case ChronoField cf -> getFieldAsLong(cf);
      default -> field.getFrom(this);
    };
  }

  private long getFieldAsLong(InternetTimeField field) {
    return switch (field) {
      case CENTIBEAT_OF_DAY -> getCentibeatOfDay();
      case BEAT_OF_DAY -> getBeat();
    };
  }

  private long getFieldAsLong(ChronoField field) {
    return switch (field) {
      case NANO_OF_DAY -> millisecondOfDay() * 1_000_000;

      case MICRO_OF_DAY -> millisecondOfDay() * 1000;

      case OFFSET_SECONDS -> ZONE.getTotalSeconds();

      case INSTANT_SECONDS ->
          date.toEpochSecond(LocalTime.MIDNIGHT, ZONE) + getFieldAsLong(SECOND_OF_DAY);

      case MILLI_OF_DAY -> millisecondOfDay();

      case SECOND_OF_DAY -> millisecondOfDay() / 1000;

      case MINUTE_OF_DAY -> millisecondOfDay() / 60_000;

      case HOUR_OF_DAY -> millisecondOfDay() / (60 * 60_000);

      case CLOCK_HOUR_OF_DAY -> { var hr = getFieldAsLong(HOUR_OF_DAY); yield 0 == hr ? 24 : hr; }

      case HOUR_OF_AMPM -> getFieldAsLong(HOUR_OF_DAY) % 12;

      case CLOCK_HOUR_OF_AMPM -> { var hr = getFieldAsLong(HOUR_OF_AMPM); yield 0 == hr ? 12 : hr; }

      case AMPM_OF_DAY -> {
        // if time is before (1ms before Noon) then signum will be -1, negated -> +1, and shift -> 0
        // if time is exactly (1ms before Noon) then signum will be 0, negated -> 0, and shift -> 0
        // if time >= 1ms after (1ms before Noon) then signum will be +1, negated -> -1, shift -> 1
        long time = millisecondOfDay();
        long justBeforeNoon = LocalTime.NOON.getLong(MILLI_OF_DAY) - 1;
        int amPM = -(Long.signum(time - justBeforeNoon)) >>> 31;
        assert 0 == (amPM & ~0x1); // all bits 0 except for possibly the last -> result is 1 or 0
        yield amPM;
      }

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
      case ChronoUnit cu -> cu.ordinal() >= ChronoUnit.MILLIS.ordinal() && ANALOG.isSupported(cu);
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

  // <editor-fold desc="Formatting Convenience Methods" defaultstate="collapsed"> ----------------

  // TODO!

  // </editor-fold>

  // <editor-fold desc="Parsing Convenience Methods" defaultstate="collapsed"> -------------------

  // TODO!

  // </editor-fold>

  @FunctionalInterface
  private interface TemporalLongToObj<T> {
    T apply(Temporal temporal, long value);
  }
}
