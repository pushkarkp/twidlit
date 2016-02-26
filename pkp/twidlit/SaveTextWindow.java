/**
 * Copyright 2015 Pushkar Piggott
 *
 * SaveTextWindow.java
 */

package pkp.twidlit;

import java.awt.Font;

//////////////////////////////////////////////////////////////////////
class SaveTextWindow extends pkp.ui.SaveTextWindow {
   public SaveTextWindow(String title, String str, String ext, String dir) {
      super(title, str, ext);
      setDirectory(dir);
      Font font = getFont();
      setFont(new Font("monospaced", font.getStyle(), font.getSize()));
   }
}
