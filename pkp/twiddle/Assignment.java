/**
 * Copyright 2015 Pushkar Piggott
 *
 * Assignment.java
 */

package pkp.twiddle;

import java.util.ArrayList;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
// An assignment maps one OR MORE chords to a keypress list.
public class Assignment extends java.lang.Object {

   ////////////////////////////////////////////////////////////////////////////
   public static Assignment sm_NO_ASSIGNMENT = new Assignment(new Twiddle(), new KeyPressList());

   ////////////////////////////////////////////////////////////////////////////
   public static Assignment combine(Assignment a1, Assignment a2) {
      return new Assignment(a1, a2);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Assignment parseLine(String str, StringBuilder err) {
      Twiddle twiddle = new Twiddle(str);
      if (!twiddle.isValid()) {
         return null;
      }
      int eq = str.indexOf('=');
      if (eq == -1) {
         return null;
      }
      str = str.substring(eq + 1).trim();
      Assignment asg = 
         new Assignment(twiddle, 
                        KeyPressList.parseTextAndTags(str, err));
      return asg;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignment(Twiddle tw, KeyPressList kpl) {
      m_Twiddles = new ArrayList<Twiddle>();
      m_Twiddles.add(tw);
      m_KeyPressList = kpl;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignment(ArrayList<Twiddle> tw, KeyPressList kpl) {
      m_Twiddles = new ArrayList<Twiddle>(tw);
      m_KeyPressList = kpl;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignment(Assignment asg, Modifiers mod) {
      m_Twiddles = new ArrayList<Twiddle>();
      for (int i = 0; i < asg.getTwiddleCount(); ++i) {
         m_Twiddles.add(new Twiddle(asg.getTwiddle(i), mod));
      }
      m_KeyPressList = asg.getKeyPressList().createModified(mod);
   }

   ////////////////////////////////////////////////////////////////////////////
   public int getTwiddleCount() { return m_Twiddles.size(); }
   public Twiddle getTwiddle(int i) { return m_Twiddles.get(i); }
   public Twiddle getTwiddle() { return getTwiddle(0); }
   public KeyPressList getKeyPressList() { return m_KeyPressList; }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isMap(Twiddle tw) {
      for (int i = 0; i < m_Twiddles.size(); ++i) {
         if (tw.equals(m_Twiddles.get(i))) {
            return true;
         }
      }
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isUse(Chord ch) {
      for (int i = 0; i < m_Twiddles.size(); ++i) {
         if (ch.equals(m_Twiddles.get(i).getChord())) {
            return true;
         }
      }
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   public ArrayList<Assignment> separate() {
      ArrayList<Assignment> asgs = new ArrayList<Assignment>();
      for (int i = 0; i < m_Twiddles.size(); ++i) {
         asgs.add(new Assignment(m_Twiddles.get(i), m_KeyPressList));
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isThumbed() {
      for (int i = 0; i < m_Twiddles.size(); ++i) {
         if (!m_Twiddles.get(i).getThumbKeys().isEmpty()) {
            return true;
         }
      }
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      return toString(KeyPress.Format.FILE, false, "\n");
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(KeyPress.Format format, boolean shortStr, String separator) {
      if (shortStr && isThumbed()) {
         shortStr = false;
      }
      String keys = " = " + m_KeyPressList.toString(format);
      String sep = "";
      String twiddles = "";
      for (int i = 0; i < m_Twiddles.size(); ++i) {
         twiddles += sep
                   + (shortStr
                     ? m_Twiddles.get(i).toShortString()
                     : m_Twiddles.get(i).toString())
                   + keys;
         sep = separator;
      }
      return twiddles;
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private Assignment(Assignment a1, Assignment a2) {
      if (!a1.m_KeyPressList.equals(a2.m_KeyPressList)) {
         Log.err("Trying to merge assignments with different keys");
      }
      m_Twiddles = new ArrayList<Twiddle>(a1.m_Twiddles);
      // define order
      m_Twiddles.addAll(a2.m_Twiddles);
      m_KeyPressList = a1.m_KeyPressList;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private ArrayList<Twiddle> m_Twiddles;
   private KeyPressList m_KeyPressList;
}
