/**
 * Copyright 2015 Pushkar Piggott
 *
 * TwidlitInit.java
 */

package pkp.twidlit;

import java.io.File;
import pkp.twiddle.KeyMap;
import pkp.times.ChordTimes;

////////////////////////////////////////////////////////////////////////////////
// Each of these Twidlit methods recreates the source.
// This interface allows the initial settings to be gathered 
// in a separate object so the source can be created once 
// with the collected settings.
interface TwidlitInit {
   boolean setKeyMap(KeyMap km);
   void setRightHand(boolean right);
   void setChords();
   boolean setKeystrokes(File f);
}
