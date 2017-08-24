/**
 * Copyright 2015 Pushkar Piggott
 *
 * Assignment.java
 */

package pkp.twiddle;

import java.util.List;
import java.util.ArrayList;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
// An assignment maps one OR MORE chords to a keypress list.
public class Assignment extends java.lang.Object implements Comparable<Assignment> {

   ////////////////////////////////////////////////////////////////////////////
   public static boolean sm_SHOW_THUMB_KEYS = true;
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
   public Assignment(List<Twiddle> tw, KeyPressList kpl) {
      m_Twiddles = new ArrayList<Twiddle>(tw);
      m_KeyPressList = kpl;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignment(Assignment asg, Modifiers mod) {
      m_Twiddles = new ArrayList<Twiddle>();
      for (Twiddle tw : asg.m_Twiddles) {
          m_Twiddles.add(new Twiddle(tw, mod));
      }
      m_KeyPressList = asg.getKeyPressList().createModified(mod);
   }

   /////////////////////////////////////////////////////////////////////////////
   public Assignment reversed() {
      // mouse buttons cannot be assigned
      if (isDefaultMouse()) {
         return this;
      }
      List<Twiddle> tws = new ArrayList<Twiddle>();
      for (Twiddle tw : m_Twiddles) {
         tws.add(tw.reversed());
      }         
      return new Assignment(tws, getKeyPressList());
   }

   ////////////////////////////////////////////////////////////////////////////
   public int getTwiddleCount() { return m_Twiddles.size(); }
   public Twiddle getTwiddle(int i) { return m_Twiddles.get(i); }
   public KeyPressList getKeyPressList() { return m_KeyPressList; }

   ////////////////////////////////////////////////////////////////////////////
   // Comparable
   @Override
   public int compareTo(Assignment a) {
      return Integer.compare(getTwiddle(0).toCfg(), a.getTwiddle(0).toCfg());
   }

   ////////////////////////////////////////////////////////////////////////////
   public Twiddle getBestTwiddle() {
      if (m_Twiddles.size() == 1) {
         return m_Twiddles.get(0);
      }
      Twiddle best = m_Twiddles.get(0);
      for (Twiddle tw : m_Twiddles) {
         if (tw.lessThan(best)) {
            best = tw;
         }
      }
      return best;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isMap(Twiddle twiddle) {
      for (Twiddle tw : m_Twiddles) {
         if (twiddle.equals(tw)) {
            return true;
         }
      }
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   public List<Assignment> separate() {
      List<Assignment> asgs = new ArrayList<Assignment>();
      for (Twiddle tw : m_Twiddles) {
         asgs.add(new Assignment(tw, m_KeyPressList));
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isDefaultMouse() {
      return m_Twiddles.size() == 1 
          && m_Twiddles.get(0).getChord().isMouseButton()
          && m_Twiddles.get(0).getThumbKeys().isEmpty()
          && m_KeyPressList.size() == 1
          && m_KeyPressList.get(0).equals( 
             KeyPress.fromMouseButton(m_Twiddles.get(0).getChord().getMouseButton()));
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isThumbed() {
      for (Twiddle tw : m_Twiddles) {
         if (!tw.getThumbKeys().isEmpty()) {
            return true;
         }
      }
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      return toString(sm_SHOW_THUMB_KEYS, KeyPress.Format.FILE, "\n");
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(boolean showThumbs, KeyPress.Format format, String separator) {
      if (!showThumbs && isThumbed()) {
         showThumbs = true;
      }
      String keys = " = " + m_KeyPressList.toString(format);
      String sep = "";
      String twiddles = "";
      for (Twiddle tw : m_Twiddles) {
         twiddles += sep
                   + (showThumbs
                     ? tw.toString()
                     : tw.toShortString())
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
   private List<Twiddle> m_Twiddles;
   private KeyPressList m_KeyPressList;
}
