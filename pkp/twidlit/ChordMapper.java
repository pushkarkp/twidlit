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
import java.util.ArrayList;
import pkp.twiddle.Twiddle;
import pkp.twiddle.Chord;
import pkp.twiddle.KeyPress;
import pkp.twiddle.KeyPressList;
import pkp.twiddle.Assignment;
import pkp.times.SortedChordTimes;
import pkp.io.Io;
import pkp.io.LineReader;
import pkp.ui.HtmlWindow;
import pkp.ui.ControlDialog;
import pkp.ui.ExtensionFileFilter;
import pkp.util.Persist;
import pkp.util.Pref;
import pkp.util.Log;

//////////////////////////////////////////////////////////////////////
class ChordMapper extends ControlDialog 
   implements ActionListener, SaveChordsWindow.ContentForTitle {

   ///////////////////////////////////////////////////////////////////
   ChordMapper(Window owner, SortedChordTimes times) {
      super(owner, "Map Chords");
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
      m_Times = times;
      m_ChordsFile = Persist.getFile("#.map.chords.file");
      m_KeysFile = Persist.getFile("#.map.keys.file");
      m_MappedFile = Persist.getFile("#.map.mapped.file");
      m_GotEnter = false;
      m_DuplicateKeys = false;
      m_ExistingTwiddles = (ArrayList<Integer>[])new ArrayList[Chord.sm_VALUES];
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         m_ExistingTwiddles[i] = new ArrayList<Integer>();
      }
      m_Assignments = new ArrayList<Assignment>();
      Box box = getBox();
      JLabel label = new JLabel("<html>Optionally select a file of existing mappings to include.</html>");
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(label);
      box.add(Box.createVerticalGlue());
      Box fileBox = createButtonLabelBox(sm_MAPPED);
      m_MappedFileLabel = (JLabel)fileBox.getComponent(2);
      setLabel(m_MappedFileLabel, m_MappedFile);
      box.add(fileBox);
      box.add(Box.createVerticalGlue());
      box.add(Box.createVerticalGlue());
      label = new JLabel("<html>Select a file of chords and a file of keystrokes to combine.</html>");
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(label);
      box.add(Box.createVerticalGlue());
      fileBox = createButtonLabelBox(sm_CHORDS);
      m_ChordsFileLabel = (JLabel)fileBox.getComponent(2);
      setLabel(m_ChordsFileLabel, m_ChordsFile);
      box.add(fileBox);
      box.add(Box.createVerticalGlue());
      fileBox = createButtonLabelBox(sm_KEYS);
      m_KeysFileLabel = (JLabel)fileBox.getComponent(2);
      setLabel(m_KeysFileLabel, m_KeysFile);
      box.add(fileBox);
      box.add(Box.createVerticalGlue());
      m_CheckBoxSkipDup = addCheckBox(box, "Skip duplicate keystrokes", Persist.getBool("#.map.skip.duplicates", true));
      m_CheckBoxSort = addCheckBox(box, "Sort by chord frequency", Persist.getBool("#.map.frequency.sort", false));
      m_CheckBoxShowAll = addCheckBox(box, "Show unmapped chords", Persist.getBool("#.map.show.unmapped", false));
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
      case sm_CHORDS: {
         File f = chooseFile("chords", m_ChordsFile, "chords");
         if (f != null) {
            m_ChordsFile = Io.asRelative(f);
            setLabel(m_ChordsFileLabel, m_ChordsFile);
         }
         return;
      }
      case sm_KEYS: {
         File f = chooseFile("keystrokes", m_KeysFile, "keys");
         if (f != null) {
            m_KeysFile = Io.asRelative(f);
            setLabel(m_KeysFileLabel, m_KeysFile);
         }
         return;
      }
      case sm_MAPPED: {
         File f = chooseFile("mapped", m_MappedFile, "cfg.chords");
         if (f != null && !f.isDirectory()) {
            m_MappedFile = Io.asRelative(f);
         }
         setLabel(m_MappedFileLabel, m_MappedFile);
         return;
      }
      case sm_OK:
         // require both chords and keys or mapped with neither
         if ((m_ChordsFile == null || m_KeysFile == null)
          && (m_MappedFile == null || m_ChordsFile != null || m_KeysFile != null)) {
            Log.warn("Mapping requires both a chords and a keystrokes files.");
            return;
         }
         Persist.setFile("#.map.chords.file", m_ChordsFile);
         Persist.setFile("#.map.keys.file", m_KeysFile);
         Persist.setFile("#.map.mapped.file", m_MappedFile);
         Persist.set("#.map.skip.duplicates", m_CheckBoxSkipDup.isSelected());
         Persist.set("#.map.frequency.sort", m_CheckBoxSort.isSelected());
         Persist.set("#.map.show.unmapped", m_CheckBoxShowAll.isSelected());
         map(m_MappedFile, m_ChordsFile, m_KeysFile);
         if (m_CheckBoxSort.isSelected()) {
            m_Assignments = orderByChordTimes(m_Assignments);
         } else if (m_CheckBoxShowAll.isSelected()) {
            m_Assignments = addUnmapped(m_Assignments);
         }
         SaveChordsWindow scw = new
            SaveChordsWindow(this, "Chord Mappings", "cfg.chords");
         scw.setPersistName("#.chord.list");
         scw.setExtension("cfg.chords");
         scw.setVisible(true);
         if (m_DuplicateKeys) {
            String action = m_CheckBoxSkipDup.isSelected() ? "skipped" : "found";
            String seeLog = Log.hasFile() ? " (see log for details)." : ".";
            Log.warn("Duplicate keystrokes were " + action + seeLog);
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
      return Assignment.toString(m_Assignments, KeyPress.Format.FILE);
   }

   // Private /////////////////////////////////////////////////////////////////

   ///////////////////////////////////////////////////////////////////
   private File chooseFile(String name, File f, String ext) {
      File dir = null;
      if (f != null) {
         if (f.isDirectory()) {
            dir = f;
         } else if (f.getParent() != null) {
            dir = f.getParentFile();
         }
      }
      JFileChooser fc = createChooser("Choose a " + name + " file", ext, f);
      ChooserActionListener cal = new ChooserActionListener(fc);
      fc.addActionListener(cal);
      fc.showDialog(this, "OK");
      return cal.getFile();
   }

   ///////////////////////////////////////////////////////////////////
   class ChooserActionListener implements ActionListener {

      ////////////////////////////////////////////////////////////////
      ChooserActionListener(JFileChooser chooser) {
         m_FileChooser = chooser;
         m_File = null;
      }

      ////////////////////////////////////////////////////////////////
      File getFile() {
         return m_File;
      }

      ////////////////////////////////////////////////////////////////
      @Override
      public void actionPerformed(ActionEvent e) {
         if (e.getActionCommand() == "CancelSelection") {
            return;
         } else if (e.getActionCommand() != "ApproveSelection") {
            Log.err("ChooserActionListener unexpected command: " + e.getActionCommand());
         }
         m_File = m_FileChooser.getSelectedFile();
       }

      // Data ////////////////////////////////////////////////////////
      private JFileChooser m_FileChooser;
      private File m_File;
   }

   ///////////////////////////////////////////////////////////////////
   private JFileChooser createChooser(String title, String ext, File dir) {
      JFileChooser fc = new JFileChooser();
      fc.setDialogTitle(title);
      if (dir == null || !dir.exists()) {
         dir = new File(".");
      }
      fc.setCurrentDirectory(dir);
      ArrayList<String> effs = new ArrayList<String>();
      effs.add(ext);
      ExtensionFileFilter.setFileFilters(fc, effs);
      fc.addChoosableFileFilter(fc.getAcceptAllFileFilter());
      return fc;
   }

   ////////////////////////////////////////////////////////////////////////////
   private Box createButtonLabelBox(String buttonLabel) {
      Box box = new Box(BoxLayout.LINE_AXIS);
      box.setOpaque(false);
      box.setAlignmentX(Component.LEFT_ALIGNMENT);
      JButton b = createButton(buttonLabel);
      b.setMargin(new Insets(0, 5, 0, 5));
      box.add(b);
      box.add(Box.createHorizontalGlue());
      JLabel label = new JLabel();
      box.add(label);
      box.add(Box.createRigidArea(new Dimension(5, 0)));
      b = new JButton(sm_CLEAR);
      b.setPreferredSize(new Dimension(15, 15));
      b.setMargin(new Insets(0, 0, 0, 0));
      b.addActionListener(new FileNameClearer(label));
      box.add(b);
      return box;
   }

   ////////////////////////////////////////////////////////////////////////////
   private JButton createButton(String label) {
      JButton button = new JButton(label);
      button.addActionListener(this);
      return button;
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
   private void setLabel(JLabel label, File f) {
      if (f == null || f.getPath() == "") {
         label.setText("unset");
      } else {
         label.setText(f.getName());
      }
   }

   //////////////////////////////////////////////////////////////////////
   class FileNameClearer implements ActionListener {

      ///////////////////////////////////////////////////////////////////
      FileNameClearer(JLabel label) {
         m_Label = label;
      }

      ///////////////////////////////////////////////////////////////////
      @Override
      public void actionPerformed(ActionEvent e) {
         if (e.getActionCommand() != sm_CLEAR) {
            Log.err("ChordMapper unexpected command: " + e.getActionCommand());
            return;
         }
         if (m_Label == m_ChordsFileLabel) {
            m_ChordsFile = null;
         }
         if (m_Label == m_KeysFileLabel) {
            m_KeysFile = null;
         }
         if (m_Label == m_MappedFileLabel) {
            m_MappedFile = null;
         }
         setLabel(m_Label, null);
      }

      ///////////////////////////////////////////////////////////////////
      private JLabel m_Label;
   }

   ////////////////////////////////////////////////////////////////////////////
   private enum Target { MAPPED, CHORD, KEYS };

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
         } else if (line.length() < 6) {
            String msg = String.format("Failed to parse too short line \"%s\"", str);
            Log.parseWarn(lr, msg, str);
            str = "";
         } else {
            Twiddle t = new Twiddle(line);
            if (!t.getChord().isValid()) {
               String msg = String.format("Failed to parse invalid twiddle \"%s\"", str);
               Log.parseWarn(lr, msg, str);
               str = "";
            } else if (!isNew(t)) {
               String msg = String.format("Skipped duplicate chord %s", t);
               Log.parseWarn(lr, msg, str);
               str = "";
            } else if (target == Target.MAPPED) {
               str = line;
            } else if (target == Target.CHORD) {
               str = t.toString();
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
         Log.log(String.format("Failed to parse \"%s\" on line %d of \"%s\".",
                               line, lr.getLineNumber(), lr.getPath()));
         return "";
      }
      if (length == rest.length()) {
         length = line.length() - initial;
      }
      String str = line.substring(initial, initial + length);
      StringBuilder err = new StringBuilder();
      KeyPressList kpl = KeyPressList.parseTextAndTags(str, err);
      kpl = handleEnter(kpl);
      if (!kpl.isValid()) {
         Log.warn(String.format("Failed to parse \"%s\" on line %d of \"%s\" (%s).",
                                str, lr.getLineNumber(), lr.getPath(), err));
         err = new StringBuilder();
         return "";
      }
//System.out.printf("%d %s %s%n", m_Assignments.size(), m_Assignments.get(1).getKeyPressList().toString(KeyPress.Format.ESC), kpl.toString(KeyPress.Format.ESC));
      String action = m_CheckBoxSkipDup.isSelected() ? "Skipped" : "Found";
      for (int i = 0; i < m_Assignments.size(); ++i) {
         if (kpl.equals(m_Assignments.get(i).getKeyPressList())) {
            Log.log(String.format(action + " repeat of '%s' on line %d of \"%s\".",
                                  line.substring(initial, initial + length), lr.getLineNumber(), lr.getPath()));
            m_DuplicateKeys = true;
            if (m_CheckBoxSkipDup.isSelected()) {
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

   ////////////////////////////////////////////////////////////////////////////
   private boolean isNew(Twiddle t) {
      ArrayList<Integer> chord = m_ExistingTwiddles[t.getChord().toInt() - 1];
      int mods = t.getThumbKeys().toInt();
      for (int i = 0; i < chord.size(); ++i) {
         if (chord.get(i) == mods) {
            return false;
         }
      }
      chord.add(mods);
      return true;
   }

   ///////////////////////////////////////////////////////////////////////////////
   private ArrayList<Assignment> orderByChordTimes(ArrayList<Assignment> asgs) {
      ArrayList<Assignment> sorted = new ArrayList<Assignment>();
      for (int i = 0; i < m_Times.getSize(); ++i) {
         String label = m_Times.getSortedLabel(i);
         int chord = Chord.fromString(label);
         if (chord == 0) {
            Log.err("Badly formed chord \"" + m_Times.getSortedLabel(i) + "\".");
            continue;
         }
         int a = 0;
         for (; a < asgs.size(); ++a) {
            Twiddle tw = asgs.get(a).getTwiddle();
            if (tw.getThumbKeys().isEmpty() && tw.getChord().toInt() == chord) {
               break;
            }
         }
         if (a < asgs.size()) {
            sorted.add(asgs.get(a));
         } else if (m_CheckBoxShowAll.isSelected()) {
            sorted.add(new Assignment(new Twiddle(chord), new KeyPressList()));
         }
      }
      return sorted;
   }

   ///////////////////////////////////////////////////////////////////////////////
   private ArrayList<Assignment> addUnmapped(ArrayList<Assignment> asgs) {
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

   // Data /////////////////////////////////////////////////////////////////////
   private static final String sm_OK = "OK";
   private static final String sm_CANCEL = "Cancel";
   private static final String sm_HELP = "Help";
   private static final String sm_MAPPED = "Mapped";
   private static final String sm_CHORDS = "Chords";
   private static final String sm_KEYS = "Keystrokes";
   private static final String sm_CLEAR = "x";

   private KeyPress m_NL;
   private KeyPress m_CR;
   private JFileChooser m_FileChooser;
   private JLabel m_ChordsFileLabel;
   private JLabel m_KeysFileLabel;
   private JLabel m_MappedFileLabel;
   private File m_ChordsFile;
   private File m_KeysFile;
   private File m_MappedFile;
   private JCheckBox m_CheckBoxSkipDup;
   private JCheckBox m_CheckBoxSort;
   private JCheckBox m_CheckBoxShowAll;
   private boolean m_DuplicateKeys;
   private boolean m_GotEnter;
   private ArrayList<Integer>[] m_ExistingTwiddles;
   private ArrayList<Assignment> m_Assignments;
   private SortedChordTimes m_Times;
}

