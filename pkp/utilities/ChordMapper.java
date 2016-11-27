/**
 * Copyright 2015 Pushkar Piggott
 *
 * ChordMapper.java
 */

package pkp.utilities;

import java.awt.Window;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
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
public class ChordMapper extends ControlDialog 
   implements ActionListener, SaveChordsWindow.ContentForTitle {

   ///////////////////////////////////////////////////////////////////
   public ChordMapper(Window owner, File mapFile, SortedChordTimes times, boolean create) {
      super(owner, (create ? Action.CREATE : Action.ASSESS).getTitle());
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

      m_ChordTimes = times;
      m_Action = create ? Action.CREATE : Action.ASSESS;
      File defaultMapFile = m_Action.getDefaultMapFile(Io.fileExists(mapFile)
                                                       ? mapFile
                                                       : null);

      Box box = getBox();
      JLabel label = new JLabel(m_Action.getCfgExplanation());
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(label);
      box.add(Box.createVerticalGlue());

      m_MapFileBox = m_Action.getMapFileBox(Io.fileExists(mapFile) ? mapFile : null);
      box.add(m_MapFileBox);
      box.add(Box.createVerticalGlue());
      box.add(Box.createVerticalGlue());

      label = new JLabel(m_Action.getChordsKeysExplanation());
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(label);
      box.add(Box.createVerticalGlue());

      m_ChordsFileBox = new 
         FileBox(sm_CHORDS, Persist.getFile(sm_CHORDS_FILE_PERSIST), 
                 "Choose a chords file", "chords", 
                 m_ChordTimes == null ? "unset" : "current");
      box.add(m_ChordsFileBox);
      box.add(Box.createVerticalGlue());

      m_KeysFileBox = new 
         FileBox(sm_KEYS, Persist.getFile(sm_KEYS_FILE_PERSIST), 
                 "Choose a keystrokes file", "keys");
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
         if (!isFileOk(m_ChordTimes, chordsFile, keysFile) || !m_Action.isFileOk(mapFile)) {
            return;
         }
         Persist.set(sm_CHORDS_FILE_PERSIST, chordsFile);
         Persist.set(sm_KEYS_FILE_PERSIST, keysFile);
         Persist.set(sm_MAP_FILE_PERSIST, mapFile);
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
         HtmlWindow hw = new HtmlWindow(getClass().getResource("/data/ref.html") + m_Action.getHelpTag());
         hw.setTitle(sm_HELP);
         hw.setVisible(true);
         return;
      }
   }

   ///////////////////////////////////////////////////////////////////
   @Override // ContentForTitle
   public String getContentForTitle(String title) {
      if (Action.CREATE.getSaveDialogTitle().equals(title)) {
         return getMap();
      } else 
      if (Action.ASSESS.getSaveDialogTitle().equals(title)) {
         return getAssessment();
      } else {
         Log.err(getClass().getSimpleName() + " unknown title: " + title);
         return "";
      }
   }

   // Private /////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////
   private enum Action {
      CREATE() {
         @Override
         String getTitle() {
            return "Create Map File";
         }
         @Override
         String getHelpTag() {
            return "#map";
         }
         @Override
         File getDefaultMapFile(File deflt) {
            return null;
         }
         @Override
         FileBox getMapFileBox(File deflt) {
            return new
               FileBox(sm_MAPPED, Persist.getFile(sm_MAP_FILE_PERSIST, null),
                       null, "Choose a map file", "cfg.chords", "none");
         }
         @Override
         String getCfgExplanation() {
            return "<html>Optionally select an existing file of mappings to include.</html>";
         }
         @Override
         String getChordsKeysExplanation() {
            return "<html>Select sorted files of chords and keystrokes to combine.</html>";
         }
         @Override
         boolean isFileOk(File mapFile) {
            return true;
         }
         @Override
         JCheckBox getCheckbox(int which, ChordMapper mapper, Box box) {
            switch (which) {
            case 0: return mapper.addCheckBox("Sort by chord frequency", Persist.getBool("#.map.frequency.sort", false), box);
            case 1: return mapper.addCheckBox("Skip duplicate keystrokes", Persist.getBool("#.map.skip.duplicates", true), box);
            case 2: return mapper.addCheckBox("Show unmapped chords", Persist.getBool("#.map.show.unmapped", false), box);
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
         String getHelpTag() {
            return "#assess";
         }
         @Override
         File getDefaultMapFile(File deflt) {
            return deflt;
         }
         @Override
         FileBox getMapFileBox(File deflt) {
            return new
               FileBox(sm_MAPPED, Persist.getFile(sm_MAP_FILE_PERSIST, deflt),
                       deflt, "Choose a map file", "cfg.chords");
         }
         @Override
         String getCfgExplanation() {
            return "<html>Select a map file to assess.</html>";
         }
         @Override
         String getChordsKeysExplanation() {
            return "<html>Select sorted files of chords and keystrokes.</html>";
         }
         @Override
         boolean isFileOk(File mapFile) {
             if (mapFile == null) {
               Log.warn("No map file specified.");
               return false;
            }
            return true;
         }
         @Override
         JCheckBox getCheckbox(int which, ChordMapper mapper, Box box) {
            switch (which) {
            case 0: return mapper.addCheckBox("Sort by chord frequency", false, box);
            case 1: return mapper.addCheckBox("More detail", Persist.getBool("#.assess.more.detail", false), box);
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
      abstract String getHelpTag();
      abstract File getDefaultMapFile(File deflt);
      abstract FileBox getMapFileBox(File deflt);
      abstract String getCfgExplanation();
      abstract String getChordsKeysExplanation();
      abstract boolean isFileOk(File mappedFile);
      abstract JCheckBox getCheckbox(int which, ChordMapper mapper, Box box);
      abstract void persist(JCheckBox check0, JCheckBox check1, JCheckBox check2);
      abstract void act(ChordMapper mapper, File mappedF, File chordF, File keysF);
      abstract String getSaveDialogTitle();
   }

   ////////////////////////////////////////////////////////////////////////////
   private JButton createButton(String label) {
      JButton b = new JButton(label);
      b.addActionListener(this);
      return b;
   }

   ////////////////////////////////////////////////////////////////////////////
   private JCheckBox addCheckBox(String label, boolean check, Box box) {
      JCheckBox cb = new JCheckBox(label, check);
      cb.setOpaque(false);
      cb.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(cb);
      return cb;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   private boolean isFileOk(SortedChordTimes times, File chordF, File keysF) {
      if (times == null && !Io.fileExists(chordF)) {
         Log.warn("No chords file.");
         return false;
      }
      if (!Io.fileExists(keysF)) {
         Log.warn("No keystrokes file.");
         return false;
      }
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   private enum Target { MAPPED, CHORD, KEYS };

   ////////////////////////////////////////////////////////////////////////////
   private void assess(File mappedF, File chordF, File keysF) {
      if (chordF != null) {
         m_ChordTimes = new SortedChordTimes(chordF);
      }

      StringBuilder err = new StringBuilder();
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
            m_SortedKplFrequencies.add(keysLr.getInt(line.substring(offset + end)));
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
      List<Assignment> mapped = (new Assignments(mappedF)).toList();
      for (Assignment asg: mapped) {
         int chordPos = m_ChordTimes.findChord(asg.getTwiddle(0).getChord().toString());
         if (chordPos == -1) {
            Log.log("Failed to find \"" + asg.getTwiddle(0).getChord() + "\" in "
                    + (chordF != null ? chordF.getPath() : "chords"));
            continue;
         }
         int keyPos = Util.find(asg.getKeyPressList(), m_SortedKpls);
         if (keyPos == -1) {
            Log.log("Failed to find '" + asg.getKeyPressList() + "' in " + keysF.getPath());
            continue;
         }
         m_Assigns.add(asg);
         m_MaxAssignLength = Math.max(m_MaxAssignLength, asg.toString().length());
         final String blank = "----";
         String diffs = String.format("%4d ", chordPos - keyPos);
         String details = String.format("%5d %5d ", keyPos, chordPos);
         int chordTime = m_ChordTimes.getCount(chordPos);
         int keyTime = keyPos < m_ChordTimes.getSize()  
                     ? m_ChordTimes.getCount(keyPos)
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
   
   ////////////////////////////////////////////////////////////////////////////
   private String getAssessment() {
      String str = getHeader("Assessment");
      if (m_MaxAssignLength == 0) {
         // no assignments
         return str + "\n# Nothing to show. Check the log.\n";
      }
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
   private void map(File mappedF, File chordF, File keysF) {
      if (mappedF != null) {
         m_Assignments = new Assignments(mappedF);
         if (m_Assignments.size() == 0) {
            Log.warn('"' + mappedF.getPath() + "\" contains no assignments.");
         }
         if (m_Assignments.isRemap()) {
            Log.warn(m_Assignments.reportRemap(mappedF.getPath()));
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
   private String getMap() {
      String str = getHeader("Mapping");
      str += m_Assignments.toString(!Assignment.sm_SHOW_THUMB_KEYS,
                                    KeyPress.Format.FILE,
                                    m_CheckBoxShowEmpty.isSelected(), 
                                    m_CheckBoxSortChords.isSelected()
                                    ? m_ChordTimes : null);
      return str;
   }

   ////////////////////////////////////////////////////////////////////////////
   private String getHeader(String title) {
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
      return "# Twidlit " + title + " at " + df.format(Calendar.getInstance().getTime()) + '\n'
           + (m_MapFileBox.getFile() != null
             ? "# Map    " + m_MapFileBox.getFile().getPath() + "\n"
             : "")
           + "# Chords " + (m_ChordsFileBox.getFile() == null ? "current" : m_ChordsFileBox.getFile().getPath()) + "\n"
           + "# Keys   " + m_KeysFileBox.getFile().getPath() + "\n";
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
   
   private static final String sm_CHORDS_FILE_PERSIST = "#.map.chords.file";
   private static final String sm_KEYS_FILE_PERSIST = "#.map.keys.file";
   private static final String sm_MAP_FILE_PERSIST = "#.map.mapped.file";

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
   private SortedChordTimes m_ChordTimes;
   private ArrayList<KeyPressList> m_SortedKpls;
   private ArrayList<Integer> m_SortedKplFrequencies;
   private boolean m_GotFreq;
}

