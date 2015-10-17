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
      m_DataStatus = DataStatus.NEW;
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
      return true;
   }

   /////////////////////////////////////////////////////////////////////////////
   int getMedian(int chord, int thumbKeys) {
      int thumb = Math.min(thumbKeys, 1);
      int count = getCount(chord, thumb);
      if (count == 0) {
         // no time
         return Integer.MAX_VALUE;
      }
      if (count == 1) {
         return m_Times[thumb][chord - 1][0];
      }
      short[] sort = java.util.Arrays.copyOf(m_Times[thumb][chord - 1], count);
      int less = -1;
      int more = count;
      final int MID = (count - 1) / 2;
      short test = sort[MID];
//System.out.printf("%s%n", list(sort));
      for (;;) {
//System.out.printf("less %d more %d test %d%n", less, more, test);
         int le = less + 1;
         int gt = more;
         int pivot = -1;
         while (le < gt) {
            if (sort[le] <= test) {
               ++le;
            } else {
               while (sort[gt - 1] > test) {
                  --gt;
               }
               if (gt - 1 > le) {
                  --gt;
                  short swap = sort[le];
                  sort[le] = sort[gt];
                  sort[gt] = swap;
                  ++le;
               }
            }
            if (sort[le - 1] == test) {
               pivot = le - 1;
            }
         }
         int found = le - 1;
         if (found != pivot) {
            short swap = sort[found];
            sort[found] = sort[pivot];
            sort[pivot] = swap;
         }
//System.out.printf("%s%nfound %d pivot %d MID %d%n", list(sort), found, pivot, MID);
         if (sort[MID] == test) {
//System.out.printf("return %d%n", test);
            return test;
         }
         if (found <= MID) {
            less = found;
            test = sort[le];
         } else {
            more = le;
            // hangs when test = sort[found]
            test = sort[found - 1];//(short)Math.min(sort[found], sort[found - 1]);
         }
      }
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
   
   /////////////////////////////////////////////////////////////////////////////
   private void load() {
//System.out.println("load " + getFileName());
      // up to sm_SPAN times for each
      m_Times = new short[sm_CHORD_TYPES][Chord.sm_VALUES][sm_SPAN];
      // the actual number of times held
      m_Counts = new byte[sm_CHORD_TYPES][Chord.sm_VALUES];
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
   /////////////////////////////////////////////////////////////////////////////
   enum DataStatus {
      NONE, SAVED, NEW;
   }
   
   private boolean m_RightHand;
   private DataStatus m_DataStatus;
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
