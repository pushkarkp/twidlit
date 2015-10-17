/**
 * Copyright 2015 Pushkar Piggott
 *
 * Twiddle.java
 */
 
package pkp.twiddle;

import java.util.ArrayList;
import java.net.URL;
import pkp.io.LineReader;
import pkp.io.SpacedPairReader;
import pkp.io.Io;
import pkp.util.Pref;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class Twiddle extends java.lang.Object {

	//////////////////////////////////////////////////////////////////////////
	public interface Restricter {
	   public boolean isAllowed(Twiddle t);
	}

   ////////////////////////////////////////////////////////////////////////////
   public static final int sm_VALUES = Chord.sm_VALUES | ThumbKeys.sm_VALUES << 8;

   ////////////////////////////////////////////////////////////////////////////
   public static ArrayList<Twiddle> read(URL url) {
      LineReader lr = new LineReader(url, Io.sm_MUST_EXIST);
      if (lr == null) {
         return null;
      }
      ArrayList<Twiddle> twiddles = new ArrayList<Twiddle>();
      String line;
      for (int i = 1; (line = lr.readLine()) != null; ++i) {
         Twiddle tw = new Twiddle(line);
         if (!tw.isValid()) {
            Log.warn(String.format("Failed to read line %d \"%s\" of \"%s\"", i, line, url.getPath()));
         } else {
//System.out.println(tw);
            twiddles.add(tw);
         }
      }
      lr.close();
      return twiddles;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public Twiddle() {
      m_Chord = new Chord(0);
      m_ThumbKeys = new ThumbKeys(0);
   }

   ////////////////////////////////////////////////////////////////////////////
   public Twiddle(Chord chord, ThumbKeys thumbKeys) {
      m_Chord = chord;
      m_ThumbKeys = thumbKeys;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Twiddle(int twiddle) {
      m_Chord = new Chord(twiddle & Chord.sm_VALUES);
      m_ThumbKeys = new ThumbKeys((twiddle >> 8) & ThumbKeys.sm_VALUES);
   }

   ////////////////////////////////////////////////////////////////////////////
   public Twiddle(int chord, int thumbKeys) {
      m_Chord = new Chord(chord & Chord.sm_VALUES);
      m_ThumbKeys = new ThumbKeys(thumbKeys & ThumbKeys.sm_VALUES);
   }

   ////////////////////////////////////////////////////////////////////////////
   public Twiddle(Twiddle tw, Modifiers m) {
      m_Chord = new Chord(tw.getChord().toInt());
      m_ThumbKeys = new ThumbKeys(tw.getThumbKeys().toInt() | ThumbKeys.fromModifiers(m));
   }

   ////////////////////////////////////////////////////////////////////////////
   public Twiddle(String in) {
      String str = in.trim();
      int split = SpacedPairReader.findWhiteSpace(str);
      if (split != -1) {
         m_ThumbKeys = new ThumbKeys(str.substring(0, split));
         m_Chord = new Chord(str.substring(split).trim());
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public Twiddle createModified(Modifiers mod) {
		return new Twiddle(m_Chord, m_ThumbKeys.plus(mod));
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean equals(Object o) {
      Twiddle other = (Twiddle)o;
      return other != null
          && other.m_Chord.equals(m_Chord)
          && other.m_ThumbKeys.equals(m_ThumbKeys);
   }

   ////////////////////////////////////////////////////////////////////////////
   public KeyPressList getKeyPressList(KeyMap map) {
      if (getThumbKeys().isEmpty()) {
         // no modifiers, no options
         return map.getKeyPressList(this);
      } else {
         // try stripping off modifiers
         final Modifiers mods[] = Modifiers.getCombinations(getThumbKeys().toModifiers());
         for (int i = 0; i < mods.length; ++i) {
//System.out.println("Twiddle getKeyPressList " + mods[i].toString());
            Twiddle tw = new Twiddle(getChord(), getThumbKeys().minus(mods[i]));
            KeyPressList kpl = map.getKeyPressList(tw);
            if (kpl != null) {
               return kpl.createModified(mods[i]);
            }
         }
      }
      return null;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isValid() {
      return m_Chord != null
          && m_Chord.isValid()
          && m_ThumbKeys != null
          && m_ThumbKeys.isValid();
   }

   ////////////////////////////////////////////////////////////////////////////
   public Chord getChord() { return m_Chord; }
   public ThumbKeys getThumbKeys() { return m_ThumbKeys; }
   public int toInt() { return (m_ThumbKeys.toInt() << 8) + m_Chord.toInt(); }
   public int toCfg() { return m_ThumbKeys.toCfg() | m_Chord.toCfg(); }
   public String toString() { return m_ThumbKeys.toString() + " " + m_Chord.toString(); }

   // Data ////////////////////////////////////////////////////////////////////
   private Chord m_Chord;
   private ThumbKeys m_ThumbKeys;

   // Main ////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      for (String arg: args) {
         Twiddle keys = new Twiddle(arg);
         System.out.printf("%s\n", keys.toString());
      }
   }
}
