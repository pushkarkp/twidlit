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
   public static final int sm_VALUES = 0xFF;
   public static final int sm_VALUES_WITH_MOUSE = 0x3FF;
   public static final int sm_ROWS = 4;
   public static final int sm_COLUMNS = 3;
   public static final int sm_MOUSE = 4;

   /////////////////////////////////////////////////////////////////////////////
   public static void use4Finger(boolean set) {
      sm_4Finger = set;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static boolean isUsing4Finger() {
      return sm_4Finger;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static Chord createMouseButton(String str) {
      Chord c = new Chord(fromString(str));
      if (!c.isMouseButton()) {
         return new Chord(0);
      }   
      return c;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static String getMouseButtonName(int chord) {
      switch (getFingerButton(sm_MOUSE, chord)) {
         case 1: return "right";
         case 2: return "middle";
         case 3: return "left";
         default: return String.format("0x%4x is not a", chord);
      }
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
   // returns button's 2 bits in place
   // index == least significant 2 bits == [0]
   // pinky == most significant 2 bits == [3]
   public static int getButtonAtFinger(int finger, int chord) {
      return chord & (3 << (3 - finger) * 2);
   }

   /////////////////////////////////////////////////////////////////////////////
   // returns button in the 2 LSBs
   public static int getFingerButton(int finger, int chord) {
      return (chord >> finger * 2) & 3;
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
   public static int getFinger(String order, int finger) {
      if ("".equals(order) || "1234".equals(order)) {
         return finger;
      }
      return order.charAt(finger) - '1';
   }

   /////////////////////////////////////////////////////////////////////////////
   public int toCfg() {
      if (isMouseButton()) {
         return 0;
      }
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
      if (str.length() < 4) {
         return 0;
      }
      if (str.length() > 4 && str.charAt(4) != ' ') {
         if ((str.length() == 5 || str.charAt(5) == ' ')
          && "||||".equals(str.substring(1, 5))) {
            return charToButton(str.charAt(0)) << 8;
         }
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
      if (value < 0 
       || (value > sm_VALUES
        && (value & ~(3 << sm_MOUSE * 2)) != 0)) {
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
//System.out.printf("%d==%d%n", m_Value, other.m_Value);

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

   ////////////////////////////////////////////////////////////////////////////
   // -1 <, 0 ==, 1 >
   public int compare(Chord other) {
      int f = getFingerCount();
      int of = other.getFingerCount();
      if (f < of) {
         return -1;
      }
      if (f > of) {
         return 1;
      }
      int i = toInt();
      int oi = other.toInt();
      if (i < oi) {
         return -1;
      }
      if (i > oi) {
         return 1;
      }
      return 0;
  }

   /////////////////////////////////////////////////////////////////////////////
   public boolean isValid() { return isChord() || isMouseButton(); }
   public boolean isChord() { return m_Value > 0 && m_Value <= sm_VALUES; }
   public boolean isMouseButton() { return (m_Value & 0x300) != 0 && (m_Value & ~0x300) == 0; }
   public int getMouseButton() { return getFingerButton(sm_MOUSE, m_Value); }
   public String getMouseButtonName() { return getMouseButtonName(m_Value); }
   public int toInt() { return m_Value; }
   public int getFingerCount() { return countFingers(m_Value); }
   public int getRowKey(int row) { return (m_Value >> row * 2) & 3; }
   public static int getKeys() { return m_Keys; }
   public static int getDepth() { return m_Depth; }
   public static int getButtonSpace() { return m_ButtonSpace; }
   public static int getFingerSpace() { return m_FingerSpace; }
   public static boolean isEccentric() { return m_Eccentric; }

   /////////////////////////////////////////////////////////////////////////////
   public String toString() {
      if (isMouseButton()) {
         return "" + buttonToChar(getFingerButton(4, m_Value)) + "||||";
      }
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
