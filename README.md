


## Integration [![Maven Central Version](https://img.shields.io/maven-central/v/io.github.oneteme/inspect-core)](https://central.sonatype.com/artifact/io.github.oneteme/inspect-core)
```XML
<dependency>
    <groupId>io.github.oneteme</groupId>
    <artifactId>inspect-core</artifactId>
    <version>{{version}}</version>
</dependency>
```

## Configuration
```YAML
spring:
  application:
    name: <appName>
    version: <version>
inspect:
  enabled: true #activate INSPECT in the host server
  target: REMOTE #remote INSPECT server
  server:
    host: <inspect-server-url>
    #compress-min-size: -1 #no compress
  dispatch:
    delay: 60 #sever trace frequency
    unit: SECONDS
    buffer-max-size: 5000 #maximum number of sessions
  track:
    rest-session:
      excludes:
        method: OPTIONS #HTTP method to exclude
        path: /favicon.ico #endpoint to exclude

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
