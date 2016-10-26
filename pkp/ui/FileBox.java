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
   public FileBox(String buttonLabel, File file, String prompt, String ext) {
      this(buttonLabel, file, null, prompt, ext, null);
   }

   ///////////////////////////////////////////////////////////////////
   public FileBox(String buttonLabel, File file, File defaultFile, String prompt, String ext) {
      this(buttonLabel, file, defaultFile, prompt, ext, null);
   }

   ///////////////////////////////////////////////////////////////////
   public FileBox(String buttonLabel, File file, String prompt, String ext, String defaultLabel) {
      this(buttonLabel, file, null, prompt, ext, defaultLabel);
   }

   ///////////////////////////////////////////////////////////////////
   public FileBox(String buttonLabel, File file, File defaultFile, String prompt, String ext, String defaultLabel) {
      super(BoxLayout.LINE_AXIS);
      m_ButtonLabel = buttonLabel;
      m_DefaultFile = Io.fileExists(defaultFile) ? defaultFile : null;
      m_File = Io.fileExists(file) ? file : m_DefaultFile;
      m_Prompt = prompt;
      m_DefaultLabel = defaultLabel != null ? defaultLabel
                     : m_DefaultFile != null ? m_DefaultFile.getPath()
                     : "unset";
      m_Toggle = false;
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
   public void setToggle(boolean set) {
      m_Toggle = set;
   }

   ///////////////////////////////////////////////////////////////////
   public void setActionListener(ActionListener al, String c) {
      m_ActionListener = al;
      m_Command = c;
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
         m_File = m_Toggle && m_File == m_DefaultFile
                ? null
                : m_DefaultFile;
         break;
      }
      setFileLabel();
      if (m_ActionListener != null) {
         m_ActionListener.actionPerformed(new ActionEvent(this, 0, m_Command));
      }
   }

   // Private /////////////////////////////////////////////////////////////////

   ///////////////////////////////////////////////////////////////////
   private void chooseFile() {
      m_FileChooser = new JFileChooser(Io.dirExists(m_File)
                                       ? m_File
                                       : m_File != null && Io.dirExists(m_File.getParentFile())
                                        ? m_File.getParentFile()
                                        : new File("."));
      m_FileChooser.setDialogTitle(m_Prompt);
      ExtensionFileFilter.addFileFilter(m_FileChooser, m_Extension);
      m_FileChooser.addActionListener(this);
      m_FileChooser.showDialog(null, "OK");
   }

   ////////////////////////////////////////////////////////////////////////////
   private void setFileLabel() {
      m_FileLabel.setText(Io.fileExists(m_File)
                          ? m_File.getName()
                          : m_DefaultLabel);
   }

   // Data /////////////////////////////////////////////////////////////////////
   private static final String sm_CLEAR = "x";

   private final String m_ButtonLabel;
   private final String m_Prompt;
   private final String m_Extension;
   private final File m_DefaultFile;
   private final String m_DefaultLabel;

   private JLabel m_FileLabel;
   private JFileChooser m_FileChooser;
   private File m_File;
   private boolean m_Toggle;
   private ActionListener m_ActionListener;
   private String m_Command;
}
