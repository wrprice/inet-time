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
import static java.util.concurrent.TimeUnit.HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.*;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.*;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// @author William R. Price
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_INFERRED")
public class InternetTimeTest {

  private InternetTime it = InternetTime.now();

  private static void assertEqualsWithTolerance(Temporal expected, Temporal actual, String what) {
    assertEqualsWithTolerance(InternetTimeUnit.CENTIBEATS, expected, actual, what);
  }

  private static void assertEqualsWithTolerance(
      InternetTimeUnit unit, Temporal expected, Temporal actual, String what) {
    var delta = expected.until(actual, ChronoUnit.MILLIS);
    var tolerance = unit.toMillis(1);
    assertThat(
        what + " (delta tolerance)",
        delta,
        allOf(greaterThan(-tolerance), lessThan(tolerance)));
  }

  @Test
  void zoneAndOffset() {
    var zone = InternetTime.ZONE;
    assertNotNull(zone);
    assertEquals(HOURS.toSeconds(1), zone.getTotalSeconds(), "total seconds");
  }

  @Test
  void howManyCentibeatsAlignOnExactSecondBoundary() {
    var range = CENTIBEAT_OF_DAY.range();
    assertEquals(
        800, // of 100,000
        LongStream.rangeClosed(range.getMinimum(), range.getMaximum())
            .filter(cb -> InternetTimeUnit.CENTIBEATS.toMillis(cb) % 1000 == 0)
            .count());
  }

  @ParameterizedTest
  @CsvSource({
    "Biel, 1",
    "SanFran, -8",
    "NewYork, -5",
    "London, 0",
    "Tokyo, 9",
    "Sydney, 10",
  })
  void wikipediaMidnightExamples(String city, int utcOffsetHours) {
    var itDate = LocalDate.of(2025, 10, 12);
    var itZone = InternetTime.ZONE;
    var reference = OffsetDateTime.of(itDate, LocalTime.MIDNIGHT, itZone); // beat @000.00

    var cityOffset = ZoneOffset.ofHours(utcOffsetHours);
    var cityODT = reference.withOffsetSameInstant(cityOffset);
    it = InternetTime.from(cityODT);

    var it2 = InternetTime.of(cityODT.toLocalDate(), 0, 0, cityOffset); // local date at beat @000
    assertEquals(it, it2, "created from localdate + offset");

    assertEquals("d12.10.2025 @000.00", it.toString(), "toString");
    assertEquals(0, it.getBeat(), "beat");
    assertEquals(0, it.getCentibeatOfDay(), "centibeat");
    assertEquals(0, it.getCentibeatOfBeat(), "centibeat-of-beat");
    assertEquals(reference.getYear(), it.getYear(), "year");
    assertEquals(reference.getMonth(), it.getMonth(), "month");
    assertEquals(reference.getDayOfMonth(), it.getDayOfMonth(), "dayOfMonth");
    assertEquals(reference.getDayOfWeek(), it.getDayOfWeek(), "dayOfWeek");
    assertEquals(reference.getDayOfYear(), it.getDayOfYear(), "dayOfYear");
    assertEquals(reference.toInstant(), it.toInstant(), "reference instant");
    assertEquals(reference.toLocalDate(), it.toLocalDate(), "toLocalDate()");
    assertEquals(reference.toLocalDateTime(), it.toLocalDateTime(), "toLocalDateTime()");
    assertEquals(reference.toOffsetTime(), it.toOffsetTime(), "toOffsetTime()");

    assertEquals(InternetTime.from(reference), it, "from different src offsets, same instant");
    assertEquals(it, InternetTime.ofInstant(cityODT.toInstant()), "from instant");
    assertSame(it, InternetTime.toStartOf(CENTIBEAT_OF_DAY, it), "toStartOf(CENTI, IT)");

    // *h:00m:00s.0 in different zones may not align with @000.00
    assertEqualsWithTolerance(cityODT.toInstant(), it.toInstant(), "instant conversion");
    assertEquals(cityODT, it.toNearestSecond().withOffsetSameInstant(cityOffset), "nearest second");
    assertEquals(
        cityODT.toOffsetTime(),
        it.toOffsetTime().withOffsetSameInstant(cityOffset),
        "toOffsetTime adjusted to city");
  }

