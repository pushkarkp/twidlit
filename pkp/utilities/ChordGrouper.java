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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.io.File;
import pkp.twiddle.Assignments;
import pkp.twiddle.Chord;
import pkp.ui.ControlDialog;
import pkp.ui.FileBox;
import pkp.ui.LabelComponentBox;
import pkp.ui.HtmlWindow;
import pkp.io.Io;
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
         value <<= 4;
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
      JLabel label = new JLabel("Optionally choose a map file.");
      label.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(label);
      box.add(Box.createRigidArea(new Dimension(0, 4)));
      m_MapFileBox = new 
         FileBox(sm_MAP, Persist.getFile(sm_FILE_PERSIST, defaultMapFile),
                 defaultMapFile, "Choose a map file", "cfg.chords", "none");
      m_MapFileBox.setToggle(true, Persist.getBool(sm_X_FLIP_PERSIST));
      m_MapFileBox.setActionListener(this, sm_FILE_BOX_UPDATE);
      box.add(m_MapFileBox);
      box.add(Box.createVerticalGlue());

      m_FreeButtonGroup = new ButtonGroup();
      m_Used = createRadioButton(sm_USED, m_FreeButtonGroup);
      box.add(m_Used);
      m_List = createCheckBox("List mappings", Persist.getBool(sm_GROUP_LIST_PERSIST));
      box.add(indent("   ", m_List));
      m_Free = createRadioButton(sm_FREE, m_FreeButtonGroup);
      box.add(m_Free);
      box.add(Box.createVerticalGlue());
      box.add(Box.createVerticalGlue());

      m_FingerButtonGroup = new ButtonGroup();
      m_GroupByMask = createRadioButton(sm_GROUP_BY, m_FingerButtonGroup);
      m_GroupByMask.addActionListener(this);
      m_GroupText = createTextField(sm_GROUP_MASK_PERSIST, "");
      box.add(createTextFieldBox(m_GroupByMask, m_GroupText, null));
      box.add(Box.createRigidArea(new Dimension(0, 2)));
      m_Generate = createRadioButton(sm_GENERATE, m_FingerButtonGroup);
      m_Generate.addActionListener(this);
      m_FixedText = createTextField(sm_GENERATE_FIXED_PERSIST, ".");
      m_AcceptText = createTextField(sm_GENERATE_ACCEPT_PERSIST, ".");
      box.add(createTextFieldBox(m_Generate, m_FixedText, m_AcceptText));
      box.add(Box.createVerticalGlue());

      m_MinGroup = new SpinnerNumberModel(Persist.getInt(sm_MINIMUM_PERSIST, 1), 1, Chord.sm_VALUES, 1);
      JSpinner spin = new JSpinner(m_MinGroup);
      addMargin(0, 3, spin);
      spin.setMaximumSize(spin.getPreferredSize());
      m_MinBox = new LabelComponentBox("Minimum size:", spin);
      m_MinBox.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(indent("     ", m_MinBox));

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
      case sm_GROUP_BY:
      case sm_GENERATE:
         enableGenerate(e.getActionCommand() == sm_GENERATE);
         return;
      case sm_OK:
         File mapFile = m_MapFileBox.getFile();
         Persist.set(sm_FILE_PERSIST, m_MapFileBox.getFile());
         Persist.set(sm_X_FLIP_PERSIST, m_MapFileBox.isToggled());
         Persist.set(sm_GROUP_MASK_PERSIST, m_GroupText.getText());
         Persist.set(sm_GENERATE_FIXED_PERSIST, m_FixedText.getText());
         Persist.set(sm_GENERATE_ACCEPT_PERSIST, m_AcceptText.getText());
         Persist.set(sm_MINIMUM_PERSIST, m_MinGroup.getNumber().intValue());
         persist(m_Used, 1);
         persist(m_Free, 1);
         persist(m_GroupByMask, 2);
         persist(m_Generate, 2);
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
         HtmlWindow hw = new HtmlWindow(getClass().getResource("/data/ref.html") + "#map");
         hw.setTitle(sm_HELP);
         hw.setVisible(true);
         return;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // ContentForTitle
   public String getContentForTitle(String title) {
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
      String str = "";
      str += "# Twidlit Chord Grouping at " + df.format(Calendar.getInstance().getTime()) + '\n';
      if (m_MapFileBox.getFile() == null) {
         str += "# All chords\n";
      } else {
         str += "# " + m_MapFileBox.getFile().getPath() + '\n';
         str += "# " + (m_Free.isSelected() ? "Free" : "Used") + " chords\n";
      }

      Assignments asgs = m_MapFileBox.getFile() == null
                       ? new Assignments()
                       : new Assignments(m_MapFileBox.getFile());
      if (m_GroupByMask.isSelected()) {
         ChordGroup group = new
            ChordGroup(stringToMask(m_GroupText.getText()),
                       asgs,
                       m_Free.isSelected(),
                       m_List.isSelected());
         return str + '\n' + group.toString();
      } else {
         str += "# Generated from " + m_FixedText.getText() + ' ' + m_AcceptText.getText() + '\n';
         if (m_MinGroup.getNumber().intValue() > 1) {
            str += "# Minimum group size " + m_MinGroup.getNumber().intValue() + '\n';
         }
         ChordGroups groups = new
            ChordGroups(stringToMask(m_FixedText.getText()),
                        stringToMask(m_AcceptText.getText()),
                        asgs,
                        m_Free.isSelected(),
                        m_List.isSelected());
         return str + '\n' + groups.toString(m_MinGroup.getNumber().intValue());
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
   private JCheckBox createCheckBox(String label, boolean check) {
      JCheckBox cb = new JCheckBox(label, check);
      cb.setOpaque(false);
      cb.setAlignmentX(Component.LEFT_ALIGNMENT);
      return cb;
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
   private static JTextField createTextField(String pName, String extraChars) {
      JFormattedTextField txt = new JFormattedTextField(createFormatter(getMaskChars() + extraChars));
      txt.setFont(new Font("monospaced", Font.PLAIN, txt.getFont().getSize()));
      txt.setHorizontalAlignment(JTextField.RIGHT);
      txt.setValue(Persist.get(pName, "????"));
      addMargin(8, 2, txt);
      txt.setMaximumSize(txt.getPreferredSize());
      return txt;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static Box createTextFieldBox(JRadioButton rb, JTextField tf1, JTextField tf2) {
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
   private void handleNoFile() {
      if (m_MapFileBox.getFile() != null) {
         m_Used.setEnabled(true);
         m_List.setEnabled(true);
      } else {
         m_Used.setEnabled(false);
         m_List.setEnabled(false);
         m_Free.setSelected(true);
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private void enableGenerate(boolean enable) {
      m_GroupText.setEnabled(!enable);
      m_FixedText.setEnabled(enable);
      m_AcceptText.setEnabled(enable);
      m_MinBox.setEnabled(enable);
   }

   /////////////////////////////////////////////////////////////////////////////
   private static final String sm_MAP = "Map";
   private static final String sm_FREE = "Group free chords";
   private static final String sm_USED = "Group used chords";
   private static final String sm_GROUP_BY = "Group by pattern";
   private static final String sm_GENERATE = "Generate patterns";
   private static final String sm_OK = "OK";
   private static final String sm_CANCEL = "Cancel";
   private static final String sm_HELP = "Help";
   
   private static final String sm_FILE_PERSIST = "#.group.chords.file";
   // group free is saved by PersistentFrame
   private static final String sm_GROUP_MASK_PERSIST = "#.group.chords.by.mask";
   private static final String sm_GROUP_LIST_PERSIST = "#.group.chords.list.mappings";
   private static final String sm_GENERATE_FIXED_PERSIST = "#.group.chords.fixed.mask";
   private static final String sm_GENERATE_ACCEPT_PERSIST = "#.group.chords.accept.mask";
   private static final String sm_MINIMUM_PERSIST = "#.group.chords.minimum";
   private static final String sm_X_FLIP_PERSIST = "#.group.chords.x.toggle";

   private static final String sm_FILE_BOX_UPDATE = "FileBoxUpdate";
  
   private FileBox m_MapFileBox;
   private ButtonGroup m_FreeButtonGroup;
   private JRadioButton m_Used;
   private JCheckBox m_List;
   private JRadioButton m_Free;
   private ButtonGroup m_FingerButtonGroup;
   private JRadioButton m_GroupByMask;
   private JRadioButton m_Generate;
   private JTextField m_GroupText;
   private JTextField m_FixedText;
   private JTextField m_AcceptText;
   private SpinnerNumberModel m_MinGroup;
   private LabelComponentBox m_MinBox;
}