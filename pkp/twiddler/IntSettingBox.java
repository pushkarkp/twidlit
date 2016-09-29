/**
 * Copyright 2016 Pushkar Piggott
 *
 * IntSettingBox.java
 */
package pkp.twiddler;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Insets;
import javax.swing.JSlider;
import javax.swing.JButton;
import javax.swing.Box;
import pkp.ui.LabelComponentBox;
import pkp.ui.SliderBuilder;

///////////////////////////////////////////////////////////////////////////////
public class IntSettingBox
   extends LabelComponentBox
   implements ActionListener {

   ////////////////////////////////////////////////////////////////////////////
   public IntSettingBox(String label, int stepWidth, int height, int min, int max, int step, int deflt, int value) {
      super(label, SliderBuilder.build(min, max, step, value), height);
      getSlider().setSnapToTicks(false);
      getSlider().setPreferredSize(new Dimension(stepWidth * (max - min) / step, height));
      add(Box.createRigidArea(new Dimension(6, 0)));
      JButton b = new JButton(sm_CAPTION);
      b.setPreferredSize(new Dimension(15, 20));
      b.setMargin(new Insets(0, 0, 0, 0));
      b.addActionListener(this);
      m_Default = deflt;
      add(b);
   }

   ////////////////////////////////////////////////////////////////////////////
   public int getValue() { return getSlider().getValue(); }

   ///////////////////////////////////////////////////////////////////
   @Override // ActionListener
   public void actionPerformed(ActionEvent e) {
      getSlider().setValue(m_Default);
   }

   ////////////////////////////////////////////////////////////////////////////
   private JSlider getSlider() { return (JSlider)getComponent(); }

   // Data ////////////////////////////////////////////////////////////////////
   private static final String sm_CAPTION = "x";
   private int m_Default;
}
