/**
 * Copyright 2015 Pushkar Piggott
 *
 * TwiddlerWindow.java
 */

package pkp.twidlit;

import java.awt.*;
import java.awt.event.MouseListener;
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
class TwiddlerWindow extends PersistentFrame implements ActionListener, Persistent {

   /////////////////////////////////////////////////////////////////////////////
   TwiddlerWindow(JCheckBoxMenuItem menuItemHide, boolean vert, boolean right, MouseListener mouseListener, KeyListener keyListener) {
      setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      setIconImage(Pref.getIcon().getImage());
      setPersistName("#.twiddler");
      setFocusable(true);
      requestFocusInWindow();
      setFocusTraversalKeysEnabled(false);
      addMouseListener(mouseListener);
      addKeyListener(keyListener);
      m_MenuItemHide = menuItemHide;

      m_COLOR_BACKGROUND = Pref.getColor(sm_COLOR_BACKGROUND_PREF, Color.black);
      m_COLOR_BUTTON = Pref.getColor(sm_COLOR_BUTTON_PREF, Color.lightGray);
      m_COLOR_LABEL = Pref.getColor(sm_COLOR_LABEL_PREF, Color.white);
      m_COLOR_BUTTON_HIGHLIGHT = Pref.getColor(sm_COLOR_BUTTON_HIGHLIGHT_PREF, Color.red);

      m_MarkTimer = new Timer(1000, this);
      m_MarkTimer.setActionCommand(sm_CLEAR_MARK_TEXT);
      m_MarkTimer.stop();
      m_HighlightTimer = new Timer(1000, this);
      m_HighlightTimer.setActionCommand(sm_HIGHLIGHT_TEXT);
      m_HighlightTimer.stop();
      m_ProgressTimer = new Timer(Pref.getInt(sm_PROGRESS_STEP_MSEC_PREF, sm_DEFAULT_STEP_MSEC), this);
      m_ProgressTimer.setActionCommand(sm_PROGRESS_TEXT);

      m_RightHand = right;
      m_Vh = vert ? Vh.VERT : Vh.HORZ;
      setPersistName(getPersistName() + '.' + m_Vh.toString());
      m_ProgressPanel = null;
      createPanels();
      buildHanded();

      m_ProgressPercent = Pref.getInt(sm_PROGRESS_TIMED_PERCENT_PREF);
      // The progress bar interval must be small enough
      // so the timed interval fits in ChordTimes storage.
      int progressMax = ChordTimes.sm_MAX_MSEC * 100 / m_ProgressPercent;
      int progressMsec = Persist.getInt(sm_PERSIST_PROGRESS_MSEC[m_RightHand?1:0], sm_DEFAULT_PROGRESS_MSEC);
      m_ProgressPanel.setMaximum(Math.max(0, Math.min(progressMsec, progressMax)));
      m_DelayMsec = Math.max(0, Persist.getInt(sm_PERSIST_DELAY_MSEC, sm_DEFAULT_DELAY_MSEC));
      HighlightStage.DELAY.setMsec(m_DelayMsec);
      m_Delay = true;
      useDelay(false);
      m_AutoScaleStep = Pref.getInt("#.chord.wait.autoscale.step", 8);
      setAutoScale(false);
   }

