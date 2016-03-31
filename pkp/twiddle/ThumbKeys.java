/**
 * Copyright 2015 Pushkar Piggott
 *
 * ThumbKeys.java
 */

package pkp.twiddle;

///////////////////////////////////////////////////////////////////////////////
/// The invalid state is -1 and is written out as 0000.
public class ThumbKeys extends java.lang.Object {

   /////////////////////////////////////////////////////////////////////////////
   public static final int sm_VALUES = 15;

   /////////////////////////////////////////////////////////////////////////////
   public static int fromModifiers(Modifiers mod) {
      int value = 0;
      if (mod.isShift()) {
         value |= sm_SHIFT;
      }
      if (mod.isCtrl()) {
         value |= sm_CTRL;
      }
      if (mod.isAlt()) {
         value |= sm_ALT;
      }
      return value;
   }

   /////////////////////////////////////////////////////////////////////////////
   public Modifiers toModifiers() {
      return toModifiers(m_Value);
   }

   /////////////////////////////////////////////////////////////////////////////
   public Modifiers toRemainingModifiers(int keys) {
      return toModifiers(m_Value & ~keys);
   }

   /////////////////////////////////////////////////////////////////////////////
   public ThumbKeys minus(Modifiers mod) {
      return new ThumbKeys(m_Value & ~fromModifiers(mod));
   }

   /////////////////////////////////////////////////////////////////////////////
   public ThumbKeys plus(Modifiers mod) {
      return new ThumbKeys(m_Value | fromModifiers(mod));
   }

   /////////////////////////////////////////////////////////////////////////////
   public int toIntNoModifiers() {
      return m_Value & ~sm_SHIFT & ~sm_CTRL & ~sm_ALT;
   }

   ////////////////////////////////////////////////////////////////////////////
   public ThumbKeys(String str) {
      int i = 0;
      while (i < str.length() && str.charAt(i) == ' ') {
         ++i;
      }
      boolean twidorStyle = false;
      int read = 0;
      for (; i < str.length() && str.charAt(i) != ' '; ++i) {
         switch (str.charAt(i)) {
         case '0': case 'O': case 'o':
            break;
         case 'N': case 'n':
            m_Value |= sm_NUM;
            break;
         case 'A': case 'a':
            m_Value |= sm_ALT;
            break;
         case 'C': case 'c':
            m_Value |= sm_CTRL;
            break;
         case 'S': case 's':
            m_Value |= sm_SHIFT;
            break;
         case 'U': case 'u':
         case 'H': case 'h':
         case 'T': case 't':
         case 'L': case 'l':
            if (!twidorStyle && read == 0) {
               m_Value = -1;
               return;
            }
            twidorStyle = true;
            break;
         case 'M': case 'm':
         case 'I': case 'i':
         case 'F': case 'f':
         case 'R': case 'r':
         case '+':
            if (!twidorStyle) {
               m_Value = -1;
               return;
            }
            break;
         default:
            m_Value = -1;
            return;
         }
         ++read;
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public ThumbKeys(int value) { m_Value = value; }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isValid() { return m_Value != -1; }
   public boolean isEmpty() { return m_Value == 0; }
   public boolean isShift() { return isShift(m_Value); }
   public boolean isCtrl() { return isCtrl(m_Value); }
   public boolean isNum() { return isNum(m_Value); }
   public boolean isAlt() { return isAlt(m_Value); }
   public int toInt() { return m_Value; }

   ////////////////////////////////////////////////////////////////////////////
   public boolean equals(Object o) {
      ThumbKeys other = (ThumbKeys)o;
      return other != null && other.m_Value == m_Value;
   }

   ////////////////////////////////////////////////////////////////////////////
   public int getCount() {
      int count = 0;
      for (int i = 0; i < 4; ++i) {
         count += (m_Value >> i & 1);
      }
      return count;
   }

   /////////////////////////////////////////////////////////////////////////////
   public int toCfg() {
      int cfg = 0;
      for (int i = 0; i < 4; ++i) {
         if ((m_Value & 1 << i) != 0) {
            cfg |= 1 << i * 4;
         }
      }
      return (cfg & 0xF0) >> 4 | (cfg & 0xF) << 4 | (cfg & 0xFF00);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      if (m_Value == -1) {
         return "0000";
      }
      String str = "";
      int len = 0;
      if (isCtrl()) {
         str += 'C';
         ++len;
      }
      if (isAlt()) {
         str += 'A';
         ++len;
      }
      if (isShift()) {
         str += 'S';
         ++len;
      }
      if (isNum()) {
         str += 'N';
         ++len;
      }
      if (len == 0) {
         str += '0';
         ++len;
      }
      for (; len < 4; ++len) {
         str += ' ';
      }
      return str;
   }
	
   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private static boolean isShift(int keys) { return (keys & sm_SHIFT) != 0; }
   private static boolean isCtrl(int keys) { return (keys & sm_CTRL) != 0; }
   private static boolean isNum(int keys) { return (keys & sm_NUM) != 0; }
   private static boolean isAlt(int keys) { return (keys & sm_ALT) != 0; }

   ////////////////////////////////////////////////////////////////////////////
   private Modifiers toModifiers(int keys) {
      Modifiers mod = Modifiers.sm_EMPTY;
      if (isShift(keys)) {
         mod = mod.plus(Modifiers.sm_SHIFT);
      }
      if (isCtrl(keys)) {
         mod = mod.plus(Modifiers.sm_CTRL);
      }
      if (isAlt(keys)) {
         mod = mod.plus(Modifiers.sm_ALT);
      }
      return mod;
   }

   // Data ////////////////////////////////////////////////////////////////////
   public static final int sm_SHIFT = 1;
   public static final int sm_CTRL = 2;
   public static final int sm_NUM = 4;
   public static final int sm_ALT = 8;
   private int m_Value;

   // Main ////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      for (String arg: args) {
         ThumbKeys thumbKeys = new ThumbKeys(arg);
         int keys = thumbKeys.toInt();
         System.out.printf("%d\n", keys);
         System.out.printf("%s\n", thumbKeys.toString());
      }
   }
}
