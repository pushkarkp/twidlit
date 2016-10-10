/**
 * Copyright 2015 Pushkar Piggott
 *
 * ProgressPanel.java
 */

package pkp.twidlit;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import pkp.ui.ScalePanel;
import pkp.util.Pref;

////////////////////////////////////////////////////////////////////////////////
class ProgressPanel extends JPanel {

   /////////////////////////////////////////////////////////////////////////////
   ProgressPanel(boolean vertical, boolean right, int samples, Color ht, Color fg, Color bg) {
      m_COLOR_HIGHLIGHT_BAR = ht;
      m_COLOR_BAR = fg;
      m_COLOR_BACKGROUND = bg;
      setBackground(m_COLOR_BACKGROUND);

      m_Vertical = vertical;
      int vh = m_Vertical ? 0 : 1;

      final int[] LAYOUT = new int[]{BoxLayout.Y_AXIS, BoxLayout.X_AXIS};
      setLayout(new BoxLayout(this, LAYOUT[vh]));
      final int[] SIZE = new int[]{53, 284};
      setPreferredSize(new Dimension(SIZE[vh], SIZE[1 - vh]));
      final int[][] BORDER = new int[][]{{6, 2, 5, 3}, {3, 5, 2, 6}};
      setBorder(BorderFactory.createMatteBorder(BORDER[vh][0], BORDER[vh][1], BORDER[vh][2], BORDER[vh][3], m_COLOR_BACKGROUND));

      final int[] VH = new int[]{JProgressBar.VERTICAL, JProgressBar.HORIZONTAL};
      final int BAR_WIDTH = 14;
      final int PANEL_WIDTH = SIZE[0] - 6;
      final int[] SAMPLES_BAR_SIZE = new int[]{BAR_WIDTH, PANEL_WIDTH};
      m_SamplesProgressBar = createProgressBar(VH[1 - vh], new Dimension(SAMPLES_BAR_SIZE[1 - vh], SAMPLES_BAR_SIZE[vh]));
      m_ScalePanel3 = new ScalePanel(!m_Vertical, PANEL_WIDTH, 1, 1, m_COLOR_BACKGROUND, m_COLOR_BAR);
      m_ScalePanel3.setMaximum(Math.min(8, samples));

      final int BAR_LENGTH = SIZE[1] - BAR_WIDTH - 40;
      final int[] PANEL_SIZE = new int[]{45, BAR_LENGTH};

      m_MainPanel = new JPanel();
      m_MainPanel.setBackground(m_COLOR_BACKGROUND);
      m_MainPanel.setLayout(new BoxLayout(m_MainPanel, LAYOUT[1 - vh]));
      m_MainPanel.setPreferredSize(new Dimension(PANEL_SIZE[vh], PANEL_SIZE[1 - vh]));

      m_ScalePanel1 = createScalePanel(m_Vertical, BAR_LENGTH);
      m_ScalePanel2 = createScalePanel(m_Vertical, BAR_LENGTH);
      
      final int[] BAR = new int[]{BAR_WIDTH, BAR_LENGTH};
      final Dimension BAR_SIZE = new Dimension(BAR[vh], BAR[1 - vh]);
      m_ProgressBar = createProgressBar(VH[vh], BAR_SIZE);
      m_MeanProgressBar = createProgressBar(VH[vh], BAR_SIZE);
      m_MeanMeanProgressBar = createProgressBar(VH[vh], BAR_SIZE);

      m_AutoScalePercent = Pref.getInt("#.chord.wait.autoscale.mean.percent", 50);

      m_RightHand = !right;
      setRightHand(right);
   }

   /////////////////////////////////////////////////////////////////////////////
   ProgressPanel(boolean vertical, ProgressPanel pp) {
      this(vertical, 
           pp.m_RightHand,
           pp.m_ScalePanel3.getMaximum(),
           pp.m_COLOR_HIGHLIGHT_BAR,
           pp.m_COLOR_BAR,
           pp.m_COLOR_BACKGROUND);
      setMaximum(pp.getMaximum()); 
      m_MeanProgressBar.setValue(pp.m_MeanProgressBar.getValue());
      m_MeanMeanProgressBar.setValue(pp.m_MeanMeanProgressBar.getValue());
      setMaximumSamples(pp.m_SamplesProgressBar.getMaximum());
      m_SamplesProgressBar.setValue(pp.m_SamplesProgressBar.getValue());
   }

   /////////////////////////////////////////////////////////////////////////////
   boolean isRightHand() {
      return m_RightHand;
   }