   ///////////////////////////////////////////////////////////////////
   @Override // Persistent
   public void persist(String tag) {
//System.out.printf("%s %d%n", sm_PERSIST_DELAY_MSEC, m_DelayMsec);
//System.out.printf("%s %d%n", sm_PERSIST_PROGRESS_MSEC, m_ProgressPanel.getMaximum());
      Persist.set(sm_PERSIST_DELAY_MSEC, m_DelayMsec);
      Persist.set(sm_PERSIST_PROGRESS_MSEC[m_RightHand?1:0], m_ProgressPanel.getMaximum());
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override
   public void setVisible(boolean visible) {
      m_MenuItemHide.setState(!visible);
      super.setVisible(visible);
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
   void reAutoScale() {
      setAutoScale(isAutoScale());
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
      Persist.set(sm_PERSIST_PROGRESS_MSEC[m_RightHand?1:0], m_ProgressPanel.getMaximum());
      m_RightHand = right;
      m_ProgressPanel.setMaximum(Persist.getInt(sm_PERSIST_PROGRESS_MSEC[m_RightHand?1:0], sm_DEFAULT_PROGRESS_MSEC));
      buildHanded();
   }

   /////////////////////////////////////////////////////////////////////////////
   int getProgressMax() {
      return m_ProgressPanel.getMaximum();
   }

   /////////////////////////////////////////////////////////////////////////////
   void useDelay(boolean set) {
      if (m_Delay == set) {
         return;
      }
      m_Delay = set;
      if (m_Delay) {
         HighlightStage.DELAY.setMsec(m_DelayMsec);
      } else {
         m_DelayMsec = m_HighlightStage.DELAY.getMsec(); 
         HighlightStage.DELAY.setMsec(0);
      }
      clearMark();
      calculateTimes();
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
      clearMark();
      calculateTimes();
   }

   /////////////////////////////////////////////////////////////////////////////
   int getProgressBarMax() {
      return m_ProgressPanel.getMaximum();
   }

   /////////////////////////////////////////////////////////////////////////////
   void setProgressBarMax(int speed) {
//System.out.printf("setProgressBarMsec(%d)%n", speed);
      m_ProgressPanel.setMaximum(speed);
      clearMark();
      calculateTimes();
   }

   /////////////////////////////////////////////////////////////////////////////
   void setDistinctChordCount(int chords) {
      m_ProgressPanel.setMaximumSamples(chords * Pref.getInt("#.chord.times.stored", 16));
      reAutoScale();
   }

   /////////////////////////////////////////////////////////////////////////////
   boolean isVertical() {
      return m_Vh == Vh.VERT;
   }

   /////////////////////////////////////////////////////////////////////////////
   void setVertical(boolean set) {
      if (set == (m_Vh == Vh.VERT)) {
         return;
      }
      boolean visible = isVisible();
      super.setVisible(false);
      int rootLength = getPersistName().length() - m_Vh.toString().length() - 1;
      m_Vh = set ? Vh.VERT : Vh.HORZ;
      changePersistName(getPersistName().substring(0, rootLength) + '.' + m_Vh.toString());
      createPanels();
      build();
      if (visible) {
         super.setVisible(true);
      }
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
      if (tw.getChord().isMouseButton()) {
         m_ProgressPanel.setMeans(0, 0, 0);
      } else {
         int ch = tw.getChord().toInt();
         int tk = tw.getThumbKeys().toInt();
         int meanMean = ct.getMeanMean(tk);
         int totalSamples = ct.getTotalSamples(tk);
         m_ProgressPanel.setMeans(ct.getMean(ch, tk), meanMean, totalSamples);
         autoScale(totalSamples, meanMean);
      }
      start();
      // don't time the first twiddle
      if (pressed == null) {
         m_ProgressTimer.stop();
      } else {
         m_ProgressTimer.start();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   void markMismatch(Twiddle tw) {
      if (m_Mark && tw != null && m_MarkTimer.getInitialDelay() > 0) {
         markNow(tw, MarkType.MISMATCH);
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

   /////////////////////////////////////////////////////////////////////////////
   void createPanels() {
      m_ThumbPanel = createThumbPanel();
      m_MousePanel = createMousePanel();
      m_ChordPanel = createChordPanel();
      m_TwiddlerPanel = createTwiddlerPanel(m_ThumbPanel, m_MousePanel, m_ChordPanel);
      if (m_ProgressPanel == null) {
         m_ProgressPanel = new ProgressPanel(m_Vh.isV(), m_RightHand, Pref.getInt("#.chord.times.stored", 16), m_COLOR_BUTTON_HIGHLIGHT, m_COLOR_BUTTON, m_COLOR_BACKGROUND);
      } else {
         m_ProgressPanel = new ProgressPanel(m_Vh.isV(), m_ProgressPanel);
      }
      final int[] BORDER = new int[]{0, 3};
      getRootPane().setBorder(BorderFactory.createMatteBorder(BORDER[m_Vh.i()], BORDER[1 - m_Vh.i()], BORDER[m_Vh.i()], BORDER[1 - m_Vh.i()], m_COLOR_BACKGROUND));
   }

   /////////////////////////////////////////////////////////////////////////////
   void build() {
      getContentPane().removeAll();
      getContentPane().setLayout(new BoxLayout(getContentPane(), m_Vh.getContentLayout()));
      m_ProgressPanel.setRightHand(m_RightHand);
      if (m_RightHand == (m_Vh == Vh.VERT)) {
         getContentPane().add(m_ProgressPanel);
         getContentPane().add(m_TwiddlerPanel);
      } else {
         getContentPane().add(m_TwiddlerPanel);
         getContentPane().add(m_ProgressPanel);
      }
      pack();
      if (isVisible()) {
         super.setVisible(true);
      }
      reAutoScale();
//System.out.printf("PP: w %d h %d%n", m_ProgressPanel.getSize().width, m_ProgressPanel.getSize().height);
//System.out.printf("TW: w %d h %d%n", m_TwiddlerPanel.getWidth(), m_TwiddlerPanel.getHeight()); 
   }

   /////////////////////////////////////////////////////////////////////////////
   private void buildHanded() {
      build();
      setTitle(m_RightHand?"Right Hand":"Left Hand");
   }

   ////////////////////////////////////////////////////////////////////////////////
   private static class KeyPanel extends JPanel {

      /////////////////////////////////////////////////////////////////////////////
      KeyPanel() {
         m_Mark = MarkType.NONE;
         m_ButtonColor = Color.black;
         m_IsMouse = false;
      }

      /////////////////////////////////////////////////////////////////////////////
      KeyPanel(Color buttonColor) {
         m_Mark = MarkType.NONE;
         m_ButtonColor = buttonColor;
         m_IsMouse = true;
      }

      /////////////////////////////////////////////////////////////////////////////
      void setMark(MarkType mark) {
         m_Mark = mark;
      }

      /////////////////////////////////////////////////////////////////////////////
      void setButtonColor(Color buttonColor) {
         m_ButtonColor = buttonColor;
      }

      /////////////////////////////////////////////////////////////////////////////
      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         if (m_IsMouse) {
            final int proportion = 2;
            int size = Math.min(getWidth(), getHeight()) / proportion;
            g.setColor(m_ButtonColor);
            g.fillOval((getWidth() - size) / 2, (getHeight() - size) / 2, size, size);
         }
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
      private Color m_ButtonColor;
      private MarkType m_Mark = MarkType.NONE;
      private boolean m_IsMouse;
   }

   ////////////////////////////////////////////////////////////////////////////////
   private static enum Vh {
      VERT(true,
           new Dimension(30, 20),
           new GridLayout(1, 3),
           new GridLayout(4, 3),
           new Dimension(20, 60),
           BoxLayout.X_AXIS,
           BoxLayout.Y_AXIS,
           new Dimension(135, 8),
           new int[]{0, 1, 2, 3}
      )
      {
         @Override
         int getButton(int finger, int button) {
            return finger * Chord.sm_BUTTONS + button - 1;
         }
      },
      
      HORZ(false,
           new Dimension(20, 30),
           new GridLayout(3, 1),
           new GridLayout(3, 4),
           new Dimension(60, 20),
           BoxLayout.Y_AXIS,
           BoxLayout.X_AXIS,
           new Dimension(4, 138),
           new int[]{2, 0, 3, 1}
      )
      {
         @Override
         int getButton(int finger, int button) {
            return (Chord.sm_BUTTONS - button) * Chord.sm_FINGERS + finger;
         }
      };
      
      Vh(boolean isVertical,
         Dimension thumbKeySize,
         GridLayout mousePanelLayout,
         GridLayout chordPanelLayout,
         Dimension chordPanelSize,
         int contentLayout,
         int twiddlerLayout,
         Dimension twiddlerRigidArea,
         int[] thumbKeyMap) {
         m_IsVertical = isVertical;
         m_ThumbKeySize = thumbKeySize;
         m_MousePanelLayout = mousePanelLayout;
         m_ChordPanelLayout = chordPanelLayout;
         m_ChordPanelSize = chordPanelSize;
         m_ContentLayout = contentLayout;
         m_TwiddlerLayout = twiddlerLayout;
         m_TwiddlerRigidArea = twiddlerRigidArea;
         m_ThumbKeyMap = thumbKeyMap;
      }

      int i() { return ordinal(); }
      boolean isV() { return m_IsVertical; }
      Dimension getThumbKeySize() { return m_ThumbKeySize; }
      GridLayout getMousePanelLayout() { return m_MousePanelLayout; }
      GridLayout getChordPanelLayout() { return m_ChordPanelLayout; }
      Dimension getChordPanelSize() { return m_ChordPanelSize; }
      int getContentLayout() { return m_ContentLayout; }
      int getTwiddlerLayout() { return m_TwiddlerLayout; }
      Dimension getTwiddlerRigidArea() { return m_TwiddlerRigidArea; }
      int[] getThumbKeyMap() { return m_ThumbKeyMap; }
      abstract int getButton(int finger, int button);

      private boolean m_IsVertical;
      private Dimension m_ThumbKeySize;
      private GridLayout m_MousePanelLayout;
      private GridLayout m_ChordPanelLayout;
      private Dimension m_ChordPanelSize;
      private int m_ContentLayout;
      private int m_TwiddlerLayout;
      private Dimension m_TwiddlerRigidArea;
      private int[] m_ThumbKeyMap;
   }

   /////////////////////////////////////////////////////////////////////////////
   private JPanel createThumbPanel() {
      JPanel thp = new JPanel(new GridLayout(2, 2));
      thp.setBackground(m_COLOR_BACKGROUND);
      final int sm_FAT = 24;
      for (int i = 0; i < 4; ++i) {
         KeyPanel p = new KeyPanel();
         p.setBackground(m_COLOR_BUTTON);
         thp.add(p);
         p.setPreferredSize(m_Vh.getThumbKeySize());
         int top[] = new int[]{10, sm_THIN + 4};
         int bot[] = new int[]{10, sm_THIN};
         int vhI = m_Vh.i();
         String text = "";
         if (i == 0) {
            text = m_Vh.isV() ? "Num" : "Shift";
            bot[0] = sm_FAT;
         } else if (i == 1) {
            text = m_Vh.isV() ? "Shift" : "Ctrl";
            top[0] = sm_FAT;
         } else if (i == 2) {
            text = m_Vh.isV() ? "Alt" : "Num";
            top[0] = sm_FAT;
         } else if (i == 3) {
            text = m_Vh.isV() ? "Ctrl" : "Alt";
            bot[0] = sm_FAT;
         }
         JLabel label = new JLabel(text);
         label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
         label.setForeground(m_COLOR_LABEL);
         p.add(label);
         p.setBorder(BorderFactory.createMatteBorder(top[1 - vhI], top[vhI], bot[1 - vhI], bot[vhI], m_COLOR_BACKGROUND));
      }
      return thp;
   }

   /////////////////////////////////////////////////////////////////////////////
   private JPanel createMousePanel() {
      JPanel mp = new JPanel(m_Vh.getMousePanelLayout());
      mp.setBackground(m_COLOR_BACKGROUND);
      Dimension size = new Dimension(10, 10);
      for (int i = 0; i < 3; ++i) {
         KeyPanel p = new KeyPanel(m_COLOR_BUTTON);
         p.setBackground(m_COLOR_BACKGROUND);
         p.setPreferredSize(size);
//         p.setBorder(BorderFactory.createMatteBorder(sm_THIN, sm_THIN, sm_THIN, sm_THIN, m_COLOR_BACKGROUND));
         mp.add(p);
      }
      return mp;
   }

   /////////////////////////////////////////////////////////////////////////////
   private JPanel createChordPanel() {
      JPanel chp = new JPanel(m_Vh.getChordPanelLayout());
      chp.setBackground(m_COLOR_BACKGROUND);
      Dimension size = m_Vh.getChordPanelSize();
      for (int i = 0; i < 12; ++i) {
         KeyPanel p = new KeyPanel();
         p.setBackground(m_COLOR_BUTTON);
         p.setPreferredSize(size);
         p.setBorder(BorderFactory.createMatteBorder(sm_THIN, sm_THIN, sm_THIN, sm_THIN, m_COLOR_BACKGROUND));
         chp.add(p);
      }
      return chp;
   }

   /////////////////////////////////////////////////////////////////////////////
   private JPanel createTwiddlerPanel(JPanel thumbPanel, JPanel mousePanel, JPanel chordPanel) {
      JPanel twp = new JPanel();
      twp.setLayout(new BoxLayout(twp, m_Vh.getTwiddlerLayout()));
      twp.setBackground(m_COLOR_BACKGROUND);
      twp.add(Box.createRigidArea(m_Vh.getTwiddlerRigidArea()));
      twp.add(thumbPanel);
      twp.add(mousePanel);
      twp.add(chordPanel);
      return twp;
   }

   /////////////////////////////////////////////////////////////////////////////
   private void calculateTimes() {
      final int progressMsec = m_ProgressPanel.getMaximum();
      double markFactor = Pref.getInt(sm_MARK_PERCENT_PREF, sm_DEFAULT_SHOW_PERCENT) / 100.0;
      m_MarkTimer.setInitialDelay((int)(0.5 + progressMsec * markFactor));
      m_Mark = (m_MarkTimer.getInitialDelay() != 0);

      double factor = (m_Delay
                      ? Pref.getInt(sm_DELAYED_SHOW_PERCENT_PREF, sm_DEFAULT_SHOW_PERCENT)
                      : Pref.getInt(sm_SHOW_PERCENT_PREF, sm_DEFAULT_SHOW_PERCENT))
                    / 100.0;
      int rest = Math.max(0, progressMsec - m_HighlightStage.DELAY.getMsec());
      // can't show for less than 0 or more than rest of progress time
      int show = Math.max(0,
                          Math.min(rest,
                                   (int)(0.5 + progressMsec * factor)));
      HighlightStage.HIGHLIGHT.setMsec(show);
      HighlightStage.HIDE.setMsec(rest - show);
      m_Highlight = show > 0;
      m_ProgressPanel.setMaximum(progressMsec);
//System.out.printf("delay %d progressMsec %d delay %d show %d hide %d factor %g%n", 
//         m_DelayMsec, progressMsec, 
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
      return m_ProgressPanel.getMaximum() > 0 
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
//System.out.println("highlight()");
      if (!m_Highlight || m_Twiddle == null) {
         return;
      }
      ThumbKeys thumbs = m_Twiddle.getThumbKeys();
      if (thumbs.isNum()) {
         int tk = m_Vh.getThumbKeyMap()[0];
         m_ThumbPanel.getComponent(tk).setBackground(m_COLOR_BUTTON_HIGHLIGHT);
      }
      if (thumbs.isShift()) {
         int tk = m_Vh.getThumbKeyMap()[1];
         m_ThumbPanel.getComponent(tk).setBackground(m_COLOR_BUTTON_HIGHLIGHT);
      }
      if (thumbs.isAlt()) {
         int tk = m_Vh.getThumbKeyMap()[2];
         m_ThumbPanel.getComponent(tk).setBackground(m_COLOR_BUTTON_HIGHLIGHT);
      }
      if (thumbs.isCtrl()) {
         int tk = m_Vh.getThumbKeyMap()[3];
         m_ThumbPanel.getComponent(tk).setBackground(m_COLOR_BUTTON_HIGHLIGHT);
      }
      if (m_Twiddle.getChord().isMouseButton()) {
         ((KeyPanel)m_MousePanel.getComponent(m_Twiddle.getChord().getMouseButton() - 1)).setButtonColor(m_COLOR_BUTTON_HIGHLIGHT);
      } else {
         for (int f = 0; f < Chord.sm_FINGERS; ++f) {
            int c = m_Twiddle.getChord().getFingerKey(f);
            if (c != 0) {
   //System.out.printf("r %d c %d button %d%n", r, c, m_Vh.getButton(r, c));
               m_ChordPanel.getComponent(m_Vh.getButton(f, c)).setBackground(m_COLOR_BUTTON_HIGHLIGHT);
            }
         }
      }
      repaint();
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private void clearHighlight() {
//System.out.println("clearHighlight()");
      for (int i = 0; i < 4; ++i) {
         m_ThumbPanel.getComponent(i).setBackground(m_COLOR_BUTTON);
      }
      for (int i = 0; i < 3; ++i) {
         ((KeyPanel)m_MousePanel.getComponent(i)).setButtonColor(m_COLOR_BUTTON);
      }
      for (int i = 0; i < 12; ++i) {
         m_ChordPanel.getComponent(i).setBackground(m_COLOR_BUTTON);
      }
      repaint();
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
         int tk = m_Vh.getThumbKeyMap()[0];
         ((KeyPanel)m_ThumbPanel.getComponent(tk)).setMark(type);
      }
      if (thumbs.isShift()) {
         int tk = m_Vh.getThumbKeyMap()[1];
         ((KeyPanel)m_ThumbPanel.getComponent(tk)).setMark(type);
      }
      if (thumbs.isAlt()) {
         int tk = m_Vh.getThumbKeyMap()[2];
         ((KeyPanel)m_ThumbPanel.getComponent(tk)).setMark(type);
      }
      if (thumbs.isCtrl()) {
         int tk = m_Vh.getThumbKeyMap()[3];
         ((KeyPanel)m_ThumbPanel.getComponent(tk)).setMark(type);
      }
      Chord ch = tw.getChord();
      if (ch.isMouseButton()) {
         ((KeyPanel)m_MousePanel.getComponent(ch.getMouseButton() - 1)).setMark(type);
      } else {
         for (int f = 0; f < Chord.sm_FINGERS; ++f) {
            int c = ch.getFingerKey(f);
            if (c != 0) {
               ((KeyPanel)m_ChordPanel.getComponent(m_Vh.getButton(f, c))).setMark(type);
            }
         }
      }
      m_MarkTimer.restart();
      repaint();
   }

   /////////////////////////////////////////////////////////////////////////////
   private void clearMark() {
      m_MarkTimer.stop();
      for (int i = 0; i < 4; ++i) {
         ((KeyPanel)m_ThumbPanel.getComponent(i)).setMark(MarkType.NONE);
      }
      for (int i = 0; i < 3; ++i) {
         ((KeyPanel)m_MousePanel.getComponent(i)).setMark(MarkType.NONE);
      }
      for (int i = 0; i < 12; ++i) {
         ((KeyPanel)m_ChordPanel.getComponent(i)).setMark(MarkType.NONE);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private void autoScale(int totalSamples, int meanMean) {
//System.out.printf("autoScale m_AutoScaleCount %d meanMean %d%n", m_AutoScaleCount, meanMean);
      if (m_AutoScaleCount == 0) {
         return;
      }
      ++m_AutoScaleCount;
      if (m_AutoScaleCount % m_AutoScaleStep != 2) {
         return;
      }
      if (totalSamples < m_AutoScaleStep) {
         m_ProgressPanel.setMaximum(sm_DEFAULT_PROGRESS_MSEC);
      } else {
         m_ProgressPanel.autoScale(meanMean);
      }
      calculateTimes();
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
   private static final String[] sm_PERSIST_PROGRESS_MSEC = 
      new String[]{"#.progress.left.msec",   // PERSIST
                   "#.progress.right.msec"}; // PERSIST

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

   private JCheckBoxMenuItem m_MenuItemHide;
   private JPanel m_ThumbPanel;
   private JPanel m_MousePanel;
   private JPanel m_ChordPanel;
   private JPanel m_TwiddlerPanel;
   private ProgressPanel m_ProgressPanel;
   private Timer m_ProgressTimer;
   private long m_ProgressStart;
   private int m_ProgressPercent;
   private Button[] m_Thumb;
   private boolean m_Highlight;
   private Timer m_HighlightTimer;
   private HighlightStage m_HighlightStage;
   private boolean m_Delay;
   private int m_DelayMsec;
   private boolean m_Mark;
   private Timer m_MarkTimer;
   private int m_AutoScaleCount;
   private int m_AutoScaleStep;
   private Twiddle m_Twiddle;
   private boolean m_RightHand;
   private Vh m_Vh;
}
