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
import java.util.Random;
import pkp.twiddle.Assignment;
import pkp.twiddle.Chord;
import pkp.twiddle.ThumbKeys;
import pkp.twiddle.KeyMap;
import pkp.twiddle.KeyPress;
import pkp.twiddle.KeyPressList;
import pkp.twiddle.Twiddle;
import pkp.util.Persist;
import pkp.util.Pref;
import pkp.util.Log;
import pkp.io.Io;
import pkp.ui.PersistentFrame;

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

      m_Random = new Random();
      m_KeysPressed = new KeyPressList();
      setKeyWaitMsec(Pref.getInt("key.wait.msec"));
      m_StartTimeMs = 0;
      m_TimeMs = 0;
      
      pack();
      setVisible(true);
      m_MenuBar.start();
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
   }
   
   ///////////////////////////////////////////////////////////////////
   void extendTitle(String extension) {
      setTitle(getClass().getSimpleName() + " - " + extension);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   ChordTimes getChordTimes() {
      return m_ChordTimes;
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
//System.out.println("keyPressed " + kp.toString(KeyPress.Format.HEX));
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
      if (m_KeyStartMs != 0) {
         Log.log(String.format("keyPressed event %dms now %dms", 
                  (int)(e.getWhen() - m_KeyStartMs),
                  (int)(System.currentTimeMillis() - m_KeyStartMs)));
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
         Log.warn("Twidlit probably has a different cfg from your Twiddler.");
      }
      m_KeysPressed = new KeyPressList();
      Twiddle tw = pressed.getTwiddle(0);
      if (pressed.hasSameKeys(m_Assignment)) {
         if (m_Timed) {
//System.out.printf("time: %d%n", m_TimeMs);
            m_ChordTimes.add(tw.getChord().toInt(), 
                             tw.getThumbKeys().getCount(), 
                             m_TimeMs);
         }
         nextTwiddle(tw);
      } else {
         m_MenuBar.getTwiddlerWindow().markMismatch(tw);
      }
      startTime();
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
   private Assignment getNext() {
      Assignment asg = null;
      for (int i = 0; asg == null && i < 100; ++i) {
         Twiddle tw;
         if (m_Twiddles == null || m_Twiddles.size() == 0) {
            tw = new Twiddle(m_Random.nextInt(Chord.sm_VALUES) + 1, 0);
         } else {
            tw = m_Twiddles.get(m_Random.nextInt(m_Twiddles.size() + 1) - 1);
         }
         //Twiddle tw = new Twiddle(new Chord("|-|'"), new ThumbKeys("0"));
//if (kpl == null) System.out.println("getNext: " + tw.toString() + " not assigned");
//else System.out.println("getNext: " + tw.toString() + ' ' + kpl.toString() + " allowed");
         asg = new Assignment(tw, tw.getKeyPressList(m_KeyMap));
      }
      if (asg == null) {
         Log.warn("Failed to find an eligible twiddle");
         return null;
      }
      return asg;
   }

   ////////////////////////////////////////////////////////////////////////////
   private void nextTwiddle(Twiddle prev) {
      m_Assignment = getNext();
//System.out.println("nextTwiddle " + m_Assignment.toString());      
      m_MenuBar.getTwiddlerWindow().show(m_Assignment.getTwiddle(0), prev);
  }

   ////////////////////////////////////////////////////////////////////////////
   private void startTime() {
      m_StartTimeMs = System.currentTimeMillis();
      m_TimeMs = 0;
  }

   // Data /////////////////////////////////////////////////////////////////////
   private static String m_HomeDir;
   private static ArrayList<Twiddle> m_Twiddles;
   private TwidlitMenu m_MenuBar;
   private Random m_Random;
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
      m_Twiddles = argv.length < 2 ? null : Twiddle.read(Io.toExistUrl(argv[1], m_HomeDir, ""));
      Twidlit twidlit = new Twidlit();
   }
}
