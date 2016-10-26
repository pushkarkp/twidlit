/**
 * Copyright 2015 Pushkar Piggott
 *
 * Assignments.java
 */

package pkp.twiddle;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import pkp.times.SortedChordTimes;
import pkp.io.LineReader;
import pkp.io.Io;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class Assignments extends ArrayList<Assignment> {

   ////////////////////////////////////////////////////////////////////////////
   public static Assignments listAll() {
      Assignments asgs = new Assignments();
      for (int i = 1; i <= Chord.sm_VALUES; ++i) {
         Twiddle tw = new Twiddle(i, 0);
         asgs.add(new Assignment(tw, KeyPressList.parseText(tw.getChord().toString() + " ")));
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Assignments listAllNamedByFingerCount() {
      Assignments asgs = new Assignments();
      for (int fingers = 1; fingers <= 4; ++fingers) {
         for (int i = 1; i <= Chord.sm_VALUES; ++i) {
            if (Chord.countFingers(i) == fingers) {
               Twiddle tw = new Twiddle(i, 0);
               asgs.add(new Assignment(tw, KeyPressList.parseText(tw.getChord().toString() + " ")));
            }
         }
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Assignments listAllByFingerCount() {
      final int MIN_CODE = 4;
      final int MAX_CODE = 0x52;
      final int MODE_INC = 0x200;
      int code = MIN_CODE;
      int mod = 0;
      Modifiers modifiers = Modifiers.fromKeyCode(mod);
      Assignments asgs = new Assignments();
      for (int fingers = 1; fingers <= 4; ++fingers) {
         for (int i = 1; i <= Chord.sm_VALUES; ++i) {
            if (Chord.countFingers(i) == fingers) {
               Twiddle tw = new Twiddle(i, 0);
               KeyPress kp;
               do {
                  kp = new KeyPress(code, modifiers);
                  ++code;
                  // omit keys with keyboard state etc
                  while (code == 0x39 // CapsLock
                      || code == 0x32 // IntlHash
                      || code == 0x46 // PrintScreen
                      || code == 0x47 // ScrollLock
                      || code == 0x48 // Pause
                      || code == 0x49 // Insert
                      ) {
                     ++code;
                  }
                  if (code > MAX_CODE) {
                     code = MIN_CODE;
                     mod += MODE_INC;
                     modifiers = Modifiers.fromKeyCode(mod);
                  }
               } while (!kp.isValid() || kp.isDuplicate() || kp.isLost());
               asgs.add(new Assignment(tw, new KeyPressList(kp)));
            }
         }
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignments() {
      super();
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignments(Assignments asgs) {
      super(asgs);
   }

   ////////////////////////////////////////////////////////////////////////////
   public Assignments(File f) {
      StringBuilder err = new StringBuilder();
      LineReader lr = new LineReader(Io.toExistUrl(f));
      for (;;) {
         String line = lr.readLine();
         if (line == null) {
            break;
         }
         add(Assignment.parseLine(line, err));
         if (!"".equals(err.toString())) {
            Log.parseWarn(lr, err.toString(), line);
            err = new StringBuilder();
         }
      }
      lr.close();
   }

   ////////////////////////////////////////////////////////////////////////////
   // An assignment maps one OR MORE chords to a keypress list.
   // Return a list of 1 to 1 mappings.
   public List<Assignment> toList() {
      ArrayList<Assignment> asgs = new ArrayList<Assignment>();
      for (Assignment a : this) {
         asgs.addAll(a.separate());
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      return toString(KeyPress.Format.FILE, false, false, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(KeyPress.Format format, boolean shortStr) {
      return toString(format, shortStr, false, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(KeyPress.Format format, boolean shortStr, boolean showAll, SortedChordTimes times) {
      if (shortStr) {
         for (Assignment asg: this) {
            if (asg.isThumbed()) {
               shortStr = false;
               break;
            }
         }
      }
      ArrayList<Assignment> asgs = new ArrayList<Assignment>();
      for (Assignment asg: this) {
         asgs.addAll(asg.separate());
		}
      if (showAll) {
         asgs = addUnmapped(asgs);
		}
      if (times != null) {
         asgs = sort(asgs, times);
		}
      String str = "";
      for (Assignment asg: asgs) {
         str += asg.toString(format, shortStr, "\n") + '\n';
		}
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isMap(Twiddle tw) {
      for (Assignment asg: this) {
			if (asg.isMap(tw)) {
            return true;
         }
		}
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isUse(Chord ch) {
      for (Assignment asg: this) {
         if (asg.isUse(ch)) {
            return true;
         }
		}
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override
   public boolean add(Assignment newAsg) {
      if (isMap(newAsg.getTwiddle())) {
         m_Remap.add(newAsg);
         return false;
      }
      KeyPressList kpl = newAsg.getKeyPressList();
      for (int i = 0; i < size(); ++i) {
         if (newAsg.getKeyPressList().equals(get(i).getKeyPressList())) {
            set(i, Assignment.combine(get(i), newAsg));
            return true;
         }
		}
		super.add(newAsg);
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isRemap() {
      return m_Remap.size() > 0;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String reportRemap(String path) {
      if (m_Remap.size() == 0) {
         return "";
      }
      final int limit = 8;
      int last = m_Remap.size() == limit + 1
               ? limit + 1      
               : Math.min(limit, m_Remap.size());
      String str = "<tt>";
      String sep = "";
      for (int i = 0; i < last; ++i) {
         str += sep + m_Remap.get(i).toString().replace("<", "&lt;");
         sep = "<br>";
      }
      str += "</tt>";
      if (last < m_Remap.size()) {
         str += sep + String.format("and %d more.", m_Remap.size() - last);
      }
      String plural = m_Remap.size() > 1 
                    ? "s of mapped chords"
                    : " of a mapped chord";
      path = "".equals(path)
           ? ""
           : " in " + path;
      String msg = "<html>Ignored the following remapping" 
                 + plural + path + ":<br>" + str + "</html>";
      m_Remap = new ArrayList<Assignment>();
      return msg;
   }

   // Private /////////////////////////////////////////////////////////////////

   ///////////////////////////////////////////////////////////////////////////////
   private static ArrayList<Assignment> addUnmapped(ArrayList<Assignment> asgs) {
      ArrayList<Assignment> all = new ArrayList<Assignment>(asgs);
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         int chord = i + 1;
         int a = 0;
         for (; a < asgs.size(); ++a) {
            Twiddle tw = asgs.get(a).getTwiddle();
            if (tw.getThumbKeys().isEmpty() && tw.getChord().toInt() == chord) {
               break;
            }
         }
         if (a == asgs.size()) {
            all.add(new Assignment(new Twiddle(chord), new KeyPressList()));
         }
      }
      return all;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static ArrayList<Assignment> sort(ArrayList<Assignment> asgs, SortedChordTimes times) {
      ArrayList<Assignment> sorted = new ArrayList<Assignment>();
      ArrayList<Assignment> sortedThumbed = new ArrayList<Assignment>();
      for (int i = 0; i < times.getSize(); ++i) {
         String label = times.getSortedLabel(i);
         int chord = Chord.fromString(label);
         if (chord == 0) {
            Log.err("Badly formed chord \"" + times.getSortedLabel(i) + "\".");
            continue;
         }
         for (int a = 0; a < asgs.size(); ++a) {
            Twiddle tw = asgs.get(a).getTwiddle();
            if (tw.getChord().toInt() == chord) {
               if (tw.getThumbKeys().isEmpty()) {
                  sorted.add(asgs.get(a));
               } else {
                  sortedThumbed.add(asgs.get(a));
               }
            }
         }
      }
      sorted.addAll(sortedThumbed);
      return sorted;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private ArrayList<Assignment> m_Remap = new ArrayList<Assignment>();
}
