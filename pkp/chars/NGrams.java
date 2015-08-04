/**
 * Copyright 2015 Pushkar Piggott
 *
 * NGrams.java
 */
package pkp.chars;

import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import pkp.lookup.LookupBuilder;
import pkp.lookup.LookupSet;
import pkp.lookup.Lookup;
import pkp.lookup.SharedIndexableInts;
import pkp.io.LineReader;
import pkp.io.Io;
import pkp.util.Pref;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
class NGrams implements SharedIndexableInts {
   
   ////////////////////////////////////////////////////////////////////////////
   NGrams(URL url) {
      m_CurrentIndex = new ArrayList<Integer>();
      m_Current = new ArrayList<NGram>();
      m_NGRAMS = read(url);
      m_Counts = new ArrayList<Integer>(m_NGRAMS.size());
      for (int i = 0; i < m_NGRAMS.size(); ++i) {
         m_Counts.add(0);
      }
      LookupBuilder lbr = new LookupBuilder(128, false);
      LookupBuilder lbs = new LookupBuilder(128, true);
      for (int i = 0; i < m_NGRAMS.size(); ++i) {
         String chars = m_NGRAMS.get(i);
         for (int j = 0; j < chars.length(); ++j) {
//System.out.printf("a>%c<%n", chars.charAt(j));
            lbr.add((int)chars.charAt(j));
         }
//System.out.printf("i>%c< %d%n", chars.charAt(0), i);
         lbs.add((int)chars.charAt(0), i + 1);
      }
      m_RELEVANT = lbr.buildLookupSet();
      m_START = lbs.buildLookup();
//System.out.println(m_START);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   @Override // SharedIndexableInts
   public int getSize() {
      return m_Counts.size();
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // SharedIndexableInts
   public String getLabel(int i) {
      return (new String(new char[15 - m_NGRAMS.get(i).length()]).replace('\0', ' ')) + m_NGRAMS.get(i);
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // SharedIndexableInts
   public int getCount(int i) {
      return m_Counts.get(i);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void nextChar(char c) {
      if (!m_RELEVANT.is(c)) {
         if (m_Current.size() > 0) {
            m_Current.clear();
            m_CurrentIndex.clear();
         }
         return;
      }
//System.out.printf("[size %d%n", m_CurrentIndex.size());
      for (int i = m_Current.size() - 1; i >= 0; --i) {
         if (!m_Current.get(i).nextChar(c)) {
//System.out.println("->" + m_Current.get(i).getChars() + "<");
            m_Current.remove(i);
            m_CurrentIndex.remove(i);
         } else if (m_Current.get(i).matched()) {
//System.out.printf("!>%c< i %d >%s<%n", c, i, m_Current.get(i).getChars());
            m_Counts.set(m_CurrentIndex.get(i), m_Counts.get(m_CurrentIndex.get(i)) + 1);
            m_Current.remove(i);
            m_CurrentIndex.remove(i);
         }
      }
      int[] indexes = m_START.getAll((int)c, Lookup.sm_NO_VALUE);
//System.out.printf("?>%c< indexes[0] %d ", c, indexes[0]);
      for (int i = 1; i < indexes[0]; ++i) {
//System.out.printf("i %d ix %d ", i, indexes[i]);
         if (indexes[i] > 0) {
            m_Current.add(new NGram(m_NGRAMS.get(indexes[i] - 1)));
//System.out.printf("+>%s< ", m_NGRAMS.get(indexes[i] - 1));
            m_CurrentIndex.add(indexes[i] - 1);
         }
      }
//System.out.println();
//System.out.printf("]size %d%n", m_CurrentIndex.size());
   }
   
   ////////////////////////////////////////////////////////////////////////////
   int maxLength() {
      int maxLength = 0;
      for (int i = 0; i < m_NGRAMS.size(); ++i) {
         maxLength = Math.max(maxLength, m_NGRAMS.get(i).length());
      }
      return maxLength;
   }
   
   // Private /////////////////////////////////////////////////////////////////
   
   ////////////////////////////////////////////////////////////////////////////
   private static ArrayList<String> read(URL url) {
      ArrayList<String> nGrams = new ArrayList<String>();
      if (url == null) {
         return nGrams;
      }
      LineReader lr = new LineReader(url, Pref.get("comment", "#"), true);
      String line;
      for (int i = 1; (line = lr.readLine()) != null; ++i) {
         String ng = Io.parseChars(line);
         if (!NGram.isValid(ng)) {
            Log.log(String.format("Failed to add line %d \"%s\" of \"%s\"", i, line, url.getPath()));
         } else {
//System.out.println("<" + ng + ">");
            nGrams.add(ng);
         }
      }
      lr.close();
      return nGrams;
   }
   
   // Data ////////////////////////////////////////////////////////////////////
   private final ArrayList<String> m_NGRAMS;
   private final LookupSet m_RELEVANT;
   private final Lookup m_START;
   private ArrayList<NGram> m_Current;
   private ArrayList<Integer> m_CurrentIndex;
   private ArrayList<Integer> m_Counts;

   // Main /////////////////////////////////////////////////////////////////////
   public static void main(String[] argv) {
      Pref.init("TwidlitPreferences.txt", "pref", "pref");
      Pref.setIconPath("/data/icon.gif");
      Log.init(Io.createFile(".", "log.txt"), Log.ExitOnError);
      URL url = null;
      try {
         url = (new File(argv[0])).toURI().toURL();
      } catch (MalformedURLException e) {
         Log.err("Failed to create URL from \"" + argv[0] + "\".");
      }
      NGrams nGrams = new NGrams(url);
      System.out.printf("maxLength %d%n", nGrams.maxLength());
      try {
         url = (new File(argv[1])).toURI().toURL();
      } catch (MalformedURLException e) {
         Log.err("Failed to create URL from \"" + argv[1] + "\".");
      }
      LineReader lr = new LineReader(url, "#", true);
      String line;
      for (int i = 1; (line = lr.readLine()) != null; ++i) {
         for (int j = 0; j < line.length(); ++j) {
            nGrams.nextChar(line.charAt(j));
         }
      }
   }
}
