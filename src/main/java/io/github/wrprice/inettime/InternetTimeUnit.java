package io.github.wrprice.inettime;

import static java.time.temporal.ChronoUnit.MILLIS;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.Locale;

/// Time units distinct to *Swatch&reg; Internet Time* (".beats").
///
/// **Implementation notes:**  
/// This is a final, immutable and thread-safe `enum`.
///
/// @author William R. Price
public enum InternetTimeUnit implements TemporalUnit {

  /// A **.beat** is equivalent to <!-- one one-thousandth -->
  /// <math><mfrac><mn>1</mn><mn>1,000</mn></mfrac></math> of a nominal 24-hour day,
  /// or 86.4 seconds.
  ///
  /// Allowed values range from 0 to 999, inclusive, and is often notated as a zero-padded value
  /// with a leading `@` symbol, as in: `@012`.
  BEATS(Duration.ofSeconds(86).plusMillis(400)),

  /// A "centibeat" is <!-- one one-hundredth -->
  ///<math><mfrac><mn>1</mn><mn>100</mn></mfrac></math> of a [.beat][#BEATS],
  /// or 864 milliseconds.
  ///
  /// Allowed values range from 0 to 99,999 inclusive, and is often notated as `@012.34`.
  CENTIBEATS(Duration.ofMillis(864)),

  ; // end of enum instances

  static final int CENTIBEATS_PER_BEAT = (int) (BEATS.toMillis(1) / CENTIBEATS.toMillis(1));

  private final String name;
  private final Duration duration;
  private final int durationAsMillis;

  private InternetTimeUnit(Duration duration) {
    this.duration = duration;
    this.durationAsMillis = (int) duration.toMillis();
    this.name = name().charAt(0) + name().substring(1).toLowerCase(Locale.US);
  }

  @Override
  public String toString() {
    return name;
  }

  long fromMillis(long millis) {
    return Math.floorDiv(millis, durationAsMillis);
  }

  long toMillis(long unitValue) {
    return Math.multiplyExact(unitValue, durationAsMillis);
  }

  @Override
  public Duration getDuration() {
    return duration;
  }

  /// {@return `false`} The duration of these units is *exact*.
  @Override
  public boolean isDurationEstimated() {
    return false;
  }

  /// {@return `false`}  The *Internet Time*-specific units are only concerned with the time within
  /// a single day.
  @Override
  public boolean isDateBased() {
    return false;
  }

  /// {@return `true`}  *Internet Time* units evenly divide a single day into 1,000 or 100,000
  /// decimal subperiods.
  @Override
  public boolean isTimeBased() {
    return true;
  }

  @Override
  public boolean isSupportedBy(Temporal temporal) {
    return temporal != null && temporal.isSupported(MILLIS);
  }

  private void throwIfIncompatible(Temporal t) {
    if (!isSupportedBy(t)) {
      throw new UnsupportedTemporalTypeException(t.getClass() + " does not support " + this);
    }
  }

  @Override
  public <R extends Temporal> R addTo(R temporal, long amount) {
    throwIfIncompatible(temporal);
    return MILLIS.addTo(temporal, toMillis(amount));
  }

  @Override
  public long between(Temporal t1Inclusive, Temporal t2Exclusive) {
    throwIfIncompatible(t1Inclusive);
    throwIfIncompatible(t2Exclusive);
    return fromMillis(t1Inclusive.until(t2Exclusive, MILLIS));
  }
}
