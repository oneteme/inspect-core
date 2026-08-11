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

Architecture & Key Concepts - Execution Model
---
Inspect-Core operates on three fundamental concepts:

#### 1. **Session** (Main Unit of Work)
A session represents a logical processing boundary that groups related operations:
- **HTTP Session**: Triggered by incoming HTTP request to the application
- **Main Session**: Application startup, batch job, or background process
- **Command Session**: Other entry points (scheduled tasks, message handlers, etc.)

Each session:
- Has a unique identifier (UUID)
- Tracks start/end timestamps
- Maintains thread context via `ThreadLocal` storage
- Owns all subordinate requests and operations

**Key classes**: `HttpSessionSignal`, `MainSessionSignal`, `SessionContextManager`

#### 2. **Request** (I/O Operation)
A request is a single interaction with an external or local resource within a session:
- **HTTP Request**: Outbound REST/WebSocket call
- **Database Request**: SQL query, connection checkout, transaction management
- **FTP Request**: File transfer operation
- **Mail Request**: SMTP/IMAP message
- **Directory Request**: LDAP query
- **Local Request**: Arbitrary method execution within the app

Each request:
- Belongs to exactly one session
- Has a unique identifier
- Records start/end time, duration, status (success/failure)
- Captures request-type-specific metadata (URL, SQL command, FTP action, etc.)

**Key classes**: `HttpRequestSignal`, `DatabaseRequestSignal`, `FtpRequestSignal`, `MailRequestSignal`, `DirectoryRequestSignal`, `LocalRequestSignal`

#### 3. **Trace Signal & Update Pattern**
Inspect-Core uses a two-phase capture model for each operation:

```
Operation Start: TraceSignal emitted
    |
    v
Operation executes...
    |
    v
Operation End: TraceUpdate emitted
```

- **TraceSignal**: Immutable snapshot captured at operation start with available metadata
    - Implements `TraceSignal` interface with `getStart()` timestamp
    - Examples: `HttpSessionSignal`, `HttpRequestSignal`, `DatabaseRequestSignal`

- **TraceUpdate**: Mutable object captured at operation end with completion metadata
    - Implements `TraceUpdate` interface with `getEnd()`, `setEnd()` methods
    - Captures outcome (status, error, exception details)
    - Examples: `HttpSessionUpdate`, `HttpRequestUpdate`, `DatabaseRequestUpdate`

### Trace Lifecycle

```
1. Operation starts → create Signal with metadata
2. Emit Signal to hub
3. Operation executes
4. Operation completes/fails → create Update with end time
5. Emit Update to hub
6. Hub queues traces for batch dispatch or local dump
7. Periodic scheduler flushes queue to exporter
8. Exporter sends to remote server or writes local files
```

---

## Core Components

### 1. TraceDispatcherHub (Central Dispatcher)

**Responsibility**: Singleton coordinator for all trace collection, buffering, and dispatch.

**Key Methods**:
- `emitTask(DispatchTask)`: Queue a trace lifecycle event (session/request start/end)
- `emitTrace(EventTrace)`: Queue an event trace for dispatch
- `hub()`: Access the global singleton
- `createHub()`: Factory for creating hub with configuration
- `getState()` / `setState()`: Query/modify dispatch state (RUNNING, PAUSED, STOPPED)
- `peek()`: Non-destructive queue inspection

**Internal State**:
- `ProcessingQueue`: Thread-safe buffer for pending traces (configurable capacity)
- `ScheduledExecutorService`: Single-threaded executor for periodic dispatch
- `List<DispatchTask>`: Queue of lifecycle events
- `AtomicReference<DispatchState>`: Current dispatch mode (RUNNING, PAUSED, STOPPED)

**Lifecycle**:
- Created once at application startup via Spring bean initialization
- Depends on `inspectHub` bean for proper resource ordering
- Registers shutdown hook to flush remaining traces on JVM exit
- Scheduler runs every N seconds (configurable, minimum 10s)

### 2. SessionContextManager (Thread-Local Context)

**Responsibility**: Manage per-thread trace context and propagate across thread/async boundaries.

**Key Concepts**:
- Uses `ThreadLocal` storage to hold current session/request context
- Wraps `Runnable`, `Callable`, `CompletableFuture` to propagate context
- Maintains stack of session contexts for nested operations
- Auto-closes contexts to prevent memory leaks

**Key Methods**:
- `activeContext()`: Get current session context or null
- `createMainSession(Instant, String)`: Start main application session
- `createHttpSession(Instant, String, URI)`: Start HTTP request session
- `createLocalRequest()`: Create local operation request within session
- `asCallable(Callable<T>, SessionTraceContext)`: Wrap callable with context
- `asRunnable(Runnable, SessionTraceContext)`: Wrap runnable with context
- `register()/unregister()`: Lifecycle callbacks for resource cleanup

