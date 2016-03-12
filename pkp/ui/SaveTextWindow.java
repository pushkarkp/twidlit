/**
 * Copyright 2015 Pushkar Piggott
 *
 *  SaveTextWindow.java
 */
package pkp.ui;

import java.io.File;
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
   public static final String sm_SAVE_AS_TEXT = "Save As...";
   
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
      m_Buttons = new ArrayList<JButton>();
      m_OkButton = 0;
      m_ButtonsSet = false;
      m_Buttons.add(new JButton(sm_SAVE_AS_TEXT));
      m_Buttons.get(m_OkButton).addActionListener(this);
      m_ButtonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
      getContentPane().add(m_ButtonPanel, BorderLayout.PAGE_END);
   }

   ///////////////////////////////////////////////////////////////////
   public void addCorner0Button(JButton b) {
      ++m_OkButton;
      m_Buttons.add(0, b); 
   }

   ///////////////////////////////////////////////////////////////////
   public void setOkButton(JButton b) { m_Buttons.set(m_OkButton, b); }
   public void addButton(JButton b) { m_Buttons.add(b); }
   public JButton getButton(int i) { return m_Buttons.get(i); }
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
      if (visible && !m_ButtonsSet) {
         m_Command = m_Buttons.get(m_OkButton).getText();
         for (int i = m_Buttons.size() - 1; i >= 0; --i) {
            m_ButtonPanel.add(m_Buttons.get(i));
         }
         m_ButtonsSet = true;
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
            Io.write(f, getText());
            if (m_ChoosenFileUser != null) {
               m_ChoosenFileUser.setFileChooser(m_FileChooser);
            }
            dispose();
         }
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
   
   // Data ////////////////////////////////////////////////////////////////////
   private JFileChooser m_FileChooser;
   private JPanel m_ButtonPanel;
   private ArrayList<JButton> m_Buttons;
   private int m_OkButton;
   private String m_Command;
   private ArrayList<String> m_Extension;
   private String m_Dir;
   private Saver m_Saver;
   private ChoosenFileUser m_ChoosenFileUser;
   private boolean m_ButtonsSet;
}
