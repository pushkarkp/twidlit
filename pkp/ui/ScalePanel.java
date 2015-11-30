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
   public static ScalePanel createVertical(int size, int scale, Color c0, Color c1) {
      return new ScalePanel(size, scale, c0, c1, true);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   public static ScalePanel createHorizontal(int size, int scale, Color c0, Color c1) {
      return new ScalePanel(size, scale, c0, c1, false);
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
         double step = getHeight() * (double)m_Scale / m_Max;
         int barSize = (int)(step + 0.5);
         // start at the bottom
         double at = getHeight() - barSize;
         // add 1 for any remainder
         int end = (int)((double)m_Max / m_Scale) + 1;
//System.out.printf("max %d scale %d getHeight() %d at %g step %g at %g barSize %d end %d%n", m_Max, m_Scale, getHeight(), at, step, at, barSize, end);
         for (int i = 0; i < end; ++i) {
            g.setColor(m_Color[i & 1]);
            g.fillRect(0, (int)(at + 0.5), getWidth(), barSize);
            at -= step;            
         }
      } else {
         double step = getWidth() * (double)m_Scale / m_Max;
         int barSize = (int)(step + 0.5);
         double at = 0.0;
         int end = (int)((double)m_Max / m_Scale) + 1;
         for (int i = 0; i < end; ++i) {
            g.setColor(m_Color[i & 1]);
            g.fillRect((int)(at + 0.5), 0, barSize, getHeight());
            at += step;            
         }
      }
   }

   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private ScalePanel(int size, int scale, Color c0, Color c1, boolean vert) {
      super();
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      setMaximum(size);
      m_Scale = scale;
      m_Color = new Color[]{c0, c1};
      m_Vertical = vert;
      setMaximumSize(m_Vertical
         ? new Dimension(2, Short.MAX_VALUE)
         : new Dimension(Short.MAX_VALUE, 2));
   }

   // Data /////////////////////////////////////////////////////////////////////
   private int m_Max;
   private int m_Scale;
   private Color[] m_Color;
   private boolean m_Vertical;
}   
