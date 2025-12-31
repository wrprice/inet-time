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
