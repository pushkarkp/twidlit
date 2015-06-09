/**
 * Copyright 2015 Pushkar Piggott
 *
 * StringInt.java
 */
package pkp.string;

///////////////////////////////////////////////////////////////////////////////
public class StringInt implements java.lang.Comparable {

   ////////////////////////////////////////////////////////////////////////////
   public StringInt(String str, int i) {
      m_String = str;
      m_Int = i;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String getString() { return m_String; }
   public int getInt() { return m_Int; }
   public String toString() { return String.format("%4d %s\n", m_Int, m_String); }

   ////////////////////////////////////////////////////////////////////////////
   public int compareTo(Object rhs) {
      StringInt si = (StringInt)rhs;
      if (si == null) {
         return -1;
      }
      int strCmp = m_String.compareToIgnoreCase(si.m_String);
      if (strCmp != 0) {
         return strCmp;
      }
      return m_Int < si.m_Int ? -1 : (m_Int > si.m_Int ? 1 : 0);
   }

   // Data ////////////////////////////////////////////////////////////////////
   private final String m_String;
   private final int m_Int;
}
