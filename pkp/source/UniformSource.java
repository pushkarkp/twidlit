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
   UniformSource(ArrayList<ArrayList<Integer>> items) {
      this(items, 2);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   UniformSource(ArrayList<ArrayList<Integer>> items, int pool) {
      m_Random = new Random();
      add(items);
      m_POOL_SIZE = Math.max(pool, m_Items.length / 16);
      m_First = 0;
      m_Next = 0;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Returns a random chord from the pool using the predefined pool size.
   // It may be the same as the last one unless next has been called.
   int get() {
      return get(m_POOL_SIZE);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   void next(boolean accepted) {
      if (accepted) {
         // let it go
         m_First = (m_First + 1) % m_Items.length;
      } else {
         // put it back in the pool 
         int first = m_Items[m_First];
         int j;
         for (int i = m_First; i != m_Next; i = j) {
            j = (i + 1) % m_Items.length;
            m_Items[i] = m_Items[j];
         }
         m_Items[m_Next] = first;
      }
//System.out.printf("next() hit %b m_First [%d] 0x%x m_Next [%d] 0x%x%n", accepted, m_First, m_Items[m_First], m_Next, m_Items[m_Next]);
   }

   /////////////////////////////////////////////////////////////////////////////
   public String toString() {
      String str = "";
      for (int i = 0; i < m_Items.length; ++i) {
         str += m_Items[i];
         str += (i == m_POOL_SIZE - 1) ? '|' : ' ';
      }
      return str;
   }
   
   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private void add(ArrayList<ArrayList<Integer>> allItems) {
      int size = 0;
      for (int j = 0; j < allItems.size(); ++j) {
         size += allItems.get(j).size();
      }
      int times = 1;
      while (size * times < m_POOL_SIZE) {
         ++times;
      }         
      m_Items = new int[size];
      int base = 0;
      for (int j = 0; j < allItems.size(); ++j) {
//System.out.printf("%d: ", j);
         ArrayList<Integer> items = allItems.get(j);
         for (int i = 0; i < items.size(); ++i) {
            m_Items[base + i] = items.get(i);
//System.out.print(m_Items[base + i] + " ");
         }
//System.out.println();
         // shuffle them
         for (int i = 0; i < items.size(); ++i) {
            int rand = m_Random.nextInt(items.size());
            int swap = m_Items[base + rand];
            m_Items[base + rand] = m_Items[base + i];
            m_Items[base + i] = swap;
         }
         base += items.size();
      }
//System.out.println(this);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private int get(int pool) {
      int ix = (m_Next + m_Random.nextInt(pool)) % m_Items.length;
//System.out.printf("pool %d ix %d%n", pool, ix);
      int swap = m_Items[ix];
      m_Items[ix] = m_Items[m_Next];
      m_Items[m_Next] = swap;
      m_Next = (m_Next + 1) % m_Items.length;
      return swap;
   }
   
   // Data /////////////////////////////////////////////////////////////////////
   private Random m_Random;
   private int[] m_Items;
   private final int m_POOL_SIZE;
   private int m_First;
   private int m_Next;
}
