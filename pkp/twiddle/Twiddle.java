/**
 * Copyright 2015 Pushkar Piggott
 *
 * Twiddle.java
 */
 
package pkp.twiddle;

import pkp.io.SpacedPairReader;

///////////////////////////////////////////////////////////////////////////////
public class Twiddle extends java.lang.Object {

	//////////////////////////////////////////////////////////////////////////
	public interface Restricter {
	   public boolean isAllowed(Twiddle t);
	}

   ////////////////////////////////////////////////////////////////////////////
   public static final int sm_VALUES = Chord.sm_VALUES | ThumbKeys.sm_VALUES << 8;

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
      final Modifiers mods[] = Modifiers.getCombinations(getThumbKeys().toModifiers());
      for (int i = 0; i < mods.length; ++i) {
         Twiddle tw = new Twiddle(getChord(), getThumbKeys().minus(mods[i]));
         KeyPressList kpl = map.getKeyPressList(tw);
         if (kpl != null) {
			return kpl.createModified(mods[i]);
         }
      }
      return null;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isValid() { return m_Chord != null && m_Chord.isValid(); }
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
