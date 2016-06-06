/**
 * Copyright 2015 Pushkar Piggott
 *
 *  Splash.java
 */
package pkp.ui;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import javax.swing.SwingConstants;
import javax.swing.ImageIcon;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Font;
// no dependence on pkp initialized
//import pkp.util.Persist;
//import pkp.util.Pref;
//import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class Splash extends JFrame {
   
   ////////////////////////////////////////////////////////////////////////////
   public Splash(int width, int height, String text, int fontSize, Color bg) {
      Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
      int x = (int)((ss.getWidth() - width + 0.5) / 2.0);
      int y = (int)((ss.getHeight() - height + 0.5) / 2.0);
      setBounds(new Rectangle(x, y, width, height));
      JLabel lb = new JLabel(text, SwingConstants.CENTER);
      lb.setBorder(new EmptyBorder(0, 20, 10, 20));
      lb.setFont(new Font(lb.getFont().getFontName(), Font.PLAIN, fontSize));
      getContentPane().add(lb);
      getContentPane().setBackground(bg);
      setUndecorated(true);
      setVisible(true);
   }      
}
