/**
 * Copyright 2015 Pushkar Piggott
 *
 * Progress.java
 */

package pkp.twidlit;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import pkp.twiddle.Twiddle;
import pkp.times.ChordTimes;
import pkp.ui.ScalePanel;
import pkp.util.Pref;

////////////////////////////////////////////////////////////////////////////////
class ProgressPanel extends JPanel {

   /////////////////////////////////////////////////////////////////////////////
   ProgressPanel(int vertMax, int horzMax, Color ht, Color fg, Color bg) {
      final int V_HEIGHT = 271;
      final int V_WIDTH = 46;
      final int BAR_WIDTH = 14;
      final int SCALE_WIDTH = 2;
      final int H_WIDTH = 20;
      final Dimension V_BAR_SIZE = new Dimension(BAR_WIDTH, V_HEIGHT);
      final Dimension V_SCALE_SIZE = new Dimension(SCALE_WIDTH, V_HEIGHT);
      final Dimension V_PANEL_SIZE = new Dimension(V_WIDTH, V_HEIGHT);
      final Dimension H_BAR_SIZE = new Dimension(H_WIDTH, BAR_WIDTH);
      final Dimension H_SCALE_SIZE = new Dimension(H_WIDTH, SCALE_WIDTH);
      final Dimension H_PANEL_SIZE = new Dimension(H_WIDTH, SCALE_WIDTH + BAR_WIDTH);

      m_COLOR_HIGHLIGHT_BAR = ht;
      m_COLOR_BAR = fg;
      m_COLOR_BACKGROUND = bg;

      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      m_VerticalPanel = this;//new JPanel();
      //add(m_VerticalPanel);
      m_VerticalPanel.setLayout(new BoxLayout(m_VerticalPanel, BoxLayout.X_AXIS));
      m_VerticalPanel.setBackground(m_COLOR_BACKGROUND);
      m_VerticalPanel.setPreferredSize(V_PANEL_SIZE);
      m_VerticalPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

      m_ProgressBar = createProgressBar(JProgressBar.VERTICAL);
      m_ProgressBar.setPreferredSize(V_BAR_SIZE);
      m_MeanProgressBar = createProgressBar(JProgressBar.VERTICAL);
      m_MeanProgressBar.setPreferredSize(V_BAR_SIZE);
      m_MeanMeanProgressBar = createProgressBar(JProgressBar.VERTICAL);
      m_MeanMeanProgressBar.setPreferredSize(V_BAR_SIZE);
      
      m_ScalePanel1 = ScalePanel.createVertical(vertMax, 1000, m_COLOR_BACKGROUND, m_COLOR_BAR);
      m_ScalePanel1.setPreferredSize(V_SCALE_SIZE);
      m_ScalePanel2 = ScalePanel.createVertical(vertMax, 1000, m_COLOR_BACKGROUND, m_COLOR_BAR);
      m_ScalePanel2.setPreferredSize(V_SCALE_SIZE);
      
      m_ScalePanel3 = ScalePanel.createHorizontal(horzMax, 1, m_COLOR_BACKGROUND, m_COLOR_BAR);
      m_ScalePanel3.setPreferredSize(H_SCALE_SIZE);
      setAlignmentX(Component.CENTER_ALIGNMENT);
      m_ScalePanel3.setAlignmentX(Component.CENTER_ALIGNMENT);
      m_ScalePanel3.setLayout(new BoxLayout(m_ScalePanel3, BoxLayout.Y_AXIS));
      //add(m_ScalePanel3);

      JPanel horizontalPanel = new JPanel();
      add(horizontalPanel);
      horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.Y_AXIS));
      horizontalPanel.setBackground(m_COLOR_BACKGROUND);
      horizontalPanel.setPreferredSize(H_PANEL_SIZE);
      setAlignmentX(Component.CENTER_ALIGNMENT);
      horizontalPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

      m_SamplesProgressBar = createProgressBar(JProgressBar.HORIZONTAL);
      m_SamplesProgressBar.setPreferredSize(H_BAR_SIZE);
      //horizontalPanel.add(m_SamplesProgressBar);

      m_RightHand = true;
      setRightHand(false);
      
      m_AutoScaleStep = Pref.getInt("chord.wait.autoscale.step", 8);
      m_AutoScalePercent = Pref.getInt("chord.wait.autoscale.mean.percent", 50);
      setAutoScale(false);
   }

   /////////////////////////////////////////////////////////////////////////////
   boolean isAutoScale() {
      return m_AutoScaleCount > 0;
   }

   /////////////////////////////////////////////////////////////////////////////
   void setAutoScale(boolean set) {
      m_AutoScaleCount = set ? 1 : 0;
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
      m_VerticalPanel.removeAll();
      if (right) {
         m_VerticalPanel.add(m_MeanMeanProgressBar);
         m_VerticalPanel.add(m_ScalePanel1);
         m_VerticalPanel.add(m_MeanProgressBar);
         m_VerticalPanel.add(m_ScalePanel2);
         m_VerticalPanel.add(m_ProgressBar);
      } else {
         m_VerticalPanel.add(m_ProgressBar);
         m_VerticalPanel.add(m_ScalePanel1);
         m_VerticalPanel.add(m_MeanProgressBar);
         m_VerticalPanel.add(m_ScalePanel2);
         m_VerticalPanel.add(m_MeanMeanProgressBar);
      }
      if (isVisible()) {
         super.setVisible(true);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   public void setMaximum(int vert, int horz) {
      m_ProgressBar.setMaximum(vert);
      m_ScalePanel1.setMaximum(vert);
      m_MeanProgressBar.setMaximum(vert);
      m_ScalePanel2.setMaximum(vert);
      m_MeanMeanProgressBar.setMaximum(vert);
      m_ScalePanel3.setMaximum(horz);
      m_SamplesProgressBar.setMaximum(horz);
   }

   /////////////////////////////////////////////////////////////////////////////
   int getMaximum() {
      return m_ProgressBar.getMaximum();
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
   void setMean(int mean) {
      m_MeanProgressBar.setValue(mean);
   }

   /////////////////////////////////////////////////////////////////////////////
   void setMeanMean(int mean) {
      m_MeanMeanProgressBar.setValue(mean);
   }

   /////////////////////////////////////////////////////////////////////////////
   void setMeans(Twiddle tw, ChordTimes ct) {
      int ch = tw.getChord().toInt();
      int tk = tw.getThumbKeys().toInt();
      autoScale(tk, ct.getMeanCount(tk), ct.getMeanMean(tk));
      setMeanMean(ct.getMeanMean(tk));
      setMean(ct.getMean(ch, tk));
   }

   /////////////////////////////////////////////////////////////////////////////
   void setSamples(int samples) {
      m_SamplesProgressBar.setValue(samples);
   }

   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private JProgressBar createProgressBar(int orientation) {
      JProgressBar pb = new JProgressBar(orientation, 0, 0);
      pb.setForeground(m_COLOR_BAR);
      pb.setBackground(m_COLOR_BACKGROUND);
      pb.setBorderPainted(false);
      return pb;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   void autoScale(int tk, int meanCount, int meanMean) {
      if (m_AutoScaleCount > 0 && meanCount > 16) {
         ++m_AutoScaleCount;
         if (m_AutoScaleCount % m_AutoScaleStep == 2) {
            int max = round(getMaximum(), 500);
            int newMaxMax = round((meanMean + sm_AUTOSCALE_HYSTERESIS) * 100 / m_AutoScalePercent, 500);
            int newMaxMin = round((meanMean - sm_AUTOSCALE_HYSTERESIS) * 100 / m_AutoScalePercent, 500);
//System.out.printf("autoScale(%d, %d, %d) max %d newMaxMin %d newMaxMax %d%n", tk, meanCount, meanMean, max, newMaxMin, newMaxMax);
            if (newMaxMax < max || newMaxMin > max) {
               setMaximum(round(meanMean * 100 / m_AutoScalePercent, 500), 8);
            }
         }
      }
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

   private JPanel m_VerticalPanel;
   private JPanel m_HorizontalPanel;
   private ScalePanel m_ScalePanel1;
   private ScalePanel m_ScalePanel2;
   private ScalePanel m_ScalePanel3;
   private JProgressBar m_ProgressBar;
   private JProgressBar m_MeanProgressBar;
   private JProgressBar m_MeanMeanProgressBar;
   private JProgressBar m_SamplesProgressBar;
   private boolean m_RightHand;
   private int m_AutoScaleCount;
   private int m_AutoScaleStep;
   private int m_AutoScalePercent;
}
