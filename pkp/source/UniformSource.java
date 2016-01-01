/**
 * Copyright 2015 Pushkar Piggott
 *
 * UniformSource.java
 *
 * If we just choose values at random from a limited stock 
 * then pretty soon they have very different frequencies. 
 * This class returns a random-ish stream with uniform-ish
 * frequencies.
 */

package pkp.source;

import java.util.Random;
import java.util.ArrayList;

////////////////////////////////////////////////////////////////////////////////
class UniformSource {

   /////////////////////////////////////////////////////////////////////////////
   UniformSource(ArrayList<Integer> items, int poolFraction) {
      this(items.size(), poolFraction);
      add(items);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   UniformSource(int size, int poolFraction) {
      m_Random = new Random();
      m_Items = new int[size];
      m_POOL_SIZE = Math.min(size, Math.max(1, size / poolFraction));
      m_Next = 0;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Returns a random chord from the pool.
   // It may be the same as the last one unless next has been called.
   int get() {
      return get(m_POOL_SIZE);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Move the pool (sliding window) up one.
   // The old m_Next-th will not be delivered til next time round.
   void next() {
      m_Next = (m_Next + 1) % m_Items.length;
   }

   /////////////////////////////////////////////////////////////////////////////
   public String toString() {
      String str = "";
      for (int i = 0; i < m_Items.length; ++i) {
         str += m_Items[i] + " ";
      }
      return str;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   void add(ArrayList<Integer> items) {
//System.out.println("UniformSource.Add()" + this);
      int base = m_Next;
      for (int i = 0; i < items.size(); ++i) {
         m_Items[base + i] = items.get(i);
//System.out.print(new Chord(m_Items[base + i]) + " ");
      }
//System.out.println();
      // shuffle them
      for (int i = 0; i < items.size(); ++i) {
         int ix = base + m_Random.nextInt(items.size());
         int swap = m_Items[ix];
         m_Items[ix] = m_Items[m_Next];
         m_Items[m_Next] = swap;
         ++m_Next;
      }
      m_Next %= m_Items.length;
   }
   
   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private int get(int pool) {
      int ix = (m_Next + m_Random.nextInt(pool)) % m_Items.length;
      int swap = m_Items[ix];
      m_Items[ix] = m_Items[m_Next];
      m_Items[m_Next] = swap;
      return swap;
   }
   
   // Data /////////////////////////////////////////////////////////////////////
   private Random m_Random;
   private int[] m_Items;
   private final int m_POOL_SIZE;
   private int m_Next;
}
