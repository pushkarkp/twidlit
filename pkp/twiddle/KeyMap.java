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
import pkp.lookup.Lookup;
import pkp.lookup.LookupBuilder;
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
   // notbotehring to make copies
   public ArrayList<Assignment> getAssignmentsReversed() {
      ArrayList<Assignment> asgs = new ArrayList<Assignment>();
      for (int i = 0; i < m_Assignments.size(); ++i) {
         Assignment asg = m_Assignments.get(i);
         Chord rev = asg.getTwiddle().getChord().reversed();
         Twiddle tw = new Twiddle(rev, asg.getTwiddle().getThumbKeys());
         asgs.add(new Assignment(tw, asg.getKeyPressList()));
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignment findLongestPrefix(KeyPressList kpl) {
      if (kpl.size() == 0) {
         return null;
      }
      KeyPress kp0 = kpl.get(0);
      int asgIndex[] = m_KeyPressIndex.getAll(kp0.getKeyCode(), kp0.getModifiers().effect().toInt());
      if (asgIndex == null) {
         return null;
	  }
      int maxI = -1;
      int maxAl = 0;
      for (int i = 1; i < asgIndex[0]; ++i) {
         KeyPressList found = m_Assignments.get(asgIndex[i]).getKeyPressList();
         if (kpl.startsWith(found) && found.size() > maxAl) {
            maxAl = found.size();
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
      if (i < 0) {
         return null;
      }
      // totally explicitly defined
      return m_Assignments.get(i).getKeyPressList();
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      String str = "";
      for (int i = 0; i < m_Assignments.size(); ++i) {
         str += Integer.toString(i) + " " + m_Assignments.get(i).toString() + "\n";
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
      LineReader lr = new LineReader(url, Pref.get("comment", "#"), true);
      String line;
      for (int i = 1; (line = lr.readLine()) != null; ++i) {
         Assignment asg = Assignment.parseLine(line);
         if (asg == null) {
            Log.log(String.format("Failed to add line %d \"%s\" of \"%s\"", i, line, url.getPath()));
         } else {
//System.out.println(asg.toString());
            m_Assignments.add(asg);
         }
      }
      lr.close();
   }

   ////////////////////////////////////////////////////////////////////////////
   private void index() {
      LookupBuilder keyPressLb = new LookupBuilder(145, true);
      LookupBuilder twiddleLb = new LookupBuilder(Chord.sm_VALUES + 1, false);
      for (int i = 0; i < m_Assignments.size(); ++i) {
         Assignment asg = m_Assignments.get(i);
         twiddleLb.add(asg.getTwiddle().getChord().toInt(), asg.getTwiddle().getThumbKeys().toInt(), i);
         KeyPress kp = asg.getKeyPressList().get(0);
         keyPressLb.add(kp.getKeyCode(), kp.getModifiers().effect().toInt(), i);
      }
      m_KeyPressIndex = keyPressLb.buildLookup();
      m_TwiddleIndex = twiddleLb.buildLookup();
//System.out.println(m_TwiddleIndex.toString());
  }

   // Data ////////////////////////////////////////////////////////////////////
   private ArrayList<Assignment> m_Assignments;
   private Lookup m_KeyPressIndex;
   private Lookup m_TwiddleIndex;

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
         case "-a":
            System.out.printf("%d\n", map.findLongestPrefix(KeyPressList.parseLine(args[i + 1])).getKeyPressList().size());
            break;
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
