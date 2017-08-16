/**
 * Copyright 2015 Pushkar Piggott
 *
 * ChordSource.java
 *
 * A wrapper on a UniformSource to return KeyPressLists
 * representing random chords.
 */

package pkp.source;

import java.util.ArrayList;
import pkp.twiddle.Chord;
import pkp.twiddle.Twiddle;
import pkp.twiddle.KeyMap;
import pkp.twiddle.KeyPress;
import pkp.twiddle.KeyPressList;
import pkp.times.ChordTimes;
import pkp.util.Pref;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
public class ChordSource implements KeyPressListSource {

   /////////////////////////////////////////////////////////////////////////////
   public ChordSource(KeyMap keyMap) {
      this(keyMap, null);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   public ChordSource(KeyMap keyMap, int[] counts) {
      m_KeyMap = keyMap;
      m_Counts = counts;
      if (m_KeyMap == null || m_KeyMap.getAssignments().isEmpty()) {
         Log.err("There were no assignments in the configuration.");
      }
      int maxCount = 1;
      if (counts != null) {
         for (int i = 0; i < counts.length; ++i) {
            if (maxCount < counts[i]) {
               maxCount = counts[i];
            }
         }
      }
      // one array for each time pressed, including zero
      ArrayList<ArrayList<Integer>> chords = new ArrayList<ArrayList<Integer>>();
      for (int i = 0; i < maxCount + 1; ++i) {
         chords.add(new ArrayList<Integer>());
      }
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         // if thumb-less twiddle of chord is mapped
         Twiddle tw = new Twiddle((byte)(i + 1), 0);
         KeyPressList kpl = m_KeyMap.getKeyPressList(tw);
         if (kpl != null && !kpl.contains(KeyPress.getLost())) {
            chords.get(counts == null
                       ? i
                       : counts[i]
                       ).add(i + 1);
         }
      }
//System.out.println();
      m_UniformSource = new UniformSource<Integer>(chords);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   public ChordSource newKeyMap(KeyMap keyMap) {
      return new ChordSource(keyMap, m_Counts);
   }
   
   ////////////////////////////////////////////////////////////////////////////
   @Override // KeyPressListSource
   public KeyPressListSource clone() { return new ChordSource(m_KeyMap, m_Counts); }
   @Override // KeyPressListSource
   public String getName() { return "RandomChords:"; }
   @Override // KeyPressListSource
   public String getFullName() { return getName(); }
   @Override // KeyPressListSource
   public KeyPressListSource getSource() { return null;  }
   @Override // KeyPressListSource
   public void close() {}

   /////////////////////////////////////////////////////////////////////////////
   @Override // KeyPressListSource
   public KeyPressList getNext() {
      KeyPressList kpl;
      do {
         Twiddle tw = new Twiddle((byte)(int)m_UniformSource.get(), 0);
         kpl = m_KeyMap.getKeyPressList(tw);
         if (kpl == null) {
            Log.log("No keys defined for " + tw);
         }
      } while (kpl == null);
      return kpl;
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // KeyPressListSource
   public KeyPressListSource.Message send(KeyPressListSource.Message m) {
      m_UniformSource.next(m != null);
      return null;
   }

   // Data /////////////////////////////////////////////////////////////////////
   private KeyMap m_KeyMap;
   private int[] m_Counts;
   private UniformSource<Integer> m_UniformSource;
}
