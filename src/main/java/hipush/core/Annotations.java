package hipush.core;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public class Annotations {

	@Documented
	@Target(METHOD)
	@Retention(SOURCE)
	public static @interface Concurrent {
		
	}
	
}
