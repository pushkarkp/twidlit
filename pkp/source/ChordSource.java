/**
 * Copyright 2015 Pushkar Piggott
 *
 * ChordSource.java
 *
 * A wrapper on a RandomChords to return strings.
 */

package pkp.source;

import java.util.ArrayList;
import pkp.twiddle.Chord;
import pkp.twiddle.Twiddle;
import pkp.twiddle.KeyMap;
import pkp.twiddle.KeyPressList;
import pkp.times.ChordTimes;
import pkp.util.Pref;

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
      // one array for each time pressed
      ArrayList<ArrayList<Integer>> chords = new ArrayList<ArrayList<Integer>>();
      for (int i = 0; i <= ChordTimes.sm_SPAN; ++i) {
         chords.add(new ArrayList<Integer>());
      }
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         // if thumbkey-less twiddle of chord is mapped
         Twiddle tw = new Twiddle(i + 1, 0);
         if (null != keyMap.getKeyPressList(tw)) {
            if (counts == null) {
               // just add in order
               chords.get(i).add(i + 1);
            } else {
               // add in order of times pressed, fewest first
//System.out.printf("%s:%d ", new Chord(i + 1), counts[i]);
               chords.get(counts[i]).add(i + 1);
            }
         }
      }
//System.out.println();
      int pool = Math.max(1, Pref.getInt("chord.pool.fraction", 16));
      m_UniformSource = new UniformSource(Chord.sm_VALUES, pool);
      for (int i = 0; i <= ChordTimes.sm_SPAN; ++i) {
         m_UniformSource.add(chords.get(i));
      }
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
      return m_KeyMap.getKeyPressList(new Twiddle(m_UniformSource.get(), 0));
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // KeyPressListSource
   public KeyPressListSource.Message send(KeyPressListSource.Message m) {
      m_UniformSource.next();
      return null;
   }

   // Data /////////////////////////////////////////////////////////////////////
   private KeyMap m_KeyMap;
   private int[] m_Counts;
   private UniformSource m_UniformSource;
}
