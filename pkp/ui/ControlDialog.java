/**
 * Copyright 2015 Pushkar Piggott
 *
 * ControlDialog.java
 */
 package pkp.ui;

import java.awt.Window;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;
import pkp.util.Pref;

///////////////////////////////////////////////////////////////////////////////
public class ControlDialog extends PersistentDialog {

   ////////////////////////////////////////////////////////////////////////////
   public interface CloseHandler {
      public void closed();
   }

   ////////////////////////////////////////////////////////////////////////////
   public ControlDialog(Window owner, String title) {
      super(owner, title);
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      setIconImage(Pref.getIcon().getImage());
      setResizable(true);
      JPanel cp = (JPanel)getContentPane();
      cp.setLayout(new BorderLayout());
      JPanel p = new JPanel();
      p.setLayout(new BorderLayout());
      p.setBorder(BorderFactory.createLineBorder(Pref.getColor("border.color")));
      cp.add(p, BorderLayout.CENTER);
      m_CenterPanel = new Box(BoxLayout.PAGE_AXIS);
      m_CenterPanel.setOpaque(true);
      m_CenterPanel.setBackground(Pref.getColor("background.color"));
      int sp = Pref.getInt("window.border.size");
      m_CenterPanel.setBorder(new EmptyBorder(sp, sp, sp, sp));
      p.add(m_CenterPanel, BorderLayout.CENTER);
      m_BottomPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
      cp.add(m_BottomPanel, BorderLayout.PAGE_END);
   }

   ////////////////////////////////////////////////////////////////////////////
   public Box getBox() {
      return m_CenterPanel;
   }

   ////////////////////////////////////////////////////////////////////////////
   public void addButton(JButton b) {
      m_BottomPanel.add(b);
   }

   // Private /////////////////////////////////////////////////////////////////

   // Data /////////////////////////////////////////////////////////////////////
   private Box m_CenterPanel;
   private JPanel m_BottomPanel;
}
