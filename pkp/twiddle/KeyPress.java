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
   public static final int sm_KEYCODE_BITS = 8;
   public static char sm_BeforeName;
   public static char sm_AfterName;

   ////////////////////////////////////////////////////////////////////////////
   public enum Format {
      TEXT_("Text"),
      TEXT("Text with named space"),
      TAG("Tags"),
      ESCAPED("Escaped"),
      HEX("Hexadecimal"),
      CFG("cfg.txt");

      public String toString() {
         return m_Name;
      }
      public final String m_Name;

      private Format(String name) {
         m_Name = name;
      }
      private int m_Value;
   }

   ////////////////////////////////////////////////////////////////////////////
   // Load the conversion tables.
   public static void init() {
      sm_Warned = false;
		sm_BeforeName = Pref.get("name.delimiter.start", "<").charAt(0);
      sm_AfterName = Pref.get("name.delimiter.end", ">").charAt(0);
      final Io.StringToInt charToInt = new Io.StringToInt() {
                                          public int cvt(String str) {
                                             return parseKeyValue(str);
                                          }
                                       };
      final Io.StringToInt parsePos = new Io.StringToInt() {
                                          public int cvt(String str) {
                                             return Io.toPosInt(str); 
                                          }
                                       };
      final Io.StringToInt parsePos0xFFFF = new Io.StringToInt() {
                                          public int cvt(String str) {
                                             return (Io.toPosInt(0xFFFF, str));
                                           }
                                       };
      final Io.StringToInt parsePos0xFF = new Io.StringToInt() {
                                          public int cvt(String str) {
                                             return (Io.toPosInt(0xFF, str));
                                          }
                                       };
      final Io.StringToInt parsePos0xF = new Io.StringToInt() {
                                          public int cvt(String str) {
                                             return (Io.toPosInt(0xF, str));
                                          }
                                       };
      sm_KeyValueToCode = LookupTableBuilder.read(
         Pref.getExistDirJarUrl("pref.dir", "TwidlitKeyValues.txt"), 
         LookupTableBuilder.sm_SWAP_KEYS, Io.sm_MUST_EXIST,
         Duplicates.OVERWRITE, 
         1, 0x7F, 
         parsePos0xFFFF, charToInt);
//System.out.println("sm_KeyValueToCode:\n" + sm_KeyValueToCode.toString());
      sm_KeyEventToCode = LookupTableBuilder.read(
         Pref.getExistDirJarUrl("pref.dir", "TwidlitKeyEvents.txt"),
         LookupTableBuilder.sm_SWAP_KEYS, Io.sm_MUST_EXIST,
         Duplicates.ERROR, 
         0x10, 0x7F, 
         parsePos0xFFFF, parsePos0xFFFF);
//System.out.println("sm_KeyEventToCode:%n" + sm_KeyEventToCode.toString());
      sm_KeyCodeToName = (new StringsIntsBuilder(Pref.getExistDirJarUrl("pref.dir", "TwidlitKeyNames.txt"), true)).build();
      // unprintables are mostly < 0x20
      sm_Unprintable = LookupSetBuilder.read(
         Pref.getExistDirJarUrl("pref.dir", "TwidlitUnprintables.txt"), 
         Io.sm_MUST_EXIST, 
         0, 0x20, parsePos0xFF);
//System.out.println("sm_Unprintable:\n" + sm_Unprintable.toString());
      // duplicates are mostly numpad keys
      sm_Duplicate = LookupSetBuilder.read(
         Pref.getExistDirJarUrl("pref.dir", "TwidlitDuplicates.txt"),
         Io.sm_MUST_EXIST, 
         0x54, 0x70, parsePos0xFF);
//System.out.println("sm_Duplicate:\n" + sm_Duplicate.toString());
      sm_Lost = LookupSetBuilder.read2(
         Pref.getExistDirJarUrl("pref.dir", "TwidlitLost.txt"),
         LookupSetBuilder.sm_SWAP_KEYS, Io.sm_MUST_EXIST, 
         0x1, 0x0, 
         parsePos0xF, parsePos0xFF);
//System.out.println("sm_Lost:\n" + sm_Lost.toString());
      int[] kcv = readKeyCodeValues(Pref.getExistDirJarUrl("pref.dir", "TwidlitKeyValues.txt"));
      sm_KeyCodeToValue = new HashMap<Integer, Character>(0x80);
      for (int i = 0; kcv[i] > 0; i += 2) {
         sm_KeyCodeToValue.put(kcv[i], (char)kcv[i + 1]);
      }
      Modifiers.init(sm_KeyCodeToName);
   }

   ////////////////////////////////////////////////////////////////////////////
	// a legal empty kp
   public KeyPress() {
      m_KeyCode = 0;
      m_Modifiers = Modifiers.sm_EMPTY;
   }

   ////////////////////////////////////////////////////////////////////////////
   public KeyPress(int keyCode) {
      this(keyCode, Modifiers.fromKeyCode(keyCode));
   }

   ////////////////////////////////////////////////////////////////////////////
   public KeyPress(int keyCode, Modifiers modifiers) {
		if (keyCode < 0) {
			m_KeyCode = 0;
			m_Modifiers = Modifiers.sm_EMPTY;
			return;
		}
//System.out.printf("KeyPress k 0x%x m 0x%x%n", keyCode, modifiers.toInt());
      m_KeyCode = keyCode & sm_KEYS;
      m_Modifiers = modifiers;
      // catch unnamed keys early
      if (toString() == null) {
			m_KeyCode = 0;
			m_Modifiers = Modifiers.sm_EMPTY;
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static KeyPress parseTag(String tag, Modifiers modifiers) {
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
      return new KeyPress(keyCode, modifiers);
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
      return new KeyPress(keyCodeWithShift,
                          Modifiers.fromKeyCode(keyCodeWithShift).plus(mod));
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
   public String toString() {
      return toString(Format.TEXT_);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toTagString() {
      return toString(Format.TAG);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(Format format) {
		if (format == Format.HEX) {
         return String.format("k 0x%x m 0x%x: 0x%x", m_KeyCode, m_Modifiers.toInt(), (m_Modifiers.toKeyCode() | (sm_KEYS & m_KeyCode)));
		}
	   if (format == Format.TAG) {
         return toString(keyCodeToString(m_KeyCode));
		}
      String str = toStringCommon();
      if (str != null) {
         if (format == Format.CFG) { 
            str = Io.toEscaped(str);
         }
//System.out.println("kp toString " + str);
         return str;
      }
      Character keyValue = sm_KeyCodeToValue.get(m_KeyCode);
		if (format == Format.ESCAPED) {
			if (keyValue != null && (keyValue <= 32 || keyValue > 126)) {
				return toString(Io.toEscaped(keyValue));
			}
			return toTagString();
		}
      if (format == Format.TEXT_ && keyValue != null && keyValue == 32) {
			return toString("" + keyValue);
		}
      String keyName = sm_KeyCodeToName.getString(m_KeyCode, "");
      if (!"".equals(keyName)) {
         return toString(sm_BeforeName + keyName + sm_AfterName);
      }
      keyName = sm_KeyCodeToName.getString(toInt(), "");
      if (!"".equals(keyName)) {
         return sm_BeforeName + keyName + sm_AfterName;
      }
      if (!sm_Warned) {
         sm_Warned = true;
         Log.warn(String.format("One or more mapped key codes have no name or value (see log for details)."));
      }
      Log.log(String.format("Key code %d (0x%x) has no name or value.", toInt(), toInt()));
      return null;
   }
/*
   ////////////////////////////////////////////////////////////////////////////
   public String toTagString(Modifiers prevModifiers) {
      String str = "";
      if (!m_Modifiers.equals(prevModifiers)) {
         str = prevModifiers.minus(m_Modifiers).toTailTagString()
             + m_Modifiers.minus(prevModifiers).toLeadTagString();
      }
      return str + keyCodeToString(m_KeyCode);
   }
*/
   
   ////////////////////////////////////////////////////////////////////////////
   public static void clearWarned() { sm_Warned = false; }
   public boolean isValid() { return m_KeyCode != 0 || !m_Modifiers.isEmpty(); }
   public boolean isModifiers() { return m_KeyCode == 0; }
   public boolean isPrintable() { return !sm_Unprintable.is(m_KeyCode); }
   public boolean isDuplicate() { return sm_Duplicate.is(m_KeyCode); }
   public boolean isLost() { return sm_Lost.is(m_KeyCode, m_Modifiers.onLeft().toInt()); }
   public int getKeyCode() { return m_KeyCode; }
   public Modifiers getModifiers() { return m_Modifiers; }
   public boolean isCtrl() { return m_Modifiers.isCtrl(); }
   public boolean isShift() { return m_Modifiers.isShift(); }
   public boolean isAlt() { return m_Modifiers.isAlt(); }
   public boolean isGui() { return m_Modifiers.isGui(); }
   public int toInt() { return m_Modifiers.toKeyCode() + m_KeyCode; }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private KeyPress(KeyPress kp) {
      m_KeyCode = kp.m_KeyCode;
      m_Modifiers = kp.m_Modifiers;
   }

   ////////////////////////////////////////////////////////////////////////////
   private String toStringCommon() {
		if (m_KeyCode == 0) {
			if (m_Modifiers.isEmpty()) {
				Log.err("Empty KeyPress.");
				return "";
			}
			return m_Modifiers.toLeadTagString();
		}
      if (sm_Duplicate.is(m_KeyCode)) {
         //Log.log(String.format("Key 0x%x is a duplicate", m_KeyCode));
         return null;
      }
      Modifiers modifiers = new Modifiers(m_Modifiers);
      Character keyValue = null;
      if (isShift()) {
         // attempt to convert to shifted value
//System.out.printf("shifting m_KeyCode 0x%x m_Modifiers 0x%x\n", m_KeyCode, m_Modifiers.toInt());
         int shiftedKeyCode = m_KeyCode | Modifiers.sm_SHIFT.toKeyCode();
         keyValue = sm_KeyCodeToValue.get(shiftedKeyCode);
         if (keyValue != null) {
            // succeeded, cancel shift modifier
            modifiers = modifiers.minus(Modifiers.sm_SHIFT);
         }
      }
      if (keyValue == null) {
         keyValue = sm_KeyCodeToValue.get(m_KeyCode);
      }
      if (keyValue == null) {
         //Log.log(String.format("Key 0x%x has no value", m_KeyCode));
         return null;
      }
      if (sm_Unprintable.is(keyValue)) {
         //Log.log(String.format("Key 0x%x value 0x%x is unprintable", m_KeyCode, (int)keyValue));   
         return null;
      }
      return toString("" + keyValue, modifiers);
   }

   ////////////////////////////////////////////////////////////////////////////
   private static String keyCodeToString(int i) {
      String str = sm_KeyCodeToName.getString(i, "");
      if (!"".equals(str)) {
         return sm_BeforeName + str + sm_AfterName;
      }
      return "0x" + Integer.toHexString((char)i);
   }


   ////////////////////////////////////////////////////////////////////////////
   private String toString(String k) {
		return toString(k, m_Modifiers);
   }
	
   ////////////////////////////////////////////////////////////////////////////
   private static String toString(String k, Modifiers m) {
		return m.toLeadTagString() + k + m.toTailTagString();
   }
	
	////////////////////////////////////////////////////////////////////////////
   private static int[] readKeyCodeValues(URL url) {
      int kv[] = new int[513];
      SpacedPairReader spr = new SpacedPairReader(url, Io.sm_MUST_EXIST);
      String keyCode;
      int key = 0;
      for (; (keyCode = spr.getNextFirst()) != null; key += 2) {
         kv[key] = Io.toInt(keyCode);
         if (kv[key] <= 0) {
            Log.err(String.format("Expected a positive integer key code while parsing \"%s\" on line %d of \"%s\".",
                                  keyCode, spr.getLineNumber(), url.getPath()));
         }
         String value = spr.getNextSecond();
         kv[key + 1] = parseKeyValue(value);
         if (kv[key + 1] <= 0) {
            Log.err(String.format("Failed to parse \"%s\" on line %d of \"%s\".",
                                  value, spr.getLineNumber(), url.getPath()));
         }
      }
      spr.close();
      kv[key] = 0;
      return kv;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static int parseKeyValue(String keyValue) {
//System.out.println("kp parseKeyValue \"" + keyValue + "\"");      
      int v = 0;
      if (keyValue.length() > 1 && Character.isDigit(keyValue.charAt(0))) {
         v = Io.toInt(keyValue);
         if (v <= 0) {
            return Io.sm_PARSE_FAILED;
         }
      } else {
         if (keyValue.length() > 2 || (keyValue.length() == 2 && keyValue.charAt(0) != '\\')) {
            return Io.sm_PARSE_FAILED;
         }
         String ch = Io.parseQuote(keyValue);
         if ("".equals(ch)) {
            return Io.sm_PARSE_FAILED;
         }
         v = ch.charAt(0);
      }
      return v;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static final int sm_KEYS = 0xFF;
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
         if (c == sm_BeforeName) {
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
