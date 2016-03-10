/**
 * Copyright 2015 Pushkar Piggott
 *
 * Twidlit.java
 */

package pkp.twidlit;

import java.awt.*;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.io.File;
import java.util.ArrayList;
import pkp.text.TextPanel;
import pkp.twiddle.Assignment;
import pkp.twiddle.Chord;
import pkp.twiddle.ThumbKeys;
import pkp.twiddle.KeyMap;
import pkp.twiddle.KeyPress;
import pkp.twiddle.KeyPressList;
import pkp.twiddle.Twiddle;
import pkp.times.ChordTimes;
import pkp.ui.PersistentFrame;
import pkp.util.Persist;
import pkp.util.Pref;
import pkp.util.Log;
import pkp.io.Io;

////////////////////////////////////////////////////////////////////////////////
class Twidlit extends PersistentFrame implements TwidlitInit, WindowListener, KeyListener, ActionListener, Log.Quitter {

   ////////////////////////////////////////////////////////////////////////////////
   // Gathers initial settings so source can be created once only.
   class Init implements TwidlitInit {
      public void setKeyMap(KeyMap km) {
         m_KeyMap = km;
      }
      public void setRightHand(boolean right) {
         m_Right = right;
      }
      public void setChords() {
         m_File = null;
      }
      public void setKeystrokes(File f) {
         m_File = f;
      }

      private KeyMap m_KeyMap;
      private boolean m_Right;
      private File m_File;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   Twidlit() {
      super();
      Color bg = Pref.getColor("background.color");
      UIManager.put("OptionPane.background", bg);
      UIManager.put("Panel.background", bg);
      UIManager.put("Button.background", bg);
      UIManager.put("CheckBox.background", bg);
      //UIManager.put("ScrollPane.background", bg);
      UIManager.put("ScrollBar.background", bg);
      // Spinner 
      UIManager.put("FormattedTextField.background", bg);
        // FileChooser list
      UIManager.put("List.background", bg);
      UIManager.put("TextField.background", bg);
      UIManager.put("ComboBox.background", bg);
      Log.setWindow(this);
      Log.setQuitter(this);
      setIconImage(Pref.getIcon().getImage());
      setFocusable(true);
      requestFocusInWindow();
      setFocusTraversalKeysEnabled(false);
      addWindowListener(this);
      addKeyListener(this);
      setTitle(getClass().getSimpleName());
      setPersistName(getClass().getSimpleName());
      setResizable(true);

      TwidlitMenu mb = new TwidlitMenu(this);
      setJMenuBar(mb);

      m_KeysPressed = new KeyPressList();
      setKeyWaitMsec(Pref.getInt("key.wait.msec"));
      m_StartTimeMs = 0;
      m_TimeMs = 0;
      m_ProgressFactor = Pref.getInt("progress.timed.percent") / 100.0;
      
      pack();
      // uses Init and calls initilize()
      mb.start();
   }

   /////////////////////////////////////////////////////////////////////////////
   TwidlitInit getInit() {
      return new Init();
   }

