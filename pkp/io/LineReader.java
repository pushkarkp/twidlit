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
import pkp.string.StringInt;
import pkp.util.NamedOrdered;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
// uses Io.trimComment() to strip comments but does not strip spaces from line.
public class LineReader implements StringSource, NamedOrdered {

   ////////////////////////////////////////////////////////////////////////////
   public LineReader(URL url) {
      this(url, false);
   }

   ////////////////////////////////////////////////////////////////////////////
   public LineReader(URL url, boolean mustExist) {
      m_Url = url;
      if (m_Url == null) {
         if (mustExist) {
            Log.err(getClass().getName() + " could not open null URL.");
         }
         return;
      }
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
            Log.err(getClass().getName() + String.format(" failed to read line %d of \"%s\".", m_LineNumber + 1, m_Url.getPath()));
         }
         if (line == null) {
            return null;
         }
         line = Io.trimComment(line);
         ++m_LineNumber;
      } while ("".equals(line.trim()));
      return line;
   }

   ///////////////////////////////////////////////////////////////////////////////
   // NamedOrdered
   public StringInt getNameAndPosition() {
      return new StringInt(getPath(), getLineNumber());
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
   public int getInt(String line) {
      int offset = Io.findFirstOf(line, Io.sm_DIGIT);
      int len = Io.findFirstOf(line.substring(offset), Io.sm_WS);
      if (len > 0) {
         StringBuilder err = new StringBuilder();
         int val = Io.parseInt(line.substring(offset, offset + len), err);
         if ("".equals(err.toString())) {
            return val;
         }
         Log.parseWarn(this, err.toString(), line);
      }
      return 0;
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
   private BufferedReader m_In;
   private int m_LineNumber = 0;
}
