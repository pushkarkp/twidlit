/**
 * Copyright 2015 Pushkar Piggott
 *
 * Modifiers.java
 */

package pkp.twiddle;

import java.awt.event.KeyEvent;
import pkp.string.StringsInts;
import pkp.util.Pref;

///////////////////////////////////////////////////////////////////////////////
class Modifiers {

   ////////////////////////////////////////////////////////////////////////////
   private static final byte sm_iLEFT_GUI = 0x8;
   private static final byte sm_iLEFT_ALT = 0x4;
   private static final byte sm_iLEFT_SHIFT = 0x2;
   private static final byte sm_iLEFT_CTRL = 0x1;
   static final Modifiers sm_GUI = new Modifiers(sm_iLEFT_GUI);
   static final Modifiers sm_ALT = new Modifiers(sm_iLEFT_ALT);
   static final Modifiers sm_SHIFT = new Modifiers(sm_iLEFT_SHIFT);
   static final Modifiers sm_CTRL = new Modifiers(sm_iLEFT_CTRL);
   static final Modifiers sm_TRANSFERABLE = sm_CTRL.plus(sm_SHIFT).plus(sm_ALT);
   static final Modifiers sm_EMPTY = new Modifiers(0);

   ////////////////////////////////////////////////////////////////////////////
   static void init(StringsInts keyCodeToName) {
      sm_KeyCodeToName = keyCodeToName;
   }

   ////////////////////////////////////////////////////////////////////////////
   static Modifiers fromKeyCode(int keyCode) {
      return new Modifiers(keyCode >> sm_BITS & sm_KEYS);
   }

   ////////////////////////////////////////////////////////////////////////////
   static Modifiers[] getCombinations(Modifiers m) {
//System.out.printf("getCombinations: m: 0x%x asButtons 0x%x%n", m.toInt(), m.asButtons().toInt());
      return sm_COMBINATIONS[m.asButtons().toInt()];
   }