  @Test
  void nowNullClock() {
    assertThrows(NullPointerException.class, () -> InternetTime.now(null));
  }

  @Test
  void nowClock() {
    final var real = Instant.now();
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(real);

    it = InternetTime.now(clock);
    verify(clock).instant();

    assertEqualsWithTolerance(real, it.toInstant(), "toInstant()");
    assertEqualsWithTolerance(
        real.truncatedTo(InternetTimeUnit.CENTIBEATS), it, "truncated Instant");

    var expectedODT = OffsetDateTime.ofInstant(real, InternetTime.ZONE);
    var actualODT = it.toOffsetDateTime();
    assertEqualsWithTolerance(expectedODT, actualODT, "result as ODT");

    var expectedZDT = ZonedDateTime.ofInstant(real, InternetTime.ZONE);
    var actualZDT = it.toZonedDateTime();
    assertEqualsWithTolerance(expectedZDT, actualZDT, "result as ZDT");

    var expectedLDT = LocalDateTime.ofInstant(real, InternetTime.ZONE);
    var actualLDT = it.toLocalDateTime();
    assertEqualsWithTolerance(expectedLDT, actualLDT, "result as LDT");
  }

  @Test
  void fromInternetTime() {
    assertSame(it, InternetTime.from(it));
  }

  @Test
  void fromOffsetDateTime() {
    var temporal = OffsetDateTime.now(ZoneId.of("America/New_York"));
    it = InternetTime.from(temporal);
    assertEqualsWithTolerance(temporal.toInstant(), it.toInstant(), "as Instant");
  }

  @Test
  void fromZonedDateTime() {
    var temporal = ZonedDateTime.now(ZoneId.of("America/Chicago"));
    it = InternetTime.from(temporal);
    assertEqualsWithTolerance(temporal.toInstant(), it.toInstant(), "as Instant");
  }

  @Test
  void fromInstant() {
    var temporal = Instant.now();
    it = InternetTime.from(temporal);
    assertEqualsWithTolerance(temporal, it.toInstant(), "as Instant"); // round trip(-ish)
  }

  @Test
  void fromUnsupportedTypes() {
    for (var t : List.of(LocalDateTime.now(), LocalDate.now(), LocalTime.now(), OffsetTime.now())) {
      assertThrows(DateTimeException.class, () -> InternetTime.from(t), t.getClass().getName());
    }
  }

  @Test
  void ofDiscreteValues() {
    int beat = 123;
    int partial = 45;
    var localNow = LocalDate.now();
    var expected = InternetTime.of(localNow, beat, partial, InternetTime.ZONE);

    var actual =
        InternetTime.of(
            localNow.getYear(),
            localNow.getMonth(),
            localNow.getDayOfMonth(),
            beat,
            partial,
            InternetTime.ZONE);
    assertEquals(expected, actual, "Month enum variant");

    actual =
        InternetTime.of(
            localNow.getYear(),
            localNow.getMonth().getValue(),
            localNow.getDayOfMonth(),
            beat,
            partial,
            InternetTime.ZONE);
    assertEquals(expected, actual, "int month value variant");
    assertEquals(beat, actual.getBeat(), "beat value");
    assertEquals(partial, actual.getCentibeatOfBeat(), "centibeat-of-beat value");
  }

  @Test
  void ofLocalDateBeatOffset() {
    var dateTime = LocalDateTime.of(2025, Month.DECEMBER, 30, 19, 21, 42);
    var odt1 = OffsetDateTime.of(dateTime, ZoneOffset.ofHours(-12));
    var odt2 = odt1.atZoneSameInstant(ZoneOffset.ofHours(+4));
    it = InternetTime.of(odt1.toLocalDate(), 999, 99, odt1.getOffset());
    var it2 = InternetTime.of(odt2.toLocalDate(), 999, 99, odt2.getOffset());
    assertEquals(999_99, it.getCentibeatOfDay(), "centibeat of day");
    assertEquals(it, it2);
  }

