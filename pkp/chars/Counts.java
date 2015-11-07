/**
 * Copyright 2015 Pushkar Piggott
 *
 * Counts.java
 */
package pkp.chars;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import pkp.twiddle.KeyPress;
import pkp.lookup.SharedIndex;
import pkp.lookup.SharedIndexableInts;
import pkp.ui.ProgressWindow;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
public class Counts {
   
   ////////////////////////////////////////////////////////////////////////////
   public Counts(URL url, int lowest, int highest) {
      m_Url = url;
      m_CharCounts = null;
      m_NGrams = null;
      m_Index = null;
      m_LowestCount = lowest;
      m_HighestCount = highest;
      m_ShowBigrams = false;
      m_ShowNGrams = false;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public Counts(Counts other) {
      m_Url = other.m_Url;
      m_CharCounts = other.m_CharCounts;
      m_NGrams = other.m_NGrams;
      m_Index = other.m_Index;
      m_LowestCount = other.m_LowestCount;
      m_HighestCount = other.m_HighestCount;
      m_ShowBigrams = other.m_ShowBigrams;
      m_ShowNGrams = other.m_ShowNGrams;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public boolean setShowBigrams(boolean set) {
      if (set == m_ShowBigrams) {
         return false;
      }
      m_ShowBigrams = set;
      m_CharCounts = null;
      m_NGrams = null;
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean setShowNGrams(boolean set) {
      if (set == m_ShowNGrams) { 
         return false;
      }
      m_ShowNGrams = set;
      m_CharCounts = null;
      m_NGrams = null;
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setBounds(int lowest, int highest) {
      if (lowest != m_LowestCount || highest != m_HighestCount) { 
         m_LowestCount = lowest;
         m_HighestCount = highest;
         m_Index = null;
      }
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public static int getProgressCount() {
      return 10;
   }

   ////////////////////////////////////////////////////////////////////////////
   public void count(File f) {
      if (f == null) {
         return;
      }
      if (f.isFile()) {
         countFile(f);
      } else if (f.isDirectory()) {
         File[] files = f.listFiles();
         if (files != null) {
            for (File file : files) {
               if (!file.isDirectory()) {
                  countFile(file);
               }
            }
         }
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public String table(ProgressWindow pw) {
      if (m_Index == null) {
         m_Index = createIndex();
      }
      pw.step();
      final int DP = 4;
      String countFormat = String.format("%%%dd", m_Index.getMaxDigits());
		int pcDigits = m_Index.calcPercents();
      String str = "";
      final int STEP = Math.max(1, m_Index.getSize() / (getProgressCount() - 1));
		for (int i = 0; i < m_Index.getSize(); ++i) {
         if (i % STEP == STEP - 1) {
            pw.step();
         }
         for (int j = 0; j < 3; ++j) {
            switch (j) {
               case 0:
                  str += m_Index.getLabel(i);
                  break;
               case 1:
                  str += String.format(countFormat, m_Index.getValue(i));
                  break;
               case 2: {
                  double pc = m_Index.getPercent(i);
                  int space = pcDigits + 1 + DP;
                  if (pc >= 10.0) {
                     --space;
                     if (pc >= 100.0) {
                        --space;
                     }
                  }
                  str += String.format(String.format("%%%d.%df", space, DP), pc);
                  break;
               }
            }
            if (j < 2) {
               str += ' ';
            }
         }
         str += '\n';
		}
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String graph(ProgressWindow pw) {
      if (m_Index == null) {
         m_Index = createIndex();
      }
      pw.step();
      int labelSize = m_ShowBigrams ? 7 : 3;
      if (m_ShowNGrams) { 
         labelSize = Math.max(labelSize, m_NGrams.maxLength());
      }
      final int WIDTH = 1 + sm_PAGE_WIDTH - labelSize;
      double scale = WIDTH / (m_Index.getMax() + 0.5);
      String str = "";
      final int STEP = Math.max(1, m_Index.getSize() / (getProgressCount() - 1));
//System.out.printf("labelSize %d WIDTH %d scale %g step %d%n", labelSize, WIDTH, scale, step);
//System.out.printf("m_Index.getSize() %d STEP %d%n", m_Index.getSize(), STEP);
		for (int i = 0; i < m_Index.getSize(); ++i) {
         if (i % STEP == STEP - 1) {
//System.out.printf("%d ", i);
            pw.step();
         }
			int dots = (int)(m_Index.getValue(i) * scale);
			if (dots > 0) {
				int last = dots;
				if (last > WIDTH - 1) {
					last = WIDTH - 1;
				}
				str += m_Index.getLabel(i);
				for (int j = 0; j < last; ++j) {
					str += '=';
				}
				if (dots == WIDTH) {
					str += '=';
				} else if (dots > WIDTH) {
					str += '>';
				}
				str += '\n';
			}
		}
//System.out.println();
//pkp.io.Io.read("");
      return str;
   }
   
   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private void countFile(File f) {
      byte[] data = new byte[(int)f.length()];
      FileInputStream fis = null;
      try {
         fis = new FileInputStream(f);
      } catch (FileNotFoundException e) {
         Log.err("Failed to open \"" + f.getPath() + '"');
         return;
      }
      try {
         fis.read(data, 0, data.length);
         fis.close();
      } catch (IOException e) {
         Log.err("Failed to read \"" + f.getPath() + '"');
         return;
      }
      if (m_CharCounts == null) {
         m_CharCounts = new CharCounts(m_ShowBigrams);
      }
      if (m_NGrams == null && m_ShowNGrams) {
         m_NGrams = new NGrams(m_Url);
      }
      boolean ignoredSome = false;
      boolean[] ignored = new boolean[128];
      ByteBuffer bb = ByteBuffer.wrap(data);
      while (bb.hasRemaining()) {
         int cin = bb.get() & 0xFF;
         if (cin >= 128) {
            ignoredSome = true;
            ignored[cin - 128] = true;
            continue;
         }
         m_CharCounts.nextChar((char)cin);
         if (m_NGrams != null) {
            m_NGrams.nextChar((char)cin);
         }
      }
      if (ignoredSome) {
         String ig = "";
         for (int i = 0; i < ignored.length; ++i) {
            if (ignored[i]) {
               ig += String.format(" %d", i + 128);
            }
         }
         Log.log("Count ignored the following bytes in \"" + f.getName() + "\":" + ig);
      }
	}
   
   ////////////////////////////////////////////////////////////////////////////
   private SharedIndex createIndex() {
      ArrayList<SharedIndexableInts> sic = new ArrayList<SharedIndexableInts>();
      sic.add(m_CharCounts);
      if (m_CharCounts.hasBigramCounts()) {
         sic.add(m_CharCounts.new BigramCounts());
      }
      if (m_NGrams != null) {
         sic.add(m_NGrams);
      }
      return SharedIndex.create(sic, m_HighestCount, m_LowestCount);
   }
   
   // Data ////////////////////////////////////////////////////////////////////
   private static int sm_PAGE_WIDTH = 78;
   private CharCounts m_CharCounts;
   private NGrams m_NGrams;
   private URL m_Url;
   private SharedIndex m_Index;
   private int m_LowestCount;
   private int m_HighestCount;
   private boolean m_ShowBigrams;
   private boolean m_ShowNGrams;
}
