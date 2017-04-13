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
      m_IntCfg = cfg.getIntSettings();
      m_BoolCfg = cfg.getBoolSettings();
      int persistVersion = versionNameToInt(Persist.get(sm_VERSION_PERSIST, "5.3"));
      if (m_IntCfg.FORMAT_VERSION.getValue() == 0) {
         m_IntCfg.FORMAT_VERSION.setValue(sm_VERSION_NAMES[persistVersion - 1].charAt(0) - '0');
      } 
      m_TwiddlerVersion = (m_IntCfg.FORMAT_VERSION.getValue() == 5)
         ? 3
         : sm_VERSION_NAMES[persistVersion - 1].charAt(2) - '0';
      JPanel cp = (JPanel)getContentPane();
      Box b = new Box(BoxLayout.LINE_AXIS);
      cp.add(b);
      b.add(Box.createHorizontalGlue());
      JLabel twiddler = new JLabel("Twiddler ");
      twiddler.setFont(twiddler.getFont().deriveFont(20.0f));
      b.add(twiddler);
      m_TwiddlerVersionButton = newButton("" + m_TwiddlerVersion);
      b.add(m_TwiddlerVersionButton);
      b.add(Box.createRigidArea(new Dimension(0, m_SliderHeight)));
      b.add(Box.createHorizontalGlue());
      b.add(new JLabel(m_IntCfg.FORMAT_VERSION.getName() + " "));
      m_FileVersionButton = newButton("" + m_IntCfg.FORMAT_VERSION.getValue());
      b.add(m_FileVersionButton);
      m_SettingsPanel = new JPanel();
      m_SettingsPanel.setLayout(new BoxLayout(m_SettingsPanel, BoxLayout.PAGE_AXIS));
      cp.add(m_SettingsPanel);
      showSettings(m_SettingsPanel);
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // Settings
   public IntSettings getIntSettings() {
      int v = getVersion();
      int i = 0;
      for (IntSettings is: m_IntCfg.values()) {
         if (is.isGuiItem(v)) {
            is.setValue(getInt(i));
            ++i;
         }
      }
      return m_IntCfg;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // Settings
   public BoolSettings getBoolSettings() {
      int v = getVersion();
      int i = 0;
      for (BoolSettings bs: m_BoolCfg.values()) {
         if (bs.isCurrent(v)) {
           bs.setValue(getBool(i));
           ++i;
         }
      }
      return m_BoolCfg;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // Settings
   public int getVersion() {
      return versionNameToInt(getVersionName());
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // ActionListener
   public void actionPerformed(ActionEvent e) {
      getIntSettings();
      getBoolSettings();
      switch (e.getActionCommand()) {
      case "2":
         m_TwiddlerVersion = 3;
         break;
      case "3":
         m_TwiddlerVersion = 2;
         m_IntCfg.FORMAT_VERSION.setValue(4);
         break;
      case "4":
         m_IntCfg.FORMAT_VERSION.setValue(5);
         m_TwiddlerVersion = 3;
         break;
      case "5":
         m_IntCfg.FORMAT_VERSION.setValue(4);
         break;
      }
      m_TwiddlerVersionButton.setText("" + m_TwiddlerVersion);
      m_FileVersionButton.setText("" + m_IntCfg.FORMAT_VERSION.getValue());
      showSettings(m_SettingsPanel);
   }

   ///////////////////////////////////////////////////////////////////
   @Override // Persistent
   public void persist(String tag) {
      Persist.set(sm_VERSION_PERSIST, getVersionName());
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private JButton newButton(String name) {
      JButton b = new JButton(name);
      b.setFont(b.getFont().deriveFont(20.0f));
      b.setMargin(new Insets(0, 2, 0, 2));
      b.addActionListener(this);
      return b;
   }

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
      int v = getVersion();
      m_BoolOffset = 0;
      for (IntSettings is: m_IntCfg.values()) {
         if (is.isGuiItem(v)) {
            sp.add(new IntSettingBox(is.getName() + is.getUnits() + "  ", m_SliderStepWidth, m_SliderHeight, is.getMin(), is.getMax(), is.getStep(), is.getDefault(), is.getValue()));
            m_BoolOffset += 1;
         }
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private void addBoolSettings(JPanel sp) {
      sp.add(Box.createRigidArea(new Dimension(0, m_SliderHeight / 4)));
      int v = getVersion();
      m_BoolOffset += 1;
      for (BoolSettings bs: m_BoolCfg.values()) {
         if (bs.isCurrent(v)) {
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
   private int versionNameToInt(String v) {
      // Accept just the Twiddler version for backward compatibility.
      if (v.length() == 1) {
         v = "4." + v;
      }
      int i = 0;
      for (; i < sm_VERSION_NAMES.length; ++i) {
         if (v.equals(sm_VERSION_NAMES[i])) {
            return i + 1;
         }
      }
      return i;
   }

   ////////////////////////////////////////////////////////////////////////////
   private String getVersionName() {
      return "" + m_IntCfg.FORMAT_VERSION.getValue() + "." + m_TwiddlerVersion;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static final String sm_VERSION_PERSIST = "#.twiddler.version";
   private static final String sm_VERSION_NAMES[] = new String[]{"4.2", "4.3", "5.3"};
   private final int m_SliderHeight;
   private final int m_SliderStepWidth;
   private int m_TwiddlerVersion;
   private int m_BoolOffset;
   private JButton m_TwiddlerVersionButton;
   private JButton m_FileVersionButton;
   private JPanel m_SettingsPanel;
   private JPanel m_BoolPanel;
   private IntSettings m_IntCfg;
   private BoolSettings m_BoolCfg;
}
