/**
 * Copyright 2015 Pushkar Piggott
 *
 * Cfg.java
 */

package pkp.twiddler;

import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import pkp.twiddle.Assignment;
import pkp.twiddle.Assignments;
import pkp.twiddle.KeyPress;
import pkp.twiddle.KeyPressList;
import pkp.twiddle.Twiddle;
import pkp.twiddle.Chord;
import pkp.io.Io;
import pkp.io.LineReader;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class Cfg implements Settings {

   ////////////////////////////////////////////////////////////////////////////
   public static Cfg read(File f) {
      String path = f.getPath().toLowerCase();
      Cfg cfg = new Cfg();
      if (path.endsWith(".cfg")) {
         return cfg.readBin(f) ? cfg : null;
      } else {
         return cfg.readText(f) ? cfg : null;
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void write(File f, Assignments asgs, Settings settings, int version) {
//System.out.printf("version %d, format %d%n", version, settings.getIntSettings().FORMAT_VERSION.getValue());
      if (version < 1 || version > 3) {
         Log.err(String.format("Version (%d) out of range.", version));
      }
      byte[] data = (version == 3) 
                  ? write5(f, asgs, settings)
                  : write4(f, asgs, settings, version);
      FileOutputStream fos = null;
      try {
         fos = new FileOutputStream(f);
      } catch (FileNotFoundException e) {
         Log.warn("Failed to open: \"" + f.getPath() + "\".");
			return;
      }
      try {
         fos.write(data, 0, data.length);
         fos.flush();
         fos.close();
      } catch (IOException e) {
         Log.warn("Failed to write: \"" + f.getPath() + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public Cfg() {
      m_Assignments = new Assignments();
   }

   ////////////////////////////////////////////////////////////////////////////
   public Cfg(Settings set, Assignments asgs) {
      m_IntSettings = set.getIntSettings();
      m_BoolSettings = set.getBoolSettings();
      m_Version = set.getVersion();
      m_Assignments = asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public void write(File f, int version) {
      write(f, getAssignments(), this, version);
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean hasAssignments() {
      return m_Assignments != null && m_Assignments.size() > 0;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // Settings
   public IntSettings getIntSettings() { return m_IntSettings; }
   @Override // Settings
   public BoolSettings getBoolSettings() { return m_BoolSettings; }
   @Override // Settings
   public int getVersion() { return m_Version; }


   ////////////////////////////////////////////////////////////////////////////
   public Assignments getAssignments() {
      return m_Assignments;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      String str = "";
      for (IntSettings is: getIntSettings().values()) {
         if (is.isGuiItem(m_Version) && !is.isDefault()) {
            str += Io.toCamel(is.getName()) + " " + is.getValue() + '\n';
         }
      }
      for (BoolSettings bs: getBoolSettings().values()) {
         if (!bs.isDefault() && bs.isCurrent(m_Version)) {
            str += Io.toCamel(bs.getName()) + " " + bs.is() + '\n';
         }
      }
      return str + m_Assignments.toString();
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private static byte[] write5(File f, Assignments a, Settings s) {
      List<Assignment> mouseKeys = a.getSortedMouseKeys();
      List<Assignment> asgs = a.to121List();
      IntSettings is = s.getIntSettings();
      // fixed header plus assignments
      int endOfTwiddles = 16 + asgs.size() * 4;
      int sizeOfMulti = countMulti(mouseKeys) + countMulti(asgs);
      if (sizeOfMulti > 0) {
         sizeOfMulti += 4;
      }
      byte[] data = new byte[endOfTwiddles + sizeOfMulti];
      ByteBuffer bb = ByteBuffer.wrap(data);

      bb.put((byte)is.FORMAT_VERSION.getValue());
      bb.put((byte)(s.getBoolSettings().toBits(3)));
      bb.putShort(otherEndian((short)asgs.size()));
      bb.putShort(otherEndian((short)is.IDLE_LIMIT.getValue()));

      int multiCount = writeMouseKeys(mouseKeys, bb);

      bb.put((byte)is.MOUSE_SENSITIVITY.getValue());
      bb.put((byte)(is.KEY_REPEAT_DELAY.getValue() / 10));
      bb.putShort((short)0);

      writeAssignments(multiCount, asgs, bb);
      if (sizeOfMulti > 0) {
         bb.putInt((int)0);
         writeMultikeyTable(mouseKeys, bb);
         writeMultikeyTable(asgs, bb);
      }

      return data;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static byte[] write4(File f, Assignments a, Settings s, int version) {
      IntSettings is = s.getIntSettings();
      List<Assignment> asgs = a.to121List();
      // assignments plus 1 terminating 0 int
      int endOfTwiddles = is.CONFIG_SIZE.getValue() + (asgs.size() + 1) * 4;
      int startOfMulti = endOfTwiddles + sm_MOUSE_SPEC_SIZE;
      // plus 1 table-terminating 0 short
      byte[] data = new byte[startOfMulti + countMulti(asgs) + 2];
      ByteBuffer bb = ByteBuffer.wrap(data);

      bb.put((byte)is.FORMAT_VERSION.getValue());
      bb.putShort(otherEndian((short)is.CONFIG_SIZE.getValue()));
      bb.putShort(otherEndian((short)endOfTwiddles));
      bb.putShort(otherEndian((short)startOfMulti));

      // int settings
     if (version == 1) {
         bb.putShort(otherEndian((short)is.MOUSE_EXIT_DELAY.getValue()));
      } else {
         bb.putShort(otherEndian((short)is.IDLE_LIMIT.getValue()));
      }
      bb.putShort(otherEndian((short)is.MS_BETWEEN_TWIDDLES.getValue()));
      bb.put((byte)is.START_SPEED.getValue());
      bb.put((byte)is.FAST_SPEED.getValue());
      bb.put((byte)is.MOUSE_SENSITIVITY.getValue());
      bb.put((byte)(is.KEY_REPEAT_DELAY.getValue() / 10));
      
      bb.put((byte)(4 | s.getBoolSettings().toBits(version)));
      writeAssignments(0, asgs, bb);
      // terminating 0 int
      bb.putInt(0);

      // mouse assignments
      byte mouseBytes[] = new byte[] {
         0x08, 0x00, 0x02, 0x04, 0x00, 0x04, 0x02, 0x00, 0x01, (byte)0x80, 0x00, (byte)0x82,
         0x40, 0x00, (byte)0x84, 0x20, 0x00, (byte)0x81, 0x00, 0x08, 0x21, 0x00, 0x04, 0x11,
         0x00, 0x02, 0x41, 0x00, (byte)0x80, (byte)0xA1, 0x00, 0x40, 0x0A, 0x00, 0x20, 0x09,
         0x0, 0x0, 0x0,
      };
      bb.put(mouseBytes);

      writeMultikeyTable(asgs, bb);
      // table-terminating 0 short
      bb.putShort((short)0);

      return data;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static void writeAssignments(int multiCount, List<Assignment> asgs, ByteBuffer bb) {
      for (int i = 0; i < asgs.size(); ++i) {
         int newMultiCount = multiCount;
         Assignment asg = asgs.get(i);
         KeyPressList kpl = asg.getKeyPressList();
         for (int j = 0; j < asg.getTwiddleCount(); ++j) {
            bb.putShort((short)asg.getTwiddle(j).toCfg());
            newMultiCount = writeKeyPressList(multiCount, kpl, bb);
         }
         multiCount = newMultiCount;
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   // Writes and increments the count (index) of multiKey sets 
   // only if there is more than one key.
   private static int writeKeyPressList(int multiCount, KeyPressList kpl, ByteBuffer bb) {
      if (kpl.size() == 1) {
         bb.putShort((short)kpl.get(0).toInt());
         return multiCount;
      }
      bb.put((byte)0xFF);
      bb.put((byte)multiCount);
      return multiCount + 1;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static void writeMultikeyTable(List<Assignment> asgs, ByteBuffer bb) {
      for (Assignment asg : asgs) {//int i = 0; i < asgs.size(); ++i) {
         writeMultikey(asg.getKeyPressList(), bb);
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private static void writeMultikey(KeyPressList kpl, ByteBuffer bb) {
      if (kpl.size() > 1) {
         bb.putShort((short)((kpl.size() + 1) << 9));
         for (int j = 0; j < kpl.size(); ++j) {
            bb.putShort((short)kpl.get(j).toInt());
         }
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean readBin(File inputFile) {
      if (inputFile == null) {
         Log.warn("No cfg file specified");
         return false;
      }
      byte[] data = new byte[(int)inputFile.length()];
      FileInputStream fis = null;
      try {
         fis = new FileInputStream(inputFile);
      } catch (FileNotFoundException e) {
         return false;
      }
      try {
         fis.read(data, 0, data.length);
         fis.close();
      } catch (IOException e) {
         Log.warn("Failed to read " + inputFile.getPath() + " " + e);
         return false;
      }
      ByteBuffer bb = ByteBuffer.wrap(data);
      m_IntSettings.FORMAT_VERSION.setValue(bb.get() & 0xFF);
      return (m_IntSettings.FORMAT_VERSION.getValue() == 4)
         ? read4(bb, inputFile.getPath())
         : read5(bb, inputFile.getPath());
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean read5(ByteBuffer bb, String path) {
      int bits = bb.get();
      m_BoolSettings.setFromBits(bits);

      int chordMaps = otherEndian(bb.getShort()) & 0xFFFF;
      m_IntSettings.IDLE_LIMIT.setValue(otherEndian(bb.getShort()) & 0xFFFF);

      KeyPress.clearWarned();
      m_Assignments = new Assignments();
      ArrayList<Twiddle> multi = new ArrayList<Twiddle>();
      ArrayList<Integer> whichKpl = new ArrayList<Integer>();
      for (int i = 0; i < Chord.sm_COLUMNS; ++i) {
         int k = bb.getShort();
         if (k != 0) {
            int c = i + 1 << Chord.sm_MOUSE * 2;
            readKeyMap(c, 0, k, path, multi, whichKpl);
         }
      }

      m_IntSettings.MOUSE_SENSITIVITY.setValue(bb.get() & 0xFF);
      m_IntSettings.KEY_REPEAT_DELAY.setValue((bb.get() & 0xFF) * 10);
      otherEndian(bb.getShort());

      for (int i = 0; i < chordMaps; ++i) {
         if (bb.remaining() < 4) {
            Log.err("Cfg file " + path + " is corrupt.");
         }
         int b = bb.getShort();
         int c = toChord(b);
         int t = toThumbKeys(b);
         int k = bb.getShort();
         if (!readKeyMap(c, t, k, path, multi, whichKpl)) {
            Log.err(String.format("Format error: twiddle 0 key 0 in %s.", path));
         }
      }

      if (multi.size() > 0) {
         bb.getInt();
      }
      ArrayList<KeyPressList> kpls = new ArrayList<KeyPressList>();
      for (int i = 0; i < multi.size(); ++i) {
         KeyPressList kpl = readMultiKeys(bb, path);
         if (kpl == null) {
            Log.err("Cfg file " + path + " is corrupt." + String.format(" %d:%d", i, multi.size()));
         }
         kpls.add(kpl);
      }

      for (int i = 0; i < multi.size(); ++i) {
         m_Assignments.add(new Assignment(multi.get(i), kpls.get(whichKpl.get(i))));
      }
      if (m_Assignments.isRemap()) {
         Log.warn(m_Assignments.reportRemap(path));
      }
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean read4(ByteBuffer bb, String path) {
      m_IntSettings.CONFIG_SIZE.setValue(otherEndian(bb.getShort()) & 0xFFFF);
      int endOfTwiddles = otherEndian(bb.getShort()) & 0xFFFF;
      int startOfMulti = otherEndian(bb.getShort()) & 0xFFFF;

      m_IntSettings.MOUSE_EXIT_DELAY.setValue(otherEndian(bb.getShort()) & 0xFFFF);
      m_IntSettings.IDLE_LIMIT.setValue(m_IntSettings.MOUSE_EXIT_DELAY.getValue());
      m_IntSettings.MS_BETWEEN_TWIDDLES.setValue(otherEndian(bb.getShort()) & 0xFFFF);
      m_IntSettings.START_SPEED.setValue(bb.get() & 0xFF);
      m_IntSettings.FAST_SPEED.setValue(bb.get() & 0xFF);
      m_IntSettings.MOUSE_SENSITIVITY.setValue(bb.get() & 0xFF);
      m_IntSettings.KEY_REPEAT_DELAY.setValue((bb.get() & 0xFF) * 10);

      int bits = bb.get();
      m_BoolSettings.setFromBits(bits);

      KeyPress.clearWarned();
      m_Assignments = new Assignments();
      ArrayList<Twiddle> multi = new ArrayList<Twiddle>();
      ArrayList<Integer> whichKpl = new ArrayList<Integer>();
      int c = 0;
      int t = 0;
      int k = 0;
      do {
         if (bb.remaining() < 4) {
            Log.err("Cfg file " + path + " is corrupt.");
         }
         int b = bb.getShort();
         c = toChord(b);
         t = toThumbKeys(b);
         k = bb.getShort();
      } while (readKeyMap(c, t, k, path, multi, whichKpl));

      // mouse assignments
      for (;;) {
         if (bb.remaining() < 2) {
            Log.err("Cfg file " + path + " is corrupt.");
         }
         t = bb.getShort();
         k = bb.get();
         if (t == 0 && k == 0) {
            break;
         }
         if (t == 0 || k == 0) {
            Log.err(String.format("Format error: twiddle 0x%x key 0x%x at %d in %s.", t, k, bb.remaining() - 2, path));
         }
         Twiddle tw = new Twiddle(toChord(t), toThumbKeys(t));
      }
      ArrayList<KeyPressList> kpls = new ArrayList<KeyPressList>();
      for (;;) {
         if (bb.remaining() < 2) {
            Log.err("Cfg file " + path + " is corrupt.");
         }
         KeyPressList kpl = readMultiKeys(bb, path);
         if (kpl == null) {
            break;
         }
         kpls.add(kpl);
      }
      for (int i = 0; i < multi.size(); ++i) {
         m_Assignments.add(new Assignment(multi.get(i), kpls.get(whichKpl.get(i))));
      }
      if (m_Assignments.isRemap()) {
         Log.warn(m_Assignments.reportRemap(path));
      }
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean readKeyMap(int c, int t, int k, String path, ArrayList<Twiddle> multi, ArrayList<Integer> whichKpl) {
      if (c == 0 && t == 0 && k == 0) {
         return false;
      }
      if ((c == 0 && t == 0) || k == 0) {
         Log.err(String.format("Format error: twiddle 0x%x key 0x%x in %s.", t, k, path));
      }
      Twiddle tw = new Twiddle(c, t);
      if (k < 0) {
         multi.add(tw);
         whichKpl.add(k & 0xFF);
         return true;
      }
      KeyPress kp = KeyPress.fromKeyCode(k);
      if (kp.isValid()) {
         KeyPressList kpl = new KeyPressList(kp);
         m_Assignments.add(new Assignment(tw, kpl));
         return true;
      }
      Log.warn(String.format("Found invalid key code 0x%x in %s.", k, path));
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static KeyPressList readMultiKeys(ByteBuffer bb, String path) {
      int s = bb.getShort();
      if (s == 0) {
         return null;
      }
      KeyPressList kpl = new KeyPressList();
      int len = ((s >> 9) & 0xFF) - 1;
      if (bb.remaining() < len * 2) {
         Log.err("Cfg file " + path + " is corrupt." + String.format(" remaining %d len * 2 %d", bb.remaining(), len * 2));
      }
      for (int i = 0; i < len; ++i) {
         int k = bb.getShort();
         KeyPress kp = KeyPress.fromKeyCode(k);
         kpl.add(kp);
      }
      return kpl;
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean readText(File f) {
      URL url = null;
      if (f != null && f.exists() && !f.isDirectory()) {
         try {
            url = f.toURI().toURL();
         } catch (MalformedURLException e) {
            Log.warn("Failed to create URL from \"" + f.getPath() + "\".");
         }
      }
      if (url == null) {
         return false;
      }
      LineReader lr = new LineReader(url, Io.sm_MUST_EXIST);
      m_Assignments = new Assignments();
      String line;
      StringBuilder err = new StringBuilder();
      for (int i = 1; (line = lr.readLine()) != null; ++i) {
         Assignment asg = Assignment.parseLine(line, err);
         if (asg != null && asg.getKeyPressList().isValid()) {
            m_Assignments.add(asg);
         } else if (!readTextSetting(line)) {
            Log.parseWarn(lr, err.toString(), line);
            err = new StringBuilder();
         }
      }
      lr.close();
      if (m_Assignments.isRemap()) {
         Log.warn(m_Assignments.reportRemap(url.getPath()));
      }
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean readTextSetting(String line) {
      List<String> strs = Io.split(line, ' ');
      if (strs.size() != 2) {
         return false;
      }
      String upper = strs.get(0).toUpperCase();
      for (IntSettings is: m_IntSettings.values()) {
         if (upper.equals(Io.toCamel(is.getName()).toUpperCase())) {
            int value = Io.toInt(strs.get(1)); 
            if (value == Io.sm_PARSE_FAILED) {
               return false;
            }
            is.setValue(value);
            return true;
         }
      }
      for (BoolSettings bs: m_BoolSettings.values()) {
         if (upper.equals(Io.toCamel(bs.getName()).toUpperCase())) {
            if (!Io.isBool(strs.get(1))) {
               return false;
            }
            bs.setValue(Io.parseBool(strs.get(1)));
            return true;
         }
      }
      return false;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static int writeMouseKeys(List<Assignment> asgs, ByteBuffer bb) {
      int multiCount = 0;
      for (Assignment a : asgs) {
         if (a.getKeyPressList().size() == 0) {
            bb.putShort(otherEndian((short)0));
         } else {
            multiCount = writeKeyPressList(multiCount, a.getKeyPressList(), bb);
         }
      }
      return multiCount;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static int countMulti(List<Assignment> asgs) {
      int size = 0;
      for (Assignment a : asgs) {
         size += (a.getKeyPressList().size() > 1)
               ? (a.getKeyPressList().size() + 1) * 2
               : 0;
      }
      return size;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static short otherEndian(short s) {
      return (short)((s >> 8 & 0xFF) | (s << 8 & 0xFF00));
   }

   /////////////////////////////////////////////////////////////////////////////
   private static int toChord(int cfg) {
      cfg &= 0xFFFF;
      // swap nibbles
      cfg = (cfg & 0xF0F0) >> 4 | (cfg & 0x0F0F) << 4;
      int chord = 0;
      for (int i = 0; i < 4; ++i) {
         chord <<= 2;
         for (int j = 1; j < 4; ++j) {
            if ((cfg & (1 << j)) != 0) {
               chord |= j;
            }
         }
         cfg >>= 4;
      }
      return chord;
   }

   /////////////////////////////////////////////////////////////////////////////
   private static int toThumbKeys(int cfg) {
      cfg &= 0xFFFF;
      cfg = (cfg & 0xF0) >> 4 | (cfg & 0xF) << 4 | (cfg & 0xFF00);
      int value = 0;
      for (int i = 0; i < 4; ++i) {
         if ((cfg & 1) != 0) {
            value |= 1 << i;
         }
         cfg >>= 4;
      }
      return value;
   }

   // Data ////////////////////////////////////////////////////////////////////
   private static final int sm_MOUSE_SPEC_SIZE = 39;
   private IntSettings m_IntSettings;
   private BoolSettings m_BoolSettings;
   private int m_Version;
   private Assignments m_Assignments = new Assignments();
}
