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

   private String calledPartyEndLocationBearing;
   private String calledPartyStartLocationBearing;
   private String callingPartyEndLocationBearing;
   private String callingPartyStartLocationBearing;
   private String calledPartyEndLocationLatitude;
   private String calledPartyStartLocationLatitude;
   private String callingPartyEndLocationLatitude;
   private String callingPartyStartLocationLatitude;
   private String calledPartyEndLocationLongitude;
   private String calledPartyStartLocationLongitude;
   private String callingPartyEndLocationLongitude;
   private String callingPartyStartLocationLongitude;

}
