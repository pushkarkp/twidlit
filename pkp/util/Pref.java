/**
 * Copyright 2015 Pushkar Piggott
 *
 * Pref.java
 */
package pkp.util;

import java.util.Properties;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.lang.reflect.Field;
import java.net.URL;
import pkp.io.Io;

///////////////////////////////////////////////////////////////////////////////
public class Pref {

   ////////////////////////////////////////////////////////////////////////////
   public static void init(String name, String parent, String jarParent) {
      m_JarParent = jarParent;
      sm_Pref = new PersistentProperties(name, parent, jarParent, Io.MustExist);
      sm_Pref.read();
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public static void writePersist() {
      Persist.write();
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public static void setIconPath(String path) {
      sm_IconPath = path;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void write() {
      sm_Pref.write();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void set(String name, String value) {
      sm_Pref.set(name, value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String get(String name) {
      return sm_Pref.get(name);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String getOrExit(String name) {
      String value = sm_Pref.get(name);
      if (value == null) {
         Log.err("\"" + name + "\" is not defined in \"" + sm_Pref.getFileName() + "\"");
      }
      return value;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String get(String name, String deflt) {
      String str = sm_Pref.get(name);
      if (str == null) {
         return deflt;
      }
      return get(name);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void set(String name, int value) {
      sm_Pref.set(name, Integer.toString(value));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int getInt(String name) {
      return Io.parseInt(name, get(name));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int getInt(String name, int deflt) {
      String str = sm_Pref.get(name);
      if (str == null) {
         return deflt;
      }
      return getInt(name);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean getBool(String name) {
      return Io.parseBool(name, get(name));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean getBool(String name, boolean deflt) {
      String str = sm_Pref.get(name);
      if (str == null) {
         return deflt;
      }
      return getBool(name);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Color getColor(String name) {
      String color = get(name);
      if (color != null) {
         try {
            Field field = Color.class.getField(color);
            return (Color)field.get(null);
         } catch (Exception e) {
            return new Color(Io.parseInt(name, color));
         }
      }
      return Color.black;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Color getColor(String name, Color deflt) {
      String str = sm_Pref.get(name);
      if (str == null) {
         return deflt;
      }
      return getColor(name);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static URL getDirJarUrl(String dirName, String fileName) {
      return Io.toUrl(fileName, Persist.get(dirName), m_JarParent);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static ImageIcon getIcon() {
      if (sm_Icon == null) {
//Io.printClassPath();         
//System.out.println(sm_IconPath);
         sm_Icon = new ImageIcon(Pref.class.getResource(sm_IconPath));
      }
      return sm_Icon;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static PersistentProperties sm_Pref;
   private static String m_JarParent;
   private static ImageIcon sm_Icon = null;
   private static String sm_IconPath = null;
}
