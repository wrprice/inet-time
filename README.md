# inet-time for Java

A small, dependency-free Java module/library implementing the late 1990s *Internet Time* standard
introduced by Swatch&reg; Ltd. in 1998, interoperable with the modern *Java Time API* (`java.time`).


## Requirements

1. Java 21 or newer at runtime<sup>\*</sup>
2. A reason to use this retro time standard in your project

<sup>\*</sup> Note: the build environment uses Java 25 for supporting Markdown-based Javadoc


## About *Internet Time*

In *Internet Time*, a day is measured by 1,000 *decimal minutes* numbered `000` to `999`,
inclusive, and normally written with a leading `@` symbol.  Each increment is called a
<abbr title="dot-beat?">**.beat**</abbr> and lasts *86.4 seconds*.  A *.beat* can be subdivided
into 100 *centibeats*, each lasting a little less than one second, notated by a decimal point
and two additional digits.

*Internet Time* does not recognize time zones nor Daylight Saving Time; the month, day, year,
and *.beat* value for a particular instant in one place is the same value on the other side of
the world at that exact same moment.  This was desired for a shared time representation on the
fledgling Internet where users interact with others across several time zones at once; a note
to meet at *.beat* `@432` would be unambiguous to all parties.  With traditional ISO-8601
notations, a time like `13:52` is insufficient without additional information about the intended
time zone and leaves individuals with the job of applying conversions.

The standard met its design goals, and Swatch sold digital wristwatches with *.beat* support
until 2016.  Despite its marketing power and inclusion in a few video games and mobile phones,
its widespread adoption was doomed to fail.

**Additional information:**

* [Wikipedia entry](https://en.wikipedia.org/wiki/Swatch_Internet_Time)
* [Official Swatch&reg; site](https://www.swatch.com/en-us/internet-time.html)
* [Unofficial wiki and live demo](https://beats.wiki/)
* [Summary on timeanddate.com](https://www.timeanddate.com/time/internettime.html)

## How to Use

This library supports the Java module system and exports its public classes from a single package
named the same as the module itself: `io.github.wrprice.inettime`.  If your project is *not*
modularized, you can instead place the library directly in the legacy classpath.

**value-based class `InternetTime`**  
Acts as a hybrid of `Instant` and an `OffsetDateTime`, representing a specific moment globally,
with a maximum resolution of one *centibeat* (<math><mfrac><mn>1</mn><mn>100,000</mn></mfrac></math>
of a day), or 864ms. This is the primary API for most application development, and can be used
where `Temporal` values are used in your other application code.  It provides factory methods and
convenient conversion operations to other Java Time API classes.

**enum `InternetTimeField`**  
An implementation of `TemporalField` supporting time-of-day fields specific to *Internet Time*,
specifically `BEAT_OF_DAY` and `CENTIBEAT_OF_DAY`.  These fields can be used with a subset of
other `Temporal` and `TemporalAccessor` types to retrieve or manipulate this time information in
those standard value types, even if you cannot use `InternetTime` directly.

**enum `InternetTimeUnit`**  
An implementation of `TemporalUnit` representing the two time units introduced by *Internet Time*,
which are:

* `BEATS` (<math><mfrac><mn>1</mn><mn>1,000</mn></mfrac></math> of one day)
* `CENTIBEATS` (<math><mfrac><mn>1</mn><mn>100</mn></mfrac></math> of one *.beat*)

All public classes are final, immutable, and thread-safe.
**For more detail, build and then open the API Javadoc.**


## Development

To build this library locally, you will need a modern version of Java (21+) and the
Gradle build tool (a "wrapper" script and small bootstrap library is included in the
project files that will download a local copy of Gradle automatically).  An appropriate
version of `java` must be on your command line `PATH`, otherwise set the `JAVA_HOME`
environment variable to an appropriate JDK installation location.

On Windows, use `gradlew.bat`.  The remainder of this document will use Unix/Linux/Mac
conventions, with `./gradlew` instead.

To build, including running all quality checks:  
```bash
./gradlew clean build
```

Run the tests:  
```bash
./gradlew test
```

Build the API documentation:  
```bash
./gradlew javadoc
```

### Contributing

- Please open issues and PRs on this GitHub repository: https://github.com/wrprice/inet-time
- Suggested contribution checklist:
  - Add or update unit tests for any new behavior (code coverage is >95%!)
  - Follow existing code style and formatting
  - Include descriptive commit messages and a PR description


## License

Copyright &copy;2025 William R. Price - All rights reserved.

TBD
