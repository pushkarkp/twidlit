/**
 * Copyright 2016 Pushkar Piggott
 *
 * CrLf.java
 */
package pkp.io;

///////////////////////////////////////////////////////////////////////////////
public class CrLf {

   ////////////////////////////////////////////////////////////////////////////
   public static String normalize(String in) {
      if (in.length() < 2) {
         return in;
      }
      CrLf crLf = new CrLf();
      char[] out = new char[in.length()];
      int next = 0;
      for (int i = 0; i < in.length(); ++i) {
         char c = crLf.next(in.charAt(i));
         if (c != '\0') {
            out[next++] = c;
         }
      }
      return new String(out, 0, next);
   }

   ////////////////////////////////////////////////////////////////////////////
   public CrLf() {
      m_Skip = false;
      m_Prev = 0;
   }

   ////////////////////////////////////////////////////////////////////////////
   public char getPrev() {
      return m_Prev;
   }

   ////////////////////////////////////////////////////////////////////////////
   public char next(char c) {
      if (c != '\r' && c != '\n') {
         m_Prev = c;
         return c;
      }
      // ignore \n after \r and \r after \n
      m_Skip = ((m_Prev == '\r' && c == '\n')
             || (m_Prev == '\n' && c == '\r')) 
            && !m_Skip;
      if (m_Skip) {
         return '\0';
      }
      m_Prev = c;
      // treat \r as \n
      return '\n';
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean m_Skip;
   private char m_Prev;

   // Main /////////////////////////////////////////////////////////////////////
   public static void main(String[] argv) {
      for (int i = 0; i < argv.length; ++i) {
         System.out.println(Io.toEscape(CrLf.normalize(Io.parseEscape(argv[i]))));
      }
   }
}