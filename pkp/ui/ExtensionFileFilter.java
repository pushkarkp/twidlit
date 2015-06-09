/**
 * Copyright 2015 Pushkar Piggott
 *
 *  ExtensionFileFilter.java
 */
package pkp.ui;

import java.io.File;
import javax.swing.filechooser.FileFilter;

//////////////////////////////////////////////////////////////////////
public class ExtensionFileFilter extends FileFilter {

   ///////////////////////////////////////////////////////////////////
   public ExtensionFileFilter(String extension) {
      m_Extension = extension.toLowerCase();
   }

   ///////////////////////////////////////////////////////////////////
   @Override
   public String getDescription() {
      return m_Extension + m_Extra;
   }

   ///////////////////////////////////////////////////////////////////
   @Override
   public boolean accept(File file) {
      return file.isDirectory() 
          || file.getAbsolutePath().toLowerCase().endsWith("." + m_Extension);
   }

   ///////////////////////////////////////////////////////////////////
   public String getExtension() {
      return m_Extension;
   }

   ///////////////////////////////////////////////////////////////////
   public File withExtension(File file) {
      if (file.getPath().endsWith(m_Extension)) {
         return file;
      }
      return new File(file.getPath() + "." + m_Extension);
   }
   
   // Data ///////////////////////////////////////////////////////////
   private static String m_Extra = " files";
   private String m_Extension;
}
