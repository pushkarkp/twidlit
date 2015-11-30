/**
 * Copyright 2015 Pushkar Piggott
 *
 * LookupBuilder.java
 */
package pkp.lookup;

import java.util.ArrayList;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class LookupBuilder {

   ////////////////////////////////////////////////////////////////////////////
   // By default flag duplicate entry as an error.
   public enum Duplicates {
      ERROR,
      IGNORE,
      OVERWRITE,
      STORE
   }

   ////////////////////////////////////////////////////////////////////////////
   LookupBuilder(int offset, int tableSize) {
      m_Offset = offset;
      m_Lookup = new ArrayList<ArrayList<Integer>>(tableSize);
      m_Overflow = new ArrayList<ArrayList<Integer>>();
      for (int i = 0; i < tableSize; ++i) {
         m_Lookup.add(new ArrayList<Integer>());
      }
      m_ScanSize = 0;
      // By default flag duplicate entry as an error.
      m_Duplicates = Duplicates.ERROR;
      m_Msg = "";
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setDuplicates(Duplicates d) {
      m_Duplicates = d;
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setMessage(String msg) {
      m_Msg = msg;
   }

   ////////////////////////////////////////////////////////////////////////////
   void newEntry(int key1, int key2, int index) {
      key1 -= m_Offset;
//System.out.printf("add: %d %d: %d \n", key1, key2, index);
      ArrayList<Integer> entry = null;
      if (key1 >= 0 && key1 < m_Lookup.size()) {
//System.out.printf("add: table entry%n");
         entry = m_Lookup.get(key1);
      } else {
//System.out.printf("add: %d %d: %d \n", key1, key2, index);
         for (int i = 0; ; ++i) {
            if (i >= m_Overflow.size()) {
//System.out.printf("add: m_Overflow.add(%d) %d%n", i, key1);
               entry = new ArrayList<Integer>();
               m_Overflow.add(entry);
               break;
            }
            entry = m_Overflow.get(i);
            if (key1 == entry.get(0)) {
//System.out.printf("add: m_Overflow.get(%d) %d%n", i, key1);
               break;
            }
         }
      }
      if (entry.size() == 0) {
//System.out.printf("add: new\n");
         entry.add(new Integer(key1));
      } else if (m_Duplicates != Duplicates.STORE
              && isHandled(key1, key2, index, entry)) {
         return;
      }
      entry.add(new Integer(key2));
      entry.add(new Integer(index));
      int size = entry.size();
//System.out.printf("add: size %d\n", size);
      if (key2 == LookupTable.sm_NO_VALUE) {
         if (entry.size() == 3) {
         } else if (entry.size() == 5) {
            m_ScanSize += 5;
         } else {
            m_ScanSize += 2;
         }
      } else {
//System.out.printf("add: size %d\n", size);
         if (size == 3) {
            m_ScanSize += 3;
         } else {
            m_ScanSize += 2;
         }
      }
//System.out.printf("add: m_ScanSize %d\n", m_ScanSize);
   }

   ////////////////////////////////////////////////////////////////////////////
   LookupImplementation implement() {
//System.out.printf("build: offset %d size %d scanSize %d oflowSize %d\n", m_Offset, m_Lookup.size(), m_ScanSize, m_Overflow.size());
      LookupImplementation lookup = new LookupImplementation(m_Offset, m_Lookup.size(), m_ScanSize, m_Overflow.size());
      for (int i = 0; i < m_Lookup.size(); ++i) {
         ArrayList<Integer> entry = m_Lookup.get(i);
         int k = i + m_Offset;
//System.out.printf("build: i %d k %d entry.size() %d\n", i, k, entry.size());
         if (entry.size() == 0) {
            lookup.empty(k);
         } else {
            lookup.add(k, entry);
         }
      }
      for (int i = 0; i < m_Overflow.size(); ++i) {
         ArrayList<Integer> entry = m_Overflow.get(i);
         if (entry.size() != 0) {
//System.out.printf("build m_Overflow: i %d entry.size() %d\n", i, entry.size());
            lookup.add(entry.get(0) + m_Offset, entry);
         }
      }
      if (lookup.getTableSize() != m_Lookup.size()) {
         Log.err(String.format("table size: expected %d actual %d%n", m_Lookup.size(), lookup.getTableSize()));
      }
      if (lookup.getOverflowUsed() != m_Overflow.size() * 2) {
         Log.err(String.format("overflow size: expected %d actual %d%n", m_Overflow.size() * 2, lookup.getOverflowUsed()));
      }
      if (lookup.getScanUsed() > 0 && lookup.getScanUsed() != m_ScanSize + 1) {
         Log.err(String.format("scan size: expected %d actual %d%n", m_ScanSize, lookup.getScanUsed()));
      }

//System.out.printf("build: end\n");
      return lookup;
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private boolean isHandled(int key1, int key2, int index, ArrayList<Integer> entry) {
//System.out.printf("add new: %d %d: %d (%d exist)\n", key1, key2, index, (entry.size() - 1) / 2);
      for (int i = 1; i < entry.size(); i += 2) {
//System.out.printf("add exist: %d %d: %d%n", key1, entry.get(i), entry.get(i + 1));
         if (entry.get(i) == key2) {
            Log.Level logLevel = Log.Level.ERROR;
            String action = "";
            switch (m_Duplicates) {
            case ERROR:
               logLevel = Log.Level.ERROR;
               action = "";
               break;
            case IGNORE:
               logLevel = Log.Level.INFO;
               action = " using the former";
               break;
            case OVERWRITE:
               logLevel = Log.Level.INFO;
               action = " using the latter";
               break;
            case STORE:
               Log.err("unexpected Duplicates.STORE");
               return false;
            }
            String msg;
            if (key2 == LookupTable.sm_NO_VALUE) {
               msg = String.format("Same key (%d 0x%x) found for %d 0x%x and %d 0x%x%s%s.", 
                                   key1, key1, entry.get(i + 1), entry.get(i + 1), index, index, action, m_Msg);
            } else {
               msg = String.format("Same key (%d %d) found for %d and %d%s%s.", 
                                   key1, key2, entry.get(i + 1), index, action, m_Msg);
            }
//System.out.println(msg);
            Log.log(logLevel, msg);
            if (m_Duplicates == Duplicates.OVERWRITE) {
               entry.set(i + 1, index);
            }
            return true;
         }
      }
      return false;
   }
               
   // Data ////////////////////////////////////////////////////////////////////
   protected int m_Offset;
   protected ArrayList<ArrayList<Integer>> m_Lookup;
   protected ArrayList<ArrayList<Integer>> m_Overflow;
   protected int m_ScanSize;
   protected Duplicates m_Duplicates;
   private String m_Msg;
}
