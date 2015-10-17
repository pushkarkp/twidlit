/**
 * Copyright 2015 Pushkar Piggott
 *
 * PersistentProperties.java
 */
package pkp.util;

import java.util.Properties;
import java.net.URL;
import java.io.*;
import pkp.io.Io;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class PersistentProperties {

   ////////////////////////////////////////////////////////////////////////////
   public static String toTag(String name) {
      return name.toLowerCase().replace(" ", ".");
   }

   ////////////////////////////////////////////////////////////////////////////
   public PersistentProperties(String fileName, String fileParent, String jarParent, boolean mustExist) {
      m_FileName = fileName;
      m_FileParent = fileParent;
      m_JarParent = jarParent;
      m_MustExist = mustExist;
      m_Properties = new Properties();
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean isEmpty() { return m_Properties.isEmpty(); }
   public boolean mustExist() { return m_MustExist; }
   public String getFileName() { return m_FileName; }
   public String getFolderName() { return m_FileParent; }

   ////////////////////////////////////////////////////////////////////////////
   public String get(String name) {
      String val = m_Properties.getProperty(toTag(name));
//System.out.println("prop.get \"" + name + "\" \"" + toTag(name) + "\" " + val);
      return val;
   }

   ////////////////////////////////////////////////////////////////////////////
   public void set(String name, String value) {
//System.out.println("prop.set \"" + toTag(name) + "=" + value);
      m_Properties.setProperty(toTag(name), value);
   }

   ////////////////////////////////////////////////////////////////////////////
   public void read() {
      URL url = Io.toUrl(m_FileName, m_FileParent, m_JarParent);
      if (url == null) {
         if (m_MustExist) {
            Io.fileExistsOrExit(Io.createFile(m_FileParent, m_FileName));
         } else {
            return;
         }
      }
//System.out.println("Persist: " + url);
      try {
         BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
         m_Properties.load(br);
         br.close();
      } catch (IOException e) {
         Log.err("Failed to read \"" + m_FileName + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public void write() {
      try {
         BufferedWriter br = new BufferedWriter(new FileWriter(Io.createFile(m_FileParent, m_FileName)));
         m_Properties.store(br, "");
      } catch (IOException e) {
         Log.err("Failed to write \"" + m_FileName + "\".");
      }
   }

   // Data ////////////////////////////////////////////////////////////////////
   private String m_FileName;
   private String m_FileParent;
   private String m_JarParent;
   private Properties m_Properties;
   private boolean m_MustExist;
}
