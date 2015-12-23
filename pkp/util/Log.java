/**
 * Copyright 2015 Pushkar Piggott
 *
 *  Log.java
 */
 package pkp.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Date;
import java.text.SimpleDateFormat;
import pkp.ui.TextWindow;

///////////////////////////////////////////////////////////////////////////////
public class Log implements ActionListener {

   ////////////////////////////////////////////////////////////////////////////
   public static final boolean ExitOnError = true;

   ////////////////////////////////////////////////////////////////////////////
   public interface Quitter {
      public void quit();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void setQuitter(Quitter qr) {
      sm_Quitter = qr;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void setWindow(Window w) {
      sm_Window = w;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void init(File f, boolean exitOnError) {
      sm_ExitOnError = exitOnError;
      sm_Log = new Log();
      sm_Log.setFile(f);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void init(boolean exitOnError) {
      init(null, exitOnError);
   }

   ////////////////////////////////////////////////////////////////////////////
   // Can set file later, after other services (such as persistent preferences)
   // that are used in opening files, and log errors, become available.
   public static void setFile(File f) {
      sm_Log.setFile1(f);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static boolean hasFile() {
      return sm_Log.hasFile1();
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Log get() {
      return sm_Log;
   }

   ////////////////////////////////////////////////////////////////////////////
   public enum Level {
      INFO("Info: "),
      WARN("Warning: "),
      ERROR("Error: ");
      public final String m_Name;
      private Level(String name) {
         m_Name = name;
      }
   };

   ////////////////////////////////////////////////////////////////////////////
   public static void log(Level level, String msg) {
      sm_Log.log1(level, msg);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void log(String msg) {
      sm_Log.log1(Level.INFO, msg);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void warn(String msg) {
      JOptionPane.showMessageDialog(sm_Window, msg, "Warning", JOptionPane.WARNING_MESSAGE);
      sm_Log.log1(Level.WARN, msg);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void err(String msg) {
      JOptionPane.showMessageDialog(sm_Window, msg, "Error", JOptionPane.ERROR_MESSAGE);
		if (sm_Log != null) {
			sm_Log.log1(Level.ERROR, msg);
		}
		if (!sm_ExitOnError) {
			return;
		}
      java.lang.Thread.dumpStack();
      if (sm_Quitter != null) {
         sm_Quitter.quit();
      } else {
			if (sm_Log != null) {
				sm_Log.close1();
			}
         System.exit(1);
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void close() {
      sm_Log.close1();
   }

   // Instance ///////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   public void setVisible(boolean visible) {
      if (visible) {
         flush();
			if (m_TextWindow == null) {
            constructTextWindow();
			}
      }
      if (m_TextWindow != null) {
         m_TextWindow.setVisible(visible);
         m_TextWindow.toFront();
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public void setLocation(Point location) {
      if (m_TextWindow != null) {
         m_TextWindow.setLocation(location);
      }
   }

   // Protected ///////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   protected Log() {
      m_File = null;
   }

   ////////////////////////////////////////////////////////////////////////////
   protected void setFile1(File f) {
      m_File = f;
      openFile();
      write(sm_DATE_TIME_FORMAT.format(new Date()) + ": started.");
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean hasFile1() {
      return m_File != null;
   }

   ////////////////////////////////////////////////////////////////////////////
   protected void log1(Level level, String text) {
      String msg = sm_TIME_FORMAT.format(new Date()) + level.m_Name + text;
      write(msg);
      if (level == Level.ERROR) {
         System.err.println(msg);      
      }
      if (m_TextWindow != null) {
         m_TextWindow.appendln(msg);
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   protected void close1() {
      setVisible(false);
      write(sm_DATE_TIME_FORMAT.format(new Date()) + ": stopped.");
      closeFile();
   }

   // ActionListener //////////////////////////////////////////////////////////
   @Override
   public void actionPerformed(ActionEvent e) {
      if (sm_BUTTON_STRING.equals(e.getActionCommand())) {
         m_TextWindow.clear();
         closeFile();
         if (m_File.exists() && !m_File.isDirectory() && !m_File.delete()) {
            warn("Log failed to delete \"" + m_File + "\".");
         }
         openFile();
      }
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   public void constructTextWindow() {
      URL url = null;
      try {
         url = m_File.toURI().toURL();
      } catch (MalformedURLException e) {
         Log.err("Failed to create URL from \"" + m_File + "\": " + e + ".");
      }
      m_TextWindow = new TextWindow(url);
      m_TextWindow.setTitle("TwidlitLog.txt");
      JButton b = new JButton(sm_BUTTON_STRING);
      b.addActionListener(this);
      b.setPreferredSize(new Dimension(65, 20));
      JPanel p = new JPanel(new FlowLayout(FlowLayout.TRAILING));
      p.add(b);
      m_TextWindow.getContentPane().add(p, BorderLayout.PAGE_END);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void openFile() {
      if (m_File == null) {
         return;
      }
      try {
         m_Out = new BufferedWriter(new FileWriter(m_File.getPath(), true));
      } catch (IOException e) {
         err("Log failed to open \"" + m_File.getPath() + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   protected void closeFile() {
      if (m_File == null) {
         return;
      }
      flush();
      try {
         m_Out.close();
      } catch (IOException e) {
         err("Log failed to close \"" + m_File.getPath() + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   protected void write(String text) {
      if (m_File == null) {
         return;
      }
      try {
         m_Out.write(text);
         m_Out.newLine();
      } catch (IOException e) {
			sm_Log = null;
         err("Log failed to write to \"" + m_File.getPath() + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private void flush() {
      if (m_File == null) {
         return;
      }
      try {
         m_Out.flush();
      } catch (IOException e) {
         err("Log failed to flush \"" + m_File.getPath() + "\".");
      }
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static final String sm_BUTTON_STRING = "Clear";
   private static final SimpleDateFormat sm_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
   private static final SimpleDateFormat sm_TIME_FORMAT = new SimpleDateFormat("hh:mm:ss ");
   private static Quitter sm_Quitter = null;
   private static Window sm_Window = null;
   private static Log sm_Log = null;
   private static boolean sm_ExitOnError = true;
   private File m_File;
   private BufferedWriter m_Out;
   private TextWindow m_TextWindow;

   // Main ////////////////////////////////////////////////////////////////////
   public static void main(String[] args) {
      Log.init(new File("log.txt"), false);
      for (int i = 0; i < args.length; ++i) {
         Log.log(args[i]);
         if (i == args.length / 2) {
            Log.get().setVisible(true);
         } else if (i > args.length / 2) {
            try {
               Thread.sleep(2000);
            } catch (InterruptedException e) {}
         }
      }
      Log.close();
      Log.log("Too late");
   }
}
