/**
 * Copyright 2015 Pushkar Piggott
 *
 * LookupBuilder.java
 */
package pkp.lookup;

import java.util.ArrayList;
import java.net.URL;
import pkp.io.LineReader;
import pkp.io.SpacedPairReader;
import pkp.io.Io;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class LookupBuilder {

   ////////////////////////////////////////////////////////////////////////////
   public static final boolean sm_REVERSE = true;

   ////////////////////////////////////////////////////////////////////////////
   public static Lookup read(URL url, String comment,
                             boolean reverse, boolean mustExist,
                             int size,  boolean duplicateKeys,
                             Io.StringToInt si1, Io.StringToInt si2) {
      LookupBuilder lb = new LookupBuilder(size, duplicateKeys);
      SpacedPairReader spr = new SpacedPairReader(url, comment, mustExist);
      String first;
      while ((first = spr.getNextFirst()) != null) {
         String second = spr.getNextSecond();
         int i1 = si1.cvt(first);
         int i2 = si2.cvt(second);
         if (i1 == Io.ParseFailed || i2 == Io.ParseFailed) {
            Log.err(String.format("Failed to parse \"%s %s\" in line %d of \"%s\".",
                                  first, second, spr.getLineNumber(), url.getPath()));
         }
         if (reverse) {
            lb.add(i2, i1);
         } else {
            lb.add(i1, i2);
         }
      }
      spr.close();
      return lb.buildLookup();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static LookupSet readSet(URL url, String comment,
                                   boolean mustExist, int size,
                                   Io.StringToInt si) {
      LookupBuilder lb = new LookupBuilder(size, false);
      LineReader lr = new LineReader(url, comment, mustExist);
      String line;
      while ((line = lr.readLine()) != null) {
         int i = si.cvt(line);
         if (i == Io.ParseFailed) {
            Log.err(String.format("Failed to parse \"%s\" in line %d of \"%s\".",
                                  line, lr.getLineNumber(), url.getPath()));
         }
         lb.add(i);
      }
      lr.close();
      return lb.buildLookupSet();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static LookupSet buildLookupSet(byte[] values) {
      int max = 0;
      for (int i = 0; i < values.length; ++i) {
         max = Math.max(max, values[i]);
      }
      LookupBuilder ib = new LookupBuilder(max, false);
      for (int i = 0; i < values.length; ++i) {
         ib.add(values[i]);
      }
      return ib.buildLookupSet();
   }

   ////////////////////////////////////////////////////////////////////////////
   public LookupBuilder(int tableSize, boolean duplicateKeys) {
      m_Lookup = new ArrayList<ArrayList<Integer>>(tableSize);
      m_DuplicateKeys = duplicateKeys;
      m_Overflow = new ArrayList<ArrayList<Integer>>();
      for (int i = 0; i < tableSize; ++i) {
         m_Lookup.add(new ArrayList<Integer>());
      }
      m_ScanSize = 0;
   }

   ////////////////////////////////////////////////////////////////////////////
   public void add(int key) {
      add(key, LookupImplementation.sm_TRUE);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void add(int key, int index) {
      add(key, Lookup.sm_NO_VALUE, index);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void add(int key1, int key2, int index) {
//System.out.printf("add: %d %d: %d \n", key1, key2, index);
      ArrayList<Integer> entry = null;
      if (key1 < m_Lookup.size()) {
//System.out.printf("add: table entry\n");
         entry = m_Lookup.get(key1);
      } else {
         for (int i = 0; ; ++i) {
            if (i >= m_Overflow.size()) {
               entry = new ArrayList<Integer>();
               m_Overflow.add(entry);
               break;
            }
            entry = m_Overflow.get(i);
//System.out.printf("add: m_Overflow.get(%d)\n", i);
            if (key1 == entry.get(0)) {
               break;
            }
         }
      }
      if (entry.size() == 0) {
//System.out.printf("add: new\n");
         entry.add(new Integer(key1));
      } else if (!m_DuplicateKeys) {
//System.out.printf("add new: %d %d: %d (%d exist)\n", key1, key2, index, (entry.size() - 1) / 2);
         for (int i = 1; i < entry.size(); i += 2) {
//System.out.printf("add exist: %d %d: %d\n", key1, entry.get(i), entry.get(i + 1));
            if (entry.get(i) == key2) {
//System.out.printf("add replace %d with %d\n", entry.get(i + 1), index);
               entry.set(i + 1, index);
               return;
            }
         }
      }
      entry.add(new Integer(key2));
      entry.add(new Integer(index));
      int size = entry.size();
//System.out.printf("add: size %d\n", size);
      if (key2 == Lookup.sm_NO_VALUE) {
         if (entry.size() == 3) {
         } else if (entry.size() == 5) {
            m_ScanSize += 5;
         } else {
            m_ScanSize += 2;
         }
      } else {
         if (size == 3) {
            m_ScanSize += 3;
         } else {
            m_ScanSize += 2;
         }
      }
//System.out.printf("add: m_ScanSize %d\n", m_ScanSize);
   }

   ////////////////////////////////////////////////////////////////////////////
   public Lookup buildLookup() {
      return build();
   }

   ////////////////////////////////////////////////////////////////////////////
   public LookupSet buildLookupSet() {
      return build();
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private LookupImplementation build() {
//System.out.printf("build: size %d scanSize %d oflowSize %d\n", m_Lookup.size(), m_ScanSize, m_Overflow.size());
      LookupImplementation lookup = new LookupImplementation(m_Lookup.size(), m_ScanSize, m_Overflow.size());
      for (int i = 0; i < m_Lookup.size(); ++i) {
         ArrayList<Integer> entry = m_Lookup.get(i);
//System.out.printf("build: i %d entry.size() %d\n", i, entry.size());
         if (entry.size() == 0) {
            lookup.empty(i);
         } else {
            lookup.add(i, entry);
         }
      }
      for (int i = 0; i < m_Overflow.size(); ++i) {
         ArrayList<Integer> entry = m_Overflow.get(i);
//System.out.printf("build m_Overflow: i %d entry.size() %d\n", i, entry.size());
         if (entry.size() != 0) {
            lookup.add(entry.get(0), entry);
         }
      }
      if (lookup.getTableSize() != m_Lookup.size()) {
         Log.err(String.format("table size: expected %d actual %d%n", m_Lookup.size(), lookup.getTableSize()));
      }
      if (lookup.getOverflowUsed() != m_Overflow.size()  * 2) {
         Log.err(String.format("overflow size: expected %d actual %d%n", m_Overflow.size() * 2, lookup.getOverflowUsed()));
      }
      if (lookup.getScanUsed() > 0 && lookup.getScanUsed() != m_ScanSize + 1) {
         Log.err(String.format("scan size: expected %d actual %d%n", m_ScanSize, lookup.getScanUsed()));
      }

//System.out.printf("build: end\n");
      return lookup;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private ArrayList<ArrayList<Integer>> m_Lookup;
   private ArrayList<ArrayList<Integer>> m_Overflow;
   private int m_ScanSize;
   private boolean m_DuplicateKeys;

   // Main /////////////////////////////////////////////////////////////////////
   public static void main (String[] args) {
      if (args.length == 0) {
         System.out.printf("usage: <table size>, [<key> [<scan>] <target>,]... - [<key> [<scan>],]...\n");
         return;
      }
      int step = 0;
      int in[] = new int[3];
      LookupBuilder builder = null;
      LookupImplementation lookup = null;
      for (int arg = 0; arg < args.length; ++arg) {
         switch (args[arg].charAt(0)) {
         case '0': case '1': case '2': case '3': case '4':
         case '5': case '6': case '7': case '8': case '9':
            String str = args[arg];
            int end = str.indexOf(',');
            // trailing comma on last arg
            if (end == -1 && arg == args.length - 1) {
               end = str.length();
               str += ',';
            }
            // allow commas to bang up against values
            if (end != -1) {
               args[arg] = str.substring(end);
               --arg;
               str = str.substring(0, end);
            }
            in[step] = Long.decode(str).intValue();
//System.out.printf("step %d in[step] %d%n", step, in[step]);
            ++step;
            break;
         case ',':
         case '-':
            switch (step) {
            case 0:
               System.out.println("missing values 0");
               System.exit(0);
            case 1:
               if (builder == null) {
                  builder = new LookupBuilder(in[0], false);
//System.out.println("new builder");
                  break;
               }
               if (lookup != null) {
                  System.out.printf("[%4d]: %d%n", in[0], lookup.get(in[0]));
                  break;
               }
               System.out.println("missing value ,");
               System.exit(0);
            case 2:
               if (builder == null) {
                  System.out.println("missing initial size value");
                  System.exit(0);
               }
               if (lookup != null) {
                  System.out.printf("[%4d]: (%d) ", in[0], lookup.get(in[0]));
                  System.out.printf("[%4d]: %d%n", in[1], lookup.get(in[0], in[1]));
               }
//System.out.printf("in[0] %d in[1] %d%n", in[0], in[1]);
               builder.add(in[0], in[1]);
               break;
            case 3:
               if (lookup != null) {
                  System.out.println("too many values");
                  System.exit(0);
               }
//System.out.printf("in[0] %d in[1] %d , in[2] %d%n", in[0], in[1], in[2]);
               builder.add(in[0], in[1], in[2]);
               break;
            default:
               System.out.println("too many values");
               System.exit(0);
            }
            step = 0;
            if (args[arg].charAt(0) == '-') {
               lookup = builder.build();
               System.out.println(lookup + "Lookups:");
            }
            break;
         }
      }
   }
}
