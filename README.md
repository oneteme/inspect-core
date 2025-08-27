# inspect-core

INSPECT — Integrated System Performance Evaluator & Communication Tracking

inspect-core is an open-source Java library designed to supervise and trace application behavior (monolithic or distributed). It collects structured events about application activity (sessions and requests) — such as startup, batch runs, incoming/outgoing HTTP traffic, external resource calls and parallel processing — and forwards them to a central inspect-server for correlation and analysis.

Why INSPECT?
- Gain a correlated, end-to-end view of processing and communications across your information system.
- Trace incoming and outgoing traffic, batch executions and application startup events in a structured, searchable way.
- Follow asynchronous and multi-threaded processing and correlate related events into the same session.
- Centralize analysis and aggregation of traces on a dedicated inspect-server to detect latency, errors and resource issues.

1. Presentation
---------------
inspect-core captures two main concepts:
- Session: represents a logical processing unit (for example: application startup, a batch job, handling of an incoming HTTP request, a test run). Sessions group related events and enable correlation of actions performed during their lifetime.
- Request: represents an interaction with an external or local resource (for example: HTTP, FTP/SFTP, LDAP/JNDI, SMTP, database calls or local operations like cache and file access). Requests include metadata such as duration, status, endpoint, payload sizes and any errors.

2. Integration (Maven)
----------------------
Add the library to your project:

Maven: [![Maven Central Version](https://img.shields.io/maven-central/v/io.github.oneteme/inspect-core?style=social)](https://central.sonatype.com/artifact/io.github.oneteme/inspect-core)
```xml
<dependency>
  <groupId>io.github.oneteme</groupId>
  <artifactId>inspect-core</artifactId>
  <version>REPLACE_WITH_VERSION</version>
</dependency>
```

Gradle (example):
```groovy
implementation 'io.github.oneteme:inspect-core:REPLACE_WITH_VERSION'
```

3. Configuration (example YAML)
-------------------------------
Add and adapt this to your `application.yml`:

```yaml
inspect:
  collector:
    enabled: true                # default=false, enable the collector
    debug-mode: false            # default=false, enable debug mode for collector
    scheduling:
      interval: 5s               # default=60s, interval between trace dispatches
    monitoring:
      http-route:
        excludes:
          method: OPTIONS        # default=[], exclude specific HTTP methods
          path: /favicon.ico, /actuator/info  # default=[], exclude matching paths
      resources:
        enabled: true            # collect memory / disk / basic resource metrics
      exception:
        max-stack-trace-rows: -1 # -1 = unlimited, limit stack trace rows in traces
        max-cause-depth: -1      # -1 = unlimited, limit nested cause depth
    tracing:
      queue-capacity: 100        # default=10000, trace dispatch queue capacity
      delay-if-pending: 0        # default=30, delay (s) if previous dispatch pending
      dump:
        enabled: true            # write traces to local disk (useful for debug)
      remote:
        mode: REST               # dispatch mode (e.g., REST)
        host: http://localhost:9001
        retention-max-age: 10d   # default=30d, retention period for local traces
```

Notes:
- Tune `scheduling.interval` and `tracing.queue-capacity` to match your load profile.
- Use HTTPS for `remote.host` in production to secure trace transfer.

4. Monitoring — what inspect-core supervises
---------------------------------------------
- Sessions
  - Startup: capture application boot sequences and initialization events.
  - Batch: track job and batch executions (duration, success/failure, exceptions).
  - Incoming HTTP: track handling of incoming HTTP requests (method, path, status, timing).
  - Tests: capture test-run executions for correlation in integration environments.

- Requests
  - HTTP: outgoing and incoming HTTP calls (URL, method, headers, payload sizes, response status).
  - FTP / SFTP: connection and transfer metadata (host, port, user, duration, errors).
  - LDAP / JNDI: lookup/list/attribute operations (endpoint, user, duration).
  - SMTP: email send operations (message metadata, recipients, size, delivery status).
  - Local operations: cache access, file operations, constant or in-process operations (context for non-network work).

- Threads
  - Tracks and correlates work executed on other threads so parallel or asynchronous processing is included in the parent session and not lost.

- Resources
  - Basic machine metrics such as memory and disk usage collected periodically to correlate resource consumption with application activity.

5. Security & privacy
---------------------
- Traces may include headers and payload metadata. Be careful with sensitive data (Authorization headers, personal data, secrets).
- Prefer filtering or masking sensitive fields before traces are dispatched.
- Secure transport to the inspect-server (HTTPS) and control access/retention policies on the server side.

6. Best practices
-----------------
- Enable `debug-mode` and `dump` only for diagnostics; both increase I/O and logs.
- Configure sensible defaults for `queue-capacity` and `scheduling.interval` based on expected traffic.
- Exclude non-relevant routes (OPTIONS, favicon, health checks) to reduce noise.
- Apply data redaction where required to comply with privacy regulations.

7. Quickstart
-------------
1. Add the dependency to your project.
2. Add the example configuration in your `application.yml` and set `collector.enabled: true`.
3. Configure `inspect.tracing.remote.host` to point to your inspect-server (or enable local dump).
4. Start the application and verify traces are received by your inspect-server or written to disk.

8. Tests, build & contribution
------------------------------
- Build and test with Maven:
  ```bash
  mvn clean test
  mvn -DskipTests package
  ```
- Contribution model: fork → branch → pull request. Add tests and documentation for new features or fixes.
- Respect the repository Code of Conduct.

9. License & contact
--------------------
- License: Apache License 2.0 (see LICENSE file).
- Report issues or request features via GitHub issues on this repository.

---

```
## Collectors

| Request  | CLASS        |
|----------|--------------|
| JDBC     | javax.sql.DataSource |
| LDAP     | javax.naming.directory.DirContext |
| SMTP     | jakarta.mail.Transport |
| HTTP     | org.springframework.http.client.ClientHttpRequestInterceptor <br> org.springframework.web.reactive.function.client.ExchangeFilterFunction |
| FTP      | com.jcraft.jsch.ChannelSftp |
| LOCAL    | org.aspectj.lang.annotation.Aspect |



