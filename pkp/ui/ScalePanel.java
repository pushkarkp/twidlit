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
   public ScalePanel(boolean vert, int size, int width, int scale, Color c0, Color c1) {
      super();
      if (vert) { 
         setPreferredSize(new Dimension(width, Short.MAX_VALUE));
         setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      } else {
         setPreferredSize(new Dimension(Short.MAX_VALUE, width));
         setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
      }
      setMaximum(size);
      m_Scale = scale;
      m_Color = new Color[]{c0, c1};
      m_Vertical = vert;
      setMaximumSize(m_Vertical
         ? new Dimension(width, Short.MAX_VALUE)
         : new Dimension(Short.MAX_VALUE, width));
//System.out.printf("vert %b size %d scale %d%n", vert, size, m_Scale);
   }

   /////////////////////////////////////////////////////////////////////////////
   public void setMaximum(int max) {
      m_Max = max * 2;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   public int getMaximum() {
      return m_Max / 2;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   @Override
   protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (m_Vertical) {
         double step = (getHeight() - 2) * (double)m_Scale / m_Max;
         int barSize = (int)(step + 0.5);
         // start at the bottom
         double at = (getHeight() - 1) - barSize;
         int end = (int)(0.5 + (double)m_Max / m_Scale);
//System.out.printf("max %d scale %d getHeight() %d at %g step %g at %g barSize %d end %d%n", m_Max, m_Scale, getHeight(), at, step, at, barSize, end);
         // skip the end pixels to match progress bar
         g.setColor(m_Color[0]);
         g.fillRect(0, 0, getWidth(), 1);
         g.fillRect(0, getHeight() - 1, getWidth(), getHeight());
         for (int i = 0; i < end; ++i) {
            g.setColor(m_Color[i & 1]);
            g.fillRect(0, (int)(at + 0.5), getWidth(), barSize);
            at -= step;            
         }
      } else {
         double step = (getWidth() - 2) * (double)m_Scale / m_Max;
         int barSize = (int)(step + 0.5);
         double at = 1.0;
         int end = (int)(0.5 + (double)m_Max / m_Scale);
         g.setColor(m_Color[0]);
         g.fillRect(0, 0, 1, getHeight());
         g.fillRect(getWidth() - 1, 0, getWidth(), getHeight());
         for (int i = 0; i < end; ++i) {
            g.setColor(m_Color[i & 1]);
            g.fillRect((int)(at + 0.5), 0, barSize, getHeight());
            at += step;            
         }
      }
   }

   // Data /////////////////////////////////////////////////////////////////////
   private int m_Max;
   private int m_Scale;
   private Color[] m_Color;
   private boolean m_Vertical;
}   
