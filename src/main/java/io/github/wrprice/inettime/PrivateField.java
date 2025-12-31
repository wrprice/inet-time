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
import static java.util.Objects.requireNonNull;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.time.temporal.ValueRange;

/**
 *
 * @author wrprice
 */
enum PrivateField implements TemporalField {

  CENTIBEAT_OF_BEAT("CentibeatOfBeat", CENTIBEATS, BEATS, 99);

  private final String name;
  private final InternetTimeUnit unit;
  private final TemporalUnit rangeUnit;
  private final ValueRange range;

  private PrivateField(
      String name, InternetTimeUnit unit, TemporalUnit rangeUnit, int rangeMax) {
    this.name = name;
    this.unit = unit;
    this.rangeUnit = rangeUnit;
    this.range = ValueRange.of(0, rangeMax);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public TemporalUnit getBaseUnit() {
    return unit;
  }

  @Override
  public TemporalUnit getRangeUnit() {
    return rangeUnit;
  }

  @Override
  public ValueRange range() {
    return range;
  }

  @Override
  public boolean isDateBased() {
    return false;
  }

  @Override
  public boolean isTimeBased() {
    return false;
  }

  @Override
  public boolean isSupportedBy(TemporalAccessor ta) {
    return false;
  }

  @Override
  public ValueRange rangeRefinedBy(TemporalAccessor ta) {
    requireNonNull(ta);
    return range();
  }

  @Override
  public long getFrom(TemporalAccessor ta) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <R extends Temporal> R adjustInto(R r, long l) {
    throw new UnsupportedOperationException();
  }
}
