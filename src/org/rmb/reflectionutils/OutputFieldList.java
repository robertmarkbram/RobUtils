package org.rmb.reflectionutils;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.rmb.reflectionutils.OutputFieldList.OutputFields.OUTPUT_FIELDS;
import static org.rmb.reflectionutils.OutputFieldList.OutputGetters.OUTPUT_GETTERS;
import static org.rmb.reflectionutils.OutputFieldList.OutputSetters.OUTPUT_SETTERS;
import static org.rmb.reflectionutils.OutputFieldList.ShowParameters.INCLUDE_PARAMS;
import static org.rmb.reflectionutils.OutputFieldList.TypeLocation.TYPE_AT_END;
import static org.rmb.reflectionutils.OutputFieldList.TypeLocation.TYPE_AT_START;
import static org.rmb.reflectionutils.OutputFieldList.TypeOutput.SIMPLE;
import static org.rmb.reflectionutils.OutputFieldList.WithType.INCLUDE_TYPE;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.rmb.reflectionutils.javadoc.FieldComment;
import org.rmb.reflectionutils.javadoc.FieldCommentSampleClass;

/**
 * <p>
 * Output list of fields, methods etc for a given class.
 * </p>
 *
 * @author robbram
 */
@SuppressWarnings("rawtypes")
public final class OutputFieldList {

   // TODO rmb - create settings object to cut down on parameters.
   // TODO rmb - add ability to specify object name for get/set lists.

   /** Class not meant for external instantiation. */
   private OutputFieldList() {

   }

   /**
    * Recursive method to add get methods to list for class. Goes up the
    * supertype chain and stops at Object (skipping all Object fields). Ignores
    * static fields.
    *
    * @param clazz
    *           current class to examine
    * @param membersByClass
    *           map of member names names by class
    * @param withType
    *           include type in output?
    * @param typeLocation
    *           output return type at end of each line or at start of each line
    *           (as you would expect in an actual method declaration).
    * @param typeOutput
    *           how to output class name
    */
   private static void addFieldNamesToList(final Class clazz,
         final Map<Class, List<String>> membersByClass,
         final WithType withType, final TypeLocation typeLocation,
         final TypeOutput typeOutput) {
      // Finished once we get to Object.
      if (clazz.getName().equals(Object.class.getName())) {
         return;
      }
      // Add fields for this class.
      List<Field> declaredFields =
            Arrays.asList(clazz.getDeclaredFields()).stream()
                  .filter(field -> !Modifier.isStatic(field.getModifiers()))
                  .collect(Collectors.toList());

      List<String> memberNames = new ArrayList<String>();
      for (Field field : declaredFields) {
         String fieldName = field.getName();
         if (withType == INCLUDE_TYPE) {
            String type = getClassName(field.getType(), typeOutput);
            if (typeLocation == TYPE_AT_END) {
               fieldName += " // " + type;
            } else {
               fieldName = type + " " + fieldName;
            }
         }
         memberNames.add(fieldName);
      }
      membersByClass.put(clazz, memberNames);

      // Add field for the superclass.
      addFieldNamesToList(clazz.getSuperclass(), membersByClass, withType,
            typeLocation, typeOutput);
   }

   /**
    * Recursive method to add get methods to list for class. Goes up the
    * supertype chain and stops at Object (skipping all Object fields).
    *
    * @param clazz
    *           current class to examine
    * @param membersByClass
    *           map of member names names by class
    * @param typeLocation
    *           output return type at end of each line or at start of each line
    *           (as you would expect in an actual method declaration).
    * @param withType
    *           include return type in output?
    * @param typeOutput
    *           how to output class name
    */
   private static void addGetMethodNamesToList(final Class clazz,
         final Map<Class, List<String>> membersByClass,
         final WithType withType, final TypeLocation typeLocation,
         final TypeOutput typeOutput) {
      // Finished once we get to Object.
      if (clazz.getName().equals(Object.class.getName())) {
         return;
      }
      // Add fields for this class.
      List<Method> declaredMembers = Arrays.asList(clazz.getDeclaredMethods());
      List<String> memberNames = new ArrayList<String>();
      for (Method method : declaredMembers) {
         extractGetMethod(withType, typeLocation, memberNames, method,
               typeOutput);
      }
      membersByClass.put(clazz, memberNames);

      // Add field for the superclass.
      addGetMethodNamesToList(clazz.getSuperclass(), membersByClass, withType,
            typeLocation, typeOutput);
   }

