/**
 * Copyright 2015 Pushkar Piggott
 *
 * TwiddlerWindow.java
 */

package pkp.twidlit;

import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import java.util.Random;
import pkp.twiddle.Chord;
import pkp.twiddle.ThumbKeys;
import pkp.twiddle.Twiddle;
import pkp.util.Pref;
import pkp.util.Persist;
import pkp.ui.PersistentFrame;

////////////////////////////////////////////////////////////////////////////////
class TwiddlerWindow extends PersistentFrame implements ActionListener/*, Lesson.Configurable*/ {

  /////////////////////////////////////////////////////////////////////////////
   TwiddlerWindow(JCheckBoxMenuItem menuItem, KeyListener keyListener) {
      setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      setIconImage(Pref.getIcon().getImage());
      setTitle("Twiddler");
      setFocusable(true);
      requestFocusInWindow();
      setFocusTraversalKeysEnabled(false);
      addKeyListener(keyListener);
      m_MenuItem = menuItem;

      m_COLOR_BACKGROUND = Pref.getColor("twiddler.background.color", Color.black);
      m_COLOR_KEY = Pref.getColor("twiddler.button.color", Color.lightGray);
      m_COLOR_LABEL = Pref.getColor("twiddler.label.color", Color.white);
      m_COLOR_KEY_HIGHLIGHT = Pref.getColor("twiddler.highlight.color", Color.red);

      m_ThumbPanel = createThumbPanel();
      m_ChordPanel = createChordPanel();
      m_TwiddlerPanel = createTwiddlerPanel(m_ThumbPanel, m_ChordPanel);
      m_ProgressBar = createProgressBar();
      getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
      pack();

      m_RightHand = true;
      setHand(false);

      m_MarkTimer = new Timer(1000, this);
      m_MarkTimer.setActionCommand(m_CLEAR_MARK_TEXT);
      m_MarkTimer.stop();
      m_HighlightTimer = new Timer(1000, this);
      m_HighlightTimer.setActionCommand(m_CLEAR_HIGHLIGHT_TEXT);
      m_HighlightTimer.stop();
      m_ProgressTimer = new Timer(Pref.getInt("twiddler.progress.step.msec", m_DEFAULT_STEP_MSEC), this);
      m_ProgressTimer.setActionCommand(m_PROGRESS_TEXT);
      setWaitFactor(1.0);
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override
   public void setVisible(boolean visible) {
      m_MenuItem.setState(visible);
      super.setVisible(visible);
   }

   /////////////////////////////////////////////////////////////////////////////
   void setHand(boolean right) {
//System.out.println("setHand " + (right?"right":"left"));
      if (m_RightHand == right) {
         return;
      }
      getContentPane().removeAll();
      m_RightHand = right;
      if (right) {
         getContentPane().add(m_ProgressBar);
         getContentPane().add(m_TwiddlerPanel);
      } else {
         getContentPane().add(m_TwiddlerPanel);
         getContentPane().add(m_ProgressBar);
      }
      if (isVisible()) {
         super.setVisible(true);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   public int getInitialWait() {
      return Pref.getInt("twiddler.highlight.msec", m_DEFAULT_HIGHLIGHT_MSEC)
           + Pref.getInt("twiddler.rehighlight.msec", m_DEFAULT_REHIGHLIGHT_MSEC);
   }

   /////////////////////////////////////////////////////////////////////////////
   public void setWaitFactor(double factor) {
      clearMark();
      m_MarkMsec = (int)(factor * Pref.getInt("twiddler.mark.msec", m_DEFAULT_MARK_MSEC));
      m_Mark = m_MarkMsec > 0;
      m_MarkTimer.setInitialDelay(m_MarkMsec);

      m_HighlightMsec = (int)(factor * Pref.getInt("twiddler.highlight.msec", m_DEFAULT_HIGHLIGHT_MSEC));
      m_Highlight = m_HighlightMsec > 0;
      m_HighlightTimer.setInitialDelay(m_HighlightMsec);
      m_HighlightTimer.restart();
      m_RehighlightMsec = (int)(factor * Pref.getInt("twiddler.rehighlight.msec", m_DEFAULT_REHIGHLIGHT_MSEC));
      rehighlightIf();

      m_ProgressBar.setMaximum(m_HighlightMsec + m_RehighlightMsec);
      zeroProgress();
   }

   /////////////////////////////////////////////////////////////////////////////
   boolean isHighlight() {
      return m_HighlightMsec > 0;
   }

   /////////////////////////////////////////////////////////////////////////////
   void setHighlight(boolean set) {
      m_Highlight = isHighlight() && set;
      if (!m_Highlight) {
         clearHighlight();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   boolean isMark() {
      return m_MarkMsec > 0;
   }

   /////////////////////////////////////////////////////////////////////////////
   void setMark(boolean set) {
      m_Mark = isMark() && set;
      if (!m_Mark) {
         clearMark();
         repaint();
     }
   }

   /////////////////////////////////////////////////////////////////////////////
   void show(Twiddle tw, Twiddle pressed) {
      // actually pressed may differ from displayed
      if (pressed != null) {
         m_Twiddle = pressed;
      }
      if (m_Mark && m_Twiddle != null) {
         markNow(m_Twiddle, MarkType.MATCH);
      }
      m_Twiddle = tw;
      rehighlightIf();
      zeroProgress();
   }

   /////////////////////////////////////////////////////////////////////////////
   void markMismatch(Twiddle tw) {
      if (m_Mark) {
         markNow(tw, MarkType.MISMATCH);
         repaint();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // ActionListener
   public void actionPerformed(ActionEvent e) {
      switch (e.getActionCommand()) {
      case m_HIGHLIGHT_TEXT:
         if (m_Highlight && m_Twiddle != null) {
            highlightNow();
            repaint();
         }
         break;
      case m_CLEAR_HIGHLIGHT_TEXT:
         timerClearHighlight();
         break;
     case m_CLEAR_MARK_TEXT:
         clearMark();
         repaint();
         break;
      case m_PROGRESS_TEXT: 
         int elapsed = (int)(System.currentTimeMillis() - m_ProgressStart);
         if (elapsed > m_ProgressBar.getMaximum()) {
            m_ProgressTimer.stop();
            m_ProgressBar.setForeground(m_COLOR_KEY_HIGHLIGHT);
         }
         m_ProgressBar.setValue(elapsed);
         break;
      }
   }

   // Private //////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   enum MarkType {
      NONE("None", Color.black),
      MATCH("Match", Pref.getColor("twiddler.mark.match.color", Color.yellow)),
      MISMATCH("Mismatch", Pref.getColor("twiddler.mark.mismatch.color", Color.black));

      boolean isMark() {
         return ordinal() != 0;
      }

      public String toString() {
         return m_Name + " " + m_Color;
      }

      Color getColor() {
         return m_Color;
      }

      final String m_Name;

      private MarkType(String name, Color color) {
         m_Name = name;
         m_Color = color;
      }

      private Color m_Color;
   }

   /////////////////////////////////////////////////////////////////////////////
   private JPanel createThumbPanel() {
      JPanel thp = new JPanel(new GridLayout(2, 2));
      thp.setBackground(m_COLOR_BACKGROUND);
      Dimension thumbKeySize = new Dimension(30, 20);
      for (int i = 0; i < 4; ++i) {
         KeyPanel p = new KeyPanel();
         thp.add(p);
         p.setBackground(m_COLOR_KEY);
         p.setPreferredSize(thumbKeySize);
         int left = 6;
         int right = 6;
         String text = "";
         if (i == 0) {
            text = "Num";
            right = sm_FAT;
         } else if (i == 1) {
            text = "Shift";
            left = sm_FAT;
         } else if (i == 2) {
            text = "Alt";
            left = sm_FAT;
         } else if (i == 3) {
            text = "Ctrl";
            right = sm_FAT;
         }
         JLabel label = new JLabel(text);
         label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
         label.setForeground(m_COLOR_LABEL);
         p.add(label);
         p.setBorder(BorderFactory.createMatteBorder(sm_THIN, left, sm_THIN, right, m_COLOR_BACKGROUND));
      }
      return thp;
   }

   /////////////////////////////////////////////////////////////////////////////
   private JPanel createChordPanel() {
      JPanel chp = new JPanel(new GridLayout(4, 3));
      chp.setBackground(m_COLOR_BACKGROUND);
      Dimension size = new Dimension(20, 60);
      for (int i = 0; i < 12; ++i) {
         KeyPanel p = new KeyPanel();
         p.setPreferredSize(size);
         p.setBackground(m_COLOR_KEY);
         p.setBorder(BorderFactory.createMatteBorder(sm_THIN, sm_THIN, sm_THIN, sm_THIN, m_COLOR_BACKGROUND));
         chp.add(p);
      }
      return chp;
   }

   /////////////////////////////////////////////////////////////////////////////
   private JPanel createTwiddlerPanel(JPanel thumbPanel, JPanel chordPanel) {
      JPanel twp = new JPanel();
      twp.setLayout(new BoxLayout(twp, BoxLayout.Y_AXIS));
      twp.setBackground(m_COLOR_BACKGROUND);
      twp.add(Box.createRigidArea(new Dimension(0, 8)));
      twp.add(thumbPanel);
      twp.add(chordPanel);
      return twp;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private JProgressBar createProgressBar() {
      JProgressBar pb = new JProgressBar(JProgressBar.VERTICAL, 0, 0);
      pb.setBackground(m_COLOR_BACKGROUND);
      pb.setBorderPainted(false);
      return pb;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private void zeroProgress() {
      m_ProgressStart = System.currentTimeMillis();
      m_ProgressBar.setValue(0);
      m_ProgressBar.setForeground(m_COLOR_KEY);
      repaint();
      if (m_Twiddle != null) {
         m_ProgressTimer.start();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private void setHighlighter(String action, int delay) {
      m_HighlightTimer.stop();
      m_HighlightTimer.setActionCommand(action);
      m_HighlightTimer.setInitialDelay(delay);
      m_HighlightTimer.restart();
   }

   /////////////////////////////////////////////////////////////////////////////
   private void rehighlightIf() {
      if (m_Highlight && m_Twiddle != null) {
         clearHighlight();
         highlightNow();
      }
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private void highlightNow() {
      setHighlighter(m_CLEAR_HIGHLIGHT_TEXT, m_HighlightMsec);
      ThumbKeys thumbs = m_Twiddle.getThumbKeys();
      if (thumbs.isNum()) {
         m_ThumbPanel.getComponent(0).setBackground(m_COLOR_KEY_HIGHLIGHT);
      }
      if (thumbs.isShift()) {
         m_ThumbPanel.getComponent(1).setBackground(m_COLOR_KEY_HIGHLIGHT);
      }
      if (thumbs.isAlt()) {
         m_ThumbPanel.getComponent(2).setBackground(m_COLOR_KEY_HIGHLIGHT);
      }
      if (thumbs.isCtrl()) {
         m_ThumbPanel.getComponent(3).setBackground(m_COLOR_KEY_HIGHLIGHT);
      }
      for (int i = 0; i < Chord.sm_ROWS; ++i) {
         int button = m_Twiddle.getChord().getRowKey(i);
         if (button != 0) {
            m_ChordPanel.getComponent(i * Chord.sm_COLUMNS + button - 1).setBackground(m_COLOR_KEY_HIGHLIGHT);
         }
      }
   }
   
   /////////////////////////////////////////////////////////////////////////////
   void markNow(Twiddle tw, MarkType type) {
      clearMark();
      ThumbKeys thumbs = tw.getThumbKeys();
      if (thumbs.isNum()) {
         ((KeyPanel)m_ThumbPanel.getComponent(0)).setMark(type);
      }
      if (thumbs.isShift()) {
         ((KeyPanel)m_ThumbPanel.getComponent(1)).setMark(type);
      }
      if (thumbs.isAlt()) {
         ((KeyPanel)m_ThumbPanel.getComponent(2)).setMark(type);
      }
      if (thumbs.isCtrl()) {
         ((KeyPanel)m_ThumbPanel.getComponent(3)).setMark(type);
      }
      Chord ch = tw.getChord();
      for (int i = 0; i < Chord.sm_ROWS; ++i) {
         int button = ch.getRowKey(i);
         if (button != 0) {
            ((KeyPanel)m_ChordPanel.getComponent(i * Chord.sm_COLUMNS + button - 1)).setMark(type);
         }
      }
      repaint();
      m_MarkTimer.restart();
   }

   /////////////////////////////////////////////////////////////////////////////
   private void timerClearHighlight() {
      setHighlighter(m_HIGHLIGHT_TEXT, m_RehighlightMsec);
      clearHighlight();
      repaint();
   }

   /////////////////////////////////////////////////////////////////////////////
   private void clearHighlight() {
      for (int i = 0; i < 4; ++i) {
         m_ThumbPanel.getComponent(i).setBackground(m_COLOR_KEY);
      }
      for (int i = 0; i < 12; ++i) {
         m_ChordPanel.getComponent(i).setBackground(m_COLOR_KEY);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private void clearMark() {
      m_MarkTimer.stop();
      for (int i = 0; i < 4; ++i) {
         ((KeyPanel)m_ThumbPanel.getComponent(i)).setMark(MarkType.NONE);
      }
      for (int i = 0; i < 12; ++i) {
         ((KeyPanel)m_ChordPanel.getComponent(i)).setMark(MarkType.NONE);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private void addSeparator(int height) {
      JPanel separator = new JPanel();
      separator.setBackground(Pref.getColor("ColorTwiddlerBackground"));
      separator.setPreferredSize(new Dimension(60, height));
      getContentPane().add(separator);
   }

   ////////////////////////////////////////////////////////////////////////////////
   private static class KeyPanel extends JPanel {

      /////////////////////////////////////////////////////////////////////////////
      KeyPanel() {
         m_Mark = MarkType.NONE;
      }

      /////////////////////////////////////////////////////////////////////////////
      void setMark(MarkType mark) {
         m_Mark = mark;
      }

      /////////////////////////////////////////////////////////////////////////////
      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         paintMark(g);
      }

      /////////////////////////////////////////////////////////////////////////////
      private void paintMark(Graphics g) {
         if (m_Mark.isMark()) {
            final int proportion = 3;
            int size = Math.min(getWidth(), getHeight()) / proportion;
            g.setColor(m_Mark.getColor());
            g.fillOval((getWidth() - size) / 2, (getHeight() - size) / 2, size, size);
         }
      }

      // Data /////////////////////////////////////////////////////////////////////
      private MarkType m_Mark = MarkType.NONE;
   }

   // Data /////////////////////////////////////////////////////////////////////
   private final String m_HIGHLIGHT_TEXT = "highlight";
   private final String m_CLEAR_HIGHLIGHT_TEXT = "clear.highlight";
   private final String m_PROGRESS_TEXT = "progress";
   private final String m_CLEAR_MARK_TEXT = "clear.mark";

   private final int m_DEFAULT_HIGHLIGHT_MSEC = 2000;
   private final int m_DEFAULT_REHIGHLIGHT_MSEC = 4000;
   private final int m_DEFAULT_MARK_MSEC = 2000;
   private final int m_DEFAULT_STEP_MSEC = 150;
   
   private final Color m_COLOR_BACKGROUND;
   private final Color m_COLOR_KEY;
   private final Color m_COLOR_LABEL;
   private final Color m_COLOR_KEY_HIGHLIGHT;

   private static final int sm_THIN = 4;
   private static final int sm_FAT = 16;

   private JCheckBoxMenuItem m_MenuItem;
   private JPanel m_ThumbPanel;
   private JPanel m_ChordPanel;
   private JPanel m_TwiddlerPanel;
   private JProgressBar m_ProgressBar;
   private Timer m_ProgressTimer;
   private long m_ProgressStart;
   private Button[] m_Thumb;
   private boolean m_Highlight;
   private int m_HighlightMsec;
   private int m_RehighlightMsec;
   private Timer m_HighlightTimer;
   private boolean m_Mark;
   private int m_MarkMsec;
   private Timer m_MarkTimer;
   private Twiddle m_Twiddle;
   private boolean m_RightHand;

   // Main /////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      Persist.init("TwidlitPersist.properties", ".", "pref");
      Pref.init("TwidlitPreferences.txt", Persist.get("pref.dir"), "pref");
      Pref.setIconPath("/data/icon.gif");
      JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem("Twidlit", true);
      TwiddlerWindow win = new TwiddlerWindow(menuItem, null);
      win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      win.setVisible(true);
      Random r = new Random();
      for (int i = 0; ; ++i) {
         Twiddle tw = new Twiddle(r.nextInt(4080) + 1);
         if ((i & 1) == 0) {
            win.show(tw, null);
         } else {
            win.markMismatch(tw);
         }
         try {
            Thread.sleep(2000);
         } catch (InterruptedException e) {}
      }
   }
}
