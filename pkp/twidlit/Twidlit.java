/**
 * Copyright 2015 Pushkar Piggott
 *
 * Twidlit.java
 */

package pkp.twidlit;

import java.awt.*;
import javax.swing.*;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import pkp.twiddle.Chord;
import pkp.twiddle.KeyMap;
import pkp.twiddle.KeyPress;
import pkp.util.Persist;
import pkp.util.Pref;
import pkp.util.Log;
import pkp.io.Io;
import pkp.ui.PersistentFrame;

////////////////////////////////////////////////////////////////////////////////
class Twidlit extends PersistentFrame implements WindowListener, Log.Quitter {

   /////////////////////////////////////////////////////////////////////////////
   Twidlit() {
      super();
      Log.setQuitter(this);
      setIconImage(Pref.getIcon().getImage());
      setFocusable(true);
      requestFocusInWindow();
      setFocusTraversalKeysEnabled(false);
      addWindowListener(this);
      setTitle("Twidlit");
      setResizable(true);
      setContentPane(new JPanel());
      getContentPane().setBackground(Pref.getColor("background.color"));

      m_MenuBar = new TwidlitMenu(this);
      setJMenuBar(m_MenuBar);

      pack();
      setVisible(true);
   }

   /////////////////////////////////////////////////////////////////////////////
   KeyMap getKeyMap() { 
      return m_KeyMap; 
   }
   
   /////////////////////////////////////////////////////////////////////////////
   String getHomeDir() { 
      return m_HomeDir; 
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
   @Override // Log.Quitter
   public void quit() {
      setVisible(false);
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

   // Private /////////////////////////////////////////////////////////////////

   // Data /////////////////////////////////////////////////////////////////////
   private static String m_HomeDir = null;
   private TwidlitMenu m_MenuBar = null;
   private KeyMap m_KeyMap = null;

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