   /**
    * Recursive method to add set methods to list for class. Goes up the
    * supertype chain and stops at Object (skipping all Object fields).
    *
    * @param clazz
    *           current class to examine
    * @param membersByClass
    *           map of member names names by class
    * @param showParameters
    *           include parameters in report?
    * @param withType
    *           include parameter type in report?
    * @param typeOutput
    *           how to output class name
    * @param objectName
    *           to prepend sets with so that they will come out as
    *           <code>objectName.setFoo(bar);</code>.f
    */
   private static void addSetMethodNamesToList(final Class clazz,
         final Map<Class, List<String>> membersByClass,
         final ShowParameters showParameters, final WithType withType,
         final TypeOutput typeOutput, final String objectName) {
      // Finished once we get to Object.
      if (clazz.getName().equals(Object.class.getName())) {
         return;
      }
      // Add fields for this class.
      List<Method> declaredMethods = Arrays.asList(clazz.getDeclaredMethods());
      List<String> memberNames = new ArrayList<String>();
      for (Method method : declaredMethods) {
         extractSetMethod(showParameters, memberNames, method, withType,
               typeOutput, objectName);
      }
      membersByClass.put(clazz, memberNames);

      // Add field for the superclass.
      addSetMethodNamesToList(clazz.getSuperclass(), membersByClass,
            showParameters, withType, typeOutput, objectName);
   }

   /**
    * @param withType
    *           include return type in output?
    * @param typeLocation
    *           output return type at end of each line or at start of each line
    *           (as you would expect in an actual method declaration).
    * @param memberNames
    *           name of the get method members in this class
    * @param method
    *           that we are looking now
    * @param typeOutput
    *           how to output class name
    */
   private static void extractGetMethod(final WithType withType,
         final TypeLocation typeLocation, final List<String> memberNames,
         final Method method, final TypeOutput typeOutput) {
      String methodName = method.getName();
      if ((methodName.startsWith("get") || methodName.startsWith("is"))
            && !methodName.equals("getClass")) {
         if (withType == INCLUDE_TYPE) {
            if (typeLocation == TYPE_AT_END) {
               memberNames.add(methodName + "(); // "
                     + getClassName(method.getReturnType(), typeOutput));
            } else {
               memberNames.add(getClassName(method.getReturnType(), typeOutput)
                     + " " + methodName + "();");
            }
         } else {
            memberNames.add(methodName + "();");
         }
      }
   }

   /**
    * @param showParameters
    *           include parameters in report?
    * @param memberNames
    *           name of the get method members in this class
    * @param method
    *           that we are looking now
    * @param withType
    *           include parameter type in report?
    * @param typeOutput
    *           how to output class name
    * @param objectName
    *           to prepend sets with so that they will come out as
    *           <code>objectName.setFoo(bar);</code>.f
    */
   private static void extractSetMethod(final ShowParameters showParameters,
         final List<String> memberNames, final Method method,
         final WithType withType, final TypeOutput typeOutput,
         final String objectName) {
      String methodName = method.getName();
      if (methodName.startsWith("set") && !methodName.equals("getClass")) {
         if (showParameters == INCLUDE_PARAMS) {
            String memberString = methodName + "(";
            if (isNotBlank(objectName)) {
               memberString = objectName + "." + memberString;
            }
            Parameter[] parameters = method.getParameters();
            String typeString = "";
            for (int index = 0; index < parameters.length; index++) {
               Parameter parameter = parameters[index];
               typeString += getClassName(parameter.getType(), typeOutput);
               // Remove "set".
               String paramName = method.getName().replaceFirst("set", "");
               // Lowercase first letter.
               String firstLetterUpper = paramName.substring(0, 1);
               String firstLetterLower = firstLetterUpper.toLowerCase();
               paramName =
                     paramName.replaceFirst(firstLetterUpper, firstLetterLower);
               memberString += paramName;

               // Doesn't work. Just gives arg0, arg1 etc. Use method name.
               // if (parameter.isNamePresent()) {
               // memberString += " " + parameter.getName();
               // }
               if (index < parameters.length - 1) {
                  memberString += ", ";
                  typeString += ", ";
               }
            }
            memberString += ");";
            if (withType.equals(INCLUDE_TYPE)) {
               memberString += " // " + typeString;
            }
            memberNames.add(memberString);
         } else {
            memberNames.add(methodName);
         }
      }

   }

