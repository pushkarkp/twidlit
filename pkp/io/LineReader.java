/**
 * Copyright 2015 Pushkar Piggott
 *
 * LineReader.java
 */
package pkp.io;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import pkp.string.StringSource;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class LineReader implements StringSource {

   ////////////////////////////////////////////////////////////////////////////
/*   public LineReader(String parent, String fileName, String comment, boolean mustExist) {
      this(Io.getPath(parent, fileName), comment, mustExist);
   }
*/
   ////////////////////////////////////////////////////////////////////////////
   public LineReader(URL url, String comment, boolean mustExist) {
      m_Url = url;
//System.out.println(url);
      if (m_Url == null) {
         if (mustExist) {
            Log.err(getClass().getName() + " could not open null URL.");
         }
         return;
      }
      m_Comment = comment;
      try {
         m_In = new BufferedReader(new InputStreamReader(m_Url.openStream()));
      } catch (IOException e) {
         if (mustExist) {
            Log.err(getClass().getName() + " failed to open \"" + m_Url.getPath() + "\".");
         }
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public String readLine() {
      String line = null;
      do {
         try {
            line = m_In.readLine();
         } catch (IOException e) {
            Log.err(getClass().getName() + " failed to read \"" + m_Url.getPath() + "\".");
         }
         if (line == null) {
            return null;
         }
         if (m_Comment != null) {
            line = Io.trimComment(m_Comment, line);
         }
         ++m_LineNumber;
      } while ("".equals(line));
      return line;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // StringSource
   public String getName() {
      return "LineReader:" + m_Url.getPath() + ':';
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // StringSource
   public String getFullName() {
      return getName();
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // StringSource
   public StringSource getSource() {
      return null;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // StringSource
   public String getNextString() {
      return readLine();
   }

   ////////////////////////////////////////////////////////////////////////////
   public void close() {
      try {
         m_In.close();
      } catch (IOException e) {
         Log.err(getClass().getName() + " failed to close \"" + m_Url.getPath() + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public String getPath() { return m_Url.getPath(); }
   public int getLineNumber() { return m_LineNumber; }

   // Data ////////////////////////////////////////////////////////////////////
   private URL m_Url;
   private String m_Comment;
   private BufferedReader m_In;
   private int m_LineNumber = 0;
}
