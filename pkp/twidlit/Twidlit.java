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
import java.util.ArrayList;
import pkp.twiddle.Assignment;
import pkp.twiddle.Chord;
import pkp.twiddle.ThumbKeys;
import pkp.twiddle.KeyMap;
import pkp.twiddle.KeyPress;
import pkp.twiddle.KeyPressList;
import pkp.twiddle.Twiddle;
import pkp.ui.PersistentFrame;
import pkp.util.Persist;
import pkp.util.Pref;
import pkp.util.Log;
import pkp.io.Io;

////////////////////////////////////////////////////////////////////////////////
class Twidlit extends PersistentFrame implements WindowListener, KeyListener, ActionListener, Log.Quitter {

   /////////////////////////////////////////////////////////////////////////////
   Twidlit() {
      super();
      Log.setWindow(this);
      Log.setQuitter(this);
      setIconImage(Pref.getIcon().getImage());
      setFocusable(true);
      requestFocusInWindow();
      setFocusTraversalKeysEnabled(false);
      addWindowListener(this);
      addKeyListener(this);
      setTitle("Twidlit");
      setResizable(true);

      m_ChordTimes = new ChordTimes();
      m_MenuBar = new TwidlitMenu(this);
      setJMenuBar(m_MenuBar);

      m_KeysPressed = new KeyPressList();
      setKeyWaitMsec(Pref.getInt("key.wait.msec"));
      m_StartTimeMs = 0;
      m_TimeMs = 0;
      
      pack();
      setVisible(true);
      // sets m_KeyMap & m_ChordSource
      m_MenuBar.start();
      // uses m_ChordSource
      nextTwiddle(null);
      startTime();
   }

   /////////////////////////////////////////////////////////////////////////////
   String getHomeDir() { return m_HomeDir; }

