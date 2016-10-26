/**
 * Copyright 2015 Pushkar Piggott
 *
 *  PersistentDialog.java
 */
package pkp.ui;

import java.awt.Window;
import java.awt.Rectangle;
import java.awt.Component;
import javax.swing.JDialog;
import javax.swing.Box;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import pkp.util.Persist;

///////////////////////////////////////////////////////////////////////////////
public class PersistentDialog extends JDialog {

   ////////////////////////////////////////////////////////////////////////////
   public PersistentDialog(Window owner, String title) {
      super(owner, title);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setPersistName(String name) {
      m_PersistName = name;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override
   public void setVisible(boolean set) {
      if (set) {
         String persistName = getPersistName();
//System.out.println(persistName);
         Rectangle r = new Rectangle(Persist.getInt(persistName + ".x", 0),
                                     Persist.getInt(persistName + ".y", 0),
                                     Persist.getInt(persistName + ".w", 500),
                                     Persist.getInt(persistName + ".h", 300));
         setBounds(r);
      }
      super.setVisible(set);
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override
   public void dispose() {
      String persistName = getPersistName();
      Rectangle r = getBounds();
      Persist.set(persistName + ".x", Integer.toString(r.x));
      Persist.set(persistName + ".y", Integer.toString(r.y));
      Persist.set(persistName + ".w", Integer.toString(r.width));
      Persist.set(persistName + ".h", Integer.toString(r.height));
      super.dispose();
   }

   ////////////////////////////////////////////////////////////////////////////
   public void persist(Box p) {
      int n = 0;
      boolean inRb = false;
      for (int j = 0; j < p.getComponentCount(); ++j) {
         if (p.getComponent(j) instanceof JRadioButton) {
            if (!inRb) {
               inRb = true;
               ++n;
            }
            JRadioButton rb = (JRadioButton)p.getComponent(j);
            if (rb.isSelected()) {
               Persist.set(getPersistName() + "." + n, Persist.toTag("#." + n + "." + rb.getActionCommand()));
            }
         } else {
            inRb = false;
         }
      }
   }

   ///////////////////////////////////////////////////////////////////
   protected JRadioButton createRadioButton(String text, ButtonGroup buttonGroup) {
      boolean selected = false;
      for (int i = 1; !selected; ++i) {
         String selection = Persist.get(getPersistName() + ' ' + i);
         if (selection == null) {
            break;
         }
         selected = Persist.match("#." + selection, "#." + i + "." + text);
      }
      JRadioButton rb = new JRadioButton(text, selected);
      rb.setOpaque(false);
      buttonGroup.add(rb);
      return rb;
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   // PersistName is converted to tag in PersistentPrperties.
   // If value is not found, try deleting twidlit.properties
   private String getPersistName() {
      if (m_PersistName == null || "".equals(m_PersistName)) {
         m_PersistName = "#." + (("".equals(getTitle()))
                                 ? getClass().getSimpleName()
                                 : getTitle());
      }
      return m_PersistName;
   }
   
   // Data ////////////////////////////////////////////////////////////////////
   private String m_PersistName = null;
}