   /**
    * Comment will come from the {@link FieldComment} annotation or generated
    * from field name.
    *
    * @param field
    *           in the class we are generating getters and setters for.
    * @return comment from the field contents.
    */
   private static String generateCommentForField(final Field field) {
      String comment = "";
      final FieldComment fieldComment = field.getAnnotation(FieldComment.class);
      if (fieldComment != null) {
         comment = fieldComment.comment();
      } else {
         comment = "the " + field.getName() + " which is a " //
               + getClassName(field.getType(), SIMPLE);
      }

      return comment;
   }

   /**
    * Comment will come from the {@link FieldComment} annotation or generated
    * from field name. <code>SOME_STATIC_FIELD</code> should have a comment of
    * "Some static field."
    *
    * @param field
    *           in the class we are generating getters and setters for.
    * @param replacements
    *           list of replacements to make to the comments. Can be null or
    *           empty, in which case it will be ignored.
    * @return comment from the field contents.
    */
   private static String generateCommentForStaticField(final Field field,
         final List<CommentReplacement> replacements) {
      String comment = "";
      final FieldComment fieldComment = field.getAnnotation(FieldComment.class);
      if (fieldComment != null) {
         comment = fieldComment.comment();
      } else {
         comment = field.getName().toLowerCase().replace('_', ' ') + ".";
      }

      if (replacements != null && !replacements.isEmpty()) {
         for (CommentReplacement replacement : replacements) {
            final String result = replacement.apply(comment);
            if (result != null) {
               comment = result;
            }
         }
      }

      final String commentFirstLetter = comment.substring(0, 1);
      comment =
            comment.replaceFirst(commentFirstLetter,
                  commentFirstLetter.toUpperCase());

      return comment;
   }

