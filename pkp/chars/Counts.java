/**
 * Copyright 2015 Pushkar Piggott
 *
 * Counts.java
 */
package pkp.chars;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import pkp.io.Io;
import pkp.twiddle.KeyPress;
import pkp.lookup.SharedIndex;
import pkp.lookup.SharedIndexableInts;
import pkp.ui.ProgressWindow;
import pkp.util.Log;

public class Counts {
   
   private static final int PAGE_WIDTH = 78;
   
   private CharCounts charCounts;
   private NGrams nGrams;
   private File file;
   private SharedIndex index;
   private Bounds bounds;
   private boolean showBigrams;

   public Counts(File f, Bounds b) {
      file = f;
      charCounts = null;
      nGrams = null;
      index = null;
      bounds = b;
      showBigrams = false;
   }
   
   public Counts(Counts other) {
      file = other.file;
      charCounts = other.charCounts;
      nGrams = other.nGrams;
      index = other.index;
      bounds = other.getBounds();
      showBigrams = other.showBigrams;
   }
   
   public void setBounds(Bounds b){
       bounds = b;
   }

   public boolean setShowBigrams(boolean set) {
      if (set == showBigrams) {
         return false;
      }
      showBigrams = set;
      charCounts = null;
      nGrams = null;
      return true;
   }

   public boolean setShowNGrams(File f) {
      if (f == file) { 
         return false;
      }
      charCounts = null;
      file = f;
      nGrams = null;
      return true;
   }

   
   public static int getProgressCount() {
      return 10;
   }

   public void count(File f) {
      if (f == null) {
         return;
      }
      if (f.isFile()) {
         countFile(f);
      } else if (f.isDirectory()) {
         File[] files = f.listFiles();
         if (files != null) {
            for (File file : files) {
               if (!file.isDirectory()) {
                  countFile(file);
               }
            }
         }
      }
   }

    public String graph(ProgressWindow pw) {
        if (index == null) {
            index = createIndex();
        }
        return generateOutput(pw, false);
    }

   public String table(ProgressWindow pw) {
      if (index == null) {
         index = createIndex();
      }
      return generateOutput(pw, true);
   }
   
   private String generateOutput(ProgressWindow pw, boolean isTable) {
      StringBuilder output = new StringBuilder();
      pw.step();
      int labelSize = getLabelSize();
      String pad = " ".repeat(labelSize);
      int progressStep = Math.max(1, index.getSize() / (getProgressCount() - 1));
      
      for (int i = 0; i < index.getSize(); ++i) {
         if (i % progressStep == progressStep - 1) {
            pw.step();
         }
         
         String label = index.getLabel(i);
         int value = index.getValue(i);
         double percent = index.getPercent(i);
         
         output.append(pad.substring(0, pad.length() - label.length())).append(label);
         output.append(' ').append(value);
         
         if (isTable) {
            output.append(' ').append(String.format("%5.2f", percent));
         }
         
         output.append('\n');
      }
      return output.toString();
   }

   private void countFile(File f) {
      byte[] data = new byte[(int)f.length()];
      FileInputStream fis = null;
      try {
         fis = new FileInputStream(f);
      } catch (FileNotFoundException e) {
         Log.err("Failed to open \"" + f.getPath() + '"');
         return;
      }
      try {
         fis.read(data, 0, data.length);
         fis.close();
      } catch (IOException e) {
         Log.err("Failed to read \"" + f.getPath() + '"');
         return;
      }
      if (m_CharCounts == null) {
         m_CharCounts = new CharCounts(m_ShowBigrams);
      }
      boolean ignoredSome = false;
      boolean[] ignored = new boolean[128];
      ByteBuffer bb = ByteBuffer.wrap(data);
      while (bb.hasRemaining()) {
         int cin = bb.get() & 0xFF;
         if (cin >= 128) {
            ignoredSome = true;
            ignored[cin - 128] = true;
            continue;
         }
         m_CharCounts.nextChar((char)cin);
         if (getNGrams() != null) {
            getNGrams().nextChar((char)cin);
         }
      }
      if (ignoredSome) {
         String ig = "";
         for (int i = 0; i < ignored.length; ++i) {
            if (ignored[i]) {
               ig += String.format(" 0x%x", i + 128);
            }
         }
         Log.log("Count ignored the following bytes in " + f.getName() + ':' + ig);
      }
	}
   
    private SharedIndex createIndex() {
      ArrayList<SharedIndexableInts> sic = new ArrayList<SharedIndexableInts>();
      if (m_CharCounts != null) {
         sic.add(m_CharCounts);
         if (m_CharCounts.hasBigramCounts()) {
            sic.add(m_CharCounts.new BigramCounts());
         }
         if (getNGrams() != null) {
            sic.add(getNGrams());
         }
      }
      return SharedIndex.create(sic, m_HighestCount, m_LowestCount);
   }
   
   private int getLabelSize() {
      int labelSize = m_ShowBigrams ? 4 : 2;
      if (getNGrams() == null) {
         return labelSize;
      }
      return Math.max(labelSize, getNGrams().findMaxLength(m_LowestCount, m_HighestCount));
   }
   
   private NGrams getNGrams() {
      if (m_NGrams == null) {
         if (m_File == null) {
            return null;
         }
         m_NGrams = new NGrams(m_File);
      }
      return m_NGrams;
   }
}
