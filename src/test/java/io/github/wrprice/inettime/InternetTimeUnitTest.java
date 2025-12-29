package io.github.wrprice.inettime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// @author William R. Price
public class InternetTimeUnitTest {
  private static final Set<InternetTimeUnit> UNITS = EnumSet.allOf(InternetTimeUnit.class);

  @Test
  void toMillis() {
    assertEquals(86400, InternetTimeUnit.BEATS.toMillis(1), "1 beat");
    assertEquals(864, InternetTimeUnit.CENTIBEATS.toMillis(1), "1 centibeat");
  }

  @ParameterizedTest
  @CsvSource({
    "BEATS, 0, 0",
    "BEATS, 1, 0",
    "BEATS, 86399, 0",
    "BEATS, 86400, 1",
    "BEATS, 86401, 1",
    "BEATS, 864000, 10",
    "BEATS, 86400000, 1000",
    "CENTIBEATS, 0, 0",
    "CENTIBEATS, 1, 0",
    "CENTIBEATS, 863, 0",
    "CENTIBEATS, 864, 1",
    "CENTIBEATS, 865, 1",
    "CENTIBEATS, 86399, 99",
    "CENTIBEATS, 86400, 100",
  })
  void fromMillis(InternetTimeUnit unit, long millis, long expectedUnits) {
    assertEquals(expectedUnits, unit.fromMillis(millis));
  }

  @Test
  void invariants() {
    assertInvariant(nil -> InternetTimeUnit.CENTIBEATS_PER_BEAT, 100, "CENTIBEATS_PER_BEAT");
    assertInvariant(InternetTimeUnit::isDurationEstimated, false, "isDurationEstimated");
    assertInvariant(InternetTimeUnit::isDateBased, false, "isDateBased");
    assertInvariant(InternetTimeUnit::isTimeBased, true, "isTimeBased");
    assertComputedInvariant(
        InternetTimeUnit::name, u -> u.toString().toUpperCase(), "name == upper(toString())");
    assertComputedInvariant(
        InternetTimeUnit::getDuration,
        u -> Duration.ofMillis(u.toMillis(1)),
        "getDuration / toMillis(1)");
    assertInvariant(u -> u.fromMillis(u.toMillis(3)), 3L, "fromMillis(toMillis(...))");
  }

  private <V> void assertInvariant(
      Function<InternetTimeUnit, ? super V> accessor, V expected, String what) {
    assertComputedInvariant(accessor, __ -> expected, what);
  }

  private <V> void assertComputedInvariant(
      Function<InternetTimeUnit, ? super V> accessor,
      Function<InternetTimeUnit, V> expectedGenerator,
      String what) {
    for (var unit : UNITS) {
      assertEquals(expectedGenerator.apply(unit), accessor.apply(unit), unit + ": " + what);
    }
  }

  @Test
  void toStringValue() {
    assertEquals("Beats", InternetTimeUnit.BEATS.toString());
    assertEquals("Centibeats", InternetTimeUnit.CENTIBEATS.toString());
  }

  @Test
  void isSupportedBy() {
    assertInvariant(u -> u.isSupportedBy(null), false, "null");
    assertInvariant(u -> u.isSupportedBy(LocalDate.EPOCH), false, "LocalDate");
    assertInvariant(u -> u.isSupportedBy(LocalTime.MIDNIGHT), true, "LocalTime");
    assertInvariant(
        u -> u.isSupportedBy(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC)),
        true,
        "LocalDateTime");
    assertInvariant(u -> u.isSupportedBy(Instant.EPOCH), true, "Instant");
    assertInvariant(
        u -> u.isSupportedBy(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)),
        true,
        "OffsetDateTime");
    assertInvariant(
        u -> u.isSupportedBy(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)),
        true,
        "ZonedDateTime");
    assertInvariant(
        u -> u.isSupportedBy(InternetTime.ofInstant(Instant.EPOCH)), true, "InternetTime");
  }

  @Test
  void addTo() {
    var temp = mock(Temporal.class);
    when(temp.isSupported(ChronoUnit.MILLIS)).thenReturn(true);

    long amount = 42;
    InternetTimeUnit.BEATS.addTo(temp, amount);
    InternetTimeUnit.CENTIBEATS.addTo(temp, amount);

    var ordered = inOrder(temp);
    ordered.verify(temp).plus(InternetTimeUnit.BEATS.toMillis(amount), ChronoUnit.MILLIS);
    ordered.verify(temp).plus(InternetTimeUnit.CENTIBEATS.toMillis(amount), ChronoUnit.MILLIS);
  }

  @Test
  void addToIncompatible() {
    var bad = LocalDate.EPOCH;
    for (var unit : UNITS) {
      assertThrows(UnsupportedTemporalTypeException.class, () -> unit.addTo(bad, 1), unit.name());
    }
  }

  @Test
  void between() {
    var start = LocalTime.MIDNIGHT;
    var end = LocalTime.NOON;
    assertEquals(0, InternetTimeUnit.BEATS.between(end, end), "BEATS");
    assertEquals(0, InternetTimeUnit.CENTIBEATS.between(end, end), "CENTIBEATS");
    assertEquals(500, InternetTimeUnit.BEATS.between(start, end), "BEATS");
    assertEquals(500_00, InternetTimeUnit.CENTIBEATS.between(start, end), "CENTIBEATS");
  }

  @Test
  void betweenIncompatible() {
    var bad = LocalDate.EPOCH;
    var good = LocalTime.MIDNIGHT;
    for (var unit : UNITS) {
      assertThrows(
          UnsupportedTemporalTypeException.class, () -> unit.between(good, bad), unit.name());
      assertThrows(
          UnsupportedTemporalTypeException.class, () -> unit.between(bad, good), unit.name());
    }
  }
}