**Thread Safety**:
- `ThreadLocal` ensures per-thread isolation
- No synchronization required for context access
- Session stack prevents context corruption in nested scenarios

### 3. ProcessingQueue (Trace Buffer)

**Responsibility**: Thread-safe, bounded FIFO queue for buffering pending traces.

**Key Methods**:
- `add(EventTrace)`: Enqueue single trace (non-blocking)
- `addAll(Collection<EventTrace>)`: Batch enqueue traces
- `pollAll()`: Drain entire queue (atomic operation)
- `peek()`: View queue size without modification
- `size()`: Get current queue size

**Overflow Handling**:
- If queue reaches capacity (default 1000), operates in "waste mode"
- In waste mode, new traces are silently discarded
- Prevents unbounded memory growth if dispatch is slower than collection

### 4. RestTraceExporter (Remote Dispatch)

**Responsibility**: HTTP client for dispatching buffered traces to remote inspect-server.

**Key Capabilities**:
- Batches traces into single POST request
- Applies GZIP compression (configurable)
- Automatic retry logic with exponential backoff (default: 3 retries)
- Registers application instance on first dispatch
- Handles remote server connectivity failures gracefully
- Response parsing and error detection

**Dispatch Process**:
1. Poll queue for pending traces
2. Serialize to JSON with Jackson TypeInfo
3. Compress payload
4. POST to `https://{host}:{port}/api/dispatch`
5. Retry on network/timeout failures
6. Log results and metrics

### 5. Monitor (Execution Lifecycle)

**Responsibility**: Factory for creating lifecycle listeners that bridge execution events to trace creation.

**Key Static Methods** (8 variants):
- `traceAroundMethod()`: Captures method entry/exit with exception handling
- `traceBeforeMethod()`: Captures only method entry (async/fire-and-forget)
- `traceAfterMethod()`: Captures only method completion
- All methods return `ExecutionListener` implementations


### 6. MachineResourceMonitor (System Metrics)

**Responsibility**: Periodic capture of JVM heap and disk utilization.

**Metrics Collected**:
- Heap memory: init, max, used, committed (via `MemoryMXBean`)
- Disk space: total, used, available

**Key Methods**:
- `onInstanceEmit()`: Populate static resource capacity into `InstanceEnvironment`
- `onSchedule()`: Emit periodic `MachineResourceUsage` snapshot

**Integration**: Registered as dispatch hook; called by scheduler on each interval

---

## Data Models & DTOs

### Session Models

#### HttpSessionSignal
```java
// Immutable signal at HTTP request start
- id: String (session UUID)
- start: Instant (request arrival time)
- threadName: String
- method: String (GET, POST, PUT, etc.)
- protocol: String (HTTP/HTTPS)
- host: String (IP or domain)
- port: int (-1 if unknown)
- path: String (request path)
- query: String (query parameters)
- authScheme: String (Basic, Bearer, etc., nullable)
- dataSize: long (request body bytes, -1 if unknown)
- contentEncoding: String (gzip, compress, etc.)
- userAgent: String (browser/client identifier)
- linked: boolean (related to prior session)
```

**Methods**:
- `setURI(URI)`: Parse URI components into signal fields
- `createCallback()`: Create mutable `HttpSessionUpdate` for completion

#### HttpSessionUpdate
```java
// Mutable completion record
- id: String (matches signal ID)
- end: Instant (response completion time)
- status: int (HTTP status code, nullable)
- duration: long (milliseconds)
- message: String (status reason, nullable)
- throwable: ExceptionInfo (error details, nullable)
```

#### MainSessionSignal / MainSessionUpdate
Similar to HTTP but for application startup/background sessions.

### Request Models

#### HttpRequestSignal
```java
// Outbound HTTP request start
- id: String (request UUID)
- sessionId: String (owning session)
- start: Instant (call start time)
- threadName: String
- method: String (HTTP method)
- protocol, host, port, path, query, authScheme, dataSize, contentEncoding (same as session)
```

#### DatabaseRequestSignal

```java
// Database interaction start
- id: String
- sessionId: String
- start: Instant
- threadName: String
- scheme: String (JDBC scheme: mysql, postgresql, oracle, etc.)
- host: String (database server IP/hostname)
- port: int (database port, -1 if unknown)
- name: String (database name, nullable)
- schema: String (active schema)
- driverVersion: String (JDBC driver version)
- productName: String (database product: MySQL, PostgreSQL, etc.)
- productVersion: String (database version)
```

