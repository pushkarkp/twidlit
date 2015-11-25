/**
 * Copyright 2015 Pushkar Piggott
 *
 * Settings.java
 */

package pkp.twiddler;

///////////////////////////////////////////////////////////////////////////////
public interface Settings {

   public static final String sm_ENABLE_REPEAT_NAME = "Enable key repeat";
   public static final String sm_ENABLE_STORAGE_NAME = "Enable mass storage";

   public IntSettings getIntSettings();
   public boolean isEnableRepeat();
   public boolean isEnableStorage();

   ////////////////////////////////////////////////////////////////////////////
   public enum IntSettings {
      MAJOR_VERSION("Major Version", 4),
      MINOR_VERSION("Minor Version", 16),
      MOUSE_EXIT_DELAY("Mouse mode exit delay", 1500),
      MS_BETWEEN_TWIDDLES("Faster mouse threshold", 383),
      START_SPEED("Starting mouse speed", 3),
      FAST_SPEED("Fast mouse speed", 6),
      MOUSE_ACCELERATION("Mouse acceleration", 10),
      MS_REPEAT_DELAY("Key repeat delay", 1000);

      public String toString() {
         return m_Name + " " + m_Value;
      }

      public int getValue() {
         return m_Value;
      }

      public boolean isDefault() {
         return m_Value == m_Default;
      }

      public void setValue(int value) {
         m_Value = value;
      }

      public final String m_Name;

      private IntSettings(String name, int def) {
         m_Name = name;
         m_Value = def;
         m_Default = def;
      }

      private int m_Value;
      private int m_Default;
   }
}
