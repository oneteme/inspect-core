package org.usf.traceapi.core;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ApplicationInfo {
	
	private final String name;
	private final String version; // maven, NPM, ..
	private final String env; //dev, rec, prod, ...
	private final String os;  //operating system : Window, Linux, ...
	private final String re;  //runtime environment : JAVA, JS, PHP, Browser, Postman ...

}
