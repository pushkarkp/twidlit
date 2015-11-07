/**
 * Copyright 2015 Pushkar Piggott
 *
 * ChordMapper.java
 */

package pkp.twidlit;

import java.awt.Font;
import java.awt.Window;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import java.io.File;
import java.util.ArrayList;
import pkp.twiddle.Twiddle;
import pkp.twiddle.Chord;
import pkp.twiddle.KeyPress;
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
   ChordMapper(Window owner) {
      super(owner, "Map Chords");
//      setModal(true);
      setResizable(true);
      m_MappedFile = Persist.getFile("mapped.chords.file");
      m_ChordsFile = Persist.getFile("map.chords.file");
      m_CharsFile = Persist.getFile("map.chars.file");
      m_GotEnter = false;
      m_ExistingTwiddles = (ArrayList<Integer>[])new ArrayList[Chord.sm_VALUES];
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         m_ExistingTwiddles[i] = new ArrayList<Integer>();
      }
      Box box = getBox();
      JLabel label = new JLabel("<html>Select a file of chords and a file of characters to combine.</html>");
      label.setAlignmentX(Component.CENTER_ALIGNMENT);
      box.add(label);
      box.add(Box.createVerticalGlue());
      Box fileBox = createButtonLabelBox(sm_CHORDS);
      m_ChordsFileLabel = (JLabel)fileBox.getComponent(2);
      setLabel(m_ChordsFileLabel, m_ChordsFile);
      box.add(fileBox);
      box.add(Box.createVerticalGlue());
      fileBox = createButtonLabelBox(sm_CHARS);
      m_CharsFileLabel = (JLabel)fileBox.getComponent(2);
      setLabel(m_CharsFileLabel, m_CharsFile);
      box.add(fileBox);
      box.add(Box.createVerticalGlue());
      box.add(Box.createVerticalGlue());
      label = new JLabel("<html>Optionally also select a file of existing mappings to include.</html>");
      label.setAlignmentX(Component.CENTER_ALIGNMENT);
      box.add(label);
      box.add(Box.createVerticalGlue());
      fileBox = createButtonLabelBox(sm_MAPPED);
      m_MappedFileLabel = (JLabel)fileBox.getComponent(2);
      setLabel(m_MappedFileLabel, m_MappedFile);
      box.add(fileBox);
      addButton(createButton(sm_HELP));
      addButton(createButton(sm_CANCEL));
      addButton(createButton(sm_OK));
      setVisible(true);
   }
   
   ///////////////////////////////////////////////////////////////////
   @Override 
   public void actionPerformed(ActionEvent e) {
      switch (e.getActionCommand()) {
      case sm_MAPPED: {
         File f = chooseFile("mapped", m_MappedFile, "cfg.txt");
         if (f != null) {
            m_MappedFile = f;
            setLabel(m_MappedFileLabel, m_MappedFile);
         }
         return;
      }
      case sm_CHORDS: {
         File f = chooseFile("chords", m_ChordsFile, "cfg.txt");
         if (f != null) {
            m_ChordsFile = f;
            setLabel(m_ChordsFileLabel, m_ChordsFile);
         }
         return;
      }
      case sm_CHARS: {
         File f = chooseFile("characters", m_CharsFile, "counts");
         if (f != null) {
            m_CharsFile = f;
            setLabel(m_CharsFileLabel, m_CharsFile);
         }
         return;
      }
      case sm_OK:
         if (m_ChordsFile == null || m_CharsFile == null) {
            Log.warn("Mapping requires both a chords and a characters files.");
            return;
         }
         Persist.set("map.chords.file", m_ChordsFile.getPath());
         Persist.set("map.chars.file", m_CharsFile.getPath());
         if (m_MappedFile != null) {
            Persist.set("mapped.chords.file", m_MappedFile.getPath());
         }
         SaveTextWindow stw = new SaveTextWindow(
            "Chord Mappings", 
            map(m_MappedFile, m_ChordsFile, m_CharsFile), 
            "cfg.txt");
//         setDirectory(dir);
         stw.setPersistName("chord.list");
         Font font = stw.getFont();
         stw.setFont(new Font("monospaced", font.getStyle(), font.getSize()));
//         stw.setSaver(new CfgSaver(command));
         stw.setExtension("cfg.txt");
         stw.setVisible(true);
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
      File dir = (f == null)
                  ? null
                  : (f.isDirectory())
                     ? f
                     : new File(f.getParent());
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
            Log.err("ChooserActionListener unexpected command " + e.getActionCommand());
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
      JButton b = createButton(buttonLabel);
      b.setBackground(Pref.getColor("background.color"));
      box.add(b);
      box.add(Box.createHorizontalGlue());
      JLabel label = new JLabel();
      box.add(label);
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
   
   ////////////////////////////////////////////////////////////////////////////
   private enum Target { MAPPED, CHORD, CHARS };
   
   ////////////////////////////////////////////////////////////////////////////
   private String map(File mappedF, File chordF, File charsF) {
      String str = "";
      if (mappedF != null && !mappedF.isDirectory()) {
         LineReader mappedLr = new LineReader(Io.toExistUrl(mappedF), Io.sm_MUST_EXIST);
         for (;;) {
            String mapped = getStr(mappedLr, Target.MAPPED);
            if (mapped == null) {
               break;
            }
            str += mapped + "\n";
         }
         mappedLr.close();
      }
      LineReader chordLr = new LineReader(Io.toExistUrl(chordF), Io.sm_MUST_EXIST);
      LineReader charsLr = new LineReader(Io.toExistUrl(charsF), Io.sm_MUST_EXIST);
      for (;;) {
         String chord = getStr(chordLr, Target.CHORD);
         String chars = getStr(charsLr, Target.CHARS);
         if (chord == null || chars == null) {
            break;
         }
         str += chord + " = " + chars + "\n";
      }
      chordLr.close();
      charsLr.close();
      return str;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   private String getStr(LineReader lr, Target target) {
      String line;
      for (;;) {
         line = lr.readLine();
         if (line == null) {
            return null;
         }
         String str = "";
         if (target == Target.CHARS) {
            str = getChars(line, lr);
         } else if (line.length() < 6) {
            Log.log(String.format("failed to parse \"%s\" on line %d of \"%s\"",
                                  line, lr.getLineNumber(), lr.getPath()));
            str = "";
         } else {
            Twiddle t = new Twiddle(line);
            if (!t.getChord().isValid()) {
               Log.log(String.format("failed to parse \"%s\" on line %d of \"%s\"",
                                     line, lr.getLineNumber(), lr.getPath()));
               str = "";
            } else if (!isNew(t)) {
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
      int space = Io.findFirstOf(line.substring(initial), Io.sm_WS);
      if (initial == space) {
         Log.log(String.format("initial |%c|", line.charAt(initial)));
         Log.log(String.format("failed to parse \"%s\" on line %d of \"%s\"",
                               line, lr.getLineNumber(), lr.getPath()));

         return "";
      }
      String chars = Io.parseEscaped(line.substring(initial, initial + space));
      if (isEnter(chars.charAt(0))
       || (chars.length() == 2 && isEnter(chars.charAt(1)))) {
         if (chars.length() == 1 
          || (isEnter(chars.charAt(0)) && isEnter(chars.charAt(1)))) {
            if (m_GotEnter) {
               return "";
            } else {
               m_GotEnter = true;
               return "<Enter>";
            }
         } else
         if (isEnter(chars.charAt(0))) {
            return "<Enter>" + KeyPress.parseText(chars.charAt(1)).toString(KeyPress.Format.ESCAPED);
         } else {
            return KeyPress.parseText(chars.charAt(0)).toString(KeyPress.Format.ESCAPED) + "<Enter>";
         }
      } else {
         return KeyPress.parseText(chars, KeyPress.Format.ESCAPED);
      }
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
   
   // Data /////////////////////////////////////////////////////////////////////
   private static final String sm_OK = "OK";
   private static final String sm_CANCEL = "Cancel";
   private static final String sm_HELP = "Help";
   private static final String sm_MAPPED = "Mapped";
   private static final String sm_CHORDS = "Chords";
   private static final String sm_CHARS = "Characters";

   private static final int sm_OFFSET[][] = new int[][]{{0, 3}, {4, 7}};
   
   private JFileChooser m_FileChooser;
   private JLabel m_MappedFileLabel;
   private JLabel m_ChordsFileLabel;
   private JLabel m_CharsFileLabel;
   private File m_MappedFile;
   private File m_ChordsFile;
   private File m_CharsFile;
   private boolean m_GotEnter;
   private ArrayList<Integer>[] m_ExistingTwiddles;
}

