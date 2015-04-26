package org.rmb.reflectionutils.javadoc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to store a comment on a field.
 *
 * @author robbram
 * @see http://stackoverflow.com/a/21889648/257233
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface FieldComment {

	/**
	 * Comment on a field, a.k.a. the javadoc comment in an annotation that I
	 * can read.
	 *
	 * @return
	 */
	String comment() default "";

}