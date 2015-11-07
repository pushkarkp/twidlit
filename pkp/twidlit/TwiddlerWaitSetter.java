/**
 * Copyright 2015 Pushkar Piggott
 *
 * TwiddlerWaitSetter.java
 */

package pkp.twidlit;

import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import pkp.ui.ControlDialog;
import pkp.ui.LabelComponentBox;
import pkp.ui.IntegerTextField;
import pkp.util.Pref;
import pkp.util.Log;

//////////////////////////////////////////////////////////////////////
class TwiddlerWaitSetter implements ActionListener {

   ///////////////////////////////////////////////////////////////////
   TwiddlerWaitSetter(Window owner, int wait) {
      m_Ok = false;
      m_Dialog = new ControlDialog(owner, "Twiddling speed");
      m_Dialog.setModal(true);
      m_Dialog.setResizable(false);
      m_Wait = new SpinnerNumberModel(wait, 1, 30000, 100);
      m_Dialog.getBox().add(new LabelComponentBox("Milliseconds to wait for chords:", 
                                                   new JSpinner(m_Wait)));
      m_Dialog.addButton(createButton("Cancel"));
      m_Dialog.addButton(createButton("OK"));
      m_Dialog.setVisible(true);
   }
   
   ///////////////////////////////////////////////////////////////////
   boolean isOk() {
      return m_Ok;
   }
   
   ///////////////////////////////////////////////////////////////////
   int getWait() {
      return m_Wait.getNumber().intValue();
   }

   ///////////////////////////////////////////////////////////////////
   @Override 
   public void actionPerformed(ActionEvent e) {
      m_Ok = "OK".equals(e.getActionCommand());
      m_Dialog.dispose();
      m_Dialog = null;
   }

   // Private /////////////////////////////////////////////////////////////////

   ///////////////////////////////////////////////////////////////////
   private JButton createButton(String label) {
      JButton button = new JButton(label);
      button.addActionListener(this);
      return button;
   }
   
   // Data /////////////////////////////////////////////////////////////////////
   private SpinnerNumberModel m_Wait;
   private ControlDialog m_Dialog;
   private boolean m_Ok;
}

