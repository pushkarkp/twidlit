/**
 * SliderBuilder.java
 */
package pkp.ui;

import java.lang.Integer;
import java.util.Hashtable;
import javax.swing.JLabel;
import javax.swing.JSlider;

///////////////////////////////////////////////////////////////////////////////
public class SliderBuilder {

   ////////////////////////////////////////////////////////////////////////////
   public static JSlider build(int min, int max, int step, int value) {
      JSlider s = new JSlider(min, max, value);
      setup(s, step);
      setupLabels(s, min, max, step);
      return s;
   }

   ////////////////////////////////////////////////////////////////////////////
   //public static JSlider build(int min, int max, int step, int lower, int upper) {
   //   JSlider s = (JSlider)new RangeSlider(min, max, lower, upper);
   //   setup(s, step);
   //   setupLabels(s, min, max, step);
   //   return s;
   //}

   ////////////////////////////////////////////////////////////////////////////
   private static void setup(JSlider s, int step) {
      s.setOpaque(false);
      s.setMajorTickSpacing(step);
      s.setPaintTicks(true);
   }

   ////////////////////////////////////////////////////////////////////////////
   private static void setupLabels(JSlider s, int min, int max, int step) {
      Hashtable<Integer, JLabel> table = new Hashtable<Integer, JLabel>();
      for (int i = min; i <= max; i += step) {
         table.put(i, new JLabel(Integer.toString(i)));
      }
      s.setLabelTable(table);
      s.setPaintLabels(true);
   }
}
