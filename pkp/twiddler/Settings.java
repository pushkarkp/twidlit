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
      MAJOR_VERSION("Major Version"),
      MINOR_VERSION("Minor Version"),
      MOUSE_EXIT_DELAY("Mouse mode exit delay"),
      MS_BETWEEN_TWIDDLES("Faster mouse threshold"),
      START_SPEED("Starting mouse speed"),
      FAST_SPEED("Fast mouse speed"),
      MOUSE_ACCELERATION("Mouse acceleration"),
      MS_REPEAT_DELAY("Key repeat delay");

      public String toString() {
         return m_Name + " " + m_Value;
      }

      public int getValue() {
         return m_Value;
      }

      public void setValue(int value) {
         m_Value = value;
      }

      public final String m_Name;

      private IntSettings(String name) {
         m_Name = name;
         m_Value = 0;
      }

      private int m_Value;
   }
}
