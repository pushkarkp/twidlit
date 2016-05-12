/**
 * Copyright 2015 Pushkar Piggott
 *
 * NGram.java
 */
package pkp.chars;

import pkp.util.Pref;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
public class NGram {
   
   ////////////////////////////////////////////////////////////////////////////
   // n > 1
   public static boolean isValid(String chars) {
      return chars != null && chars.length() > 1;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public NGram(String chars) {
      if (chars.length() < 2) {
         Log.err("Ngram too short \"" + chars + '"');
      }
      m_Chars = new String(chars);
      m_Next = 1;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public String getChars() {
      return m_Chars;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public boolean nextChar(char next) {
      if (m_Chars.charAt(m_Next) == next) {
         ++m_Next;
         return true;
      }
      m_Next = 0;
      return false;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public boolean matched() {
      return m_Next == m_Chars.length();
   }
   
   // Data ////////////////////////////////////////////////////////////////////
   private String m_Chars;
   private int m_Next;
}
