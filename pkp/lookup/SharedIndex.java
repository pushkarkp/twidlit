/**
 * Copyright 2015 Pushkar Piggott
 *
 * SharedIndex.java
 */
package pkp.lookup;

import java.util.ArrayList;

////////////////////////////////////////////////////////////////////////////////
public class SharedIndex {
   
   ////////////////////////////////////////////////////////////////////////////
   public SharedIndex(ArrayList<SharedIndexableInts> sic, int lowest, int highest) {
      m_Sic = sic;
      // only index non-zero items
      int indexSize = 0;
      for (int i = 0; i < sic.size(); ++i) {
         SharedIndexableInts s = sic.get(i);
         int size = s.getSize();
         for (int j = 0; j < size; ++j) {
            int count = s.getCount(j);
            if (lowest <= count && count <= highest) {
               ++indexSize;
            }
         }
      }
      // only refer to non-zero items
      m_Values = new int[indexSize];
      m_CounterIndex = new int[indexSize];
      m_Index = new int[indexSize];
      m_Offset = new int[indexSize];
      m_Percent = null;
      int k = 0;
      for (int i = 0; i < sic.size(); ++i) {
         SharedIndexableInts s = sic.get(i);
         int size = s.getSize();
         for (int j = 0; j < size; ++j) {
            int count = s.getCount(j);
            if (lowest <= count && count <= highest) {
               m_Values[k] = count;
               m_CounterIndex[k] = i;
               m_Index[k] = k;
               m_Offset[k] = j;
               ++k;
            }
         }
      }
      // sort
      for (int i = 0; i < m_Index.length; ++i) {
         for (int j = i + 1; j < m_Index.length; ++j) {
            if (m_Values[m_Index[j]] > m_Values[m_Index[i]]) {
               int tmp = m_Index[i];
               m_Index[i] = m_Index[j];
               m_Index[j] = tmp;
            }
         }
      }
   }
   
   ///////////////////////////////////////////////////////////////////////////////
   public int getSize() {
      return m_Index.length;
   }
   
   ///////////////////////////////////////////////////////////////////////////////
   public String getLabel(int i) {
      int ix = m_Index[i];
//System.out.printf("i %d ix %d m_Values[ix] %d m_CounterIndex[ix] %d m_Offset[ix] %d%n", i, ix, m_Values[ix], m_CounterIndex[ix], m_Offset[ix]);
      return m_Sic.get(m_CounterIndex[ix]).getLabel(m_Offset[ix]);
   }
   
   ///////////////////////////////////////////////////////////////////////////////
   public int getValue(int i) {
      return m_Values[m_Index[i]];
   }
   
   ///////////////////////////////////////////////////////////////////////////////
   public int getMax() {
      int max = -Integer.MAX_VALUE;
      for (int i = 0; i < m_Values.length; ++i) {
         max = Math.max(max, m_Values[i]);
      }
      return max;
   }
   
   ///////////////////////////////////////////////////////////////////////////////
   public double getPercent(int i) {
      return m_Percent[m_Index[i]];
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public int calcPercents() {
      m_Percent = new double[m_Index.length];
      int pcDigits = 1;
      int ci = -1;
      int size = 0;
      double factor = 0.0;
      for (int i = 0; i < m_CounterIndex.length; ++i) {
         if (m_CounterIndex[i] != ci) {
            ci = m_CounterIndex[i];
            double sum = 0.0;
            int j = i;
            for (; j < m_CounterIndex.length && m_CounterIndex[i] == ci; ++j) {
               sum += m_Values[j];
            }
            size = j - i;
            factor = 100.0 / sum;
         }
         m_Percent[i] = m_Values[i] * factor;
         if (m_Percent[i] >= 10.0) {
            if (m_Percent[i] >= 100.0) {
               pcDigits = Math.max(pcDigits, 3);
            } else {
               pcDigits = Math.max(pcDigits, 2);
            }
         }
      }
      return pcDigits;
   }

   ///////////////////////////////////////////////////////////////////////////////
   public int getMaxDigits() {
      int maxDigits = 1;
      for (int i = 0; i < m_Values.length; ++i) {
         int digits = 0;
         for (int val = m_Values[i]; val > 0; val /= 10) {
            ++digits;
         }
         if (digits > maxDigits) {
            maxDigits = digits;
         }
      }
      return maxDigits;
   }

   ///////////////////////////////////////////////////////////////////////////////
   public String toString() {
      return String.format("m_Sic.size() %d", m_Sic.size());
   }
   
   // Private /////////////////////////////////////////////////////////////////

   // Data ////////////////////////////////////////////////////////////////////
   private ArrayList<SharedIndexableInts> m_Sic;
   private int[] m_Values;
   private int[] m_CounterIndex;
   private int[] m_Index;
   private int[] m_Offset;
   private double[] m_Percent;
}
