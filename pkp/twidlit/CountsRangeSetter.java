/**
 * Copyright 2015 Pushkar Piggott
 *
 * CountsRangeSetter.java
 */

package pkp.twidlit;

import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.JButton;
import pkp.ui.ControlDialog;
import pkp.ui.LabelComponentBox;
import pkp.ui.IntegerTextField;
import pkp.util.Pref;
import pkp.util.Log;

//////////////////////////////////////////////////////////////////////
class CountsRangeSetter implements ActionListener {

   ///////////////////////////////////////////////////////////////////
   CountsRangeSetter(Window owner, int min, int max) {
      m_Min = new IntegerTextField(min, 0, Integer.MAX_VALUE);
      m_Max = new IntegerTextField(max, 0, Integer.MAX_VALUE);
      m_Ok = false;
      m_Dialog = new ControlDialog(owner, "Range of Counts Displayed");
      m_Dialog.setModal(true);
      m_Dialog.setResizable(false);
      Box box = m_Dialog.getBox();
      box.add(new LabelComponentBox("Minimum count:", m_Min));
      box.add(new LabelComponentBox("Maximum count:", m_Max));
      m_Dialog.addButton(createButton("Cancel"));
      m_Dialog.addButton(createButton("OK"));
      m_Dialog.setVisible(true);
   }
   
   ///////////////////////////////////////////////////////////////////
   boolean isOk() {
      return m_Ok;
   }
   
   ///////////////////////////////////////////////////////////////////
   int getMinimum() {
      return (Integer)m_Min.getValue();
   }
   
   ///////////////////////////////////////////////////////////////////
   int getMaximum() {
      return (Integer)m_Max.getValue();
   }
   
   ///////////////////////////////////////////////////////////////////
   @Override 
   public void actionPerformed(ActionEvent e) {
      if (!"OK".equals(e.getActionCommand())
       && !"Cancel".equals(e.getActionCommand())) {
         Log.warn("CountsRangeSetter got unknown ActionCommand \"" + e.getActionCommand() + "\".");
      }
      m_Ok = "OK".equals(e.getActionCommand());
      if (m_Ok) {
         if (getMinimum() > getMaximum()) {
            m_Ok = false;
            Log.warn("Minimum exceeds maximum.");
            return;
         }
         if (getMinimum() < 0) {
            m_Ok = false;
            Log.warn("Both values must be greater than or equal to zero.");
            return;
         }
      }
      m_Dialog.dispose();
      m_Dialog = null;
   }

   // Private /////////////////////////////////////////////////////////////////

   ///////////////////////////////////////////////////////////////////
   private JButton createButton(String label) {
      JButton button = new JButton(label);
      button.setBackground(Pref.getColor("background.color"));
      button.addActionListener(this);
      return button;
   }
   
   // Data /////////////////////////////////////////////////////////////////////
   private IntegerTextField m_Min;
   private IntegerTextField m_Max;
   private ControlDialog m_Dialog;
   private boolean m_Ok;
}

