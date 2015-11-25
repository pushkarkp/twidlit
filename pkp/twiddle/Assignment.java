/**
 * Copyright 2015 Pushkar Piggott
 *
 * Assignment.java
 */

package pkp.twiddle;

import java.util.ArrayList;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class Assignment extends java.lang.Object {

   ////////////////////////////////////////////////////////////////////////////
   public static String toString(ArrayList<Assignment> asgs, KeyPress.Format format) {
      String str = "";
      for (Assignment asg: asgs) {
			str += asg.toString(format) + '\n';
		}
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Assignment combine(Assignment a1, Assignment a2) {
      return new Assignment(a1, a2);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Assignment parseLine(String str) {
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
                        KeyPressList.parseTextAndTags(str));
      return asg;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static ArrayList<Assignment> listAll() {
      ArrayList<Assignment> asgs = new ArrayList<Assignment>();
      for (int i = 1; i <= Chord.sm_VALUES; ++i) {
         Twiddle tw = new Twiddle(i, 0);
         asgs.add(new Assignment(tw, KeyPressList.parseText(tw.getChord().toString() + " ")));
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static ArrayList<Assignment> listAllNamedByFingerCount() {
      ArrayList<Assignment> asgs = new ArrayList<Assignment>();
      for (int fingers = 1; fingers <= 4; ++fingers) {
         for (int i = 1; i <= Chord.sm_VALUES; ++i) {
            if (Chord.countFingers(i) == fingers) {
               Twiddle tw = new Twiddle(i, 0);
               asgs.add(new Assignment(tw, KeyPressList.parseText(tw.getChord().toString() + " ")));
            }
         }
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static ArrayList<Assignment> listAllByFingerCount() {
      final int MIN_CODE = 4;
      final int MAX_CODE = 0x52;
      final int MODE_INC = 0x200;
      int code = MIN_CODE;
      int mod = 0;
      Modifiers modifiers = Modifiers.fromKeyCode(mod);
      ArrayList<Assignment> asgs = new ArrayList<Assignment>();
      for (int fingers = 1; fingers <= 4; ++fingers) {
         for (int i = 1; i <= Chord.sm_VALUES; ++i) {
            if (Chord.countFingers(i) == fingers) {
               Twiddle tw = new Twiddle(i, 0);
               KeyPress kp;
               do {
                  kp = new KeyPress(code, modifiers);
                  ++code;
                  // omit keys with keyboard state etc
                  while (code == 0x39 // CapsLock
                      || code == 0x49 // Insert
                      || code == 0x53 // NumLock
                      || code == 0x32 // IntlHash
                      || code == 0x46) { // PrintScreen
                     ++code;
                  }
                  if (code > MAX_CODE) {
                     code = MIN_CODE;
                     mod += MODE_INC;
                     modifiers = Modifiers.fromKeyCode(mod);
                  }
               } while (!kp.isValid() || kp.isDuplicate() || kp.isLost());
               asgs.add(new Assignment(tw, new KeyPressList(kp)));
            }
         }
      }
      return asgs;
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
   public Assignment(Assignment a1, Assignment a2) {
      if (!a1.hasSameKeys(a2)) {
         Log.err("Trying to merge assignments with different keys");
      }
      m_Twiddles = new ArrayList<Twiddle>(a1.m_Twiddles);
      // define order
      m_Twiddles.addAll(a1.m_Twiddles);
      m_KeyPressList = a1.m_KeyPressList;
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
   public boolean hasSameKeys(Assignment asg) {
//System.out.printf("Assignment.hasSameKeys: %s == %s%n", m_KeyPressList.toString(), asg.m_KeyPressList.toString());
      return asg.m_KeyPressList.equals(m_KeyPressList);
   }

   ////////////////////////////////////////////////////////////////////////////
   public int getTwiddleCount() { return m_Twiddles.size(); }
   public Twiddle getTwiddle(int i) { return m_Twiddles.get(i); }
   public KeyPressList getKeyPressList() { return m_KeyPressList; }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      return toString(KeyPress.Format.CFG);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(KeyPress.Format format) {
      String keys = " = " + m_KeyPressList.toString(format);
      String twiddles = "";
      for (int i = 0; i < m_Twiddles.size(); ++i) {
         twiddles += m_Twiddles.get(i).toString() + keys;
      }
      return twiddles;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private ArrayList<Twiddle> m_Twiddles;
   private KeyPressList m_KeyPressList;
}
