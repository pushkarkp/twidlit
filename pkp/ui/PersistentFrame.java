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
public class PersistentFrame extends JFrame {

   ////////////////////////////////////////////////////////////////////////////
   @Override
   public void setTitle(String title) {
      super.setTitle(title);
      m_PersistName = title;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override
   public void setVisible(boolean set) {
      if (set) {
         String persistName = getPersistName();
//System.out.printf("%s %d%n", persistName, Persist.getInt(m_PersistName + ".x", 0));
         Rectangle r = new Rectangle(Persist.getInt(m_PersistName + ".x", 0),
                                     Persist.getInt(m_PersistName + ".y", 0),
                                     Persist.getInt(m_PersistName + ".w", 500),
                                     Persist.getInt(m_PersistName + ".h", 300));
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
         m_PersistName = getTitle();
      }
      return m_PersistName;
   }
   
   // Data ////////////////////////////////////////////////////////////////////
   private String m_PersistName = null;
}
