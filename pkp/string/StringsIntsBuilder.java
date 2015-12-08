/**
 * Copyright 2015 Pushkar Piggott
 *
 * StringsIntsBuilder.java
 */
package pkp.string;

import java.util.ArrayList;
import java.util.Arrays;
import java.net.URL;
import pkp.io.SpacedPairReader;
import pkp.io.Io;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class StringsIntsBuilder extends java.lang.Object {

   ////////////////////////////////////////////////////////////////////////////
   public StringsIntsBuilder(URL url, boolean singleWord) {
      this(url, singleWord, Io.sm_parseInt);
   }

   ////////////////////////////////////////////////////////////////////////////
   public StringsIntsBuilder(URL url, boolean singleWord, Io.StringToInt si) {
      this();
      SpacedPairReader spr = new SpacedPairReader(url, Io.sm_MUST_EXIST);
      spr.setSingleToken(singleWord);
      String value;
      while ((value = spr.getNextFirst()) != null) {
         int i = si.cvt(value);
         if (i == Io.sm_PARSE_FAILED) {
            Log.err(String.format("Failed to parse \"%s\" in line %d of \"%s\".",
                                  value, spr.getLineNumber(), url.getPath()));
         }
         add(spr.getNextSecond(), i);
      }
      spr.close();
   }


   ////////////////////////////////////////////////////////////////////////////
   public int size() { return m_Strings.size(); }

   ////////////////////////////////////////////////////////////////////////////
   public void add(String str, int i) {
      m_Ints.add(new Integer(i));
      m_Strings.add(new String(str));
   }

   ////////////////////////////////////////////////////////////////////////////
   public StringsInts build() {
      int size = size();
      StringInt[] si = new StringInt[size];
      for (int i = 0; i < size; ++i) {
         si[i] = new StringInt(m_Strings.get(i), m_Ints.get(i));
      }
      Arrays.sort(si);
      return new StringsInts(si);
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private StringsIntsBuilder() {
      m_Strings = new ArrayList<String>();
      m_Ints = new ArrayList<Integer>();
   }

   // Data ////////////////////////////////////////////////////////////////////
   private ArrayList<String> m_Strings;
   private ArrayList<Integer> m_Ints;
}
