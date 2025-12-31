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

import static io.github.wrprice.inettime.InternetTimeField.BEAT_OF_DAY;
import static io.github.wrprice.inettime.InternetTimeField.CENTIBEAT_OF_DAY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.time.temporal.*;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// @author William R. Price
public class InternetTimeFieldTest {
  private static final Set<InternetTimeField> FIELDS = EnumSet.allOf(InternetTimeField.class);

  @Test
  void invariants() {
    assertInvariant(InternetTimeField::getRangeUnit, ChronoUnit.DAYS, "getRangeUnit");
    assertInvariant(InternetTimeField::isDateBased, false, "isDateBased");
    assertInvariant(InternetTimeField::isTimeBased, true, "isTimeBased");
    assertComputedInvariant(
        InternetTimeField::toString, f -> f.getDisplayName(Locale.ROOT), "name == getDisplayName");
  }

  private <V> void assertInvariant(
      Function<InternetTimeField, ? super V> accessor, V expected, String what) {
    assertComputedInvariant(accessor, __ -> expected, what);
  }

  private <V> void assertComputedInvariant(
      Function<InternetTimeField, ? super V> accessor,
      Function<InternetTimeField, V> expectedGenerator,
      String what) {
    for (var field : FIELDS) {
      assertEquals(expectedGenerator.apply(field), accessor.apply(field), field + ": " + what);
    }
  }

  @Test
  void toStringValue() {
    for (var field : FIELDS) {
      var expected = switch(field) {
        case BEAT_OF_DAY -> "BeatOfDay";
        case CENTIBEAT_OF_DAY -> "CentibeatOfDay";
      };
      assertEquals(expected, field.toString(), field.name());
    }
  }

  @Test
  void getDisplayNameNullLocale() {
    for (var field : FIELDS) {
      assertThrows(NullPointerException.class, () -> field.getDisplayName(null), field.name());
    }
  }

  @Test
  void getBaseUnit() {
    for (var field : FIELDS) {
      var expected = switch(field) {
        case BEAT_OF_DAY -> InternetTimeUnit.BEATS;
        case CENTIBEAT_OF_DAY -> InternetTimeUnit.CENTIBEATS;
      };
      assertEquals(expected, field.getBaseUnit(), field.name());
    }
  }

  @Test
  void range() {
    for (var field : FIELDS) {
      var expected = switch(field) {
        case BEAT_OF_DAY -> ValueRange.of(0, 999);
        case CENTIBEAT_OF_DAY -> ValueRange.of(0, 999_99);
      };
      assertEquals(expected, field.range(), field.name());
    }
  }

  @Test
  void isSupportedByInternetTime() {
    var it = InternetTime.now();
    for (var field : FIELDS) {
      assertTrue(field.isSupportedBy(it), field.name());
    }
  }

