/**
 * Copyright 2015 Pushkar Piggott
 *
 * PersistentMenuBar.java
 */

package pkp.ui;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ButtonGroup;
import pkp.util.Persistent;
import pkp.util.Persist;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
public class PersistentMenuBar extends JMenuBar implements ActionListener, ItemListener, Persistent {

   ////////////////////////////////////////////////////////////////////////////
   public static void persist(JMenu menu, String tag) {
      for (int j = 0; j < menu.getItemCount(); ++j) {
         if (menu.getItem(j) instanceof JRadioButtonMenuItem) {
            JRadioButtonMenuItem rbItem = (JRadioButtonMenuItem)menu.getItem(j);
            if (rbItem.isSelected()) {
               Persist.set(menu.getText(), Persist.toTag(tag + rbItem.getActionCommand()));
            }
         } else
         if (menu.getItem(j) instanceof JCheckBoxMenuItem) {
            JCheckBoxMenuItem cbItem = (JCheckBoxMenuItem)menu.getItem(j);
            Persist.set(tag + menu.getText() + ' ' + cbItem.getText(), cbItem.getState() ? "true" : "false");
         } else
         if (menu.getItem(j) instanceof JMenu) {
            persist((JMenu)menu.getItem(j), tag);
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // Component
   public void setVisible(boolean set) {
      if (!set) {
         persist("");
      }
      super.setVisible(set);
   }

   ///////////////////////////////////////////////////////////////////
   @Override // Persistent
   public void persist(String tag) {
      for (int i = 0; i < getMenuCount(); ++i) {
         persist(getMenu(i), tag);
      }
   }

   ///////////////////////////////////////////////////////////////////
   @Override // ActionListener
   public void actionPerformed(ActionEvent e) {
      Log.err("PersistentMenuBar.actionPerformed() not implemented");
   }

   ///////////////////////////////////////////////////////////////////
   @Override // ItemListener
   public void itemStateChanged(ItemEvent e) {
      Log.err("PersistentMenuBar.itemStateChanged() not implemented");
   }

   // Protected //////////////////////////////////////////////////////

   ///////////////////////////////////////////////////////////////////
   protected JMenuItem add(JMenu menu, String text) {
      JMenuItem item = new JMenuItem(text);
      menu.add(item);
      item.addActionListener(this);
      return item;
   }

   ///////////////////////////////////////////////////////////////////
   protected JRadioButtonMenuItem addRadioItem(JMenu menu, String text, ButtonGroup buttonGroup) {
      String persist = Persist.get(menu.getText());
//System.out.println("addRadioItem:" + menu.getText() + ":" + persist);
      boolean selected = persist != null && Persist.match(persist, text);
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(text, selected);
      menu.add(item);
      item.setActionCommand(text);
      item.addActionListener(this);
      buttonGroup.add(item);
      return item;
   }

   ///////////////////////////////////////////////////////////////////
   protected JCheckBoxMenuItem addCheckItem(JMenu menu, String text) {
      return addCheckItem(menu, text, true);
   }

   ///////////////////////////////////////////////////////////////////
   protected JCheckBoxMenuItem addCheckItem(JMenu menu, String text, boolean enabled) {
      String persist = Persist.get(menu.getText() + " " + text);
      boolean selected = persist != null && persist.toLowerCase().equals("true");
      JCheckBoxMenuItem item = new JCheckBoxMenuItem(text, selected);
      menu.add(item);
      item.addItemListener(this);
      item.setEnabled(enabled);
      return item;
   }

   ///////////////////////////////////////////////////////////////////
   protected void setStateFromCheckItems() {
      for (int i = 0; i < getMenuCount(); ++i) {
         setStateFromCheckItems(getMenu(i), 0);
      }
   }

   ///////////////////////////////////////////////////////////////////
   protected void setStateFromCheckItems(JMenu menu, int start) {
      for (int i = start; i < menu.getItemCount(); ++i) {
         if (menu.getItem(i) instanceof JCheckBoxMenuItem
          && ((JCheckBoxMenuItem)menu.getItem(i)).isEnabled()) {
            itemStateChanged((JCheckBoxMenuItem)menu.getItem(i));
         }
      }
   }

   ///////////////////////////////////////////////////////////////////
   protected void itemStateChanged(JCheckBoxMenuItem item) {
      Log.err("PersistentMenuBar.itemStateChanged() not implemented");
   }
}