   /////////////////////////////////////////////////////////////////////////////
   void setRightHand(boolean right) {
      if (m_RightHand == right) {
         return;
      }
      m_RightHand = right;
      m_MainPanel.removeAll();
      if (right == m_Vertical) {
         m_MainPanel.add(m_MeanMeanProgressBar);
         m_MainPanel.add(m_ScalePanel1);
         m_MainPanel.add(m_MeanProgressBar);
         m_MainPanel.add(m_ScalePanel2);
         m_MainPanel.add(m_ProgressBar);
      } else {
         m_MainPanel.add(m_ProgressBar);
         m_MainPanel.add(m_ScalePanel1);
         m_MainPanel.add(m_MeanProgressBar);
         m_MainPanel.add(m_ScalePanel2);
         m_MainPanel.add(m_MeanMeanProgressBar);
      }
      if (m_Vertical) {
         add(m_MainPanel);
         add(m_ScalePanel3);
         add(m_SamplesProgressBar);
      } else {
         add(m_SamplesProgressBar);
         add(m_ScalePanel3);
         add(m_MainPanel);
      }
      validate();
      if (isVisible()) {
         super.setVisible(true);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   void setMaximumSamples(int maxSamples) {
      m_SamplesProgressBar.setMaximum(maxSamples);
   }

   /////////////////////////////////////////////////////////////////////////////
   void setMaximum(int time) {
//System.out.printf("set max %d%n", time);
      m_ProgressBar.setMaximum(time);
      m_ScalePanel1.setMaximum(time);
      m_MeanProgressBar.setMaximum(time);
      m_ScalePanel2.setMaximum(time);
      m_MeanMeanProgressBar.setMaximum(time);
   }

   /////////////////////////////////////////////////////////////////////////////
   int getMaximum() {
      return m_ProgressBar.getMaximum();
   }

   /////////////////////////////////////////////////////////////////////////////
   int getSamplesMaximum() {
      return m_SamplesProgressBar.getMaximum();
   }

   /////////////////////////////////////////////////////////////////////////////
   void autoScale(int newMax) {
      int oldMax = round(getMaximum(), 500);
      int newMaxMax = round((newMax + sm_AUTOSCALE_HYSTERESIS) * 100 / m_AutoScalePercent, 500);
      int newMaxMin = round((newMax - sm_AUTOSCALE_HYSTERESIS) * 100 / m_AutoScalePercent, 500);
//System.out.printf("autoScale(%d) max %d newMaxMin %d newMaxMax %d%n", newMax, max, newMaxMin, newMaxMax);
      if (newMaxMax < oldMax || newMaxMin > oldMax) {
         setMaximum(round(newMax * 100 / m_AutoScalePercent, 500));
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   void setHighlight() {
      m_ProgressBar.setForeground(m_COLOR_HIGHLIGHT_BAR);
   }

   /////////////////////////////////////////////////////////////////////////////
   void setLowlight() {
      m_ProgressBar.setForeground(m_COLOR_BAR);
   }

   /////////////////////////////////////////////////////////////////////////////
   void setProgress(int progress) {
      m_ProgressBar.setValue(progress);
   }

   /////////////////////////////////////////////////////////////////////////////
   void setMeans(int mean, int meanMean, int totalSamples) {
      m_MeanProgressBar.setValue(mean);
      m_MeanMeanProgressBar.setValue(meanMean);
      m_SamplesProgressBar.setValue(totalSamples);
   }

   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private JProgressBar createProgressBar(int orientation, Dimension size) {
      JProgressBar pb = new JProgressBar(orientation, 0, 0);
      pb.setForeground(m_COLOR_BAR);
      pb.setBackground(m_COLOR_BACKGROUND);
      pb.setBorderPainted(false);
      pb.setPreferredSize(size);
      return pb;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private ScalePanel createScalePanel(boolean vert, int size) {
      ScalePanel sp = new ScalePanel(vert, size, 2, 1000, m_COLOR_BACKGROUND, m_COLOR_BAR);
      if (!vert) {
         sp.setAlignmentX(Component.CENTER_ALIGNMENT);
      }
      return sp;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private static int round(int val, int mod) {
      val += mod / 2;
      return val - val % mod;
   }

   // Data /////////////////////////////////////////////////////////////////////
   private final Color m_COLOR_HIGHLIGHT_BAR;
   private final Color m_COLOR_BAR;
   private final Color m_COLOR_BACKGROUND;

   private static final int sm_THIN = 4;
   private static final int sm_AUTOSCALE_HYSTERESIS = 50;

   private boolean m_Vertical;
   private JPanel m_MainPanel;
   private ScalePanel m_ScalePanel1;
   private ScalePanel m_ScalePanel2;
   private ScalePanel m_ScalePanel3;
   private JProgressBar m_ProgressBar;
   private JProgressBar m_MeanProgressBar;
   private JProgressBar m_MeanMeanProgressBar;
   private JProgressBar m_SamplesProgressBar;
   private boolean m_RightHand;
   private int m_AutoScalePercent;
}
