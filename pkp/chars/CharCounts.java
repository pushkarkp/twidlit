/**
 * Copyright 2015 Pushkar Piggott
 *
 * CharCounts.java
 */
package pkp.chars;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import pkp.util.Pref;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
public class CharCounts {
   
   ////////////////////////////////////////////////////////////////////////////
   public CharCounts() {
      m_Counts = new int[m_CHARS];
      m_MaxRepeat = Pref.getInt("count.repeats.max", 2);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public CharCounts(File f) {
      this();
      count(f);
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
   public String table() {
      return table(m_Counts, true);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public String graph() {
      return graph(m_Counts, true, 1.0);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public static String table(int[] counts, boolean sort) {
      final int DP = 4;
      String str = new String();
      String countFormat = String.format("%%%dd", getMaxDigits(counts));
		double[] pc = new double[counts.length];
		int pcDigits = calcPercent(counts, pc);
      int[] index = index(counts, sort);
		for (int i = 0; i < counts.length; ++i) {
			if (counts[index[i]] != 0) {
				for (int j = 0; j < 4; ++j) {
					switch (j) {
						case 0:
							str += String.format("%3d", index[i]);
							break;
						case 1:
							str += printSymbol(index[i]);
							break;
						case 2:
							str += String.format(countFormat, counts[index[i]]);
							break;
						case 3: {
							int space = pcDigits + 1 + DP;
							if (pc[index[i]] >= 10.0) {
								--space;
								if (pc[index[i]] >= 100.0) {
									--space;
								}
							}
							str += String.format(String.format("%%%d.%df", space, DP), pc[index[i]]);
							break;
						}
					}
					if (j < 3) {
						str += ' ';
					}
				}
				str += '\n';
			}
		}
      return str;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public static String graph(int[] counts, boolean sort, double zoom) {
      String str = new String();
		int max = 0;
		for (int i = 1; i < counts.length; ++i) {
			if (counts[i] > counts[max]) {
				max = i;
			}
		}
      int[] index = index(counts, sort);
      double scale = zoom * sm_PAGE_WIDTH / (counts[max] + 0.5);
		for (int i = 0; i < counts.length; ++i) {
//System.out.printf("zoom %f counts[max] %d counts[k] %d\n", zoom, counts[max], counts[k]);
			int dots = (int)(counts[index[i]] * scale);
//System.out.printf("dots %d\n", dots);
			if (dots > 0) {// && dots <= sm_PAGE_WIDTH) {
//			 ||(dots == 0 && (showall == ALL || showall == ALL_BELOW)) ||
//				(dots > 76 && (showall == ALL || showall == ALL_ABOVE))) {
				int last = dots;
				if (last > sm_PAGE_WIDTH - 1) {
					last = sm_PAGE_WIDTH - 1;
				}
				str += printSymbol(index[i]);
				for (int j = 0; j < last; ++j) {
					str += '=';
				}
				if (dots == sm_PAGE_WIDTH) {
					str += '=';
				} else if (dots > sm_PAGE_WIDTH) {
					str += '>';
				}
				str += '\n';
			}
		}
      return str;
   }
   
   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   public static int[] index(int[] counts, boolean sort) {
      int[] index = new int[counts.length];
      if (!sort) {
         for (int i = 0; i < counts.length; ++i) {
            index[i] = i;
         }
         return index;
      }
      int lastMax = Integer.MAX_VALUE;
      for (int i = 0; i < index.length; ++i) {
         int max = Integer.MIN_VALUE;
         for (int j = 0; j < counts.length; ++j) {
            if (lastMax > counts[j] && counts[j] > max) {
               max = counts[j];
               index[i] = j;
            }
         }
         lastMax = max;
      }
      return index;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   private static String[] s_SYM = new String[] 
      {"NUL", "SOH", "STX", "ETX", "EOT", "ENQ", "ACK", "BEL", "BS", "TAB", 
       "LF", "VT", "FF", "CR", "SO", "SI", "DLE", "DC1", "DC2", "DC3", "DC4", 
       "NAK", "SYN", "ETB", "CAN", "EM", "SUB", "ESC",  "FS", "GS", "RS", "US", 
       "SPC"
      };

   ////////////////////////////////////////////////////////////////////////////
   private static String printSymbol(int ch) {
      if (ch < 33) {
         return String.format("%-3s", s_SYM[ch]);
      }
      if (32 < ch && ch < 127) {
         return String.format("%c  ", (char)ch);
      }
      return String.format("%3d", ch);
   }

///////////////////////////////////////////////////////////////////////////////
   private static int getMaxDigits(int[] counts) {
      int maxDigits = 0;
      for (int i = 0; i < counts.length; ++i) {
         int digits = 0;
         for (int val = counts[i]; val > 0; val /= 10) {
            ++digits;
         }
         if (digits > maxDigits) {
            maxDigits = digits;
         }
      }
      return maxDigits;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static int calcPercent(int[] counts, double[] ppc) {
      int pcDigits = 1;
      double sum = 0.0;
      for (int i = 0; i < counts.length; ++i) {
         sum += counts[i];
      }
      double factor = 100.0 / sum;
      for (int i = 0; i < counts.length; ++i) {
         ppc[i] = counts[i] * factor;
         if (ppc[i] >= 10.0) {
            if (ppc[i] >= 100.0) {
               pcDigits = Math.max(pcDigits, 3);
            } else {
               pcDigits = Math.max(pcDigits, 2);
            }
         }
      }
      return pcDigits;
   }

   ////////////////////////////////////////////////////////////////////////////
   private void countFile(File f) {
      Log.log("Counting \"" + f.getPath() + '"');
      byte[] data = new byte[(int)f.length()];
      FileInputStream fis = null;
      try {
         fis = new FileInputStream(f);
      } catch (FileNotFoundException e) {
      }
      try {
         fis.read(data, 0, data.length);
         fis.close();
      } catch (IOException e) {
      }
      ByteBuffer bb = ByteBuffer.wrap(data);
      int prev = -1;
      int repeat = 0;
      while (bb.hasRemaining()) {
         int cin = bb.get();
         if (cin < 0 || cin >= m_CHARS) {
            Log.log(String.format("Count ignored %d at %d in \"%s\"", cin, bb.position(), f.getPath()));
            continue;
         } else if (cin == prev) {
            ++repeat;
            if (repeat >= m_MaxRepeat) {
//System.out.printf("Ignoring %dth repeat%n", repeat);               
               continue;
            }
         } else {
            repeat = 0;
            prev = cin;
         }
         ++m_Counts[cin];
      }
	}
   
   // Data ////////////////////////////////////////////////////////////////////
   private static int sm_PAGE_WIDTH = 72;
   private static int m_CHARS = 256;
   private int[] m_Counts;
   private int m_MaxRepeat;
}
