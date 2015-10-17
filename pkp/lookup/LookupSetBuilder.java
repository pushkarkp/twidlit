/**
 * Copyright 2015 Pushkar Piggott
 *
 * LookupBuilder.java
 */
package pkp.lookup;

import java.net.URL;
import pkp.io.LineReader;
import pkp.io.SpacedPairReader;
import pkp.io.Io;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class LookupSetBuilder extends LookupBuilder {

   ////////////////////////////////////////////////////////////////////////////
   public static final boolean sm_SWAP_KEYS = true;

   ////////////////////////////////////////////////////////////////////////////
   public static LookupSet read(URL url, 
                                boolean mustExist, 
                                int minFreq, 
                                int maxFreq,
                                Io.StringToInt si) {
      LookupSetBuilder lb = new LookupSetBuilder(minFreq, maxFreq);
      lb.setMessage(String.format(" in %s", url.getPath()));
      LineReader lr = new LineReader(url, mustExist);
      String line;
      while ((line = lr.readLine()) != null) {
         int i = si.cvt(line);
         if (i == Io.sm_PARSE_FAILED) {
            Log.err(String.format("Failed to parse \"%s\" in line %d of \"%s\".",
                                  line, lr.getLineNumber(), url.getPath()));
         }
         lb.add(i);
      }
      lr.close();
      return lb.build();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static LookupSet read2(URL url, 
                                 boolean reverse, 
                                 boolean mustExist, 
                                 int minFreq, 
                                 int maxFreq,
                                 Io.StringToInt si1,
                                 Io.StringToInt si2) {
      LookupSetBuilder lb = new LookupSetBuilder(minFreq, maxFreq);
      SpacedPairReader spr = new SpacedPairReader(url, mustExist);
      String str1;
      while ((str1 = spr.getNextFirst()) != null) {
         int v1 = si1.cvt(str1);
         int v2 = si2.cvt(spr.getNextSecond());
         if (v1 == Io.sm_PARSE_FAILED || v2 == Io.sm_PARSE_FAILED) {
            Log.err(String.format("Failed to parse \"%s\", line %d of \"%s\".",
                                  spr.getNextLine(), spr.getLineNumber(), url.getPath()));
         }
         if (reverse) {
            lb.add(v2, v1);
         } else {
            lb.add(v1, v2);
         }
      }
      spr.close();
      return lb.build();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static LookupSet buildLookupSet(byte[] values) {
      int min = Integer.MAX_VALUE;
      int max = 0;
      for (int i = 0; i < values.length; ++i) {
         min = Math.min(min, values[i]);
         max = Math.max(max, values[i]);
      }
      LookupSetBuilder ib = new LookupSetBuilder(min, max - min);
      for (int i = 0; i < values.length; ++i) {
         ib.add(values[i]);
      }
      return ib.build();
   }

   ////////////////////////////////////////////////////////////////////////////
   public LookupSetBuilder(int minFreq, int maxFreq) {
      super(minFreq, maxFreq - minFreq + 1);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void add(int key) {
      newEntry(key, LookupTable.sm_NO_VALUE, LookupImplementation.sm_TRUE);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void add(int key1, int key2) {
      newEntry(key1, key2, LookupImplementation.sm_TRUE);
   }

   ////////////////////////////////////////////////////////////////////////////
   public LookupSet build() {
      return super.implement();
   }

   // Private /////////////////////////////////////////////////////////////////
/*
   // Main /////////////////////////////////////////////////////////////////////
   public static void main (String[] args) {
      if (args.length == 0) {
         System.out.printf("usage: [<offset>] <table size>, [<key> [<scan>] <target>,]... - [<key> [<scan>],]...\n");
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
         case '-':
         case ',':
            switch (step) {
            case 0:
               System.out.println("missing values");
               System.exit(0);
            case 1:
               if (builder == null) {
                  builder = new LookupBuilder(0, in[0], false);
//System.out.printf("new builder zero %d%n", in[0]);
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
                  builder = new LookupBuilder(in[0], in[1], false);
//System.out.printf("new builder %d %d%n", in[0], in[1]);
                  break;
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
*/}
