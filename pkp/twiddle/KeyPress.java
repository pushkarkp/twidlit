/**
/**
 * Copyright 2015 Pushkar Piggott
 *
 * KeyPress.java
 */

package pkp.twiddle;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.net.URL;
import pkp.lookup.LookupSet;
import pkp.lookup.LookupSetBuilder;
import pkp.lookup.LookupTable;
import pkp.lookup.LookupTableBuilder;
import pkp.lookup.LookupBuilder.Duplicates;
import pkp.io.Io;
import pkp.io.SpacedPairReader;
import pkp.string.StringsInts;
import pkp.string.StringsIntsBuilder;
import pkp.util.Persist;
import pkp.util.Pref;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class KeyPress {

   ////////////////////////////////////////////////////////////////////////////
   static final int sm_KEYCODE_BITS = 8;

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
   // Load the conversion tables.
   public static void init() {
      sm_Warned = false;
      String str = Pref.get("file.format", Format.STD.name());
      sm_FileFormat = Format.valueOf(str.toUpperCase());
      str = Pref.get("display.format", Format.STD.name());
      sm_DisplayFormat = Format.valueOf(str.toUpperCase());
      final Io.StringToInt escKey = new Io.StringToInt() {
                                       public int cvt(String str) {
                                          return escKeyToInt(str);
                                       }
                                    };
      final Io.StringToInt escChar = new Io.StringToInt() {
                                       public int cvt(String str) {
                                          return Io.parseEscape1(str);
                                       }
                                    };
      final Io.StringToInt parsePos0xFFFF = new Io.StringToInt() {
                                          public int cvt(String str) {
                                             return Io.toPosInt(0xFFFF, str);
                                           }
                                       };
      sm_KeyValueToCode = LookupTableBuilder.read(
         Persist.getExistDirJarUrl("pref.dir", "TwidlitKeyValues.txt"),
         LookupTableBuilder.sm_SWAP_KEYS, Io.sm_MUST_EXIST,
         Duplicates.OVERWRITE,
         1, 0x7F,
         escKey, escChar);
      sm_KeyEventToCode = LookupTableBuilder.read(
         Persist.getExistDirJarUrl("pref.dir", "TwidlitKeyEvents.txt"),
         LookupTableBuilder.sm_SWAP_KEYS, Io.sm_MUST_EXIST,
         Duplicates.ERROR,
         0x10, 0x7F,
         escKey, parsePos0xFFFF);
      sm_KeyCodeToName = (new StringsIntsBuilder(Persist.getExistDirJarUrl("pref.dir", "TwidlitKeyNames.txt"), true, escKey)).build();
      // unprintables are mostly < 0x20
      sm_Unprintable = LookupSetBuilder.read(
         Persist.getExistDirJarUrl("pref.dir", "TwidlitUnprintables.txt"),
         Io.sm_MUST_EXIST,
         0, 0x20, escChar);
      // duplicates are mostly numpad keys
      sm_Duplicate = LookupSetBuilder.read(
         Persist.getExistDirJarUrl("pref.dir", "TwidlitDuplicates.txt"),
         Io.sm_MUST_EXIST,
         0x54, 0x70, escKey);
      sm_Lost = LookupSetBuilder.read(
         Persist.getExistDirJarUrl("pref.dir", "TwidlitLost.txt"),
         Io.sm_MUST_EXIST,
         0x1, 0x0, escKey);
      int[] kcv = readKeyCodeValues(Persist.getExistDirJarUrl("pref.dir", "TwidlitKeyValues.txt"));
      sm_KeyCodeToValue = new HashMap<Integer, Character>(0x80);
      for (int i = 0; kcv[i] > 0; i += 2) {
         sm_KeyCodeToValue.put(kcv[i], (char)kcv[i + 1]);
      }
      Modifiers.init(sm_KeyCodeToName);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static KeyPress noModifiers(int keyCode) {
      return new KeyPress(keyCode, null);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public static KeyPress fromKeyCode(int keyCode) {
      return new KeyPress(keyCode & sm_KEYS, Modifiers.fromKeyCode(keyCode));
   }
   
   ////////////////////////////////////////////////////////////////////////////
   // a legal empty kp (!isValid())
   public KeyPress() {
      m_KeyCode = 0;
      m_Modifiers = Modifiers.sm_EMPTY;
   }

   ////////////////////////////////////////////////////////////////////////////
   public KeyPress(int keyCode, Modifiers modifiers) {
      if (keyCode < 0) {
         m_KeyCode = 0;
         m_Modifiers = Modifiers.sm_EMPTY;
         return;
      }
      if (keyCode > sm_KEYS) {
         Log.err(String.format("KeyCode (0x%x) includes modifiers", keyCode));
         m_KeyCode = 0;
         m_Modifiers = Modifiers.sm_EMPTY;
         return;
      }
//System.out.printf("KeyPress k 0x%x m 0x%x%n", keyCode, modifiers.toInt());
      // don't allow modifiers in keycode
      m_KeyCode = keyCode & sm_KEYS;
      m_Modifiers = modifiers;
      // catch unnamed keys early
      if (toString() == null) {
			m_KeyCode = 0;
			m_Modifiers = Modifiers.sm_EMPTY;
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static KeyPress parseEscape(String str) {
      return noModifiers(escKeyToInt(str));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int escKeyToInt(String str) {
      if (!"\\k".equals(str.substring(0, 2))) {
         Log.err("Expecting \"\\kxxxx\" (x is a hex digit), found \"" + str + '"');
      }
      if (str.length() < 6) {
         Log.err("Expecting \"\\kxxxx\" (x is a hex digit), found too short \"" + str + '"');
      }
      if (Io.findFirstNotOfUpTo(str.substring(2), Io.sm_HEX_DIGIT, 4) < 4) {
         Log.err("Expecting \"\\kxxxx\" (x is a hex digit), found non-hex digit in \"" + str + '"');
      }
      return Integer.parseInt(str.substring(2), 16);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static KeyPress parseTag(String tag, Modifiers modifiers) {
      if ("".equals(tag)) {
         return new KeyPress();
      }
      char c0 = tag.charAt(0);
//System.out.printf("kp parseTag tag %s c0 %c\n", tag, c0);
      if ('0' <= c0 && c0 <= '9') {
//System.out.printf("tag %s c0 %c\n", tag, c0);
         if (tag.length() > 1 && tag.substring(0, 2).equals("0x")) {
            return parseText((char)Integer.parseInt(tag.substring(2), 16), modifiers);
         }
         return parseText((char)Integer.parseInt(tag), modifiers);
      }
      boolean closing = (c0 == '/');
      if (closing) {
         tag = tag.substring(1);
      }
      int keyCode = sm_KeyCodeToName.getInt(tag, 0);
      Modifiers mod = Modifiers.fromKeyCode(keyCode);
//System.out.printf("parseTag: keyCode 0x%x modifiers 0x%x tag %s mod 0x%x\n", keyCode, modifiers.toInt(), tag, mod.toInt());
      if (mod.isEmpty()) {
         if (closing) {
				Log.err("Unrecognized closing modifier: \"" + tag + "\".");
            keyCode = 0;
         }
      } else if (closing) {
         modifiers = modifiers.minus(mod);
			if (modifiers.isEmpty()) {
				return new KeyPress();
			}
      } else {
         modifiers = modifiers.plus(mod);
      }
      return new KeyPress(keyCode & sm_KEYS, modifiers);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static KeyPress parseText(char ch, Modifiers mod) {
//System.out.println("kp parseText '" + ch + "'");
      int keyCodeWithShift = sm_KeyValueToCode.get(ch);
		if (keyCodeWithShift < 0) {
         Log.log("\"" + ch + "\" has no code.");
         return new KeyPress();
      }
//System.out.printf("parseText |%c| (%d) -> 0x%x (mod 0x%x)\n", ch, (int)ch, keyCodeWithShift, mod.toInt());
      return new KeyPress(keyCodeWithShift & sm_KEYS,
                          Modifiers.fromKeyCode(keyCodeWithShift).plus(mod));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static KeyPress parseText(char ch) {
      return parseText(ch, Modifiers.fromKeyCode(0));
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String parseText(String in, Format f) {
      String out = "";
      for (int i = 0; i < in.length(); ++i) {
         out += parseText(in.charAt(i)).toString(f);
      }
      return out;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static String toString(KeyEvent ke) {
      return String.format("key event code 0x%x ", ke.getKeyCode())
           + Modifiers.fromKeyEvent(ke).toString();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static KeyPress parseEvent(KeyEvent ke) {
//System.out.printf("java key code %d modifiers 0x%x side %d\n", ke.getKeyCode(), Modifiers.fromKeyEvent(ke).toInt(), ke.getKeyLocation());
      int keyCode = sm_KeyEventToCode.get(ke.getKeyCode());
      if (keyCode < 0) {
         Log.log(String.format("Key event code %d has no key code.", ke.getKeyCode()));
         return new KeyPress();
      }
     return new KeyPress(keyCode, Modifiers.fromKeyEvent(ke));
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean equals(KeyPress kp) {
//System.out.printf("0x%x != 0x%x || 0x%x != 0x%x%n", m_KeyCode, kp.m_KeyCode, m_Modifiers.toInt(), kp.m_Modifiers.toInt());
      return m_KeyCode == kp.m_KeyCode
          && m_Modifiers.equals(kp.m_Modifiers);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() { return toString(Format.FILE); }
   public String toTagString() { return toString(Format.TAG); }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(Format format) {
		if (format == Format.DISPLAY) {
         format = sm_DisplayFormat;
		}
		if (format == Format.FILE) {
         format = sm_FileFormat;
		}
//System.out.println(format.name());
		if (format == Format.HEX) {
         return String.format("\\k%04x", toInt());
		}
	   if (format == Format.TAG) {
         return m_Modifiers.toString(keyCodeToTagString(m_KeyCode));
		}
		if (m_KeyCode == 0) {
			if (m_Modifiers.isEmpty()) {
				Log.err("Empty KeyPress.");
				return "empty";
			}
			return m_Modifiers.toLeadTagString();
		}
      if (sm_Duplicate.is(m_KeyCode)) {
         Log.log(String.format("Key 0x%x is a duplicate", m_KeyCode));
         return "duplicate";
      }
      Modifiers modifiers = new Modifiers(m_Modifiers);
      Character keyValue = null;
      if (isShift()) {
         // attempt to convert to shifted value
         int shiftedKeyCode = m_KeyCode | Modifiers.sm_SHIFT.toKeyCode();
         keyValue = sm_KeyCodeToValue.get(shiftedKeyCode);
         if (keyValue != null) {
            // succeeded, cancel shift modifier
            modifiers = modifiers.minus(Modifiers.sm_SHIFT);
         }
      }
      if (keyValue == null) {
         keyValue = sm_KeyCodeToValue.get(m_KeyCode);
         if (keyValue == null) {
            // no value, try name
            String str = toTag(modifiers);
            if (str != null) {
//System.out.println("toTag1 " + str);
               return str;
            }
            if (!sm_Warned) {
               sm_Warned = true;
               Log.err(String.format("One or more mapped key codes have no name or value (see log for details)."));
            }
            Log.log(String.format("Key \\x%x has no name or value", m_KeyCode));
            return "no name or value";
         }
      }
      if (format == Format.TAG
       || (format == Format.STD
       &&  (keyValue == 32 || keyValue == 10 || keyValue == 9))
       || sm_Unprintable.is(keyValue)) {
         String str = toTag(modifiers);
         if (str != null) {
//System.out.println("toTag2 " + str);
            return str;
         }
      }
      if (format == Format.STD
       || format == Format.ESC) {
         return modifiers.toString(Io.toEscapeChar(keyValue));
      }
      if (format != Format.TXT) {
         Log.err("Unexpected format specifier " + format);
      }
      return modifiers.toString("" + keyValue);
   }
/*
   ////////////////////////////////////////////////////////////////////////////
   public String toTagString(Modifiers prevModifiers) {
      String str = "";
      if (!m_Modifiers.equals(prevModifiers)) {
         str = prevModifiers.minus(m_Modifiers).toTailTagString()
             + m_Modifiers.minus(prevModifiers).toLeadTagString();
      }
      return str + keyCodeToTagString(m_KeyCode);
   }
*/

   ////////////////////////////////////////////////////////////////////////////
   public static void clearWarned() { sm_Warned = false; }
   public boolean isValid() { return m_KeyCode != 0 || !m_Modifiers.isEmpty(); }
   public boolean isModifiers() { return m_KeyCode == 0; }
   public boolean isPrintable() { return !sm_Unprintable.is(m_KeyCode); }
   public boolean isDuplicate() { return sm_Duplicate.is(m_KeyCode); }
   public boolean isLost() { return sm_Lost.is(m_KeyCode, m_Modifiers.onLeft().toInt()); }
   public boolean isCtrl() { return m_Modifiers.isCtrl(); }
   public boolean isShift() { return m_Modifiers.isShift(); }
   public boolean isAlt() { return m_Modifiers.isAlt(); }
   public boolean isGui() { return m_Modifiers.isGui(); }
   public int toInt() { return m_Modifiers.toKeyCode() + m_KeyCode; }

   int getKeyCode() { return m_KeyCode; }
   Modifiers getModifiers() { return m_Modifiers; }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private KeyPress(KeyPress kp) {
      m_KeyCode = kp.m_KeyCode;
      m_Modifiers = kp.m_Modifiers;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static String keyCodeToTagString(int i) {
      String str = sm_KeyCodeToName.getString(i, "");
      if (!"".equals(str)) {
         return '<' + str + '>';
      }
      return "0x" + Integer.toHexString((char)i);
   }

	////////////////////////////////////////////////////////////////////////////
   private static int[] readKeyCodeValues(URL url) {
      int kv[] = new int[513];
      SpacedPairReader spr = new SpacedPairReader(url, Io.sm_MUST_EXIST);
      String keyCode;
      int key = 0;
      for (; (keyCode = spr.getNextFirst()) != null; key += 2) {
         kv[key] = escKeyToInt(keyCode);
         if (kv[key] <= 0) {
            Log.err(String.format("Expected a positive integer key code while parsing \"%s\" on line %d of \"%s\".",
                                  keyCode, spr.getLineNumber(), url.getPath()));
         }
         String value = spr.getNextSecond();
         kv[key + 1] = Io.parseEscape1(value);
         if (kv[key + 1] < 0) {
            Log.err(String.format("Failed to parse \"%s\" on line %d of \"%s\".",
                                  value, spr.getLineNumber(), url.getPath()));
         }
      }
      spr.close();
      kv[key] = 0;
      return kv;
   }

   ////////////////////////////////////////////////////////////////////////////
   private String toTag(Modifiers modifiers) {
      String keyName = sm_KeyCodeToName.getString(m_KeyCode, "");
      if (!"".equals(keyName)) {
         return modifiers.toString('<' + keyName + '>');
      }
      keyName = sm_KeyCodeToName.getString(toInt(), "");
      if (!"".equals(keyName)) {
         return '<' + keyName + '>';
      }
      return null;
   }
   
   // Data ////////////////////////////////////////////////////////////////////
   private static final int sm_KEYS = 0xFF;
   private static Format sm_DisplayFormat;
   private static Format sm_FileFormat;
   private static LookupTable sm_KeyEventToCode;
   private static HashMap<Integer, Character> sm_KeyCodeToValue;
   private static StringsInts sm_KeyCodeToName;
   private static LookupTable sm_KeyValueToCode;
   private static LookupSet sm_Unprintable;
   private static LookupSet sm_Duplicate;
   private static LookupSet sm_Lost;
   private static boolean sm_Warned;

   private int m_KeyCode;
   private Modifiers m_Modifiers;

   ////////////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      Persist.init("TwidlitPersist.properties", ".", "pref");
      Pref.init("TwidlitPreferences.txt", Persist.get("pref.dir"), "pref");
      KeyPress.init();
      if (args.length == 0) {
         //System.out.println("add a string");
         for (int i = 0; i < 255; ++i) {
            System.out.print(String.format("%d %s%n", i, sm_KeyCodeToName.getString(i, "")));
         }
         return;
      }
      for (int i = 0; i < args.length; ++i) {
			char c = args[i].charAt(0);
         if (c == '<') {
            KeyPress act = KeyPress.parseTag(args[i].substring(1, args[i].length() - 1), Modifiers.sm_EMPTY);
            System.out.printf("'%s': '%s' '%s'\n", args[i], act.toString(), act.toTagString());
         } else if (c == '-') {
            switch (args[i].charAt(1)) {
            case 'c': System.out.print(sm_KeyCodeToValue.toString()); break;
            case 'e': System.out.print(sm_KeyEventToCode.toString()); break;
            case 'n': System.out.print(sm_KeyCodeToName.toString()); break;
            case 'v': System.out.print(sm_KeyValueToCode.toString()); break;
            }
         } else {
            for (int j = 0; j < args[i].length(); ++j) {
               char ch = args[i].charAt(j);
               KeyPress kp = KeyPress.parseText(ch, Modifiers.sm_EMPTY);
               System.out.printf("'%c': '%s'\n", ch, kp.toTagString());
            }
         }
      }
   }
}
