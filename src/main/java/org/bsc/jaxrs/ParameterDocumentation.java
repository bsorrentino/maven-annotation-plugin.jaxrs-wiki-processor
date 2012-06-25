package org.bsc.jaxrs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.PARAMETER})
public @interface ParameterDocumentation {
	
	String value() default "";
	
	
}
