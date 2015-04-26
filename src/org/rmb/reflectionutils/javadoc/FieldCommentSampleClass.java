package org.rmb.reflectionutils.javadoc;

/**
 * Fill this class with fields that have {@link FieldComment}s for output of
 * getters and setters.
 *
 * @author robbram
 */
public final class FieldCommentSampleClass {

	/** This is the name of the thing. */
	@FieldComment(comment = "This is the name of the thing.")
	private String name;

	/** How old it is. */
	@FieldComment(comment = "How old it is.")
	private int age;

	private int year;

}
