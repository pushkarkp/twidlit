/**
 * Copyright 2015 Pushkar Piggott
 *
 * TwidlitMenu.java
 */

package pkp.twidlit;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import java.util.ArrayList;
import pkp.twiddle.Assignment;
import pkp.twiddle.KeyMap;
import pkp.twiddler.Cfg;
import pkp.twiddler.SettingsWindow;
import pkp.chars.Counts;
import pkp.ui.HtmlWindow;
import pkp.ui.SaveTextWindow;
import pkp.ui.ProgressWindow;
import pkp.ui.ExtensionFileFilter;
import pkp.io.Io;
import pkp.util.*;

////////////////////////////////////////////////////////////////////////////////
class TwidlitMenu extends JMenuBar implements ActionListener, ItemListener, Persistent {

   /////////////////////////////////////////////////////////////////////////////
   TwidlitMenu(Twidlit twidlit) {
      m_Twidlit = twidlit;
      m_CharCounts = null;

      JMenu viewMenu = new JMenu(sm_FILE_VIEW_TEXT);
      m_ViewChordsItem = add(viewMenu, sm_VIEW_CHORDS_TEXT);
      m_ViewReversedItem = add(viewMenu, sm_VIEW_REVERSED_TEXT);
      add(viewMenu, sm_VIEW_CHORD_LIST_TEXT);

      JMenu fileMenu = new JMenu(sm_FILE_MENU_TEXT);
      add(fileMenu);
      add(fileMenu, sm_FILE_OPEN_TEXT);
      fileMenu.add(viewMenu);
      fileMenu.addSeparator();
      m_TwiddlerSettingsItem = add(fileMenu, sm_FILE_TWIDDLER_SETTINGS_TEXT);
      m_SettingsWindow = new SettingsWindow(new Cfg());
      fileMenu.addSeparator();
      add(fileMenu, sm_FILE_PREF_TEXT);
      fileMenu.addSeparator();
      add(fileMenu, sm_FILE_QUIT_TEXT);
      m_FileChooser = null;
      m_PrefDir = Persist.get(sm_PREF_DIR_PERSIST, m_Twidlit.getHomeDir());
      m_CfgDir = Persist.get(sm_CFG_DIR_PERSIST, m_Twidlit.getHomeDir());

      m_CountsMenu = new JMenu(sm_COUNTS_MENU_TEXT);
      add(m_CountsMenu);
      m_CountsBigrams = addCheckItem(m_CountsMenu, sm_COUNTS_BIGRAMS_TEXT).isSelected();
      JCheckBoxMenuItem nGrams = addCheckItem(m_CountsMenu, sm_COUNTS_NGRAMS_TEXT);
      m_NGramsUrl = Pref.getDirJarUrl("pref.dir", "TwidlitNGrams.txt");
      if (m_NGramsUrl == null) {
         nGrams.setEnabled(false); 
         nGrams.setState(false);
      }
      m_CountsNGrams = nGrams.isSelected();
      m_CountsMenu.addSeparator();
      add(m_CountsMenu, sm_COUNTS_FILE_TEXT);
      add(m_CountsMenu, sm_COUNTS_FILES_TEXT);
      m_CountsMenu.addSeparator();      
      add(m_CountsMenu, sm_COUNTS_RANGE_TEXT);
      m_CountsTableItem = add(m_CountsMenu, sm_COUNTS_TABLE_TEXT);
      m_CountsGraphItem = add(m_CountsMenu, sm_COUNTS_GRAPH_TEXT);
      m_CountsMenu.addSeparator();
      m_ClearCountsItem = add(m_CountsMenu, sm_COUNTS_CLEAR_TEXT);
      m_CountsInDir = Persist.get(sm_COUNTS_DIR_PERSIST, m_Twidlit.getHomeDir());
      m_CountsOutDir = Persist.get(sm_COUNTS_TEXT_DIR_PERSIST, m_Twidlit.getHomeDir());
      m_CountsMinimum = 1;
      m_CountsMaximum = Integer.MAX_VALUE;
      
      JMenu helpMenu = new JMenu(sm_HELP_MENU_TEXT);
      add(helpMenu);
      add(helpMenu, sm_HELP_INTRO_TEXT);
      add(helpMenu, sm_HELP_PREF_TEXT);
      add(helpMenu, sm_HELP_REF_TEXT);
      helpMenu.addSeparator();
      add(helpMenu, sm_HELP_SHOW_LOG_TEXT);
      helpMenu.addSeparator();
      add(helpMenu, sm_HELP_ABOUT_TEXT);
      
      enableFileMenus(false);
      enableCountsMenuItems(false);
   }

