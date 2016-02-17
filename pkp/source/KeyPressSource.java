/**
 * Copyright 2016 Pushkar Piggott
 *
 * KeyPressSource.java
 *
 * A wrapper on a UniformSource to return KeyPressLists
 * representing keystrokes read from file.
 */

package pkp.source;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import pkp.twiddle.KeyPress;
import pkp.twiddle.KeyPressList;
import pkp.io.LineReader;
import pkp.io.Io;
import pkp.util.Pref;

////////////////////////////////////////////////////////////////////////////////
public class KeyPressSource implements KeyPressListSource {

   /////////////////////////////////////////////////////////////////////////////
   public KeyPressSource(File f) {
      m_File = f;
      ArrayList<ArrayList<KeyPressList>> keys = new ArrayList<ArrayList<KeyPressList>>();
      if (m_File == null || !m_File.exists()) {
         keys.add(getDefault());
      } else {
         LineReader lr = new LineReader(Io.toExistUrl(m_File), Io.sm_MUST_EXIST);
//new KeyPressList(KeyPress.fromKeyCode(
         keys.add(getKeys(lr));
         lr.close();
      }
      m_UniformSource = new UniformSource<KeyPressList>(keys, 4);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   @Override // KeyPressListSource
   public KeyPressListSource clone() { return new KeyPressSource(m_File); }
   @Override // KeyPressListSource
   public String getName() { return "RandomChords:"; }
   @Override // KeyPressListSource
   public String getFullName() { return getName(); }
   @Override // KeyPressListSource
   public KeyPressListSource getSource() { return null;  }
   @Override // KeyPressListSource
   public void close() {}

   /////////////////////////////////////////////////////////////////////////////
   @Override // KeyPressListSource
   public KeyPressList getNext() {
      return m_UniformSource.get();
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // KeyPressListSource
   public KeyPressListSource.Message send(KeyPressListSource.Message m) {
      m_UniformSource.next(m != null);
      return null;
   }

   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   // upper and lower case alphabetics by default
   private ArrayList<KeyPressList> getDefault() {
      byte[] str = new byte[2 * ('z' - 'a' + 1)];
      int k = 0;
      for (int i = 'A'; i <= 'a'; i += 'a' - 'A') {
         for (int j = 0; j <= 'z' - 'a'; ++j) {
            str[k++] = (byte)(i + j);
         }
      }
      ArrayList<KeyPressList> al = new ArrayList<KeyPressList>();
      add(al, new String(str), 1);
      return al;
   }

   /////////////////////////////////////////////////////////////////////////////
   private ArrayList<KeyPressList> getKeys(LineReader lr) {
      ArrayList<KeyPressList> al = new ArrayList<KeyPressList>();
      String line;
      while ((line = lr.readLine()) != null) {
         int times = 1;
         int at = line.indexOf(':');
         if (at != -1) {
            int count = getInt(line.substring(0, at));
            if (count > 0) {
               times = count;
               line = line.substring(at + 1);
            }
         }
         add(al, line, times);
      }
      return al;
   }

   /////////////////////////////////////////////////////////////////////////////
   private void add(ArrayList<KeyPressList> al, String str, int times) {
      ArrayList<KeyPressList> kpls = new ArrayList<KeyPressList>();
      List<String> strs = Io.split(str, ' ');
      for (int i = 0; i < strs.size(); ++i) {
         KeyPressList kpl = KeyPressList.parseTextAndTags(strs.get(i));
         if (kpl.isValid()) {
            kpls.add(kpl);
         }
      }
      for (int i = 0; i < times; ++i) {
         for (int j = 0; j < kpls.size(); ++j) {
            al.add(kpls.get(j));
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private int getInt(String str) {
      int parsed = Io.toInt(str);
      if (parsed == Io.sm_PARSE_FAILED || parsed < 1) {
         return 0;
      }
      return parsed;
   }

   // Data /////////////////////////////////////////////////////////////////////
   private File m_File;
   private UniformSource<KeyPressList> m_UniformSource;
}