   /////////////////////////////////////////////////////////////////////////////
   KeyMap getKeyMap() {
      return m_KeyMap;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   void setKeyMap(KeyMap km) {
      m_KeyMap = km;
      m_ChordSource = createChordSource(m_KeyMap, m_ChordTimes.getCounts());
   }
   
   ///////////////////////////////////////////////////////////////////
   void extendTitle(String extension) {
      setTitle(getClass().getSimpleName() + " - " + extension);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   void setRightHand(boolean right) {
      m_ChordTimes.setRightHand(right);
      if (m_KeyMap != null) {
         m_ChordSource = createChordSource(m_KeyMap, m_ChordTimes.getCounts());
      }
   }
   
   /////////////////////////////////////////////////////////////////////////////
   boolean isRightHand() {
      return m_ChordTimes.isRightHand();
   }
   
   /////////////////////////////////////////////////////////////////////////////
   void clearTimes() {
      m_ChordTimes.clear();
      m_ChordSource = createChordSource(m_KeyMap, m_ChordTimes.getCounts());
   }
   
   /////////////////////////////////////////////////////////////////////////////
   SortedChordTimes getChordTimes() {
      return new SortedChordTimes(m_ChordTimes);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   @Override // Log.Quitter
   public void quit() {
      setVisible(false);
      m_ChordTimes.persist("");
      if (m_MenuBar != null) {
         m_MenuBar.close();
      }
      Log.close();
      // call this explicitly since we are not DISPOSE_ON_CLOSE
      dispose();
      // close saves to persist, so write persist last
      Pref.writePersist();
      System.exit(0);
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // WindowListener
   public void windowActivated(WindowEvent e) {
      requestFocusInWindow();
   }
   @Override // WindowListener
   public void windowDeactivated(WindowEvent e) {
      requestFocusInWindow();
   }
   @Override // WindowListener
   public void windowDeiconified(WindowEvent e) {
      requestFocusInWindow();
   }
   @Override // WindowListener
   public void windowIconified(WindowEvent e) {
   }
   @Override // WindowListener
   public void windowOpened(WindowEvent e) {
      requestFocusInWindow();
   }
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
      if (kp.getKeyCode() == 0) {
         return;
      }
//System.out.printf("keyPressed %s %s%n", kp.toString(KeyPress.Format.HEX), kp.toString());
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
//System.out.println("actionPerformed m_Assignment " + m_Assignment.toString());
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
      if (pressed.hasSameKeys(m_Assignment)) {
//System.out.printf("%d %s timeMs %d%n", tw.getChord().toInt(), tw.getChord(), m_TimeMs);
         if (!m_Timed
          || m_ChordTimes.add(tw.getChord().toInt(), 
                              tw.getThumbKeys().getCount(), 
                              m_TimeMs)) {
//System.out.printf("%s %s%n", tw.getChord(), m_ChordTimes.getTimes(tw.getChord().toInt(), 0));
            m_ChordSource.next();
         }
         nextTwiddle(tw);
         startTime();
      } else {
         m_MenuBar.getTwiddlerWindow().markMismatch(tw);
         continueTime();
      }
   }
   
   /////////////////////////////////////////////////////////////////////////////
   void setKeyWaitMsec(int newValue) {
      m_KeyWaitMsec = newValue;
      m_TimerKeyWait = new Timer(m_KeyWaitMsec, this);
      m_TimerKeyWait.setActionCommand("key.collection.complete");
   }

   /////////////////////////////////////////////////////////////////////////////
   void setTimed(boolean set) {
      m_Timed = set;
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private static ChordSource createChordSource(KeyMap keyMap, int[] counts) {
      ArrayList<Byte>[] chords = (ArrayList<Byte>[])new ArrayList[ChordTimes.sm_SPAN + 1];
      for (int i = 0; i <= ChordTimes.sm_SPAN; ++i) {
         chords[i] = new ArrayList<Byte>();
      }
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         Twiddle tw = new Twiddle(i + 1, 0);
         if (null != keyMap.getKeyPressList(tw)) {
//System.out.printf("%s:%d ", new Chord(i + 1), counts[i]);
            chords[counts[i]].add((byte)(i + 1));
         }
      }
//System.out.println();
      return new ChordSource(chords);
   }

   ////////////////////////////////////////////////////////////////////////////
   private void nextTwiddle(Twiddle prev) {
      Twiddle tw = new Twiddle(m_ChordSource.get(), 0);
      m_Assignment = new Assignment(tw, tw.getKeyPressList(m_KeyMap));
      m_MenuBar.getTwiddlerWindow().show(m_Assignment.getTwiddle(0), prev);
  }

   ////////////////////////////////////////////////////////////////////////////
   private void startTime() {
      m_StartTimeMs = System.currentTimeMillis();
      m_TimeMs = 0;
  }

   ////////////////////////////////////////////////////////////////////////////
   private void continueTime() {
      m_TimeMs = 0;
  }

   // Data /////////////////////////////////////////////////////////////////////
   private static String m_HomeDir;
   private TwidlitMenu m_MenuBar;
   private ChordSource m_ChordSource;
   private KeyMap m_KeyMap;
   private Assignment m_Assignment;
   private KeyPressList m_KeysPressed;
   private Timer m_TimerKeyWait;
   private int m_KeyWaitMsec;
   private long m_StartTimeMs;
   private int m_TimeMs;
   private long m_KeyStartMs;
   private boolean m_Timed;
   private ChordTimes m_ChordTimes;

   // Main /////////////////////////////////////////////////////////////////////
   public static void main(String[] argv) {
      m_HomeDir = argv.length == 0 ? "." : argv[0];
      Persist.init("TwidlitPersist.properties", m_HomeDir, "pref");
      Pref.init("TwidlitPreferences.txt", Persist.get("pref.dir"), "pref");
      Pref.setIconPath("/data/icon.gif");
      Log.init(Io.createFile(m_HomeDir, "TwidlitLog.txt"), Log.ExitOnError);
      KeyPress.init();
      Chord.use4Finger(!Pref.get("write.syntax", "4finger").toLowerCase().equals("0MRL"));
      Twidlit twidlit = new Twidlit();
   }
}
