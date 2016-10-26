/**
 * Copyright 2015 Pushkar Piggott
 *
 * IntegerSetter.java
 */

package pkp.ui;

import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

//////////////////////////////////////////////////////////////////////
public class IntegerSetter implements ActionListener {

   ///////////////////////////////////////////////////////////////////
   public IntegerSetter(Window owner, String title, String caption, int init, int min, int max, int step) {
//System.out.printf("init %d min %d %d max %d step %d%n", init, min, max, step);
      m_Ok = false;
      m_Dialog = new ControlDialog(owner, title);
      m_Dialog.setModal(true);
      m_Dialog.setResizable(false);
      m_Value = new SpinnerNumberModel(init, min, max, step);
      JSpinner spin = new JSpinner(m_Value);
      m_Dialog.getBox().add(new LabelComponentBox(caption, spin));
      m_Dialog.addButton(createButton("OK"));
      m_Dialog.addButton(createButton("Cancel"));
      m_Dialog.setVisible(true);
   }
   
   ///////////////////////////////////////////////////////////////////
   public boolean isOk() {
      return m_Ok;
   }
   
   ///////////////////////////////////////////////////////////////////
   public int getValue() {
      return m_Value.getNumber().intValue();
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
   private SpinnerNumberModel m_Value;
   private ControlDialog m_Dialog;
   private boolean m_Ok;
}

