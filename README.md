
## Integration
```XML
<dependency>
    <groupId>io.github.oneteme</groupId>
    <artifactId>inspect-core</artifactId>
    <version>0.0.4</version>
</dependency>
```

## Configuration
```YAML
inspect:
  enabled: true #activate INSPECT in the host server
  dispatch:
    delay: 60 #sever trace frequency
    unit: SECONDS
    buffer-max-size: 5000 #maximum number of sessions
  track:
    rest-session:
      excludes:
        method: OPTIONS #server HTTP method to exclude
        path: /favicon.ico, /error #server endpoint to exclude
```
## Collectors

| Request  | CLASS        |
|----------|--------------|
| JDBC     | javax.sql.DataSource |
| LDAP     | javax.naming.directory.DirContext |
| SMTP     | jakarta.mail.Transport |
| HTTP     | org.springframework.http.client.ClientHttpRequestInterceptor <br> org.springframework.web.reactive.function.client.ExchangeFilterFunction |
| FTP      | com.jcraft.jsch.ChannelSftp |
