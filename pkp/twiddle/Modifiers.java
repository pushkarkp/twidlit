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
public class Modifiers {

   ////////////////////////////////////////////////////////////////////////////
   private static final int sm_iGUI = 0x88;
   private static final int sm_iALT = 0x44;
   private static final int sm_iSHIFT = 0x22;
   private static final int sm_iCTRL = 0x11;
   public static final Modifiers sm_GUI = new Modifiers(sm_iGUI);
   public static final Modifiers sm_ALT = new Modifiers(sm_iALT);
   public static final Modifiers sm_SHIFT = new Modifiers(sm_iSHIFT);
   public static final Modifiers sm_CTRL = new Modifiers(sm_iCTRL);
   public static final Modifiers sm_TRANSFERABLE = sm_CTRL.plus(sm_SHIFT).plus(sm_ALT);
   public static final Modifiers sm_EMPTY = new Modifiers(0);

   ////////////////////////////////////////////////////////////////////////////
   public static void init(StringsInts keyCodeToName) {
      sm_IgnoreKeyboardSide = Pref.getBool("ignore.keyboard.side", false);
      sm_KeyCodeToName = keyCodeToName;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Modifiers fromKeyCode(int keyCode) {
      return new Modifiers(keyCode >> sm_BITS & sm_KEYS);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Modifiers[] getCombinations(Modifiers m) {
      return sm_COMBINATIONS[m.transferable().toInt()];
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Modifiers fromKeyEvent(KeyEvent ke) {
      int m = 0;
      if (ke.isControlDown()) {
         m |= sm_LEFT_CTRL;
      }
      if (ke.isShiftDown()) {
         m |= sm_LEFT_SHIFT;
      }
      if (ke.isAltDown()) {
         m |= sm_LEFT_ALT;
      }
      if (ke.isMetaDown()) {
         m |= sm_LEFT_GUI;
      }
      if (ke.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
         m <<= 4;
      }
      return new Modifiers(m);
   }

   ////////////////////////////////////////////////////////////////////////////
   public Modifiers(Modifiers m) {
      m_Value = m.m_Value;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean equals(Modifiers m) {
      return effect().m_Value == m.effect().m_Value;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isSubsetOf(Modifiers m) {
      int mEffect = m.effect().m_Value;
      return (effect().m_Value & mEffect) == mEffect;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Modifiers effect() {
      return new Modifiers((m_Value | m_Value >> 4) & sm_LEFT);
   }

   ////////////////////////////////////////////////////////////////////////////
   public Modifiers plus(Modifiers m) {
      return new Modifiers(m_Value | m.m_Value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public Modifiers minus(Modifiers m) {
      return new Modifiers(m_Value & ~m.m_Value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public int toKeyCode() {
      return m_Value << KeyPress.sm_KEYCODE_BITS;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isEmpty() { return m_Value == 0; }
   public boolean isCtrl() { return (m_Value & sm_iCTRL) != 0; }
   public boolean isShift() { return (m_Value & sm_iSHIFT) != 0; }
   public boolean isAlt() { return (m_Value & sm_iALT) != 0; }
   public boolean isGui() { return (m_Value & sm_iGUI) != 0; }
   public boolean isValid() { return (m_Value & sm_KEYS) == 0; }
   public int toInt() { return m_Value; }
   public String toString() { return toString(true); }
   public String toLeadTagString() { return toString(true); }
   public String toTailTagString() { return toString(false); }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private Modifiers(int m) {
      m_Value = m & sm_KEYS;
   }

   ////////////////////////////////////////////////////////////////////////////
   private Modifiers transferable() {
      return new Modifiers((m_Value | m_Value >> 4) & (sm_LEFT_ALT | sm_LEFT_SHIFT | sm_LEFT_CTRL));
   }

   ////////////////////////////////////////////////////////////////////////////
   private String toString(boolean lead) {
      if (m_Value == 0) {
         return "";
      }
      final String prefix = (lead) ? "" : "/";
      String str = "";
      for (int i = sm_iCTRL; i < sm_MAX_BIT << 1; i <<= 1) {
         if ((m_Value & i) != 0) {
			String strTag = KeyPress.sm_BeforeName + prefix + sm_KeyCodeToName.getString(i << 8, "undefined") + KeyPress.sm_AfterName;
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
   private static final int sm_LEFT = 0x0F;
   private static final int sm_RIGHT = 0xF0;
   private static final int sm_MAX_BIT = 0x80;
   private static final int sm_LEFT_GUI = 0x8;
   private static final int sm_RIGHT_GUI = 0x80;
   private static final int sm_LEFT_ALT = 0x4;
   private static final int sm_RIGHT_ALT = 0x40;
   private static final int sm_LEFT_SHIFT = 0x2;
   private static final int sm_RIGHT_SHIFT = 0x20;
   private static final int sm_LEFT_CTRL = 0x1;
   private static final int sm_RIGHT_CTRL = 0x10;
   private static final Modifiers sm_COMBINATIONS[][] = new Modifiers[][] {
      {sm_EMPTY},
      {sm_EMPTY, sm_CTRL},
      {sm_EMPTY, sm_SHIFT},
      {sm_EMPTY, sm_SHIFT, sm_CTRL, sm_SHIFT.plus(sm_CTRL)},
      {sm_EMPTY, sm_ALT},
      {sm_EMPTY, sm_CTRL, sm_ALT, sm_ALT.plus(sm_CTRL)},
      {sm_EMPTY, sm_SHIFT, sm_ALT, sm_ALT.plus(sm_SHIFT)},
      {sm_EMPTY, sm_SHIFT, sm_CTRL, sm_ALT, sm_SHIFT.plus(sm_CTRL), sm_ALT.plus(sm_SHIFT), sm_ALT.plus(sm_CTRL), sm_ALT.plus(sm_SHIFT.plus(sm_CTRL))},
   };
   private static boolean sm_IgnoreKeyboardSide;
   private static StringsInts sm_KeyCodeToName;
   private final int m_Value;
}
