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

import static io.github.wrprice.inettime.InternetTimeUnit.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.Objects.requireNonNull;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime; // for Javadoc reference
import java.time.ZoneOffset;
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
  /// The **.beat** of the day, or the number of *millidays* since UTC+1 midnight.
  ///
  /// Counts the *.beat* within the day, from 0 to (1,000 - 1).
  BEAT_OF_DAY("BeatOfDay", BEATS, 999),

  /// The [centibeats][InternetTimeUnit#CENTIBEATS] elapsed since UTC+1 midnight.
  ///
  /// Counts the *centibeats* within the day, from 0 to (100,000 - 1).
  CENTIBEAT_OF_DAY("CentibeatOfDay", CENTIBEATS, 999_99),

  /// The [centibeats][InternetTimeUnit#CENTIBEATS] elapsed since the last
  /// [*.beat*][#BEAT_OF_DAY]-of-day boundary, from 0 to (100 - 1).
  CENTIBEAT_OF_BEAT("CentibeatOfBeat", CENTIBEATS, 99) {
    @Override
    public TemporalUnit getRangeUnit() {
      return InternetTimeUnit.BEATS;
    }

    @Override
    public long getFrom(TemporalAccessor temporal) {
      return super.getFrom(temporal) % CENTIBEATS_PER_BEAT;
    }

    @Override
    long adjustIntoMillisOfDayWithOffset(long newValue, Temporal temporal, int utcOffsetSecs) {
      long centibeats = CENTIBEAT_OF_DAY.getFrom(temporal);
      long priorValue = centibeats % CENTIBEATS_PER_BEAT;
      centibeats += newValue - priorValue;
      return super.adjustIntoMillisOfDayWithOffset(centibeats, temporal, utcOffsetSecs);
    }
  }
  ; // end of enum instances

  private static final long MAX_MILLI_OF_DAY =
      ChronoField.MILLI_OF_DAY.range().getMaximum();

  static final long MILLIS_PER_DAY = MAX_MILLI_OF_DAY + 1;

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

  /// The unit this field is measured in.
  ///
  /// @return an [InternetTimeUnit]
  @Override
  public TemporalUnit getBaseUnit() {
    return unit;
  }

  /// The unit this field in bound by.
  ///
  /// For example, for [#BEAT_OF_DAY] this returns [ChronoUnit#DAYS] but for [#CENTIBEAT_OF_BEAT]
  /// this returns [InternetTimeUnit#BEATS].
  ///
  /// @return denominator unit that defines the range
  @Override
  public TemporalUnit getRangeUnit() {
    return ChronoUnit.DAYS;
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
    return switch (temporal) {
      case InternetTime __ -> true;
      case Instant __ -> true;
      case null -> false;
      default ->
          ChronoField.MILLI_OF_DAY.isSupportedBy(temporal)
              && ChronoField.OFFSET_SECONDS.isSupportedBy(temporal);
    };
  }

  private void throwIfNotSupported(TemporalAccessor temporal) {
    if (!isSupportedBy(requireNonNull(temporal, "temporal"))) {
      throw exceptionForUnsupported(temporal);
    }
  }

  DateTimeException exceptionForUnsupported(TemporalAccessor ta) {
    return new UnsupportedTemporalTypeException(
        this + " not supported by " + ta.getClass() + ": " + ta);
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
    final int utcOffsetSecs;
    switch (temporal) {
      case InternetTime it -> { return it.getLong(this); }
      case Instant i -> {
        utcOffsetSecs = ZoneOffset.UTC.getTotalSeconds();
        temporal = i.atOffset(ZoneOffset.UTC);
      }
      default -> {
        throwIfNotSupported(temporal);
        utcOffsetSecs = temporal.get(ChronoField.OFFSET_SECONDS);
      }
    }
    return unit.fromMillis(toNormalizedMilliOfDay(temporal, utcOffsetSecs));
  }

  // Internal support for InternetTime#<init>(LocalDateTime) in (assumed) correct ZoneOffset
  long getFrom(LocalDateTime temporal, int utcOffsetSecs) {
    return unit.fromMillis(toNormalizedMilliOfDay(temporal, utcOffsetSecs));
  }

  static long toNormalizedMilliOfDay(TemporalAccessor temporal, int utcOffsetSecs) {
    long milliOfDay = ChronoField.MILLI_OF_DAY.getFrom(temporal);
    milliOfDay = Math.subtractExact(milliOfDay, SECONDS.toMillis(utcOffsetSecs)); // to UTC
    milliOfDay = Math.addExact(milliOfDay, INET_UTC_OFFSET_MILLIS); // UTC --> INet Time
    return wrapMilliOfDay(milliOfDay);
  }

  @Override
  public <R extends Temporal> R adjustInto(R temporal, long newValue) {
    if (!range().isValidValue(newValue)) {
      throw new DateTimeException("Value out of range for " + this + ": " + newValue);
    }
    switch (temporal) {
      case InternetTime it -> {
        @SuppressWarnings("unchecked")
        R r = (R) it.with(this, newValue);
        return r;
      }
      case Instant i -> {
        @SuppressWarnings("unchecked")
        R r = (R) adjustInto(i.atOffset(ZoneOffset.UTC), newValue).toInstant();
        return r;
      }
      default -> throwIfNotSupported(temporal);
    }
    int utcOffsetSecs = temporal.get(ChronoField.OFFSET_SECONDS);
    long millisOfDay = adjustIntoMillisOfDayWithOffset(newValue, temporal, utcOffsetSecs);
    return ChronoField.MILLI_OF_DAY.adjustInto(temporal, millisOfDay);
  }

  long adjustIntoMillisOfDayWithOffset(long newValue, Temporal temporal, int utcOffsetSecs) {
    return toOffsetMilliOfDay(unit.toMillis(newValue), utcOffsetSecs);
  }

  private long toOffsetMilliOfDay(long normalizedMillisOfDay, int utcOffsetSecs) {
    long milliOfDay = normalizedMillisOfDay;
    milliOfDay = Math.subtractExact(milliOfDay, INET_UTC_OFFSET_MILLIS); // INet Time --> UTC
    milliOfDay = Math.addExact(milliOfDay, SECONDS.toMillis(utcOffsetSecs)); // UTC -> wanted offset
    return wrapMilliOfDay(milliOfDay);
  }

  static long wrapMilliOfDay(long value) {
    if (value < 0) {
      value %= MILLIS_PER_DAY;
      value -= Long.signum(value) * MILLIS_PER_DAY;
    } else if (value > MAX_MILLI_OF_DAY) {
      value %= MILLIS_PER_DAY;
    }
    assert value >= 0 && value <= MAX_MILLI_OF_DAY;
    return value;
  }

  long truncate(long millisOfDay) {
    var result = unit.toMillis(unit.fromMillis(millisOfDay));
    assert ChronoField.MILLI_OF_DAY.range().checkValidValue(result, this) >= 0;
    return result;
  }
}
