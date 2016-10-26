/**
 * SortedChordTimes.java
 */

package pkp.times;

import java.io.File;
import pkp.twiddle.Twiddle;
import pkp.twiddle.Chord;
import pkp.lookup.SharedIndexableInts;
import pkp.lookup.SharedIndex;
import pkp.io.LineReader;
import pkp.io.Io;
import pkp.util.Log;
import pkp.util.Util;

////////////////////////////////////////////////////////////////////////////////
// Chord 0 is not counted so we subtract 1 and use Chord.sm_VALUES counts.
public class SortedChordTimes implements SharedIndexableInts {
   
   /////////////////////////////////////////////////////////////////////////////
   public SortedChordTimes(File chordF) {
      m_Times = new int[Chord.sm_VALUES];
      m_Labels = new String[Chord.sm_VALUES];
      StringBuilder err = new StringBuilder();
      LineReader chordLr = new LineReader(Io.toExistUrl(chordF), Io.sm_MUST_EXIST);
      for (int i = 0; i < Chord.sm_VALUES;) {
         String line = chordLr.readLine();
         if ("".equals(line)) {
            continue;
         }
         if (line == null) {
            break;
         }
         Twiddle t = new Twiddle(line);
         if (!t.getChord().isValid()) {
            Log.parseWarn(chordLr, String.format("Failed to parse invalid chord \"%s\"", line));
         } else if (!t.getThumbKeys().isEmpty()) {
            Log.parseWarn(chordLr, String.format("Ignored chord with thumb keys \"%s\"", line));
         } else {
            m_Labels[i] = t.toString();
            m_Times[i] = chordLr.getInt(line);
            if (m_Times[i] == 0) {
               m_Times[i] = sm_EMPTY;
            }
            ++i;
         }
      }
      chordLr.close();
      // sort them
      for (int i = 0; i < m_Times.length; ++i) {
         for (int k = i + 1; k < m_Times.length; ++k) {
            if (m_Times[k] < m_Times[i]) {
               int t = m_Times[i];
               m_Times[i] = m_Times[k];
               m_Times[k] = t;
               String s = m_Labels[i];
               m_Labels[i] = m_Labels[k];
               m_Labels[k] = s;
            }
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   public SortedChordTimes(ChordTimes times) {
      m_Times = new int[Chord.sm_VALUES];
      m_Labels = new String[Chord.sm_VALUES];
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         m_Times[i] = times.getMean(i + 1, 0);
         if (m_Times[i] == 0) {
            m_Times[i] = sm_EMPTY;
         }
         int range = times.getRange(i + 1, 0);
         m_Labels[i] = (new Chord(i + 1)).toString()
                     + ((m_Times[i] == sm_EMPTY)
                        ? "" 
                        : String.format(" %d %d (", m_Times[i], range)
                         + times.getTimes(i + 1, 0) + ")");
      }
      m_Index = SharedIndex.create(this, 0, Integer.MAX_VALUE);
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // SharedIndexableInts
   public int getSize() {
      return Chord.sm_VALUES;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // SharedIndexableInts
   public String getLabel(int i) {
      return m_Labels[i];
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // SharedIndexableInts
   public int getCount(int i) {
      return m_Times[i];
   }

   ////////////////////////////////////////////////////////////////////////////
   public int findChord(String chord) {
      return Util.findStartsWith(chord, m_Labels);
   }

   ///////////////////////////////////////////////////////////////////////////////
   public String listChordsByTime() {
      String str = "";
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         str += m_Index.getLabel(i) + "\n";
      }
      return str;
   }
   
   ///////////////////////////////////////////////////////////////////////////////
   public String getSortedLabel(int i) {
      return m_Index.getLabel(i);
   }
   
   ///////////////////////////////////////////////////////////////////////////////
   public int getSortedValue(int i) {
      return m_Index.getValue(i);
   }
   
   // Private //////////////////////////////////////////////////////////////////

   // Data /////////////////////////////////////////////////////////////////////
   private final int sm_EMPTY = Integer.MAX_VALUE;

   private int[] m_Times;
   private String[] m_Labels;
   private SharedIndex m_Index;
}
