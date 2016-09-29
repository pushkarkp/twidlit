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
      MAJOR_VERSION("Major Version", "", 4, 0, 0, 0, -1),
      MINOR_VERSION("Minor Version", "", 16, 0, 0, 0, -1),
      MOUSE_EXIT_DELAY("Mouse mode exit delay", " (ms)", 1500, 0, 5000, 1000, 1),
      MS_BETWEEN_TWIDDLES("Faster mouse threshold", " (ms)", 383, 0, 5000, 1000, 1),
      START_SPEED("Starting mouse speed", "", 3, 0, 10, 2, 1),
      FAST_SPEED("Fast mouse speed", "", 6, 0, 10, 2, 1),
      MOUSE_SENSITIVITY("Mouse sensitivity", "", 128, 0, 255, 50, 3),
      MS_REPEAT_DELAY("Key repeat delay", " (ms)", 1000, 0, 2500, 500, 3),
      IDLE_LIMIT("Idle limit", " (s)", 1500, 0, 60000, 12000, 2);

      public String toString() {
         return m_Name + " " + m_Value;
      }

      public String getName() {
         return m_Name;
      }

      public String getUnits() {
         return m_Units;
      }

      public boolean isCurrent(int version) {
         return (m_Versions & version) != 0;
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
      ENABLE_REPEAT("Enable key repeat", false, 3),
      ENABLE_STORAGE("Enable mass storage", false, 1),
      STICKY_NUM("Sticky Num button", false, 2),
      STICKY_SHIFT("Sticky Shift button", false, 2),
      NO_BLUETOOTH("Disable Bluetooth", false, 2);

      public String toString() {
         return m_Name + " " + m_Value;
      }

      public String getName() {
         return m_Name;
      }

      public boolean isCurrent(int version) {
         return (m_Versions & version) != 0;
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

      public void setValue(boolean value) {
         m_Value = value;
      }

      private BoolSettings(String name, boolean def, int v) {
         m_Name = name;
         m_Value = def;
         m_Default = def;
         m_Versions = v;
      }

      private final String m_Name;
      private final int m_Versions;
      private final boolean m_Default;
      private boolean m_Value;
   }
}