   ////////////////////////////////////////////////////////////////////////////
   static String toString(Modifiers[] m) {
      if (m == null || m.length == 0) {
         return "none";
      }
      String str = String.format("%d mods: ", m.length);
      String sep = "";
      for (int i = 0; i < m.length; ++i) {
         str += sep + String.format("0x%x", m[i].toInt());
         sep = ", ";
      }
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   static Modifiers fromKeyEvent(KeyEvent ke) {
      byte m = 0;
      if (ke.isControlDown()) {
         m |= sm_iLEFT_CTRL;
      }
      if (ke.isShiftDown()) {
         m |= sm_iLEFT_SHIFT;
      }
      if (ke.isAltDown()) {
         m |= sm_iLEFT_ALT;
      }
      if (ke.isMetaDown()) {
         m |= sm_iLEFT_GUI;
      }
      if (ke.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
         m <<= 4;
      }
      return new Modifiers(m);
   }

   ////////////////////////////////////////////////////////////////////////////
   Modifiers(Modifiers m) {
      m_Value = m.m_Value;
      m_Sided = m.m_Sided;
   }

   ////////////////////////////////////////////////////////////////////////////
   boolean equals(Modifiers m) {
      return m_Value == m.m_Value
          || (!m_Sided && onLeft().m_Value == m.onLeft().m_Value);
   }

   ////////////////////////////////////////////////////////////////////////////
   boolean isSubsetOf(Modifiers m) {
      if ((m_Value & m.m_Value) == m_Value) {
         return true;
      }
      if (m_Sided) {
         return false;
      }
      byte onLeft = onLeft().m_Value;
      return (onLeft & m.onLeft().m_Value) == onLeft;
   }

   ////////////////////////////////////////////////////////////////////////////
   // Makes side neutral.
   Modifiers onLeft() {
      return new Modifiers((m_Value | m_Value >> 4) & sm_LEFT);
   }

   ////////////////////////////////////////////////////////////////////////////
   Modifiers plus(Modifiers m) {
      return new Modifiers(m_Value | m.m_Value);
   }

   ////////////////////////////////////////////////////////////////////////////
   Modifiers minus(Modifiers m) {
      return new Modifiers(m_Value & ~m.m_Value);
   }

   ////////////////////////////////////////////////////////////////////////////
   int toKeyCode() {
      int value = (int)m_Value << KeyPress.sm_KEYCODE_BITS;
//System.out.printf("toKeyCode: 0x%x%n", value);
      return value;
   }

   ////////////////////////////////////////////////////////////////////////////
   boolean isEmpty() { return m_Value == 0; }
   boolean isCtrl() { return (m_Value & sm_iBOTH_CTRL) != 0; }
   boolean isShift() { return (m_Value & sm_iBOTH_SHIFT) != 0; }
   boolean isAlt() { return (m_Value & sm_iBOTH_ALT) != 0; }
   boolean isGui() { return (m_Value & sm_iBOTH_GUI) != 0; }
   boolean isValid() { return (m_Value & sm_KEYS) == 0; }
   int toInt() { return (int)m_Value & sm_KEYS; }
   public String toString() { return toString(true); }
   String toLeadTagString() { return toString(true); }
   String toTailTagString() { return toString(false); }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private Modifiers(int m) {
      m_Value = (byte)(m & sm_KEYS);
      m_Sided = true;
   }

   ////////////////////////////////////////////////////////////////////////////
   private Modifiers asButtons() {
      int value = m_Sided 
                 ? m_Value
                 : (m_Value | m_Value >> 4);
      return new Modifiers(value & (sm_iLEFT_ALT | sm_iLEFT_SHIFT | sm_iLEFT_CTRL));
   }

   ////////////////////////////////////////////////////////////////////////////
   private String toString(boolean lead) {
      if (m_Value == 0) {
         return "";
      }
      final String prefix = (lead) ? "" : "/";
      String str = "";
      for (int i = sm_iLEFT_CTRL; i <= sm_MAX_BIT; i <<= 1) {
         if ((m_Value & i) != 0) {
            String strTag = 
               KeyPress.sm_BeforeName 
             + prefix 
             + sm_KeyCodeToName.getString(i << 8, "undefined") 
             + KeyPress.sm_AfterName;
            if (lead) {
               str += strTag;
            } else {
               str = strTag + str;
            }
         }
      }
      return str;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static final int sm_KEYS = 0xFF;
   private static final int sm_BITS = 8;
   private static final int sm_MAX_BIT = 0x80;
   private static final int sm_LEFT = 0x0F;
   private static final int sm_RIGHT = 0xF0;
   private static final int sm_iBOTH_GUI = 0x88;
   private static final int sm_RIGHT_GUI = 0x80;
   private static final int sm_iBOTH_ALT = 0x44;
   private static final int sm_RIGHT_ALT = 0x40;
   private static final int sm_iBOTH_SHIFT = 0x22;
   private static final int sm_RIGHT_SHIFT = 0x20;
   private static final int sm_iBOTH_CTRL = 0x11;
   private static final int sm_RIGHT_CTRL = 0x10;
   // Thumb buttons register only the left modifier key (0x8 not 0x80 or 0x88).
   private static final Modifiers sm_COMBINATIONS[][] = new Modifiers[][] {
      null,
      {sm_EMPTY, sm_CTRL},
      {sm_EMPTY, sm_SHIFT},
      {sm_EMPTY, sm_SHIFT, sm_CTRL, sm_SHIFT.plus(sm_CTRL)},
      {sm_EMPTY, sm_ALT},
      {sm_EMPTY, sm_CTRL, sm_ALT, sm_ALT.plus(sm_CTRL)},
      {sm_EMPTY, sm_SHIFT, sm_ALT, sm_ALT.plus(sm_SHIFT)},
      {sm_EMPTY, sm_SHIFT, sm_CTRL, sm_ALT, sm_SHIFT.plus(sm_CTRL), sm_ALT.plus(sm_SHIFT), sm_ALT.plus(sm_CTRL), sm_ALT.plus(sm_SHIFT.plus(sm_CTRL))},
   };
   private static StringsInts sm_KeyCodeToName;
   private final byte m_Value;
   private final boolean m_Sided;
}
