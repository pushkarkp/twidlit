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
         if (t.getChord().isMouseButton()) {
            Log.parseWarn(chordLr, String.format("Ignored mouse button \"%s\"", line));
         } else if (!t.getChord().isChord()) {
            Log.parseWarn(chordLr, String.format("Failed to parse invalid chord \"%s\"", line));
         } else if (!t.getThumbKeys().isEmpty()) {
            Log.parseWarn(chordLr, String.format("Ignored chord with thumb keys \"%s\"", line));
         } else {
            m_Labels[i] = t.getChord().toString();
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
   public static SortedChordTimes asComparison(ChordTimes times, boolean side) {
      SortedChordTimes sct = new SortedChordTimes();
      int count = 0;
      double sum = 0.0;
      int iCount = 0;
      double iSum = 0.0;
      int rCount = 0;
      double rSum = 0.0;
      for (int j = 0; j < Chord.sm_VALUES; ++j) {
         int i = j + 1;
         final int r = Chord.reverse(i);
         final Chord c = Chord.fromChordValue(i);
         if (i < r && (side == c.none(Chord.Position.R))) {
            final int iSample = times.getCount(i, 0);
            final int rSample = times.getCount(r, 0);
            if (iSample == 0 || rSample == 0) {
               sct.m_Labels[j] = c.toString();
               sct.m_Times[j] = sm_EMPTY;
            } else {
               final int iTime = times.getMean(i, 0);
               final int rTime = times.getMean(r, 0);
               double percent;
               if (iTime <= rTime) {
                  final int diff = rTime - iTime;
                  percent = diff * 100.0 / rTime;
                  sct.m_Labels[j] = c
                       + String.format(" %5d %5d %6.1f %3d %3d", iTime, diff, percent, iSample, rSample);
                  sct.m_Times[j] = iTime;
                  if (side) {
                     ++iCount;
                     iSum += percent;
                  }
               } else {
                  final int diff = iTime - rTime;
                  percent = diff * 100.0 / iTime;
                  sct.m_Labels[j] = Chord.fromChordValue(r)
                       + String.format(" %5d %5d %6.1f %3d %3d", rTime, diff, percent, rSample, iSample);
                  sct.m_Times[j] = rTime;
                  if (side) {
                     ++rCount;
                     rSum += percent;
                  }
               }
               ++count;
               sum += percent;
            }
         }
      }
      sct.m_Index = SharedIndex.create(sct, 0, Integer.MAX_VALUE);

      if (count > 0) {
         sct.m_Preamble +=
            String.format("# Mean diff of %d %s chords: %.1f%%%n", 
                          count, (side ? "side" : "cross"), sum / count);
         if (side) {
            if (rCount > 0) {
               sct.m_Preamble += 
                  String.format("# Mean diff of %d %s chords: %.1f%%%n", 
                                rCount, (times.isRightHand() ? "near" : "far"), rSum / rCount);
            }
            if (rCount > 0) {
               sct.m_Preamble += 
                  String.format("# Mean diff of %d %s chords: %.1f%%%n", 
                                iCount, (times.isRightHand() ? "far" : "near"), iSum / iCount);
            }
         }
      }
      sct.m_Preamble += "#     msec  Diff      %  #<  #>\n";

      return sct;
   }

   /////////////////////////////////////////////////////////////////////////////
   public SortedChordTimes(ChordTimes times) {
      m_Preamble = "#   Mean Range (Times)\n";
      m_Times = new int[Chord.sm_VALUES];
      m_Labels = new String[Chord.sm_VALUES];
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         m_Times[i] = times.getMean(i + 1, 0);
         if (m_Times[i] == 0) {
            m_Times[i] = sm_EMPTY;
         }
         int range = times.getRange(i + 1, 0);
         m_Labels[i] = Chord.fromChordValue(i + 1).toString()
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
   public String getPreamble() {
      return m_Preamble;
   }

   ////////////////////////////////////////////////////////////////////////////
   public int findChord(String chord) {
      return Util.findStartsWith(chord, m_Labels);
   }

   ///////////////////////////////////////////////////////////////////////////////
   public String listChordsByTime() {
      String str = "";
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         if (m_Index.getLabel(i) != null) {
            str += m_Index.getLabel(i) + "\n";
         }
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

   /////////////////////////////////////////////////////////////////////////////
   private SortedChordTimes() {
      m_Preamble = "";
      m_Times = new int[Chord.sm_VALUES];
      m_Labels = new String[Chord.sm_VALUES];
   }

   // Data /////////////////////////////////////////////////////////////////////
   private static final int sm_EMPTY = Integer.MAX_VALUE;

   private String m_Preamble;
   private int[] m_Times;
   private String[] m_Labels;
   private SharedIndex m_Index;
}
