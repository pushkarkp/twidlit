/**
 * Copyright 2015 Pushkar Piggott
 *
 * Settings.java
 */

package pkp.twiddler;

///////////////////////////////////////////////////////////////////////////////
public interface Settings {

   public IntSettings getIntSettings();
   public BoolSettings getBoolSettings();
   public int getVersion();

   ////////////////////////////////////////////////////////////////////////////
   public enum IntSettings {
      FORMAT_VERSION("Format Version", "", 0, 4, 5, 1, -1),
      CONFIG_SIZE("Chord-map offset", "", 16, 0, 0x7FFF, 1, 3),
      MOUSE_EXIT_DELAY("Mouse mode exit delay", " (ms)", 1500, 0, 5000, 1000, 1),
      MS_BETWEEN_TWIDDLES("Faster mouse threshold", " (ms)", 383, 0, 5000, 1000, 1),
      START_SPEED("Starting mouse speed", "", 3, 0, 10, 2, 1),
      FAST_SPEED("Fast mouse speed", "", 6, 0, 10, 2, 1),
      MOUSE_SENSITIVITY("Mouse sensitivity", "", 128, 0, 255, 50, 7),
      KEY_REPEAT_DELAY("Key repeat delay", " (ms)", 1000, 0, 2500, 500, 7),
      IDLE_LIMIT("Idle limit", " (s)", 1500, 0, 60000, 12000, 6);

      public String toString() {
         return m_Name + " " + m_Value;
      }

      public String getName() {
         return m_Name;
      }

      public String getUnits() {
         return m_Units;
      }

      public int getValue() {
         return m_Value;
      }

      public int getDefault() {
         return m_Default;
      }

      public boolean isDefault() {
         return m_Value == m_Default;
      }

      public int getMin() {
         return m_Min;
      }

      public int getMax() {
         return m_Max;
      }

      public int getStep() {
         return m_Step;
      }

      public boolean isCurrent(int version) {
         return (m_Versions & 1 << version - 1) != 0;
      }

      // skip immutable version and offset
      public boolean isGuiItem(int version) {
         return this != IntSettings.FORMAT_VERSION
             && this != IntSettings.CONFIG_SIZE
             && isCurrent(version);
      }

      public void setValue(int value) {
         m_Value = value;
      }

      private IntSettings(String name, String units, int def, int min, int max, int step, int v) {
         m_Name = name;
         m_Units = units;
         m_Value = def;
         m_Default = def;
         m_Min = min;
         m_Max = max;
         m_Step = step;
         m_Versions = v;
      }

      private final String m_Name;
      private final String m_Units;
      private final int m_Versions;
      private final int m_Min;
      private final int m_Max;
      private final int m_Step;
      private final int m_Default;
      private int m_Value;
   }

   ////////////////////////////////////////////////////////////////////////////
   public enum BoolSettings {
      ENABLE_REPEAT("Enable key repeat", true, 1, 7),
      ENABLE_STORAGE("Enable mass storage", false, 2, 1),
      DIRECT_KEY_MODE("Enable direct key mode", false, 2, 4),
      JOYSTICK_CLICKS_LEFT("Joystick clicks left", true, 4, 4),
      NO_BLUETOOTH("Disable Bluetooth", false, 8, 6),
      STICKY_NUM("Sticky Num button", false, 16, 6),
      STICKY_SHIFT("Sticky Shift button", false, 128, 6);

      public String toString() {
         return m_Name + " " + m_Value;
      }

      public String getName() {
         return m_Name;
      }

      public boolean is() {
         return m_Value;
      }

      public boolean getDefault() {
         return m_Default;
      }

      public boolean isDefault() {
         return m_Value == m_Default;
      }

      public boolean isCurrent(int version) {
         return version == 0 || (m_Versions & 1 << version - 1) != 0;
      }

      public void setValue(boolean value) {
         m_Value = value;
      }

      public void fromBit(int bits) {
         m_Value = (bits & m_Bit) != 0;
      }

      public int toBit() {
         return m_Value ? m_Bit : 0;
      }

      public int toBit(int version) {
         return isCurrent(version) ? toBit() : 0;
      }

      static void setFromBits(int bits) {
         for (BoolSettings s : values()) {
           s.fromBit(bits);
         }
      }

      static int toBits() {
         return toBits(0);
      }

      static int toBits(int version) {
         int bits = 0;
         for (BoolSettings s : values()) {
           bits |= s.toBit(version);
         }
         return bits;
      }

      static String allToString() {
         return allToString(0);
      }

      static String allToString(int version) {
         String str = "";
         String sep = "";
         for (BoolSettings s : values()) {
            if (s.is() && s.isCurrent(version)) {
               str += sep + s.getName();
               sep = ", ";
            }
         }
         return str;
      }

      private BoolSettings(String name, boolean def, int bit, int v) {
         m_Name = name;
         m_Value = def;
         m_Default = def;
         m_Bit = bit;
         m_Versions = v;
      }

      private final String m_Name;
      private final int m_Versions;
      private final int m_Bit;
      private final boolean m_Default;
      private boolean m_Value;
   }
}
