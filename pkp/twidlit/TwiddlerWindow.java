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
import javax.swing.Timer;
import java.util.Random;
import pkp.times.ChordTimes;
import pkp.twiddle.Chord;
import pkp.twiddle.ThumbKeys;
import pkp.twiddle.Twiddle;
import pkp.ui.PersistentFrame;
import pkp.util.Persist;
import pkp.util.Persistent;
import pkp.util.Pref;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
class TwiddlerWindow extends PersistentFrame implements ActionListener, Persistent/*, Lesson.Configurable*/ {

  /////////////////////////////////////////////////////////////////////////////
   TwiddlerWindow(boolean right, JCheckBoxMenuItem menuItem, KeyListener keyListener) {
      setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      setIconImage(Pref.getIcon().getImage());
      setTitle("Twiddler");
      setFocusable(true);
      requestFocusInWindow();
      setFocusTraversalKeysEnabled(false);
      addKeyListener(keyListener);
      m_MenuItem = menuItem;
      getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

      m_COLOR_BACKGROUND = Pref.getColor(sm_COLOR_BACKGROUND_PREF, Color.black);
      m_COLOR_BUTTON = Pref.getColor(sm_COLOR_BUTTON_PREF, Color.lightGray);
      m_COLOR_LABEL = Pref.getColor(sm_COLOR_LABEL_PREF, Color.white);
      m_COLOR_BUTTON_HIGHLIGHT = Pref.getColor(sm_COLOR_BUTTON_HIGHLIGHT_PREF, Color.red);

      m_ThumbPanel = createThumbPanel();
      m_ChordPanel = createChordPanel();
      m_TwiddlerPanel = createTwiddlerPanel(m_ThumbPanel, m_ChordPanel);
      m_ProgressPanel = new ProgressPanel(6, 8, m_COLOR_BUTTON_HIGHLIGHT, m_COLOR_BUTTON, m_COLOR_BACKGROUND);

      m_RightHand = !right;
      setRightHand(right);
      pack();

      m_MarkTimer = new Timer(1000, this);
      m_MarkTimer.setActionCommand(sm_CLEAR_MARK_TEXT);
      m_MarkTimer.stop();
      m_HighlightTimer = new Timer(1000, this);
      m_HighlightTimer.setActionCommand(sm_HIGHLIGHT_TEXT);
      m_HighlightTimer.stop();
      m_ProgressTimer = new Timer(Pref.getInt(sm_PROGRESS_STEP_MSEC_PREF, sm_DEFAULT_STEP_MSEC), this);
      m_ProgressTimer.setActionCommand(sm_PROGRESS_TEXT);

      m_ProgressPercent = Pref.getInt(sm_PROGRESS_TIMED_PERCENT_PREF);
      // The progress bar interval must be small enough
      // so the timed interval fits in ChordTimes storage.
      int progressMax = ChordTimes.sm_MAX_MSEC * 100 / m_ProgressPercent;
      int progressMsec = Persist.getInt(sm_PERSIST_PROGRESS_MSEC, sm_DEFAULT_PROGRESS_MSEC);
      m_ProgressMsec = Math.max(0, Math.min(progressMsec, progressMax));
      m_DelayMsec = Math.max(0, Persist.getInt(sm_PERSIST_DELAY_MSEC, sm_DEFAULT_DELAY_MSEC));
      HighlightStage.DELAY.setMsec(m_DelayMsec);
      m_Delay = true;
      useDelay(false);
   }

