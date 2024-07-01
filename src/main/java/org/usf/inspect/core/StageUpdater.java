package org.usf.inspect.core;

import static java.util.Optional.ofNullable;

import java.security.Principal;

import org.aspectj.lang.ProceedingJoinPoint;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 
 * @author u$f
 *
 */
public interface StageUpdater {

    default void update(MutableStage session, HttpServletRequest req) { }
    
    default void update(MutableStage session, ProceedingJoinPoint joinPoint) { }
    
    static String getUser(HttpServletRequest req){
    	return ofNullable(req.getUserPrincipal())
    			.map(Principal::getName)
    			.orElse(null);
    }
}