   /////////////////////////////////////////////////////////////////////////////
   void close() {
      persist("");
   }

   ///////////////////////////////////////////////////////////////////
   @Override // ActionListener
   public void actionPerformed(ActionEvent e) {
      actionPerformed(e.getActionCommand());
   }

   ///////////////////////////////////////////////////////////////////
   @Override // Persistent
   public void persist(String tag) {
      Persist.set(sm_PREF_DIR_PERSIST, Io.getRelativePath(m_PrefDir));
      Persist.set(sm_CFG_DIR_PERSIST, Io.getRelativePath(m_CfgDir));
      Persist.set(sm_COUNTS_DIR_PERSIST, Io.getRelativePath(m_CountsInDir));
      Persist.set(sm_COUNTS_TEXT_DIR_PERSIST, Io.getRelativePath(m_CountsOutDir));

      for (int i = 0; i < getMenuCount(); ++i) {
         JMenu menu = getMenu(i);
         for (int j = 0; j < menu.getItemCount(); ++j) {
            if (menu.getItem(j) instanceof JRadioButtonMenuItem) {
               JRadioButtonMenuItem rbItem = (JRadioButtonMenuItem)menu.getItem(j);
               if (rbItem.isSelected()) {
                  Persist.set(menu.getText(), rbItem.getActionCommand());
               }
            } else
            if (menu.getItem(j) instanceof JCheckBoxMenuItem) {
               JCheckBoxMenuItem cbItem = (JCheckBoxMenuItem)menu.getItem(j);
               Persist.set(menu.getText() + cbItem.getText(), cbItem.getState() ? "true" : "false");
            }
         }
      }
   }

   ///////////////////////////////////////////////////////////////////
   void enableCountsMenu(boolean set) {
      m_CountsMenu.setEnabled(set);
   }

   ///////////////////////////////////////////////////////////////////
   void enableCountsMenuItems(boolean set) {
      m_CountsTableItem.setEnabled(set);
      m_CountsGraphItem.setEnabled(set);
      m_ClearCountsItem.setEnabled(set);
   }

   // Private ////////////////////////////////////////////////////////

   ///////////////////////////////////////////////////////////////////
   private JMenuItem add(JMenu menu, String text) {
      JMenuItem item = new JMenuItem(text);
      menu.add(item);
      item.addActionListener(this);
      return item;
   }

   ///////////////////////////////////////////////////////////////////
   private JRadioButtonMenuItem addRadioItem(JMenu menu, String text, ButtonGroup buttonGroup) {
      String persist = Persist.get(menu.getText());
      boolean selected = persist != null && persist.equals(text);
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(text, selected);
      menu.add(item);
      item.setActionCommand(text);
      item.addActionListener(this);
      buttonGroup.add(item);
      return item;
   }

   ///////////////////////////////////////////////////////////////////
   private JCheckBoxMenuItem addCheckItem(JMenu menu, String text) {
      String persist = Persist.get(menu.getText() + text);
      boolean selected = persist != null && persist.equalsIgnoreCase("true");
      JCheckBoxMenuItem item = new JCheckBoxMenuItem(text, selected);
      menu.add(item);
      item.addItemListener(this);
      return item;
   }

