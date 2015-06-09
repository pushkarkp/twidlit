/**
 * Copyright 2015 Pushkar Piggott
 *
 * StringsInts.java
 */
package pkp.string;

import java.util.Arrays;

///////////////////////////////////////////////////////////////////////////////
public class StringsInts extends java.lang.Object {

   ////////////////////////////////////////////////////////////////////////////
   public StringsInts(StringInt[] stringsInts) {
      m_StringsInts = stringsInts;
   }

   ////////////////////////////////////////////////////////////////////////////
   public int getInt(String str, int fail) {
      StringInt si = new StringInt(str, -1);
      int ix = -Arrays.binarySearch(m_StringsInts, si) - 1;
      if (m_StringsInts[ix].getString().equalsIgnoreCase(str)) {
         return m_StringsInts[ix].getInt();
      }
      return fail;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String getString(int i, String fail) {
      for (StringInt si: m_StringsInts) {
         if (i == si.getInt()) {
            return si.getString();
         }
      }
      return fail;
   }

   ////////////////////////////////////////////////////////////////////////////
   public int size() {
      return m_StringsInts.length;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      String str = "";
      for (StringInt si: m_StringsInts) {
         str += si.toString();
      }
      return str;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private StringInt[] m_StringsInts;
}
