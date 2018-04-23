/**
 * Copyright 2015 Pushkar Piggott
 *
 * PersistentWindow.java
 */

package pkp.ui;

import java.awt.*;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import pkp.util.Persist;
import pkp.util.Pref;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
public class PersistentWindow
   extends PersistentFrame
   implements WindowListener {

   /////////////////////////////////////////////////////////////////////////////
   public PersistentWindow() {
      addWindowListener(this);
      setResizable(true);
      restoreIconified();
   }

   /////////////////////////////////////////////////////////////////////////////
   public void restoreIconified() {
      if (Persist.getBool(getPersistName() + ".minimized")) {
         setState(Frame.ICONIFIED);
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // WindowListener
   public void windowActivated(WindowEvent e) {}
   @Override // WindowListener
   public void windowDeactivated(WindowEvent e) { save(); }
   @Override // WindowListener
   public void windowDeiconified(WindowEvent e) {
      Persist.unset(getPersistName() + ".minimized");
   }
   @Override // WindowListener
   public void windowIconified(WindowEvent e) {
      Persist.set(getPersistName() + ".minimized", true);
   }
   @Override // WindowListener
   public void windowOpened(WindowEvent e) {}
   @Override // WindowListener
   public void windowClosed(WindowEvent e) {}
   @Override // WindowListener
   public void windowClosing(WindowEvent e) { save(); }
}