  @Test
  @SuppressFBWarnings("EC_UNRELATED_TYPES")
  void equalsAndHashCode() {
    var it2 =
        InternetTime.of(it.toLocalDate(), it.getBeat(), it.getCentibeatOfBeat(), InternetTime.ZONE);
    assertEquals(it, it, "same.equals(same)");
    assertEquals(it, it2, "equivalent state");
    assertEquals(it.hashCode(), it2.hashCode(), "equivalent objects -> equal hash codes");

    it2 = it.plus(1, CENTIBEATS);
    assertNotEquals(it, it2, "+1 centibeat");
    assertNotEquals(it.hashCode(), it2.hashCode(), "different centibeat, different hash codes");

    it2 = it.minus(1, CENTIBEATS);
    assertNotEquals(it, it2, "-1 centibeat");

    it2 = it.plus(1, ChronoUnit.DAYS);
    assertNotEquals(it, it2, "+1 day");
    assertNotEquals(it.hashCode(), it2.hashCode(), "different date, different hash codes");

    it2 = it.minus(1, ChronoUnit.DAYS);
    assertNotEquals(it, it2, "-1 day");

    assertFalse(it.equals(null), "equals(null)");
    assertFalse(it.equals(it.toInstant()), "equals(other type)");
  }

  @Test
  void compareTo() {
    var it2 =
        InternetTime.of(it.toLocalDate(), it.getBeat(), it.getCentibeatOfBeat(), InternetTime.ZONE);
    assertEquals(0, it.compareTo(it2), "equals");

    it2 = it.plus(1, CENTIBEATS);
    assertEquals(-1, it.compareTo(it2), "it < it2 (centibeat)");

    it2 = it.minus(1, CENTIBEATS);
    assertEquals(+1, it.compareTo(it2), "it > it2 (centibeat)");

    it2 = it.plus(1, ChronoUnit.DAYS);
    assertEquals(-1, it.compareTo(it2), "it < it2 (day)");

    it2 = it.minus(1, ChronoUnit.DAYS);
    assertEquals(+1, it.compareTo(it2), "it > it2 (day)");
  }

  @Test
  void toStringFormat() {
    it = InternetTime.of(2025, 12, 31, 234, 56, InternetTime.ZONE);
    assertEquals("d31.12.2025 @234.56", it.toString());
  }

  @ParameterizedTest
  @CsvSource({
    "0,   00:00+01:00",
    "1,   00:01:26.400+01:00",
    "10,  00:14:24+01:00",
    "100, 02:24+01:00",
    "499, 11:58:33.600+01:00",
    "500, 12:00+01:00",
    "501, 12:01:26.400+01:00",
    "998, 23:57:07.200+01:00",
    "999, 23:58:33.600+01:00",
  })
  void timeOfBeat(int beat, String expected) {
    OffsetTime time = InternetTime.timeOfBeat(beat);
    assertEquals(InternetTime.ZONE, time.getOffset(), "offset");
    assertEquals(beat, time.get(BEAT_OF_DAY), "beat of day");
    assertEquals(expected, time.toString());
  }

  @ParameterizedTest
  @CsvSource({
    "0,    0, 00:00+01:00",
    "0,    1, 00:00:00.864+01:00",
    "0,   99, 00:01:25.536+01:00",
    "499, 49, 11:59:15.936+01:00",
    "500, 50, 12:00:43.200+01:00",
    "501, 51, 12:02:10.464+01:00",
    "999, 98, 23:59:58.272+01:00",
    "999, 99, 23:59:59.136+01:00",
  })
  void timeOfBeat(int beat, int centibeatOfBeat, String expected) {
    OffsetTime time = InternetTime.timeOfBeat(beat, centibeatOfBeat);
    assertEquals(InternetTime.ZONE, time.getOffset(), "offset");
    assertEquals(centibeatOfBeat + CENTIBEATS_PER_BEAT * beat, time.get(CENTIBEAT_OF_DAY), "centi");
    assertEquals(expected, time.toString());
  }

  @Test
  void toStartOfBeatNullChecks() {
    assertThrows(NullPointerException.class, () -> InternetTime.toStartOf(null, it), "field");
    assertThrows(NullPointerException.class, () -> InternetTime.toStartOf(BEAT_OF_DAY, null), "it");
  }

