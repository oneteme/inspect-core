package org.usf.inspect;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.module.SimpleModule;

/**
 * 
 * @author u$f
 *
 */
@SuppressWarnings("serial")
public final class InspectCoreModule extends SimpleModule {

	public InspectCoreModule() {
		super("inspect-core");
		registerSubtypes(
				new NamedType(ReportEvent.class, 				"00"),  
				new NamedType(SystemMetrics.class, 				"01"),
				new NamedType(RestRemoteServerProperties.class, "02"), //TODO: change name because it is not a trace event
				new NamedType(SessionMaskUpdate.class,			"03"),  
				new NamedType(SessionAsyncDurationUpdate.class,	"04"),  
				new NamedType(ExceptionTrace.class,				"05"), 
				new NamedType(SessionEvent.class,				"06"),
				new NamedType(MainSessionSignal.class,  		"10"),
				new NamedType(MainSessionUpdate.class,  		"11"), 
				new NamedType(HttpSessionSignal.class,  		"20"), 
				new NamedType(HttpSessionUpdate.class,  		"21"), 
				new NamedType(LocalRequestSignal.class, 		"110"),
				new NamedType(LocalRequestUpdate.class, 		"111"),
				new NamedType(HttpRequestSignal.class,  		"120"), 
				new NamedType(HttpRequestUpdate.class,  		"121"), 
				new NamedType(DatabaseRequestSignal.class,		"130"),
				new NamedType(DatabaseRequestUpdate.class,		"131"),
				new NamedType(FtpRequestSignal.class,		  	"140"), 
				new NamedType(FtpRequestUpdate.class,  			"141"),
				new NamedType(MailRequestSignal.class,  		"150"), 
				new NamedType(MailRequestUpdate.class,  		"151"), 
				new NamedType(DirectoryRequestSignal.class,		"160"),
				new NamedType(DirectoryRequestUpdate.class,		"161"), 
				new NamedType(HttpSessionStage.class,  			"210"), 
				new NamedType(HttpRequestStage.class,  			"220"), 
				new NamedType(DatabaseRequestStage.class,		"230"), 
				new NamedType(FtpRequestStage.class,  			"240"),
				new NamedType(MailRequestStage.class,  			"250"), 
				new NamedType(DirectoryRequestStage.class,		"260"));
	}
	
	public static JsonMapper.Builder jsonMapperBuilder() {
		return JsonMapper.builder()
                .addModule(new InspectCoreModule())
				.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .changeDefaultPropertyInclusion(incl -> incl //replace .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                        .withValueInclusion(JsonInclude.Include.NON_EMPTY)   // for standard 
                        .withContentInclusion(JsonInclude.Include.NON_EMPTY)); // for Maps/Collections
	}

}
