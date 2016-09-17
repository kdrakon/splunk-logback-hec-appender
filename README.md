# Splunk Logback HTTP Event Collector Appender

**still under construction**

This is a [Logback Appender](http://logback.qos.ch/manual/appenders.html) made for Splunk's HTTP Event Collector (HEC) API. Splunk provides their [own appenders](https://github.com/splunk/splunk-library-javalogging), but at the time of this libraries creation, the Logback one was quite limited in terms of configuration and the data you could append to log indexes. This provides much more logging data than the current standard Splunk appender — which supplies only log *messages* and *severity*.

One of the additional features this appender provides is based on the capabilities of the [logstash-logback-encoder](https://github.com/logstash/logstash-logback-encoder): appending extra JSON fields or metadata to your log entries via [`customFields`](#splunkhttpeventcollectorjsonlayout).

## Configuration
### Sample XML Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <appender name="splunk" class="io.policarp.splunk.logback.SplunkHttpEventCollectorLogbackAppender">

        <splunkUrl>https://somewhere.splunkcloud.com/services/collector/event</splunkUrl>
        <token>1234-5678-91011-ABC-321</token>
        <buffer>25</buffer>
        <flush>10</flush>
        <parallelism>8</parallelism>

        <layout class="io.policarp.splunk.logback.SplunkHttpEventCollectorJsonLayout">
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

### SplunkHttpEventCollectorLogbackAppender
####Logback Configuration
- `<splunkUrl>`
  - The URL of the HEC endpoint 
  - e.g. https://somewhere.splunkcloud.com/services/collector/event
- `<token>`
  - The token that authorizes posting to the HEC endpoint
  - e.g. _1234-5678-91011-ABC-321_
- `<queue>`
  - A maximum queue size for log messages to be stored in memory. If this fills up and log messages are not posted to Splunk in a timely manner, then the queue will cause blocking to occur on subsequent appends. 
  - 1000 *(default)*
- `<buffer>`
  - Log messages are buffered in memory off of the queue. Once a buffer is filled, logs are instantly posted to HEC endpoint.
  - 25 *(default)*
- `<flush>`
  - Specifies a timeout in seconds in which the buffer should be flushed regardless of the current number of logs in it. This ensures log messages don't stagnate.
  - 30 *(default)*
- `<parallelism>`
  - Log messages are posted to the HEC endpoint in parallel. This number specifies how many parallel posts should happen asynchronously.
  
The appender also supports the addition of [Logback Filter's](http://logback.qos.ch/manual/filters.html) — see the XML example above.

### SplunkHttpEventCollectorJsonLayout
By default, the `SplunkHttpEventCollectorLogbackAppender` will use a default configured `SplunkHttpEventCollectorJsonLayout`. The JSON data is rendered using [json4s](https://github.com/json4s/json4s) from the following case classes:

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
    callingClass: Option[String],
    callingMethod: Option[String],
    callingLine: Option[String],
    callingFile: Option[String],
    exception: Option[String],
    stacktrace: Option[List[String]],
    customFields: Option[mutable.HashMap[String, String]]
  ) extends EventJson
}
```

####Logback Configuration
- `<custom>`
  - A custom field that is appended to log messages (`customFields`). This must be encoded as individual `<custom>` key-value pairs separated by an equal (`=`) sign. For example:
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
  You can override the layout with a class extending either `SplunkHttpEventCollectorJsonLayout` `BaseSplunkHttpEventCollectorJsonLayout`, or `LayoutBase[ILoggingEvent]`. Then `<layout>` can be specified in the `<appender>` section to specify — see the XML example above

## HTTP Client
The base implementation uses the [skinny-framework's HTTP client](https://github.com/skinny-framework/skinny-framework). It is a tiny library and does not bring with it many dependencies. `SplunkHttpEventCollectorLogbackAppender` uses `SkinnyHttpHecClient` for HTTP communication.

You can however bring in your own implementation by mixing in your own class that extends `SplunkHttpEventCollectorClient` with `SplunkHttpEventCollectorLogbackAppenderBase`.