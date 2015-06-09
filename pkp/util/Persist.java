/**
 * Copyright 2015 Pushkar Piggott
 *
 * Persist.java
 */
package pkp.util;

import java.util.Properties;
import pkp.io.Io;

///////////////////////////////////////////////////////////////////////////////
public class Persist {

   ////////////////////////////////////////////////////////////////////////////
   public static void init(String fName, String parent,  String jarParent) {
      sm_Persist = new PersistentProperties(fName, parent, jarParent, Io.MustExist);
      sm_Persist.read();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void write() {
      sm_Persist.write();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void set(String name, String value) {
      sm_Persist.set(name, value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String get(String name) {
      return sm_Persist.get(name);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String get(String name, String defaultValue) {
      if (sm_Persist == null && !sm_Persist.mustExist()) {
         return defaultValue;
      }
      String value = sm_Persist.get(name);
      if (value == null) {
         return defaultValue;
      }
      return value;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void set(String name, int value) {
      sm_Persist.set(name, Integer.toString(value));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int getInt(String name) {
      return Io.parseInt(name, sm_Persist.get(name));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int getInt(String name, int defaultValue) {
      if (sm_Persist == null && !sm_Persist.mustExist()) {
         return defaultValue;
      }
      String value = sm_Persist.get(name);
      if (value == null) {
         return defaultValue;
      }
      return Io.parseInt(name, value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void set(String name, boolean value) {
      Io.parseInt(name, Boolean.toString(value));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean getBool(String name) {
      return Io.parseBool(name, sm_Persist.get(name));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean getBool(String name, boolean defaultValue) {
      if (sm_Persist == null && !sm_Persist.mustExist()) {
         return defaultValue;
      }
      String value = sm_Persist.get(name);
      if (value == null) {
         return defaultValue;
      }
      return Io.parseBool(name, value);
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static PersistentProperties sm_Persist;
}
