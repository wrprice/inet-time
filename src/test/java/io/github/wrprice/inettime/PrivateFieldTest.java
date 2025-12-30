package io.github.wrprice.inettime;

import static io.github.wrprice.inettime.InternetTimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ValueRange;
import org.junit.jupiter.api.Test;

/// This test suite mostly exists for the benefit of the code coverage metrics. :-)
/// @author William R. Price
public class PrivateFieldTest {
  private static final PrivateField pf = PrivateField.CENTIBEAT_OF_BEAT;

  @Test
  void toStringFormat() {
    assertEquals("CentibeatOfBeat", pf.toString());
  }

  @Test
  void baseUnit() {
    assertEquals(CENTIBEATS, pf.getBaseUnit());
  }

  @Test
  void rangeUnit() {
    assertEquals(BEATS, pf.getRangeUnit());
  }

  @Test
  void range() {
    assertEquals(ValueRange.of(0, 99), pf.range());
  }

  @Test
  void rangeRefinedBy() {
    assertThrows(NullPointerException.class, () -> pf.rangeRefinedBy(null));
    assertEquals(ValueRange.of(0, 99), pf.rangeRefinedBy(Instant.now()));
  }

  @Test
  void isDateTimeBased() {
    assertFalse(pf.isDateBased(), "date");
    assertFalse(pf.isTimeBased(), "time");
  }

  @Test
  void unimplemented() {
    assertThrows(
        UnsupportedOperationException.class, () -> pf.getFrom(Instant.now()), "getFrom");
    assertThrows(
        UnsupportedOperationException.class, () -> pf.adjustInto(Instant.now(), 0), "adjustInto");
  }
}
