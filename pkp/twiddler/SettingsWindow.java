/**
 * Copyright 2015 Pushkar Piggott
 *
 * SettingsWindow.java
 */

package pkp.twiddler;

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JSlider;
import pkp.ui.ControlWindow;
import pkp.ui.SliderBuilder;
import pkp.ui.LabelComponentBox;
import pkp.util.Pref;

///////////////////////////////////////////////////////////////////////////////
public class SettingsWindow extends ControlWindow implements Settings  {

   ////////////////////////////////////////////////////////////////////////////
   public SettingsWindow(Settings cfg) {
      super("Twiddler Settings");
      m_IntCfg = cfg.getIntSettings();
      JPanel cp = (JPanel)getContentPane();
      Box b = new Box(BoxLayout.LINE_AXIS);
      cp.add(b);
      b.add(Box.createHorizontalGlue());
      b.add(new JLabel("Version "));
      b.add(new JLabel(String.format("%d.%d", m_IntCfg.MAJOR_VERSION.getValue(), m_IntCfg.MINOR_VERSION.getValue())));
      m_MinorVersion = m_IntCfg.MINOR_VERSION.getValue();
      int sp = Pref.getInt("#.control.separation.size");
      cp.add(Box.createRigidArea(new Dimension(0, sp)));
      cp.add(new LabelComponentBox(m_IntCfg.MOUSE_EXIT_DELAY.m_Name, SliderBuilder.build(0, 5000, 1000, m_IntCfg.MOUSE_EXIT_DELAY.getValue())));
      cp.add(Box.createRigidArea(new Dimension(0, sp)));
      cp.add(new LabelComponentBox(m_IntCfg.MS_BETWEEN_TWIDDLES.m_Name, SliderBuilder.build(0, 5000, 1000, m_IntCfg.MS_BETWEEN_TWIDDLES.getValue())));
      cp.add(Box.createRigidArea(new Dimension(0, sp)));
      cp.add(new LabelComponentBox(m_IntCfg.START_SPEED.m_Name, SliderBuilder.build(0, 10, 1, m_IntCfg.START_SPEED.getValue())));
      cp.add(Box.createRigidArea(new Dimension(0, sp)));
      cp.add(new LabelComponentBox(m_IntCfg.FAST_SPEED.m_Name, SliderBuilder.build(0, 10, 1, m_IntCfg.FAST_SPEED.getValue())));
      cp.add(Box.createRigidArea(new Dimension(0, sp)));
      cp.add(new LabelComponentBox(m_IntCfg.MOUSE_ACCELERATION.m_Name, SliderBuilder.build(0, 255, 50, m_IntCfg.MOUSE_ACCELERATION.getValue())));
      cp.add(Box.createRigidArea(new Dimension(0, sp)));
      cp.add(new LabelComponentBox(m_IntCfg.MS_REPEAT_DELAY.m_Name, SliderBuilder.build(0, 2500, 500, m_IntCfg.MS_REPEAT_DELAY.getValue())));
      Dimension size = new Dimension(40, 10);
      for (int i = 2; i < cp.getComponentCount(); i += 2) {
         LabelComponentBox vsb = (LabelComponentBox)cp.getComponent(i);
         ((JSlider)vsb.getComponent()).setSnapToTicks(false);
//         vsb.setTextPreferredSize(size);
      }
      cp.add(Box.createRigidArea(new Dimension(0, sp)));
      b = new Box(BoxLayout.LINE_AXIS);
      cp.add(b);
      b.add(new JCheckBox("Mass storage enabled", cfg.isEnableStorage()));
      b.add(Box.createHorizontalGlue());
      b.add(new JCheckBox("Key repeat enabled", cfg.isEnableRepeat()));
      for (int i = 0; i < b.getComponentCount(); i += 2) {
         ((JComponent)b.getComponent(i)).setOpaque(false);
      }
      pack();
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // Settings
   public IntSettings getIntSettings() { return m_IntCfg; }

   ////////////////////////////////////////////////////////////////////////////
   @Override // Settings
   public boolean isEnableRepeat() { return getBool(1); }
   @Override // Settings
   public boolean isEnableStorage() { return getBool(0); }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private int getVersion() {
      return Integer.parseInt(((JLabel)((Box)getContentPane().getComponent(0)).getComponent(2)).getText());
   }

   ////////////////////////////////////////////////////////////////////////////
   private int getInt(int i) {
      return ((JSlider)((LabelComponentBox)getContentPane().getComponent((i + 1) * 2)).getComponent()).getValue();
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean getBool(int i) {
      return ((JCheckBox)((Box)getContentPane().getComponent(sm_BOOL_BOX)).getComponent(i * 2)).isSelected();
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static final int sm_BOOL_BOX = 14;
   private IntSettings m_IntCfg;
   private int m_MinorVersion;
}
