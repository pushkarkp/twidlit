/**
 * Copyright 2015 Pushkar Piggott
 *
 * SaveChordsWindow.java
 */

package pkp.twidlit;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import pkp.twiddle.Chord;

//////////////////////////////////////////////////////////////////////
class SaveChordsWindow extends SaveTextWindow implements ActionListener {

   //////////////////////////////////////////////////////////////////////
   interface ContentForTitle {
      String getContentForTitle(String title);
   }

   ///////////////////////////////////////////////////////////////////
   SaveChordsWindow(ContentForTitle cft, String title, String dir) {
      super(title, "", "chords", dir);
      m_ContentForTitle = cft;
      addButton(new JButton(sm_CONVERT_TEXT[0]));
      getButton(1).addActionListener(this);
      replaceText(getContent()); 
   }
   
   ///////////////////////////////////////////////////////////////////
   @Override 
   public void actionPerformed(ActionEvent e) {
      if (e.getActionCommand() == sm_CONVERT_TEXT[0]) {
         getButton(1).setText(sm_CONVERT_TEXT[1]);
         replaceText(getContent()); 
         return;
      }
      if (e.getActionCommand() == sm_CONVERT_TEXT[1]) {
         getButton(1).setText(sm_CONVERT_TEXT[0]);
         replaceText(getContent()); 
         return;
      }
      super.actionPerformed(e);
   }
   
   ///////////////////////////////////////////////////////////////////
   private String getContent() {
      if (getButton(1).getText() == sm_CONVERT_TEXT[1]) {
         Chord.use4Finger(false);
      }
      String str = m_ContentForTitle.getContentForTitle(getTitle());
       if (getButton(1).getText() == sm_CONVERT_TEXT[1]) {
         Chord.use4Finger(true);
      }
      return str;
   }
   
   // Data ///////////////////////////////////////////////////////////
   private static final String[] sm_CONVERT_TEXT = {"As 0MRL", "As 4finger"};
   private final ContentForTitle m_ContentForTitle;
}
