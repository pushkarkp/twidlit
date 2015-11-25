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
//System.out.printf("%s %d%n", persistName, Persist.getInt(persistName + ".x", 0));
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
//System.out.printf("%s %d%n", persistName + ".x", r.x);
      Persist.set(persistName + ".x", Integer.toString(r.x));
      Persist.set(persistName + ".y", Integer.toString(r.y));
      Persist.set(persistName + ".w", Integer.toString(r.width));
      Persist.set(persistName + ".h", Integer.toString(r.height));
      super.dispose();
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setPersistName(String name) {
      m_PersistName = name;
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   // PersistName is converted to tag in PersistentPrperties.
   // If value is not found, try deleting ./Persistent.properties
   private String getPersistName() {
//System.out.printf("getPersistName() %s%n", m_PersistName);
      if (m_PersistName == null || "".equals(m_PersistName)) {
         if ("".equals(getTitle())) {
            return getClass().getSimpleName();
         }
         return getTitle();
      }
      return m_PersistName;
   }
   
   // Data ////////////////////////////////////////////////////////////////////
   private String m_PersistName = null;
}
