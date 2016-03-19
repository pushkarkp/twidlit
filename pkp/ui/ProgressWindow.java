/**
 * Copyright 2015 Pushkar Piggott
 *
 * ProgressWindow.java
 */
package pkp.ui;

import javax.swing.JProgressBar;
import javax.swing.JLabel;
import pkp.util.Pref;

///////////////////////////////////////////////////////////////////////////////
public class ProgressWindow extends ControlWindow {

   ////////////////////////////////////////////////////////////////////////////
   public ProgressWindow(String title, String label, int min, int max) {
      super(title);
      if (label != null && !"".equals(label)) {
         add(new JLabel(label));
      }
      m_ProgressBar = new JProgressBar(min, max);
      m_ProgressBar.setBackground(Pref.getColor("#.background.color"));
      add(m_ProgressBar);
      m_Count = min;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public void setValue(int value) {
      m_ProgressBar.setValue(value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public int getMinimum() {
      return m_ProgressBar.getMinimum();
   }

   ////////////////////////////////////////////////////////////////////////////
   public int getMaximum() {
      return m_ProgressBar.getMaximum();
   }

   ////////////////////////////////////////////////////////////////////////////
   public void step() {
      m_ProgressBar.setValue(++m_Count);
   }

   // Data ////////////////////////////////////////////////////////////////////
   private JProgressBar m_ProgressBar;
   private int m_Count;
}
