/**
 * Copyright 2015 Pushkar Piggott
 *
 * ScalePanel.java
 */

package pkp.ui;

import java.awt.*;
import javax.swing.JPanel;
import javax.swing.BoxLayout;

////////////////////////////////////////////////////////////////////////////////
public class ScalePanel extends JPanel {

   /////////////////////////////////////////////////////////////////////////////
   public static ScalePanel createVertical(int max, Color c0, Color c1) {
      return new ScalePanel(max, c0, c1, true);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   public static ScalePanel createHorizontal(int max, Color c0, Color c1) {
      return new ScalePanel(max, c0, c1, false);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   public void setMaximum(int max) {
      m_Max = max * 2;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   @Override
   protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (m_Vertical) {
         double step = (double)getHeight() / m_Max;
         double at = getHeight() - step;
         int size = (int)step + 1;
         for (int i = 0; i < m_Max; ++i) {
            g.setColor(m_Color[i & 1]);
            g.fillRect(0, (int)at, getWidth(), size);
//System.out.printf("step %g at %g size %d%n", step, at, size);
            at -= step;            
         }
      } else {
         double at = 0.0;
         double step = (double)getWidth() / m_Max;
         int size = (int)step + 1;
         for (int i = 0; i < m_Max; ++i) {
            g.setColor(m_Color[i & 1]);
            g.fillRect((int)at, 0, size, getHeight());
            at += step;            
         }
      }
   }

   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private ScalePanel(int max, Color c0, Color c1, boolean vert) {
      super();
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      setMaximum(max);
      m_Color = new Color[]{c0, c1};
      m_Vertical = vert;
      setMaximumSize(m_Vertical
         ? new Dimension(2, Short.MAX_VALUE)
         : new Dimension(Short.MAX_VALUE, 2));
   }

   // Data /////////////////////////////////////////////////////////////////////
   private int m_Max;
   private Color[] m_Color;
   private boolean m_Vertical;
}   