   /////////////////////////////////////////////////////////////////////////////
   void initialize(TwidlitInit init) {
      if (!(init instanceof Init)) {
         Log.err("Bad TwidlitInit");
         return;
      }
      if (m_ChordTimes != null
       || m_KeyMap != null
       || m_TextPanel != null) {
         Log.err("Repeated initialization");
         return;
      }
      Init in = (Init)init;
      m_TwiddlerWindow.setRightHand(in.m_Right);
      m_KeyMap = in.m_KeyMap;
      m_TextPanel = new TextPanel(m_KeyMap);
      setContentPane(m_TextPanel);
      if (in.m_File == null) {
         setChords();
      } else {
         setKeystrokes(in.m_File);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // TwidlitInit
   public void setKeyMap(KeyMap km) {
      if (km == null) {
         return;
      }
      m_KeyMap = km;
      m_TextPanel.setKeyMap(m_KeyMap);
      start();
   }
   
   /////////////////////////////////////////////////////////////////////////////
   KeyMap getKeyMap() {
      return m_KeyMap;
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // TwidlitInit
   public void setRightHand(boolean right) {
      if (right != isRightHand()) {
         m_TwiddlerWindow.setRightHand(right);
         if (m_TextPanel.isChords()) {
            // new times need new chords
            setChords();
         } else {
            setChordTimes(!m_TextPanel.isChords());
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   boolean isRightHand() {
      return m_TwiddlerWindow.isRightHand();
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // TwidlitInit
   public void setChords() {
      m_TwiddlerWindow.useDelay(false);
      setChordTimes(false);
      m_TextPanel.setChords(m_ChordTimes.getCounts());
      start();
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // TwidlitInit
   public void setKeystrokes(File f) {
      if (!m_TextPanel.isKeystrokes()) {
         m_TwiddlerWindow.useDelay(true);
         setChordTimes(true);
      }
      m_TextPanel.setKeystrokes(f);
      start();
   }

   /////////////////////////////////////////////////////////////////////////////
   String getHomeDir() {
      return m_HomeDir;
   }

   /////////////////////////////////////////////////////////////////////////////
   void setTwiddlerWindow(TwiddlerWindow tw) {
      m_TwiddlerWindow = tw;
   }

   /////////////////////////////////////////////////////////////////////////////
   TwiddlerWindow getTwiddlerWindow() {
      return m_TwiddlerWindow;
   }

   /////////////////////////////////////////////////////////////////////////////
   void extendTitle(String extension) {
      if (extension == null || "".equals(extension)) {
         setTitle(getClass().getSimpleName());
      } else {
         setTitle(getClass().getSimpleName() + " - " + extension);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   void clearTimes() {
      m_ChordTimes.clear();
      if (m_TextPanel.isChords()) {
         setChords();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   ChordTimes getChordTimes() {
      return m_ChordTimes;
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // Log.Quitter
   public void quit() {
      //m_TwiddlerWindow.setVisible(false);
      m_TwiddlerWindow.dispose();
      setVisible(false);
      m_ChordTimes.persist("");
      getJMenuBar().setVisible(false); // persist
      Log.close();
      // call this explicitly since we are not DISPOSE_ON_CLOSE
      dispose();
      // close saves to persist, so write persist last
      Pref.writePersist();
      System.exit(0);
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // WindowListener
   public void windowActivated(WindowEvent e) { requestFocusInWindow(); }
   @Override // WindowListener
   public void windowDeactivated(WindowEvent e) { requestFocusInWindow(); }
   @Override // WindowListener
   public void windowDeiconified(WindowEvent e) { requestFocusInWindow(); }
   @Override // WindowListener
   public void windowIconified(WindowEvent e) {}
   @Override // WindowListener
   public void windowOpened(WindowEvent e) { requestFocusInWindow(); }
   @Override // WindowListener
   public void windowClosed(WindowEvent e) { quit(); }
   @Override // WindowListener
   public void windowClosing(WindowEvent e) { quit(); }

   ////////////////////////////////////////////////////////////////////////////
   @Override // KeyListener
   public void keyTyped(KeyEvent e) {}
   @Override // KeyListener
   public void keyReleased(KeyEvent e) {}

   ////////////////////////////////////////////////////////////////////////////
   @Override // KeyListener
   public void keyPressed(KeyEvent e) {
//System.out.println("keyPressed " + KeyPress.toString(e));
      KeyPress kp = KeyPress.parseEvent(e);
      // ignore invisible keys such as bare shift or control
      if (kp.isModifiers()) {
         return;
      }
//System.out.printf("pressed %s %s%n", kp.toString(KeyPress.Format.HEX), kp.toString());
      m_TimerKeyWait.restart();
      if (m_KeyStartMs != 0) {
         Log.log(String.format("keyPressed event %dms now %dms", 
                  (int)(e.getWhen() - m_KeyStartMs),
                  (int)(System.currentTimeMillis() - m_KeyStartMs)));
      }
      m_KeyStartMs = System.currentTimeMillis();
      // store time of arrival of first key press
      if (m_TimeMs == 0) {
         m_TimeMs = (int)(e.getWhen() - m_StartTimeMs);
//System.out.printf("e.getWhen() %d m_StartTimeMs %d m_TimeMs %d limit %d%n", e.getWhen(), m_StartTimeMs, m_TimeMs, 2 * m_TwiddlerWindow.getProgressMax());
      }
      m_KeysPressed.add(kp);
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // ActionListener
   public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand() != m_TimerKeyWait.getActionCommand()) {
         Log.err("Unknown action: " + e.getActionCommand());
         return;
      }
      m_KeyStartMs = 0;
      m_TimerKeyWait.stop();
      // convert keys to twiddle
//System.out.println("actionPerformed m_KeysPressed " + m_KeysPressed.toString());
      Assignment pressed = m_KeysPressed.findLongestPrefix(m_KeyMap);
      if (pressed == null) {
         // no matching twiddle
         Log.warn("Unknown keys: \"" + m_KeysPressed
                + String.format("\" key.wait.msec (%d) is probably too small", m_KeyWaitMsec));
         m_KeysPressed = new KeyPressList();
         return;
      }
//System.out.println("actionPerformed pressed " + pressed.toString());
      if (!pressed.getKeyPressList().equals(m_KeysPressed)) {
         // only partial match
         Log.log("Only matched \"" 
            + pressed.getKeyPressList() + "\" (" + pressed.getKeyPressList().toString(KeyPress.Format.HEX)
            + ") of \"" + m_KeysPressed + "\" (" + m_KeysPressed.toString(KeyPress.Format.HEX) + ')');
//         Log.warn("Twidlit probably has a different cfg from your Twiddler.");
      }
      m_KeysPressed = new KeyPressList();
      Twiddle tw = pressed.getTwiddle(0);
      Twiddle next = m_TextPanel.getNextTwiddle(pressed.getKeyPressList());
      if (next == null) {
         m_TwiddlerWindow.markMismatch(tw);
         continueTime();
      } else {
         // only accept chord if not timing or successfully timed
         m_TextPanel.next(
            !m_Timed
            // only record times within 2* progress bar
            || (m_TimeMs < (int)(0.5 + m_ProgressFactor * m_TwiddlerWindow.getProgressMax())
             && m_ChordTimes.add(tw.getChord().toInt(),
                                 tw.getThumbKeys().toInt(),
                                 m_TimeMs))
         );
//System.out.printf("%s %s%n", tw.getChord(), m_ChordTimes.getTimes(tw.getChord().toInt(), 0));
         show(next, pressed.getTwiddle(0));
         startTime();
      }
   }
   
   /////////////////////////////////////////////////////////////////////////////
   void setKeyWaitMsec(int newValue) {
      m_KeyWaitMsec = newValue;
      m_TimerKeyWait = new Timer(m_KeyWaitMsec, this);
      m_TimerKeyWait.setActionCommand("key.collection.complete");
   }

   /////////////////////////////////////////////////////////////////////////////
   boolean isTimed() {
      return m_Timed;
   }

   /////////////////////////////////////////////////////////////////////////////
   void setTimed(boolean set) {
      m_Timed = set;
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private void start() {
      show(m_TextPanel.getFirstTwiddle(), null);
      startUnrecordedTime();
   }

   ////////////////////////////////////////////////////////////////////////////
   private void show(Twiddle next, Twiddle prev) {
      int ch = next.getChord().toInt();
      int tk = next.getThumbKeys().toInt();
      m_TwiddlerWindow.setMeanMean(m_ChordTimes.getMeanMean(tk));
      m_TwiddlerWindow.setMean(m_ChordTimes.getMean(ch, tk));
      m_TwiddlerWindow.show(next, prev);
  }

   ////////////////////////////////////////////////////////////////////////////
   private void startTime() {
      m_StartTimeMs = System.currentTimeMillis();
      m_TimeMs = 0;
  }

   ////////////////////////////////////////////////////////////////////////////
   // Start the timer in the past so the resulting time is not recorded.
   private void startUnrecordedTime() {
      m_StartTimeMs = System.currentTimeMillis() - 2 * m_TwiddlerWindow.getProgressMax();
      m_TimeMs = 0;
  }

   ////////////////////////////////////////////////////////////////////////////
   // Discard the current interval's end so it can continue to grow.
   private void continueTime() {
      m_TimeMs = 0;
  }

   /////////////////////////////////////////////////////////////////////////////
   private void setChordTimes(boolean keys) {
      if (m_ChordTimes != null) {
         m_ChordTimes.persist("");
      }
      m_ChordTimes = new ChordTimes(keys, isRightHand());
   }

   // Data /////////////////////////////////////////////////////////////////////
   private static String m_HomeDir;
   private TwiddlerWindow m_TwiddlerWindow;
   private TextPanel m_TextPanel;
   private KeyMap m_KeyMap;
   private KeyPressList m_KeysPressed;
   private Timer m_TimerKeyWait;
   private int m_KeyWaitMsec;
   private long m_StartTimeMs;
   private int m_TimeMs;
   private long m_KeyStartMs;
   private boolean m_Timed;
   private ChordTimes m_ChordTimes;
   private double m_ProgressFactor;

   // Main /////////////////////////////////////////////////////////////////////
   public static void main(String[] argv) {
      m_HomeDir = argv.length == 0 ? "." : argv[0];
      Log.init(Log.ExitOnError);
      Persist.init("twidlit.properties", m_HomeDir, "pref");
      Pref.init("twidlit.preferences", Persist.get("pref.dir"), "pref");
      Pref.setIconPath("/data/icon.gif");
      if (Pref.getBool("write.log", true)) {
         Log.setFile(Io.createFile(m_HomeDir, "twidlit.log"));
      }
      KeyPress.init();
      Twidlit twidlit = new Twidlit();
   }
}