  @Test
  void toStartOfBeatFromInternetTime() {
    assertSame(it, InternetTime.toStartOf(CENTIBEAT_OF_DAY, it), "always centibeat-aligned");

    var it2 = InternetTime.of(it.toLocalDate(), Math.max(1, it.getBeat()), 0, InternetTime.ZONE);
    assertSame(it2, InternetTime.toStartOf(BEAT_OF_DAY, it2), "any beat without fractional part");

    var it3 = InternetTime.of(it.toLocalDate(), Math.max(1, it.getBeat()), 67, InternetTime.ZONE);
    var it4 = InternetTime.toStartOf(BEAT_OF_DAY, it3);
    assertInstanceOf(InternetTime.class, it4);
    assertEquals(Math.max(1, it.getBeat()), it4.getBeat(), "beat value");
    assertEquals(0, it4.getCentibeatOfBeat(), "centibeat-of-beat value");
    assertEquals(it2, it4, "non-aligned");
  }

  @Test
  void toStartOfBeatFromInstant() {
    var in = InternetTime.toStartOf(CENTIBEAT_OF_DAY, it.toInstant());
    assertInstanceOf(Instant.class, in);
    assertEqualsWithTolerance(CENTIBEATS, it.toInstant(), in, "instant");

    var in2 = InternetTime.toStartOf(BEAT_OF_DAY, it.toInstant());
    assertThat("beat-aligned vs centi-aligned", in2, lessThanOrEqualTo(in));
    assertEqualsWithTolerance(BEATS, in, in2, "delta w/in 1 beat");
  }

  @Test
  void toStartOfBeatFromOffsetDateTime() {
    var odt = InternetTime.toStartOf(CENTIBEAT_OF_DAY, it.toOffsetDateTime());
    assertInstanceOf(OffsetDateTime.class, odt);
    assertEquals(it.toInstant(), odt.toInstant(), "instant exact");
    int beat = odt.get(BEAT_OF_DAY);

    var odt2 = InternetTime.toStartOf(BEAT_OF_DAY, it.toOffsetDateTime());
    assertThat("beat-aligned vs centi-aligned", odt2, lessThanOrEqualTo(odt));
    assertEqualsWithTolerance(BEATS, odt, odt2, "delta w/in 1 beat");
    assertEquals(beat, odt2.get(BEAT_OF_DAY), "beat value");
    assertEquals(0, odt2.get(CENTIBEAT_OF_DAY) % CENTIBEATS_PER_BEAT, "centibeat-of-beat value");
  }

  @Test
  void toStartOfBeatFromUnsupportedTemporals() {
    var values = List.of(LocalDate.now(), YearMonth.now());
    for (var temporal : values) {
      assertThrows(
          UnsupportedTemporalTypeException.class,
          () -> InternetTime.toStartOf(BEAT_OF_DAY, temporal),
          temporal.getClass().getSimpleName());
    }
  }

  @ParameterizedTest
  @CsvSource({
    "0,    0, 00:00",
    "0,    1, 00:00:01",
    "0,   99, 00:01:26",
    "499, 49, 11:59:16",
    "500, 50, 12:00:43",
    "501, 51, 12:02:10",
    "999, 98, 23:59:58",
    "999, 99, 23:59:59",
  })
  void toNearestSecond(int beat, int centibeatOfBeat, String expected) {
    it = InternetTime.of(it.toLocalDate(), beat, centibeatOfBeat, InternetTime.ZONE);
    var out = it.toNearestSecond();
    var ldt = out.toLocalDateTime();
    assertEquals(it.toLocalDate(), ldt.toLocalDate(), "local date");
    assertEquals(expected, ldt.toLocalTime().toString(), "local time");
  }

  @Test
  void isSupportedField() {
    assertFalse(it.isSupported((TemporalField) null), "null");
    for (var itf : InternetTimeField.values()) {
      assertTrue(it.isSupported(itf), itf.name());
    }
    for (var cf : ChronoField.values()) {
      assertTrue(it.isSupported(cf), cf.name());
    }

    var otherType = mock(TemporalField.class);
    when(otherType.isSupportedBy(it)).thenReturn(false);
    assertFalse(it.isSupported(otherType), "other field type");
    verify(otherType).isSupportedBy(it);
  }