   ///////////////////////////////////////////////////////////////////
   @Override // Persistent
   public void persist(String tag) {
//System.out.printf("%s %d%n", sm_PERSIST_DELAY_MSEC, m_DelayMsec);
//System.out.printf("%s %d%n", sm_PERSIST_PROGRESS_MSEC, m_ProgressMsec);
      Persist.set(sm_PERSIST_DELAY_MSEC, m_DelayMsec);
      Persist.set(sm_PERSIST_PROGRESS_MSEC, m_ProgressMsec);
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override
   public void setVisible(boolean visible) {
      m_MenuItem.setState(visible);
      super.setVisible(visible);
   }

   /////////////////////////////////////////////////////////////////////////////
   boolean isAutoScale() {
      return m_ProgressPanel.isAutoScale();
   }

   /////////////////////////////////////////////////////////////////////////////
   void setAutoScale(boolean set) {
      m_ProgressPanel.setAutoScale(set);
   }

   /////////////////////////////////////////////////////////////////////////////
   boolean isRightHand() {
      return m_RightHand;
   }

   /////////////////////////////////////////////////////////////////////////////
   void setRightHand(boolean right) {
//System.out.printf("setRightHand(%b)%n", right);
      if (m_RightHand == right) {
         return;
      }
      m_RightHand = right;
      getContentPane().removeAll();
      m_ProgressPanel.setRightHand(right);
      if (right) {
         getContentPane().add(m_ProgressPanel);
         getContentPane().add(m_TwiddlerPanel);
      } else {
         getContentPane().add(m_TwiddlerPanel);
         getContentPane().add(m_ProgressPanel);
      }
      if (isVisible()) {
         super.setVisible(true);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   int getProgressMax() {
      return m_ProgressPanel.getMaximum();
   }

   /////////////////////////////////////////////////////////////////////////////
   void useDelay(boolean set) {
//System.out.printf("useDelay(%b) m_Delay %b%n", set, m_Delay);
      if (m_Delay != set) {
         m_Delay = set;
         if (m_Delay) {
            HighlightStage.DELAY.setMsec(m_DelayMsec);
         } else {
            m_DelayMsec = m_HighlightStage.DELAY.getMsec(); 
            HighlightStage.DELAY.setMsec(0);
         }
         calculateTimes();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   int getDelay() {
      return m_HighlightStage.DELAY.getMsec();
   }

   /////////////////////////////////////////////////////////////////////////////
   void setDelay(int delay) {
//System.out.printf("setDelay(%d)%n", delay);
      m_DelayMsec = delay; 
      if (m_Delay) {
         HighlightStage.DELAY.setMsec(delay);
      }
      calculateTimes();
   }

   /////////////////////////////////////////////////////////////////////////////
   int getProgressBarMsec() {
      return m_ProgressMsec;
   }

   /////////////////////////////////////////////////////////////////////////////
   void setProgressBarMsec(int speed) {
//System.out.printf("setProgressBarMsec(%d)%n", speed);
      m_ProgressMsec = speed;
      calculateTimes();
   }

   /////////////////////////////////////////////////////////////////////////////
   // show new and actually pressed
   void show(Twiddle tw, Twiddle pressed, ChordTimes ct) {
      // actually pressed may differ from displayed
      if (m_Mark && pressed != null && m_MarkTimer.getInitialDelay() > 0) {
         markNow(pressed, MarkType.MATCH);
      }
      if (tw == null) {
         return;
      }
      m_Twiddle = tw;
      m_ProgressPanel.setMeans(tw, ct);
      start();
      // don't time the first twiddle
      if (pressed != null) {
         m_ProgressTimer.start();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   void markMismatch(Twiddle tw) {
      if (m_Mark && tw != null && m_MarkTimer.getInitialDelay() > 0) {
         markNow(tw, MarkType.MISMATCH);
         repaint();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // ActionListener
   public void actionPerformed(ActionEvent e) {
      switch (e.getActionCommand()) {
      case sm_HIGHLIGHT_TEXT:
         m_HighlightStage = m_HighlightStage.transition(this); 
         break;
     case sm_CLEAR_MARK_TEXT:
         clearMark();
         repaint();
         break;
      case sm_PROGRESS_TEXT: 
         int elapsed = (int)(System.currentTimeMillis() - m_ProgressStart);
         if (elapsed <= m_ProgressPanel.getMaximum()) {
            m_ProgressPanel.setProgress(elapsed);
//System.out.printf("+ %d%n", elapsed);
         } else 
         if (elapsed <= m_ProgressPanel.getMaximum() * m_ProgressPercent / 100) {
            m_ProgressPanel.setHighlight();
            m_ProgressPanel.setProgress(2 * m_ProgressPanel.getMaximum() - elapsed);
//System.out.printf("- %d%n", elapsed);
         } else {
            m_ProgressTimer.stop();
            m_ProgressPanel.setLowlight();
            m_ProgressPanel.setProgress(0);
//System.out.printf("| %d%n", elapsed);
         }
         break;
      }
   }

   // Private //////////////////////////////////////////////////////////////////

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

   /////////////////////////////////////////////////////////////////////////////
   private JPanel createThumbPanel() {
      JPanel thp = new JPanel(new GridLayout(2, 2));
      thp.setBackground(m_COLOR_BACKGROUND);
      Dimension thumbKeySize = new Dimension(30, 20);
      for (int i = 0; i < 4; ++i) {
         KeyPanel p = new KeyPanel();
         thp.add(p);
         p.setBackground(m_COLOR_BUTTON);
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
         p.setBackground(m_COLOR_BUTTON);
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
   private void setChordShowRatio(int ratio) {
      calculateTimes();
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private void calculateTimes() {
      clearMark();
      double markFactor = Pref.getInt(sm_MARK_PERCENT_PREF, sm_DEFAULT_SHOW_PERCENT) / 100.0;
      m_MarkTimer.setInitialDelay((int)(0.5 + m_ProgressMsec * markFactor));
      m_Mark = (m_MarkTimer.getInitialDelay() != 0);

      double factor = (m_Delay
                      ? Pref.getInt(sm_DELAYED_SHOW_PERCENT_PREF, sm_DEFAULT_SHOW_PERCENT)
                      : Pref.getInt(sm_SHOW_PERCENT_PREF, sm_DEFAULT_SHOW_PERCENT))
                    / 100.0;
      int rest = Math.max(0, m_ProgressMsec - m_HighlightStage.DELAY.getMsec());
      // can't show for less than 0 or more than rest of progress time
      int show = Math.max(0,
                          Math.min(rest,
                                   (int)(0.5 + m_ProgressMsec * factor)));
      HighlightStage.HIGHLIGHT.setMsec(show);
      HighlightStage.HIDE.setMsec(rest - show);
      m_Highlight = show > 0;
      m_ProgressPanel.setMaximum(m_ProgressMsec, 8);
//System.out.printf("delay %d m_ProgressMsec %d delay %d show %d hide %d factor %g%n", 
//         m_DelayMsec, m_ProgressMsec, 
//         HighlightStage.DELAY.getMsec(), 
//         HighlightStage.HIGHLIGHT.getMsec(), 
//         HighlightStage.HIDE.getMsec(), 
//         factor);
      start();
   }

   /////////////////////////////////////////////////////////////////////////////
   private enum HighlightStage {
      START(0) {
         @Override
         HighlightStage transition(TwiddlerWindow tw) {
            if (DELAY.getMsec() == 0) {
               return DELAY.transition(tw);
            }
            return super.transition(tw);
         }
      },
      DELAY(0) {
         @Override
         void start(TwiddlerWindow tw) {
            tw.setHighlightTimer(getMsec());
//System.out.printf("DELAY %d%n", getMsec());
         }
      },
      HIGHLIGHT(2000) {
         @Override
         void start(TwiddlerWindow tw) {
            tw.timerHighlight();
            tw.setHighlightTimer(getMsec());
//System.out.printf("HIGHLIGHT %d%n", getMsec());
         }
      },
      HIDE(4000) {
         @Override
         void start(TwiddlerWindow tw) {
            tw.timerClearHighlight();
            tw.setHighlightTimer(getMsec());
//System.out.printf("HIDE %d%n", getMsec());
         }
      };
      
      public int getMsec() { return m_Msec; }
      public void setMsec(int msec) { m_Msec = msec; }

      void start(TwiddlerWindow tw) {
//System.out.printf("START %d%n", getMsec());
      }

      HighlightStage transition(TwiddlerWindow tw) {
         HighlightStage next = (ordinal() == m_VALUES.length - 1)
                             ? START.transition(tw)
                             : m_VALUES[ordinal() + 1];
//System.out.printf("transition %s -> %s%n", name(), next.name());
         next.start(tw);
         return next;
      }

      private HighlightStage(int msec) { m_Msec = msec; }

      private static final HighlightStage[] m_VALUES = values(); 
      private int m_Msec;
   }

   /////////////////////////////////////////////////////////////////////////////
   private void start() {
      clearHighlight();
//System.out.printf("start m_Highlight %b m_HighlightStage.HIGHLIGHT.getMsec() %d%n", 
//                  m_Highlight, m_HighlightStage.HIGHLIGHT.getMsec());
      if (m_Highlight && m_HighlightStage.HIGHLIGHT.getMsec() > 0) {
         m_HighlightStage = HighlightStage.START.transition(this);
      } else {
         m_HighlightTimer.stop();
      }
      m_ProgressStart = System.currentTimeMillis();
      m_ProgressPanel.setProgress(0);
      m_ProgressPanel.setLowlight();
      repaint();
   }

   /////////////////////////////////////////////////////////////////////////////
   private boolean isHighlight() {
      return m_ProgressMsec > 0 
          && (m_Delay
              ? Pref.getInt(sm_DELAYED_SHOW_PERCENT_PREF, sm_DEFAULT_SHOW_PERCENT) > 0
              : Pref.getInt(sm_SHOW_PERCENT_PREF, sm_DEFAULT_SHOW_PERCENT) > 0);
   }

   /////////////////////////////////////////////////////////////////////////////
   private void setHighlightTimer(int delay) {
      m_HighlightTimer.stop();
      m_HighlightTimer.setInitialDelay(delay);
      m_HighlightTimer.restart();
   }

   /////////////////////////////////////////////////////////////////////////////
   private void timerHighlight() {
      highlight();
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private void timerClearHighlight() {
      clearHighlight();
      repaint();
   }

   /////////////////////////////////////////////////////////////////////////////
   private void highlight() {
      if (!m_Highlight || m_Twiddle == null) {
         return;
      }
      ThumbKeys thumbs = m_Twiddle.getThumbKeys();
      if (thumbs.isNum()) {
         m_ThumbPanel.getComponent(0).setBackground(m_COLOR_BUTTON_HIGHLIGHT);
      }
      if (thumbs.isShift()) {
         m_ThumbPanel.getComponent(1).setBackground(m_COLOR_BUTTON_HIGHLIGHT);
      }
      if (thumbs.isAlt()) {
         m_ThumbPanel.getComponent(2).setBackground(m_COLOR_BUTTON_HIGHLIGHT);
      }
      if (thumbs.isCtrl()) {
         m_ThumbPanel.getComponent(3).setBackground(m_COLOR_BUTTON_HIGHLIGHT);
      }
      for (int i = 0; i < Chord.sm_ROWS; ++i) {
         int button = m_Twiddle.getChord().getRowKey(i);
         if (button != 0) {
            m_ChordPanel.getComponent(i * Chord.sm_COLUMNS + button - 1).setBackground(m_COLOR_BUTTON_HIGHLIGHT);
         }
      }
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private void clearHighlight() {
      for (int i = 0; i < 4; ++i) {
         m_ThumbPanel.getComponent(i).setBackground(m_COLOR_BUTTON);
      }
      for (int i = 0; i < 12; ++i) {
         m_ChordPanel.getComponent(i).setBackground(m_COLOR_BUTTON);
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   enum MarkType {
      NONE("None", Color.white),
      MATCH("Match", Pref.getColor(sm_COLOR_MARK_MATCH_PREF, Color.yellow)),
      MISMATCH("Mismatch", Pref.getColor(sm_COLOR_MARK_MISMATCH_PREF, Color.black));

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
   private void markNow(Twiddle tw, MarkType type) {
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
      m_MarkTimer.restart();
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
      separator.setBackground(m_COLOR_BACKGROUND);
      separator.setPreferredSize(new Dimension(60, height));
      getContentPane().add(separator);
   }

   // Data /////////////////////////////////////////////////////////////////////

   private static final String sm_COLOR_BACKGROUND_PREF = "#.twiddler.background.color";
   private static final String sm_COLOR_BUTTON_PREF = "#.twiddler.button.color";
   private static final String sm_COLOR_LABEL_PREF = "#.twiddler.label.color";
   private static final String sm_COLOR_BUTTON_HIGHLIGHT_PREF = "#.twiddler.highlight.color";
   private static final String sm_COLOR_MARK_MATCH_PREF = "#.twiddler.mark.match.color";
   private static final String sm_COLOR_MARK_MISMATCH_PREF = "#.twiddler.mark.mismatch.color";
   private static final String sm_SHOW_PERCENT_PREF = "#.chord.wait.chord.show.percent";
   private static final String sm_DELAYED_SHOW_PERCENT_PREF = "#.chord.wait.delay.chord.show.percent";
   private static final String sm_MARK_PERCENT_PREF = "#.chord.wait.mark.percent";
   private static final String sm_PROGRESS_STEP_MSEC_PREF = "#.progress.step.msec";
   private static final String sm_PROGRESS_TIMED_PERCENT_PREF = "#.chord.wait.timed.percent";
   
   private static final String sm_PERSIST_DELAY_MSEC = "#.progress.delay.msec";
   private static final String sm_PERSIST_PROGRESS_MSEC = "#.progress.msec";

   private static final String sm_HIGHLIGHT_TEXT = "highlight";
   private static final String sm_PROGRESS_TEXT = "progress";
   private static final String sm_CLEAR_MARK_TEXT = "clear.mark";

   private static final int sm_DEFAULT_DELAY_MSEC = 2000;
   private static final int sm_DEFAULT_PROGRESS_MSEC = 6000;
   private static final int sm_DEFAULT_SHOW_PERCENT = 33;
   private static final int sm_DEFAULT_MARK_MSEC = 2000;
   private static final int sm_DEFAULT_STEP_MSEC = 150;
   
   private final Color m_COLOR_BACKGROUND;
   private final Color m_COLOR_BUTTON;
   private final Color m_COLOR_LABEL;
   private final Color m_COLOR_BUTTON_HIGHLIGHT;

   private static final int sm_THIN = 4;
   private static final int sm_FAT = 16;

   private JCheckBoxMenuItem m_MenuItem;
   private JPanel m_ThumbPanel;
   private JPanel m_ChordPanel;
   private JPanel m_TwiddlerPanel;
   private ProgressPanel m_ProgressPanel;
   private Timer m_ProgressTimer;
   private long m_ProgressStart;
   private int m_ProgressPercent;
   private Button[] m_Thumb;
   private int m_ProgressMsec;
   private boolean m_Highlight;
   private Timer m_HighlightTimer;
   private HighlightStage m_HighlightStage;
   private boolean m_Delay;
   private int m_DelayMsec;
   private boolean m_Mark;
   private Timer m_MarkTimer;
   private Twiddle m_Twiddle;
   private boolean m_RightHand;
}
