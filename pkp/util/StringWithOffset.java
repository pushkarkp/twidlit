/**
 * Copyright 2016 Pushkar Piggott
 *
 * StringWithOffset.java
 */
package pkp.util;

public class StringWithOffset {
   public StringWithOffset(String str) {
      m_Str = str;
      m_Offset = 0;
   }
   public StringWithOffset(String str, int i) {
      m_Str = str;
      setOffset(i);
   }
   public void setOffset(int i) {
      m_Offset = Math.max(0, Math.min(i, m_Str.length()));
   }
   public void setOffsetToEnd() {
      m_Offset = m_Str.length();
   }
   public String getString() {
      return m_Str;
   }
   public int getOffset() {
      return m_Offset;
   }
   private String m_Str;
   private int m_Offset;
}

