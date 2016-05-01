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
   public KeyMap(Assignments asgs) {
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
   public Assignments getAssignments() {
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
   public Assignments getAssignmentsReversed() {
      Assignments asgs = new Assignments();
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

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private KeyMap() {
      m_Assignments = new Assignments();
      m_KeyPressIndex = null;
      m_TwiddleIndex = null;
   }

   ////////////////////////////////////////////////////////////////////////////
   private void read(URL url) {
      LineReader lr = new LineReader(url, Io.sm_MUST_EXIST);
      String line;
      StringBuilder err = new StringBuilder();
      for (int i = 1; (line = lr.readLine()) != null; ++i) {
         Assignment asg = Assignment.parseLine(line, err);
         if (asg == null) {
            Log.parseErr(lr, err.toString(), line);
            err = new StringBuilder();
         } else {
//System.out.println("|" + line + "|->|" + asg.toString()+ '|');
            m_Assignments.add(asg);
         }
      }
      lr.close();
      if (m_Assignments.isRemap()) {
         Log.warn(m_Assignments.reportRemap(lr.getPath()));
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private void index() {
      // A = 0x04, NumpadEqual = 0x67
      LookupTableBuilder keyPressLtb = new LookupTableBuilder(0, 0x67);
      keyPressLtb.setMessage(" building the keystroke table.");
      keyPressLtb.setDuplicates(Duplicates.STORE);
      LookupTableBuilder twiddleLtb = new LookupTableBuilder(1, Chord.sm_VALUES);
      twiddleLtb.setMessage(" building the chord table.");
      ArrayList<Twiddle> dup = new ArrayList<Twiddle>();
      for (int i = 0; i < m_Assignments.size(); ++i) {
         Assignment asg = m_Assignments.get(i);
         for (int j = 0; j < asg.getTwiddleCount(); ++j) {
            Twiddle tw = asg.getTwiddle(j);
            if (!twiddleLtb.add(tw.getChord().toInt(), tw.getThumbKeys().toInt(), i)) {
               dup.add(tw);
            }
         }
         KeyPress kp = asg.getKeyPressList().get(0);
         keyPressLtb.add(kp.getKeyCode(), kp.getModifiers().toInt(), i);
      }
      if (dup.size() > 0) {
         String str = "";
         for (int i = 0; i < dup.size(); ++i) {
            str += dup.get(i).toShortString() + ' ';
         }
         Log.warn("<html><tt>" + str + "</tt>mapped more than once.</html>");
      }
      m_KeyPressIndex = keyPressLtb.build();
      m_TwiddleIndex = twiddleLtb.build();
  }

   // Data ////////////////////////////////////////////////////////////////////
   private Assignments m_Assignments;
   private LookupTable m_KeyPressIndex;
   private LookupTable m_TwiddleIndex;

   // Main ////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      Pref.init("twidlit.preferences", "pref", "pref");
      Log.init(new File("twidlit.log"), Log.ExitOnError);
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
