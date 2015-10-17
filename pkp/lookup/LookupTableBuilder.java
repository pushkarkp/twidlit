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
public class LookupTableBuilder extends LookupBuilder {

   ////////////////////////////////////////////////////////////////////////////
   public static final boolean sm_SWAP_KEYS = true;

   ////////////////////////////////////////////////////////////////////////////
   public static LookupTable read(URL url, 
                                  String comment,
                                  boolean reverse, 
                                  boolean mustExist,
                                  LookupBuilder.Duplicates duplicates,
                                  int minFreq, 
                                  int maxFreq,
                                  Io.StringToInt si1, 
                                  Io.StringToInt si2) {
      LookupTableBuilder lb = new LookupTableBuilder(minFreq, maxFreq);
      lb.setDuplicates(duplicates);
      lb.setMessage(String.format(" in %s", url.getPath()));
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
      return lb.build();
   }

   ////////////////////////////////////////////////////////////////////////////
   public LookupTableBuilder(int minFreq, int maxFreq) {
      super(minFreq, maxFreq - minFreq + 1);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void add(int key, int index) {
      super.newEntry(key, LookupTable.sm_NO_VALUE, index);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void add(int key1, int key2, int index) {
      super.newEntry(key1, key2, index);
   }

   ////////////////////////////////////////////////////////////////////////////
   public LookupTable build() {
      return super.implement();
   }

   // Main /////////////////////////////////////////////////////////////////////
   public static void main (String[] args) {
      if (args.length == 0) {
         System.out.printf("usage: [<offset>] <table size>, [<key> [<scan>] <target>,]... - [<key> [<scan>],]...\n");
         return;
      }
      int step = 0;
      int in[] = new int[3];
      LookupTableBuilder builder = null;
      LookupTable lookup = null;
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
                  builder = new LookupTableBuilder(0, in[0]);
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
                  builder = new LookupTableBuilder(in[0], in[1]);
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
}
