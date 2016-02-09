/**
 * Copyright 2015 Pushkar Piggott
 *
 *  SaveTextWindow.java
 */
package pkp.ui;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.BorderLayout;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import pkp.ui.ExtensionFileFilter;
import pkp.util.Log;
import pkp.io.Io;

///////////////////////////////////////////////////////////////////////////////
public class SaveTextWindow extends TextWindow implements ActionListener {

   ////////////////////////////////////////////////////////////////////////////
   public interface Saver {
      public void fileChosen(JFileChooser fc);
   }

   ////////////////////////////////////////////////////////////////////////////
   public interface ChoosenFileUser {
      public void setFileChooser(JFileChooser fc);
   }

   ////////////////////////////////////////////////////////////////////////////
   public SaveTextWindow(String title, String str) {
      this(title, str, "txt");
   }

   ////////////////////////////////////////////////////////////////////////////
   public SaveTextWindow(String title, String str, String ext) {
      super(title, str);
      m_FileChooser = null;
      m_Extension = new ArrayList<String>();
      m_Extension.add(ext);
      m_Dir = ".";
      m_Saver = null;
      m_ChoosenFileUser = null;
      m_Button = new JButton("Save As...");
      m_Button.addActionListener(this);
      m_ButtonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
      getContentPane().add(m_ButtonPanel, BorderLayout.PAGE_END);
   }

   ///////////////////////////////////////////////////////////////////
   public void setButton(JButton b) { m_Button = b; }
   public void setDirectory(String dir) { m_Dir = dir; }
   public String getDirectory() { return m_Dir; }
   public void setSaver(Saver fs) { m_Saver = fs; } 
   public void setChoosenFileUser(ChoosenFileUser cfu) { m_ChoosenFileUser = cfu; } 
//   public JFileChooser getFileChooser() { return m_FileChooser; }

   ///////////////////////////////////////////////////////////////////
   public void setExtension(String ext) {
      if (m_Extension.size() != 1) {
         Log.err("SaveTextWindow: setExtension() expects only one extension");
      }
      m_Extension.set(0, ext); 
   }

   ///////////////////////////////////////////////////////////////////
   public void addExtension(String ext) {
      m_Extension.add(ext); 
   }

   ///////////////////////////////////////////////////////////////////
   @Override
   public void setVisible(boolean visible) { 
      if (visible && m_Button != null) {
         m_ButtonPanel.add(m_Button);
         m_Command = m_Button.getText();
         m_Button = null;
      }
      super.setVisible(visible);      
   }
   
   // ActionListener //////////////////////////////////////////////////////////
   @Override
   public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand() == m_Command) {
         if (m_FileChooser == null) {
            makeChooser();
         }
         m_FileChooser.showSaveDialog(null);
      } else if (e.getActionCommand() == "ApproveSelection") {
         if (m_Saver != null) {
            m_Saver.fileChosen(m_FileChooser);
            return;
         }
         File f = m_FileChooser.getSelectedFile();
         if (m_FileChooser.getFileFilter() instanceof ExtensionFileFilter) {
            f = ((ExtensionFileFilter)m_FileChooser.getFileFilter()).withExtension(f);
         }
         if (!f.exists()
          || JOptionPane.showConfirmDialog(
               m_FileChooser, 
               "\"" + f.getPath() + "\" exists, overwrite?", 
               "File Exists", 
               JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            write(f);
            if (m_ChoosenFileUser != null) {
               m_ChoosenFileUser.setFileChooser(m_FileChooser);
            }
         }
      } else if (e.getActionCommand() != "CancelSelection") {
         // ignore other actions
         //Log.err("SaveTextWindow: Unknown action \"" + e.getActionCommand() + '"');
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public void makeChooser() {
      m_FileChooser = new JFileChooser();
      m_FileChooser.addActionListener(this);
      if (!Io.dirExists(m_Dir)) {
         m_Dir = ".";
      }
      m_FileChooser.setCurrentDirectory(new File(m_Dir));
      ExtensionFileFilter.setFileFilters(m_FileChooser, m_Extension);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public void write(File f) {
      try {
         BufferedWriter bw = new BufferedWriter(new FileWriter(f));
         bw.write(getText());
			bw.flush();
         bw.close();
      } catch (IOException e) {
         Log.warn("failed to write to \"" + f.getPath() + "\".");
         return;
      }
   }

   // Data ////////////////////////////////////////////////////////////////////
   private JFileChooser m_FileChooser;
   private JPanel m_ButtonPanel;
   private JButton m_Button;
   private String m_Command;
   private ArrayList<String> m_Extension;
   private String m_Dir;
   private Saver m_Saver;
   private ChoosenFileUser m_ChoosenFileUser;
}