  @Test
  void range() {
    assertThrows(NullPointerException.class, () -> it.range(null));
    for (var itf : InternetTimeField.values()) {
      assertEquals(itf.range(), it.range(itf), itf.name());
    }
    var odt = it.toOffsetDateTime();
    for (var cf : ChronoField.values()) {
      assertEquals(odt.range(cf), it.range(cf), cf.name());
    }

    var otherRange = ValueRange.of(-1, +1);
    var otherType = mock(TemporalField.class);
    when(otherType.rangeRefinedBy(it)).thenReturn(otherRange);
    assertEquals(otherRange, it.range(otherType), "other field type");
  }

  @Test
  void query() {
    assertEquals(IsoChronology.INSTANCE, it.query(TemporalQueries.chronology()), "chronology");
    assertEquals(CENTIBEATS, it.query(TemporalQueries.precision()), "precision");
    assertEquals(InternetTime.ZONE, it.query(TemporalQueries.zone()), "zone");
    assertEquals(InternetTime.ZONE, it.query(TemporalQueries.offset()), "offset");
    assertNull(it.query(TemporalQueries.zoneId()), "zoneId");
  }

  @Test
  void getLongUnknownField() {
    var field = mock(TemporalField.class);
    var expected = System.currentTimeMillis();
    when(field.getFrom(it)).thenReturn(expected);
    assertEquals(expected, it.getLong(field));
  }

  @Test
  void getLongInternetTimeFields() {
    assertEquals(it.getBeat(), it.getLong(BEAT_OF_DAY), "beat of day");
    assertEquals(it.getCentibeatOfDay(), it.getLong(CENTIBEAT_OF_DAY), "centibeat of day");
  }

  @Test
  void getLongChronoFields() {
    var date = it.toLocalDate();
    var dateTime = it.toLocalDateTime();
    for (var f : ChronoField.values()) {
      long expected =
          switch (f) {
            case ALIGNED_DAY_OF_WEEK_IN_MONTH, ALIGNED_DAY_OF_WEEK_IN_YEAR, ALIGNED_WEEK_OF_MONTH,
                ALIGNED_WEEK_OF_YEAR, DAY_OF_MONTH, DAY_OF_WEEK, DAY_OF_YEAR, EPOCH_DAY, ERA,
                MONTH_OF_YEAR, PROLEPTIC_MONTH, YEAR, YEAR_OF_ERA -> date.getLong(f);

            case AMPM_OF_DAY, CLOCK_HOUR_OF_AMPM, CLOCK_HOUR_OF_DAY, HOUR_OF_AMPM, HOUR_OF_DAY,
                MICRO_OF_DAY, MICRO_OF_SECOND, MILLI_OF_DAY, MILLI_OF_SECOND, MINUTE_OF_DAY,
                MINUTE_OF_HOUR, NANO_OF_DAY, NANO_OF_SECOND, SECOND_OF_DAY,
                SECOND_OF_MINUTE -> dateTime.getLong(f);

            case INSTANT_SECONDS ->
                date.toEpochSecond(LocalTime.MIDNIGHT, InternetTime.ZONE)
                + it.getLong(ChronoField.SECOND_OF_DAY);

            case OFFSET_SECONDS -> InternetTime.ZONE.getTotalSeconds();
          };
      assertEquals(expected, it.getLong(f), f.name());
    }

    var itMid = InternetTime.of(date, 0, 0, InternetTime.ZONE);
    assertEquals(12L, itMid.getLong(ChronoField.CLOCK_HOUR_OF_AMPM), "midnight AMPM clock hour");
    assertEquals(24L, itMid.getLong(ChronoField.CLOCK_HOUR_OF_DAY), "midnight 24hr clock hour");
  }

  @Test
  void isSupportedUnit() {
    assertFalse(it.isSupported((TemporalUnit) null), "null");
    for (var itu : InternetTimeUnit.values()) {
      assertTrue(it.isSupported(itu), itu.name());
    }
    for (var cu : ChronoUnit.values()) {
      var supported =
          switch (cu) {
            case FOREVER, MICROS, NANOS -> false;
            default -> true;
          };
      assertEquals(supported, it.isSupported(cu), cu.name());
    }

    var unknown = mock(TemporalUnit.class);
    when(unknown.isSupportedBy(it)).thenReturn(true, false);
    assertTrue(it.isSupported(unknown), "unknown unit returns true");
    assertFalse(it.isSupported(unknown), "unknown unit returns false");
    verify(unknown, times(2)).isSupportedBy(it);
  }

