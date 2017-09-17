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
   public static final int sm_FINGERS = 4;
   public static final int sm_BUTTONS = 3;

   ////////////////////////////////////////////////////////////////////////////
   public enum Format {
      DISPLAY("For display (user set)"),
      FILE("For files (user set)"),
      STD("Escaped text with named white space"),
      TAG("Tags"),
      ESC("Escaped"),
      HEX("Hexadecimal"),
      TXT("Text");

      public String toString() {
         return m_Description;
      }

      private Format(String description) {
         m_Description = description;
      }
      private final String m_Description;
   }

   ////////////////////////////////////////////////////////////////////////////
   public enum Button {
      O("|", 0),
      L("'", 1),
      M("-", 2),
      R(",", 3);

      public String toString() {
         return m_Name;
      }

      public int toInt() {
         return m_I;
      }

      private Button(String name, int i) {
         m_Name = name;
         m_I = i;
      }
      private final String m_Name;
      private final int m_I;
   }

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
         switch (button & sm_BUTTONS) {
         case 0: return '|';
         case 1: return ',';
         case 2: return '-';
         case 3: return '\'';
         }
      } else {
         switch (button & sm_BUTTONS) {
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
      return chord & (sm_BUTTONS << (3 - finger) * 2);
   }

   /////////////////////////////////////////////////////////////////////////////
   // returns button in the 2 LSBs
   public static int getFingerButton(int finger, int chord) {
      return (chord >> finger * 2) & sm_BUTTONS;
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
   public static int reverse(int ch) {
      for (int i = 0; i < 5; ++i) {
         if ((ch & 1 << i * 2) != 0) {
            ch ^= 1 << i * 2 + 1;
         }
      }
      return ch;
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
         switch (chord & 0xc0) {
         case 0x40:
            cfg |= 2;
            break;
         case 0x80:
            cfg |= 4;
            break;
         case 0xc0:
            cfg |= 8;
            break;
         }
         chord <<= 2;
      }
      return cfg;
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
   public static Chord fromChordValue(int cv) {
      return new Chord(cv & sm_VALUES);
   }

   /////////////////////////////////////////////////////////////////////////////
   public static Chord fromMouseButton(int mb) {
      return (mb < 1 || mb > sm_BUTTONS)
              ? new Chord(0)
              : new Chord((sm_BUTTONS + 1 - mb) << 8);
   }

   /////////////////////////////////////////////////////////////////////////////
   public Chord(String str) {
      m_Value = fromString(str);
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean equals(Object obj) {
      Chord other = (Chord)obj;
//System.out.printf("%d==%d%n", m_Value, other.m_Value);

      return other != null && other.m_Value == m_Value;
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean contains(int finger, Button b) {
      return getFingerButton(finger, toInt()) == b.toInt();
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean none(Button b) {
      for (int f = 0; f < sm_FINGERS; ++f) {
         if (contains(f, b)) {
            return false;
         }
      }
      return true;
   }

   /////////////////////////////////////////////////////////////////////////////
   public Chord reversed() {
      return new Chord(reverse(toInt()));
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
   public int getMouseButton() { 
      int b = getFingerButton(sm_MOUSE_FINGER, m_Value);
      return b == 2
             ? b
             : b ^ 2;
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean isValid() { return isChord() || isMouseButton(); }
   public boolean isChord() { return m_Value > 0 && m_Value <= sm_VALUES; }
   public boolean isMouseButton() { return (m_Value & 0x300) != 0 && (m_Value & ~0x300) == 0; }
   public int toInt() { return m_Value; }
   public int getFingerCount() { return countFingers(m_Value); }
   public int getFingerKey(int f) { return (m_Value >> f * 2) & 3; }
   public static int getKeys() { return m_Keys; }
   public static int getDepth() { return m_Depth; }
   public static int getButtonSpace() { return m_ButtonSpace; }
   public static int getFingerSpace() { return m_FingerSpace; }
   public static boolean isEccentric() { return m_Eccentric; }

   /////////////////////////////////////////////////////////////////////////////
   public String toString() {
      if (isMouseButton()) {
         return "" + buttonToChar(getFingerButton(sm_MOUSE_FINGER, m_Value)) + "||||";
      }
      String str = "";
      for (int finger = 0; finger < 4; ++finger) {
         str += buttonToChar(getFingerButton(finger, m_Value));
      }
      return str;
   }

   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private Chord(int value) {
      if (value < 0 
       || (value > sm_VALUES
        && (value & ~(3 << sm_MOUSE_FINGER * 2)) != 0)) {
         m_Value = 0;
      } else {
         m_Value = value;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
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
   private static final int sm_MOUSE_FINGER = 4;
   private static boolean sm_4Finger = true;
   private static int m_Keys;
   private static int m_Depth;
   private static int m_ButtonSpace;
   private static int m_FingerSpace;
   private static boolean m_Eccentric;
   private int m_Value;

   // Main /////////////////////////////////////////////////////////////////////
   public static void main (String[] args) {
      for (String arg: args) {
         Chord c = new Chord(arg);
         System.out.printf("New %s 0x%04x%n", c, Io.otherEndian((short)c.toCfg()));
         System.out.printf("New %s 0x%04x%n", c, c.toCfg());
//         calculateImpediments(chord);
//         System.out.printf("%s %3d: %d Keys, Depth %d, Button space %d, Finger space %d, %sEccentric\n",
//                           chord.toString(),
//                           chord.toInt(), getKeys(), getDepth(),
//                           getButtonSpace(), getFingerSpace(),
//                           (isEccentric() ? "" : "Not "));
      }
   }
}
