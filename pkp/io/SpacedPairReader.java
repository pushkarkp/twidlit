/**
 * Copyright 2015 Pushkar Piggott
 *
 * SpacedPairReader.java
 */
package pkp.io;

import java.net.URL;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
// Reads and trims 2 space separated items on a line.
// Use SetSingleToken() to insist on no space in the second item.
public class SpacedPairReader {

   ////////////////////////////////////////////////////////////////////////////
   public SpacedPairReader(URL url, boolean mustExist) {
      m_In = new LineReader(url, mustExist);
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
      Log.log(String.format("%s expected '%s' but read '%s' on line %d of \"%s\"\n",
                            module, expected, first, getLineNumber(), m_In.getPath()));
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   public int getLineNumber() {
      return m_In.getLineNumber();
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
         Log.warn(String.format("Failed to parse \"%s\", line %d of \"%s\".",
                               in, getLineNumber(), m_In.getPath()));
         return;
      }
      m_1 = m_Line.substring(0, split);
      m_2 = m_Line.substring(split).trim();
      if (m_SingleToken2 && Io.findFirstOf(m_2, Io.sm_WS) != m_2.length()) {
         Log.err(String.format("Failed to parse \"%s\", line %d of \"%s\".",
                               in, getLineNumber(), m_In.getPath()));
      }
   }

   // Data ////////////////////////////////////////////////////////////////////
   private LineReader m_In = null;
   private String m_Line = null;
   private String m_1 = null;
   private String m_2 = null;
   private boolean m_SingleToken2 = false;
}
