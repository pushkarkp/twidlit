/**
 * Copyright 2015 Pushkar Piggott
 *
 * KeyPressList.java
 */

package pkp.twiddle;

import java.util.ArrayList;
import java.util.StringTokenizer;
import pkp.util.Pref;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class KeyPressList extends java.lang.Object {

   ////////////////////////////////////////////////////////////////////////////
   // a comma separated list of ints, tags and quoted text
   // do we really want to support all this?
   public static KeyPressList parseLine(String in) {
//System.out.println("parseLine " + in);
      KeyPressList kpl = new KeyPressList();
      int start = (in.charAt(0) == '"') ? 0 : 1;
      StringTokenizer qTok = new StringTokenizer(in.trim(), "\"");
      if (!qTok.hasMoreElements()) {
//System.out.println("!qTok.hasMoreElements() " + in);
         if (in.length() != 1 || in.charAt(0) != '"'
          || !kpl.append(KeyPress.parseText(in.charAt(0), Modifiers.sm_EMPTY), in)) {
            return new KeyPressList();
         }
      }
      Modifiers modifiers = Modifiers.sm_EMPTY;
      for (int i = 0; qTok.hasMoreElements(); ++i) {
         String qStr = qTok.nextToken();
         if (((i - start) & 1) == 0) {
//System.out.println("((i - start) & 1) == 0 |" + in + "|" + qStr);
            modifiers = kpl.parseQuote(qStr, modifiers);
            if (!kpl.isValid()) {
               return new KeyPressList();
            }
         } else {
//System.out.println("else |" + in + "|" + qStr);
            qStr = qStr.trim();
            boolean first = (i == 0);
            boolean last = !qTok.hasMoreElements();
            int end = qStr.indexOf(Pref.get("comment", "#").charAt(0));
            if (end != -1) {
               qStr = qStr.substring(0, end).trim();
               if (qStr.length() == 0) {
                  return new KeyPressList();
               }
               last = true;
            }
            if ((!first && qStr.charAt(0) != ',')
             || (!last && qStr.charAt(qStr.length() - 1) != ',')) {
               System.err.println("missing comma");
               kpl.clear();
               return new KeyPressList();
            }
            StringTokenizer cTok = new StringTokenizer(qStr, ",");
            for (int j = 0; cTok.hasMoreElements(); ++j) {
               String cStr = cTok.nextToken().trim();
               KeyPress kp = KeyPress.parseTag(cStr, modifiers);
               if (kp.isModifiers()) {
                  modifiers = kp.getModifiers();
               }
               if (!kpl.append(kp, cStr)) {
                  return new KeyPressList();
               }
            }
         }
      }
      return kpl;
   }

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
//System.out.printf("parseTextAndTags |%s| [%c] 0x%x\n", str, str.charAt(0),  (int)str.charAt(0));
      KeyPressList kpl = new KeyPressList();
      Modifiers tagMod = Modifiers.sm_EMPTY;
		for (int i = 0; i < str.length(); ++i) {
			KeyPress kp = null;
         char c = str.charAt(i);
         if (c != KeyPress.sm_BeforeName) {
//System.out.printf("parseTextAndTags1[%d] |%c| (%d) tagMod 0x%x\n", i, c, (int)c, tagMod.toInt());
				kp = KeyPress.parseText(c, tagMod);
			} else {
            String rest = str.substring(i + 1);
            int end = rest.indexOf(KeyPress.sm_AfterName);
            if (end < 0 || rest.substring(0, end).indexOf(KeyPress.sm_BeforeName) >= 0) {
//System.out.printf("parseTextAndTags2[%d] |%c| (%d) tagMod 0x%x\n", i, c, (int)c, tagMod.toInt());
					kp = KeyPress.parseText(c, tagMod);
            } else {
//System.out.printf("parseTextAndTags3[%d] |%s| tagMod 0x%x\n", i, rest.substring(0, end), tagMod.toInt());
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
//System.out.printf("parseTextAndTags add: keycode 0x%x mod 0x%x\n", kp.getKeyCode(), kp.getModifiers().toInt());
		   kpl.append(kp, str);
      }
//System.out.println(kpl);
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
      int size = size();
      if (size != rhs.size()) {
         return false;
      }
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
         Log.warn("KeyPressList is empty");
         return null;
      }
      final Modifiers mods[] = Modifiers.getCombinations(get(0).getModifiers());
//System.out.println("mods: " + get(0).getModifiers()); 
      for (int i = 0; i < mods.length; ++i) {
         KeyPressList kpl = getPrefixMinusModifiers(mods[i]);
         if (mods[i].equals(get(0).getModifiers())) {
//            System.out.println("findLongestPrefix: no mods: " + kpl);
         }         
//System.out.println(kpl); 
         Assignment asg = map.findLongestPrefix(kpl);
         if (asg != null) {
            return new Assignment(asg.getTwiddle().createModified(mods[i]), asg.getKeyPressList().createModified(mods[i]));
         }
      }
      Log.warn("\"" + toString() + "\" has no matching twiddle.");
      return null;
   }

   ////////////////////////////////////////////////////////////////////////////
   private KeyPressList getPrefixMinusModifiers(Modifiers mod) {
      KeyPressList kpl = new KeyPressList();
      for (int i = 0; i < size(); ++i) {
         if (get(i).getModifiers().isSubsetOf(mod)) {
            kpl.add(new KeyPress(get(i).getKeyCode(), get(i).getModifiers().minus(mod)));
         } else {
           break;
         }
      }
      return kpl;
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
      return toString(KeyPress.Format.TEXT_);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(KeyPress.Format format) {
      if (m_List.size() == 0) {
         return "empty";
      }
      String str = "";
      String sep = "";
      for (KeyPress kp: m_List) {
			if (format == KeyPress.Format.HEX) {
            str += sep + kp.toString(KeyPress.Format.HEX);
			   sep = " ";
			} else {
            str += kp.toString(format);
			}
      }
      return str;
   }
/*
      if (!m_Modifiers.equals(prevModifiers)) {
         str = prevModifiers.minus(m_Modifiers).toTailTagString()
             + m_Modifiers.minus(prevModifiers).toLeadTagString();
      }
*/
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
   public boolean isValid() { return size() > 0 && get(size() - 1).isValid(); }
   public int size() { return m_List.size(); }
   public KeyPress get(int i) { return m_List.get(i); }
   public void add(KeyPress kp) { m_List.add(kp); }

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
         if (ch == KeyPress.sm_BeforeName && (end = str.indexOf(KeyPress.sm_AfterName)) != -1) {
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
   private void clear() {
      m_List.clear();
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