  @Test
  void isSupportedBy() {
    assertInvariant(f -> f.isSupportedBy(null), false, "null");
    assertInvariant(f -> f.isSupportedBy(LocalDate.EPOCH), false, "LocalDate");
    assertInvariant(f -> f.isSupportedBy(LocalTime.MIDNIGHT), false, "LocalTime");
    assertInvariant(
        f -> f.isSupportedBy(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)),
        false,
        "LocalDateTime");
    assertInvariant(u -> u.isSupportedBy(Instant.EPOCH), true, "Instant");
    assertInvariant(
        f -> f.isSupportedBy(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)),
        true,
        "OffsetDateTime");
    assertInvariant(
        f -> f.isSupportedBy(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)),
        true,
        "ZonedDateTime");
    assertInvariant(
        f -> f.isSupportedBy(InternetTime.ofInstant(Instant.EPOCH)), true, "InternetTime");
  }

  @Test
  void isSupportedByNotSupported() {
    assertUnsupportedTemporalAccessors(InternetTimeField::isSupportedBy, false);
  }

  private void assertUnsupportedTemporalAccessors(
      BiFunction<InternetTimeField, ? super Temporal, ?> action) {
    assertUnsupportedTemporalAccessors(action, null);
  }

  private void assertUnsupportedTemporalAccessors(
      BiFunction<InternetTimeField, ? super Temporal, ?> action, Object expectedInsteadOfException) {
    var temporal = mock(Temporal.class);
    for (var field : FIELDS) {
      final Consumer<String> test;
      if (null == expectedInsteadOfException) {
        assertThrows(
            NullPointerException.class, () -> action.apply(field, null), field + " - null");
        final var expected = UnsupportedTemporalTypeException.class;
        test =
            txt -> assertThrows(expected, () -> action.apply(field, temporal), field + " - " + txt);
      } else {
        assertEquals(
            expectedInsteadOfException, action.apply(field, null), field + " - null");
        test =
            txt -> assertEquals(
                expectedInsteadOfException, action.apply(field, temporal), field + " - " + txt);
      }

      when(temporal.isSupported(ChronoField.MILLI_OF_DAY)).thenReturn(true);
      when(temporal.isSupported(ChronoField.OFFSET_SECONDS)).thenReturn(false);
      test.accept("MILLI but not OFFSET");

      when(temporal.isSupported(ChronoField.MILLI_OF_DAY)).thenReturn(false);
      when(temporal.isSupported(ChronoField.OFFSET_SECONDS)).thenReturn(true);
      test.accept("OFFSET but not MILLI");

      when(temporal.isSupported(ChronoField.MILLI_OF_DAY)).thenReturn(false);
      when(temporal.isSupported(ChronoField.OFFSET_SECONDS)).thenReturn(false);
      test.accept("neither MILLI nor OFFSET");
    }
  }

  @Test
  void rangeRefinedBy() {
    var temp = mock(Temporal.class);
    when(temp.isSupported(ChronoField.MILLI_OF_DAY)).thenReturn(true);
    when(temp.isSupported(ChronoField.OFFSET_SECONDS)).thenReturn(true);

    var it = InternetTime.now();

    for (var field : FIELDS) {
      var expected = field.range();
      assertEquals(expected, field.rangeRefinedBy(temp), field.name() + " - Temporal");
      assertEquals(expected, field.rangeRefinedBy(it), field.name() + " - InternetTime");
    }
  }

  @Test
  void rangeRefinedByNotSupported() {
    assertUnsupportedTemporalAccessors(InternetTimeField::rangeRefinedBy);
  }

  @Test
  void getFromInternetTime() {
    var it = InternetTime.now();
    for (var field : FIELDS) {
      var expected = switch (field) {
        case BEAT_OF_DAY -> it.getBeat();
        case CENTIBEAT_OF_DAY -> it.getCentibeatOfDay();
      };
      assertEquals(expected, field.getFrom(it), field.name());
    }
  }

  @Test
  void getFromTemporal() {
    long msOfDay = 864 * 12_345L;
    long utcOffsetSec = -6 * 3600L;
    long expectedNetMillisOfDay = (msOfDay - ((utcOffsetSec - 3600) * 1000));
    var temp = makeMockTemporal(msOfDay, utcOffsetSec);
    for (var field : FIELDS) {
      var expected = switch (field) {
        case BEAT_OF_DAY -> expectedNetMillisOfDay / 86400;
        case CENTIBEAT_OF_DAY -> expectedNetMillisOfDay / 864;
      };
      assertEquals(expected, field.getFrom(temp), field.name());
    }
  }

  private Temporal makeMockTemporal(long milliOfDay, long offsetSec) {
    var temporal = mock(Temporal.class);
    when(temporal.isSupported(ChronoField.MILLI_OF_DAY)).thenReturn(true);
    when(temporal.isSupported(ChronoField.OFFSET_SECONDS)).thenReturn(true);
    when(temporal.getLong(ChronoField.MILLI_OF_DAY)).thenReturn(milliOfDay);
    when(temporal.getLong(ChronoField.OFFSET_SECONDS)).thenReturn(offsetSec);
    return temporal;
  }

  @Test
  void getFromNotSupported() {
    assertUnsupportedTemporalAccessors(InternetTimeField::getFrom);
  }

  @Test
  void adjustIntoOutOfRange() {
    var mock = makeMockTemporal(0, 0);
    for (var field : FIELDS) {
      var min = field.range().getMinimum();
      var max = field.range().getMaximum();
      var tooSmall = min - 1;
      var tooLarge = max + 1;

      assertThrows(
          DateTimeException.class, () -> field.adjustInto(mock, tooSmall), field + " too small");
      assertThrows(
          DateTimeException.class, () -> field.adjustInto(mock, tooLarge), field + " too large");
      assertDoesNotThrow(() -> field.adjustInto(mock, min), field + " min");
      assertDoesNotThrow(() -> field.adjustInto(mock, max), field + " max");
    }
  }

  @Test
  void adjustInto() {
    var it = InternetTime.now();
    var odt = it.toOffsetDateTime().withOffsetSameInstant(ZoneOffset.ofHours(-6));
    if (it.getBeat() == 123) {
      it = it.plus(1, ChronoUnit.HOURS);
      odt = odt.plus(1, ChronoUnit.HOURS);
    }
    assertEquals(odt.toInstant(), it.toInstant(), "pre-requisite");

    for (var field : FIELDS) {
      var toSet = switch (field) {
        case BEAT_OF_DAY -> 123;
        case CENTIBEAT_OF_DAY -> 123_45;
      };
      var newIt = field.adjustInto(it, toSet);
      var newOdt = field.adjustInto(odt, toSet);
      assertInstanceOf(InternetTime.class, newIt);
      assertInstanceOf(OffsetDateTime.class, newOdt);
      assertNotSame(it, newIt);
      assertNotSame(odt, newOdt);
      assertEquals(toSet, newIt.get(field), field + " InternetTime");
      assertEquals(toSet, newOdt.get(field), field + " OffsetDateTime");
      assertEquals(it.toLocalDate(), newIt.toLocalDate(), field + " InternetTime -> LocalDate");
      assertEquals(odt.toLocalDate(), newOdt.toLocalDate(), field + " OffsetDateTime -> LocalDate");
      assertEquals(
          newOdt.withOffsetSameInstant(ZoneOffset.UTC).toLocalTime(),
          newIt.toLocalDateTime().minusHours(1).toLocalTime(),
          field + " (ODT vs IT) in UTC local time (ignoring date)");
    }
  }

  @Test
  void adjustIntoNotSupported() {
    assertUnsupportedTemporalAccessors((f, t) -> f.adjustInto(t, 0));
  }

  @ParameterizedTest
  @CsvSource({
    "-172800001, 86399999",
    "-172800000, 0",
    "-86400001, 86399999",
    "-86400000, 0",
    "-86399999, 1",
    "-1, 86399999",
    "0, 0",
    "1, 1",
    "86399999, 86399999",
    "86400000, 0",
    "172800000, 0",
    "172800001, 1",
  })
  void wrapMilliOfDay(long in, long expected) {
    assertEquals(expected, InternetTimeField.wrapMilliOfDay(in));
  }
}
