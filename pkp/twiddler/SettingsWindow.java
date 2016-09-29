/**
 * Copyright 2015 Pushkar Piggott
 *
 * SettingsWindow.java
 */

package pkp.twiddler;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JSlider;
import pkp.ui.ControlWindow;
import pkp.util.Persist;
import pkp.util.Persistent;
import pkp.util.Pref;

///////////////////////////////////////////////////////////////////////////////
public class SettingsWindow 
   extends ControlWindow
   implements Settings, Persistent, ActionListener  {

   ////////////////////////////////////////////////////////////////////////////
   public SettingsWindow(Settings cfg) {
      super("Twiddler Settings");
      m_SliderHeight = Pref.getInt("#.slider.height");
      m_SliderStepWidth = Pref.getInt("#.slider.step.width");
      m_Version = getVersion(Persist.get(sm_VERSION_PERSIST, "2"));
      m_IntCfg = cfg.getIntSettings();
      m_BoolCfg = cfg.getBoolSettings();
      JPanel cp = (JPanel)getContentPane();
      Box b = new Box(BoxLayout.LINE_AXIS);
      cp.add(b);
      b.add(Box.createHorizontalGlue());
      JLabel twiddler = new JLabel("Twiddler ");
      twiddler.setFont(twiddler.getFont().deriveFont(20.0f));
      b.add(twiddler);
      m_Button = new JButton(getVersionName(m_Version));
      m_Button.setFont(m_Button.getFont().deriveFont(20.0f));
      m_Button.setMargin(new Insets(0, 2, 0, 2));
      m_Button.addActionListener(this);
      b.add(m_Button);
      b.add(Box.createRigidArea(new Dimension(0, m_SliderHeight)));
      b.add(Box.createHorizontalGlue());
      b.add(new JLabel("File Version "));
      b.add(new JLabel(String.format("%d.%d", m_IntCfg.MAJOR_VERSION.getValue(), m_IntCfg.MINOR_VERSION.getValue())));
      m_MinorFileVersion = m_IntCfg.MINOR_VERSION.getValue();
      m_SettingsPanel = new JPanel();
      m_SettingsPanel.setLayout(new BoxLayout(m_SettingsPanel, BoxLayout.PAGE_AXIS));
      cp.add(m_SettingsPanel);
      showSettings(m_SettingsPanel);
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // Settings
   public IntSettings getIntSettings() {
      int i = 0;
      for (IntSettings is: m_IntCfg.values()) {
         // skip immutable major and minor version
         if (is.ordinal() > 1 && is.isCurrent(m_Version)) {
            is.setValue(getInt(i));
            ++i;
         }
      }
      return m_IntCfg;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // Settings
   public BoolSettings getBoolSettings() {
      int i = 0;
      for (BoolSettings bs: m_BoolCfg.values()) {
         if (bs.isCurrent(m_Version)) {
           bs.setValue(getBool(i));
           ++i;
         }
      }
      return m_BoolCfg;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // Settings
   public int getVersion() {
      return m_Version;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // ActionListener
   public void actionPerformed(ActionEvent e) {
      getIntSettings();
      getBoolSettings();
      switch (e.getActionCommand()) {
      case "2":
         m_Version = getVersion("3");
         break;
      case "3":
         m_Version = getVersion("2");
         break;
      }
      m_Button.setText(getVersionName(m_Version));
      showSettings(m_SettingsPanel);
   }

   ///////////////////////////////////////////////////////////////////
   @Override // Persistent
   public void persist(String tag) {
      Persist.set(sm_VERSION_PERSIST, getVersionName(m_Version));
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private void showSettings(JPanel sp) {
      sp.removeAll();
      addIntSettings(m_SettingsPanel);
      addBoolSettings(m_SettingsPanel);
      for (int i = 0; i < m_SettingsPanel.getComponentCount(); ++i) {
         ((JComponent)m_SettingsPanel.getComponent(i)).setOpaque(false);
      }
      getContentPane().revalidate();
      pack();
   }

   ////////////////////////////////////////////////////////////////////////////
   private void addIntSettings(JPanel sp) {
      m_BoolOffset = 0;
      for (IntSettings is: m_IntCfg.values()) {
         // skip immutable major and minor version
         if (is.ordinal() > 1 && is.isCurrent(m_Version)) {
            sp.add(new IntSettingBox(is.getName() + is.getUnits() + "  ", m_SliderStepWidth, m_SliderHeight, is.getMin(), is.getMax(), is.getStep(), is.getDefault(), is.getValue()));
            m_BoolOffset += 1;
         }
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private void addBoolSettings(JPanel sp) {
      sp.add(Box.createRigidArea(new Dimension(0, m_SliderHeight / 4)));
      m_BoolOffset += 1;
      for (BoolSettings bs: m_BoolCfg.values()) {
         if (bs.isCurrent(m_Version)) {
            sp.add(new JCheckBox(bs.getName(), bs.is()));
         }
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private int getInt(int i) {
      return ((IntSettingBox)m_SettingsPanel.getComponent(i)).getValue();
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean getBool(int i) {
      int offset = i + m_BoolOffset;
      return ((JCheckBox)(m_SettingsPanel.getComponent(offset))).isSelected();
   }

   ////////////////////////////////////////////////////////////////////////////
   private int getVersion(String v) {
      for (int i = 0; i < sm_VERSION_NAMES.length; ++i) {
         if (v.equals(sm_VERSION_NAMES[i])) {
            return i + 1;
         }
      }
      return 0;
   }

   ////////////////////////////////////////////////////////////////////////////
   private String getVersionName(int v) {
      if (v < 1 || v > sm_VERSION_NAMES.length) {
         return "";
      }
      return sm_VERSION_NAMES[v - 1];
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static final String sm_VERSION_PERSIST = "#.twiddler.version";
   private static final String sm_VERSION_NAMES[] = new String[]{"2", "3"};
   private final int m_SliderHeight;
   private final int m_SliderStepWidth;
   private int m_Version;
   private int m_BoolOffset;
   private JButton m_Button;
   private JPanel m_SettingsPanel;
   private JPanel m_BoolPanel;
   private IntSettings m_IntCfg;
   private BoolSettings m_BoolCfg;
   private int m_MinorFileVersion;
}
