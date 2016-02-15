/**
 * Copyright 2015 Pushkar Piggott
 *
 *  TextWindow.java
 */
package pkp.ui;

import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.net.URL;
import pkp.util.Pref;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class TextWindow extends PersistentFrame {

   ////////////////////////////////////////////////////////////////////////////
   public TextWindow() {
      super();
      setIconImage(Pref.getIcon().getImage());
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      getContentPane().setBackground(Pref.getColor("background.color"));
      getContentPane().setLayout(new BorderLayout());
      m_TextArea = new JTextArea();
      m_TextArea.setBackground(Pref.getColor("background.color"));
      m_TextArea.setEditable(false);
      m_ScrollPane = new JScrollPane(m_TextArea);
      getContentPane().add(m_ScrollPane, BorderLayout.CENTER);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public TextWindow(String title, String str) {
      this();
      setTitle(title);
      replaceText(str);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public TextWindow(URL url) {
      this();
      setTitle(url.toString());
      appendTextFile(url);
      scrollTop();
   }

   ////////////////////////////////////////////////////////////////////////////
   public Font getFont() {
      return m_TextArea.getFont();
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setFont(Font font) {
      m_TextArea.setFont(font);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void appendTextFile(URL url) {
      BufferedReader in = null;
      try {
         in = new BufferedReader(new InputStreamReader(url.openStream()));
      } catch (IOException e) {
         Log.err("TextWindow failed to open the file \"" + url + "\"");
      }
      try {
         String line;
         for (int i = 0; (line = in.readLine()) != null; ++i) {
            appendln(line);
         }
         in.close();
      } catch (IOException e) {
         Log.err("TextWindow failed to read the file \"" + url + "\"");
      }
      pack();
   }

   ////////////////////////////////////////////////////////////////////////////
   public void clear() {
      m_TextArea.setText("");
   }

   ////////////////////////////////////////////////////////////////////////////
   public void append(String text) {
      m_TextArea.append(text);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void appendln(String text) {
      m_TextArea.append(text + "\n");
   }

   ////////////////////////////////////////////////////////////////////////////
   public void scrollTop() {
      m_TextArea.setCaretPosition(0);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void replaceText(String str) {
      clear();
      append(str);
      scrollTop();
   }

   ////////////////////////////////////////////////////////////////////////////
   public String getText() {
      return m_TextArea.getText();
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setBackgroundColor(Color c) {
      ((JLabel)getContentPane()).setBackground(c);
   }

   // Data ////////////////////////////////////////////////////////////////////
   private JScrollPane m_ScrollPane;
   private JTextArea m_TextArea;
}
