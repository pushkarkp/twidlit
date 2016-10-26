/**
 * Copyright 2015 Pushkar Piggott
 *
 * LabelComponentBox.java
 */
package pkp.ui;

import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JComponent;

///////////////////////////////////////////////////////////////////////////////
public class LabelComponentBox extends Box {

   ////////////////////////////////////////////////////////////////////////////
   public LabelComponentBox(String label, JComponent component) {
      this(label, component, 0);
   }

   ////////////////////////////////////////////////////////////////////////////
   public LabelComponentBox(String label, JComponent component, int height) {
      super(BoxLayout.LINE_AXIS);
      setOpaque(false);
      m_Label = new JLabel(label);
      add(m_Label);
      add(Box.createHorizontalGlue());
      if (height != 0) {
         add(Box.createRigidArea(new Dimension(0, height)));
      }
      m_Component = component;
      add(m_Component);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setEnabled(boolean set) {
      m_Label.setEnabled(set);
      m_Component.setEnabled(set);
   }

   ////////////////////////////////////////////////////////////////////////////
   public JLabel getLabel() { return m_Label; }
   public JComponent getComponent() { return m_Component; }

   // Data ////////////////////////////////////////////////////////////////////
   private JLabel m_Label;
   private JComponent m_Component;
}
