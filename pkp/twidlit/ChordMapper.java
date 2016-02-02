/**
 * Copyright 2015 Pushkar Piggott
 *
 * ChordMapper.java
 */

package pkp.twidlit;

import java.awt.Font;
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
import pkp.ui.SaveTextWindow;
import pkp.ui.ControlDialog;
import pkp.ui.ExtensionFileFilter;
import pkp.util.Persist;
import pkp.util.Pref;
import pkp.util.Log;

//////////////////////////////////////////////////////////////////////
class ChordMapper extends ControlDialog implements ActionListener {

   ///////////////////////////////////////////////////////////////////
   ChordMapper(Window owner, SortedChordTimes times) {
      super(owner, "Map Chords");
      setResizable(true);
      m_NL = null;
      m_CR = null;
      if ("windows".equalsIgnoreCase(Pref.get("new.line"))) {
         KeyPressList kpl = KeyPressList.parseText("\n");
         if (!kpl.isValid()) {
            Log.log("Using Windows new line and \n is not defined");
         } else {
            m_NL = kpl.get(0);
            kpl = KeyPressList.parseTextAndTags("\r");
            if (kpl.isValid()) {
               m_CR = kpl.get(0);
            } else {
               Log.log("Using Windows new line and \r is not defined");
               m_NL = null;
            }
         }
      }
      m_Times = times;
      m_ChordsFile = Persist.getFile("map.chords.file");
      m_CharsFile = Persist.getFile("map.chars.file");
      m_MappedFile = Persist.getFile("map.mapped.file");
      m_GotEnter = false;
      m_DuplicateChars = false;
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
      label = new JLabel("<html>Select a file of chords and a file of characters to combine.</html>");
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(label);
      box.add(Box.createVerticalGlue());
      fileBox = createButtonLabelBox(sm_CHORDS);
      m_ChordsFileLabel = (JLabel)fileBox.getComponent(2);
      setLabel(m_ChordsFileLabel, m_ChordsFile);
      box.add(fileBox);
      box.add(Box.createVerticalGlue());
      fileBox = createButtonLabelBox(sm_CHARS);
      m_CharsFileLabel = (JLabel)fileBox.getComponent(2);
      setLabel(m_CharsFileLabel, m_CharsFile);
      box.add(fileBox);
      box.add(Box.createVerticalGlue());
      m_CheckBoxSkipDup = new JCheckBox("Skip duplicate characters", Persist.getBool("map.skip.duplicates", true));
      m_CheckBoxSkipDup.setOpaque(false);
      m_CheckBoxSkipDup.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(m_CheckBoxSkipDup);
      m_CheckBoxSort = new JCheckBox("Sort by chord frequency", Persist.getBool("map.frequency.sort", false));
      m_CheckBoxSort.setOpaque(false);
      m_CheckBoxSort.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(m_CheckBoxSort);
      addButton(createButton(sm_HELP));
      addButton(createButton(sm_CANCEL));
      addButton(createButton(sm_OK));
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
         File f = chooseFile("chords", m_ChordsFile, "cfg.txt");
         if (f != null) {
            m_ChordsFile = Io.asRelative(f);
            setLabel(m_ChordsFileLabel, m_ChordsFile);
         }
         return;
      }
      case sm_CHARS: {
         File f = chooseFile("characters", m_CharsFile, "counts");
         if (f != null) {
            m_CharsFile = Io.asRelative(f);
            setLabel(m_CharsFileLabel, m_CharsFile);
         }
         return;
      }
      case sm_MAPPED: {
         File f = chooseFile("mapped", m_MappedFile, "cfg.txt");
         if (f != null && !f.isDirectory()) {
            m_MappedFile = Io.asRelative(f);
         }
         setLabel(m_MappedFileLabel, m_MappedFile);
         return;
      }
      case sm_OK:
         // require both chords and chars or mapped with neither
         if ((m_ChordsFile == null || m_CharsFile == null)
          && (m_MappedFile == null || m_ChordsFile != null || m_CharsFile != null)) {
            Log.warn("Mapping requires both a chords and a characters files.");
            return;
         }
         if (m_ChordsFile != null) {
            Persist.set("map.chords.file", m_ChordsFile.getPath());
         }
         if (m_CharsFile != null) {
            Persist.set("map.chars.file", m_CharsFile.getPath());
         }
         if (m_MappedFile == null) {
            Persist.unset("map.mapped.file");
         } else {
            Persist.set("map.mapped.file", m_MappedFile.getPath());
         }
         Persist.set("map.skip.duplicates", m_CheckBoxSkipDup.isSelected());
         Persist.set("map.frequency.sort", m_CheckBoxSort.isSelected());
         map(m_MappedFile, m_ChordsFile, m_CharsFile);
         if (m_CheckBoxSort.isSelected()) {
            m_Assignments = orderByChordTimes(m_Assignments);
         }
         SaveTextWindow stw = new SaveTextWindow(
            "Chord Mappings",
            Assignment.toString(m_Assignments, KeyPress.Format.FILE),
            "cfg.txt");
         stw.setPersistName("chord.list");
         Font font = stw.getFont();
         stw.setFont(new Font("monospaced", font.getStyle(), font.getSize()));
         stw.setExtension("cfg.txt");
         stw.setVisible(true);
         if (m_DuplicateChars) {
            String action = m_CheckBoxSkipDup.isSelected() ? "skipped" : "found";
            String seeLog = Log.hasFile() ? " (see log for details)." : ".";
            Log.warn("Duplicate characters were " + action + seeLog);
         }
         // return;
      case sm_CANCEL:
         setVisible(false);
         dispose();
         return;
      case sm_HELP:
         HtmlWindow hw = new HtmlWindow(getClass().getResource("/data/map.html"));
         hw.setTitle(sm_HELP);
         hw.setVisible(true);
         return;
      }
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
      effs.add("txt");
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
      b.setBackground(Pref.getColor("background.color"));
      b.setMargin(new Insets(0, 5, 0, 5));
      box.add(b);
      box.add(Box.createHorizontalGlue());
      JLabel label = new JLabel();
      box.add(label);
      box.add(Box.createRigidArea(new Dimension(5, 0)));
      b = new JButton(sm_CLEAR);
      b.setPreferredSize(new Dimension(15, 15));
      b.setMargin(new Insets(0, 0, 0, 0));
      b.setBackground(Pref.getColor("background.color"));
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
         if (m_Label == m_CharsFileLabel) {
            m_CharsFile = null;
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
   private enum Target { MAPPED, CHORD, CHARS };

   ////////////////////////////////////////////////////////////////////////////
   private void map(File mappedF, File chordF, File charsF) {
      if (mappedF != null) {
         LineReader mappedLr = new LineReader(Io.toExistUrl(mappedF), Io.sm_MUST_EXIST);
         for (;;) {
            String mapped = getStr(mappedLr, Target.MAPPED);
            if (mapped == null) {
               break;
            }
            m_Assignments.add(Assignment.parseLine(mapped));
         }
         mappedLr.close();
      }
      if (chordF != null && charsF != null) {
         LineReader chordLr = new LineReader(Io.toExistUrl(chordF), Io.sm_MUST_EXIST);
         LineReader charsLr = new LineReader(Io.toExistUrl(charsF), Io.sm_MUST_EXIST);
         for (;;) {
            String chord = getStr(chordLr, Target.CHORD);
            String chars = getStr(charsLr, Target.CHARS);
            if (chord == null || chars == null) {
               break;
            }
            m_Assignments.add(Assignment.parseLine(chord + " = " + chars));
         }
         chordLr.close();
         charsLr.close();
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
         if (target == Target.CHARS) {
            str = getChars(line, lr);
         } else if (line.length() < 6) {
            Log.warn(String.format("Line too short. Failed to parse \"%s\" on line %d of \"%s\"",
                                   line, lr.getLineNumber(), lr.getPath()));
            str = "";
         } else {
            Twiddle t = new Twiddle(line);
            if (!t.getChord().isValid()) {
               Log.warn(String.format("Invalid twiddle. Failed to parse \"%s\" on line %d of \"%s\"",
                                      line, lr.getLineNumber(), lr.getPath()));
               str = "";
            } else if (!isNew(t)) {
               Log.log(String.format("Skipped duplicate chord %s on line %d of \"%s\"",
                                     t, lr.getLineNumber(), lr.getPath()));
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
   private String getChars(String line, LineReader lr) {
      int initial = Io.findFirstNotOf(line, Io.sm_WS);
      int length = Io.findFirstOf(line.substring(initial), Io.sm_WS);
      if (length == 0) {
         Log.log(String.format("Initial |%c|", line.charAt(initial)));
         Log.log(String.format("Failed to parse \"%s\" on line %d of \"%s\"",
                               line, lr.getLineNumber(), lr.getPath()));
         return "";
      }
      if (length == -1) {
         length = line.length() - initial;
      }
      KeyPressList kpl = KeyPressList.parseTextAndTags(line.substring(initial, initial + length));
      kpl = handleEnter(kpl);
      if (!kpl.isValid()) {
         return "";
      }
//System.out.printf("%d %s %s%n", m_Assignments.size(), m_Assignments.get(1).getKeyPressList().toString(KeyPress.Format.ESC), kpl.toString(KeyPress.Format.ESC));
      String action = m_CheckBoxSkipDup.isSelected() ? "Skipped" : "Found";
      for (int i = 0; i < m_Assignments.size(); ++i) {
         if (kpl.equals(m_Assignments.get(i).getKeyPressList())) {
            Log.log(String.format(action + " repeat of '%s' on line %d of \"%s\"",
                                  line.substring(initial, initial + length), lr.getLineNumber(), lr.getPath()));
            m_DuplicateChars = true;
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
      ArrayList<Assignment> sorted = new ArrayList<Assignment>(Chord.sm_VALUES);
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         String label = m_Times.getSortedLabel(i);
System.out.printf("i %d label %s%n", i, label);
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
         sorted.add(i, (a < asgs.size()) ? asgs.get(a) : Assignment.sm_NO_ASSIGNMENT);
      }
      return sorted;
   }
   
   // Data /////////////////////////////////////////////////////////////////////
   private static final String sm_OK = "OK";
   private static final String sm_CANCEL = "Cancel";
   private static final String sm_HELP = "Help";
   private static final String sm_MAPPED = "Mapped";
   private static final String sm_CHORDS = "Chords";
   private static final String sm_CHARS = "Characters";
   private static final String sm_CLEAR = "x";

   private static final int sm_OFFSET[][] = new int[][]{{0, 3}, {4, 7}};

   private KeyPress m_NL;
   private KeyPress m_CR;
   private JFileChooser m_FileChooser;
   private JLabel m_ChordsFileLabel;
   private JLabel m_CharsFileLabel;
   private JLabel m_MappedFileLabel;
   private File m_ChordsFile;
   private File m_CharsFile;
   private File m_MappedFile;
   private JCheckBox m_CheckBoxSkipDup;
   private JCheckBox m_CheckBoxSort;
   private boolean m_DuplicateChars;
   private boolean m_GotEnter;
   private ArrayList<Integer>[] m_ExistingTwiddles;
   private ArrayList<Assignment> m_Assignments;
   private SortedChordTimes m_Times;
}