   /**
    * Generate get and set methods with comments for all declared non-static
    * fields in a class. If a field is annotated with {@link FieldComment},
    * javadoc comments will be generated using the contents of the
    * <code>comment</code> attribute in that annotation.
    *
    * @param clazz
    *           class to examine
    * @param outputFields
    *           should fields be output?
    * @param outputGetters
    *           should getters, a.k.a. accessors be output?
    * @param outputSetters
    *           should setters, a.k.a. mutators be output?
    */
   public static void generateGetAndSetMethods(final Class clazz,
         final OutputFields outputFields, final OutputGetters outputGetters,
         final OutputSetters outputSetters) {
      // Add fields for this class.
      List<Field> declaredFields =
            Arrays.asList(clazz.getDeclaredFields()).stream()
                  .filter(field -> !Modifier.isStatic(field.getModifiers()))
                  .collect(Collectors.toList());
      StringBuilder fields = new StringBuilder();
      StringBuilder getters = new StringBuilder();
      StringBuilder setters = new StringBuilder();

      for (Field field : declaredFields) {
         String type = getClassName(field.getType(), SIMPLE);
         // Is type a generic type?
         // TODO rmb doesn't work. Why?
         String typeGenericSt = null;
         Type genericSuperclass = field.getClass().getGenericSuperclass();
         if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType typeGeneric =
                  (ParameterizedType) field.getGenericType();
            Class<?> typeGenericClass =
                  (Class<?>) typeGeneric.getActualTypeArguments()[0];
            typeGenericSt = getClassName(typeGenericClass, SIMPLE);
         }

         String name = field.getName();
         String comment = generateCommentForField(field);
         String commentFirstLetter = comment.substring(0, 1);
         String lowerCaseComment = comment.replaceFirst(commentFirstLetter, //
               commentFirstLetter.toLowerCase());
         String nameFirstLetter = name.substring(0, 1);
         String capitalName = name.replaceFirst(//
               nameFirstLetter, nameFirstLetter.toUpperCase());

         fields.append("   /** ").append(comment).append(" */\n   private ")
               .append(type);
         if (isNotBlank(typeGenericSt)) {
            fields.append("<").append(typeGenericSt).append(">");
         }
         fields.append(" ").append(name).append(";\n\n");

         getters.append("   /** @return ").append(lowerCaseComment)
               .append(" */\n   public ").append(type);
         if (isNotBlank(typeGenericSt)) {
            getters.append("<").append(typeGenericSt).append(">");
         }
         getters.append(" get").append(capitalName)
               .append("() {\n      return ").append(name)
               .append(";\n   }\n\n");

         setters.append("   /** @param the").append(capitalName).append(" ")
               .append(lowerCaseComment).append(" */\n   public void set")
               .append(capitalName).append("(final ").append(type);
         if (isNotBlank(typeGenericSt)) {
            setters.append("<").append(typeGenericSt).append(">");
         }
         setters.append(" the").append(capitalName).append(") {\n      this.")
               .append(name).append(" = the").append(capitalName)
               .append(";\n   }\n\n");
      }
      if (outputFields.equals(OUTPUT_FIELDS)) {
         System.out.print(fields);
      }
      if (outputGetters.equals(OUTPUT_GETTERS)) {
         System.out.print(getters);
      }
      if (outputSetters.equals(OUTPUT_SETTERS)) {
         System.out.print(setters);
      }
   }

   /**
    * Generate static fields with comments.
    *
    * @param clazz
    *           class to examine
    * @param replacements
    *           list of replacements to make to the comments. Can be null or
    *           empty, in which case it will be ignored.
    * @throws Exception
    *            if we are unable to examine a field's value.
    */
   public static void generateStaticFieldComments(final Class clazz,
         final List<CommentReplacement> replacements) throws Exception {
      // Add fields for this class.
      List<Field> declaredFields =
            Arrays.asList(clazz.getDeclaredFields()).stream()
                  .filter(field -> Modifier.isStatic(field.getModifiers()))
                  .collect(Collectors.toList());
      StringBuilder fields = new StringBuilder();

      for (Field field : declaredFields) {
         String type = getClassName(field.getType(), SIMPLE);
         // Is type a generic type?
         // TODO rmb doesn't work. Why?
         String typeGenericSt = null;
         Type genericSuperclass = field.getClass().getGenericSuperclass();
         if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType typeGeneric =
                  (ParameterizedType) field.getGenericType();
            Class<?> typeGenericClass =
                  (Class<?>) typeGeneric.getActualTypeArguments()[0];
            typeGenericSt = getClassName(typeGenericClass, SIMPLE);
         }

         final String name = field.getName();
         final String comment =
               generateCommentForStaticField(field, replacements);

         fields.append("   /** ")//
               .append(comment)//
               .append(" */\n   ")//
               .append(Modifier.toString(field.getModifiers())).append(" ")//
               .append(type);
         if (isNotBlank(typeGenericSt)) {
            fields.append("<").append(typeGenericSt).append(">");
         }
         fields.append(" ").append(name)//
               .append(" = ")//
               .append(getFieldValue(field))//
               .append(";\n\n");

      }
      System.out.print(fields);
   }

   /**
    * @param field
    *           field which should have a value
    * @return field value as a string for output
    * @throws Exception
    *            if we cannot access field value.
    */
   private static String getFieldValue(final Field field) throws Exception {
      if (!Modifier.isStatic(field.getModifiers())) {
         throw new IllegalStateException(
               "Do not currently handle getting field"
                     + " value for instance fields.");
      }

      if (field.getType().isAssignableFrom(String.class)) {
         return "\"" + field.get(null).toString() + "\"";
      } else {
         return field.get(null).toString();
      }
   }

   /**
    * @param clazz
    *           class whose name we want
    * @param typeOutput
    *           how to output class name
    * @return name or simple name depending on {@link #fullTypeNames}.
    */
   private static String getClassName(final Class clazz,
         final TypeOutput typeOutput) {
      switch (typeOutput) {
         case SIMPLE:
            return clazz.getSimpleName();
         default:
            return clazz.getName();
      }
   }

   /**
    * @param clazz
    *           class whose name we want (as string class name)
    * @param typeOutput
    *           how to output class name
    * @return name or simple name depending on {@link #fullTypeNames}.
    */
   private static String getClassNameFromString(final String clazz,
         final TypeOutput typeOutput) {
      switch (typeOutput) {
         case SIMPLE:
            return clazz.replaceAll(".*[.]", "");
         default: // FULL
            return clazz;
      }
   }

   /**
    * Output list of field names.
    *
    * @param clazz
    *           class to output fields from
    * @param withType
    *           include type in output?
    * @param typeLocation
    *           output return type at end of each line or at start of each line
    *           (as you would expect in an actual method declaration).
    * @param typeOutput
    *           how to output class name
    * @throws Exception
    *            if something goes wrong with reflection
    */
   public static void listFields(final Class clazz, final WithType withType,
         final TypeLocation typeLocation, final TypeOutput typeOutput)
         throws Exception {
      Map<Class, List<String>> membersByClass =
            new HashMap<Class, List<String>>();
      addFieldNamesToList(clazz, membersByClass, INCLUDE_TYPE, typeLocation,
            typeOutput);
      System.out.println("\n\n=============== FIELDS ===============");
      outputMemberNames(clazz, membersByClass, typeOutput);
   }

   /**
    * Output list of all get methods in class.
    *
    * @param clazz
    *           class you want to examine
    * @param withType
    *           include return type in report?
    * @param typeLocation
    *           true: output has return type at end of each line; false: output
    *           includes return type at start of each line (as you would expect
    *           in an actual method declaration).
    * @param typeOutput
    *           how to output class name
    * @throws Exception
    *            if something goes wrong with reflection
    */
   public static void listGetMethods(final Class clazz,
         final WithType withType, final TypeLocation typeLocation,
         final TypeOutput typeOutput) throws Exception {
      Map<Class, List<String>> membersByClass =
            new HashMap<Class, List<String>>();
      addGetMethodNamesToList(clazz, membersByClass, withType, typeLocation,
            typeOutput);
      System.out.println("\n\n=============== GET METHODS ===============");
      outputMemberNames(clazz, membersByClass, typeOutput);
   }

   /**
    * Output list of all set methods in class.
    *
    * @param clazz
    *           class you want to examine
    * @param showParameters
    *           include parameters in report?
    * @param withType
    *           include parameter type in report?
    * @param typeOutput
    *           how to output class name
    * @param objectName
    *           to prepend sets with so that they will come out as
    *           <code>objectName.setFoo(bar);</code>.f
    * @throws Exception
    *            if something goes wrong with reflection
    */
   public static void listSetMethods(final Class clazz,
         final ShowParameters showParameters, final WithType withType,
         final TypeOutput typeOutput, final String objectName)//
         throws Exception {
      Map<Class, List<String>> membersByClass =
            new HashMap<Class, List<String>>();
      addSetMethodNamesToList(clazz, membersByClass, //
            showParameters, withType, typeOutput, objectName);
      System.out.println("\n\n=============== SET METHODS ===============");
      outputMemberNames(clazz, membersByClass, typeOutput);
   }

   /**
    * @param args
    *           not used
    * @throws Exception
    *            for any exceptions from reflection.
    */
   public static void main(final String[] args) throws Exception {

      generateStaticFieldComments(FieldCommentSampleClass.class,
            getReplacements1());

      final boolean no = false;
      if (no) {
         generateStaticFieldComments(FieldCommentSampleClass.class,
               getReplacements1());
         generateGetAndSetMethods(FieldCommentSampleClass.class, //
               OutputFields.NO_FIELDS, //
               OutputGetters.OUTPUT_GETTERS, //
               OutputSetters.OUTPUT_SETTERS);
         outputToStringHashAndEquals(FieldCommentSampleClass.class);
         listGetMethods(FieldCommentSampleClass.class, INCLUDE_TYPE,
               TYPE_AT_START, SIMPLE);
         listSetMethods(FieldCommentSampleClass.class, INCLUDE_PARAMS,
               WithType.INCLUDE_TYPE, SIMPLE, null);
         listFields(FieldCommentSampleClass.class, INCLUDE_TYPE, TYPE_AT_START,
               SIMPLE);
      }
   }

   /**
    * @param clazz
    *           class you want to examine
    * @param membersByClass
    *           map of member names names by class - may be get or set methods
    * @param typeOutput
    *           how to output class name
    * @throws Exception
    *            if something goes wrong with reflection
    */
   private static void outputMemberNames(final Class clazz,
         final Map<Class, List<String>> membersByClass,
         final TypeOutput typeOutput) throws Exception {

      List<String> memberNames = new ArrayList<String>(membersByClass.size());
      List<String> output = new ArrayList<String>();

      // Go through list by class, collecting the output to print later.
      for (Map.Entry<Class, List<String>> fieldsInClass : membersByClass
            .entrySet()) {
         Class key = fieldsInClass.getKey();
         List<String> value = fieldsInClass.getValue();
         memberNames.addAll(value);
         output.add("\n   ---- "
               + getClassNameFromString(key.getName(), typeOutput) + " ----");
         for (String fieldName : value) {
            output.add("   " + fieldName);
         }
      }

      // First we print the entire list.
      Collections.sort(memberNames);
      System.out.println("---- All inherited for "
            + getClassName(clazz, typeOutput) + " ----");
      for (String fieldName : memberNames) {
         System.out.println(fieldName);
      }

      // Now we print out the lists of members by class name.
      for (String line : output) {
         System.out.println(line);
      }
   }

   /**
    * Output string including toString(), hashCode() and equals(). NOT recursive
    * and only does it for instance fields (not static ones).
    *
    * @param clazz
    *           class to output fields from
    */
   public static void outputToStringHashAndEquals(final Class clazz) {
      List<Field> instanceFields =
            Arrays.asList(clazz.getDeclaredFields()).stream()
                  .filter(field -> !Modifier.isStatic(field.getModifiers()))
                  .collect(Collectors.toList());

      // @formatter:off
		// toString.
		StringBuilder toString = new StringBuilder();
		toString.append("	@Override\n");
		toString.append("	public String toString() {\n");
		toString.append("		org.apache.commons.lang.builder.ToStringBuilder"
				+ ".setDefaultStyle(org.apache.commons.lang.builder"
				+ ".ToStringStyle.SHORT_PREFIX_STYLE);\n");
		toString.append("		// @formatter:off\n");
		toString.append("		return new org.apache.commons.lang.builder"
				+ ".ToStringBuilder(this)\n");

		// Hashcode.
		StringBuilder hashCode = new StringBuilder();
		hashCode.append("	@Override\n");
		hashCode.append("	public int hashCode() {\n");
		hashCode.append("		// @formatter:off\n");
		hashCode.append("		return new org.apache.commons.lang.builder"
				+ ".HashCodeBuilder()\n");

		// Equals.
		StringBuilder equals = new StringBuilder();
		equals.append("	@Override\n");
		equals.append("	public boolean equals(final Object obj) {\n");
		equals.append("		if (obj == this) {\n");
		equals.append("			return true; // test for reference equality\n");
		equals.append("		}\n");
		equals.append("		if (obj == null) {\n");
		equals.append("			return false; // test for null\n");
		equals.append("		}\n");
		equals.append("		if (obj instanceof ");
		equals.append(getClassName(clazz, SIMPLE));
		equals.append(") {\n");
		equals.append("			final ");
		equals.append(getClassName(clazz, SIMPLE));
		equals.append(" other = (");
		equals.append(getClassName(clazz, SIMPLE));
		equals.append(") obj;\n");
		equals.append("			// @formatter:off\n");
		equals.append("			return new org.apache.commons.lang.builder"
				+ ".EqualsBuilder()\n");
		// @formatter:on

      for (Field field : instanceFields) {
         String fieldName = field.getName();

         // Tostring.
         toString.append("			.append(\"");
         toString.append(fieldName);
         toString.append("\", ");
         toString.append(fieldName);
         toString.append(")\n");

         // Hashcode.
         hashCode.append("				.append(");
         hashCode.append(fieldName);
         hashCode.append(")\n");

         // Equals.
         equals.append("					.append(");
         equals.append(fieldName);
         equals.append(", other.");
         equals.append(fieldName);
         equals.append(")\n");

      }

      // Finish toString.
      toString.append("			.toString();\n");
      toString.append("		// @formatter:on\n");
      toString.append("	}\n");

      // Finish hashCode.
      hashCode.append("				.toHashCode();\n");
      hashCode.append("		// @formatter:on\n");
      hashCode.append("	}\n");

      // Finish equals.
      equals.append("					.isEquals();\n");
      equals.append("			// @formatter:on\n");
      equals.append("		} else {\n");
      equals.append("			return false;\n");
      equals.append("		}\n");
      equals.append("	}\n");

      System.out.println(equals);
      System.out.println(hashCode);
      System.out.println(toString);

   }

   /** Output fields in outputs? */
   public enum OutputFields {
      /** Output fields. */
      OUTPUT_FIELDS,
      /** Don't output fields. */
      NO_FIELDS;
   }

   /** Output get methods, accessors, in outputs? */
   public enum OutputGetters {
      /** Output get methods, accessors. */
      OUTPUT_GETTERS,
      /** Don't output get methods, accessors. */
      NO_GETTERS;
   }

   /** Output set methods, mutators, in outputs? */
   public enum OutputSetters {
      /** Output set methods, mutators. */
      OUTPUT_SETTERS,
      /** Don't output set methods, mutators. */
      NO_SETTERS;
   }

   /** Include parameters in get methods? */
   public enum ShowParameters {
      /** Include parameters. */
      INCLUDE_PARAMS,
      /** Do not include parameters. */
      NO_PARAMS;
   }

   /** Include type information at the end of the string (gets and fields)? */
   public enum TypeLocation {
      /** Put type information at end. */
      TYPE_AT_END,
      /** Put type information at start. */
      TYPE_AT_START;
   }

   /**
    * <p>
    * Output fully qualified type names via getName() or simple name via
    * getSimpleName().
    * </p>
    * <ul>
    * <li>String.class.getSimpleName() gives String</li>
    * <li>String.class.getName() gives java.lang.String</li>
    * </ul>
    */
   public enum TypeOutput {
      /** Fully qualified type names via getName(). */
      FULL,
      /** Simple name via getSimpleName(). */
      SIMPLE;
   }

   /** Include type information in outputs? */
   public enum WithType {
      /** Output type information. */
      INCLUDE_TYPE,
      /** Don't output type information. */
      NO_TYPE;
   }

   /**
    * Replace a token in a comment with something else.
    */
   public static final class CommentReplacement {

      /** Pattern to look for. */
      private final Pattern pattern;

      /** Token that will be inserted instead. */
      private final String replacementValue;

      /**
       * @param thePattern
       *           regex pattern to look for
       * @param theReplacementValue
       *           value to replace it with
       */
      public CommentReplacement(final String thePattern,
            final String theReplacementValue) {
         pattern = Pattern.compile(thePattern);
         replacementValue = theReplacementValue;
      }

      /**
       * Apply replacement pattern to <code>comment</code> and return result.
       *
       * @param comment
       *           that we wish to apply this replacement strategy to.
       * @return result of replacement or <b>null</b> if no match was found
       */
      public String apply(final String comment) {
         final Matcher matcher = pattern.matcher(comment);
         if (matcher.find()) {
            return matcher.replaceAll(replacementValue);
         } else {
            return null;
         }
      }

      @Override
      public String toString() {
         return "Replace [" + pattern.toString() + "] with ["
               + replacementValue + "].";
      }
   }

   /**
    * KEY, NUM.
    *
    * @return first list of replacements.
    */
   private static List<CommentReplacement> getReplacements1() {
      List<CommentReplacement> replacements =
            new ArrayList<OutputFieldList.CommentReplacement>();
      replacements.add(new CommentReplacement("^key ", "Key to field: "));
      replacements.add(new CommentReplacement("^num ", "Number: "));
      return replacements;
   }

}
