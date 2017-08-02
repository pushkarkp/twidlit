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
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.ArrayList;
import java.lang.NumberFormatException;
import java.util.List;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class Io {

   public static final boolean sm_MUST_EXIST = true;
   public static final boolean sm_SINGLE_VALUE = true;
   public static final int sm_PARSE_FAILED = Integer.MIN_VALUE;
   public static final char sm_COMMENT = '#';
   public static final String sm_WS = "   ";
   public static final String sm_DIGIT = "0123456789";
   public static final String sm_HEX_DIGIT = "0123456789abcdefABCDEF";

   ////////////////////////////////////////////////////////////////////////////
   public static int read(String prompt) {
      System.out.print(prompt);
      try {
         return System.in.read();
      } catch (IOException e) {}
      return 0;
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

   ////////////////////////////////////////////////////////////////////////////////
   public static List<String> listFromCodeJarFolder(String folderName) {
      CodeSource src = Io.class.getProtectionDomain().getCodeSource();
      if (src == null) {
         return null;
      }
      return listFromJarFolder(src.getLocation(), folderName);
   }

   ////////////////////////////////////////////////////////////////////////////////
   public static List<String> listFromJarFolder(URL url, String prefix) {
      ArrayList<String> strs = new ArrayList<String>();
      if (!"".equals(prefix) && !prefix.endsWith("/")) {
         prefix = prefix + '/';
      }
      try {
         ZipInputStream zip = new ZipInputStream(url.openStream());
         ZipEntry e = null;
         while((e = zip.getNextEntry()) != null) {
            if (e.getName().startsWith(prefix)) {
               strs.add(e.getName().substring(prefix.length()));
            }
         }
      } catch (IOException e) {
         Log.err("Failed to get jar resources: " + e);
      }
      return strs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String readLineFromCodeJar(int line, String jarPath) {
      return read(line, line, Io.class.getResourceAsStream(jarPath)).get(0);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static List<String> read(int from, int to, InputStream in) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      ArrayList<String> strs = new ArrayList<String>();
      try {
         String line;
         for (int i = 0;
              i <= to && (line = reader.readLine()) != null;
              ++i) {
            if (i >= from) {
               strs.add(line);
            }
         }
      } catch (IOException e) {}
      return strs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void saveFromCodeJar(String fName, String inParent, String outParent) {
      File save = new File(outParent, fName);
      InputStream fis = Io.class.getResourceAsStream('/' + inParent + '/' + fName);
      FileOutputStream fos = null;
      try {
         fos = new FileOutputStream(save);
         byte[] buf = new byte[1024];
         int i = 0;
         while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
         }
      } catch (Exception e) {
         Log.err("Failed to copy '" + inParent + "/" + fName + "' from JAR: " + e);
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
   public static boolean fileExists(File f) {
      return f != null && f.exists() && !f.isDirectory();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean dirExists(File f) {
      return f != null && f.exists() && f.isDirectory();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void dirExistsOrExit(File f) {
      if (!dirExists(f)) {
         Log.err("Did not find the directory \"" + f.getPath() + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void fileExistsOrExit(File f) {
      if (!fileExists(f)) {
         Log.err("Did not find the file \"" + f.getPath() + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static List<File> listAllFilesInTree(File f) {
      ArrayList<File> files = new ArrayList<File>();
      if (f == null || !f.exists()) {
         return files;
      }
      if (f.isFile()) {
         files.add(f);
         return files;
      }
      for (File file : f.listFiles()) {
         if (file.isFile()) {
            files.add(file);
         } else if (file.isDirectory()) {
            files.addAll(listAllFilesInTree(file));
         }
      }
      return files;
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
   public static File asRelative(File f) {
      return new File(getRelativePath(f.getPath()));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String getRelativePath(String path) {
      String ud = System.getProperty("user.dir");
      if (path.startsWith(ud)) {
         int prefix = ud.length();
         if (prefix < path.length()) {
            ++prefix;
         }
         path = path.substring(prefix);
      }
      if ("".equals(path)) {
         return ".";
      }
      return path;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void write(File f, String str) {
		BufferedWriter bw;
      try {
         bw = new BufferedWriter(new FileWriter(f));
         bw.write(str);
			bw.flush();
         bw.close();
      } catch (IOException e) {
         Log.warn("Io failed to write to \"" + f.getPath() + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static List<String> split(String str, char c) {
      String ch = String.valueOf(c);
      ArrayList<String> als = new ArrayList<String>();
      int start = 0;
      do {
         while (start < str.length() && str.charAt(start) == c) {
            ++start;
         }
         str = str.substring(start);
         int end = findFirstOf(str, ch);
         String found = str.substring(0, end);
         if (!"".equals(found)) {
            als.add(found);
         }
         start = end;
      } while (start < str.length());
      return als;
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
         if (p != -1 && p < upTo) {
            upTo = p;
         }
      }
//System.out.printf("\"%s\" '%s' %d%n", str, chars, upTo);
      return upTo == -1 ? str.length() : upTo;
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
      if (sm_COMMENT == 0) {
         return line;
      }
      return trimToLineEnd(sm_COMMENT, line);
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
   public interface StringToIntsErr {
      int[] cvt(String str, StringBuilder err);
   }

   ////////////////////////////////////////////////////////////////////////////
   public interface StringToIntErr {
      int cvt(String str, StringBuilder err);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static final StringToIntErr sm_parseIntErr =
      new StringToIntErr() {
         public int cvt(String str, StringBuilder err) {
            return toInt(str, err);
         }
      };

   ////////////////////////////////////////////////////////////////////////////
   public interface StringToInts {
      int[] cvt(String str);
   }

   ////////////////////////////////////////////////////////////////////////////
   public interface StringToInt {
      int cvt(String str);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static final StringToInt sm_parseInt = 
      new StringToInt() {
         public int cvt(String str) {
            return toInt(str);
         }
      };

   ////////////////////////////////////////////////////////////////////////////
   public static int toPosInt(String value) {
      return toPosInt(value, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int toPosInt(String value, StringBuilder err) {
      int i = toInt(value, err);
      if (i >= 0) {
         return i;
      }
      Log.err(err, String.format("Expecting a positive integer, got %d", i));
      return sm_PARSE_FAILED;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int toPosInt(int max, String value) {
      return toPosInt(max, value, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int toPosInt(int max, String value, StringBuilder err) {
      int i = toInt(value, err);
      if (i >= 0 && i <= max) {
         return i;
      }
      Log.err(err, String.format("Expecting an integer in [0..%d], got %d", max, i));
      return sm_PARSE_FAILED;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int toIntWarnParse(String value, StringBuilder err) {
      return toIntWarnParse(value, 10, err);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int toIntWarnParse(String value, int base, StringBuilder err) {
      try {
         return Integer.parseInt(value, base);
      } catch (NumberFormatException e) {
         Log.warn(err, "Failed to parse \"" + value + "\" to integer");
         return sm_PARSE_FAILED;
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int toInt(String value) {
      return toInt(value, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int toInt(String value, StringBuilder err) {
      if (value != null && !"".equals(value)) {
         int neg = 1;
         if (value.length() > 0 && value.charAt(0) == '-') {
            value = value.substring(1).trim();
            neg = -1;
         }
         try {
            int parsed;
            if (value.length() > 1 && value.substring(0,2).equals("0x")) {
               parsed = toIntWarnParse(value.substring(2), 16, err);
            } else {
               parsed = toIntWarnParse(value, err);
            }
            return parsed == sm_PARSE_FAILED ? parsed : neg * parsed;
         } catch (NumberFormatException e) {}
      }
      return sm_PARSE_FAILED;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int parseInt(String value) {
      return parseInt("", value, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int parseInt(String value, StringBuilder err) {
      return parseInt("", value, err);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int parseInt(String str, String value) {
      return parseInt(str, value, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int parseInt(String str, String value, StringBuilder err) {
      int result = toInt(value, err);
      if (result != sm_PARSE_FAILED) {
         return result;
      }
      if (value == null || "".equals(value)) {
         Log.err(err, "Missing " + ("".equals(str)
                    ? "number"
                    : "value for \"" + str + '"'));
      } else {
         Log.err(err, "Failed to parse " + ("".equals(str)
                    ? "number \"" + value + '"'
                    : '"' + str + "\" value \"" + value + '"'));
      }
      return sm_PARSE_FAILED;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean isBool(String value) {
      return "true".equalsIgnoreCase(value)
          || "false".equalsIgnoreCase(value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean parseBool(String value) {
      return parseBool("", value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean parseBool(String str, String value) {
      if (value == null) {
         return false;
      }
      if ("".equals(value)) {
         Log.err("Missing boolean" + ("".equals(str)
               ? "."
               : " value for \"" + str + '"'));
         return false;
      }
      if (!isBool(value)) {
         Log.err("Failed to parse " + ("".equals(str)
               ? "boolean \"" + value + '"'
               : '"' + str + "\" boolean value \"" + value + '"'));
         return false;
      }
      return "true".equalsIgnoreCase(value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String toEscape(String str) {
      String out = "";
      for (int i = 0; i < str.length(); ++i) {
         out += toEscapeChar(str.charAt(i));
      }
      return out;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String toEscapeChar(char c) {
      switch (c) {
      case 7: return "\\a";
      case '\b': return "\\b";
      case 127: return "\\d";
      case 27: return "\\e";
      case '\f': return "\\f";
      case '\n': return "\\n";
      case '\r': return "\\r";
      case ' ': return "\\s";
      case '\t': return "\\t";
      case '\\': return "\\\\";
      case '<': return "\\<";
      case 11: return "\\v";
      }
      return (c < 32) ? String.format("\\x%02x", (int)c) : "" + c;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String toEscapeCommented(String str) {
      String out = "";
      for (int i = 0; i < str.length(); ++i) {
         out += toEscapeCharCommented(str.charAt(i));
      }
      return out;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String toEscapeCharCommented(char c) {
      if (c == sm_COMMENT) {
         return "\\" + sm_COMMENT;
      }
      return toEscapeChar(c);
   }

   ////////////////////////////////////////////////////////////////////////////
   // returns the value of a char that was preceded by \.
   static char escapedToChar(char c) {
      switch (c) {
      case 'a': return 7;
      case 'b': return '\b';
      case 'd': return 127;
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
   // returns an interpretation of the first escaped character in the string
   public static int parseEscapeFirst(String str, StringBuilder err) {
      CharIntPair cip = parseEscape1By1(str, err);
      if (cip == null) {
         return sm_PARSE_FAILED;
      }
      return cip.m_Char;
   }

   ////////////////////////////////////////////////////////////////////////////
   // returns an interpretation of the string representing one escaped character
   public static int parseEscape1(String str, StringBuilder err) {
      CharIntPair cip = parseEscape1By1(str, err);
//System.out.printf("%c (%d) %d%n", cip.m_Char, (int)cip.m_Char, cip.m_Int);
      if (cip == null) {
         return sm_PARSE_FAILED;
      }
      if (cip.m_Int != str.length()) {
         char c = cip.m_Char;
         Log.err(err, String.format("Expecting an escaped character, got \"%s\"", str));
         return sm_PARSE_FAILED;
      }
      return cip.m_Char;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public static String parseEscape(String str) {
      return parseEscape(str, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   // returns an interpretation of the string of escaped characters
   // includes the form \xnn where nn is a 2 digit hex value.
   public static String parseEscape(String str, StringBuilder err) {
      char[] buf = new char[str.length()];
      int i = 0;
      int j = 0;
      CharIntPair cip = null;
      for (;;) {
         cip = parseEscape1By1(str.substring(j), err);
         if (cip == null) {
            if (err != null && err.length() != 0) {
               return null;
            }
            break;
         }
         buf[i++] = cip.m_Char;
         j += cip.m_Int;
      }
      return new String(buf, 0, i);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean isPrintable(int ch) {
      return 32 < ch && ch < 127;
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
         if (c == ' ' || c == '_') {
            initial = true;
         } else {
            if (initial) {
               initial = false;
               c = Character.toUpperCase(c);
            } else {
               c = Character.toLowerCase(c);
            }
            out += c;
         }
      }
      return out;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static short otherEndian(short s) {
      return (short)((s >> 8 & 0xFF) | (s << 8 & 0xFF00));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int otherEndian(int i) {
      return (i >> 24 & 0xFF) | (i >> 8 & 0xFF00)
           | (i << 8 & 0xFF0000) | (i << 24 & 0xFF000000);
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private static class CharIntPair {
      CharIntPair(char c, int i) {
         m_Char = c;
         m_Int = i;
      }
      char m_Char;
      int m_Int;
   }   

   ////////////////////////////////////////////////////////////////////////////
   // return the character and the length of the string parsed.
   private static CharIntPair parseEscape1By1(String str, StringBuilder err) {
      if ("".equals(str)) {
         return null;
      }
      char c = str.charAt(0);
      if (c != '\\') {
         return new CharIntPair(c, 1);
      }
      if (str.length() == 1) {
         Log.warn(err, "Unexpected '\\' at end of line");
         return null;
      }
      char c1 = str.charAt(1);
      if (c1 != 'x') {
         return new CharIntPair(escapedToChar(c1), 2);
      }
      if (str.length() < 4) {
         Log.warn(err, "Too few digits after '\\x' at end of line");
         return null;
      }
      int c2 = toIntWarnParse(str.substring(2, 4), 16, err);
      if (c2 == sm_PARSE_FAILED) {
         return null;
      }
      return new CharIntPair((char)c2, 4);
   }

   // Main /////////////////////////////////////////////////////////////////////
   public static void main (String[] args) {
      System.out.println(trimComment(args[0]));
   }
}
