/**
 * Copyright 2015 Pushkar Piggott
 *
 * LookupImplementation.java
 *
 * A lookup that allows multiple values and sparseness.
 * Target values cannot be negative.
 * Key values can be negative if offset compensates(?). 
 */
package pkp.lookup;

import java.util.ArrayList;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
class LookupImplementation implements LookupTable, LookupSet {

   ////////////////////////////////////////////////////////////////////////////
   static final int sm_TRUE = 1;

   ////////////////////////////////////////////////////////////////////////////
   LookupImplementation(int offset, int tableSize, int scanSize, int overflowSize) {
//System.out.printf("create: tableSize %d scanSize %d overflowSize %d\n", tableSize, scanSize, overflowSize);
	   m_Offset = offset;
	   m_Lookup = new int[tableSize];
      m_Overflow = new int[overflowSize * 2];
      m_OverflowUsed = 0;
      if (scanSize > 0) {
         // skip m_Scan[0] as a 0 in m_Lookup means its empty
         m_Scan = new int[scanSize + 1];
         m_ScanUsed = 1;
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isSingleKeyed() {
      return m_Scan == null;
   }

   ////////////////////////////////////////////////////////////////////////////
   void empty(int key) {
      key -= m_Offset;
      // only when uninitialized
      if (key < m_Lookup.length && m_Lookup[key] == 0) {
         m_Lookup[key] = sm_NO_VALUE;
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   void add(int key, ArrayList<Integer> scanValues) {
      key -= m_Offset;
      int size = scanValues.size();
//System.out.printf("add1: key %d m_Lookup.length %d scansize %d\n", key, m_Lookup.length, size);
      if (0 <= key && key < m_Lookup.length) {
         if (size == 3 && scanValues.get(1) == sm_NO_VALUE) {
//System.out.printf("add1: no scan %d m_Lookup[%d] = %d\n", m_Lookup[key], key, scanValues.get(2));
            m_Lookup[key] = scanValues.get(2);
            return;
         } else {
//System.out.printf("add1: m_Lookup[%d] = -%d\n", key, m_ScanUsed);
            m_Lookup[key] = -m_ScanUsed;
         }
      } else {
//System.out.printf("add1: m_OverflowUsed %d%n", m_OverflowUsed);
         if (m_OverflowUsed + 2 > m_Overflow.length) {
            Log.err(String.format("m_Overflow.length %d m_OverflowUsed %d\n", m_Overflow.length, m_OverflowUsed));
         }
         m_Overflow[m_OverflowUsed++] = key;
         if (size == 3 && scanValues.get(1) == sm_NO_VALUE) {
            m_Overflow[m_OverflowUsed++] = scanValues.get(2);
            return;
         } else {
            m_Overflow[m_OverflowUsed++] = -m_ScanUsed;
         }
      }
//System.out.printf("add1: m_Scan[%d++] (length %d) = %d (size)\n", m_ScanUsed, m_Scan.length, size);
      m_Scan[m_ScanUsed++] = size;
      for (int i = 1; i < size; ++i) {
//System.out.printf("add1: m_Scan[%d++] = %d\n", m_ScanUsed, scanValues.get(i));
         m_Scan[m_ScanUsed++] = scanValues.get(i);
      }
//System.out.print("add1: " + foundToString(key, get(key)));
   }

   ////////////////////////////////////////////////////////////////////////////
   // return the value stored (even if a scan index)
   // or sm_NO_VALUE if none or out of range
   @Override // LookupTable
   public int get(int key1) {
      key1 -= m_Offset;
      if (0 <= key1 && key1 < m_Lookup.length) {
//System.out.printf("get: m_Lookup[%d] %d%n", key1, m_Lookup[key1]);
         return m_Lookup[key1];
      }
      for (int i = 0; i < m_Overflow.length; i += 2) {
         if (m_Overflow[i] == key1) {
            return m_Overflow[i + 1];
         }
      }
      return sm_NO_VALUE;
   }

   ////////////////////////////////////////////////////////////////////////////
   // return the first matching index or -1
   @Override // LookupTable
   public int get(int key1, int key2) {
      int found = get(key1);
      if (found == sm_NO_VALUE) {
//System.out.printf("get: no value (key1 0x%x)\n", key1);
         return sm_NO_VALUE;
      }
      if (found >= 0) {
         if (key2 == sm_NO_VALUE) {
//System.out.printf("get: found %d (no key2)\n", found);
            return found;
         }
//System.out.printf("get: no value (key1 0x%x key2 0x%x)\n", key1, key2);
         return sm_NO_VALUE;
      }
      int start = -found;
      int size = getSize(start);
      for (int i = 1; i < size; i += 2) {
         if (m_Scan[start + i] == key2) {
//System.out.printf("get: key1 %d key2 %d found %d\n", key1, key2, found);
            return m_Scan[start + i + 1];
         }
      }
      return sm_NO_VALUE;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // LookupSet
   public boolean is(int key) {
//System.out.printf("0x%x: 0x%x%n", key, get(key));
      return get(key) == sm_TRUE;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // LookupSet
   public boolean is(int key1, int key2) {
//System.out.printf("0x%x 0x%x: 0x%x%n", key1, key2, get(key1, key2));
      return get(key1, key2) == sm_TRUE;
   }

   ////////////////////////////////////////////////////////////////////////////
   // return all matching indices
   @Override // LookupTable
   public int[] getAll(int key1, int key2) {
//System.out.printf("getAll: key1 0x%x, key2 0x%x\n", key1, key2);
      int found = get(key1);
//System.out.printf("getAll: found %x\n", found);
      if (found >= 0) {
         if (key2 == sm_NO_VALUE) {
//System.out.printf("getAll: found %x\n", found);
            return new int[] {2, found};
         }
//System.out.println("getAll: return null 1");
         return null;
      }
      if (found == sm_NO_VALUE) {
//System.out.println("getAll: return null 2");
         return null;
      }
      int start = -found;
      int size = getSize(start);
      int[] all = new int[size / 2 + 1];
      int j = 0;
      for (int i = 1; i < size; i += 2) {
//System.out.printf("getAll found: key2 %d: %d\n", m_Scan[start + i], m_Scan[start + i + 1]);
         if (m_Scan[start + i] == key2) {
//System.out.printf("getAll found: %d\n", m_Scan[start + i + 1]);
            all[++j] = m_Scan[start + i + 1];
         }
      }
      if (j == 0) {
//System.out.println("getAll: return null 3");
         return null;
      }
      all[0] = j + 1;
      return all;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      String str = "";
      for (int i = 0; i < m_Lookup.length; ++i) {
         int k = i + m_Offset;
         str += foundToString(k, get(k));
      }
      for (int i = 0; i < m_OverflowUsed; i += 2) {
         str += foundToString(m_Overflow[i] + m_Offset, m_Overflow[i + 1]);
      }
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   int getOffset() { return m_Offset; }
   int getTableSize() { return m_Lookup.length; }
   int getOverflowUsed() { return m_OverflowUsed; }
   int getScanUsed() { return m_ScanUsed; }
      
   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private String foundToString(int k, int found) {
//System.out.printf("foundToString: k %d found %d\n", k, found);
      String str = String.format("0x%x: ", k);
      if (found >= 0) {
         str += String.format("%d%n", found);
      } else if (found == sm_NO_VALUE) {
         str += "no value\n";
      } else {
         str += String.format("%d ", found) + scanToString(-found);
      }
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   private String scanToString(int start) {
      String str = "";
      int size = getSize(start);
//System.out.printf("scanToString start: %d size: %d\n", start, size);
      for (int j = 1; j < size; j += 2) {
         str += String.format("%4d: %d", m_Scan[start + j], m_Scan[start + j + 1]);
      }
      str += '\n';
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   private int getSize(int start) {
		if (m_Scan == null) {
			return 0;
		}
      int size = m_Scan[start];
      if (m_ScanUsed <= start ||
          m_ScanUsed < start + size) {
         // this is an error
         return -1;
      }
      return size;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private int m_Offset;
   private int[] m_Lookup;
   private int[] m_Overflow;
   private int m_OverflowUsed;
   private int[] m_Scan;
   private int m_ScanUsed;
}
