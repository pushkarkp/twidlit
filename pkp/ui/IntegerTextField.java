/**
 * Copyright 2015 Pushkar Piggott
 *
 * IntegerTextField.java
 */
package pkp.ui;

import java.awt.Dimension;
import javax.swing.JTextField;
import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;
import java.text.ParseException;
import java.text.NumberFormat;

///////////////////////////////////////////////////////////////////////////////
public class IntegerTextField extends JFormattedTextField {

   ///////////////////////////////////////////////////////////////////
   public IntegerTextField(int value, int min, int max) {
      super(createFormatter(min, max));
      setPreferredSize(new Dimension(90, (int)getPreferredSize().getHeight()));
      setMinimumSize(getPreferredSize());
      setMaximumSize(getPreferredSize());
      setOpaque(false);
      setValue(new Integer(value));
      setHorizontalAlignment(JTextField.RIGHT);
      setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
   }
   
   ///////////////////////////////////////////////////////////////////
   public void closed() {
      if (isEditValid()) {
         try {
            commitEdit();
         } catch (ParseException e) {
         }
      }
   }
   
   // Private /////////////////////////////////////////////////////////////////

   ///////////////////////////////////////////////////////////////////
   private static NumberFormatter createFormatter(int min, int max) {
      NumberFormatter nf = new NumberFormatter(NumberFormat.getInstance());
      nf.setValueClass(Integer.class);
      nf.setMinimum(min);
      nf.setMaximum(max);
      return nf;
   }
}
