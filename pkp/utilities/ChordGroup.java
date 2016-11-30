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

import pkp.twiddle.Chord;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
public class ChordGroup {

   /////////////////////////////////////////////////////////////////////////////
   public static final int sm_MaskShift = 4;
   public static final int sm_Maskable[] = {0x1, 0x2, 0x4, 0x8};

   /////////////////////////////////////////////////////////////////////////////
   // index == least significant nibble == [0]
   // pinky == most significant nibble == [3]
   public static int getMaskAtFinger(int finger, int mask) {
      return mask & (15 << (3 - finger) * sm_MaskShift);
   }

    /////////////////////////////////////////////////////////////////////////////
   public static int getMaskFinger(int finger, int mask) {
      return (mask >> finger * sm_MaskShift) & 15;
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
   public ChordGroup(ChordText chordText, boolean free, boolean showText, int mask) {
      m_ChordText = chordText;
      m_GroupFree = free;
      m_ShowText = !free && showText;
      m_Mask = mask;
      m_Count = 0;
   }

   /////////////////////////////////////////////////////////////////////////////
   public int getSize() {
      return m_Count;
   }

   /////////////////////////////////////////////////////////////////////////////
   public String maskToString() {
      return maskToString(m_Mask);
   }

   /////////////////////////////////////////////////////////////////////////////
   public int eligibleCount() {
      int count = 0;
      for (int c = 1; c <= Chord.sm_VALUES; ++c) {
         if (m_GroupFree == (m_ChordText.get(c) == null)) {
            ++count;
         }
      }
      return count;
   }

   /////////////////////////////////////////////////////////////////////////////
   public String groupToString(String priority) {
      String str = "";
      final int maskableChords[] = getMaskableChords();
      for (int c = 1; c <= Chord.sm_VALUES; ++c) {
         int chord = Chord.orderFingers(priority, c);
         if ((maskableChords[chord] & m_Mask) == maskableChords[chord]) {
            String line = m_ChordText.get(chord);
            if (m_GroupFree == (line == null)) {
               ++m_Count;
               if (!m_ShowText) {
                  str += " " + new Chord(chord);
               } else {
                  str += "\n   " + line;
               }
            }
         }
      }
      return str + '\n';
   }

   /////////////////////////////////////////////////////////////////////////////
   public String toString(String priority) {
      String str = groupToString(priority);
      return maskToString() + String.format(" %3d:", getSize()) + str;
   }

   /////////////////////////////////////////////////////////////////////////////
   public String toString() {
      return toString("");
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
         maskable <<= sm_MaskShift;
         maskable |= sm_Maskable[Chord.getFingerButton(finger, chord)];
      }
      return maskable;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static int sm_MaskableChords[];

   private final ChordText m_ChordText;
   private final boolean m_GroupFree;
   private final boolean m_ShowText;
   private final int m_Mask;
   private boolean[] m_Chord;
   private String m_String;
   private int m_Count;
}