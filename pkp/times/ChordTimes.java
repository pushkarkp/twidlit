/**
 * Copyright 2015 Pushkar Piggott
 *
 * ChordTimes.java
 */

package pkp.times;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import pkp.twiddle.Chord;
import pkp.util.Persistent;
import pkp.util.Persist;
import pkp.util.Pref;
import pkp.util.Log;
import pkp.io.Io;

////////////////////////////////////////////////////////////////////////////////
// Chord 0 is not counted so we subtract 1 and use Chord.sm_VALUES counts.
public class ChordTimes implements Persistent {
   
   /////////////////////////////////////////////////////////////////////////////
   // without and with thumbkeys
   static final int sm_CHORD_TYPES = 2;

   /////////////////////////////////////////////////////////////////////////////
   public ChordTimes(/*boolean isKeys, */boolean isRightHand) {
      this(true, isRightHand, Pref.getInt("chord.times.span", 16));
   }

   /////////////////////////////////////////////////////////////////////////////
   public int getSpan() {
      return m_SPAN;
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean hasData() {
      return m_DataStatus != DataStatus.NONE;
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean isKeys() {
      return m_KEYS;
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean isRightHand() {
      return m_RIGHTHAND;
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
   public int[] getCounts() {
      int[] counts = new int[Chord.sm_VALUES];
      for (int c = 0; c < Chord.sm_VALUES; ++c) {
         counts[c] = getCount(c + 1, 0);
      }
      return counts;
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean add(int chord, int thumbKeys, int timeMs) {
      if (timeMs > Short.MAX_VALUE) {
         return false;
      }
      int thumb = Math.min(thumbKeys, 1);
      addMean(false, chord, thumb);
      m_DataStatus = DataStatus.NEW;
      if ((chord & ~Chord.sm_VALUES) != 0) {
         Log.err(String.format("Chord value %d is not in the range [1..%d]\n", chord, Chord.sm_VALUES));
         chord &= Chord.sm_VALUES;
      }
      byte count = m_Counts[thumb][chord - 1];
      int i = count & (m_SPAN - 1);
      m_Times[thumb][chord - 1][i] = (short)timeMs;
      ++i;
      int full = count & m_SPAN;
      if (i == m_SPAN) {
         i = 0;
         full = m_SPAN;
      }
      m_Counts[thumb][chord - 1] = (byte)(full | i);
      addMean(true, chord, thumb);
//System.out.printf("add i %d full %d m_Counts[thumb][chord - 1] %d%n", i, full, m_Counts[thumb][chord - 1]);
//System.out.printf("add() meanCount %d%n", m_MeanCount[thumb]);
      return true;
   }

   /////////////////////////////////////////////////////////////////////////////
   public int getMean(int chord, int thumbKeys) {
      short[] sort = getIq(chord, thumbKeys);
      if (sort == null) {
         return Integer.MAX_VALUE;
      }
      int sum = 0;
      int i = 0;
      for (; sort[i] != -1; ++i) {
         sum += sort[i];
      }
      return sum / i;
   }

   /////////////////////////////////////////////////////////////////////////////
   int getRange(int chord, int thumbKeys) {
      short[] sort = getIq(chord, thumbKeys);
      if (sort == null) {
         return Integer.MAX_VALUE;
      }
      int i = Math.min(m_SPAN - 3, sort.length - 1);
      while (sort[i] != -1) {
         --i;
      }
      if (i == 1) {
         return 0;
      }
      getMinMax(sort, i);
//System.out.printf("getRange() max %d min %d%n", sort[i - 1], sort[i - 2]);
      return sort[i - 1] - sort[i - 2];
   }

   ////////////////////////////////////////////////////////////////////////////
   public int getMeanMean(int thumbKeys) {
      int thumb = Math.min(thumbKeys, 1);
      if (m_MeanCount[thumb] == 0) {
         return 0;
      }
      int meanMean = m_MeanSum[thumb] / m_MeanCount[thumb];
System.out.printf("getMeanMean() thumb %d m_MeanSum[thumb] %d m_MeanCount[thumb] %d meanmean %d%n", thumb, m_MeanSum[thumb], m_MeanCount[thumb], meanMean);
      return meanMean;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override
   public void persist(String tag) {
//System.out.println("persist " + getFileName());
      if (m_DataStatus != DataStatus.NEW) {
         return;
      }
      m_DataStatus = DataStatus.SAVED;
      byte[] data = new byte[sm_CHORD_TYPES * Chord.sm_VALUES * (1 + m_SPAN * 2)];
      ByteBuffer bb = ByteBuffer.wrap(data);
      for (int thumb = 0; thumb < sm_CHORD_TYPES; ++thumb) {
         for (int c = 0; c < Chord.sm_VALUES; ++c) {
            byte count = m_Counts[thumb][c];
            int first = 0;
            int limit = m_SPAN;
            if ((count & m_SPAN) != 0) {
               first = count & ~m_SPAN;
            } else {
               limit = count;
            }
            bb.put((byte)limit);
            short[] times = m_Times[thumb][c];
            for (int i = 0; i < limit; ++i) {
               bb.putShort(times[(i + first) % m_SPAN]);
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
         // use bb.size()
         fos.write(data, 0, data.length);
         fos.flush();
         fos.close();
      } catch (IOException e) {
         Log.warn("Failed to write: \"" + f.getPath() + "\".");
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   String getTimes(int chord, int thumbKeys) {
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
   private static void legalSpan(int span) {
      if (span <= 64) {
         for (int i = 0; i < 6; ++i) {
            if ((span & 1 << i) != 0
             && (span & ~(1 << i)) == 0) {
               return;
            }
         }
      }
      Log.err(String.format("chord.times.span (%d) must be one of (1, 2, 4, 8, 16, 32, 64).", span));
   }

   /////////////////////////////////////////////////////////////////////////////
   private static String list(short[] a) {
      String str = "";
      for (int i = 0; i < a.length; ++i) {
         str += String.format("%d ", a[i]);
      }
      return str;
   }

   /////////////////////////////////////////////////////////////////////////////
   private ChordTimes(boolean isKeys, boolean isRightHand, int span) {
      m_SPAN = span;
      legalSpan(m_SPAN);
      m_KEYS = isKeys;
      m_RIGHTHAND = isRightHand;
      load();
   }

   /////////////////////////////////////////////////////////////////////////////
   // returns the number of valid entries [0..m_SPAN]
   private int getCount(int chord, int thumb) {
      int count = m_Counts[thumb][chord - 1] & m_SPAN;
      if (count != 0) {
         return count;
      }
      return m_Counts[thumb][chord - 1] & (m_SPAN - 1);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   // add the latest or remove the oldest mean
   private void addMean(boolean add, int chord, int thumb) {
      int mean = getMean(chord, thumb);
      if (mean == Integer.MAX_VALUE) {
         if (add) {
            Log.err("Added zero time");
         }
         mean = 0;
         ++m_MeanCount[thumb];
      }
      m_MeanSum[thumb] += add ? mean : -mean;
//System.out.printf("addMean() add %b chord %d thumb %d mean %d m_MeanCount[thumb] %d m_MeanSum[thumb] %d%n", add, chord, thumb, mean, m_MeanCount[thumb], m_MeanSum[thumb]);
   }

   /////////////////////////////////////////////////////////////////////////////
   // returns the InterQuartile values terminated by -1
   private short[] getIq(int chord, int thumbKeys) {
      int thumb = Math.min(thumbKeys, 1);
      int end = getCount(chord, thumb);
      if (end == 0) {
         // no time
         return null;
      }
      short[] sort = java.util.Arrays.copyOf(m_Times[thumb][chord - 1], end + 1);
      if (end > 2) {
         int iq = end / 2;
         for (; end > iq; end -= 2) {
            getMinMax(sort, end);
         }
      }
      sort[end] = -1;
//System.out.printf("getIq() length %d end %d%n", sort.length, end);
//System.out.println("getIq() " + list(sort));
      return sort;
   }

   /////////////////////////////////////////////////////////////////////////////
   // move min and max to end
   private void getMinMax(short[] data, int count) {
      int min = count - 2;
      int max = count - 1;
      if (data[max] < data[min]) {
         short swap = data[max];
         data[max] = data[min];
         data[min] = swap;
      }
      for (int i = 0; i < min; ++i) {
         if (data[max] < data[i]) {
            short swap = data[max];
            data[max] = data[i];
            data[i] = swap;
         }
         if (data[min] > data[i]) {
            short swap = data[min];
            data[min] = data[i];
            data[i] = swap;
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private void load() {
//System.out.println("load " + getFileName());
      // up to m_SPAN times for each
      m_Times = new short[sm_CHORD_TYPES][Chord.sm_VALUES][m_SPAN];
      // the actual number of times held
      m_Counts = new byte[sm_CHORD_TYPES][Chord.sm_VALUES];
      m_MeanSum = new int[sm_CHORD_TYPES];
      m_MeanSum[0] = 0;
      m_MeanSum[1] = 0;
      m_MeanCount = new int[sm_CHORD_TYPES];
      m_MeanCount[0] = 0;
      m_MeanCount[1] = 0;
      File f = Io.createFile(Persist.getFolderName(), getFileName());
      if (!f.exists() || f.isDirectory()) {
         m_DataStatus = DataStatus.NONE;
         return;
      }
      m_DataStatus = DataStatus.SAVED;
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
            int count = bb.get();
            if (count > 0) {
               m_Counts[thumb][c] = (byte)Math.min(count, m_SPAN);
               short[] times = m_Times[thumb][c];
               int start = Math.max(0, count - m_SPAN);
               for (int i = 0; i < start; ++i) {
                  bb.getShort();
               }
               int end = count - start;
               for (int i = 0; i < end; ++i) {
                  times[i] = bb.getShort();
               }
               addMean(true, c + 1, thumb);
            }
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private String getFileName() {
      return /*(m_KEYS ? "keys" : "chords") + '.' + */(m_RIGHTHAND ? "right" : "left") + ".times";
   }
      
   // Data /////////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   enum DataStatus {
      NONE, SAVED, NEW;
   }
   
   // number off attempts we keep track of
   private final int m_SPAN;
   private final boolean m_KEYS;
   private final boolean m_RIGHTHAND;
   private DataStatus m_DataStatus;
   private short[][][] m_Times;
   private byte[][] m_Counts;
   private int m_MeanSum[];
   private int m_MeanCount[];

   // Main /////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      final int LIMIT = 100;
      Log.init(Log.ExitOnError);
      Persist.init("TwidlitPersist.properties", ".", "pref");
      Pref.init("TwidlitPreferences.txt", Persist.get("pref.dir"), "pref");
      int count = Integer.parseInt(args[2]);
      ChordTimes times = new ChordTimes(true, true, Integer.parseInt(args[0]));
      for (int i = 0; i < count; ++i) {
         times.add(1, 0, i + 1);
         times.add(2, 0, i + 3);
         times.add(Chord.sm_VALUES, 1, i + 1);
      }
      int limit = Math.min(count, times.getSpan());
      for (int i = 0; i < limit; ++i) {
         System.out.printf("ChordTimes %d %d %d%n", times.m_Times[0][0][i], times.m_Times[0][1][i], times.m_Times[1][Chord.sm_VALUES - 1][i]);
      }
      System.out.printf("Mean %d %d %d meanmean[0] %d%n", times.getMean(1, 0), times.getMean(2, 0), times.getMean(Chord.sm_VALUES, 1), times.getMeanMean(0));
      int size = Integer.parseInt(args[1]);
      if (size > 0) {
         times.persist("");
         times = new ChordTimes(true, true, size);
         limit = Math.min(count, times.getSpan());
         for (int i = 0; i < limit; ++i) {
            System.out.printf("ChordTimes %d %d%n", times.m_Times[0][0][i], times.m_Times[1][Chord.sm_VALUES - 1][i]);
         }
         System.out.printf("Mean %d %d meanmean[0] %d%n", times.getMean(1, 0), times.getMean(Chord.sm_VALUES, 1), times.getMeanMean(0));
      }
   }
}
