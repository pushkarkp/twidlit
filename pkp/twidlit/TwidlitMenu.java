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
import pkp.twiddle.Twiddle;
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
      add(viewMenu, sm_VIEW_CHORDS_TEXT);
      add(viewMenu, sm_VIEW_REVERSED_TEXT);
      add(viewMenu, sm_VIEW_CHORD_LIST_TEXT);

      JMenu fileMenu = new JMenu(sm_FILE_MENU_TEXT);
      add(fileMenu);
      add(fileMenu, sm_FILE_OPEN_TEXT);
      fileMenu.add(viewMenu);
      fileMenu.addSeparator();
      add(fileMenu, sm_FILE_TWIDDLER_SETTINGS_TEXT);
      m_SettingsWindow = new SettingsWindow(new Cfg());
      fileMenu.addSeparator();
      add(fileMenu, sm_FILE_PREF_TEXT);
      fileMenu.addSeparator();
      add(fileMenu, sm_FILE_QUIT_TEXT);
      m_FileChooser = null;
      m_PrefDir = Persist.get(sm_PREF_DIR_PERSIST, m_Twidlit.getHomeDir());
      m_CfgDir = Persist.get(sm_CFG_DIR_PERSIST, m_Twidlit.getHomeDir());
      m_CfgFName = Persist.get(sm_CFG_FILE_PERSIST, "twiddler.cfg");

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
      
      JMenu twiddlerMenu = new JMenu(sm_TWIDDLER_MENU_TEXT);
      add(twiddlerMenu);
      m_HandButtons = new ButtonGroup();
      addRadioItem(twiddlerMenu, Hand.LEFT.toString(), m_HandButtons);
      addRadioItem(twiddlerMenu, Hand.RIGHT.toString(), m_HandButtons);
      twiddlerMenu.addSeparator();
      int startChecks = twiddlerMenu.getItemCount();
      m_TwiddlerWindow = new TwiddlerWindow(addCheckItem(twiddlerMenu, sm_TWIDDLER_VISIBLE_TEXT), m_Twidlit);
      ButtonModel bm = m_HandButtons.getSelection();
      if (bm == null) {
         m_TwiddlerWindow.setHand(false);
      } else {
         m_TwiddlerWindow.setHand(Hand.create(bm.getActionCommand()).isRight());
      }
      m_TwiddlerWait = m_TwiddlerWindow.getInitialWait();
      addCheckItem(twiddlerMenu, sm_TWIDDLER_HIGHLIGHT_TEXT, m_TwiddlerWindow.isHighlight());
      addCheckItem(twiddlerMenu, sm_TWIDDLER_MARK_TEXT, m_TwiddlerWindow.isMark());
      for (int i = startChecks; i < twiddlerMenu.getItemCount(); ++i) {
         itemStateChanged((JCheckBoxMenuItem)twiddlerMenu.getItem(i));
      }
      twiddlerMenu.addSeparator();
      add(twiddlerMenu, sm_TWIDDLER_WAIT_TEXT);
      
      JMenu helpMenu = new JMenu(sm_HELP_MENU_TEXT);
      add(helpMenu);
      add(helpMenu, sm_HELP_INTRO_TEXT);
      add(helpMenu, sm_HELP_PREF_TEXT);
      add(helpMenu, sm_HELP_REF_TEXT);
      helpMenu.addSeparator();
      add(helpMenu, sm_HELP_SHOW_LOG_TEXT);
      helpMenu.addSeparator();
      add(helpMenu, sm_HELP_ABOUT_TEXT);
      
      enableCountsMenuItems(false);
   }

   /////////////////////////////////////////////////////////////////////////////
   TwiddlerWindow getTwiddlerWindow() {
      return m_TwiddlerWindow;
   }

   /////////////////////////////////////////////////////////////////////////////
   void close() {
      persist("");
   }

   /////////////////////////////////////////////////////////////////////////////
   // only start doing stuff after everything is set up and visible
   public void start() {
      File f = Io.createFile(m_CfgDir, m_CfgFName);
      if (f.exists()) {
         m_Twidlit.extendTitle(f.getAbsolutePath());
      }
      setCfg(Cfg.read(f));
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
      Persist.set(sm_CFG_FILE_PERSIST, m_CfgFName);
      Persist.set(sm_COUNTS_DIR_PERSIST, Io.getRelativePath(m_CountsInDir));
      Persist.set(sm_COUNTS_TEXT_DIR_PERSIST, Io.getRelativePath(m_CountsOutDir));

      for (int i = 0; i < getMenuCount(); ++i) {
         JMenu menu = getMenu(i);
         for (int j = 0; j < menu.getItemCount(); ++j) {
            if (menu.getItem(j) instanceof JRadioButtonMenuItem) {
               JRadioButtonMenuItem rbItem = (JRadioButtonMenuItem)menu.getItem(j);
               if (rbItem.isSelected()) {
                  Persist.set(menu.getText(), Persist.toTag(rbItem.getActionCommand()));
               }
            } else
            if (menu.getItem(j) instanceof JCheckBoxMenuItem) {
               JCheckBoxMenuItem cbItem = (JCheckBoxMenuItem)menu.getItem(j);
               Persist.set(menu.getText() + ' ' + cbItem.getText(), cbItem.getState() ? "true" : "false");
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
//System.out.println("addRadioItem:" + menu.getText() + ":" + persist);
      boolean selected = persist != null && Persist.match(persist, text);
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(text, selected);
      menu.add(item);
      item.setActionCommand(text);
      item.addActionListener(this);
      buttonGroup.add(item);
      return item;
   }

   ///////////////////////////////////////////////////////////////////
   private JCheckBoxMenuItem addCheckItem(JMenu menu, String text) {
      return addCheckItem(menu, text, true);
   }

   ///////////////////////////////////////////////////////////////////
   private JCheckBoxMenuItem addCheckItem(JMenu menu, String text, boolean enabled) {
      String persist = Persist.get(menu.getText() + " " + text);
      boolean selected = persist != null && persist.toLowerCase().equals("true");
      JCheckBoxMenuItem item = new JCheckBoxMenuItem(text, selected);
      menu.add(item);
      item.addItemListener(this);
      item.setEnabled(enabled);
      return item;
   }

   ///////////////////////////////////////////////////////////////////
   @Override // ItemListener
   public void itemStateChanged(ItemEvent e) {
      if (e.getItem() instanceof JCheckBoxMenuItem) {
         itemStateChanged((JCheckBoxMenuItem)e.getItem());
      }
   }

   ///////////////////////////////////////////////////////////////////
   private void itemStateChanged(JCheckBoxMenuItem item) {
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
      case sm_TWIDDLER_VISIBLE_TEXT:
         m_TwiddlerWindow.setVisible(item.getState());
         return;
      case sm_TWIDDLER_HIGHLIGHT_TEXT:
         m_TwiddlerWindow.setHighlight(item.getState());
         return;
      case sm_TWIDDLER_MARK_TEXT:
         m_TwiddlerWindow.setMark(item.getState());
         return;
      }
   }

   ///////////////////////////////////////////////////////////////////
   private void actionPerformed(String command) {
      switch (command) {
      case sm_FILE_OPEN_TEXT:
         m_FileChooser = makeCfgFileChooser(new FileOpenActionListener());
         //m_FileChooser.setSelectedFile(new File("twiddler"));
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
         CountsRangeSetter crs = new CountsRangeSetter(m_Twidlit, m_CountsMinimum, m_CountsMaximum);
         if (crs.isOk()) {
            m_CountsMinimum = crs.getMinimum(); 
            m_CountsMaximum = crs.getMaximum();
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
      case sm_TWIDDLER_WAIT_TEXT:
         TwiddlerWaitSetter ws = new TwiddlerWaitSetter(m_Twidlit, m_TwiddlerWait);
         if (ws.isOk()) {
            m_TwiddlerWait = ws.getWait();
            m_TwiddlerWindow.setWaitFactor((double)m_TwiddlerWait / m_TwiddlerWindow.getInitialWait());
         }
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
         m_AboutWindow.setResizable(true);
         return;
      }
      default:
         if (Hand.isHand(command)) {
            Hand hand = Hand.create(command);
            m_Twidlit.extendTitle(hand.getSmallName());
            m_TwiddlerWindow.setHand(hand.isRight());
            return;
         }
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
      tw.setExtension(sm_CFG_TXT);
      tw.addExtension(sm_CFG);
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
      fc.setFileFilter(new ExtensionFileFilter(sm_CFG_TXT));
      fc.addChoosableFileFilter(new ExtensionFileFilter(sm_CFG));
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
      boolean settingsVisible = m_SettingsWindow.isVisible();
      if (settingsVisible) {
         m_SettingsWindow.setVisible(false);
      }
      m_SettingsWindow = new SettingsWindow(cfg);
      if (settingsVisible) {
         m_SettingsWindow.setVisible(true);
      }
   }

   ///////////////////////////////////////////////////////////////////
   private ExtensionFileFilter getFileFilter(JFileChooser fc) {
      if (fc.getFileFilter() instanceof ExtensionFileFilter) {
         return (ExtensionFileFilter)fc.getFileFilter();
      }
      return new ExtensionFileFilter(sm_CFG);
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
            ExtensionFileFilter eff = getFileFilter(m_FileChooser);
            File f = m_FileChooser.getSelectedFile();
            f = eff.withExtension(f);
            if (!f.exists() || f.isDirectory()) {
               Log.warn("\"" + f.getPath() + "\" is not an existing file.");
               return;
            }             
            switch (eff.getExtension()) {
            case sm_CFG_TXT:
               m_CfgDir = f.getParent();
               m_CfgFName = f.getName();
               m_Twidlit.extendTitle(f.getAbsolutePath());
               setCfg(Cfg.readText(f));
               return;
            case sm_CFG:
               setCfg(Cfg.read(f));
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
         ExtensionFileFilter eff = getFileFilter(fc);
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
         case sm_CFG_TXT:
            Cfg.writeText(f, m_SettingsWindow, asgs);
            return;
         case sm_CFG:
            Cfg.write(f, m_SettingsWindow, asgs);
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
   static final String sm_TWIDDLER_MENU_TEXT = "Twiddler";
   static final String sm_TWIDDLER_WAIT_TEXT = "Set Speed...";
   static final String sm_TWIDDLER_VISIBLE_TEXT = "Visible";
   static final String sm_TWIDDLER_HIGHLIGHT_TEXT = "Highlight Expected Chord";
   static final String sm_TWIDDLER_MARK_TEXT = "Mark Chord Pressed";
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
   static final String sm_CFG_FILE_PERSIST = "cfg.file";
   static final String sm_COUNTS_DIR_PERSIST = "counts.dir";
   static final String sm_COUNTS_TEXT_DIR_PERSIST = "counts.text.dir";
   static final String sm_COUNTS_MINIMUM_PERSIST = "counts.minimum";
   static final String sm_COUNTS_MAXIMUM_PERSIST = "counts.maximum";

   static final String[] sm_PREF_FILES = new String[] {
      "TwidlitDuplicates.txt",
      "TwidlitKeyNames.txt",
      "TwidlitKeyValues.txt",
      "TwidlitKeyEvents.txt",
      "TwidlitLost.txt",
      "TwidlitNGrams.txt",
      "TwidlitPreferences.txt",
      "TwidlitUnprintables.txt"
   };

   // Data ///////////////////////////////////////////////////////////
   private Twidlit m_Twidlit;
   private String m_PrefDir;
   private String m_CfgDir;
   private String m_CfgFName;
   private SettingsWindow m_SettingsWindow;
   private JFileChooser m_FileChooser;
   private JMenu m_CountsMenu;
   private JMenuItem m_CountsTableItem;
   private JMenuItem m_CountsGraphItem;
   private JMenuItem m_ClearCountsItem;
   private Counts m_CharCounts;
   private String m_CountsInDir;
   private String m_CountsOutDir;
   private URL m_NGramsUrl;
   private int m_CountsMinimum; 
   private int m_CountsMaximum; 
   private boolean m_CountsBigrams; 
   private boolean m_CountsNGrams; 
   private TwiddlerWindow m_TwiddlerWindow;
   private ButtonGroup m_HandButtons;
   private int m_TwiddlerWait;
   private HtmlWindow m_IntroWindow;
   private HtmlWindow m_PrefWindow;
   private HtmlWindow m_RefWindow;
   private HtmlWindow m_AboutWindow;
}