  @Test
  void untilValidations() {
    assertThrows(NullPointerException.class, () -> it.until(null, BEATS), "null endExclusive");
    assertThrows(NullPointerException.class, () -> it.until(it, null), "null unit");
    assertThrows(DateTimeException.class, () -> it.until(LocalDate.EPOCH, BEATS), "incompatible");
    assertThrows(
        UnsupportedTemporalTypeException.class,
        () -> it.until(it, ChronoUnit.NANOS),
        "unsupported unit");
  }

  @Test
  void untilUnknownUnit() {
    var unit = mock(TemporalUnit.class);
    var expected = System.currentTimeMillis();
    when(unit.between(eq(it), any())).thenReturn(expected);
    assertEquals(expected, it.until(OffsetDateTime.MAX, unit));
  }

  @Test
  void untilChronoUnit() {
    it = InternetTime.of(LocalDate.EPOCH, 0, 0, InternetTime.ZONE);
    final var odt = it.toOffsetDateTime();

    List<Supplier<Temporal>> factories =
        List.of(OffsetDateTime::now, ZonedDateTime::now, InternetTime::now);
    for (var factory : factories) {
      var end = factory.get();
      for (var unit : ChronoUnit.values()) {
        if (!it.isSupported(unit)) {
          continue;
        }
        assertEquals(
            odt.until(end, unit),
            it.until(end, unit),
            end.getClass().getSimpleName() + "/" + unit);
      }
    }
  }

  @Test
  void withUnknownField() {
    var expected = InternetTime.ofInstant(Instant.EPOCH);
    var field = mock(TemporalField.class);
    long value = System.currentTimeMillis();
    when(field.adjustInto(it, value)).thenReturn(expected);
    assertEquals(expected, it.with(field, value));
  }

  @Test
  void withInternetTimeFields() {
    it = InternetTime.of(it.toLocalDate(), 420, 69, InternetTime.ZONE);
    assertEquals(113, it.with(BEAT_OF_DAY, 113).getBeat(), "beat of day");
    assertEquals(70, it.with(CENTIBEAT_OF_DAY, 70).getCentibeatOfDay(), "centibeat of day");
    assertEquals(70, it.with(CENTIBEAT_OF_BEAT, 70).getCentibeatOfBeat(), "centibeat of beat");
  }

  @Test
  void withChronoFields() {
    var offsetDateTime = it.toOffsetDateTime();
    for (var f : ChronoField.values()) {
      var newValue = 1;
      long expected =
          switch (f) {
            case OFFSET_SECONDS -> 3600;
            case MICRO_OF_DAY -> 0;
            case NANO_OF_SECOND, NANO_OF_DAY, MICRO_OF_SECOND, MILLI_OF_SECOND, MILLI_OF_DAY,
                SECOND_OF_DAY, SECOND_OF_MINUTE, INSTANT_SECONDS ->
                    InternetTime.toStartOf(
                        CENTIBEAT_OF_DAY,
                        offsetDateTime.with(f, newValue)
                    ).getLong(f);
            default -> newValue;
          };
      assertEquals(expected, it.with(f, newValue).getLong(f), f.name());
    }
  }

  @Test
  void plusUnknownUnit() {
    var expected = InternetTime.ofInstant(Instant.EPOCH);
    var unit = mock(TemporalUnit.class);
    long value = System.currentTimeMillis();
    when(unit.addTo(it, value)).thenReturn(expected);
    assertEquals(expected, it.plus(value, unit));
  }

  @Test
  void plusInternetTimeFields() {
    it = InternetTime.of(it.toLocalDate(), 420, 69, InternetTime.ZONE);
    var odt = it.toOffsetDateTime();

    assertEquals(
        InternetTime.from(odt.plus(BEATS.toMillis(13), ChronoUnit.MILLIS)),
        it.plus(13, BEATS),
        "+13 beats");

    assertEquals(
        InternetTime.from(odt.plus(CENTIBEATS.toMillis(-13), ChronoUnit.MILLIS)),
        it.minus(13, CENTIBEATS),
        "-13 centibeats");
  }

