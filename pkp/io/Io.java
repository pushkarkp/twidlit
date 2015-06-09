/**
 * Copyright 2015 Pushkar Piggott
 *
 * Io.java
 */
package pkp.io;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import pkp.util.Pref;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class Io {

   public static final boolean MustExist = true;
   public static final int ParseFailed = Integer.MIN_VALUE;

   ////////////////////////////////////////////////////////////////////////////
   public static URL toUrl(String fName, String fileParent) {
      return toUrl(fName, fileParent, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static URL toUrl(String fName, String fileParent, String jarParent) {
      if (fileParent != null) {
         File f = createFile(fileParent, fName);
         if (f.exists()) {
            try {
//System.out.println("toUrl file:" + f);
               return f.toURI().toURL();
            } catch (MalformedURLException e) {
               Log.err("Failed to create URL from '" + f.getPath() + "' " + e);
               return null;
            }
         }
      }
      if (jarParent == null) {
         jarParent = (fileParent != null ? fileParent : "");
      }
      if (".".equals(jarParent)) {
         jarParent = "";
      }
      if (!"".equals(jarParent) && !jarParent.endsWith("/")) {
         jarParent += "/";
      }
//System.out.println("toUrl jar: " + "/" + jarParent + fName);
      return Io.class.getResource("/" + jarParent + fName);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public static void saveFromJar(String fName, String inParent, String outParent) {
      File save = new File(outParent, fName);
      if (!save.exists()) {
         InputStream fis = Io.class.getResourceAsStream("/" + inParent + "/" + fName);
         FileOutputStream fos = null;
         try {
            fos = new FileOutputStream(save);
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
               fos.write(buf, 0, i);
            }
         } catch (Exception e) {
            Log.err("Failed to copy '" + inParent + "/" + fName + "' from JAR.");
         } finally {
            try {
               if (fis != null) {
                  fis.close();
               }
               if (fos != null) {
                  fos.close();
               }
            } catch (Exception e) {} 
         }
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void printClassPath() {
      URL[] urls = ((URLClassLoader)ClassLoader.getSystemClassLoader()).getURLs();
      for(URL url: urls){
        	System.out.println(url.getFile());
      }
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public static void crash() {
      String n = null;
      n.length();
  }

   ////////////////////////////////////////////////////////////////////////////
   public static File createFile(String parent, String file) {
      return parent != null && !"".equals(parent)
             ? new File(parent, file)
             : new File(file);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public static boolean dirExists(String dir) {
      if (dir == null) {
         return false;
      }
      File f = new File(dir);
      return f.exists() && f.isDirectory();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void dirExistsOrExit(File f) {
      if (!f.exists() || !f.isDirectory()) {
         Log.err("Did not find the directory \"" + f.getPath() + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void fileExistsOrExit(File f) {
      if (!f.exists() || f.isDirectory()) {
         Log.err("Did not find the file \"" + f.getPath() + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int countFiles(File f) {
      if (f == null || !f.exists() || !f.isDirectory()) {
         return 0;
      }
      int count = 0;
      File[] files = f.listFiles();
      if (files != null) {
         for (File file : files) {
            if (!file.isDirectory()) {
               ++count;;
            }
         }
      }
      return count;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String getPath(String file) {
      return (new File(file)).getPath();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String getPath(String parent, String file) {
      return (new File(parent, file)).getPath();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String getRelativePath(String path) {
      String cd = System.getProperty("user.dir");
      if (path.startsWith(cd)) {
         int prefix = cd.length();
         if (prefix < path.length()) {
            ++prefix;
         }
         return path.substring(prefix);
      }
      return path;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String trimComment(String comment, String str) {
      int commentAt = str.indexOf(comment);
      if (commentAt == -1) {
         return str.trim();
      }
      return str.substring(0, commentAt).trim();
   }

   ////////////////////////////////////////////////////////////////////////////
   public interface StringToInt {
       int cvt(String str);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int toInt(String value) {
      if (value != null && !"".equals(value)) {
         int neg = 1;
         if (value.length() > 0 && value.charAt(0) == '-') {
            value = value.substring(1).trim();
            neg = -1;
         }
         try {
            if (value.length() > 1 && value.substring(0,2).equals("0x")) {
               return neg * Integer.parseInt(value.substring(2), 16);
            } else {
               return neg * Integer.parseInt(value);
            }
         } catch (NumberFormatException e) {}
      }
      return ParseFailed;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int parseInt(String value) {
      return parseInt("", value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int parseInt(String str, String value) {
      int result = toInt(value);
      if (result != ParseFailed) {
         return result;
      }
      if (value == null || "".equals(value)) {
         if ("".equals(str)) {
            Log.err("Missing number.");
         }
         Log.err("Missing value for \"" + str + "\".");
      } else if ("".equals(str)) {
         Log.err("Failed to parse number \"" + value + "\".");
      }
      Log.err("Failed to parse \"" + str + "\" value \"" + value + "\".");
      return ParseFailed;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean parseBool(String str) {
      return parseBool("", str);
   }


   ////////////////////////////////////////////////////////////////////////////
   public static boolean parseBool(String str, String value) {
      if (value == null) {
         return false;
      }
      if ("".equals(value)) {
         if ("".equals(str)) {
            Log.err("Missing boolean.");
         } else {
            Log.err("Missing boolean value for \"" + str + "\".");
         }
      }
      if ("true".equalsIgnoreCase(value)) {
         return true;
      }
      if ("false".equalsIgnoreCase(value)) {
         return false;
      }
      if ("".equals(str)) {
         Log.err("Failed to parse boolean \"" + str + "\".");
      } else {
         Log.err("Failed to parse \"" + str + "\" boolean value \"" + value + "\".");
      }
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String toEscaped(String str) {
      String out = "";
      for (int i = 0; i < str.length(); ++i) {
         char c = str.charAt(i);
         switch (c) {
         case '\b': out += "\\b"; break;
         case '\f': out += "\\f"; break;
         case '\n': out += "\\n"; break;
         case '\t': out += "\\t"; break;
         case '\r': out += "\\r"; break;
         case ' ': out += "\\s"; break;
         default: out += c; break;
         }
      }
      return out;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String toEscaped(char c) {
      String out = "";
      switch (c) {
      case '\b': out += "\\b"; break;
      case '\f': out += "\\f"; break;
      case '\n': out += "\\n"; break;
      case '\t': out += "\\t"; break;
      case '\r': out += "\\r"; break;
      case ' ': out += "\\s"; break;
      default: out += c; break;
      }
      return out;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String parseChars(String str) {
      if ("".equals(str)) {
         return "";
      }
      if (str.length() > 1 && isQuote(str.charAt(0)) && isQuote(str.charAt(str.length() - 1))) {
         str = str.substring(1, str.length() - 1);
      }
      int extra = 0;
      for (int i = 0; i < str.length(); ++i) {
         if (str.charAt(i) == '\\') {
            ++extra;
            ++i;
         }
      }
      byte[] seps = new byte[str.length() - extra];
      extra = 0;
      for (int i = 0; i < str.length(); ++i) {
         byte c = (byte)str.charAt(i);
         if (c != '\\') {
            seps[i - extra] = c;
         } else {
            if (str.length() <= i + 1) {
               return "";
            }
            switch (str.charAt(i + 1)) {
            case 'b': seps[i - extra] = '\b'; break;
            case 'n': seps[i - extra] = '\n'; break;
            case 'r': seps[i - extra] = '\r'; break;
            case 's': seps[i - extra] = ' '; break;
            case 't': seps[i - extra] = '\t'; break;
            case 'f': seps[i - extra] = '\f'; break;
            default: seps[i - extra] = (byte)str.charAt(i + 1); break;
            }
            ++i;
            ++extra;
         }
      }
      return new String(seps);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String toCamel(String str) {
      String out = "";
      boolean initial = false;
      for (int i = 0; i < str.length(); ++i) {
         char c = str.charAt(i);
         if (c == ' ') {
            initial = true;
         } else {
            if (initial) {
               initial = false;
               c = Character.toUpperCase(c);
            }
            out += c;
         }
      }
      return out;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean isQuote(char c) {
      return c == '"' || c == '\'';
   }
}
