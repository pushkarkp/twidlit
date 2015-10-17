/**
 * Copyright 2015 Pushkar Piggott
 *
 * KeyMap.java
 */

package pkp.twiddle;

import java.util.ArrayList;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;
import pkp.lookup.LookupTable;
import pkp.lookup.LookupTableBuilder;
import pkp.lookup.LookupBuilder.Duplicates;
import pkp.io.LineReader;
import pkp.io.Io;
import pkp.util.Pref;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class KeyMap {

   ////////////////////////////////////////////////////////////////////////////
   public KeyMap(ArrayList<Assignment> asgs) {
      this();
      m_Assignments = asgs;
      index();
  }

   ////////////////////////////////////////////////////////////////////////////
   public KeyMap(URL url) {
      this();
      read(url);
      index();
  }

   ////////////////////////////////////////////////////////////////////////////
   public ArrayList<Assignment> getAssignments() {
      return m_Assignments;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignment findLongestPrefix(KeyPressList kpl) {
      if (kpl.size() == 0) {
         return null;
      }
      KeyPress kp0 = kpl.get(0);
      int asgIndex[] = m_KeyPressIndex.getAll(kp0.getKeyCode(), kp0.getModifiers().toInt());
      if (asgIndex == null) {
         return null;
      } 
      // only one assignment can match a given prefix
      int maxI = -1;
      int maxLen = 0;
      for (int i = 1; i < asgIndex[0]; ++i) {
         KeyPressList found = m_Assignments.get(asgIndex[i]).getKeyPressList();
//System.out.println("keymap findLongestPrefix kpl found " + found.toString());
         if (kpl.startsWith(found) && found.size() > maxLen) {
            maxLen = found.size();
            maxI = i;
         }
      }
      if (maxI < 0) {
         return null;
      }
      return m_Assignments.get(asgIndex[maxI]);
   }

   ////////////////////////////////////////////////////////////////////////////
   public KeyPressList getKeyPressList(Twiddle tw) {
      int i = m_TwiddleIndex.get(tw.getChord().toInt(), tw.getThumbKeys().toInt());
      if (i == LookupTable.sm_NO_VALUE) {
         return null;
      }
      // totally explicitly defined
      return m_Assignments.get(i).getKeyPressList();
   }

   ////////////////////////////////////////////////////////////////////////////
   public ArrayList<Assignment> getAssignmentsReversed() {
      ArrayList<Assignment> asgs = new ArrayList<Assignment>();
      for (int i = 0; i < m_Assignments.size(); ++i) {
         Assignment asg = m_Assignments.get(i);
         Assignment newAsg = m_Assignments.get(i);
         ArrayList<Twiddle> tw = new ArrayList<Twiddle>();
         for (int j = 0; j < asg.getTwiddleCount(); ++j) {
            Twiddle t = asg.getTwiddle(j);
            Chord rev = t.getChord().reversed();
            tw.add(new Twiddle(rev, t.getThumbKeys()));
         }
         asgs.add(new Assignment(tw, asg.getKeyPressList()));
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      String str = "";
      for (int i = 0; i < m_Assignments.size(); ++i) {
         str += m_Assignments.get(i).toString() + "\n";
      }
      return str;
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private KeyMap() {
      m_Assignments = new ArrayList<Assignment>();
      m_KeyPressIndex = null;
      m_TwiddleIndex = null;
   }

   ////////////////////////////////////////////////////////////////////////////
   private void read(URL url) {
      LineReader lr = new LineReader(url, Io.sm_MUST_EXIST);
      String line;
      for (int i = 1; (line = lr.readLine()) != null; ++i) {
         Assignment asg = Assignment.parseLine(line);
         if (asg == null) {
            Log.log(String.format("Failed to add line %d \"%s\" of \"%s\"", i, line, url.getPath()));
         } else {
//System.out.println(asg.toString());
            add(asg);
         }
      }
      lr.close();
   }


   ////////////////////////////////////////////////////////////////////////////
   private void add(Assignment asg) {
      for (int i = m_Assignments.size() - 1; i >= 0; --i) {
         if (asg.hasSameKeys(m_Assignments.get(i))) {
            m_Assignments.set(i, new Assignment(asg, m_Assignments.get(i)));
            return;
         }
      }
      m_Assignments.add(asg);
   }

   ////////////////////////////////////////////////////////////////////////////
   private void index() {
      // A = 0x04, NumpadEqual = 0x67
      LookupTableBuilder keyPressLtb = new LookupTableBuilder(0, 0x67);
      keyPressLtb.setDuplicates(Duplicates.STORE);
      LookupTableBuilder twiddleLtb = new LookupTableBuilder(1, Chord.sm_VALUES);
      for (int i = 0; i < m_Assignments.size(); ++i) {
         Assignment asg = m_Assignments.get(i);
         for (int j = 0; j < asg.getTwiddleCount(); ++j) {
            Twiddle tw = asg.getTwiddle(j);
            twiddleLtb.add(tw.getChord().toInt(), tw.getThumbKeys().toInt(), i);
         }
         KeyPress kp = asg.getKeyPressList().get(0);
         keyPressLtb.add(kp.getKeyCode(), kp.getModifiers().toInt(), i);
      }
      m_KeyPressIndex = keyPressLtb.build();
      m_TwiddleIndex = twiddleLtb.build();
//System.out.println(m_KeyPressIndex.toString());
//System.out.println(m_TwiddleIndex.toString());
  }

   // Data ////////////////////////////////////////////////////////////////////
   private ArrayList<Assignment> m_Assignments;
   private LookupTable m_KeyPressIndex;
   private LookupTable m_TwiddleIndex;

   // Main ////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      Pref.init("TwidlitPreferences.txt", "pref", "pref");
      Log.init(new File("TwidlitLog.txt"), Log.ExitOnError);
      KeyPress.init();
      File f = new File("pref", "keymap.txt");
      URL url = null;
      try {
         url = f.toURI().toURL();
      } catch (MalformedURLException e) {
         Log.err("Failed to create URL from \"" + f.getPath() + "\".");
      }
      KeyMap map = new KeyMap(url);
//System.out.print(map.toString());
      for (int i = 0; i < args.length; ++i) {
         switch (args[i]) {
         case "-t":
            Twiddle t = new Twiddle(args[i + 1]);
            System.out.print(t.toString() + " ");
            KeyPressList kpl = map.getKeyPressList(t);
            if (kpl == null) {
               System.out.println("not assigned");
            } else {
               System.out.println(kpl.toString());
            }
            break;
         }
      }
   }
}
