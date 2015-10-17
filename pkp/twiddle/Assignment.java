/**
 * Copyright 2015 Pushkar Piggott
 *
 * Assignment.java
 */

package pkp.twiddle;

import java.util.ArrayList;

///////////////////////////////////////////////////////////////////////////////
public class Assignment extends java.lang.Object {

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
      KeyPressList kpl = KeyPressList.parseTextAndTags(str);
      Assignment asg = new Assignment(twiddle, kpl);
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
         for (int i = 1; i <= 255; ++i) {
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
      final int MAX_CODE = 0x64;
      final int MODE_INC = 0x2200;
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
                  if (code > MAX_CODE) {
                     code = MIN_CODE;
                     mod += MODE_INC;
                     modifiers = Modifiers.fromKeyCode(mod);
                  }
               } while (!kp.isValid());
               asgs.add(new Assignment(tw, new KeyPressList(kp)));
            }
         }
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignment(Twiddle tw, KeyPressList kpl) {
      m_Twiddle = tw;
      m_KeyPressList = kpl;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignment(Assignment asg, Modifiers mod) {
      m_Twiddle = new Twiddle(asg.getTwiddle(), mod);
      m_KeyPressList = asg.getKeyPressList().createModified(mod);
   }

   ////////////////////////////////////////////////////////////////////////////
   public Twiddle getTwiddle() { return m_Twiddle; }
   public KeyPressList getKeyPressList() { return m_KeyPressList; }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      return toString(KeyPress.Format.TEXT);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(KeyPress.Format format) {
      return m_Twiddle.toString() + " = " + m_KeyPressList.toString(format);
   }

   // Data ////////////////////////////////////////////////////////////////////
   private Twiddle m_Twiddle;
   private KeyPressList m_KeyPressList;
}
