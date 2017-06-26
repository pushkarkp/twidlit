/**
 * Copyright 2015 Pushkar Piggott
 *
 * Assignments.java
 *
 * An assignment maps each KeyPressList to all its twiddles.
 * toSortedMouseButtons() returns a list of all 3 mouse twiddles,
 * each with their KeyPressList, if any.
 * to121ChordList() returns a list of all defined chord twiddles
 * in order, each with their KeyPressList, if any.
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
         Assignment a = Assignment.parseLine(line, err);
         if (a != null && a.getTwiddle(0).isValid() && a.getKeyPressList().isValid()) {
            add(a);
         }
         if (!"".equals(err.toString())) {
            Log.parseWarn(lr, err.toString(), line);
            err = new StringBuilder();
         }
      }
      lr.close();
   }

   ////////////////////////////////////////////////////////////////////////////
   // An assignment maps one KeyPressList to all its twiddles.
   // This returns a list of all defined chord twiddles each,
   // with their KeyPressList.
   public List<Assignment> to121ChordList() {
      List<Assignment> asgs = new ArrayList<Assignment>();
      for (Assignment asg : this) {
         List<Assignment> sep = asg.separate();
         for (Assignment a : sep) {
            if (!a.getTwiddle(0).getChord().isMouseButton()) {
               asgs.add(a);
            }
         }
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   // Returns a list of all 3 mouse twiddles,
   // in order, each with their KeyPressList, if any.
   public List<Assignment> toSortedMouseButtons() {
      List<Assignment> asgs = new ArrayList<Assignment>(3);
      for (int i = 0; i < 3; ++i) {
         asgs.add(new Assignment());
      }
      List<Assignment> mbs = toMouseButtons();
      for (Assignment a : mbs) {
         if (!a.isDefaultMouse()) {
            asgs.set(a.getTwiddle(0).getChord().getMouseButton() - 1, 
                     a);
         }
      }
      return asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public List<Assignment> toMouseButtons() {
      List<Assignment> mbs = new ArrayList<Assignment>(3);
      for (Assignment asg : this) {
         List<Assignment> sep = asg.separate();
         for (Assignment a : sep) {
            if (a.getTwiddle(0).getChord().isMouseButton()
             && !a.isDefaultMouse()) {
               mbs.add(a);
            }
         }
      }
      if (mbs.size() > 3) {
         Log.warn("Found more than 3 mouse button mappings.");
      }
      return mbs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      return toString(Assignment.sm_SHOW_THUMB_KEYS, KeyPress.Format.FILE, false, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(boolean showThumbs, KeyPress.Format format) {
      return toString(showThumbs, format, false, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString(boolean showThumbs, KeyPress.Format format, boolean showAll, SortedChordTimes times) {
      if (!showThumbs) {
         for (Assignment asg: this) {
            if (asg.isThumbed()) {
               showThumbs = true;
               break;
            }
         }
      }
      List<Assignment> asgs = toMouseButtons();
      List<Assignment> c = to121ChordList();
      for (Assignment a : c) {
         asgs.add(a);
      }
      if (showAll) {
         asgs.addAll(getUnmapped(asgs));
      }
      if (times != null) {
         asgs = sort(asgs, times);
      }
      String str = "";
      for (Assignment asg: asgs) {
         str += asg.toString(showThumbs, format, "\n") + "\n";
      }
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   public int find(Twiddle tw) {
      for (int i = 0; i < size(); ++i) {
         if (get(i).isMap(tw)) {
            return i;
         }
      }
      return -1;
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isMap(Twiddle tw) {
      return find(tw) != -1;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override
   public boolean add(Assignment newAsg) {
      if (newAsg == null) {
         return false;
      }
      if (newAsg.getTwiddleCount() > 1) {
         Log.err("Adding multi-chord assignment.");
      }
      if (isMap(newAsg.getTwiddle(0))) {
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
   private static List<Assignment> getUnmapped(List<Assignment> asgs) {
      List<Assignment> unmapped = new ArrayList<Assignment>();
      for (int chord = 1; chord <= Chord.sm_VALUES; ++chord) {
         int a = 0;
         for (; a < asgs.size(); ++a) {
            // only one twiddle per assignment in 121 list
            Twiddle tw = asgs.get(a).getTwiddle(0);
            if (tw.getThumbKeys().isEmpty()
             && tw.getChord().toInt() == chord) {
               break;
            }
         }
         if (a == asgs.size()) {
            unmapped.add(new Assignment(new Twiddle(chord), new KeyPressList()));
         }
      }
      return unmapped;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static List<Assignment> sort(List<Assignment> asgs, SortedChordTimes times) {
      List<Assignment> sorted = new ArrayList<Assignment>();
      List<Assignment> sortedThumbed = new ArrayList<Assignment>();
      for (int i = 0; i < times.getSize(); ++i) {
         String label = times.getSortedLabel(i);
         int chord = Chord.fromString(label);
         if (chord == 0) {
            Log.err("Badly formed chord \"" + times.getSortedLabel(i) + "\".");
            continue;
         }
         for (int a = 0; a < asgs.size(); ++a) {
            Twiddle tw = asgs.get(a).getTwiddle(0);
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
   private List<Assignment> m_Remap = new ArrayList<Assignment>();
}
