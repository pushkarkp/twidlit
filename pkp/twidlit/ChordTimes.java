/**
 * Copyright 2015 Pushkar Piggott
 *
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
class ChordTimes implements Persistent {
   
   /////////////////////////////////////////////////////////////////////////////
   // with and without thumbkeys
   static final int sm_CHORD_TYPES = 2;
   // number off attempts we keep track of
   static final int sm_SPAN = 8;

   /////////////////////////////////////////////////////////////////////////////
   ChordTimes() {
      m_RightHand = false;
      load();
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean hasData() {
      return m_DataStatus != DataStatus.NONE;
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean isRightHand() {
      return m_RightHand;
   }

   /////////////////////////////////////////////////////////////////////////////
   void setRightHand(boolean isRightHand) {
//System.out.println("setRightHand " + isRightHand);
      if (isRightHand == m_RightHand) {
         return;
      }
      persist("");
      m_RightHand = isRightHand;
      load();
   }

   /////////////////////////////////////////////////////////////////////////////
   void clear() {
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
   boolean add(int chord, int thumbKeys, int timeMs) {
      if (timeMs > Short.MAX_VALUE) {
         return false;
      }
      int thumb = Math.min(thumbKeys, 1);
      addMean(false, chord, thumb);
      m_DataStatus = DataStatus.NEW;
      if ((chord & ~Chord.sm_VALUES) != 0) {
         Log.err(String.format("Chord value %d is greater than %d\n", chord, Chord.sm_VALUES));
         chord &= Chord.sm_VALUES;
      }
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
      addMean(true, chord, thumb);
//System.out.printf("add i %d full %d m_Counts[thumb][chord - 1] %d%n", i, full, m_Counts[thumb][chord - 1]);
      return true;
   }

   /////////////////////////////////////////////////////////////////////////////
   int getMean(int chord, int thumbKeys) {
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
      int i = sm_SPAN - 3;
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
//System.out.printf("getMeanMean() thumbKeys %d meanmean %d%n", thumbKeys, meanMean);
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
   private static String list(short[] a) {
      String str = "";
      for (int i = 0; i < a.length; ++i) {
         str += String.format("%d ", a[i]);
      }
      return str;
   }

   /////////////////////////////////////////////////////////////////////////////
   private int getCount(int chord, int thumb) {
      int count = m_Counts[thumb][chord - 1] & sm_SPAN;
      if (count != 0) {
         return count;
      }
      return m_Counts[thumb][chord - 1] & (sm_SPAN - 1);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   private void addMean(boolean add, int chord, int thumb) {
      int mean = getMean(chord, thumb);
//System.out.printf("addMean() chord %d thumb %d mean %d%n", chord, thumb, mean);
      if (mean == Integer.MAX_VALUE) {
         mean = 0;
         ++m_MeanCount[thumb];
      }
      m_MeanSum[thumb] += add ? mean : -mean;
//System.out.printf("addMean() m_MeanCount[thumb] %d m_MeanSum[thumb] %d%n", m_MeanCount[thumb], m_MeanSum[thumb]);
   }

   /////////////////////////////////////////////////////////////////////////////
   private short[] getIq(int chord, int thumbKeys) {
      int thumb = Math.min(thumbKeys, 1);
      int count = getCount(chord, thumb);
      if (count == 0) {
         // no time
         return null;
      }
      int end = count;
      short[] sort = java.util.Arrays.copyOf(m_Times[thumb][chord - 1], count);
      if (count > 2) {
         getMinMax(sort, end);
         end -= 2;
         if (count > 6) {
            getMax(sort, end);
            end -= 1;
         }
      }
      sort[end] = -1;
//System.out.printf("getIq() (%d) %d %d %d %d %d %d %d %d%n", count, sort[0], sort[1], sort[2], sort[3], sort[4], sort[5], sort[6], sort[7]);
      return sort;
   }

   /////////////////////////////////////////////////////////////////////////////
   private void getMinMax(short[] data, int count) {
      int min = count - 2;
      int max = count - 1;
      for (int i = 0; i < max; ++i) {
         if (data[min] > data[i]) {
            short swap = data[min];
            data[min] = data[i];
            data[i] = swap;
         }
         if (data[max] < data[i]) {
            short swap = data[max];
            data[max] = data[i];
            data[i] = swap;
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private void getMax(short[] data, int count) {
      int max = count - 1;
      for (int i = 0; i < max; ++i) {
         if (data[max] < data[i]) {
            short swap = data[max];
            data[max] = data[i];
            data[i] = swap;
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private void load() {
//System.out.println("load " + getFileName());
      // up to sm_SPAN times for each
      m_Times = new short[sm_CHORD_TYPES][Chord.sm_VALUES][sm_SPAN];
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
            addMean(false, c + 1, thumb);
            m_Counts[thumb][c] = bb.get();
            for (int i = 0; i < sm_SPAN; ++i) {
               m_Times[thumb][c][i] = bb.getShort();
            }
            addMean(true, c + 1, thumb);
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private String getFileName() {
      return (m_RightHand ? "right" : "left") + ".times";
   }
      
   // Data /////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////
   enum DataStatus {
      NONE, SAVED, NEW;
   }
   
   private boolean m_RightHand;
   private DataStatus m_DataStatus;
   private short[][][] m_Times;
   private byte[][] m_Counts;
   private int m_MeanSum[];
   private int m_MeanCount[];

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
      System.out.printf("Result %d %d%n", times.getMean(1, 0), times.getMean(Chord.sm_VALUES, 1));
/*      times.persist("");
      times = new ChordTimes();
      System.out.printf("Result %g %g%n", times.getMs(1, 0), times.getMs(Chord.sm_VALUES, 1));
*/   }
}
