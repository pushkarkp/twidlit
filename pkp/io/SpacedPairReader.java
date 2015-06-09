/**
 * Copyright 2015 Pushkar Piggott
 *
 * SpacedPairReader.java
 */
package pkp.io;

import java.net.URL;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class SpacedPairReader {

   ////////////////////////////////////////////////////////////////////////////
   public static int findWhiteSpace(String str) {
      int ws = str.indexOf('\t');
      if (ws == -1) {
         return str.indexOf(' ');
      }
      return ws;
   }

   ////////////////////////////////////////////////////////////////////////////
   public SpacedPairReader(URL url, String comment, boolean mustExist) {
      m_Comment = comment;
      m_In = new LineReader(url, null, mustExist);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setSingleToken(boolean single) {
      m_SingleToken2 = single;
   }

   ////////////////////////////////////////////////////////////////////////////
   public SpacedPairReader(URL url, boolean mustExist) {
      this(url, null, mustExist);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setComment(String comment) {
      m_Comment = comment;
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
      String in = null;
      m_Line = null;
      do {
         in = m_In.readLine();
         if (in == null) {
            return;
         }
         if (m_Comment == null) {
            m_Line = in;
         } else {
            m_Line = Io.trimComment(m_Comment, in);
         }
      } while ("".equals(m_Line));
      int split = findWhiteSpace(m_Line);
      if (split == -1) {
         m_Line = in.trim();
         split = findWhiteSpace(m_Line);
         if (split == -1 || !m_Comment.equals(m_Line.substring(split).trim())) {
            Log.log(String.format("Failed to parse \"%s\", line %d of \"%s\".",
                                  in, getLineNumber(), m_In.getPath()));
         }
      }
      m_1 = m_Line.substring(0, split);
      m_2 = m_Line.substring(split).trim();
      if (m_SingleToken2 && findWhiteSpace(m_2) != -1) {
         Log.err(String.format("Failed to parse \"%s\", line %d of \"%s\".",
                               in, getLineNumber(), m_In.getPath()));
      }
   }

   // Data ////////////////////////////////////////////////////////////////////
   private LineReader m_In = null;
   private String m_Comment = null;
   private String m_Line = null;
   private String m_1 = null;
   private String m_2 = null;
   private boolean m_SingleToken2 = false;
}
