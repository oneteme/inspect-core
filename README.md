# inspect-core

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.oneteme/inspect-core?style=social)](https://central.sonatype.com/artifact/io.github.oneteme/inspect-core)  
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)

Short description
-----------------
inspect-core is a lightweight Java library to capture structured runtime telemetry (sessions, requests, resource metrics and thread correlations) from monolithic or distributed applications. It focuses on producing correlated traces that can be dispatched to an inspect server or written locally for offline analysis.

Why use inspect-core?
- End-to-end correlated tracing across threads and asynchronous work.
- Capture HTTP, JDBC, LDAP, FTP/SFTP, SMTP and local operations.
- Configurable local dump or remote dispatch to an inspect-server.
- Minimal intrusion: pluggable collectors and Spring-friendly integrations.

Quick Start
-----------
Maven
```xml
<dependency>
  <groupId>io.github.oneteme</groupId>
  <artifactId>inspect-core</artifactId>
  <version>REPLACE_WITH_VERSION</version>
</dependency>
```

Configuration (example application.yml)
```yaml
inspect:
  collector:
    enabled: true
    debug-mode: false
    scheduling:
      interval: 5s
    monitoring:
      http-route:
        excludes:
          method: OPTIONS
          path: /favicon.ico, /actuator/info
      resources:
        enabled: true
      exception:
        max-stack-trace-rows: -1
        max-cause-depth: -1
    tracing:
      queue-capacity: 1000
      delay-if-pending: 0
      dump:
        enabled: false
      remote:
        mode: REST
        host: https://inspect-server.example.com
        retention-max-age: 30d
```

Key Concepts
------------
- Session: Logical processing unit (app startup, batch job, incoming request) that groups related events.
- Request: Interaction with external/local resources with metadata (type, duration, status, error).
- Thread correlation: Propagate session context across threads to maintain continuity.
- Resource monitoring: Periodic basic system metrics (memory, disk) aligned with trace timelines.

Integration Notes
-----------------
- Spring Boot: add dependency and enable collector via configuration.
- Web clients: supports both blocking (ClientHttpRequestInterceptor) and reactive (ExchangeFilterFunction) capture.
- Sensitive data: redact Authorization headers, PII and large payloads before dispatch.

Build & Test
------------
Maven:
```bash
mvn clean test
mvn -DskipTests package
```

Contribute
----------
Fork → feature branch → PR with tests and documentation. See CONTRIBUTING.md for details.

License & Contact
-----------------
Apache License 2.0 — see LICENSE file. Report issues on GitHub.