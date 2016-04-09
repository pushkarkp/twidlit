/**
 * Copyright 2015 Pushkar Piggott
 *
 *  PersistentFrame.java
 */
package pkp.ui;

import java.awt.Rectangle;
import javax.swing.JFrame;
import pkp.util.Persist;

///////////////////////////////////////////////////////////////////////////////
// Persistence key is the title unless PersistentName is set.
public class PersistentFrame extends JFrame {

   ////////////////////////////////////////////////////////////////////////////
   @Override
   public void setVisible(boolean set) {
      if (set) {
         String persistName = getPersistName();
//System.out.printf("%s %d%n", Persist.toTag(persistName) + ".x", Persist.getInt(persistName + ".x", 0));
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
      save();
      super.dispose();
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setPersistName(String name) {
      m_PersistName = name;
   }

   ////////////////////////////////////////////////////////////////////////////
   public void changePersistName(String name) {
      save();
      m_PersistName = name;
   }

   ////////////////////////////////////////////////////////////////////////////
   // PersistName is converted to tag in PersistentPrperties.
   // If value is not found, try deleting twidlit.properties
   public String getPersistName() {
//System.out.printf("getPersistName() %s%n", m_PersistName);
      if (m_PersistName == null || "".equals(m_PersistName)) {
         m_PersistName = "#." + (("".equals(getTitle()))
                                 ? getClass().getSimpleName()
                                 : getTitle());
      }
      return m_PersistName;
   }
   
   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private void save() {
      String persistName = getPersistName();
      Rectangle r = getBounds();
//System.out.printf("%s %d%n", Persist.toTag(persistName) + ".x", r.x);
      Persist.set(persistName + ".x", Integer.toString(r.x));
      Persist.set(persistName + ".y", Integer.toString(r.y));
      Persist.set(persistName + ".w", Integer.toString(r.width));
      Persist.set(persistName + ".h", Integer.toString(r.height));
   }

   // Data ////////////////////////////////////////////////////////////////////
   private String m_PersistName = null;
}
