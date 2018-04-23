/**
 * Copyright 2015 Pushkar Piggott
 *
 * ControlWindow.java
 */
 package pkp.ui;

import javax.swing.JFrame;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import pkp.util.Pref;

///////////////////////////////////////////////////////////////////////////////
public class ControlWindow extends PersistentWindow {

   ////////////////////////////////////////////////////////////////////////////
   public ControlWindow(String title) {
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      setTitle(title);
      setIconImage(Pref.getIcon().getImage());
      JPanel cp = (JPanel)getContentPane();
      cp.setBackground(Pref.getColor("#.background.color"));
      cp.setLayout(new BoxLayout(cp, BoxLayout.PAGE_AXIS));
      int sp = Pref.getInt("#.window.border.size");
      cp.setBorder(new EmptyBorder(sp, sp, sp, sp));
   }
}
