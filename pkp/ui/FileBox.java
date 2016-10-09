/**
 * Copyright 2015 Pushkar Piggott
 *
 * FileBox.java
 */

package pkp.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import java.io.File;
import pkp.ui.ExtensionFileFilter;
import pkp.io.Io;
import pkp.util.Log;

//////////////////////////////////////////////////////////////////////
public class FileBox 
   extends Box
   implements ActionListener {

   ///////////////////////////////////////////////////////////////////
   public FileBox(String buttonLabel, File file, File defaultFile, String prompt, String ext) {
      super(BoxLayout.LINE_AXIS);
      m_ButtonLabel = buttonLabel;
      m_DefaultFile = defaultFile;
      m_File = file == null ? m_DefaultFile : file;
      m_Prompt = prompt;
      m_Extension = ext;
      setOpaque(false);
      setAlignmentX(Component.LEFT_ALIGNMENT);
      JButton b = new JButton(buttonLabel);
      b.setMargin(new Insets(0, 5, 0, 5));
      b.addActionListener(this);
      add(b);
      add(Box.createHorizontalGlue());
      m_FileLabel = new JLabel();
      add(m_FileLabel);
      add(Box.createRigidArea(new Dimension(5, 0)));
      b = new JButton(sm_CLEAR);
      b.setPreferredSize(new Dimension(15, 15));
      b.setMargin(new Insets(0, 0, 0, 0));
      b.addActionListener(this);
      add(b);
      setFileLabel();
   }

   ///////////////////////////////////////////////////////////////////
   public File getFile() {
      return m_File;
   }

   ///////////////////////////////////////////////////////////////////
   @Override
   public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand() == m_ButtonLabel) {
         chooseFile();
         return;
      }
      switch (e.getActionCommand()) {
      default:
         Log.err("FileBox unexpected command: " + e.getActionCommand());
         return;
      case "CancelSelection":
         return;
      case "ApproveSelection":
         if (m_FileChooser.getSelectedFile() != null) {
            m_File = Io.asRelative(m_FileChooser.getSelectedFile());
            setFileLabel();
         }
         break;
      case sm_CLEAR:
         m_File = m_DefaultFile;
         break;
      }
      setFileLabel();
   }

   // Private /////////////////////////////////////////////////////////////////

   ///////////////////////////////////////////////////////////////////
   private void chooseFile() {
      File dir = null;
      if (m_File != null) {
         if (m_File.isDirectory()) {
            dir = m_File;
         } else {
            dir = m_File.getParentFile();
         }
      }
      m_FileChooser = new JFileChooser(dir == null || !dir.exists()
                                       ? new File(".")
                                       : dir);
      m_FileChooser.setDialogTitle(m_Prompt);
      ExtensionFileFilter.addFileFilter(m_FileChooser, m_Extension);
      m_FileChooser.addActionListener(this);
      m_FileChooser.showDialog(null, "OK");
   }

   ////////////////////////////////////////////////////////////////////////////
   private void setFileLabel() {
      m_FileLabel.setText(m_File == null || m_File.getPath() == ""
                          ? "unset"
                          : m_File.getName());
   }

   // Data /////////////////////////////////////////////////////////////////////
   private static final String sm_CLEAR = "x";

   private final String m_ButtonLabel;
   private final String m_Prompt;
   private final String m_Extension;
   private final File m_DefaultFile;

   private JLabel m_FileLabel;
   private JFileChooser m_FileChooser;
   private File m_File;
}
