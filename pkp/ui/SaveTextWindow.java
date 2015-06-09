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
      super(title, str);
      m_FileChooser = new JFileChooser();
      m_ButtonText = "Save As...";
      m_Extension = new ArrayList<String>();
      m_Extension.add("txt");
      m_Dir = ".";
      m_Saver = null;
      m_ChoosenFileUser = null;
      m_Button = new JButton();
      m_Button.addActionListener(this);
      JPanel p = new JPanel(new FlowLayout(FlowLayout.TRAILING));
      p.add(m_Button);
      getContentPane().add(p, BorderLayout.PAGE_END);
   }

   ///////////////////////////////////////////////////////////////////
   public void setButtonText(String txt) { m_ButtonText = txt; }
   public void setDirectory(String dir) { m_Dir = dir; }
   public String getDirectory() { return m_Dir; }
   public void setSaver(Saver fs) { m_Saver = fs; } 
   public void setChoosenFileUser(ChoosenFileUser cfu) { m_ChoosenFileUser = cfu; } 
   public JFileChooser getFileChooser() { return m_FileChooser; }

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
      if (visible) {
         m_Button.setText(m_ButtonText);
      }
      super.setVisible(visible);      
   }
   
   // ActionListener //////////////////////////////////////////////////////////
   @Override
   public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand() == m_ButtonText) {
         makeChooser();
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
         Log.err("SaveTextWindow: Unknown action \"" + e.getActionCommand() + '"');
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public void makeChooser() {
      m_FileChooser.addActionListener(this);
      if (!Io.dirExists(m_Dir)) {
         m_Dir = ".";
      }
      m_FileChooser.setCurrentDirectory(new File(m_Dir));
      m_FileChooser.removeChoosableFileFilter(m_FileChooser.getAcceptAllFileFilter());
      for (int i = 0; i < m_Extension.size(); ++i) {
         m_FileChooser.addChoosableFileFilter(new ExtensionFileFilter(m_Extension.get(i)));
      }
      m_FileChooser.addChoosableFileFilter(m_FileChooser.getAcceptAllFileFilter());
      m_FileChooser.showSaveDialog(null);
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
   private String m_ButtonText;
   private JButton m_Button;
   private ArrayList<String> m_Extension;
   private String m_Dir;
   private Saver m_Saver;
   private ChoosenFileUser m_ChoosenFileUser;
}
