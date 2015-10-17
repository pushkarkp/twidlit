/**
 * ChordTimes.java
 */

package pkp.twidlit;

import java.util.Random;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import pkp.twiddle.Chord;
import pkp.util.Persistent;
import pkp.util.Persist;
import pkp.util.Log;
import pkp.io.Io;

////////////////////////////////////////////////////////////////////////////////
// Chord 0 is not counted so we subtract 1 and use Chord.sm_VALUES counts.
public class ChordTimes implements Persistent {
   
   /////////////////////////////////////////////////////////////////////////////
   // with and without thumbkeys
   static final int sm_CHORD_TYPES = 2;
   // number off attempts we keep track of
   static final int sm_SPAN = 8;

   /////////////////////////////////////////////////////////////////////////////
   public ChordTimes() {
      m_RightHand = false;
      load();
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean isRightHand() {
      return m_RightHand;
   }

   /////////////////////////////////////////////////////////////////////////////
   public void setRightHand(boolean isRightHand) {
//System.out.println("setRightHand " + isRightHand);
      if (isRightHand == m_RightHand) {
         return;
      }
      persist("");
      m_RightHand = isRightHand;
      load();
   }

   /////////////////////////////////////////////////////////////////////////////
   public void clear() {
      File f = Io.createFile(Persist.getFolderName(), getFileName());
      if (f.exists() && !f.isDirectory()) {
         f.delete();
      }
      load();
   }
      
   /////////////////////////////////////////////////////////////////////////////
   public void add(int chord, int thumbKeys, int timeMs) {
      if (timeMs > Short.MAX_VALUE) {
         return;
      }
      if ((chord & ~Chord.sm_VALUES) != 0) {
         Log.err(String.format("Chord value %d is greater than %d\n", chord, Chord.sm_VALUES));
         chord &= Chord.sm_VALUES;
      }
      int thumb = Math.min(thumbKeys, 1);
      byte count = m_Counts[thumb][chord - 1];
      int i = count & (sm_SPAN - 1);
      m_Times[thumb][chord - 1][i] = (short)timeMs;
      ++i;
      int full = count & sm_SPAN;
      if (i == sm_SPAN) {
         i = 0;
         full = sm_SPAN;
      }
      m_Counts[thumb][chord - 1] = (byte)(full | i);
//System.out.printf("add i %d full %d m_Counts[thumb][chord - 1] %d%n", i, full, m_Counts[thumb][chord - 1]);
   }

   /////////////////////////////////////////////////////////////////////////////
   public int getMedian(int chord, int thumbKeys) {
      int thumb = Math.min(thumbKeys, 1);
      int count = getCount(chord, thumb);
      if (count == 0) {
         // no count
         return Integer.MAX_VALUE;
      }
      // median
      short[] times = m_Times[thumb][chord - 1];
      short[] sorted = new short[count];
		for (int i = 0; i < count; ++i) {
         sorted[i] = times[i];
      }
      int min;
		for (min = 0; min < count; min += 2) {
         for (int i = min + 1; i < count; ++i) {
            if (sorted[i] < sorted[min]) {
               short swap = sorted[i];
               sorted[i] = sorted[min];
               sorted[min] = swap;
            } else
            if (sorted[i] > sorted[min + 1]) {
               short swap = sorted[i];
               sorted[i] = sorted[min + 1];
               sorted[min + 1] = swap;
            }
/*          System.out.print("Sorting ");
            for (int j = 0; j < count; ++j) {
               System.out.printf("%d ", sorted[j]);
            }
            System.out.println();
*/       }
      }
/*    System.out.print("Sorted ");
      for (int j = 0; j < count; ++j) {
         System.out.printf("%d ", sorted[j]);
      }
      System.out.println();
*/    if ((count & 1) == 1) {
         return sorted[count - 1];
      }
      return (sorted[count - 1] + sorted[count - 2]) / 2;
   }
/*
   /////////////////////////////////////////////////////////////////////////////
   public double getMs1(int chord, int thumbKeys) {
      int thumb = Math.min(thumbKeys, 1);
      int count = m_Counts[thumb][chord - 1] & sm_SPAN;
      if (count == 0) {
         count = m_Counts[thumb][chord - 1] & (sm_SPAN - 1);
         if (count == 0) {
            // no count
            return Double.MAX_VALUE;
         }
      }
      // median
      short[] times = m_Times[thumb][chord - 1];
      short[] sort = new short[count];
      int less = 0;
      int more = count - 1;
      int pivot = count / 2;
		while (less != pivot) {
         short test = src[pivot];
         for (; less < more; ++less) {
            if (src[less] > test) {
               while (src[more] > test) {
                  --more;
               }
               swap(src[less], src[more]); 
            }
         }
      }
      return (sorted[count - 1] + sorted[count - 2]) / 2.0;
   }
*/
   ////////////////////////////////////////////////////////////////////////////
   @Override
   public void persist(String tag) {
//System.out.println("persist " + getFileName());
      byte[] data = new byte[sm_CHORD_TYPES * Chord.sm_VALUES * (1 + sm_SPAN * 2)];
      ByteBuffer bb = ByteBuffer.wrap(data);
      for (int thumb = 0; thumb < sm_CHORD_TYPES; ++thumb) {
         for (int c = 0; c < Chord.sm_VALUES; ++c) {
            bb.put(m_Counts[thumb][c]);
            for (int i = 0; i < sm_SPAN; ++i) {
               bb.putShort(m_Times[thumb][c][i]);
            }
         }
      }
      File f = Io.createFile(Persist.getFolderName(), getFileName());
      FileOutputStream fos = null;
      try {
         fos = new FileOutputStream(f);
      } catch (FileNotFoundException e) {
         Log.warn("Failed to open: \"" + f.getPath() + "\".");
			return;
      }
      try {
         fos.write(data, 0, data.length);
         fos.flush();
         fos.close();
      } catch (IOException e) {
         Log.warn("Failed to write: \"" + f.getPath() + "\".");
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   public String getTimes(int chord, int thumbKeys) {
      int thumb = Math.min(thumbKeys, 1);
      int count = getCount(chord, thumb);
      String str = String.format("%d:", count);
      for (int i = 0; i < count; ++i) {
         str += String.format(" %d", m_Times[thumb][chord - 1][i]);
      }
      return str;
   }
   
   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private int getCount(int chord, int thumb) {
      int count = m_Counts[thumb][chord - 1] & sm_SPAN;
      if (count == 0) {
         return m_Counts[thumb][chord - 1] & (sm_SPAN - 1);
      }
      return count;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private void load() {
//System.out.println("load " + getFileName());
      // up to sm_SPAN times for each
      m_Times = new short[sm_CHORD_TYPES][Chord.sm_VALUES][sm_SPAN];
      // the actual number of times held
      m_Counts = new byte[sm_CHORD_TYPES][Chord.sm_VALUES];
      File f = Io.createFile(Persist.getFolderName(), getFileName());
      if (!f.exists() || f.isDirectory()) {
         return;
      }
      byte[] data = new byte[(int)f.length()];
      FileInputStream fis = null;
      try {
         fis = new FileInputStream(f);
      } catch (FileNotFoundException e) {
         Log.log("No existing times");
         return;
      }
      try {
         fis.read(data, 0, data.length);
         fis.close();
      } catch (IOException e) {
         Log.err("Failed to read times: " + e);
      }
      ByteBuffer bb = ByteBuffer.wrap(data);
      for (int thumb = 0; thumb < sm_CHORD_TYPES; ++thumb) {
         for (int c = 0; c < Chord.sm_VALUES; ++c) {
            m_Counts[thumb][c] = bb.get();
            for (int i = 0; i < sm_SPAN; ++i) {
               m_Times[thumb][c][i] = bb.getShort();
            }
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private String getFileName() {
      return (m_RightHand ? "right" : "left") + ".times";
   }
      
   // Data /////////////////////////////////////////////////////////////////////
   private boolean m_RightHand;
   private short[][][] m_Times;
   private byte[][] m_Counts;

   // Main /////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      final int LIMIT = 100;
      Persist.init("TwidlitPersist.properties", ".", "pref");
      ChordTimes times = new ChordTimes();
      Random rand = new Random();
      int count = Integer.parseInt(args[0]);
      for (int i = 0; i < count; ++i) {
         times.add(1, 0, rand.nextInt(LIMIT));
         times.add(Chord.sm_VALUES, 1, rand.nextInt(LIMIT));
      }
      int limit = Math.min(count, sm_SPAN);
      for (int i = 0; i < limit; ++i) {
         System.out.printf("ChordTimes %d %d%n", times.m_Times[0][0][i], times.m_Times[1][Chord.sm_VALUES - 1][i]);
      }
      System.out.printf("Result %d %d%n", times.getMedian(1, 0), times.getMedian(Chord.sm_VALUES, 1));
/*      times.persist("");
      times = new ChordTimes();
      System.out.printf("Result %g %g%n", times.getMs(1, 0), times.getMs(Chord.sm_VALUES, 1));
*/   }
}
