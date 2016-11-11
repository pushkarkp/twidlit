/**
 * Copyright 2015 Pushkar Piggott
 *
 * ChordText.java
 */
package pkp.utilities;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import pkp.twiddle.Twiddle;
import pkp.twiddle.Chord;
import pkp.io.LineReader;
import pkp.io.Io;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class ChordText {

   ////////////////////////////////////////////////////////////////////////////
   public ChordText(LineReader lr) {
      m_Lines = new String[Chord.sm_VALUES + 1];
      if (lr == null) {
         return;
      }
      lr.setKeepComment(true);
      String line;
      for (int i = 1; (line = lr.readLine()) != null; ++i) {
         Twiddle tw = new Twiddle(line);
         if (tw.getThumbKeys().isEmpty()
          && tw.getChord().isValid()) {
            m_Lines[tw.getChord().toInt()] = line;
         }
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public String get(int c) { return m_Lines[c]; }

   // Data ////////////////////////////////////////////////////////////////////
   private String[] m_Lines;

   // Main /////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      URL url = Io.toExistUrl(new File(args[0]));
      ChordText al = new ChordText(new LineReader(url, Io.sm_MUST_EXIST));
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         if (al.get(i) != null) {
            System.out.printf("%d %s%n", i, al.get(i));
         }
      }
   }
}
