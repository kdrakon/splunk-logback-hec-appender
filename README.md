# Splunk Logback HTTP Event Collector Appender

[![Build Status](https://travis-ci.org/kdrakon/splunk-logback-hec-appender.svg?branch=master)](https://travis-ci.org/kdrakon/splunk-logback-hec-appender)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.policarp/splunk-logback-hec-appender_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.policarp/splunk-logback-hec-appender_2.12)

This is a [Logback Appender](http://logback.qos.ch/manual/appenders.html) made for Splunk's HTTP Event Collector (HEC) API. Splunk provides their [own appenders](https://github.com/splunk/splunk-library-javalogging), but at the time of this libraries creation, the Logback one was quite limited in terms of configuration and the data you could append to log indexes. This appender provides much more logging data than the current standard Splunk appender — which supplies only log fields for *message* and *severity*. With the addition of JSON fields containing more logging metadata, complex Splunk search queries can be written. This can potentially lead to better insight into your application.

Some of the inspiration for this appender is based on the capabilities of the [logstash-logback-encoder](https://github.com/logstash/logstash-logback-encoder), which originally provided the ability to specify custom JSON fields in Logstash entries.

It is implemented using the principles of reactive streams. This was very straightforwardly done using the [Monix library](https://monix.io/).

## Compatability
- due to the use of the Skinny Framework's HTTP client, the minimum **Java** version is **8**. However, I haven't tested the appender in a Java project yet. Technically, it should be compatible, but please let me know if it definitely works.
- I have tested the appender against the Enterprise Splunk Cloud HTTP Event Collector (*version 6.6.1*).

## Configuration
### Sample XML Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

  <appender name="splunk" class="io.policarp.logback.SplunkHecAppender">

    <splunkUrl>https://somewhere.splunkcloud.com/services/collector/event</splunkUrl>
    <token>1234-5678-91011-ABC-321</token>
    <buffer>25</buffer>
    <flush>10</flush>
    <parallelism>8</parallelism>

    <layout class="io.policarp.logback.SplunkHecJsonLayout">
      <source>my-application</source>
      <custom>appversion=${APP_VERSION}</custom>
      <custom>user=${USER}</custom>
    </layout>

    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
      <level>INFO</level>
    </filter>

  </appender>

  <root level="DEBUG">
    <appender-ref ref="splunk"/>
  </root>

</configuration>
```

### SplunkHecAppender
#### Logback Configuration
- `<splunkUrl>`
  - The URL of the HEC endpoint 
  - e.g. https://somewhere.splunkcloud.com/services/collector/event
- `<token>`
  - The token that authorizes posting to the HEC endpoint
  - e.g. _1234-5678-91011-ABC-321_
- `<buffer>`
  - Log messages are buffered in memory off of the queue. Once a buffer is filled, logs are instantly posted to the HEC endpoint. This size also signifies the maximum payload size sent to the endpoint.
  - 25 *(default)*
- `<flush>`
  - Specifies a timeout in **seconds** in which the buffer should be flushed regardless of the current number of logs in it. This ensures log messages don't stagnate.
  - 30 *(default)*
- `<parallelism>`
  - Log messages are posted to the HEC endpoint in parallel. This number specifies how many parallel posts should happen asynchronously.
  - defaults to number of CPU cores
  
The appender also supports the addition of [Logback Filter's](http://logback.qos.ch/manual/filters.html) — see the XML example above.

### SplunkHecJsonLayout
By default, the `SplunkHecAppender` will use a default configured `SplunkHecJsonLayout`. The JSON data is rendered using [json4s](https://github.com/json4s/json4s) from the following case classes:

```scala
package object json {

  trait EventJson

  case class BaseJson(
    time: Long,
    event: EventJson,
    host: Option[String] = None,
    source: Option[String] = None,
    sourcetype: Option[String] = None,
    index: Option[String] = None
  )

  case class FullEventJson(
    message: String,
    level: String,
    thread: String,
    logger: String,
    callingClass: Option[String] = None,
    callingMethod: Option[String] = None,
    callingLine: Option[String] = None,
    callingFile: Option[String] = None,
    exception: Option[String] = None,
    stacktrace: Option[List[String]] = None,
    customFields: Option[mutable.HashMap[String, String]] = None
  ) extends EventJson
}
```

####Logback Configuration
- `<custom>`
  - A custom field that is appended to log messages (under the JSON field `customFields`). This must be encoded as individual `<custom>` tags with key-value pairs separated by an equal (`=`) sign. For example:
    - applicationVersion=${GIT_APP_VERSION}
    - applicationName=SuperCoolApp
- `<maxStackTrace>`
  - A max-depth to trim stack traces. If the trace is trimmed, an ellipsis is appended to the end.
  - 500 *(default)*
- Standard [Splunk metadata fields](http://dev.splunk.com/view/event-collector/SP-CAAAE6P#meta) are configurable, including:
  - `<source>`
  - `<sourcetype>`
  - `<index>`
  - `<host>`
  
  ####Custom Layout
  You can override the layout with a class extending either `SplunkHecJsonLayout`,`SplunkHecJsonLayoutBase`, or `LayoutBase[ILoggingEvent]`. Then `<layout>` can be specified in the `<appender>` section — see the XML example above

## HTTP Client
The base implementation uses the [skinny-framework's HTTP client](https://github.com/skinny-framework/skinny-framework). It is a tiny library and does not bring with it many dependencies. `SplunkHecAppender` uses `SkinnyHttpHecClient` for HTTP communication.

You can however bring in your own implementation by mixing in your own class that extends `SplunkHecClient` with `SplunkHecAppenderBase`.

**Note**: the `SplunkHecAppender` has a Logback filter automatically added that filters out logging from the Skinny HTTP client to avoid an infinite feedback loop. To filter out logging in its entirety, append something like this to your config:

```xml 
<logger name="skinny.http" level="OFF"/>
```
