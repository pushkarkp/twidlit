/**
 * Copyright 2015 Pushkar Piggott
 *
 * ChordMapper.java
 */

package pkp.twidlit;

import java.awt.Window;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import pkp.twiddle.Twiddle;
import pkp.twiddle.Chord;
import pkp.twiddle.KeyPress;
import pkp.twiddle.KeyPressList;
import pkp.twiddle.Assignment;
import pkp.twiddle.Assignments;
import pkp.times.SortedChordTimes;
import pkp.io.Io;
import pkp.io.LineReader;
import pkp.ui.HtmlWindow;
import pkp.ui.ControlDialog;
import pkp.ui.ExtensionFileFilter;
import pkp.ui.FileBox;
import pkp.string.StringInt;
import pkp.util.Persist;
import pkp.util.Pref;
import pkp.util.Log;
import pkp.util.Util;

//////////////////////////////////////////////////////////////////////
class ChordMapper extends ControlDialog 
   implements ActionListener, SaveChordsWindow.ContentForTitle {

   /////////////////////////////////////////////////////////////////////////
   public enum Action {
      CREATE() {
         @Override
         String getTitle() {
            return "Create Map File";
         }
         @Override
         File getDefaultMapFile(File deflt) {
            return null;
         }
         @Override
         String getCfgExplanation() {
            return "<html>Optionally select an existing file of mappings to include.</html>";
         }
         @Override
         String getChordsKeysExplanation() {
            return "<html>Select a file of chords and a file of keystrokes to combine. The chords should be sorted by speed and the keystrokes sorted by frequency.</html>";
         }
         @Override
         boolean isFileOk(File chordsFile, File keysFile, File mappedFile) {
            if ((chordsFile == null || keysFile == null)
             && (mappedFile == null || chordsFile != null || keysFile != null)) {
               Log.warn("Mapping requires both a chords file and a keystrokes files.");
               return false;
            }
            return true;
         }
         @Override
         JCheckBox getCheckbox(int which, ChordMapper mapper, Box box) {
            switch (which) {
            case 0: return mapper.addCheckBox(box, "Sort by chord frequency", Persist.getBool("#.map.frequency.sort", false));
            case 1: return mapper.addCheckBox(box, "Skip duplicate keystrokes", Persist.getBool("#.map.skip.duplicates", true));
            case 2: return mapper.addCheckBox(box, "Show unmapped chords", Persist.getBool("#.map.show.unmapped", false));
            default: return null;
            }
         }
         @Override
         void persist(JCheckBox check0, JCheckBox check1, JCheckBox check2) {
            Persist.set("#.map.frequency.sort", check0.isSelected());
            Persist.set("#.map.skip.duplicates", check1.isSelected());
            Persist.set("#.map.show.unmapped", check2.isSelected());
         }
         @Override
         void act(ChordMapper mapper, File mappedF, File chordF, File keysF) {
            mapper.map(mappedF, chordF, keysF);
         }
         @Override
         String getSaveDialogTitle() {
            return "Chord Mappings";
         }
      },
      ASSESS() {
         @Override
         String getTitle() {
            return "Assess Map File";
         }
         @Override
         File getDefaultMapFile(File deflt) {
            return deflt;
         }
         @Override
         String getCfgExplanation() {
            return "<html>Select a map file to assess.</html>";
         }
         @Override
         String getChordsKeysExplanation() {
            return "<html>Select a file of chords and a file of keystrokes.</html>";
         }
         @Override
         boolean isFileOk(File chordsFile, File keysFile, File mappedFile) {
            if (chordsFile == null || keysFile == null || mappedFile == null) {
               Log.warn("Assessment requires all files.");
               return false;
            }
            return true;
         }
         @Override
         JCheckBox getCheckbox(int which, ChordMapper mapper, Box box) {
            switch (which) {
            case 0: return mapper.addCheckBox(box, "Sort by chord frequency", false);
            case 1: return mapper.addCheckBox(box, "More detail", Persist.getBool("#.assess.more.detail", false));
            default: return null;
            }
         }
         @Override
         void persist(JCheckBox check0, JCheckBox check1, JCheckBox check2) {
            Persist.set("#.assess.more.detail", check1.isSelected());
         }
         @Override
         void act(ChordMapper mapper, File mappedF, File chordF, File keysF) {
            mapper.assess(mappedF, chordF, keysF);
         }
         @Override
         String getSaveDialogTitle() {
            return "Chord Mappings Assessment";
         }
      };
      abstract String getTitle();
      abstract File getDefaultMapFile(File deflt);
      abstract String getCfgExplanation();
      abstract String getChordsKeysExplanation();
      abstract boolean isFileOk(File chordsFile, File keysFile, File mappedFile);
      abstract JCheckBox getCheckbox(int which, ChordMapper mapper, Box box);
      abstract void persist(JCheckBox check0, JCheckBox check1, JCheckBox check2);
      abstract void act(ChordMapper mapper, File mappedF, File chordF, File keysF);
      abstract String getSaveDialogTitle();
   }

   ///////////////////////////////////////////////////////////////////
   ChordMapper(Window owner, File mapFile, SortedChordTimes times, Action action) {
      super(owner, action.getTitle());
      setResizable(true);
      m_NL = null;
      m_CR = null;
      if ("windows".equalsIgnoreCase(Pref.get("#.new.line"))) {
         KeyPressList kpl = KeyPressList.parseText("\n");
         if (!kpl.isValid()) {
            Log.log("Using Windows new line and \n is not defined");
         } else {
            m_NL = kpl.get(0);
            kpl = KeyPressList.parseText("\r");
            if (kpl.isValid()) {
               m_CR = kpl.get(0);
            } else {
               Log.log("Using Windows new line and \r is not defined");
               m_NL = null;
            }
         }
      }
      m_GotEnter = false;
      m_DuplicateKeys = 0;
      m_DuplicateKey = "";
      m_ExistingTwiddles = (ArrayList<Integer>[])new ArrayList[Chord.sm_VALUES];
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         m_ExistingTwiddles[i] = new ArrayList<Integer>();
      }
      m_Assignments = new Assignments();

      m_Times = times;
      m_Action = action;
      File defaultMapFile = m_Action.getDefaultMapFile(mapFile);
      if (defaultMapFile != null 
       && (!defaultMapFile.exists() || defaultMapFile.isDirectory())) {
         defaultMapFile = null;
      }

      Box box = getBox();
      JLabel label = new JLabel(m_Action.getCfgExplanation());
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(label);
      box.add(Box.createVerticalGlue());

      m_MapFileBox = new
         FileBox(sm_MAPPED, Persist.getFile("#.map.mapped.file", defaultMapFile),
                 defaultMapFile, "Choose a map file", "cfg.chords");
      box.add(m_MapFileBox);
      box.add(Box.createVerticalGlue());
      box.add(Box.createVerticalGlue());

      label = new JLabel(m_Action.getChordsKeysExplanation());
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(label);
      box.add(Box.createVerticalGlue());

      m_ChordsFileBox = new 
         FileBox(sm_CHORDS, Persist.getFile("#.map.chords.file"), 
                 null, "Choose a chords file", "chords");
      box.add(m_ChordsFileBox);
      box.add(Box.createVerticalGlue());

      m_KeysFileBox = new 
         FileBox(sm_KEYS, Persist.getFile("#.map.keys.file"), 
                 null, "Choose a keystrokes file", "keys");
      box.add(m_KeysFileBox);
      box.add(Box.createVerticalGlue());

      m_CheckBoxSortChords = m_Action.getCheckbox(0, this, box);
      m_CheckBoxSkipDupKeys = m_Action.getCheckbox(1, this, box);
      // alias for assess
      m_CheckBoxMoreDetail = m_CheckBoxSkipDupKeys;
      m_CheckBoxShowEmpty = m_Action.getCheckbox(2, this, box);

      addButton(createButton(sm_OK));
      addButton(createButton(sm_CANCEL));
      addButton(createButton(sm_HELP));
      setVisible(true);
   }

   ///////////////////////////////////////////////////////////////////
   @Override
   public void actionPerformed(ActionEvent e) {
      switch (e.getActionCommand()) {
      default: {
         Log.err("ChordMapper unexpected command: " + e.getActionCommand());
         return;
      }
      case sm_OK:
         File chordsFile = m_ChordsFileBox.getFile();
         File keysFile = m_KeysFileBox.getFile();
         File mapFile = m_MapFileBox.getFile();
         if (!m_Action.isFileOk(chordsFile, keysFile, mapFile)) {
            return;
         }
         Persist.setFile("#.map.chords.file", chordsFile);
         Persist.setFile("#.map.keys.file", keysFile);
         Persist.setFile("#.map.mapped.file", mapFile);
         m_Action.persist(m_CheckBoxSortChords, m_CheckBoxSkipDupKeys, m_CheckBoxShowEmpty);
         m_Action.act(this, mapFile, chordsFile, keysFile);
         SaveChordsWindow scw = new
            SaveChordsWindow(this, 
                             m_Action.getSaveDialogTitle(), 
                             mapFile != null
                              ? mapFile.getParent()
                              : ".");
         scw.setPersistName("#.chord.list");
         scw.setExtension("cfg.chords");
         scw.setVisible(true);
         if (m_DuplicateKeys > 0) {
            String action = m_CheckBoxSkipDupKeys.isSelected() ? "skipped" : "found";
            String seeLog = Log.hasFile() ? " (see log for details)." : ".";
            Log.warn(String.format("%d duplicate keystrokes (eg %s) were ", m_DuplicateKeys, m_DuplicateKey)
                    + action + " in " + keysFile.getPath() + seeLog);
         }
         // return;
      case sm_CANCEL:
         setVisible(false);
         dispose();
         return;
      case sm_HELP:
         HtmlWindow hw = new HtmlWindow(getClass().getResource("/data/ref.html") + "#map");
         hw.setTitle(sm_HELP);
         hw.setVisible(true);
         return;
      }
   }

   ///////////////////////////////////////////////////////////////////
   @Override // ContentForTitle
   public String getContentForTitle(String title) {
      if (Action.CREATE.getSaveDialogTitle().equals(title)) {
         return m_Assignments.toString(KeyPress.Format.FILE, false, 
                                       m_CheckBoxShowEmpty.isSelected(), 
                                       m_CheckBoxSortChords.isSelected()
                                       ? m_Times : null);
      } else 
      if (Action.ASSESS.getSaveDialogTitle().equals(title)) {
         return getAssessment();
      } else {
         Log.err(getClass().getSimpleName() + " unknown title: " + title);
         return "";
      }
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private JButton createButton(String label) {
      JButton b = new JButton(label);
      b.addActionListener(this);
      return b;
   }

   ////////////////////////////////////////////////////////////////////////////
   private JCheckBox addCheckBox(Box box, String label, boolean check) {
      JCheckBox cb = new JCheckBox(label, check);
      cb.setOpaque(false);
      cb.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(cb);
      return cb;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   private enum Target { MAPPED, CHORD, KEYS };

   ////////////////////////////////////////////////////////////////////////////
   private void assess(File mappedF, File chordF, File keysF) {
      StringBuilder err = new StringBuilder();
      m_SortedTwiddles = new ArrayList<Twiddle>();
      m_SortedTwiddleTimes = new ArrayList<Integer>();
      LineReader chordLr = new LineReader(Io.toExistUrl(chordF), Io.sm_MUST_EXIST);
      for (;;) {
         String line = chordLr.readLine();
         if ("".equals(line)) {
            continue;
         }
         if (line == null) {
            break;
         }
         Twiddle t = new Twiddle(line);
         if (!t.getChord().isValid()) {
            Log.parseWarn(chordLr, String.format("Failed to parse invalid chord \"%s\"", line));
         } else if (!t.getThumbKeys().isEmpty()) {
            Log.parseWarn(chordLr, String.format("Ignored chord with thumb keys \"%s\"", line));
         } else {
            m_SortedTwiddles.add(t);
            m_SortedTwiddleTimes.add(getInt(line, chordLr));
         }
      }
      chordLr.close();
      Util.sortAscending(m_SortedTwiddleTimes, m_SortedTwiddles);

      m_SortedKpls = new ArrayList<KeyPressList>();
      m_SortedKplFrequencies = new ArrayList<Integer>();
      m_GotFreq = false;
      LineReader keysLr = new LineReader(Io.toExistUrl(keysF), Io.sm_MUST_EXIST);
      for (;;) {
         String line = keysLr.readLine();
         if ("".equals(line)) {
            continue;
         }
         if (line == null) {
            break;
         }
         int offset = Io.findFirstNotOf(line, Io.sm_WS);
         int end = Io.findFirstOf(line.substring(offset), Io.sm_WS);
         KeyPressList kpl = KeyPressList.parseTextAndTags(line.substring(offset, offset + end), err);
         if (!"".equals(err.toString())) {
            Log.parseWarn(keysLr, err.toString(), line);
            err = new StringBuilder();
         } else {
            m_SortedKpls.add(kpl);
            m_SortedKplFrequencies.add(getInt(line.substring(offset + end), keysLr));
            if (!m_GotFreq && m_SortedKplFrequencies.get(m_SortedKplFrequencies.size() - 1) > 0) {
               m_GotFreq = true;
            }
         }
      }
      keysLr.close();
      Util.sortDescending(m_SortedKplFrequencies, m_SortedKpls);

      m_Assigns = new ArrayList<Assignment>();
      m_MaxAssignLength = 0;
      m_Assessments = new ArrayList<String>();
      LineReader mappedLr = new LineReader(Io.toExistUrl(mappedF), Io.sm_MUST_EXIST);
      for (;;) {
         String line = mappedLr.readLine();
         if ("".equals(line)) {
            continue;
         }
         if (line == null) {
            break;
         }
         Assignment asg = Assignment.parseLine(line, err);
         if (!"".equals(err.toString())) {
            Log.parseWarn(mappedLr, err.toString(), line);
            err = new StringBuilder();
         }
         if (asg == null) {
            continue;
         }
         int chordPos = Util.find(asg.getTwiddle(), m_SortedTwiddles);
         if (chordPos == -1) {
            Log.log("Failed to find '" + asg.getTwiddle() + "' in " + chordF.getPath());
         } else {
            int keyPos = Util.find(asg.getKeyPressList(), m_SortedKpls);
            if (keyPos == -1) {
               Log.log("Failed to find '" + asg.getKeyPressList() + "' in " + keysF.getPath());
            } else {
               m_Assigns.add(asg);
               m_MaxAssignLength = Math.max(m_MaxAssignLength, asg.toString().length());
               final String blank = "----";
               String diffs = String.format("%4d ", chordPos - keyPos);
               String details = String.format("%5d %5d ", keyPos, chordPos);
               int chordTime = m_SortedTwiddleTimes.get(chordPos);
               int keyTime = keyPos < m_SortedTwiddleTimes.size()  
                           ? m_SortedTwiddleTimes.get(keyPos)
                           : 0;
               if (keyTime == 0) {
                  diffs += String.format("%6s ", blank);
                  details += String.format("%5s %5d ", blank, chordTime);
               } else {
                  diffs += String.format("%6d ", keyTime * 100 / chordTime);
                  details += String.format("%5d %5d ", keyTime, chordTime);
               }
               if (m_GotFreq) {
                  int keyOccur = m_SortedKplFrequencies.get(keyPos);
                  int chordOccur = chordPos < m_SortedKplFrequencies.size()  
                                 ? m_SortedKplFrequencies.get(chordPos)
                                 : 0;
                  if (chordOccur == 0) {
                     diffs += String.format("%6s ", blank);
                     details += String.format("%8d %8s ", keyOccur, blank);
                  } else {
                     diffs += String.format("%6d ", chordOccur * 100 / keyOccur);
                     details += String.format("%8d %8d ", keyOccur, chordOccur);
                  }
               }
               if (m_CheckBoxMoreDetail.isSelected()) {
                  diffs += details;
               }
               m_Assessments.add(diffs);
            }
         }
      }
      mappedLr.close();
   }
   
   ////////////////////////////////////////////////////////////////////////////
   private String getAssessment() {
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
      String str = "";
      str += "# Twidlit Assessment at " + df.format(Calendar.getInstance().getTime()) + '\n';
      str += "# Map    " + m_MapFileBox.getFile().getPath() + "\n";
      str += "# Chords " + m_ChordsFileBox.getFile().getPath() + "\n";
      str += "# Keys   " + m_KeysFileBox.getFile().getPath() + "\n";
      String freq1 = m_GotFreq ? "  Change %" : "Speed";
      String freq2 = m_GotFreq ? "Speed   Freq" : "    %";
      String freq3 = m_GotFreq ? "  " : " ";
      String freq4 = m_GotFreq ? "    Key Occurrence" : "";
      String freq5 = m_GotFreq ? "     From       To" : "";
      String format = String.format("%%-%ds # ", m_MaxAssignLength);
      String l1 = String.format(format, " ") + "Moved " + freq1;
      String l2 = String.format(format, " ") + "      " + freq2;
      if (m_CheckBoxMoreDetail.isSelected()) {
         l1 += freq3;
         l1 += "     Moved    Chord Time" + freq4;
         l2 += "  From    To  From    To" + freq5;
      }
      str += l1 + '\n' + l2 + '\n';
      for (int i = 0; i < m_Assigns.size(); ++i) {
         str += String.format(format, m_Assigns.get(i)) + m_Assessments.get(i) + '\n';
      }
      return str;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   private int getInt(String line, LineReader lr) {
      int offset = Io.findFirstOf(line, Io.sm_DIGIT);
      int len = Io.findFirstOf(line.substring(offset), Io.sm_WS);
      if (len > 0) {
         StringBuilder err = new StringBuilder();
         int val = Io.parseInt(line.substring(offset, offset + len), err);
         if ("".equals(err.toString())) {
            return val;
         }
         Log.parseWarn(lr, err.toString(), line);
      }
      return 0;
   }

   ////////////////////////////////////////////////////////////////////////////
   private void map(File mappedF, File chordF, File keysF) {
      if (mappedF != null) {
         StringBuilder err = new StringBuilder();
         LineReader mappedLr = new LineReader(Io.toExistUrl(mappedF), Io.sm_MUST_EXIST);
         for (;;) {
            String mapped = getStr(mappedLr, Target.MAPPED);
            if (mapped == null) {
               break;
            }
            m_Assignments.add(Assignment.parseLine(mapped, err));
            if (!"".equals(err.toString())) {
               Log.parseWarn(mappedLr, err.toString(), mapped);
               err = new StringBuilder();
            }
         }
         mappedLr.close();
         if (m_Assignments.isRemap()) {
            Log.warn(m_Assignments.reportRemap(mappedLr.getPath()));
         }
      }
      if (chordF != null && keysF != null) {
         StringBuilder err = new StringBuilder();
         LineReader chordLr = new LineReader(Io.toExistUrl(chordF), Io.sm_MUST_EXIST);
         LineReader keysLr = new LineReader(Io.toExistUrl(keysF), Io.sm_MUST_EXIST);
         for (;;) {
            String chord = getStr(chordLr, Target.CHORD);
            String keys = getStr(keysLr, Target.KEYS);
            if (chord == null || keys == null) {
               break;
            }
            m_Assignments.add(Assignment.parseLine(chord + " = " + keys, err));
            if (!"".equals(err.toString())) {
               Log.parseWarn(keysLr, err.toString(), keys);
               err = new StringBuilder();
            }
         }
         chordLr.close();
         keysLr.close();
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private String getStr(LineReader lr, Target target) {
      for (;;) {
         String line = lr.readLine();
         if (line == null) {
            return null;
         }
         String str = "";
         if (target == Target.KEYS) {
            str = getKeys(line, lr);
         } else if (line.length() < 4) {
            Log.parseWarn(lr, String.format("Failed to parse too short line \"%s\"", line));
         } else {
            Twiddle t = new Twiddle(line);
            if (!t.getChord().isValid()) {
               Log.parseWarn(lr, String.format("Failed to parse invalid chord \"%s\"", line));
            } else if (target == Target.MAPPED) {
               str = line;
            } else if (target == Target.CHORD) {
               // ignore duplicate chords
               if (m_Assignments.isMap(t)) {
                  StringInt si = lr.getNameAndPosition();
                  Log.log(String.format("Skipped duplicate chord \"%s\" in line %d of %s", t, si.getInt(), si.getString()));
                  str = "";
               } else {
                  str = t.toString();
               }
            }
         }
         if (str != "") {
            return str;
         }
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private String getKeys(String line, LineReader lr) {
      int initial = Io.findFirstNotOf(line, Io.sm_WS);
      String rest = line.substring(initial);
      int length = Io.findFirstOf(rest, Io.sm_WS);
      if (length == 0) {
         Log.log(String.format("Initial |%c|", line.charAt(initial)));
         Log.log(String.format("Failed to parse \"%s\" on line %d of %s.",
                               line, lr.getLineNumber(), lr.getPath()));
         return "";
      }
      if (length == rest.length()) {
         length = line.length() - initial;
      }
      String str = line.substring(initial, initial + length);
      StringBuilder err = new StringBuilder();
      KeyPressList kpl = KeyPressList.parseTextAndTags(str, err);
      if (!kpl.isValid()) {
         Log.warn(String.format("Failed to parse \"%s\" on line %d of %s (%s).",
                                str, lr.getLineNumber(), lr.getPath(), err));
         err = new StringBuilder();
         return "";
      }
      kpl = handleEnter(kpl);
      if (!kpl.isValid()) {
         // discarded enter
         return "";
      }
//System.out.printf("%d %s %s%n", m_Assignments.size(), m_Assignments.get(1).getKeyPressList().toString(KeyPress.Format.ESC), kpl.toString(KeyPress.Format.ESC));
      String action = m_CheckBoxSkipDupKeys.isSelected() ? "Skipped" : "Found";
      for (int i = 0; i < m_Assignments.size(); ++i) {
         if (kpl.equals(m_Assignments.get(i).getKeyPressList())) {
            String keys = line.substring(initial, initial + length);
            Log.log(String.format(action + " repeat of '%s' on line %d of %s.",
                                  keys, lr.getLineNumber(), lr.getPath()));
            if (m_CheckBoxSkipDupKeys.isSelected()) {
               ++m_DuplicateKeys;
               if ("".equals(m_DuplicateKey)) {
                  m_DuplicateKey = keys;
               }
               return "";
            }
         }
      }
      return kpl.toString(KeyPress.Format.TAG);
   }

   ////////////////////////////////////////////////////////////////////////////
   private KeyPressList handleEnter(KeyPressList kpl) {
      // not windows eol or empty
      if (m_NL == null || !kpl.isValid()) {
         return kpl;
      }
      boolean cr0 = m_CR.equals(kpl.get(0));
      if (kpl.size() == 1) {
         // convert /r or /n to enter
         if (cr0 || m_NL.equals(kpl.get(0))) {
            if (m_GotEnter) {
               return new KeyPressList();
            }
            m_GotEnter = true;
            return new KeyPressList(m_NL);
         }
         return kpl;
      }
      if (kpl.size() == 2) {
         // convert /r/n to enter
         if (cr0 && m_NL.equals(kpl.get(1))) {
            if (m_GotEnter) {
               return new KeyPressList();
            }
            m_GotEnter = true;
            return new KeyPressList(m_NL);
         }
      }
      for (int i = 0; i < kpl.size(); ++i) {
         // convert /r to enter
         if (m_CR.equals(kpl.get(i))) {
            kpl.set(i, m_NL);
         }
      }
      return kpl;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static boolean isEnter(char c) {
      return c == '\r' || c == '\n';
   }

   // Data /////////////////////////////////////////////////////////////////////
   private static final String sm_OK = "OK";
   private static final String sm_CANCEL = "Cancel";
   private static final String sm_HELP = "Help";
   private static final String sm_MAPPED = "Map";
   private static final String sm_CHORDS = "Chords";
   private static final String sm_KEYS = "Keystrokes";

   private KeyPress m_NL;
   private KeyPress m_CR;
   private Action m_Action;
   private FileBox m_ChordsFileBox;
   private FileBox m_KeysFileBox;
   private FileBox m_MapFileBox;
   private JCheckBox m_CheckBoxSortChords;
   private JCheckBox m_CheckBoxSkipDupKeys;
   private JCheckBox m_CheckBoxMoreDetail;
   private JCheckBox m_CheckBoxShowEmpty;
   private int m_DuplicateKeys;
   private String m_DuplicateKey;
   private boolean m_GotEnter;
   private ArrayList<Integer>[] m_ExistingTwiddles;
   private Assignments m_Assignments;
   private ArrayList<Assignment> m_Assigns;
   private int m_MaxAssignLength;
   private ArrayList<String> m_Assessments;
   private SortedChordTimes m_Times;
   private ArrayList<Twiddle> m_SortedTwiddles;
   private ArrayList<Integer> m_SortedTwiddleTimes;
   private ArrayList<KeyPressList> m_SortedKpls;
   private ArrayList<Integer> m_SortedKplFrequencies;
   private boolean m_GotFreq;
}