#### FtpRequestSignal / MailRequestSignal / DirectoryRequestSignal
Similar structure with protocol-specific fields (server, port, action type, etc.)

### Exception & Error Models

#### ExceptionInfo
```java
// Serializable exception snapshot
- type: String (fully qualified exception class name)
- message: String (exception message, truncated if needed)
- stackTraceRows: StackTraceRow[] (truncated stack trace)
- cause: ExceptionInfo (chained exception, recursive)
```

#### StackTraceRow
```java
// Single stack frame
- className: String
- methodName: String
- fileName: String (source file)
- lineNumber: int
- nativeMethod: boolean
```

**Key Methods**:
- `exceptionStackTraceRows(Throwable)`: Extract stack from exception
- `appendStackTrace()`: Append additional frames

### Environment & Configuration Models

#### InstanceEnvironment
```java
// Application runtime environment snapshot
- id: String (application instance UUID)
- instant: Instant (startup time)
- type: InstanceType (SERVER, CLIENT, BATCH, etc.)
- name: String (application/service name)
- version: String (application version)
- env: String (dev, test, staging, prod)
- address: String (IP address)
- os: String (operating system)
- re: String (runtime environment: JAVA, .NET, etc.)
- user: String (process owner user)
- branch: String (git branch)
- hash: String (commit hash)
- collector: String (collector library version)
- additionalProperties: Map<String, String> (custom metadata)
- resource: MachineResource (heap/disk capacity)
- end: Instant (shutdown time, nullable)
```

#### MachineResource
```java
// Static resource capacity
- heapInitMb: long (initial heap)
- heapMaxMb: long (max heap)
- diskTotalMb: long (total disk)
```

#### MachineResourceUsage
```java
// Periodic resource snapshot
- instant: Instant (sampling time)
- heapUsedMb: long
- heapCommittedMb: long
- diskFreeMb: long
```

### Logging & Events

#### LogEntry
```java
// Captured log event
- instant: Instant
- level: Level (INFO, WARN, ERROR, REPORT)
- message: String
- stackRows: StackTraceRow[] (exception stack if present)
- sessionId: String (owning session, nullable)
- instanceId: String (for server-side filtering)
```

**Levels**:
- INFO: Informational message
- WARN: Warning condition
- ERROR: Error condition
- REPORT: High-priority diagnostic report

#### EventTrace
Marker interface for all trace-emitted events. Implemented by:
- `HttpSessionSignal`, `HttpSessionUpdate`
- `HttpRequestSignal`, `HttpRequestUpdate`
- `DatabaseRequestSignal`, `DatabaseRequestUpdate`
- `FtpRequestSignal`, `FtpRequestUpdate`
- `MailRequestSignal`, `MailRequestUpdate`
- `DirectoryRequestSignal`, `DirectoryRequestUpdate`
- `LogEntry`, `MachineResourceUsage`

JSON serialization uses Jackson `@JsonTypeInfo` with type name discrimination.

---

## Action Types (Enums)

### HttpAction
```
CONNECT, DISCONNECT, REQUEST, RESPONSE, CANCEL, TIMEOUT, REDIRECT
```

### DatabaseAction
```
CONNECT, DISCONNECT, PREPARE, EXECUTE, FETCH, COMMIT, ROLLBACK, CANCEL, TIMEOUT
```

### FtpAction
```
CONNECT, DISCONNECT, AUTHENTICATE, PUT, GET, DELETE, LIST, MKDIR, RENAME, CANCEL
```

### MailAction
```
CONNECT, DISCONNECT, AUTHENTICATE, SEND, RECEIVE, DELETE
```

### DirAction
```
CONNECT, DISCONNECT, AUTHENTICATE, SEARCH, COMPARE, MODIFY, ADD, DELETE
```

### LocalRequestType
```
EXEC, START, STOP, INVOKE, SCHEDULE, ASYNC, OTHER
```

---

## Dispatch & Scheduling

### DispatchState
```
RUNNING   → Actively collecting and dispatching traces
PAUSED    → Collection paused (traces discarded)
STOPPED   → Dispatch halted, system shutdown
DELAYING  → Temporary delay before next dispatch cycle
```

### DispatchMode
```
ASYNC     → Non-blocking dispatch on separate thread
SYNC      → Blocking dispatch on caller thread
BATCH     → Collect and dispatch in batches
STREAMING → Real-time dispatch of individual traces
```

