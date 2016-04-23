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
   public StringsIntsBuilder(URL url, Io.StringToIntErr si, boolean singleValue) {
      this(url, si, singleValue, Log.Level.ERROR);
   }

   ////////////////////////////////////////////////////////////////////////////
   public StringsIntsBuilder(URL url, Io.StringToIntErr si, boolean singleValue, Log.Level parseFailLogLevel) {
      this();
      SpacedPairReader spr = new 
         SpacedPairReader(url, Io.sm_MUST_EXIST, parseFailLogLevel);
      spr.setSingleToken(singleValue);
      String value;
      StringBuilder err = new StringBuilder();
      while ((value = spr.getNextFirst()) != null) {
         int i = si.cvt(value, err);
         if (i == Io.sm_PARSE_FAILED) {
            Log.parseErr(spr, err.toString(), value);
            err = new StringBuilder();
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
