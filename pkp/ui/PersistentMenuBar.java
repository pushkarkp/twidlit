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
      int rb = 0;
      boolean inRb = false;
      for (int j = 0; j < menu.getItemCount(); ++j) {
         if (menu.getItem(j) instanceof JRadioButtonMenuItem) {
            if (!inRb) {
               inRb = true;
               ++rb;
            }
            JRadioButtonMenuItem rbItem = (JRadioButtonMenuItem)menu.getItem(j);
            if (rbItem.isSelected()) {
               Persist.set("#." + tag + menu.getText() + "." + rb, Persist.toTag("#." + rb + "." + rbItem.getActionCommand()));
            }
         } else {
            inRb = false;
            if (menu.getItem(j) instanceof JCheckBoxMenuItem) {
               JCheckBoxMenuItem cbItem = (JCheckBoxMenuItem)menu.getItem(j);
               Persist.set("#." + tag + menu.getText() + ' ' + cbItem.getText(), cbItem.getState() ? "true" : "false");
            } else
            if (menu.getItem(j) instanceof JMenu) {
               persist((JMenu)menu.getItem(j), tag);
            }
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
      boolean selected = false;
      for (int i = 1; !selected; ++i) {
         String selection = Persist.get("#." + menu.getText() + "." + i);
         if (selection == null) {
            break;
         }
         selected = Persist.match("#." + selection, "#." + i + "." + text);
      }
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
      boolean selected = Persist.getBool("#." + menu.getText() + '.' + text);
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
