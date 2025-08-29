package org.usf.inspect.http;

import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;

import java.util.function.Predicate;

import org.springframework.util.AntPathMatcher;
import org.usf.inspect.core.HttpRouteMonitoringProperties;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * 
 * @author u$f
 *
 */
@RequiredArgsConstructor
public final class HttpRoutePredicate {
	
	private final Predicate<HttpServletRequest> predicate;
	
	public static HttpRoutePredicate compile(HttpRouteMonitoringProperties config) {
		Predicate<HttpServletRequest> filter = req-> true;
		if(!config.getExcludes().isEmpty()) {
			var pArr = config.excludedPaths();
			if(nonNull(pArr) && pArr.length > 0) {
				var matcher = new AntPathMatcher();
				filter = req-> stream(pArr).noneMatch(p-> matcher.match(p, req.getServletPath()));
			}
			var mArr = config.excludedMethods();
			if(nonNull(mArr) && mArr.length > 0) {
				filter = filter.and(req-> stream(mArr).noneMatch(m-> m.equals(req.getMethod())));
			}
		}
		return new HttpRoutePredicate(filter);
	}

	public boolean accept(HttpServletRequest t) {
		return predicate.test(t);
	}
}