   ///////////////////////////////////////////////////////////////////
   @Override // ItemListener
   public void itemStateChanged(ItemEvent e) {
      if (!(e.getItem() instanceof JCheckBoxMenuItem)) {
         return;
      }
      JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getItem();
      switch (item.getText()) {
      case sm_COUNTS_BIGRAMS_TEXT:
         m_CountsBigrams = item.isSelected();
         if (m_CharCounts != null
          && m_CharCounts.setShowBigrams(m_CountsBigrams)) {
             enableCountsMenuItems(false);
          }
         break;
      case sm_COUNTS_NGRAMS_TEXT:
         m_CountsNGrams = item.isSelected();
         if (m_CharCounts != null
          && m_CharCounts.setShowNGrams(m_CountsNGrams)) {
             enableCountsMenuItems(false);
          }
         break;
      }
   }

   ///////////////////////////////////////////////////////////////////
   private void actionPerformed(String command) {
      switch (command) {
      case sm_FILE_OPEN_TEXT:
         m_FileChooser = makeCfgFileChooser(new FileOpenActionListener());
         m_FileChooser.setSelectedFile(new File("twiddler"));
         m_FileChooser.showOpenDialog(m_Twidlit);
         m_FileChooser = null;
         return;
      case sm_VIEW_CHORDS_TEXT:
      case sm_VIEW_REVERSED_TEXT:
      case sm_VIEW_CHORD_LIST_TEXT:
         viewSaveText(command);
         return;
      case sm_FILE_PREF_TEXT:
         m_FileChooser = makeFileChooser(new PrefActionListener(), m_PrefDir);
         m_FileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
         m_FileChooser.setDialogTitle("Set Preferences Folder");
         m_FileChooser.showDialog(m_Twidlit, "OK");
         m_FileChooser = null;
         return;
      case sm_COUNTS_FILE_TEXT:
      case sm_COUNTS_FILES_TEXT:
         m_FileChooser = makeFileChooser(new CountsFileActionListener(command), m_CountsInDir);
         if (command.equals(sm_COUNTS_FILE_TEXT)) {
            m_FileChooser.setDialogTitle("Select a Text File");
            m_FileChooser.addChoosableFileFilter(new ExtensionFileFilter("txt"));
            m_FileChooser.addChoosableFileFilter(m_FileChooser.getAcceptAllFileFilter());
         } else {
            m_FileChooser.setDialogTitle("Select a Folder of Text Files");
            m_FileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
         }
         m_FileChooser.showDialog(m_Twidlit, "OK");
         m_FileChooser = null;
         return;
      case sm_FILE_TWIDDLER_SETTINGS_TEXT:
         m_SettingsWindow.setVisible(true);
         return;
      case sm_FILE_QUIT_TEXT:
         m_Twidlit.quit();
         return;
      case sm_COUNTS_RANGE_TEXT:
         CountsRangeSetter setter = new CountsRangeSetter(m_Twidlit, m_CountsMinimum, m_CountsMaximum);
         if (setter.isOk()) {
            m_CountsMinimum = setter.getMinimum(); 
            m_CountsMaximum = setter.getMaximum();
            if (m_CharCounts != null) {
               m_CharCounts.setBounds(m_CountsMinimum, m_CountsMaximum);
            }
         }
         return;
      case sm_COUNTS_TABLE_TEXT:
      case sm_COUNTS_GRAPH_TEXT:
         showCounts(command);
         return;
      case sm_COUNTS_CLEAR_TEXT:
         m_CharCounts = null;
         enableCountsMenuItems(false);
         return;
      case sm_HELP_INTRO_TEXT: {
         m_IntroWindow = showHtml(m_IntroWindow, sm_HELP_INTRO_TEXT, "/data/intro.html");
         return;
      }
      case sm_HELP_PREF_TEXT: {
         m_PrefWindow = showHtml(m_PrefWindow, sm_HELP_PREF_TEXT, "/data/pref.html");
         return;
      }
      case sm_HELP_REF_TEXT: {
         m_RefWindow = showHtml(m_RefWindow, sm_HELP_REF_TEXT, "/data/ref.html");
         return;
		}
      case sm_HELP_SHOW_LOG_TEXT:
         Log.get().setVisible(true);
         return;
      case sm_HELP_ABOUT_TEXT: {
         m_AboutWindow = showHtml(m_AboutWindow, sm_HELP_ABOUT_TEXT, "/data/about.html");
         //m_AboutWindow.setResizable(false);
         return;
      }
      default:
      }
   }