  @Test
  void plusChronoFields() {
    var offsetDateTime = it.toOffsetDateTime();
    for (var unit : ChronoUnit.values()) {
      long adder = 1;
      InternetTime expected =
          switch (unit) {
            case ERAS -> {
              adder = -1; // valid values = 0 or 1
              yield InternetTime.from(offsetDateTime.plus(adder, unit));
            }
            case FOREVER, NANOS, MICROS -> {
                assertThrows(
                    UnsupportedTemporalTypeException.class,
                    () -> it.plus(0, unit),
                    unit.name());
                yield null;
            }
            default -> InternetTime.from(offsetDateTime.plus(adder, unit));
          };
      if (null != expected) {
        assertEquals(expected, it.plus(adder, unit), unit.name());
      }
    }
  }

  @Test
  void staticFormatters() {
    it = it.with(CENTIBEAT_OF_DAY, 234_67);
    var localDateStr = it.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
    var cases =
        Map.of(
            InternetTime.LOCAL_DATE_BEATS, localDateStr + " @234",
            InternetTime.LOCAL_DATE_CENTIBEATS, localDateStr + " @234.67",
            InternetTime.OFFSET_DATE_BEATS, localDateStr + "+01:00 @234",
            InternetTime.OFFSET_DATE_CENTIBEATS, localDateStr + "+01:00 @234.67");
    cases.forEach(
        (fmt, expected) -> {
          assertEquals(expected, fmt.format(it), fmt + " fmt.format(it)");
          assertEquals(expected, it.format(fmt), fmt + " it.format(fmt)");
        });
  }

  @ParameterizedTest
  @CsvSource({
    "SHORT,  234",
    "MEDIUM, @234",
    "LONG,   234.67",
    "FULL,   @234.67",
  })
  void beatFormatter(FormatStyle style, String expected) {
    it = it.with(CENTIBEAT_OF_DAY, 234_67);
    var fmt = InternetTime.beatFormatter(style);
    assertEquals(expected, fmt.format(it), "formatter.format(it)");
    assertEquals(expected, it.format(fmt), "it.format(formatter)");
  }

  @Test
  void beatFormatterNullStyle() {
    assertThrows(NullPointerException.class, () -> InternetTime.beatFormatter(null));
  }

  @Test
  void formatNull() {
    assertThrows(NullPointerException.class, () -> it.format(null));
  }

  @Test
  void parseNullFormatter() {
    assertThrows(NullPointerException.class, () -> InternetTime.parse("", null));
  }

  @Test
  void parseNullText() {
    var parser = InternetTime.OFFSET_DATE_BEATS;
    assertThrows(NullPointerException.class, () -> InternetTime.parse(null, parser));
  }

  @ParameterizedTest
  @CsvSource({
    "LOCAL_DATE_BEATS,       2026-01-02 @345,          2026-01-02, 345, 0",
    "LOCAL_DATE_CENTIBEATS,  2026-01-02 @345.78,       2026-01-02, 345, 78",
    "LOCAL_DATE_CENTIBEATS,  2026-01-02 @345,          2026-01-02, 345, 0", // optional fraction
    "OFFSET_DATE_BEATS,      2026-01-02+01:00 @345,    2026-01-02, 345, 0",
    "OFFSET_DATE_CENTIBEATS, 2026-01-02+01:00 @345.78, 2026-01-02, 345, 78",
    "OFFSET_DATE_CENTIBEATS, 2026-01-02+01:00 @345,    2026-01-02, 345, 0", // optional fraction
    "OFFSET_DATE_BEATS,      2026-01-02+02:00 @345,    2026-01-02, 345, 0",
    "OFFSET_DATE_CENTIBEATS, 2026-01-02+02:00 @345.78, 2026-01-02, 345, 78",
    "OFFSET_DATE_BEATS,      2026-01-02+00:00 @345,    2026-01-02, 345, 0",
    "OFFSET_DATE_CENTIBEATS, 2026-01-02+00:00 @345.78, 2026-01-02, 345, 78",
    "OFFSET_DATE_BEATS,      2026-01-02-05:00 @345,    2026-01-02, 345, 0",
    "OFFSET_DATE_CENTIBEATS, 2026-01-02-05:00 @345.78, 2026-01-02, 345, 78",
    "OFFSET_DATE_BEATS,      2026-01-02-12:00 @345,    2026-01-03, 345, 0",
    "OFFSET_DATE_CENTIBEATS, 2026-01-02-12:00 @345.23, 2026-01-03, 345, 23",
    "OFFSET_DATE_BEATS,      2026-01-02+12:00 @789,    2026-01-01, 789, 0",
    "OFFSET_DATE_CENTIBEATS, 2026-01-02+12:00 @789.23, 2026-01-01, 789, 23",
  })
  void parseStaticFormats(
      String fmtName, String input, LocalDate expectDateUtc1, int expectBeats, int expectCenti) {
    var fmt = Map.of(
        "LOCAL_DATE_BEATS", InternetTime.LOCAL_DATE_BEATS,
        "LOCAL_DATE_CENTIBEATS", InternetTime.LOCAL_DATE_CENTIBEATS,
        "OFFSET_DATE_BEATS", InternetTime.OFFSET_DATE_BEATS,
        "OFFSET_DATE_CENTIBEATS", InternetTime.OFFSET_DATE_CENTIBEATS
      ).get(fmtName);
    var expect = InternetTime.of(expectDateUtc1, expectBeats, expectCenti, InternetTime.ZONE);
    assertEquals(expect, InternetTime.parse(input, fmt), "IT.parse(str, fmt)");
    assertEquals(expect, fmt.parse(input, InternetTime::from), "fmt.parse(str, IT::from)");
  }

