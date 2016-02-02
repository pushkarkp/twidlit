/**
 * Copyright 2015 Pushkar Piggott
 *
 * KeyPressList.java
 */

package pkp.twiddle;

import java.util.ArrayList;
import java.util.StringTokenizer;
import pkp.util.StringWithOffset;
import pkp.util.Pref;
import pkp.util.Log;
import pkp.io.Io;

///////////////////////////////////////////////////////////////////////////////
public class KeyPressList extends java.lang.Object {

   ////////////////////////////////////////////////////////////////////////////
   public static KeyPressList parseText(String str) {
      KeyPressList kpl = new KeyPressList();
      for (int i = 0; i < str.length(); ++i) {
         KeyPress kp = KeyPress.parseText(str.charAt(i), Modifiers.sm_EMPTY);
         if (!kp.isValid()) {
            return new KeyPressList();
         }
         kpl.append(kp, str);
      }
      return kpl;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static KeyPressList parseTextAndTags(String str) {
//System.out.printf("parseTextAndTags() |%s| [%c] \\x%x%n", str, str.charAt(0),  (int)str.charAt(0));
      KeyPressList kpl = new KeyPressList();
      Modifiers tagMod = Modifiers.sm_EMPTY;
		for (int i = 0; i < str.length(); ++i) {
			KeyPress kp = null;
         char c = str.charAt(i);
         if (c != '<') {
//System.out.printf("parseTextAndTags1 [%d] |%c| (%d) tagMod 0x%x%n", i, c, (int)c, tagMod.toInt());
            StringWithOffset swo = new StringWithOffset(str, i);
            kp = KeyPress.parse(swo, tagMod);
            // for ++i will re-add 1
            i = swo.getOffset() - 1;
         } else {
            String rest = str.substring(i + 1);
            int end = rest.indexOf('>');
            // accept unescaped < if at EOL
            if (end < 0 || rest.substring(0, end).indexOf('<') >= 0) {
//System.out.printf("parseTextAndTags3 [%d] |%c| (%d) tagMod 0x%x%n", i, c, (int)c, tagMod.toInt());
               kp = KeyPress.parseText(c, tagMod);
            } else {
//System.out.printf("parseTextAndTags4 [%d] |%s| tagMod \\x%x%n", i, rest.substring(0, end), tagMod.toInt());
               kp = KeyPress.parseTag(rest.substring(0, end), tagMod);
               if (kp.isModifiers()) {
                  tagMod = kp.getModifiers();
               }
					i += end + 1;
					if (kp.getKeyCode() == 0) {
						// this is OK, closed all modifiers
						continue;
					}
				}
			}
			if (!kp.isValid()) {
            Log.log(String.format("Failed to find keypress for \"%c\" (%d) in \"%s\"", c, (int)c, str));
            return new KeyPressList();
         }
//System.out.printf("parseTextAndTags5 add: keycode 0x%x mod 0x%x%n", kp.getKeyCode(), kp.getModifiers().toInt());
         kpl.append(kp, str);
      }
//System.out.println("parseTextAndTags6 " + kpl);
      return kpl;
   }

   ////////////////////////////////////////////////////////////////////////////
   public KeyPressList() {
      m_List = new ArrayList<KeyPress>();
   }

   ////////////////////////////////////////////////////////////////////////////
   public KeyPressList(KeyPress kp) {
      this();
		add(kp);
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean equals(KeyPressList rhs) {
//System.out.printf("%d == %d%n", size(), rhs.size());
      int size = size();
      if (size != rhs.size()) {
         return false;
      }
//System.out.printf("%s =kpl= %s%n", toString(KeyPress.Format.HEX), rhs.toString(KeyPress.Format.HEX));
      for (int i = 0; i < size; ++i) {
         if (!get(i).equals(rhs.get(i))) {
            return false;
         }
      }
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignment findLongestPrefix(KeyMap map) {
//System.out.println("findLongestPrefix: \"" + toString() + "\"");
      if (!isValid()) {
         Log.err("KeyPressList is empty");
         return null;
      }
      // lookup as-is first
      Assignment asg = map.findLongestPrefix(this);
      if (asg == null && !get(0).getModifiers().isEmpty()) {
         // try stripping off modifiers
         final Modifiers mods[] = Modifiers.getCombinations(get(0).getModifiers());
//System.out.printf("findLongestPrefix: button mods: %s (%s)%n", get(0).getModifiers(), Modifiers.toString(mods));
         for (int i = 0; i < mods.length; ++i) {
            KeyPressList kpl = getPrefixMinusModifiers(mods[i]);
//System.out.printf("findLongestPrefix: button mods[i] 0x%x kpl %s%n", mods[i].toInt(), kpl.toString());
            asg = map.findLongestPrefix(kpl);
            if (asg != null) {
//System.out.println("findLongestPrefix: asg " + asg);
//System.out.printf("findLongestPrefix: mod 0x%x%n", mods[i].toInt());
               return new Assignment(asg, mods[i]);
            }
         }
      }
      return asg;
   }

   ////////////////////////////////////////////////////////////////////////////
   public KeyPressList createModified(Modifiers mod) {
      KeyPressList kpl = new KeyPressList();
     for (int i = 0; i < size(); ++i) {
        KeyPress kp = new KeyPress(get(i).getKeyCode(), get(i).getModifiers().plus(mod));
        kpl.add(kp);
      }
      return kpl;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      return toString(KeyPress.Format.HEX);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(KeyPress.Format format) {
      if (size() == 0) {
         return "empty";
      }
      String str = "";
      for (KeyPress kp: m_List) {
         str += kp.toString(format);
      }
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean startsWith(KeyPressList kpl) {
      if (size() < kpl.size()) {
         return false;
      }
      for (int i = 0; i < kpl.size(); ++i) {
//System.out.printf("get(i) %s kpl.get(i) %s\n", get(i).toString(), kpl.get(i).toString());
         if (!get(i).equals(kpl.get(i))) {
            return false;
         }
      }
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   public KeyPressList sublist(int start) {
      KeyPressList kpl = new KeyPressList();
      for (int i = start; i < size(); ++i) {
         kpl.add(get(i));
      }
      return kpl;
   }

   ////////////////////////////////////////////////////////////////////////////
   public KeyPressList set(int i, KeyPress kp) {
      m_List.set(i, kp);
      return this;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isValid() { return size() > 0 && get(size() - 1).isValid(); }
   public int size() { return m_List.size(); }
   public KeyPress get(int i) { return m_List.get(i); }
   public KeyPressList add(KeyPress kp) { m_List.add(kp); return this; }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private Modifiers parseQuote(String str, Modifiers mod) {
//System.out.println("parseQuote " + str);
      String orig = str;
      int end = 0;
      for (;
           !"".equals(str);
           str = str.substring(end + 1)) {
         KeyPress kp = null;
         Character ch = str.charAt(0);
         if (ch == '<' && (end = str.indexOf('>')) != -1) {
            kp = KeyPress.parseTag(str.substring(1, end), mod);
            if (kp.isModifiers()) {
               mod = kp.getModifiers();
            }
         } else {
            kp = KeyPress.parseText(ch, mod);
            end = 0;
         }
         if (!kp.isValid()) {
            return Modifiers.sm_EMPTY;
         }
         append(kp, orig);
      }
      return mod;
   }

   ////////////////////////////////////////////////////////////////////////////
   private KeyPressList getPrefixMinusModifiers(Modifiers mod) {
//System.out.printf("kpl getPrefixMinusModifiers: mods 0x%x kpl %s (%d)%n", mod.toInt(), toString(), size());
      KeyPressList kpl = new KeyPressList();
      for (int i = 0; i < size(); ++i) {
         if (mod.isSubsetOf(get(i).getModifiers())) {
            kpl.add(new KeyPress(get(i).getKeyCode(), get(i).getModifiers().minus(mod)));
         } else {
           break;
         }
      }
      return kpl;
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean append(KeyPress kp, String str) {
//System.out.println("append: |" + kp.toString() + "| str \"" + str + "\"");
      if (!kp.isValid()) {
         m_List.clear();
         Log.log(String.format("KeyPressList failed to parse: %s", str));
         return false;
      }
      if (kp.getKeyCode() != 0) {
         m_List.add(kp);
      }
      return true;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private ArrayList<KeyPress> m_List;

   // Main ////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      if (args.length == 0) {
         System.out.println("usage '<action list>'...'");
         return;
      }
      Pref.init("TwidlitPreferences.txt", "pref", "pref");
      KeyPress.init();
      for (String arg: args) {
         KeyPressList kpl = KeyPressList.parseTextAndTags(arg);
         System.out.println(kpl.toString());
      }
   }
}
