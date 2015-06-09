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
import java.awt.Desktop;
import javax.swing.border.EmptyBorder;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import pkp.util.Pref;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class HtmlWindow extends PersistentFrame {

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
   public HtmlWindow(URL url) {
      setIconImage(Pref.getIcon().getImage());
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      try {
         m_EditorPane = new JEditorPane(url);
         m_EditorPane.setContentType("text/html");
         m_EditorPane.setEditable(false);
         m_EditorPane.setBackground(Pref.getColor("background.color"));
         m_EditorPane.setBorder(new EmptyBorder(0, 20, 10, 20));
         m_EditorPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hle) {
               if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                  String desc = hle.getDescription();
//System.out.println(desc);
                  // internal link
                  if (desc != null && desc.startsWith("#")) {
                     desc = desc.substring(1);
                     m_EditorPane.scrollToReference(desc);
                     return;
                  }
                  // external link
                  String link = hle.getURL().toString();
                  try {
                     if (link.startsWith("file:/")) {
                        // HyperlinkEvent adds stuff to the front
                        link = link.substring("file:/".length());
                     }
                     //if (link.startsWith("jar:file:/")) {
                     //   link = "file:" + link.substring(link.lastIndexOf('!'));
                     //}
//System.out.println(link);
                     Desktop desktop = Desktop.getDesktop();
                     desktop.browse((new URL(link)).toURI());
                  } catch (Exception ex) {
                     ex.printStackTrace();
                  }
               }
            }
         });
         getContentPane().add(new JScrollPane(
            m_EditorPane,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
      } catch (IOException e) {
         Log.err("HtmlWindow failed to find the file '" + url.getPath() + "' " + e);
      }
      pack();
      setResizable(true);
   }
   
   // Data ////////////////////////////////////////////////////////
   JEditorPane m_EditorPane;
}
