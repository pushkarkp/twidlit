/**
 * Copyright 2015 Pushkar Piggott
 *
 *  ExtensionFileFilter.java
 */
package pkp.ui;

import java.util.List;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

//////////////////////////////////////////////////////////////////////
public class ExtensionFileFilter extends FileFilter {

   ///////////////////////////////////////////////////////////////////
   public static void addFileFilter(JFileChooser fc, String extension) {
      fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());
      fc.addChoosableFileFilter(new ExtensionFileFilter(extension));
      fc.addChoosableFileFilter(fc.getAcceptAllFileFilter());
   }

   ///////////////////////////////////////////////////////////////////
   public static void setFileFilters(JFileChooser fc, List<String> extensions) {
      fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());
      for (int i = 0; i < extensions.size(); ++i) {
         fc.addChoosableFileFilter(new ExtensionFileFilter(extensions.get(i)));
      }
      fc.addChoosableFileFilter(fc.getAcceptAllFileFilter());
   }

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
