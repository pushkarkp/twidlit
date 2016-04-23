/**
 * Copyright 2015 Pushkar Piggott
 *
 * Cfg.java
 */

package pkp.twiddler;

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
import pkp.twiddle.KeyPress;
import pkp.twiddle.KeyPressList;
import pkp.twiddle.Twiddle;
import pkp.io.Io;
import pkp.io.SpacedPairReader;
import pkp.util.Pref;
import pkp.util.Log;

///////////////////////////////////////////////////////////////////////////////
public class Cfg implements Settings {

   ////////////////////////////////////////////////////////////////////////////
   public static Cfg read(File f) {
      Cfg cfg = new Cfg();
      return cfg.read1(f) ? cfg : null;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static Cfg readText(File f) {
      Cfg cfg = new Cfg();
      return cfg.readText1(f) ? cfg : null;
   }

   ////////////////////////////////////////////////////////////////////////////
   public void writeText(File f) {
      Io.write(f, toString());
   }

   ////////////////////////////////////////////////////////////////////////////
   public static void write(File f, Settings tc, ArrayList<Assignment> asgs) {
      int endOfTwiddles = sm_CONFIG_SIZE + (asgs.size() + 1) * 4;
      int startOfMulti = endOfTwiddles + sm_THUMB_SPEC_SIZE;
      byte[] data = new byte[startOfMulti + countMulti(asgs) + 2];
      ByteBuffer bb = ByteBuffer.wrap(data);

      IntSettings is = tc.getIntSettings();
      bb.put((byte)is.MAJOR_VERSION.getValue());
      bb.putShort(otherEndian((short)is.MINOR_VERSION.getValue()));
      bb.putShort(otherEndian((short)endOfTwiddles));
      bb.putShort(otherEndian((short)startOfMulti));
      bb.putShort(otherEndian((short)is.MOUSE_EXIT_DELAY.getValue()));
      bb.putShort(otherEndian((short)is.MS_BETWEEN_TWIDDLES.getValue()));
      bb.put((byte)is.START_SPEED.getValue());
      bb.put((byte)is.FAST_SPEED.getValue());
      bb.put((byte)is.MOUSE_ACCELERATION.getValue());
      bb.put((byte)(is.MS_REPEAT_DELAY.getValue() / 10));
      bb.put((byte)(4 | (tc.isEnableRepeat() ? 1 : 0) | (tc.isEnableStorage() ? 2 : 0)));

      int k = 0;
      for (int i = 0; i < asgs.size(); ++i) {
         Assignment asg = asgs.get(i);
         KeyPressList kpl = asg.getKeyPressList();
         for (int j = 0; j < asg.getTwiddleCount(); ++j) {
            bb.putShort((short)asg.getTwiddle(j).toCfg());
            if (kpl.size() == 1) {
               bb.putShort((short)kpl.get(0).toInt());
            } else {
               bb.put((byte)0xFF);
               bb.put((byte)k);
            }
         }
         if (kpl.size() > 1) {
            ++k;
         }
      }
      bb.putInt(0);

      // mouse assignments here
      byte mouseBytes[] = new byte[] {
      0x08, 0x00, 0x02, 0x04, 0x00, 0x04, 0x02, 0x00, 0x01, (byte)0x80, 0x00, (byte)0x82,
      0x40, 0x00, (byte)0x84, 0x20, 0x00, (byte)0x81, 0x00, 0x08, 0x21, 0x00, 0x04, 0x11,
      0x00, 0x02, 0x41, 0x00, (byte)0x80, (byte)0xA1, 0x00, 0x40, 0x0A, 0x00, 0x20, 0x09,
      0x0, 0x0, 0x0,
      };
      bb.put(mouseBytes);

      for (int i = 0; i < asgs.size(); ++i) {
         KeyPressList kpl = asgs.get(i).getKeyPressList();
         if (kpl.size() > 1) {
            bb.putShort((short)((kpl.size() + 1) << 9));
            for (int j = 0; j < kpl.size(); ++j) {
               bb.putShort((short)kpl.get(j).toInt());
            }
         }
      }
//System.out.printf("bb.position() %d 0x%x%n", (bb.position() - 1), (bb.position() - 1));
      if ((bb.position() - 1) % 4 != 0) {
//System.out.printf("(bb.position() - 1) %% 4 %d%n", (bb.position() - 1) % 4);
         if ((bb.position() - 1) % 2 != 0) {
            Log.warn("Wrote an odd number of bytes");
         }
      }

      FileOutputStream fos = null;
      try {
         fos = new FileOutputStream(f);
      } catch (FileNotFoundException e) {
         Log.warn("failed to open: \"" + f.getPath() + "\".");
			return;
      }
      try {
         fos.write(data, 0, data.length);
         fos.flush();
         fos.close();
      } catch (IOException e) {
         Log.warn("failed to write: \"" + f.getPath() + "\".");
      }
   }

   ////////////////////////////////////////////////////////////////////////////
   public Cfg() {
      m_EnableRepeat = false;
      m_EnableStorage = false;
      m_Assignments = new ArrayList<Assignment>();
   }

   ////////////////////////////////////////////////////////////////////////////
   public Cfg(Settings set, ArrayList<Assignment> asgs) {
      m_IntSettings = set.getIntSettings();
      m_EnableRepeat = isEnableRepeat();
      m_EnableStorage = isEnableStorage();
      m_Assignments = asgs;
   }

   ////////////////////////////////////////////////////////////////////////////
   public void write(File f) {
      write(f, this, getAssignments());
   }

   ////////////////////////////////////////////////////////////////////////////
   public boolean hasAssignments() {
      return m_Assignments != null && m_Assignments.size() > 0;
   }

   ////////////////////////////////////////////////////////////////////////////
   @Override // Settings
   public IntSettings getIntSettings() { return m_IntSettings; }

   ////////////////////////////////////////////////////////////////////////////
   @Override // Settings
   public boolean isEnableRepeat() { return m_EnableRepeat; }
   @Override // Settings
   public boolean isEnableStorage() { return m_EnableStorage; }

   ////////////////////////////////////////////////////////////////////////////
   public ArrayList<Assignment> getAssignments() {
      return m_Assignments;
   }

   ////////////////////////////////////////////////////////////////////////////
   public String toString() {
      String str = Assignment.toString(m_Assignments, KeyPress.Format.FILE);
      for (IntSettings is: getIntSettings().values()) {
         if (!is.isDefault()) {
            str += Io.toCamel(is.m_Name) + " " + is.getValue() + '\n';
         }
      }
      if (isEnableRepeat()) {
         str += String.format("%s %s",
                              Io.toCamel(Settings.sm_ENABLE_REPEAT_NAME),
                              Boolean.toString(isEnableRepeat())) + '\n';
      }
      if (isEnableStorage()) {
         str += String.format("%s %s",
                              Io.toCamel(Settings.sm_ENABLE_STORAGE_NAME),
                              Boolean.toString(isEnableStorage())) + '\n';
      }
      return str;
   }

   // Private /////////////////////////////////////////////////////////////////

   ////////////////////////////////////////////////////////////////////////////
   private boolean read1(File inputFile) {
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
         Log.warn("Failed to read \"" + inputFile.getPath() + "\" " + e);
         return false;
      }
      ByteBuffer bb = ByteBuffer.wrap(data);

      m_IntSettings.MAJOR_VERSION.setValue(bb.get() & 0xFF);
      m_IntSettings.MINOR_VERSION.setValue(otherEndian(bb.getShort()));
      int endOfTwiddles = otherEndian(bb.getShort());
      int startOfMulti = otherEndian(bb.getShort());
//System.out.printf("endOfTwiddles %d 0x%x startOfMulti %d 0x%x sum %d 0x%x diff %d 0x%x%n",
//                  endOfTwiddles, endOfTwiddles, startOfMulti, startOfMulti,
//                  endOfTwiddles + startOfMulti, endOfTwiddles + startOfMulti,
//                  endOfTwiddles - startOfMulti, endOfTwiddles - startOfMulti);
      m_IntSettings.MOUSE_EXIT_DELAY.setValue(otherEndian(bb.getShort()));
      m_IntSettings.MS_BETWEEN_TWIDDLES.setValue((int)otherEndian(bb.getShort()));
      m_IntSettings.START_SPEED.setValue(bb.get() & 0xFF);
      m_IntSettings.FAST_SPEED.setValue(bb.get() & 0xFF);
      m_IntSettings.MOUSE_ACCELERATION.setValue(bb.get() & 0xFF);
      m_IntSettings.MS_REPEAT_DELAY.setValue((bb.get() & 0xFF) * 10);
      int bits = bb.get() & 0xFF;
      m_EnableRepeat = (bits & 1) != 0;
      m_EnableStorage = (bits & 2) != 0;

      KeyPress.clearWarned();
      m_Assignments = new ArrayList<Assignment>();
      ArrayList<Twiddle> multi = new ArrayList<Twiddle>();
      ArrayList<Integer> whichKpl = new ArrayList<Integer>();
      for (;;) {
         if (bb.remaining() < 2) {
            Log.err("Cfg file is corrupt.");
         }
         short t = bb.getShort();
         short k = bb.getShort();
//System.out.printf("t %d k %d%n", t, k);
         if (t == 0 && k == 0) {
            break;
         }
         if (t == 0 || k == 0) {
            Log.err(String.format("Cfg file format error: twiddle 0x%x key 0x%x%n", t, k));
         }
         Twiddle tw = new Twiddle(toChord(t), toThumbKeys(t));
         if (k >= 0) {
            KeyPress kp = KeyPress.fromKeyCode(k);
            if (!kp.isValid()) {
               Log.warn(String.format("Found invalid key code %d", k));
               continue;
            }
            KeyPressList kpl = new KeyPressList(kp);
            m_Assignments.add(new Assignment(tw, kpl));
//System.out.printf("1 %s (t 0x%x) %s (k 0x%x)\n", tw.toString(), t, kpl.toString(), k);
         } else {
            multi.add(tw);
            whichKpl.add(k & 0xFF);
//System.out.printf("2 Multi: %s (t 0x%x) (k 0x%x)\n", tw.toString(), t, k & 0xFF);
         }
      }
      // mouse assignments
      for (;;) {
         if (bb.remaining() < 2) {
            Log.err("Cfg file is corrupt.");
         }
         short t = bb.getShort();
         byte k = bb.get();
         if (t == 0 && k == 0) {
            break;
         }
         if (t == 0 || k == 0) {
            Log.err(String.format("\"%s\" has a format error: %x %x.", inputFile.getPath(), t, k));
         }
         Twiddle tw = new Twiddle(toChord(t), toThumbKeys(t));
//System.out.printf("3 Mouse: %s (t 0x%x) (k 0x%x)\n", tw.toString(), t, k);
      }
      ArrayList<KeyPressList> kpls = new ArrayList<KeyPressList>();
      for (;;) {
         if (bb.remaining() < 2) {
            Log.err("Cfg file is corrupt.");
         }
         short s = bb.getShort();
         if (s == 0) {
            break;
         }
         KeyPressList kpl = new KeyPressList();
         int len = ((s >> 9) & 0xFF) - 1;
         if (bb.remaining() < len * 2) {
            Log.err("Cfg file is corrupt.");
         }
//System.out.printf("4 s 0x%x: len %d%n", s, len);
         for (int i = 0; i < len; ++i) {
            short k = bb.getShort();
            KeyPress kp = KeyPress.fromKeyCode(k);
//System.out.printf("4 %d: %s (k 0x%x)%n", i, kp.toTagString(), k);
            kpl.add(kp);
         }
         kpls.add(kpl);
      }
      for (int i = 0; i < multi.size(); ++i) {
         Assignment asg = new Assignment(multi.get(i), kpls.get(whichKpl.get(i)));
         m_Assignments.add(asg);
//System.out.println("5 " + asg);
      }
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean readText1(File f) {
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
      SpacedPairReader spr = new SpacedPairReader(url, Io.sm_MUST_EXIST);
      boolean readSettings = false;

      m_Assignments = new ArrayList<Assignment>();
      String line;
      StringBuilder err = new StringBuilder();
      for (int i = 1; (line = spr.getNextLine()) != null; ++i) {
         Assignment asg = Assignment.parseLine(line, err);
         if (asg != null && asg.getKeyPressList().isValid()) {
            m_Assignments.add(asg);
         } else if (asg != null || readSettings
                 || !(readSettings = readTextSettings(spr))) {
            Log.parseWarn(spr, err.toString(), line);
            err = new StringBuilder();
         }
      }
      spr.close();
      return true;
   }

   ////////////////////////////////////////////////////////////////////////////
   private boolean readTextSettings(SpacedPairReader spr) {
      boolean found = true;
      for (IntSettings is: m_IntSettings.values()) {
         if (!spr.isMatchFirst(Io.toCamel(is.m_Name), "Cfg")) {
            if (is == m_IntSettings.MAJOR_VERSION) {
               found = false;
               break;
            } else {
               Log.warn("Failed to read " + is);
               //return;
            }
         }
         int value = Integer.parseInt(spr.getNextSecond());
         is.setValue(value);
      }

      if (found) {
         if (spr.isMatchFirst(Io.toCamel(Settings.sm_ENABLE_REPEAT_NAME), "Cfg")) {
            m_EnableRepeat = spr.getNextSecond().equals("true");
         } else {
            Log.warn("Failed to read " + Settings.sm_ENABLE_REPEAT_NAME);
            return found;
         }
         if (spr.isMatchFirst(Io.toCamel(Settings.sm_ENABLE_STORAGE_NAME), "Cfg")) {
            m_EnableStorage = spr.getNextSecond().equals("true");
         } else {
            Log.warn("Failed to read " + Settings.sm_ENABLE_STORAGE_NAME);
            return found;
         }
         spr.getNextLine();
      }
      return found;
   }

   ////////////////////////////////////////////////////////////////////////////
   private static int countMulti(ArrayList<Assignment> asgs) {
      int size = 0;
      for (int i = 0; i < asgs.size(); ++i) {
         KeyPressList kpl = asgs.get(i).getKeyPressList();
         if (kpl.size() > 1) {
            size += (kpl.size() + 1) * 2;
         }
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
   private static final int sm_CONFIG_SIZE = 16;
   private static final int sm_THUMB_SPEC_SIZE = 39;
   private IntSettings m_IntSettings;
   private boolean m_EnableRepeat = false;
   private boolean m_EnableStorage = false;
   private ArrayList<Assignment> m_Assignments = new ArrayList<Assignment>();
}
