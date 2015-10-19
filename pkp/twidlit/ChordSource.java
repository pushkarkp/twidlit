/**
 * Copyright 2015 Pushkar Piggott
 *
 * ChordSource.java
 *
 * If we just choose chords at random then pretty soon they
 * have very different frequencies. This class returns a
 * random-ish stream with uniform-ish frequencies.
 */

package pkp.twidlit;

import java.util.Random;
import java.util.ArrayList;
import pkp.twiddle.Chord;
import pkp.util.Pref;

////////////////////////////////////////////////////////////////////////////////
class ChordSource {

   /////////////////////////////////////////////////////////////////////////////
   ChordSource(ArrayList<Byte>[] chords) {
//System.out.println("ChordSource()");
      m_Random = new Random();
      int size = 0;
      for (int i = 0; i < chords.length; ++i) {
         size += chords[i].size();
      }
      m_Chords = new byte[size];
      int pref = Math.max(1, Pref.getInt("chord.pool.fraction", 16));
      m_POOL_SIZE = Math.min(size, Math.max(1, size / pref));
      m_Next = 0;
      for (int i = 0; i < chords.length; ++i) {
//System.out.printf("i %d m_Next %d%n", i, m_Next);
         add(chords[i]);
      }
//System.out.println(this);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Returns a random chord from the pool.
   // It may be the same as the lst one unless next has been called.
   int get() {
      return get(m_POOL_SIZE);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Move the pool (sliding window) up one.
   // The old m_Next-th will not be delivered til next time round.
   void next() {
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
   void add(ArrayList<Byte> chords) {
//System.out.println("ChordSource.Add()\n" + this);
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
   int get(int pool) {
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
