/**
 * Copyright 2015 Pushkar Piggott
 *(
 * CharCounts.java
 */
package pkp.chars;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import pkp.lookup.SharedIndexableInts;
import pkp.util.Pref;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
class CharCounts implements SharedIndexableInts {
   
   ////////////////////////////////////////////////////////////////////////////
   static int sm_CHARS = 128;
   
   ////////////////////////////////////////////////////////////////////////////
   CharCounts(boolean bigrams) {
      m_MAX_REPEAT = Pref.getInt("count.repeats.max", 2);
      m_Counts = new int[bigrams
                         ? (sm_CHARS + 1) * sm_CHARS
                         : sm_CHARS];
      m_Prev = -1;
      m_Repeat = 0;
      m_Bigrams = bigrams;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public boolean hasBigramCounts() {
      return m_Bigrams;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // SharedIndexableInts
   public int getSize() {
      return sm_CHARS;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // SharedIndexableInts
   public int getCount(int i) {
      return m_Counts[i];
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // SharedIndexableInts
   public String getLabel(int i) {
      return String.format("    %3d     ", i) + printSymbol(i);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void nextChar(char c) {
      if (c != m_Prev) {
         m_Repeat = 0;
      } else {
         ++m_Repeat;
         if (m_Repeat >= m_MAX_REPEAT) {
//System.out.printf("Ignoring %dth repeat%n", m_Repeat);               
            return;
         }
      }
      ++m_Counts[c];
      if (m_Bigrams && m_Prev != -1) {
         ++m_Counts[combine(m_Prev, c)];
//System.out.printf("\"%c%c\" combine(m_Prev, c) %5d m_Counts[combine(m_Prev, c)] %3d%n", m_Prev, c, combine(m_Prev, c), m_Counts[combine(m_Prev, c)]);               
      }
      m_Prev = c;
   }

   /////////////////////////////////////////////////////////////////////////////
   class BigramCounts implements SharedIndexableInts {
      
      /////////////////////////////////////////////////////////////////////////
      @Override // SharedIndexableInts
      public int getSize() {
         return m_Counts.length - sm_CHARS;
      }

      /////////////////////////////////////////////////////////////////////////
      @Override // SharedIndexableInts
      public int getCount(int i) {
         return m_Counts[i + sm_CHARS];
      }

      /////////////////////////////////////////////////////////////////////////
      @Override // SharedIndexableInts
      public String getLabel(int i) {
         int first = i / sm_CHARS;
         int second = i % sm_CHARS;   
         String codes = String.format("%3d %3d ", first, second);
         if (isPrintable(first) && isPrintable(second)) {
            return codes + String.format("    %c%c ", (char)first, (char)second);
         } else {
            return codes + printSymbol(first) + " " + printSymbol(second);
         }
      }
   }
   
   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private static String[] s_SYM = new String[] 
      {"NUL", "SOH", "STX", "ETX", "EOT", "ENQ", "ACK", "BEL", "BS", "TAB", 
       "LF", "VT", "FF", "CR", "SO", "SI", "DLE", "DC1", "DC2", "DC3", "DC4", 
       "NAK", "SYN", "ETB", "CAN", "EM", "SUB", "ESC",  "FS", "GS", "RS", "US", 
       "SPC"
      };

   ////////////////////////////////////////////////////////////////////////////
   private static String printSymbol(int ch) {
      if (isPrintable(ch)) {
         return String.format("%c  ", (char)ch);
      }
      if (ch < 33) {
         return String.format("%-3s", s_SYM[ch]);
      }
      if (ch == 127) {
         return "BKS";
      }
      return String.format("%3d", ch);
   }

   ////////////////////////////////////////////////////////////////////////////
   private static boolean isPrintable(int ch) {
      return 32 < ch && ch < 127;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static int combine(int first, int second) {
      return (first + 1) * sm_CHARS + second;   
   }
   
   // Data ////////////////////////////////////////////////////////////////////
   private static int m_MAX_REPEAT;
   private int[] m_Counts;
   private int m_Prev;
   private int m_Repeat;
   private boolean m_Bigrams;
}
