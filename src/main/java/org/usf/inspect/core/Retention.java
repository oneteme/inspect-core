package org.usf.inspect.core;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;

@Getter
@Setter
@ToString
public class Retention {
    private Duration diagnostic = Duration.ofDays(30);
    private Duration audit = Duration.ofDays(30);
}