### Dispatch Lifecycle
1. **Startup**: `TraceDispatcherHub` created, scheduler started
2. **Collection**: Traces emitted, queued in `ProcessingQueue`
3. **Scheduled Dispatch** (every N seconds):
    - Poll all pending traces from queue
    - Group into batch
    - Invoke `TraceExporter.export(List<EventTrace>)`
    - On success: traces delivered, queue cleared
    - On failure: retry with backoff, queue retained
4. **Shutdown**: Shutdown hook flushes remaining traces, closes executor

---

## Technologies & Dependencies

### Build & Runtime
- **Java**: 21+ (Java LTS)
- **Build Tool**: Maven 3.6+
- **JUnit**: 5.8.1 (testing)

### Spring Framework Integration
- **Spring Boot**: 3.0.5 (auto-configuration)
- **Spring AOP**: 3.0.5 (method interception)
- **Spring WebFlux**: 3.0.5 (reactive HTTP support)
- **Spring LDAP**: 3.2.0 (directory operations)

### Serialization
- **Jackson**: `com.fasterxml.jackson.databind` (JSON with type discrimination)
- **Lombok**: 1.18.32 (annotations for POJOs: `@Getter`, `@Setter`, `@RequiredArgsConstructor`)

### External Technology Support (Optional, via Wrappers)
- **JDBC**: DataSource, Connection, Statement wrappers for SQL interception
- **HTTP Clients**:
    - Blocking: `ClientHttpRequestInterceptor` for `RestTemplate`
    - Reactive: `ExchangeFilterFunction` for `WebClient`
- **Reactor**: Hook-based context propagation for Project Reactor (non-blocking Flux/Mono)
- **Flyway**: Database migration tracing (optional)
- **LDAP**: `spring-ldap-core` ContextSource wrapping
- **SFTP/FTP**: `JSch` (JSch `ChannelSftp` wrapper)
- **SMTP/IMAP**: Jakarta Mail API wrapper
- **File System**: Native file operations via `File` I/O

---

## Configuration

### Key Configuration Classes
- `InspectCollectorConfiguration`: Top-level config container
- `SchedulingProperties`: Dispatch frequency and mode
- `MonitoringConfiguration`: Collection rules
- `TracingProperties`: Queue and export settings
- `RemoteServerProperties`: REST endpoint configuration
- `ResourceMonitoringProperties`: System metrics collection

---

Spring Integration

Auto-Configuration

Inspect-Core provides Spring Boot auto-configuration via:
- `InspectConfiguration`: Main configuration class
- `InspectCollectorConfiguration`: Conditional bean for enabled collector

Beans Registered

1. **inspectHub** (`TraceHub`): Central dispatcher singleton
2. **httpSessionMonitor** (`HttpSessionMonitor`): HTTP session tracking filter
3. **httpRequestMonitor** (`HttpRequestMonitor`): Outbound HTTP client interception
4. **databaseConnectionMonitor**: JDBC wrapper registration
5. **machineResourceMonitor** (`MachineResourceMonitor`): JVM metrics
6. **traceExporter** (`TraceExporter`): REST or dump dispatcher

Module Configurations
---
Conditional beans for optional technology support:

- **ReactorModuleConfiguration**: Reactor hook registration (WebClient, Schedulers)
- **FlywayModuleConfiguration**: Flyway migration tracing
- **DirectoryModuleConfiguration**: LDAP ContextSource wrapping

### Thread Propagation

Context propagation wrappers for async scenarios:
- `SessionContextManager.asRunnable()`
- `SessionContextManager.asCallable()`
- `SessionContextManager.asCompletableFuture()`

Example:
```java
// Auto-wrapping in Spring async methods
@Async
public void backgroundTask() {
    // Context auto-propagated via SessionContextManager
}

// Manual wrapping for CompletableFuture
CompletableFuture.supplyAsync(
    SessionContextManager.asCallable(() -> expensiveOperation())
);
```

---

REST API (inspect-server endpoints)
---

Inspect-Core dispatches to inspect-server via:

```
POST /api/dispatch HTTP/1.1
Host: inspect-server.example.com
Content-Type: application/json
Content-Encoding: gzip

[
  {
    "@type": "HttpSessionSignal",
    "id": "uuid-...",
    "start": "2024-01-15T10:30:00.000Z",
    "method": "GET",
    "protocol": "HTTPS",
    "host": "api.example.com",
    ...
  },
  {
    "@type": "HttpRequestSignal",
    "id": "uuid-...",
    "sessionId": "uuid-...",
    ...
  }
]
```

**Response**: 200 OK with metadata or error details

Contribute
----------
Fork → feature branch → PR with tests and documentation. See CONTRIBUTING.md for details.

License & Contact
-----------------
Apache License 2.0 — see LICENSE file. Report issues on GitHub.