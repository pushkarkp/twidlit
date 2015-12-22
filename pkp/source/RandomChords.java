/**
 * Copyright 2015 Pushkar Piggott
 *
 * RandomChords.java
 *
 * If we just choose chords at random then pretty soon they
 * have very different frequencies. This class returns a
 * random-ish stream with uniform-ish frequencies.
 */

package pkp.source;

import java.util.Random;
import java.util.ArrayList;
import pkp.twiddle.Twiddle;
import pkp.twiddle.Chord;
import pkp.twiddle.KeyMap;
import pkp.times.ChordTimes;
import pkp.util.Pref;

////////////////////////////////////////////////////////////////////////////////
public class RandomChords {

   ////////////////////////////////////////////////////////////////////////////
   public static RandomChords create(KeyMap keyMap) {
      return create(keyMap, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static RandomChords create(KeyMap keyMap, int[] counts) {
      ArrayList<ArrayList<Byte>> chords = new ArrayList<ArrayList<Byte>>();
      for (int i = 0; i <= ChordTimes.sm_SPAN; ++i) {
         chords.add(new ArrayList<Byte>());
      }
      for (int i = 0; i < Chord.sm_VALUES; ++i) {
         Twiddle tw = new Twiddle(i + 1, 0);
         if (null != keyMap.getKeyPressList(tw)) {
            if (counts == null) {
               chords.get(i).add((byte)(i + 1));
            } else {
//System.out.printf("%s:%d ", new Chord(i + 1), counts[i]);
               chords.get(counts[i]).add((byte)(i + 1));
            }
         }
      }
//System.out.println();
      return new RandomChords(chords);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Returns a random chord from the pool.
   // It may be the same as the last one unless next has been called.
   public int get() {
      return get(m_POOL_SIZE);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   public Twiddle getTwiddle() {
      return new Twiddle(get(), 0);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Move the pool (sliding window) up one.
   // The old m_Next-th will not be delivered til next time round.
   public void next() {
      m_Next = (m_Next + 1) % m_Chords.length;
   }

   /////////////////////////////////////////////////////////////////////////////
   public String toString() {
      String str = "";
      for (int i = 0; i < m_Chords.length; ++i) {
         str += new Chord(m_Chords[i]) + " ";
      }
      return str;
   }
   
   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private RandomChords(ArrayList<ArrayList<Byte>> chords) {
      m_Random = new Random();
      int size = 0;
      for (int i = 0; i < chords.size(); ++i) {
         size += chords.get(i).size();
      }
      m_Chords = new byte[size];
      int pref = Math.max(1, Pref.getInt("chord.pool.fraction", 16));
      m_POOL_SIZE = Math.min(size, Math.max(1, size / pref));
      m_Next = 0;
      for (int i = 0; i < chords.size(); ++i) {
         add(chords.get(i));
      }
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private void add(ArrayList<Byte> chords) {
//System.out.println("RandomChords.Add()\n" + this);
      int base = m_Next;
      for (int i = 0; i < chords.size(); ++i) {
         m_Chords[base + i] = chords.get(i);
//System.out.print(new Chord(m_Chords[base + i]) + " ");
      }
//System.out.println();
      // shuffle them
      for (int i = 0; i < chords.size(); ++i) {
         int ix = base + m_Random.nextInt(chords.size());
         byte swap = m_Chords[ix];
         m_Chords[ix] = m_Chords[m_Next];
         m_Chords[m_Next] = swap;
         ++m_Next;
      }
      m_Next %= m_Chords.length;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private int get(int pool) {
      int ix = (m_Next + m_Random.nextInt(pool)) % m_Chords.length;
      byte swap = m_Chords[ix];
      m_Chords[ix] = m_Chords[m_Next];
      m_Chords[m_Next] = swap;
      return swap & 0xFF;
   }
   
   // Data /////////////////////////////////////////////////////////////////////
   private Random m_Random;
   private byte[] m_Chords;
   private final int m_POOL_SIZE;
   private int m_Next;
}
