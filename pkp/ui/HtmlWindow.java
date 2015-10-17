/**
 * Copyright 2015 Pushkar Piggott
 *
 *  HtmlWindow.java
 */
package pkp.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Container;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Desktop;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.border.EmptyBorder;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import pkp.util.Pref;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class HtmlWindow extends PersistentFrame implements ActionListener {

   ////////////////////////////////////////////////////////////////////////////
   public static HtmlWindow create(String fileName) {
      try {
         return new HtmlWindow(new File(fileName).toURI().toURL());      
      } catch (MalformedURLException e) {
         Log.err("Failed to create URL from '" + fileName + "' " + e);
      }
      return null;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public HtmlWindow(URL url) {//, boolean withBackButton) {
      setIconImage(Pref.getIcon().getImage());
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      m_Position = -1;
      m_History = new ArrayList<String>();
      m_EditorPane = new JEditorPane();
      m_EditorPane.setContentType("text/html");
      m_EditorPane.setEditable(false);
      m_EditorPane.setBackground(Pref.getColor("background.color"));
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
      getContentPane().add(new JScrollPane(
         m_EditorPane,
         JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
      startWith(url.toString());
      pack();
      setResizable(true);
   }
   
   // ActionListener //////////////////////////////////////////////////////////
   @Override
   public void actionPerformed(ActionEvent e) {
      if (m_BackButton == null) {
         addButtons();
      }
      String link = m_History.get(m_Position);
      switch (e.getActionCommand()) {
      case m_BACK_TEXT:
         if (m_Position > 0) {
            --m_Position;
            if (m_History.get(m_Position + 1).startsWith("#")
             || !m_History.get(m_Position).startsWith("#")) {
               if (m_History.get(m_Position).startsWith("#")) {
                  link = m_History.get(m_Position);
               } else {
                  link = m_History.get(m_Position) + "#top";
               }
            } else {
               for (int i = m_Position - 1; i >= 0; --i) {
                  if (!m_History.get(i).startsWith("#")) {
                     link = m_History.get(i)
                          + m_History.get(m_Position);
                     break;
                  }
               }
            }
         }
         break;
      case m_FORE_TEXT:
         if (m_Position < m_History.size() - 1) {
            ++m_Position;
            link = m_History.get(m_Position);
         }
         break;
      }
      show(link);
      setButtons();
   }
   
   // Private/////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private void addButtons() {
      JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING));
      p.setBackground(Pref.getColor("background.color"));
      m_BackButton = createButton(m_BACK_TEXT);
      p.add(m_BackButton);
      m_ForeButton = createButton(m_FORE_TEXT);
      p.add(m_ForeButton);
      getContentPane().add(p, BorderLayout.PAGE_START);
      setVisible(true);
   }
   
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
   private void startWith(String link) {
      if (show(link)) {
         ++m_Position;
         m_History.add(link);
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private void goTo(String link) {
      if (show(link)) {
         ++m_Position;
         if (m_Position < m_History.size()) {
            m_History.subList(m_Position, m_History.size()).clear();
         }
         m_History.add(link);
         if (m_BackButton == null) {
            addButtons();
         }
         setButtons();
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean show(String link) {
//System.out.println("link " + link);
      if (link.startsWith("#")) {
         // internal link
         link = link.substring(1);
         m_EditorPane.scrollToReference(link);
      } else {
         // external link
         try {
            m_EditorPane.setPage(new URL(link));
         } catch (IOException e) {
            try {
               if (m_Position >= 0) {
                  m_EditorPane.setPage(m_History.get(m_Position));
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
   private void setButtons() {
      m_BackButton.setEnabled(m_Position > 0);
      m_ForeButton.setEnabled(m_Position < m_History.size() - 1);
   }

   // Data ////////////////////////////////////////////////////////
   private final String m_BACK_TEXT = "<";
   private final String m_FORE_TEXT = ">";
   
   private int m_Position;
   private ArrayList<String> m_History;
   private JButton m_BackButton;
   private JButton m_ForeButton;
   private JEditorPane m_EditorPane;
}
