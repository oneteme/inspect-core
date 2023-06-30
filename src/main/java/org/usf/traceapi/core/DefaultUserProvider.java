package org.usf.traceapi.core;

import static java.util.Optional.ofNullable;

import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;

public final class DefaultUserProvider implements ApiUserProvider, BatchUserProvider {

	@Override
	public String getUser() {
		return null;
	}

	@Override
	public String getUser(HttpServletRequest req) {
		return ofNullable(req.getUserPrincipal())
        		.map(Principal::getName)
        		.orElse(null);
	}

}
