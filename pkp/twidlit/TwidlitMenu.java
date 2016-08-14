/**
 * Copyright 2015 Pushkar Piggott
 *
 * TwidlitMenu.java
 */

package pkp.twidlit;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import pkp.twiddle.Assignment;
import pkp.twiddle.Assignments;
import pkp.twiddle.Chord;
import pkp.twiddle.KeyMap;
import pkp.twiddle.Twiddle;
import pkp.twiddler.Cfg;
import pkp.twiddler.SettingsWindow;
import pkp.chars.Counts;
import pkp.times.ChordTimes;
import pkp.times.SortedChordTimes;
import pkp.ui.PersistentMenuBar;
import pkp.ui.HtmlWindow;
import pkp.ui.SaveTextWindow.Saver;
import pkp.ui.ProgressWindow;
import pkp.ui.ExtensionFileFilter;
import pkp.ui.IntegerSetter;
import pkp.io.Io;
import pkp.util.*;

////////////////////////////////////////////////////////////////////////////////
class TwidlitMenu extends PersistentMenuBar 
   implements ActionListener, ItemListener, SaveChordsWindow.ContentForTitle, Persistent {

   /////////////////////////////////////////////////////////////////////////////
   TwidlitMenu(Twidlit twidlit) {
      m_Twidlit = twidlit;
      m_CharCounts = null;

      JMenu fileMenu = new JMenu(sm_FILE_MENU_TEXT);
      add(fileMenu);
      m_AllChordsItem = add(fileMenu, sm_FILE_ALL_CHORDS_TEXT);
      add(fileMenu, sm_FILE_OPEN_TEXT);
      add(fileMenu, sm_FILE_SAVE_AS_TEXT);
      fileMenu.addSeparator();
      add(fileMenu, sm_FILE_MAP_CHORDS_TEXT);
      fileMenu.addSeparator();
      add(fileMenu, sm_FILE_TWIDDLER_SETTINGS_TEXT);
      m_SettingsWindow = new SettingsWindow(new Cfg());
      add(fileMenu, sm_FILE_PREF_TEXT);
      fileMenu.addSeparator();
      add(fileMenu, sm_FILE_QUIT_TEXT);
      m_FileChooser = null;
      m_PrefDir = Persist.get(sm_PREF_DIR_PERSIST, m_Twidlit.getHomeDir());
      m_CfgDir = Persist.get(sm_CFG_DIR_PERSIST, m_Twidlit.getHomeDir());
      m_CfgFName = Persist.get(sm_CFG_FILE_PERSIST, "");
      m_KeyPressFile = Persist.getFile(sm_KEY_SOURCE_FILE_PERSIST, "key.source.file");
      
      m_CountsMenu = new JMenu(sm_COUNTS_MENU_TEXT);
      add(m_CountsMenu);
      m_CountsBigrams = addCheckItem(m_CountsMenu, sm_COUNTS_BIGRAMS_TEXT).isSelected();
      m_CountsNGrams = addCheckItem(m_CountsMenu, sm_COUNTS_NGRAMS_TEXT);
      m_NGramsFile = Persist.getFile(sm_NGRAMS_FILE_PERSIST);
      m_CountsNGrams.setEnabled(m_NGramsFile != null);
      if (m_NGramsFile == null) {
         m_CountsNGrams.setState(false);
      }
      add(m_CountsMenu, sm_COUNTS_NGRAMS_FILE_TEXT);
      m_CountsMenu.addSeparator();
      add(m_CountsMenu, sm_COUNTS_FILE_TEXT);
      add(m_CountsMenu, sm_COUNTS_FILES_TEXT);
      m_CountsMenu.addSeparator();      
      add(m_CountsMenu, sm_COUNTS_RANGE_TEXT);
      m_CountsTableItem = add(m_CountsMenu, sm_COUNTS_TABLE_TEXT);
      m_CountsGraphItem = add(m_CountsMenu, sm_COUNTS_GRAPH_TEXT);
      m_CountsMenu.addSeparator();
      m_ClearCountsItem = add(m_CountsMenu, sm_COUNTS_CLEAR_TEXT);
      m_CountsAllFiles = false;
      m_CountsInDir = Persist.get(sm_COUNTS_DIR_PERSIST, m_Twidlit.getHomeDir());
      m_CountsOutDir = Persist.get(sm_COUNTS_TEXT_DIR_PERSIST, m_Twidlit.getHomeDir());
      m_CountsMinimum = 1;
      m_CountsMaximum = 2000000000;
      
      JMenu tutorMenu = new JMenu(sm_TUTOR_MENU_TEXT);
      add(tutorMenu);
      m_HandButtons = new ButtonGroup();
      JCheckBoxMenuItem hide = addCheckItem(tutorMenu, sm_TUTOR_HIDE_TWIDDLER_TEXT);
      boolean vertical = addCheckItem(tutorMenu, sm_TUTOR_VERTICAL_TWIDDLER_TEXT).isSelected();
      TwiddlerWindow tw = new TwiddlerWindow(hide, vertical, isRightHand(), m_Twidlit);
      m_Twidlit.setTwiddlerWindow(tw);
      tutorMenu.addSeparator();
      addRadioItem(tutorMenu, Hand.LEFT.toString(), m_HandButtons);
      addRadioItem(tutorMenu, Hand.RIGHT.toString(), m_HandButtons);
      tutorMenu.addSeparator();
      m_SourceButtons = new ButtonGroup();
      addRadioItem(tutorMenu, sm_TUTOR_CHORDS_TEXT, m_SourceButtons);
      addRadioItem(tutorMenu, sm_TUTOR_KEYS_TEXT, m_SourceButtons);
      tutorMenu.addSeparator();
      m_TutorAutoScaleItem = addCheckItem(tutorMenu, sm_TUTOR_AUTOSCALE_TEXT);
      m_SpeedItem = add(tutorMenu, sm_TUTOR_SET_CHORD_TIME_TEXT);
      m_SpeedItem.setEnabled(!m_TutorAutoScaleItem.isSelected());
      m_DelayItem = add(tutorMenu, sm_TUTOR_SET_DELAY_TEXT);
      tutorMenu.addSeparator();
      m_TutorTimedItem = addCheckItem(tutorMenu, sm_TUTOR_TIMED_TEXT);
      m_TutorOtherTimed = Persist.getBool(sm_TUTOR_OTHER_TIMED_PERSIST);
      add(tutorMenu, sm_TUTOR_CHORDS_BY_TIME_TEXT);
      add(tutorMenu, sm_TUTOR_CLEAR_TIMES_TEXT);
      
      JMenu helpMenu = new JMenu(sm_HELP_MENU_TEXT);
      add(helpMenu);
      add(helpMenu, sm_HELP_INTRO_TEXT);
      add(helpMenu, sm_HELP_ACTIVITIES_TEXT);
      add(helpMenu, sm_HELP_REF_TEXT);
      add(helpMenu, sm_HELP_SYNTAX_TEXT);
      helpMenu.addSeparator();
      addCheckItem(helpMenu, sm_HELP_WRITE_LOG_TEXT);
      m_ShowLog = add(helpMenu, sm_HELP_SHOW_LOG_TEXT);
      helpMenu.addSeparator();
      add(helpMenu, sm_HELP_ABOUT_TEXT);
      
      enableCountsMenuItems(false);
      // after setStateFromCheckItems()
      m_TutorAutoScale = Persist.getBool(sm_TUTOR_OTHER_AUTOSCALE_PERSIST);
   }

   /////////////////////////////////////////////////////////////////////////////
   // only start doing stuff after everything is set up.
   public void start() {
      // use TwidlitInit to gather data so the source is not recreated over and over
      m_TwidlitInit = m_Twidlit.getInit();
      File f = Io.createFile(m_CfgDir, m_CfgFName);
      Cfg cfg = Cfg.read(f);
      String cfgFileName = m_CfgFName;
      if (cfg == null && !"".equals(m_CfgFName)) {
         cfgFileName = "";
         Log.warn("Failed to find " + f.getPath());
      }
      setCfg(cfg);
      m_TwidlitInit.setRightHand(isRightHand());
      setSource();
      // setSource() flips timed booleans so flip them back 
      useOtherTimed();
      // use the gathered settings to set up the source
      m_Twidlit.initialize(m_TwidlitInit, "Press the chord shown.");
      m_Twidlit.extendTitle(cfgFileName);
      if (!isChords() && m_Twidlit.isChords()) {
         // set keystrokes failed
         setChords();
      }
      // revert to using Twidlit itself for settings
      m_TwidlitInit = m_Twidlit;
      m_Twidlit.setVisible(true);
      setStateFromCheckItems();
   }
   
   ///////////////////////////////////////////////////////////////////
   @Override // ActionListener
   public void actionPerformed(ActionEvent e) {
      actionPerformed(e.getActionCommand());
   }

   ///////////////////////////////////////////////////////////////////
   @Override // Persistent
   public void persist(String tag) {
      m_Twidlit.getTwiddlerWindow().persist(tag);
      Persist.set(sm_PREF_DIR_PERSIST, Io.getRelativePath(m_PrefDir));
      Persist.set(sm_CFG_DIR_PERSIST, Io.getRelativePath(m_CfgDir));
      Persist.set(sm_CFG_FILE_PERSIST, m_CfgFName);
      Persist.setFile(sm_NGRAMS_FILE_PERSIST, m_NGramsFile);
      Persist.set(sm_COUNTS_DIR_PERSIST, Io.getRelativePath(m_CountsInDir));
      Persist.set(sm_COUNTS_TEXT_DIR_PERSIST, Io.getRelativePath(m_CountsOutDir));
      Persist.setFile(sm_KEY_SOURCE_FILE_PERSIST, m_KeyPressFile);
      Persist.set(sm_TUTOR_OTHER_AUTOSCALE_PERSIST, m_TutorAutoScale);
      Persist.set(sm_TUTOR_OTHER_TIMED_PERSIST, m_TutorOtherTimed);
      super.persist("");
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

   ///////////////////////////////////////////////////////////////////
   @Override // ItemListener
   public void itemStateChanged(ItemEvent e) {
      if (e.getItem() instanceof JCheckBoxMenuItem) {
         itemStateChanged((JCheckBoxMenuItem)e.getItem());
      }
   }

   ///////////////////////////////////////////////////////////////////
   @Override // PersistentMenuBar
   protected void itemStateChanged(JCheckBoxMenuItem item) {
      switch (item.getText()) {
      case sm_COUNTS_BIGRAMS_TEXT:
         if (m_CharCounts != null) {
            actionPerformed(sm_COUNTS_CLEAR_TEXT);
            if (m_CharCounts != null) {
               return;
            }
         }
         m_CountsBigrams = item.isSelected();
         if (m_CharCounts != null
          && m_CharCounts.setShowBigrams(m_CountsBigrams)) {
            enableCountsMenuItems(false);
         }
         return;
      case sm_COUNTS_NGRAMS_TEXT:
         if (m_CharCounts != null) {
            actionPerformed(sm_COUNTS_CLEAR_TEXT);
            if (m_CharCounts != null) {
               return;
            }
         }
         if (m_CharCounts != null
          && m_CharCounts.setShowNGrams(item.isSelected()
                                        ? m_NGramsFile : null)) {
            enableCountsMenuItems(false);
         }
         return;
      case sm_TUTOR_AUTOSCALE_TEXT:
         m_SpeedItem.setEnabled(!item.isSelected());
         m_TutorAutoScale = item.isSelected();
         m_Twidlit.getTwiddlerWindow().setAutoScale(item.isSelected());
//System.out.printf("1m_TutorAutoScale %b%n", m_TutorAutoScale);
         return;
      case sm_TUTOR_TIMED_TEXT:
         m_TutorAutoScaleItem.setEnabled(item.isSelected());
         if (item.isSelected()) {
            m_TutorAutoScaleItem.setSelected(m_TutorAutoScale);
         } else {
//System.out.printf("0m_TutorAutoScale %b%n", m_TutorAutoScale);
            boolean autoScale = m_TutorAutoScaleItem.isSelected();
            m_TutorAutoScaleItem.setSelected(false);
            m_TutorAutoScale = autoScale;
//System.out.printf("2m_TutorAutoScale %b%n", m_TutorAutoScale);
         }
         m_Twidlit.setTimed(item.getState());
         return;
      case sm_TUTOR_HIDE_TWIDDLER_TEXT:
         m_Twidlit.getTwiddlerWindow().setVisible(!item.getState());
         return;
      case sm_TUTOR_VERTICAL_TWIDDLER_TEXT:
         m_Twidlit.getTwiddlerWindow().setVertical(item.getState());
         return;
      case sm_HELP_WRITE_LOG_TEXT:
         if (item.getState() != Log.hasFile()) {
            Log.setFile(item.getState()
                        ? Io.createFile(m_Twidlit.getHomeDir(), 
                                        sm_LOG_FILE_NAME)
                        : null);
         }
         m_ShowLog.setEnabled(Log.hasFile());
         return;
      }
   }

   ///////////////////////////////////////////////////////////////////
   @Override // ContentForTitle
   public String getContentForTitle(String title) {
      switch (title) {
      case sm_ALL_CHORDS_TITLE:
         return (new Cfg(m_SettingsWindow, 
                         Assignments.listAllByFingerCount())).toString();
      case sm_SAVE_AS_TITLE:
         return (new Cfg(m_SettingsWindow,
                         m_Twidlit.getKeyMap().getAssignments())).toString();
      case sm_CHORDS_BY_TIME_TITLE: {
         ChordTimes ct = m_Twidlit.getChordTimes();
         SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
         return "# T" + ct.getExtension().replace('.', ' ').substring(1) + '\n'
              + "# " + df.format(Calendar.getInstance().getTime()) + '\n'
              + "#   Mean Range (Times)\n"
              + (new SortedChordTimes(ct)).listChordsByTime();
      }
      default:
         Log.err("TwidlitMenu.getContentForTitle() bad title: " + title);
         return "";
      }
   }

   // Private ////////////////////////////////////////////////////////

   ///////////////////////////////////////////////////////////////////
   private boolean isRightHand() {
      ButtonModel handSelected = m_HandButtons.getSelection();
      return handSelected != null && Hand.create(handSelected.getActionCommand()).isRight();
   }

   ///////////////////////////////////////////////////////////////////
   private void setSource() {
      ButtonModel sourceSelected = m_SourceButtons.getSelection();
      if (sourceSelected == null) {
         m_DelayItem.setEnabled(false);
         m_TwidlitInit.setChords();
         return;
      }
      switch (sourceSelected.getActionCommand()) {
      case sm_TUTOR_CHORDS_TEXT:
         m_DelayItem.setEnabled(false);
         m_TwidlitInit.setChords();
         useOtherTimed();
         return;
      case sm_TUTOR_KEYS_TEXT:
         if (m_TwidlitInit.setKeystrokes(m_KeyPressFile)) {
            m_DelayItem.setEnabled(true);
            useOtherTimed();
            return;
         }
         setChords();
         return;
      }
   }
   
   ///////////////////////////////////////////////////////////////////
   private void setChords() {
      AbstractButton chords = m_SourceButtons.getElements().nextElement();
      m_SourceButtons.setSelected(chords.getModel(), true);
      setSource();
   }

   ///////////////////////////////////////////////////////////////////
   private boolean isChords() {
      return m_SourceButtons.getSelection() == m_SourceButtons.getElements().nextElement();
   }

   ///////////////////////////////////////////////////////////////////
   private void actionPerformed(String command) {
      switch (command) {
      case sm_FILE_ALL_CHORDS_TEXT:
         m_SaveChordsWindow = new SaveChordsWindow(this, sm_ALL_CHORDS_TITLE, m_CfgDir);
         m_SaveChordsWindow.setPersistName(sm_CHORD_LIST_PERSIST);
         JButton b = new JButton(sm_USE_ALL_CHORDS_TEXT);
         b.addActionListener(this);
         m_SaveChordsWindow.setOkButton(b);
         m_SaveChordsWindow.setVisible(true);
         return;
      case sm_USE_ALL_CHORDS_TEXT: {
         setCfg(null);
         m_CfgFName = "";
         m_Twidlit.extendTitle(m_CfgFName);
         m_SaveChordsWindow.dispose();
         m_SaveChordsWindow = null;
         return;
      }
      case sm_FILE_OPEN_TEXT:
         m_FileChooser = makeCfgFileChooser(new FileOpenActionListener());
         m_FileChooser.showOpenDialog(m_Twidlit);
         m_FileChooser = null;
         return;
      case sm_FILE_SAVE_AS_TEXT:
      case sm_TUTOR_CHORDS_BY_TIME_TEXT:
         viewSaveText(command);
         return;
      case sm_FILE_MAP_CHORDS_TEXT: {
         ChordTimes ct = m_Twidlit.getChordTimes();
         // don't use the keystroke-prompted chord times
         if (ct.isKeystrokes()) {
            ct = new ChordTimes(false, isRightHand());
         }
         new ChordMapper(m_Twidlit, 
                         new SortedChordTimes(ct));
         return;
      }
      case sm_FILE_PREF_TEXT:
         m_FileChooser = makeFileChooser(new PrefActionListener(), m_PrefDir);
         m_FileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
         m_FileChooser.setDialogTitle("Set Preferences Folder");
         m_FileChooser.showDialog(m_Twidlit, "OK");
         m_FileChooser = null;
         return;
      case sm_COUNTS_NGRAMS_FILE_TEXT:
         if (m_CharCounts != null) {
            actionPerformed(sm_COUNTS_CLEAR_TEXT);
            if (m_CharCounts != null) {
               return;
            }
         }
         m_FileChooser = makeFileChooser(new CountsFileActionListener(sm_COUNTS_NGRAM_FILE_TEXT), null);
         m_FileChooser.setDialogTitle("Select an NGrams File");
         if (m_NGramsFile != null) {
            m_FileChooser.setSelectedFile(m_NGramsFile);
         }
         m_FileChooser.addChoosableFileFilter(new ExtensionFileFilter("keys"));
         m_FileChooser.addChoosableFileFilter(m_FileChooser.getAcceptAllFileFilter());
         m_FileChooser.showDialog(m_Twidlit, "OK");
         m_FileChooser = null;
         m_CountsNGrams.setEnabled(m_NGramsFile != null);
         if (m_NGramsFile == null) {
            m_CountsNGrams.setState(false);
         }
         return;
      case sm_COUNTS_FILE_TEXT:
      case sm_COUNTS_FILES_TEXT:
         m_FileChooser = makeFileChooser(new CountsFileActionListener(command), m_CountsInDir);
         if (command.equals(sm_COUNTS_FILE_TEXT)) {
            m_FileChooser.setDialogTitle("Select a Text File to Count");
            m_FileChooser.addChoosableFileFilter(new ExtensionFileFilter("txt"));
            m_FileChooser.addChoosableFileFilter(m_FileChooser.getAcceptAllFileFilter());
            if (m_CountsAllFiles) {
               m_FileChooser.setFileFilter(m_FileChooser.getAcceptAllFileFilter());
            }
         } else {
            m_FileChooser.setDialogTitle("Select a Folder of Text Files to Count");
            m_FileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
         }
         m_FileChooser.showDialog(m_Twidlit, "OK");
         m_CountsAllFiles = m_FileChooser.getFileFilter() == m_FileChooser.getAcceptAllFileFilter();
         m_FileChooser = null;
         return;
      case sm_FILE_TWIDDLER_SETTINGS_TEXT:
         m_SettingsWindow.setVisible(true);
         return;
      case sm_FILE_QUIT_TEXT:
         m_Twidlit.quit(0);
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
         if (JOptionPane.showConfirmDialog(
                m_Twidlit,
                "Sure you want to clear the character counts?", 
                sm_COUNTS_CLEAR_TEXT, 
                JOptionPane.YES_NO_OPTION)
              == JOptionPane.YES_OPTION) {
            m_CharCounts = null;
            enableCountsMenuItems(false);
         }
         return;
      case sm_TUTOR_CLEAR_TIMES_TEXT: {
         String hand = Hand.createRight(m_Twidlit.isRightHand()).toString();
         if (JOptionPane.showConfirmDialog(
                m_Twidlit,
                "<html>Sure you want to clear the chord times for the <em>"
                + hand.toLowerCase() + "</em>?</html>", 
                sm_TUTOR_CLEAR_TIMES_TEXT, 
                JOptionPane.YES_NO_OPTION)
              == JOptionPane.YES_OPTION) {
            m_Twidlit.clearTimes();
         }
         return;
      }
      case sm_TUTOR_SET_DELAY_TEXT: {
         IntegerSetter is = new IntegerSetter(
            m_Twidlit, "Chord Delay",
            String.format("The milliseconds before the chord is displayed [0..%d]:", ChordTimes.sm_MAX_MSEC),
            m_Twidlit.getTwiddlerWindow().getDelay(), 
            0, ChordTimes.sm_MAX_MSEC, 100);
         if (is.isOk()) {
            m_Twidlit.getTwiddlerWindow().setDelay(is.getValue());
         }
         return;
      }
      case sm_TUTOR_SET_CHORD_TIME_TEXT: {
         int max = ChordTimes.sm_MAX_MSEC * 100 / Pref.getInt("#.chord.wait.timed.percent");
         IntegerSetter is = new IntegerSetter(
            m_Twidlit, "Chord Wait",
            String.format("The progress bar interval in milliseconds [0..%d]:", max),
            m_Twidlit.getTwiddlerWindow().getProgressBarMax(),
            0, max, 100);
         if (is.isOk()) {
            m_Twidlit.getTwiddlerWindow().setProgressBarMax(is.getValue());
         }
         return;
      }
      case sm_TUTOR_KEYS_TEXT: {
         SourceFileActionListener sfal = new SourceFileActionListener();
         m_FileChooser = makeFileChooser(sfal, null);
         m_FileChooser.setDialogTitle("Select a Keystrokes File");
         if (m_KeyPressFile != null) {
            m_FileChooser.setSelectedFile(m_KeyPressFile);
         }
         m_FileChooser.addChoosableFileFilter(new ExtensionFileFilter("keys"));
         m_FileChooser.addChoosableFileFilter(m_FileChooser.getAcceptAllFileFilter());
         m_FileChooser.showDialog(m_Twidlit, "OK");
         m_FileChooser = null;
         if (sfal.getFile() != null) {
            m_KeyPressFile = sfal.getFile();
         }
         // no break
      }
      case sm_TUTOR_CHORDS_TEXT: {
         setSource();
         return;
      }
      case sm_HELP_INTRO_TEXT: {
         m_HelpWindow = showHtml(m_HelpWindow, sm_HELP_MENU_TEXT, "/data/intro.html");
         return;
      }
      case sm_HELP_ACTIVITIES_TEXT: {
         m_HelpWindow = showHtml(m_HelpWindow, sm_HELP_MENU_TEXT, "/data/act.html");
         return;
      }
      case sm_HELP_REF_TEXT: {
         m_HelpWindow = showHtml(m_HelpWindow, sm_HELP_MENU_TEXT, "/data/ref.html");
         return;
      }
      case sm_HELP_SYNTAX_TEXT: {
         m_HelpWindow = showHtml(m_HelpWindow, sm_HELP_MENU_TEXT, "/data/syn.html");
         return;
      }
      case sm_HELP_SHOW_LOG_TEXT:
         Log.get().setVisible(true);
         return;
      case sm_HELP_ABOUT_TEXT: {
         m_AboutWindow = showHtml(m_AboutWindow, sm_HELP_ABOUT_TEXT, "/data/about.html");
         return;
      }
      default:
         if (Hand.isHand(command)) {
            Hand hand = Hand.create(command);
            if (hand.isRight() != m_Twidlit.isRightHand()) {
               m_TwidlitInit.setRightHand(hand.isRight());
            }
            return;
         }
      }
   }

   ///////////////////////////////////////////////////////////////////
   private HtmlWindow showHtml(HtmlWindow hw, String title, String path) {
      if (hw != null) {
         hw.toFront();
         hw.goTo(getClass().getResource(path).toString());
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
   private void useOtherTimed() {
      if (m_TutorOtherTimed == m_Twidlit.isTimed()) {
         return;
      }
      m_Twidlit.setTimed(m_Twidlit.isTimed());
      m_TutorTimedItem.setSelected(!m_TutorTimedItem.isSelected());
      m_TutorOtherTimed = !m_TutorOtherTimed;      
   }

   ///////////////////////////////////////////////////////////////////
   private void viewSaveText(String command) {
      SaveChordsWindow scw =  null;
      switch (command) {
      case sm_FILE_SAVE_AS_TEXT:
         scw = new SaveChordsWindow(this, sm_SAVE_AS_TITLE, m_CfgDir);
         scw.setPersistName(sm_CHORD_LIST_PERSIST);
         scw.setSaver(new CfgSaver(command, scw));
         scw.setExtension(sm_CFG_CHORDS);
         scw.addExtension(sm_CFG);
         break;
      case sm_TUTOR_CHORDS_BY_TIME_TEXT:
         scw = new SaveChordsWindow(this, sm_CHORDS_BY_TIME_TITLE, m_CfgDir);
         scw.setExtension(m_Twidlit.getChordTimes().getExtension());
         break;
       default:
         Log.err("TwidlitMenu.viewSaveText(): unexpected command " + command);
         return;
      }
      scw.setVisible(true);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   private JFileChooser makeFileChooser(ActionListener al, String dir) {
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
   private JFileChooser makeCfgFileChooser(ActionListener al) {
      JFileChooser fc = makeFileChooser(al, m_CfgDir);
      fc.setFileFilter(new ExtensionFileFilter(sm_CFG_CHORDS));
      fc.addChoosableFileFilter(new ExtensionFileFilter(sm_CFG));
      return fc;
   }
   
   ///////////////////////////////////////////////////////////////////
   private void showCounts(String command) {
      if (m_CharCounts == null) {
         Log.warn("No counts to show");
      }
      (new CharCountShowThread(m_CharCounts, 
                               command, 
                               m_CountsOutDir)).start();
   }

   ///////////////////////////////////////////////////////////////////
   private void setCfg(Cfg cfg) {
      m_AllChordsItem.setEnabled(cfg != null);
      if (cfg == null) {
         cfg = new Cfg(m_SettingsWindow, Assignments.listAllByFingerCount());
      }
      m_TwidlitInit.setKeyMap(new KeyMap(cfg.getAssignments()));
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
   class PrefActionListener implements ActionListener {
      @Override 
      public void actionPerformed(ActionEvent e) {
         if (e.getActionCommand() == "ApproveSelection") {
            File outFolder = m_FileChooser.getSelectedFile();
            if (!outFolder.exists() || !outFolder.isDirectory()) {
               Log.warn("\"" + outFolder.getPath() + "\" is not an existing folder.");
               return;
            }
            List<String> names = Io.listFromCodeJarFolder(sm_PREF);
            boolean saved = false;
            for (int i = 0; i < names.size(); ++i) {
               File save = new File(outFolder, names.get(i));
               if (!save.exists()) {
                  saved = true;
                  Io.saveFromCodeJar(names.get(i), sm_PREF, outFolder.getPath());
               }
            }
            m_PrefDir = outFolder.getPath();
            if (!saved) {
               Log.warn("Nothing saved, all preference files already exist in " + outFolder.getPath());
            }
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
            case sm_CFG_CHORDS:
            case sm_CFG:
               Cfg cfg = Cfg.read(f);
               if (cfg != null) {
                  setCfg(cfg);
                  if (!isChords() && m_Twidlit.isChords()) {
                     // mappings no longer adequate
                     setChords();
                  }
                  // set only after success, or will fail next startup
                  m_CfgDir = f.getParent();
                  m_CfgFName = f.getName();
                  m_Twidlit.extendTitle(m_CfgFName);
               }
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
      CfgSaver(String action, SaveTextWindow stw) {
         m_Window = stw;
         switch (action) {
         default:
            Log.err("CfgSaver: unknown action \"" + action + '"');
         case sm_FILE_SAVE_AS_TEXT:
         case sm_TUTOR_CHORDS_BY_TIME_TEXT:
            m_Action = action;
         }   
      }

      ////////////////////////////////////////////////////////////////
      @Override 
      public void fileChosen(JFileChooser fc) {
         if (m_Action == sm_TUTOR_CHORDS_BY_TIME_TEXT) {
            Log.err(String.format("m_Action == %s", sm_TUTOR_CHORDS_BY_TIME_TEXT));
         }
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
         switch (eff.getExtension()) {
         case sm_CFG_CHORDS:
            // 4finger or 0MRL
            Io.write(f, m_Window.getText());
            break;
         case sm_CFG:
            (new Cfg(m_SettingsWindow, 
                     m_Twidlit.getKeyMap().getAssignments())).write(f);
            break;
         default:
            Log.err("CfgSaver: unknown extension \"" + eff.getExtension() + '"');
            break;
         }
         m_Window.dispose();
      }

      // Data ////////////////////////////////////////////////////////
      private String m_Action;
      private SaveTextWindow m_Window;
   }

   ///////////////////////////////////////////////////////////////////
   class SourceFileActionListener implements ActionListener {

      ////////////////////////////////////////////////////////////////
      @Override 
      public void actionPerformed(ActionEvent e) {
         if (e.getActionCommand() == "ApproveSelection") {
            File f = m_FileChooser.getSelectedFile();
            if (f.isDirectory()) {
               Log.warn("\"" + f.getPath() + "\" is a file, Counts expected a folder.");
            } else {
               m_File = f;
            }
         } else if (e.getActionCommand() != "CancelSelection") {
            Log.err("SourceFileActionListener unexpected command " + e.getActionCommand());
         }
      }
      
      ////////////////////////////////////////////////////////////////
      File getFile() { return m_File; }

      // Data ////////////////////////////////////////////////////////
      private File m_File;
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
            if (m_Action.equals(sm_COUNTS_NGRAM_FILE_TEXT)) {
               m_NGramsFile = f;
               return;
            }
            if (m_CharCounts == null) {
               m_CharCounts = new Counts(m_CountsNGrams.isSelected()
                                         ? m_NGramsFile : null,
                                         m_CountsMinimum,
                                         m_CountsMaximum);
               m_CharCounts.setShowBigrams(m_CountsBigrams);               
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
         enableCountsMenu(false);
         if (!m_File.isDirectory()) {
            m_Counts.count(m_File);
         } else {
            List<File> files = Io.listAllFilesInTree(m_File);
            ProgressWindow pw = new ProgressWindow("Count Progress", "", 0, files.size());
            pw.setVisible(true);
            for (File file : files) {
               m_Counts.count(file);
               pw.step();
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
      CharCountShowThread(Counts counts, String what, String outDir) {
         m_Counts = new Counts(counts);
         m_ShowWhat = what;
         m_OutDir = outDir;
      }

      /////////////////////////////////////////////////////////////////////////////
      public void run() {
         ProgressWindow pw = new ProgressWindow(
            "Count Progress", "", 
            0, (int)(Counts.getProgressCount()));
         pw.setVisible(true);
         SaveTextWindow stw = null;
         switch (m_ShowWhat) {
         case sm_COUNTS_TABLE_TEXT:
            stw = new SaveTextWindow(
               "Character Counts", 
               m_Counts.table(pw),
               "count.keys", 
               m_OutDir);
            break;
         case sm_COUNTS_GRAPH_TEXT:
            stw = new SaveTextWindow(
               "Graph of Character Counts", 
               m_Counts.graph(pw),
               "graph.keys",
               m_OutDir);
            break;
         }
         stw.setChoosenFileUser(new CountsChoosenFileUser());
         stw.setVisible(true);
         pw.setVisible(false);
      }

      // Data ////////////////////////////////////////////////////////////////////
      private Counts m_Counts;
      private String m_ShowWhat;
      private String m_OutDir;
   }
   
   // Final //////////////////////////////////////////////////////////
   private static final String sm_FILE_MENU_TEXT = "File";
   private static final String sm_FILE_ALL_CHORDS_TEXT = "Map All Chords...";
   private static final String sm_FILE_OPEN_TEXT = "Open...";
   private static final String sm_FILE_SAVE_AS_TEXT = "Save As...";
   private static final String sm_FILE_MAP_CHORDS_TEXT = "Create Map File...";
   private static final String sm_FILE_TWIDDLER_SETTINGS_TEXT = "Twiddler Settings";
   private static final String sm_FILE_PREF_TEXT = "Preferences...";
   private static final String sm_FILE_QUIT_TEXT = "Quit";
   private static final String sm_COUNTS_MENU_TEXT = "Counts";
   private static final String sm_COUNTS_NGRAMS_FILE_TEXT = "Ngrams File...";
   private static final String sm_COUNTS_FILE_TEXT = "Count File...";
   private static final String sm_COUNTS_FILES_TEXT = "Count Files...";
   private static final String sm_COUNTS_BIGRAMS_TEXT = "Include Bigrams";
   private static final String sm_COUNTS_NGRAMS_TEXT = "Include Ngrams";
   private static final String sm_COUNTS_NGRAM_FILE_TEXT = "NGrams File...";
   private static final String sm_COUNTS_RANGE_TEXT = "Set Range Displayed...";
   private static final String sm_COUNTS_TABLE_TEXT = "Table Counts";
   private static final String sm_COUNTS_GRAPH_TEXT = "Graph Counts";
   private static final String sm_COUNTS_CLEAR_TEXT = "Clear Counts";
   private static final String sm_TUTOR_MENU_TEXT = "Tutor";
   private static final String sm_TUTOR_HIDE_TWIDDLER_TEXT = "Hide Twiddler";
   private static final String sm_TUTOR_VERTICAL_TWIDDLER_TEXT = "Vertical Twiddler";
   private static final String sm_TUTOR_CHORDS_TEXT = "Chords";
   private static final String sm_TUTOR_KEYS_TEXT = "Keystrokes";
   private static final String sm_TUTOR_AUTOSCALE_TEXT = "Auto-scale";
   private static final String sm_TUTOR_SET_CHORD_TIME_TEXT = "Set Chord Wait...";
   private static final String sm_TUTOR_SET_DELAY_TEXT = "Set Chord Delay...";
   private static final String sm_TUTOR_TIMED_TEXT = "Timed";
   private static final String sm_TUTOR_CHORDS_BY_TIME_TEXT = "List Chords By Time";
   private static final String sm_TUTOR_CLEAR_TIMES_TEXT = "Clear Times";
   public static final String sm_HELP_MENU_TEXT = "Help";
   private static final String sm_HELP_INTRO_TEXT = "Introduction";
   private static final String sm_HELP_ACTIVITIES_TEXT = "Activities";
   private static final String sm_HELP_REF_TEXT = "Reference";
   private static final String sm_HELP_SYNTAX_TEXT = "File Types";
   public static final String sm_HELP_WRITE_LOG_TEXT = "Log";
   private static final String sm_HELP_SHOW_LOG_TEXT = "View Log";
   private static final String sm_HELP_ABOUT_TEXT = "About";
   private static final String sm_ALL_CHORDS_TITLE = "All Chords Mapped";
   private static final String sm_SAVE_AS_TITLE = "Mapped Chords";
   private static final String sm_CHORDS_BY_TIME_TITLE = "Chords By Time";
   private static final String sm_USE_ALL_CHORDS_TEXT = "Use";

   public static final String sm_LOG_FILE_NAME = "twidlit.log";

   private static final String sm_TEST = "#.test.code.untargeted";
   private static final String sm_TEST_PERSIST = "#.test.code.not.in.persist";
   private static final String sm_TEST_PREF = "#.test.code.not.in.pref";

   private static final String sm_PREF = "pref";
   private static final String sm_CFG = "cfg";
   private static final String sm_CFG_CHORDS = "cfg.chords";
   private static final String sm_PREF_DIR_PERSIST = "#.pref.dir";
   private static final String sm_CFG_DIR_PERSIST = "#.cfg.dir";
   private static final String sm_CFG_FILE_PERSIST = "#.cfg.file";
   private static final String sm_CHORD_LIST_PERSIST = "#.chord.list";
   private static final String sm_NGRAMS_FILE_PERSIST = "#.ngrams.file";
   private static final String sm_COUNTS_DIR_PERSIST = "#.counts.dir";
   private static final String sm_COUNTS_TEXT_DIR_PERSIST = "#.counts.out.dir";
   private static final String sm_KEY_SOURCE_FILE_PERSIST = "#.key.source.file";
   private static final String sm_TUTOR_OTHER_TIMED_PERSIST = "#.tutor.other.timed";
   private static final String sm_TUTOR_OTHER_AUTOSCALE_PERSIST = "#.tutor.other.autoscale";

   // Data ///////////////////////////////////////////////////////////
   // At initialization time m_TwidlitInit is a separate object
   // that collects the settings for the source.
   private TwidlitInit m_TwidlitInit;
   private Twidlit m_Twidlit;
   private String m_PrefDir;
   private String m_CfgDir;
   private String m_CfgFName;
   private SettingsWindow m_SettingsWindow;
   private SaveChordsWindow m_SaveChordsWindow;
   private JFileChooser m_FileChooser;
   private JMenuItem m_AllChordsItem;
   private JMenu m_CountsMenu;
   private JMenuItem m_CountsTableItem;
   private JMenuItem m_CountsGraphItem;
   private JMenuItem m_ClearCountsItem;
   private JMenuItem m_DelayItem;
   private JMenuItem m_SpeedItem;
   private JCheckBoxMenuItem m_TutorTimedItem;
   private JCheckBoxMenuItem m_TutorAutoScaleItem;
   private Counts m_CharCounts;
   private boolean m_CountsAllFiles;
   private String m_CountsInDir;
   private String m_CountsOutDir;
   private int m_CountsMinimum; 
   private int m_CountsMaximum; 
   private boolean m_CountsBigrams; 
   private JCheckBoxMenuItem m_CountsNGrams; 
   private File m_NGramsFile;
   private ButtonGroup m_HandButtons;
   private ButtonGroup m_SourceButtons;
   private boolean m_TutorAutoScale;
   private boolean m_TutorOtherTimed;
   private File m_KeyPressFile;
   private HtmlWindow m_HelpWindow;
   private HtmlWindow m_AboutWindow;
   private JMenuItem m_ShowLog;
}
