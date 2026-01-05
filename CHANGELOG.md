# Changelog

-----

## [Unreleased]


-----

## [0.8.64] - 2026-01-04

### Added

- Lots more API documentation (Javadoc)
- `InternetTimeField.CENTIBEAT_OF_BEAT` enum instance; previously internal and private
- `InternetTime`:
    - DateTimeFormatter `static final` fields
    - Parsing and formatting convenience methods

### Removed

- `InternetTime::ofInstant` removed for naming consistency, use `InternetTime::from` instead.
  Other `of` methods produce exact results, but converting from Instant is rarely exact.

### Fixed

- Minor bug fixes & API spec compliance


-----

## [0.4.20] - 2025-12-31

- Initial published release