  @ParameterizedTest
  @CsvSource({
    "SHORT,      123, 123,  0",
    "SHORT,      456, 456,  0",
    "MEDIUM,    @123, 123,  0",
    "MEDIUM,    @456, 456,  0",
    "LONG,       123, 123,  0",
    "LONG,       456, 456,  0",
    "LONG,    123.98, 123, 98",
    "LONG,    456.12, 456, 12",
    "FULL,   @123.98, 123, 98",
    "FULL,   @456.12, 456, 12",
    "FULL,      @123, 123,  0",
    "FULL,      @456, 456,  0",
  })
  void parseBeatFormats(
      FormatStyle style, String input, int expectBeats, int expectCenti) {
    var fmt = InternetTime.beatFormatter(style);
    var expect = InternetTime.of(LocalDate.EPOCH, expectBeats, expectCenti, InternetTime.ZONE);
    assertEquals(expect, InternetTime.parse(input, fmt), "IT.parse(str, fmt)");
    assertEquals(expect, fmt.parse(input, InternetTime::from), "fmt.parse(str, IT::from)");
  }

  @Test
  void parseStandardFormats() {
    var str = "2026-01-03T12:34:56.7Z";
    it = InternetTime.of(LocalDate.of(2026, 01, 03), 565, 93, InternetTime.ZONE);
    assertEquals(it, InternetTime.parse(str, DateTimeFormatter.ISO_INSTANT), "ISO_INSTANT");
    assertEquals(it, InternetTime.parse(str, DateTimeFormatter.ISO_DATE_TIME), "ISO_DATE_TIME");
    assertEquals(
        it.minus(1, ChronoUnit.HOURS),
        InternetTime.parse(
            str.replace("Z", ""), DateTimeFormatter.ISO_LOCAL_DATE_TIME), "ISO_LOCAL_DATE_TIME");
    assertEquals(
        it.plus(4167, CENTIBEATS),
        InternetTime.parse(
            str.replace("Z", "-01:00"),
            DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        "ISO_OFFSET_DATE_TIME");

    str = "12:34:56.7-00:00";
    it = InternetTime.of(LocalDate.EPOCH, 565, 93, InternetTime.ZONE);
    assertEquals(it, InternetTime.parse(str, DateTimeFormatter.ISO_OFFSET_TIME), "ISO_OFFSET_TIME");
    assertEquals(
        it.minus(1, ChronoUnit.HOURS),
        InternetTime.parse(str.replace("-00:00", ""), DateTimeFormatter.ISO_TIME), "ISO_TIME");

    assertThrows(
        DateTimeException.class,
        () -> InternetTime.parse(
            "2026-01-03-06:00", DateTimeFormatter.ISO_OFFSET_DATE), "ISO_OFFSET_DATE");
    assertThrows(
        DateTimeException.class,
        () -> InternetTime.parse("2026-01-03", DateTimeFormatter.ISO_DATE), "ISO_DATE");
  }
}
