/**
 * SliderBox.java
 */
package pkp.ui;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JSlider;

///////////////////////////////////////////////////////////////////////////////
public class SliderBox extends Box {

   ////////////////////////////////////////////////////////////////////////////
   public SliderBox(String label, JSlider slider) {
      super(BoxLayout.LINE_AXIS);
      setOpaque(false);
      m_Slider = slider;
      add(m_Slider);
      add(Box.createHorizontalGlue());
      m_Label = new JLabel("  " + label);
      add(m_Label);
   }

   ////////////////////////////////////////////////////////////////////////////
   public JLabel getLabel() { return m_Label; }
   public JSlider getSlider() { return m_Slider; }

   ////////////////////////////////////////////////////////////////////////////
   //public static final long serialVersionUID = 0xAA21L;

   // Data ////////////////////////////////////////////////////////////////////
   JLabel m_Label;
   JSlider m_Slider;
}
