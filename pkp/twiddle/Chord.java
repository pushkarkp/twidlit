/**
 * Copyright 2015 Pushkar Piggott
 *
 * Chord.java
 */
 
 package pkp.twiddle;

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
      int i = 0;
      while (i < str.length() && str.charAt(i) == ' ') {
         ++i;
      }
      str = str.substring(i);
      if (str.length() < 4 || (str.length() > 4 && str.charAt(4) != ' ')) {
         return 0;
      }
      int value = 0;
      for (int finger = 0; finger < 4; ++finger) {
         value <<= 2;
         switch (str.charAt(3 - finger)) {
         case '|':
         case '0':
            value += 0;
            break;
         case ',':
         case 'R':
         case 'r':
            value += 1;
            break;
         case '-':
         case 'M':
         case 'm':
            value += 2;
            break;
         case '\'':
         case 'L':
         case 'l':
            value += 3;
            break;
         default:
            return 0;
         }
      }
      return value;
   }

   /////////////////////////////////////////////////////////////////////////////
   public Chord(String str) {
      m_Value = fromString(str);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Chord buttons go |0 -1 ,2 '3
   public Chord(int value) {
      if (value < 0 || value > 255) {
         m_Value = 0;
      } else {
         m_Value = value;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   public boolean equals(Object o) {
      Chord other = (Chord)o;
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
      if (sm_4Finger) {
         return to4Finger();
      } else {
         return to0MRL();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   String to4Finger() {
      String str = "";
      for (int finger = 0; finger < 4; ++finger) {
         switch ((m_Value >> finger * 2) & 3) {
         case 0:
            str += '|';
            break;
         case 1:
            str += ',';
            break;
         case 2:
            str += '-';
            break;
         case 3:
            str += '\'';
            break;
         }
      }
      return str;
   }

   /////////////////////////////////////////////////////////////////////////////
   String to0MRL() {
      String str = "";
      for (int finger = 0; finger < 4; ++finger) {
         switch ((m_Value >> finger * 2) & 3) {
         case 0:
            str += '0';
            break;
         case 1:
            str += 'R';
            break;
         case 2:
            str += 'M';
            break;
         case 3:
            str += 'L';
            break;
         }
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
         System.out.printf("%s %s %3d: %d Keys, Depth %d, Button space %d, Finger space %d, %sEccentric\n",
                           chord.toString(), chord.to0MRL(),
                           chord.toInt(), getKeys(), getDepth(),
                           getButtonSpace(), getFingerSpace(),
                           (isEccentric() ? "" : "Not "));
      }
   }
}
