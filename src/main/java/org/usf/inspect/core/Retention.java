package org.usf.inspect.core;

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
	
    private Duration diagnostic = Duration.ofDays(10);
    private Duration audit = Duration.ofDays(7);

}
