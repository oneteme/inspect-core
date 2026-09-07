package org.usf.inspect.core;

import static java.time.Duration.ofDays;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public final class Retention {
	
    private Duration diagnostic = ofDays(10);
    private Duration audit = ofDays(7);

}
