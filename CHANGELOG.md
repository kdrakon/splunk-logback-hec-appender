# Changelog

## 1.1.0
## Fixed
- provides fix for dead-lock caused by unseen synchronized block caused by ch.qos.logback.core.AppenderBase
- the fix for the dead-lock is addressed by removing the use of blocking queue for logging events. As a result, this also removed the need for SplunkHecAppenderStrategy's
## Changed
- additionally, added Monix circuit breakers around consuming the log event stream and pushing data to Splunk via HTTP
- updated Monix to 2.3.0
- cross compiling to Scala 2.11.11 and 2.12.2

## 1.0.3
- Updated dependencies to latest versions.

## 1.0.2
## Fixed
- [issue #7](https://github.com/kdrakon/splunk-logback-hec-appender/issues/7): ThresholdFilter not working

## 1.0.1
### Changed
- updated Monix library from 2.0.1 to 2.0.2
### Added
- added this Changelog

## 1.0.0
- first release