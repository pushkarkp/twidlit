/**
 * Copyright 2015 Pushkar Piggott
 *
 * Chord.java
 */
 
package pkp.twiddle;

import pkp.io.Io;

////////////////////////////////////////////////////////////////////////////////
public class Chord {

   /////////////////////////////////////////////////////////////////////////////
   public static final int sm_VALUES = 255;
   public static final int sm_ROWS = 4;
   public static final int sm_COLUMNS = 3;

   /////////////////////////////////////////////////////////////////////////////
   public static void use4Finger(boolean set) {
      sm_4Finger = set;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static boolean isUsing4Finger() {
      return sm_4Finger;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static char buttonToChar(int button) {
      if (isUsing4Finger()) {
         switch (button & 3) {
         case 0: return '|';
         case 1: return ',';
         case 2: return '-';
         case 3: return '\'';
         }
      } else {
         switch (button & 3) {
         case 0: return '0';
         case 1: return 'R';
         case 2: return 'M';
         case 3: return 'L';
         }
      }
      return '?';
   }

   /////////////////////////////////////////////////////////////////////////////
   public static int getFingerButton(int finger, int chord) {
      return (chord >> finger * 2) & 3;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static int reverseButtons(int chord) {
      int backwards = 0;
      for (int i = 0; i < 4; ++i) {
         backwards <<= 2;
         backwards |= (chord & 3);
         chord >>= 2;
      }
      return backwards;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static int orderFingers(String order, int chord) {
      if ("".equals(order) || "1234".equals(order)) {
         return chord;
      }
      int sorted = 0;
      for (int i = 0; i < 4; ++i) {
         // convert 1234 to 3210
         int f = 4 - (order.charAt(i) - '0');
         sorted <<= 2;
         sorted |= getFingerButton(f, chord);
      }
      return sorted;
   }

   /////////////////////////////////////////////////////////////////////////////
   public int toCfg() {
      int cfg = 0;
      int chord = m_Value;
      for (int i = 0; i < 4; ++i) {
         cfg <<= 4;
         switch (chord & 3) {
         case 1:
            cfg |= 2;
            break;
         case 2:
            cfg |= 4;
            break;
         case 3:
            cfg |= 8;
            break;
         }
         chord >>= 2;
      }
      // swap nibbles
      return (cfg & 0xF0F0) >> 4 | (cfg & 0x0F0F) << 4;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static int fromString(String str) {
      str = str.substring(Io.findFirstNotOf(str, Io.sm_WS));
      if (str.length() < 4 || (str.length() > 4 && str.charAt(4) != ' ')) {
         return 0;
      }
      int value = 0;
      for (int finger = 0; finger < 4; ++finger) {
         value <<= 2;
         int b = charToButton(str.charAt(3 - finger));
         if (b == -1) {
            return 0;
         }
         value += b;
      }
      return value;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static int charToButton(char c) {
      switch (c) {
      case '|':
      case '0':
         return 0;
      case ',':
      case 'R':
      case 'r':
         return 1;
      case '-':
      case 'M':
      case 'm':
         return 2;
      case '\'':
      case 'L':
      case 'l':
         return 3;
      default:
         return 0;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   public static int countFingers(int chord) {
      int fingers = 0;
      for (int finger = 0; finger < 4; ++finger) {
         if (getFingerButton(finger, chord) != 0) {
            ++fingers;
         }
      }
      return fingers;
   }

   /////////////////////////////////////////////////////////////////////////////
   public Chord(String str) {
      m_Value = fromString(str);
   }

   /////////////////////////////////////////////////////////////////////////////
   public Chord(int value) {
      if (value < 0 || value > sm_VALUES) {
         m_Value = 0;
      } else {
         m_Value = value;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   public Chord(byte value) {
      m_Value = value & sm_VALUES;
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean equals(Object obj) {
      Chord other = (Chord)obj;
      return other != null && other.m_Value == m_Value;
   }

   /////////////////////////////////////////////////////////////////////////////
   public Chord reversed() {
      int ch = toInt();
      for (int i = 0; i < 4; ++i) {
         ch <<= 2;
         switch (ch & 0x300) {
         case 0x300: ch &= ~0x200; break;
         case 0x100: ch |= 0x200; break;
         }
      }
      return new Chord(ch >> 8);
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean isValid() { return m_Value != 0; }
   public int toInt() { return m_Value; }
   public int getRowKey(int row) { return (m_Value >> row * 2) & 3; }
   public static int getKeys() { return m_Keys; }
   public static int getDepth() { return m_Depth; }
   public static int getButtonSpace() { return m_ButtonSpace; }
   public static int getFingerSpace() { return m_FingerSpace; }
   public static boolean isEccentric() { return m_Eccentric; }

   /////////////////////////////////////////////////////////////////////////////
   public String toString() {
      String str = "";
      for (int finger = 0; finger < 4; ++finger) {
         str += buttonToChar(getFingerButton(finger, m_Value));
      }
      return str;
   }

   // Private //////////////////////////////////////////////////////////////////
   private static void calculateImpediments(Chord chord) {
      m_Keys = 0;
      m_Depth = 0;
      m_Eccentric = false;
      m_FingerSpace = 0;
      int lastFinger = -1;
      int min = 3;
      int max = 0;
System.out.printf("value %d\n", chord.toInt());      
      for (int finger = 0; finger < 4; ++finger) {
         int pressed = (chord.toInt() >> finger * 2) & 3;
         if (pressed != 0) {
            ++m_Keys;
            m_Depth = finger + 1;
            min = Math.min(min, pressed);
            max = Math.max(max, pressed);
            if (lastFinger > -1) {
               m_FingerSpace = finger - lastFinger - 1;
            }
            lastFinger = finger;
            if ((pressed & 1) != 0) {
               m_Eccentric = true;
            }
         }
      }
      if (m_Keys > 0) {
         m_ButtonSpace = max - min;
      }
   }

   // Data /////////////////////////////////////////////////////////////////////
   private static boolean sm_4Finger = true;
   private int m_Value;
   private static int m_Keys;
   private static int m_Depth;
   private static int m_ButtonSpace;
   private static int m_FingerSpace;
   private static boolean m_Eccentric;

   // Main /////////////////////////////////////////////////////////////////////
   public static void main (String[] args) {
      for (String arg: args) {
         Chord chord = new Chord(arg);
         /*
         for (int i = 0; i < 4; ++i) {
            System.out.printf("%d", chord.getRowKey(i));
         }
         System.out.println("");
         */
         calculateImpediments(chord);
         System.out.printf("%s %3d: %d Keys, Depth %d, Button space %d, Finger space %d, %sEccentric\n",
                           chord.toString(),
                           chord.toInt(), getKeys(), getDepth(),
                           getButtonSpace(), getFingerSpace(),
                           (isEccentric() ? "" : "Not "));
      }
   }
}
