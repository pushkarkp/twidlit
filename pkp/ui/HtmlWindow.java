/**
 * Copyright 2015 Pushkar Piggott
 *
 *  HtmlWindow.java
 */
package pkp.ui;

import java.io.IOException;
import java.net.URL;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Desktop;
import java.util.ArrayList;
import javax.swing.border.EmptyBorder;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import pkp.util.Pref;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class HtmlWindow extends PersistentFrame implements ActionListener {

   ////////////////////////////////////////////////////////////////////////////
   public HtmlWindow() {
      this("");
   }

   ////////////////////////////////////////////////////////////////////////////
   public HtmlWindow(URL url) {
      this(url.toString());
   }

   ////////////////////////////////////////////////////////////////////////////
   public HtmlWindow(String startLink) {
      setResizable(true);
      setIconImage(Pref.getIcon().getImage());
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      m_I = -1;
      m_Link = new ArrayList<String>();
      m_ScrollPos = new ArrayList<Integer>();
      m_EditorPane = new JEditorPane();
      m_EditorPane.setContentType("text/html");
      m_EditorPane.setEditable(false);
      m_EditorPane.setOpaque(false);
      m_EditorPane.setBorder(new EmptyBorder(0, 20, 10, 20));
      m_EditorPane.addHyperlinkListener(new HyperlinkListener() {
         @Override
         public void hyperlinkUpdate(HyperlinkEvent hle) {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
               String link = hle.getDescription();
               if (link == null || !link.startsWith("#")) {
                  link = hle.getURL().toString();
               }
               if (link.startsWith("#")
                || link.startsWith("file:/")
                || link.startsWith("jar:file:/")) {
                  goTo(link);
               } else {
                  // external link, fire up the browser
                  try {
                     Desktop desktop = Desktop.getDesktop();
                     desktop.browse((new URL(link)).toURI());
                  } catch (Exception ex) {
                     ex.printStackTrace();
                  }
               }
            }
         }
      });
      m_EditorPane.addPropertyChangeListener("page", new PropertyChangeListener() {
         @Override
         public void propertyChange(PropertyChangeEvent pce) {
            if (m_Width == 0) {
               m_Width = getWidth();
            }
            if (m_PagePos != -1) {
               m_ScrollPane.getVerticalScrollBar().setValue(m_PagePos);
               m_PagePos = -1;
            }
            Object title = m_EditorPane.getDocument().getProperty("title");
            if (title != null) {
               m_Title.setText(title.toString());
            }
            m_EditorPane.setVisible(true);
         }
      });
      m_PagePos = -1;
      m_Width = 0;
      m_Title = new JLabel();
      m_Title.setOpaque(false);
      m_ScrollPane = new JScrollPane(
         m_EditorPane,
         JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      m_ScrollPane.getViewport().setBackground(Pref.getColor("#.background.color"));
      getContentPane().add(m_ScrollPane);
      pack();
      goTo(startLink);
   }
   
   // ActionListener //////////////////////////////////////////////////////////
   @Override
   public void actionPerformed(ActionEvent e) {
      if (m_I >= 0) {
         // save pre-jump location as a vscroll offset
         m_ScrollPos.set(m_I, m_ScrollPane.getVerticalScrollBar().getValue());
      }
      switch (e.getActionCommand()) {
      default:
         return;
      case m_FORE_TEXT:
         if (m_I < m_Link.size() - 1) {
            advanceI(1);
            // repeat the original jump
            show(m_Link.get(m_I));
         }
         return;
      case m_BACK_TEXT:
         if (m_I > 0) {
            break;
         }
         return;
      }
      if (getWidth() < m_Width - 5 || m_Width + 5 < getWidth()) {
         m_Width = getWidth();
         for (int i = 0; i < m_ScrollPos.size(); ++i) {
            // scroll offsets are now invalid
            m_ScrollPos.set(i, -1);
         }
      }
      advanceI(-1);
      if (m_Link.get(m_I + 1).charAt(0) == '#') {
         // reversing internal link on current page
         int pos = m_ScrollPos.get(m_I);
         if (pos != -1) {
            // just scroll to jump-off point
            m_ScrollPane.getVerticalScrollBar().setValue(pos);
         } else {
            // invalid scroll position
            // go to anchor, ignore page if present
            String link = m_Link.get(m_I);
            int hash = link.indexOf('#');
            link = (hash == -1 ? "#top" : link.substring(hash));
            show(link);
         }
         return;
      }
      // back to previous page, find page link
      int i = m_I - 1;
      while (i >= 0) {
         if (m_Link.get(i).charAt(0) != '#') {
            break;
         }
         --i;
      }
      if (i == -1) {
         Log.err("HtmlWindow: missing link in history");
         return;
      }
      String link = m_Link.get(i);
      int hash = link.indexOf('#');
      m_PagePos = m_ScrollPos.get(m_I);
      if (m_PagePos == -1) {
         if (hash == -1) {
            // no scroll, no anchor, add it
            hash = m_Link.get(m_I).indexOf('#');
            if (hash != -1) {
               link += m_Link.get(m_I).substring(hash);
            }
         }
      } else {
         // remove internal anchor, using scroll position
         if (hash != -1) {
            link = link.substring(0, hash);
         }
      }
      show(link);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public void goTo(String link) {
      if (m_I >= 0) {
         // save pre-jump location as a vscroll offset
         m_ScrollPos.set(m_I, m_ScrollPane.getVerticalScrollBar().getValue());
      }
      if (show(link)) {
         advanceI(1);
         if (m_I < m_Link.size()) {
            m_Link.subList(m_I, m_Link.size()).clear();
            m_ScrollPos.subList(m_I, m_ScrollPos.size()).clear();
         }
         m_Link.add(link);
         m_ScrollPos.add(new Integer(-1));
      }
   }

   // Private/////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private JButton createButton(String name) {
      JButton b = new JButton(name);
      b.addActionListener(this);
      b.setOpaque(false);
      b.setPreferredSize(new Dimension(25, 15));
      b.setMargin(new Insets(2, 2, 2, 2));
      b.setEnabled(false);
      return b;
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean show(String link) {
      if (link == null || "".equals(link)) {
         return false;
      }
      if (link.charAt(0) == '#') {
         // internal link
         link = link.substring(1);
         m_EditorPane.scrollToReference(link);
      } else {
         // external link
         try {
            m_EditorPane.setVisible(false);
            m_EditorPane.setPage(new URL(link));
         } catch (IOException e) {
            try {
               if (m_I >= 0) {
                  m_EditorPane.setPage(m_Link.get(m_I));
               }
               return false;
            } catch (IOException f) {
               return false;
            }
         }
      }
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   private void advanceI(int i) {
      m_I += i;
      if (m_I > 0) {
         if (m_BackButton == null) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING));
            m_BackButton = createButton(m_BACK_TEXT);
            p.add(m_BackButton);
            m_ForeButton = createButton(m_FORE_TEXT);
            p.add(m_ForeButton);
            getContentPane().add(p, BorderLayout.PAGE_START);
            p.add(m_Title);
            setVisible(true);
         }
      }
      if (m_BackButton != null) {
         m_BackButton.setEnabled(m_I > 0);
         m_ForeButton.setEnabled(m_I < m_Link.size() - 1);
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private static String toString(int i, ArrayList<String> link, ArrayList<Integer> scrollPos) {
      String str = "";
      for (int j = link.size() - 1; j >= 0; --j) {
         if (j == i) {
            str += "> ";
         }
         str += String.format("%s %d%n", link.get(i), scrollPos.get(i));
      }
      return str;
   }

   // Data ////////////////////////////////////////////////////////
   private final String m_BACK_TEXT = "<";
   private final String m_FORE_TEXT = ">";
   
   private ArrayList<String> m_Link;
   private ArrayList<Integer> m_ScrollPos;
   private int m_I;
   private JButton m_BackButton;
   private JButton m_ForeButton;
   private JLabel m_Title;
   private JScrollPane m_ScrollPane;
   private JEditorPane m_EditorPane;
   private int m_PagePos;
   private int m_Width;
}
