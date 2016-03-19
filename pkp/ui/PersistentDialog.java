/**
 * Copyright 2015 Pushkar Piggott
 *
 *  PersistentDialog.java
 */
package pkp.ui;

import java.awt.Window;
import java.awt.Rectangle;
import javax.swing.JDialog;
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
