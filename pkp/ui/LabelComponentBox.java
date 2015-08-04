/**
 * LabelComponentBox.java
 */
package pkp.ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JComponent;

///////////////////////////////////////////////////////////////////////////////
public class LabelComponentBox extends Box {

   ////////////////////////////////////////////////////////////////////////////
   public LabelComponentBox(String label, JComponent component) {
      super(BoxLayout.LINE_AXIS);
      setOpaque(false);
      m_Label = new JLabel(label);
      add(m_Label);
      add(Box.createHorizontalGlue());
      m_Component = component;
      add(m_Component);
   }

   ////////////////////////////////////////////////////////////////////////////
   public JLabel getLabel() { return m_Label; }
   public JComponent getComponent() { return m_Component; }

   // Data ////////////////////////////////////////////////////////////////////
   private JLabel m_Label;
   private JComponent m_Component;
}
