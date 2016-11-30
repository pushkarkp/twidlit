/**
 * Copyright 2016 Pushkar Piggott
 *
 * ChordGrouper.java
 */

package pkp.utilities;

import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.text.MaskFormatter;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.io.File;
import pkp.twiddle.Chord;
import pkp.ui.ControlDialog;
import pkp.ui.FileBox;
import pkp.ui.LabelComponentBox;
import pkp.ui.HtmlWindow;
import pkp.io.Io;
import pkp.io.LineReader;
import pkp.util.Persist;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
public class ChordGrouper extends ControlDialog 
   implements ActionListener, SaveChordsWindow.ContentForTitle {

   /////////////////////////////////////////////////////////////////////////////
   public static String getMaskChars() {
      return "0123456789abcdefrmlABCDEF|-,'?";
   }

   /////////////////////////////////////////////////////////////////////////////
   public static int stringToMask(String txt) {
      int value = 0;
      for (int finger = 0; finger < 4; ++finger) {
         value <<= ChordGroup.sm_MaskShift;
         final int f = charToMask(txt.charAt(finger));
         if (f == -1) {
            Log.warn("Bad character '" + txt.charAt(finger) + "' in mask \"" + txt + '"'); 
            return 0;
         }
         value += f;
      }
      return value;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static int charToMask(char c) {
      switch (c) {
      case '.': return 0;
      case 'a': case 'A': return 0xA;
      case 'b': case 'B': return 0xB;
      case 'c': case 'C': return 0xC;
      case 'd': case 'D': return 0xD;
      case 'e': case 'E': return 0xE;
      case 'f': case 'F': case '?': return 0xF;
      }
      // 0 == no buttons (1) in 0MRL
      final int v = (int)(c - '0');
      if (1 <= v && v <= 9) {
         return v;
      }
      return ChordGroup.sm_Maskable[Chord.charToButton(c)];
   }

   /////////////////////////////////////////////////////////////////////////////
   public ChordGrouper(Window owner, File mapFile) {
      super(owner, "Group Chords");
      setResizable(true);
      File defaultMapFile = Io.fileExists(mapFile)
                          ? mapFile
                          : null;
      Box box = getBox();
      JLabel label = new JLabel("Optionally choose a chords file.");
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(label);
      box.add(Box.createRigidArea(new Dimension(0, 4)));
      m_ChordsFileBox = new 
         FileBox(sm_CHORDS, Persist.getFile(sm_FILE_PERSIST, defaultMapFile),
                 defaultMapFile, "Choose a chords file", "chords", "none");
      m_ChordsFileBox.setCanToggle(true, Persist.getBool(sm_X_TOGGLED_PERSIST));
      m_ChordsFileBox.setActionListener(this, sm_FILE_BOX_UPDATE);
      box.add(m_ChordsFileBox);
      box.add(Box.createVerticalGlue());

      m_FreeButtonGroup = new ButtonGroup();
      m_Used = createRadioButton(sm_USED, m_FreeButtonGroup);
      m_Used.addActionListener(this);
      box.add(m_Used);
      m_ShowText = createCheckBox("Show text");
      box.add(indent("   ", m_ShowText));
      m_GroupFree = createRadioButton(sm_FREE, m_FreeButtonGroup);
      m_GroupFree.addActionListener(this);
      box.add(m_GroupFree);
      box.add(Box.createRigidArea(new Dimension(0, 4)));
      box.add(Box.createVerticalGlue());

      m_FingerButtonGroup = new ButtonGroup();
      m_GroupByMask = createRadioButton(sm_GROUP_BY, m_FingerButtonGroup);
      m_GroupByMask.addActionListener(this);
      m_GroupText = createTextField(sm_GROUP_MASK_PERSIST, getMaskChars());
      box.add(createRadioButtonTextFieldBox(m_GroupByMask, m_GroupText, null));
      box.add(Box.createRigidArea(new Dimension(0, 2)));
      m_Generate = createRadioButton(sm_GENERATE, m_FingerButtonGroup);
      m_Generate.addActionListener(this);
      m_FixedText = createTextField(sm_GENERATE_FIXED_PERSIST, getMaskChars() + ".");
      m_AcceptText = createTextField(sm_GENERATE_ACCEPT_PERSIST, getMaskChars() + ".");
      box.add(createRadioButtonTextFieldBox(m_Generate, m_FixedText, m_AcceptText));
      box.add(Box.createRigidArea(new Dimension(0, 2)));
      m_MinGroup = new SpinnerNumberModel(Persist.getInt(sm_MINIMUM_PERSIST, 1), 1, Chord.sm_VALUES, 1);
      JSpinner spin = new JSpinner(m_MinGroup);
      addMargin(0, 3, spin);
      spin.setMaximumSize(spin.getPreferredSize());
      m_MinBox = new LabelComponentBox("Minimum group size", spin);
      m_MinBox.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(indent("     ", m_MinBox));
      box.add(Box.createVerticalGlue());

      m_Priority = createTextField(sm_PRIORITY_PERSIST, "1234");
      LabelComponentBox priorityBox = new LabelComponentBox(sm_PRIORITY, m_Priority);
      priorityBox.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(priorityBox);

      handleNoFile();
      enableGenerate(m_Generate.isSelected());

      addButton(createButton(sm_OK));
      addButton(createButton(sm_CANCEL));
      addButton(createButton(sm_HELP));
      pack();
      setVisible(true);
   }

   ///////////////////////////////////////////////////////////////////
   @Override // ActionListener
   public void actionPerformed(ActionEvent e) {
      switch (e.getActionCommand()) {
      case sm_FILE_BOX_UPDATE:
         handleNoFile();
         return;
      case sm_USED:
      case sm_FREE:
         m_ShowText.setEnabled(e.getActionCommand() == sm_USED);
         return;
      case sm_GROUP_BY:
      case sm_GENERATE:
         enableGenerate(e.getActionCommand() == sm_GENERATE);
         return;
      case sm_OK:
         if (hasDuplicates(m_Priority.getText())) {
            m_Priority.grabFocus();
            return;
         }
         if (hasMatchingDots(m_FixedText.getText(), m_AcceptText.getText())) {
            m_FixedText.grabFocus();
            return;
         }
         Persist.set(sm_FILE_PERSIST, m_ChordsFileBox.getFile());
         Persist.set(sm_X_TOGGLED_PERSIST, m_ChordsFileBox.isToggled());
         Persist.set(sm_GROUP_MASK_PERSIST, m_GroupText.getText());
         Persist.set(sm_GENERATE_FIXED_PERSIST, m_FixedText.getText());
         Persist.set(sm_GENERATE_ACCEPT_PERSIST, m_AcceptText.getText());
         Persist.set(sm_MINIMUM_PERSIST, m_MinGroup.getNumber().intValue());
         persist(m_Used, 1);
         persist(m_ShowText);
         persist(m_GroupFree, 1);
         persist(m_GroupByMask, 2);
         persist(m_Generate, 2);
         Persist.set(sm_PRIORITY_PERSIST, m_Priority.getText());
         SaveChordsWindow scw = new
         SaveChordsWindow(this, "Chord Groups", "txt");
         scw.setPersistName("#.chord.groups");
         scw.setVisible(true);
         //return;
      case sm_CANCEL:
         setVisible(false);
         dispose();
         return;
      case sm_HELP:
         HtmlWindow hw = new HtmlWindow(getClass().getResource("/data/ref.html") + "#group");
         hw.setTitle(sm_HELP);
         hw.setVisible(true);
         return;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // ContentForTitle
   public String getContentForTitle(String title) {
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
      String head = "";
      head += "# Twidlit Chord Grouping at " + df.format(Calendar.getInstance().getTime()) + '\n';

      ChordText chordText = new 
         ChordText(m_ChordsFileBox.getFile() != null
                 ? new LineReader(Io.toUrl(m_ChordsFileBox.getFile()))
                 : null);

      if (m_ChordsFileBox.getFile() == null) {
         head += "# All chords (255)\n";
      } else {
         head += "# " + m_ChordsFileBox.getFile().getPath() + '\n';
         head += "# " + (m_GroupFree.isSelected() ? "Free" : "Used") + " chords ("
              + String.valueOf((new ChordGroup(chordText, m_GroupFree.isSelected(), false, 0)).eligibleCount())
              + "/255)\n";
      }
      head += "# " + "Finger priority " + m_Priority.getText() + '\n';
      
      if (m_GroupByMask.isSelected()) {
         ChordGroup group = new
            ChordGroup(chordText,
                       m_GroupFree.isSelected(),
                       m_ShowText.isSelected(),
                       stringToMask(m_GroupText.getText()));
         return head + '\n' + group.toString(m_Priority.getText());
      } else {
         head += "# Generated from " + m_FixedText.getText() + ' ' + m_AcceptText.getText() + '\n';
         if (m_MinGroup.getNumber().intValue() > 1) {
            head += "# Minimum group size " + m_MinGroup.getNumber().intValue() + '\n';
         }
         ChordGroups groups = new
            ChordGroups(chordText,
                        m_GroupFree.isSelected(),
                        m_ShowText.isSelected(),
                        stringToMask(m_FixedText.getText()),
                        stringToMask(m_AcceptText.getText()),
                        m_Priority.getText());
         return head + '\n' + groups.toString(m_MinGroup.getNumber().intValue());
      }
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private static void addMargin(int w, int h, Component c) {
      c.setPreferredSize(addMargin(w, h, c.getPreferredSize()));
   }
   ////////////////////////////////////////////////////////////////////////////
   private static Dimension addMargin(int w, int h, Dimension d) {
      return new Dimension(d.width + w, d.height + h);
   }

   ////////////////////////////////////////////////////////////////////////////
   private JButton createButton(String label) {
      JButton b = new JButton(label);
      b.addActionListener(this);
      return b;
   }

   ////////////////////////////////////////////////////////////////////////////
   private Box indent(String i, Component c) {
      Box b = new Box(BoxLayout.LINE_AXIS);
      b.setAlignmentX(Component.LEFT_ALIGNMENT);
      b.setOpaque(false);
      b.add(new JLabel(i));
      b.add(c);
      return b;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static JTextField createTextField(String pName, String chars) {
      JFormattedTextField txt = new JFormattedTextField(createFormatter(chars));
      txt.setFont(new Font("monospaced", Font.PLAIN, txt.getFont().getSize()));
      txt.setHorizontalAlignment(JTextField.RIGHT);
      txt.setValue(Persist.get(pName, "????"));
      addMargin(8, 2, txt);
      txt.setMaximumSize(txt.getPreferredSize());
      return txt;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static Box createRadioButtonTextFieldBox(JRadioButton rb, JTextField tf1, JTextField tf2) {
      Box b = new Box(BoxLayout.LINE_AXIS);
      b.setAlignmentX(Component.LEFT_ALIGNMENT);
      b.setOpaque(false);
      b.add(rb);
      b.add(Box.createHorizontalGlue());
      b.add(tf1);
      if (tf2 != null) {
         b.add(tf2);
      }
      return b;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static AbstractFormatter createFormatter(String chars) {
      MaskFormatter f = null;
      try {
         f = new MaskFormatter("****");
      } catch (java.text.ParseException e) {
         Log.err("Text formatter is bad: " + e.getMessage());
      }
      f.setValidCharacters(chars);
      return f;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static boolean hasDuplicates(String txt) {
      boolean present[] = new boolean[4];
      for (int i = 0; i < txt.length(); ++i) {
         int value = txt.charAt(i) - '1';
         if (present[value]) {
            Log.warn(String.format("Repeated value %d in \"%s %s\".", value + 1, sm_GENERATE, txt));
            return true;
         }
         present[value] = true;
      }
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static boolean hasMatchingDots(String fix, String accept) {
      for (int i = 0; i < fix.length(); ++i) {
         if (fix.charAt(i) == '.' && accept.charAt(i) == '.') {
            Log.warn(String.format("Dots in the same finger in \"%s %s %s\".", sm_PRIORITY, fix, accept));
            return true;
         }
      }
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   private void handleNoFile() {
      if (m_ChordsFileBox.getFile() != null) {
         m_Used.setEnabled(true);
         m_ShowText.setEnabled(m_Used.isSelected());
      } else {
         m_Used.setEnabled(false);
         m_ShowText.setEnabled(false);
         m_GroupFree.setSelected(true);
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private void enableGenerate(boolean enable) {
      m_GroupText.setEnabled(!enable);
      m_FixedText.setEnabled(enable);
      m_AcceptText.setEnabled(enable);
      m_MinBox.setEnabled(enable);
   }

   // Data /////////////////////////////////////////////////////////////////////
   private static final String sm_CHORDS = "Chords";
   private static final String sm_FREE = "Group free chords";
   private static final String sm_USED = "Group used chords";
   private static final String sm_GROUP_BY = "Group by pattern";
   private static final String sm_GENERATE = "Generate patterns";
   private static final String sm_PRIORITY = "Finger priority";
   private static final String sm_OK = "OK";
   private static final String sm_CANCEL = "Cancel";
   private static final String sm_HELP = "Help";
   
   private static final String sm_FILE_PERSIST = "#.group.chords.file";
   private static final String sm_X_TOGGLED_PERSIST = "#.group.chords.x.toggled";
   // group free is saved by PersistentFrame
   private static final String sm_GROUP_MASK_PERSIST = "#.group.chords.by.mask";
   private static final String sm_GROUP_LIST_PERSIST = "#.group.chords.show.text";
   private static final String sm_GENERATE_FIXED_PERSIST = "#.group.chords.fixed.mask";
   private static final String sm_GENERATE_ACCEPT_PERSIST = "#.group.chords.accept.mask";
   private static final String sm_MINIMUM_PERSIST = "#.group.chords.minimum";
   private static final String sm_PRIORITY_PERSIST = "#.group.chords.finger priority";

   private static final String sm_FILE_BOX_UPDATE = "FileBoxUpdate";
  
   private FileBox m_ChordsFileBox;
   private ButtonGroup m_FreeButtonGroup;
   private JRadioButton m_Used;
   private JCheckBox m_ShowText;
   private JRadioButton m_GroupFree;
   private ButtonGroup m_FingerButtonGroup;
   private JRadioButton m_GroupByMask;
   private JRadioButton m_Generate;
   private JTextField m_GroupText;
   private JTextField m_FixedText;
   private JTextField m_AcceptText;
   private SpinnerNumberModel m_MinGroup;
   private LabelComponentBox m_MinBox;
   private JTextField m_Priority;
}
