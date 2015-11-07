/**
 * Copyright 2015 Pushkar Piggott
 *
 * Io.java
 */
package pkp.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class Io {

   public static final boolean sm_MUST_EXIST = true;
   public static final int sm_PARSE_FAILED = Integer.MIN_VALUE;
   public static final String sm_WS = "   ";

   ////////////////////////////////////////////////////////////////////////////
   public static int read(String prompt) {
      System.out.print(prompt);
      try {
         return System.in.read();
      } catch (IOException e) {}
      return 0;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void setComment(char comment) {
      sm_Comment = comment;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static URL toExistUrl(File f) {
      return toExistUrl(f.getName(), f.getParent(), null);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public static URL toUrl(File f) {
      return toUrl(f.getName(), f.getParent(), null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static URL toExistUrl(String fName, String fileParent, String jarParent) {
      URL url = toUrl(fName, fileParent, jarParent);
      if (url == null) {
         Log.err('"' + fName + "\" not found.");
      }
      return url;
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
      URL url = Io.class.getResource("/" + jarParent + fName);
      return url;
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
      return createFile(parent, file).getPath();
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
   public static int findFirstOf(String str, String chars) {
      return findFirstOfUpTo(str, chars, str.length());
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int findFirstOfUpTo(String str, String chars, int upTo) {
      upTo = Math.min(upTo, str.length());
      for (int i = 0; i < chars.length(); ++i) {
         int p = str.indexOf(chars.charAt(i));
         if (p < upTo) {
            upTo = p;
         }
      }
      return upTo;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int findFirstNotOf(String str, String chars) {
      return findFirstNotOfUpTo(str, chars, str.length());
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int findFirstNotOfUpTo(String str, String chars, int upTo) {
      upTo = Math.min(upTo, str.length());
      for (int j = 0; j < upTo; ++j) {
         char c = str.charAt(j);
         int i = 0;
         for (; i < chars.length(); ++i) {
            if (c == chars.charAt(i)) {
               break;
            }
         }
         if (i == chars.length()) {
            return j;
         }
      }
      return upTo;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String trimComment(String line) {
      if (sm_Comment == 0) {
         return line;
      }
      return trimToLineEnd(sm_Comment, line);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String trimToLineEnd(char from, String line) {
      int strAt = -1;
      for (;;) {
         int c = line.substring(strAt + 1).indexOf(from);
         if (c == -1) {
            // not there
            return line;
         }
         strAt += c + 1;
         // odd number of preceding '\'?
         int i = 1;
         while (strAt - i >= 0
             && line.charAt(strAt - i) == '\\') {
            ++i;
         }
         // even
         if ((i & 1) == 1) {
            return line.substring(0, strAt);
         }
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public interface StringToInt {
       int cvt(String str);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int toPosInt(String value) {
       int i = toInt(value);
       return (i >= 0) ? i : sm_PARSE_FAILED;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int toPosInt(int max, String value) {
      int i = Io.toInt(value); 
      return (i >= 0 && i <= max) ? i : Io.sm_PARSE_FAILED;
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
      return sm_PARSE_FAILED;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int parseInt(String value) {
      return parseInt("", value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int parseInt(String str, String value) {
      int result = toInt(value);
      if (result != sm_PARSE_FAILED) {
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
      return sm_PARSE_FAILED;
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
         out += toEscaped(str.charAt(i));
      }
      return out;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String toEscaped(char c) {
      switch (c) {
      case 7: return "\\a";
      case '\b': return "\\b";
      case 27: return "\\e";
      case '\f': return "\\f";
      case '\n': return "\\n";
      case '\r': return "\\r";
      case ' ': return "\\s";
      case '\t': return "\\t";
      case '\\': return "\\\\";
      case '#': return "\\#";
      case 11: return "\\v";
      case 0: return "\\0";
      default: return "" + c;
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static char parseEscaped(char c) { 
      switch (c) {
      case 'a': return 7;
      case 'b': return '\b';
      case 'e': return 27;
      case 'f': return '\f';
      case 'n': return '\n';
      case 'r': return '\r';
      case 's': return ' ';
      case 't': return '\t';
      case 'v': return 11;
      case '0': return 0;
      default: return c;
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int parseEscapedChar(String str) {
      String c = parseEscaped(str);
      if (c.length() != 1) {
         Log.err(String.format("%s->%s (%d)", str, c, c.length()));
         return -1;
      }
      return c.charAt(0) & 0xFF;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String parseEscaped(String str) {
      if ("".equals(str)) {
         return "";
      }
      char[] buf = new char[str.length()];
      int skipped = 0;
      int i = 0;
      for (; i < str.length(); ++i) {
         char c = str.charAt(i);
         if (c == '\\' && i < str.length() - 1) {
            ++i;
            ++skipped;
            if ((str.charAt(i) == 'x' || str.charAt(i) == 'X')
             && i < str.length() - 2) {
               skipped += 2;
               i += 2;
               c = (char)Integer.parseInt(str.substring(i - 1, i + 1), 16);
//System.out.printf("%s %d%n", str.substring(i - 1, i + 1), (int)c);
            } else {
               c = parseEscaped(str.charAt(i));
            }
         }
         buf[i - skipped] = c;
      }
      return new String(buf, 0, i - skipped);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean isPrintable(int ch) {
      return 32 < ch && ch < 127;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String parseQuote(String str) {
      if (str.length() > 1 && isQuote(str.charAt(0)) && isQuote(str.charAt(str.length() - 1))) {
         str = str.substring(1, str.length() - 1);
      }
      return parseEscaped(str);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean isQuote(char c) {
      return c == '"' || c == '\'';
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

   // Data ////////////////////////////////////////////////////////////////////
   private static char sm_Comment = '#';

   // Main /////////////////////////////////////////////////////////////////////
   public static void main (String[] args) {
      System.out.println(trimComment(args[0]));
   }
}