   ///////////////////////////////////////////////////////////////////
   private HtmlWindow showHtml(HtmlWindow hw, String title, String path) {
      if (hw != null) {
         hw.toFront();
      } else {
         hw = new HtmlWindow(getClass().getResource(path));
         hw.setTitle(title);
      }
      if (!hw.isVisible()) {
         hw.setVisible(true);
      }
      return hw;
   }

   ///////////////////////////////////////////////////////////////////
   private void viewSaveText(String command) {
      MenuSaveTextWindow tw = null;
      switch (command) {
      case sm_VIEW_CHORDS_TEXT:
         tw = new MenuSaveTextWindow(
            "Chords", 
            Cfg.toString(m_SettingsWindow,
                         m_Twidlit.getKeyMap().getAssignments()), 
            m_CfgDir);
         break;
      case sm_VIEW_REVERSED_TEXT:
         tw = new MenuSaveTextWindow(
            "Chords Reversed",
            Cfg.toString(m_SettingsWindow,
                         m_Twidlit.getKeyMap().getAssignmentsReversed()), 
            m_CfgDir);
         break;
      case sm_VIEW_CHORD_LIST_TEXT:
         tw = new MenuSaveTextWindow(
            "Chord List", 
            Cfg.toString(m_SettingsWindow, 
                         Assignment.listAllByFingerCount()),
            m_CfgDir);
         break;
       default:
         Log.err("TwidlitMenu.viewSaveText: unexpected command " + command);  
      }
      tw.setPersistName(sm_CFG);
      tw.setSaver(new CfgSaver(command));
      tw.setExtension(sm_CFG);
      tw.addExtension(sm_CFG_TXT);
      tw.setVisible(true);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public JFileChooser makeFileChooser(ActionListener al, String dir) {
      JFileChooser fc = new JFileChooser();
      fc.addActionListener(al);
      if (dir == null || "".equals(dir) || !Io.dirExists(dir)) {
         dir = ".";
      }
      fc.setCurrentDirectory(new File(dir));
      fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());
      return fc;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public JFileChooser makeCfgFileChooser(ActionListener al) {
      JFileChooser fc = makeFileChooser(al, m_CfgDir);
      fc.setFileFilter(new ExtensionFileFilter(sm_CFG));
      fc.addChoosableFileFilter(new ExtensionFileFilter(sm_CFG_TXT));
      return fc;
   }
   
   ///////////////////////////////////////////////////////////////////
   private void showCounts(String command) {
      if (m_CharCounts == null) {
         Log.warn("No counts to show");
      }
      (new CharCountShowThread(m_CharCounts, 
                               command == sm_COUNTS_GRAPH_TEXT, 
                               m_CountsOutDir)).start();
   }

   ///////////////////////////////////////////////////////////////////
   private void setCfg(Cfg cfg) {
      m_Twidlit.setKeyMap(new KeyMap(cfg.getAssignments()));
      boolean visible = m_SettingsWindow.isVisible();
      if (visible) {
         m_SettingsWindow.setVisible(false);
      }
      m_SettingsWindow = new SettingsWindow(cfg);
      if (visible) {
         m_SettingsWindow.setVisible(true);
      }
      enableFileMenus(true);
   }

   ///////////////////////////////////////////////////////////////////
   private void enableFileMenus(boolean set) {
      m_ViewChordsItem.setEnabled(set);
      m_ViewReversedItem.setEnabled(set);
      m_TwiddlerSettingsItem.setEnabled(set);
   }

   ///////////////////////////////////////////////////////////////////
   class MenuSaveTextWindow extends SaveTextWindow {
      public MenuSaveTextWindow(String title, String str, String dir) {
         super(title, str);
         setDirectory(dir);
         Font font = getFont();
         setFont(new Font("monospaced", font.getStyle(), font.getSize()));
      }
   }
   
   ///////////////////////////////////////////////////////////////////
   class PrefActionListener implements ActionListener {
      @Override 
      public void actionPerformed(ActionEvent e) {
         if (e.getActionCommand() == "ApproveSelection") {
            File f = m_FileChooser.getSelectedFile();
            if (!f.exists() || !f.isDirectory()) {
               Log.warn("\"" + f.getPath() + "\" is not an existing folder.");
               return;
            }
            for (int i = 0; i < sm_PREF_FILES.length; ++i) {
               File save = new File(f, sm_PREF_FILES[i]);
               if (!save.exists()) {
                  Io.saveFromJar(sm_PREF_FILES[i], "pref", f.getPath());
               }
            }
            m_PrefDir = f.getPath();
         } else if (e.getActionCommand() != "CancelSelection") {
            Log.err("PrefActionListener: unexpected command " + e.getActionCommand());
         }
      }
   }

   ///////////////////////////////////////////////////////////////////
   class FileOpenActionListener implements ActionListener {
      @Override 
      public void actionPerformed(ActionEvent e) {
         if (e.getActionCommand() == "ApproveSelection") {
            ExtensionFileFilter eff = 
               (m_FileChooser.getFileFilter() instanceof ExtensionFileFilter)
               ? (ExtensionFileFilter)m_FileChooser.getFileFilter()
               : new ExtensionFileFilter(sm_CFG);
            File f = m_FileChooser.getSelectedFile();
            f = eff.withExtension(f);
            if (!f.exists() || f.isDirectory()) {
               Log.warn("\"" + f.getPath() + "\" is not an existing file.");
               return;
            }             
            m_CfgDir = f.getParent();
            m_Twidlit.extendTitle(f.getAbsolutePath());
            switch (eff.getExtension()) {
            case sm_CFG:
               setCfg(Cfg.read(f));
               return;
            case sm_CFG_TXT:
               setCfg(Cfg.readText(f));
               return;
            }
            Log.err("FileOpenActionListener: unknown extension \"" + eff.getExtension() + '"');
         } else if (e.getActionCommand() != "CancelSelection") {
            Log.err("FileOpenActionListener unexpected command " + e.getActionCommand());
         }
      }
   }

   ///////////////////////////////////////////////////////////////////
   class CfgSaver implements SaveTextWindow.Saver {

      ////////////////////////////////////////////////////////////////
      CfgSaver(String action) {
         switch (action) {
         default:
            Log.err("CfgSaver: unknown action \"" + action + '"');
         case sm_VIEW_CHORDS_TEXT:
         case sm_VIEW_REVERSED_TEXT:
         case sm_VIEW_CHORD_LIST_TEXT:
            m_Action = action;
         }   
      }

      ////////////////////////////////////////////////////////////////
      @Override 
      public void fileChosen(JFileChooser fc) {
         ExtensionFileFilter eff = 
            (fc.getFileFilter() instanceof ExtensionFileFilter)
            ? (ExtensionFileFilter)fc.getFileFilter()
            : new ExtensionFileFilter(sm_CFG);
         File f = fc.getSelectedFile();
         f = eff.withExtension(f);
         if (f.exists()
          && JOptionPane.showConfirmDialog(
               m_FileChooser, 
               "\"" + f.getPath() + "\" exists, overwrite?", 
               "File Exists", 
               JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
         }
         ArrayList<Assignment> asgs =
            (m_Action == sm_VIEW_CHORD_LIST_TEXT)
            ? Assignment.listAllByFingerCount()
            : (m_Action == sm_VIEW_REVERSED_TEXT)
             ? m_Twidlit.getKeyMap().getAssignmentsReversed()
             : m_Twidlit.getKeyMap().getAssignments();
         switch (eff.getExtension()) {
         case sm_CFG:
            m_CfgDir = f.getParent();
            m_Twidlit.extendTitle(f.getAbsolutePath());
            Cfg.write(f, m_SettingsWindow, asgs);
            return;
         case sm_CFG_TXT:
            Cfg.writeText(f, m_SettingsWindow, asgs);
            return;
         default:
            Log.err("CfgSaver: unknown extension \"" + eff.getExtension() + '"');
         }
     }

      // Data ////////////////////////////////////////////////////////
      private String m_Action;
   }

   ///////////////////////////////////////////////////////////////////
   class CountsFileActionListener implements ActionListener {

      ////////////////////////////////////////////////////////////////
      CountsFileActionListener(String action) {
         m_Action = action;
      }

      ////////////////////////////////////////////////////////////////
      @Override 
      public void actionPerformed(ActionEvent e) {
         if (e.getActionCommand() == "ApproveSelection") {
            File f = m_FileChooser.getSelectedFile();
            if (!f.exists()) {
               Log.warn("\"" + f.getPath() + "\" does not exist.");
               return;
            }
            if (m_CharCounts == null) {
               m_CharCounts = new Counts(m_NGramsUrl,
                                         m_CountsMinimum,
                                         m_CountsMaximum);
               m_CharCounts.setShowBigrams(m_CountsBigrams);               
               m_CharCounts.setShowNGrams(m_CountsNGrams);               
            }
            if (m_Action.equals(sm_COUNTS_FILE_TEXT)) {
               if (f.isDirectory()) {
                  Log.warn("\"" + f.getPath() + "\" is a folder, Counts expected a file.");
                  return;
               }
               m_CountsInDir = f.getParent() == null ? "." : f.getParent();
               if (f.length() < 500000) {
                  m_CharCounts.count(f);
                  enableCountsMenuItems(true);
                  return;
               }
            } else {
               if (!f.isDirectory()) {
                  Log.warn("\"" + f.getPath() + "\" is a file, Counts expected a folder.");
                  return;               
               }
               m_CountsInDir = f.getPath();
            }
            (new CharCountThread(m_CharCounts, f)).start();
         } else if (e.getActionCommand() != "CancelSelection") {
            Log.err("CountsFileActionListener unexpected command " + e.getActionCommand());
         }
      }

      // Data ////////////////////////////////////////////////////////
      private String m_Action;
   }

    ///////////////////////////////////////////////////////////////////
   class CountsChoosenFileUser implements SaveTextWindow.ChoosenFileUser {
      @Override 
      public void setFileChooser(JFileChooser fc) {
         m_CountsOutDir = fc.getCurrentDirectory().getPath();
      }
   }

   ////////////////////////////////////////////////////////////////////////////////
   class CharCountThread extends Thread {
      
      /////////////////////////////////////////////////////////////////////////////
      CharCountThread(Counts counts, File f) {
         m_Counts = counts;
         m_File = f;
      }

      /////////////////////////////////////////////////////////////////////////////
      public void run() {
         if (!m_File.isDirectory()) {
            enableCountsMenu(false);
            m_Counts.count(m_File);
         } else {
            ProgressWindow pw = new ProgressWindow("Count Progress", "", 0, Io.countFiles(m_File));
            pw.setVisible(true);
            File[] files = m_File.listFiles();
            if (files == null) {
               return;
            }
            enableCountsMenu(false);
            for (File file : files) {
               if (!file.isDirectory()) {
                  m_Counts.count(file);
                  pw.step();
               }
            }
            pw.setVisible(false);
            pw.dispose();
         }
         enableCountsMenu(true);
         enableCountsMenuItems(true);
      }

      // Data ////////////////////////////////////////////////////////////////////
      private Counts m_Counts;
      private File m_File;
   }

   ////////////////////////////////////////////////////////////////////////////////
   class CharCountShowThread extends Thread {
      
      /////////////////////////////////////////////////////////////////////////////
      CharCountShowThread(Counts counts, boolean graph, String outDir) {
         m_Counts = new Counts(counts);
         m_Graph = graph;
         m_OutDir = outDir;
      }

      /////////////////////////////////////////////////////////////////////////////
      public void run() {
         ProgressWindow pw = new ProgressWindow(
                                    "Count Progress", "", 
                                    0, (int)(Counts.getProgressCount()));
         pw.setVisible(true);
         MenuSaveTextWindow tw = null;
         if (m_Graph) {
            tw = new MenuSaveTextWindow(
               "Graph of Character Counts", 
               m_Counts.graph(pw), 
               m_OutDir);
         } else {
            tw = new MenuSaveTextWindow(
               "Table of Character Counts", 
               m_Counts.table(pw), 
               m_OutDir);
         }
         tw.setChoosenFileUser(new CountsChoosenFileUser());
         tw.setVisible(true);
         pw.setVisible(false);
      }

      // Data ////////////////////////////////////////////////////////////////////
      private Counts m_Counts;
      private boolean m_Graph;
      private String m_OutDir;
   }

   // Final //////////////////////////////////////////////////////////
   static final String sm_FILE_MENU_TEXT = "File";
   static final String sm_FILE_OPEN_TEXT = "Open...";
   static final String sm_FILE_VIEW_TEXT = "View & Save";
   static final String sm_VIEW_CHORDS_TEXT = "Chords";
   static final String sm_VIEW_REVERSED_TEXT = "Chords Reversed";
   static final String sm_VIEW_CHORD_LIST_TEXT = "Chord List";
   static final String sm_FILE_TWIDDLER_SETTINGS_TEXT = "Twiddler Settings";
   static final String sm_FILE_PREF_TEXT = "Preferences...";
   static final String sm_FILE_QUIT_TEXT = "Quit";
   static final String sm_COUNTS_MENU_TEXT = "Counts";
   static final String sm_COUNTS_FILE_TEXT = "Count File...";
   static final String sm_COUNTS_FILES_TEXT = "Count Files...";
   static final String sm_COUNTS_BIGRAMS_TEXT = "Include Bigrams";
   static final String sm_COUNTS_NGRAMS_TEXT = "Include Ngrams";
   static final String sm_COUNTS_RANGE_TEXT = "Set Range Displayed...";
   static final String sm_COUNTS_TABLE_TEXT = "Table Counts";
   static final String sm_COUNTS_GRAPH_TEXT = "Graph Counts";
   static final String sm_COUNTS_CLEAR_TEXT = "Clear Counts";
   static final String sm_HELP_MENU_TEXT = "Help";
   static final String sm_HELP_INTRO_TEXT = "Introduction";
   static final String sm_HELP_PREF_TEXT = "Preferences";
   static final String sm_HELP_REF_TEXT = "Reference";
   static final String sm_HELP_SHOW_LOG_TEXT = "View Log";
   static final String sm_HELP_ABOUT_TEXT = "About";

   static final String sm_CFG = "cfg";
   static final String sm_CFG_TXT = "cfg.txt";
   static final String sm_PREF_DIR_PERSIST = "pref.dir";
   static final String sm_CFG_DIR_PERSIST = "cfg.dir";
   static final String sm_COUNTS_DIR_PERSIST = "counts.dir";
   static final String sm_COUNTS_TEXT_DIR_PERSIST = "counts.text.dir";
   static final String sm_COUNTS_MINIMUM_PERSIST = "counts.minimum";
   static final String sm_COUNTS_MAXIMUM_PERSIST = "counts.maximum";

   static final String[] sm_PREF_FILES = new String[] {
      "TwidlitPreferences.txt",
      "TwidlitUnprintable.txt",
      "TwidlitKeyNames.txt",
      "TwidlitKeyValues.txt",
      "TwidlitNGrams.txt"
   };
   
   // Data ///////////////////////////////////////////////////////////
   private Twidlit m_Twidlit;
   private Counts m_CharCounts;
   private JMenuItem m_ViewChordsItem;
   private JMenuItem m_ViewReversedItem;
   private JMenuItem m_TwiddlerSettingsItem;
   private JMenu m_CountsMenu;
   private JMenuItem m_CountsTableItem;
   private JMenuItem m_CountsGraphItem;
   private JMenuItem m_ClearCountsItem;
   private SettingsWindow m_SettingsWindow;
   private JFileChooser m_FileChooser;
   private HtmlWindow m_IntroWindow = null;
   private HtmlWindow m_PrefWindow = null;
   private HtmlWindow m_RefWindow = null;
   private HtmlWindow m_AboutWindow = null;
   private String m_PrefDir;
   private String m_CfgDir;
   private String m_CountsInDir;
   private String m_CountsOutDir;
   private URL m_NGramsUrl;
   private int m_CountsMinimum; 
   private int m_CountsMaximum; 
   private boolean m_CountsBigrams; 
   private boolean m_CountsNGrams; 
}
