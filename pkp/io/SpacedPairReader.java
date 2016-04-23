/**
 * Copyright 2015 Pushkar Piggott
 *
 * SpacedPairReader.java
 */
package pkp.io;

import java.net.URL;
import pkp.string.StringInt;
import pkp.util.NamedOrdered;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
// Reads and trims 2 space separated items on a line.
// Use SetSingleToken() to insist on no space in the second item.
public class SpacedPairReader implements NamedOrdered {

   ////////////////////////////////////////////////////////////////////////////
   public SpacedPairReader(URL url, boolean mustExist) {
      this(url, mustExist, Log.Level.WARN);
   }

   ////////////////////////////////////////////////////////////////////////////
   public SpacedPairReader(URL url, boolean mustExist, Log.Level parseFailLogLevel) {
      m_In = new LineReader(url, mustExist);
      m_LogLevel = parseFailLogLevel;
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setSingleToken(boolean single) {
      m_SingleToken2 = single;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String getNextFirst() {
      if (m_1 == null) {
         parseLine();
      }
      String str = m_1;
      m_1 = null;
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String getNextSecond() {
      String str = m_2;
      m_2 = null;
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String getNextLine() {
      if (m_Line == null) {
         parseLine();
      }
      String str = m_Line;
      m_Line = null;
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isMatchFirst(String expected, String module) {
      String first = getNextFirst();
      if (first.equals(expected)) {
         return true;
      }
      if (module != null) {
         Log.log(String.format("%s expecting '%s', found '%s' on line %d of \"%s\"\n",
                               module, expected, first, getLineNumber(), m_In.getPath()));
      }
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   public int getLineNumber() {
      return m_In.getLineNumber();
   }

   ///////////////////////////////////////////////////////////////////////////////
   // NamedOrdered
   public StringInt getNameAndPosition() {
      return m_In.getNameAndPosition();
   }

   ////////////////////////////////////////////////////////////////////////////
   public void close() {
      m_In.close();
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private void parseLine() {
      String in = m_In.readLine();
      if (in == null) {
         return;
      }
      m_Line = in.trim();
      int split = Io.findFirstOf(m_Line, Io.sm_WS);
      if (split == m_Line.length()) {
         String msg = String.format("No space in \"%s\"", m_Line);
         Log.parseFail(m_LogLevel, getLineNumber(), m_In.getPath(), msg);
         return;
      }
      m_1 = m_Line.substring(0, split);
      m_2 = m_Line.substring(split).trim();
      if (m_SingleToken2 && Io.findFirstOf(m_2, Io.sm_WS) != m_2.length()) {
         String msg = String.format("More than one space in \"%s\"", m_Line);
         Log.parseFail(m_LogLevel, getLineNumber(), m_In.getPath(), msg);
      }
   }

   // Data ////////////////////////////////////////////////////////////////////
   private LineReader m_In = null;
   private String m_Line = null;
   private String m_1 = null;
   private String m_2 = null;
   private boolean m_SingleToken2 = false;
   private Log.Level m_LogLevel;
}
