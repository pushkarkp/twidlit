/**
 * Copyright 2015 Pushkar Piggott
 *
 * KeyPress.java
 */

package pkp.twiddle;

import java.util.HashMap;
import java.net.URL;
import pkp.lookup.Lookup;
import pkp.lookup.LookupSet;
import pkp.lookup.LookupBuilder;
import pkp.io.Io;
import pkp.io.SpacedPairReader;
import pkp.string.StringsInts;
import pkp.string.StringsIntsBuilder;
//import pkp.string.SubstitutedString.Substituter;
import pkp.util.Pref;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class KeyPress /*implements Substituter*/ {

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
      HEX("Hexadecimal");

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
      final int Size = 128;
      final String Com = Pref.get("comment", "#");
      final Io.StringToInt charToInt = new Io.StringToInt() {
                                          public int cvt(String str) {
                                             return parseKeyValue(str);
                                          }
                                       };
      final Io.StringToInt parseInt = new Io.StringToInt() {
                                         public int cvt(String str) {
                                            return Io.toInt(str);
                                         }
                                      };
//System.out.println("KeyPress: " + Pref.getDirJarUrl("pref.dir", "TwidlitKeyValues.txt"));
      sm_KeyValueToCode = LookupBuilder.read(Pref.getDirJarUrl("pref.dir", "TwidlitKeyValues.txt"), Com,
                                             LookupBuilder.sm_REVERSE, Io.MustExist,
                                             Size, false, parseInt, charToInt);
//System.out.println("KeyPress: " + Pref.getDirJarUrl("pref.dir", "TwidlitKeyNames.txt").toString());
      sm_KeyCodeToName = (new StringsIntsBuilder(Pref.getDirJarUrl("pref.dir", "TwidlitKeyNames.txt"), true)).build();
//System.out.println("KeyPress: " + Pref.getDirJarUrl("pref.dir", "TwidlitUnprintable.txt").toString());
      sm_Unprintable = LookupBuilder.readSet(Pref.getDirJarUrl("pref.dir", "TwidlitUnprintable.txt"), Com,
                                             Io.MustExist,
                                             Size, parseInt);

      int[] kcv = readKeyCodeValues(Pref.getDirJarUrl("pref.dir", "TwidlitKeyValues.txt"));
      sm_KeyCodeToValue = new HashMap<Integer, Character>(Size);
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
      m_KeyCode = keyCode & sm_KEYS;
      m_Modifiers = modifiers;
      // catch unnamed keys early
      if (toString() == null) {
         //Log.err(String.format("KeyPress: Key code %d (0x%x) has no name or value", getKeyCode(), getKeyCode()));          
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
   public boolean equals(KeyPress kp) {
      return m_KeyCode == kp.m_KeyCode
          && m_Modifiers.equals(kp.m_Modifiers);
   }
/*
   ////////////////////////////////////////////////////////////////////////////
	@Override // Substituter
   public String substitute(String original) {
      return parseText(original.charAt(0), Modifiers.sm_EMPTY).toTagString();
   }
*/
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
         return new String("0x" + Integer.toHexString(m_Modifiers.toKeyCode() | (sm_KEYS & m_KeyCode)));
		}
	   if (format == Format.TAG) {
         return toString(keyCodeToString(m_KeyCode));
		}
      String str = toStringCommon();
      if (str != null) {
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
      keyName = sm_KeyCodeToName.getString(toLookupInt(), "");
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
   public int getKeyCode() { return m_KeyCode; }
   public Modifiers getModifiers() { return m_Modifiers; }
   public boolean isCtrl() { return m_Modifiers.isCtrl(); }
   public boolean isShift() { return m_Modifiers.isShift(); }
   public boolean isAlt() { return m_Modifiers.isAlt(); }
   public boolean isGui() { return m_Modifiers.isGui(); }
   public int toInt() { return m_Modifiers.toKeyCode() + m_KeyCode; }
   public int toLookupInt() { return m_Modifiers.effect().toKeyCode() + m_KeyCode; }

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
      Modifiers modifiers = new Modifiers(m_Modifiers);
      Character keyValue = null;
      if (isShift()) {
         // attempt to convert to shifted value
//System.out.printf("shifting m_KeyCode 0x%x m_Modifiers 0x%x\n", m_KeyCode, m_Modifiers.toInt());
         int keyCode = m_KeyCode | Modifiers.sm_SHIFT.toKeyCode();
         keyValue = sm_KeyCodeToValue.get(keyCode);
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
      SpacedPairReader spr = new SpacedPairReader(url, Pref.get("comment", "#"), true);
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
            return Io.ParseFailed;
         }
      } else {
         if (keyValue.length() > 2 || (keyValue.length() == 2 && keyValue.charAt(0) != '\\')) {
            return Io.ParseFailed;
         }
         String ch = Io.parseChars(keyValue);
         if ("".equals(ch)) {
            return Io.ParseFailed;
         }
         v = ch.charAt(0);
      }
      return v;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static final int sm_KEYS = 0xFF;
   private static Lookup sm_KeyEventToCode;
   private static HashMap<Integer, Character> sm_KeyCodeToValue;
   private static StringsInts sm_KeyCodeToName;
   private static Lookup sm_KeyValueToCode;
   private static LookupSet sm_Unprintable;
   private static boolean sm_Warned;
   private int m_KeyCode;
   private Modifiers m_Modifiers;

   ////////////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      if (args.length == 0) {
         System.out.println("add a string");
         return;
      }
      Pref.init("TwidlitPreferences.txt", "pref", "pref");
      KeyPress.init();
      for (int i = 0; i < args.length; ++i) {
			char c = args[i].charAt(0);
         if (c == sm_BeforeName) {
            KeyPress act = KeyPress.parseTag(args[i].substring(1, args[i].length() - 1), Modifiers.sm_EMPTY);
            System.out.printf("'%s': '%s' '%s'\n", args[i], act.toString(), act.toTagString());
         } else if (c == '-') {
            switch (args[i].charAt(1)) {
            case 'c': System.out.print(sm_KeyCodeToValue.toString()); break;
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
