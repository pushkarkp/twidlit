/**
 * Copyright 2016 Pushkar Piggott
 *
 * ChordGroup.java
 *
 * A chord is 4 x 2bit values.
 * Each value is 0-3, (a button press, | to ').
 * A mask is 4 x 4bit values.
 * Each value is 0-F, (any combination of button presses).
 */

package pkp.utilities;

import java.util.ArrayList;
import pkp.twiddle.Assignments;
import pkp.twiddle.Assignment;
import pkp.twiddle.Twiddle;
import pkp.twiddle.KeyPress;
import pkp.twiddle.Chord;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
public class ChordGroup {

   /////////////////////////////////////////////////////////////////////////////
   public static final int sm_Maskable[] = {
      0x1, 0x2, 0x4, 0x8};

   /////////////////////////////////////////////////////////////////////////////
   public static int getMaskFinger(int finger, int mask) {
      return (mask >> finger * 4) & 15;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static String maskToString(int mask) {
      String str = "";
      for (int finger = 0; finger < 4; ++finger) {
         str += maskFingerToChar(getMaskFinger(3 - finger, mask));
      }
      return str;
   }

   /////////////////////////////////////////////////////////////////////////////
   public ChordGroup(int mask, Assignments asgs, boolean free, boolean showMapping) {
      m_Mask = mask;
      if (!free && showMapping) {
         m_Assignments = asgs;
      }
      m_Chord = new boolean[Chord.sm_VALUES + 1];
      m_Size = 0;
      m_Selected = new int[Chord.sm_VALUES + 1];
      final int maskableChords[] = getMaskableChords();
      for (int c = 1; c <= Chord.sm_VALUES; ++c) {
         if ((maskableChords[c] & mask) == maskableChords[c]) {
            int found = asgs.find(new Twiddle(c, 0));
            if (free == (found == -1)) {
               m_Chord[c] = true;
               m_Selected[m_Size] = found;
               ++m_Size;
            }
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   public int getSize() {
      return m_Size;
   }

   /////////////////////////////////////////////////////////////////////////////
   public String maskToString() {
      return maskToString(m_Mask);
   }

   /////////////////////////////////////////////////////////////////////////////
   public String groupToString() {
      String str = "";
      if (m_Assignments == null) {
         for (int i = 1; i <= Chord.sm_VALUES; ++i) {
            if (m_Chord[i]) {
               str += new Chord(i) + " ";
            }
         }
      } else {
         str += "\n   ";
         for (int i = 0; i < m_Size; ++i) {
            str += m_Assignments.get(m_Selected[i]).toString(!Assignment.sm_SHOW_THUMB_KEYS, KeyPress.Format.FILE, "") + "\n   ";
         }
      }
      return str;
   }

   /////////////////////////////////////////////////////////////////////////////
   public String toString() {
      return maskToString() + String.format(" %3d: ", getSize()) + groupToString();
   }

   // Private /////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private static char maskFingerToChar(int m) {
      switch (m) {
      case 0:   return '.';
      case 1: 
      case 2:
      case 4:   return Chord.buttonToChar(m >> 1);
      case 8:   return Chord.buttonToChar(3);
      case 0xA: return 'a';
      case 0xB: return 'b';
      case 0xC: return 'c';
      case 0xD: return 'd';
      case 0xE: return 'e';
      case 0xF: return '?';
      }
      if (1 <= m && m <= 9) {
         return (char)(m + '0');
      }
      Log.err(String.format("Not a mask finger 0x%x", m));
      return ' ';
   }

   ////////////////////////////////////////////////////////////////////////////
   private static int[] getMaskableChords() {
      if (sm_MaskableChords == null) {
         sm_MaskableChords = new int[Chord.sm_VALUES + 1];
         for (int i = 0; i <= Chord.sm_VALUES; ++i) {
            sm_MaskableChords[i] = chordToMaskable(i);
         }
      }
      return sm_MaskableChords;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static int chordToMaskable(int chord) {
      int maskable = 0;
      for (int finger = 0; finger < 4; ++finger) {
         maskable <<= 4;
         maskable |= sm_Maskable[Chord.getFingerButton(finger, chord)];
      }
      return maskable;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static int sm_MaskableChords[];

   private int m_Mask;
   private Assignments m_Assignments;
   private boolean[] m_Chord;
   private int[] m_Selected;
   private int m_Size;
}