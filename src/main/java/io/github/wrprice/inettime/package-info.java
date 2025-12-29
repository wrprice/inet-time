/// Classes implementing the late 1990s *Internet Time* standard introduced by Swatch&reg; Ltd.
/// in 1998, and interoperable with the modern [*Java Time API*][java.time].
///
/// In *Internet Time*, a day is measured by 1,000 *decimal minutes* numbered `000` to `999`,
/// inclusive, and normally written with a leading `@` symbol.  Each increment is called a
/// <abbr title="dot-beat?">**.beat**</abbr> and lasts *86.4 seconds*.  A *.beat* can be subdivided
/// into 100 *centibeats*, each lasting a little less than one second, notated by a decimal point
/// and two additional digits.
///
/// *Internet Time* does not recognize time zones nor Daylight Saving Time; the month, day, year,
/// and *.beat* value for a particular instant in one place is the same value on the other side of
/// the world at that exact same moment.  This was desired for a shared time representation on the
/// fledgling Internet where users interact with others across several time zones at once; a note
/// to meet at *.beat* `@432` would be unambiguous to all parties.  With traditional ISO-8601
/// notations, a time like `13:52` is insufficient without additional information about the intended
/// time zone and leaves individuals with the job of applying conversions.
///
/// The standard met its design goals, and Swatch sold digital wristwatches with *.beat* support
/// until 2016.  Despite its marketing power and inclusion in a few video games and mobile phones,
/// its widespread adoption was doomed to fail.
///
/// **Additional information:**
///
/// * [Wikipedia entry](https://en.wikipedia.org/wiki/Swatch_Internet_Time)
/// * [Official Swatch&reg; site](https://www.swatch.com/en-us/internet-time.html)
/// * [Unofficial wiki and live demo](https://beats.wiki/)
/// * [Summary on timeanddate.com](https://www.timeanddate.com/time/internettime.html)
///
/// ## Package specification:
///
/// Unless otherwise noted, passing a null argument to a constructor or method in any class or
/// interface in this package will cause a [NullPointerException] to be thrown. The Javadoc "@param"
/// definition is used to note particular details or exceptions to the null-behavior. The Javadoc
/// does not explicitly document "@throws NullPointerException" on methods.
package io.github.wrprice.inettime;
