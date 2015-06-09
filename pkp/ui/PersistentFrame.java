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
   public void setPersistName(String name) {
      m_PersistName = name;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override
   public void setVisible(boolean set) {
      if (set) {
         String persistName = getPersistName();
//System.out.println(persistName);         
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
      Persist.set(persistName + ".x", Integer.toString(r.x));
      Persist.set(persistName + ".y", Integer.toString(r.y));
      Persist.set(persistName + ".w", Integer.toString(r.width));
      Persist.set(persistName + ".h", Integer.toString(r.height));
      super.dispose();
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private String getPersistName() {
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
