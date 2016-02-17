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
import java.util.List;
import java.util.ArrayList;

////////////////////////////////////////////////////////////////////////////////
class UniformSource<T> {

   /////////////////////////////////////////////////////////////////////////////
   UniformSource(ArrayList<ArrayList<T>> items) {
      this(items, 2);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   UniformSource(ArrayList<ArrayList<T>> items, int pool) {
      m_Random = new Random();
      add(items);
      m_POOL_SIZE = Math.max(pool, m_Items.size() / 16);
      m_First = 0;
      m_Next = 0;
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Returns a random chord from the pool using the predefined pool size.
   // It may be the same as the last one unless next has been called.
   T get() {
      return get(m_POOL_SIZE);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   void next(boolean accepted) {
      if (accepted) {
         // let it go
         m_First = (m_First + 1) % m_Items.size();
      } else {
         // put it back in the pool 
         T first = m_Items.get(m_First);
         int j;
         for (int i = m_First; i != m_Next; i = j) {
            j = (i + 1) % m_Items.size();
            m_Items.set(i, m_Items.get(j));
         }
         m_Items.set(m_Next, first);
      }
//System.out.printf("next() hit %b m_First .get(%d) 0x%x m_Next .get(%d) 0x%x%n", accepted, m_First, m_Items.get(m_First), m_Next, m_Items.get(m_Next));
   }

   /////////////////////////////////////////////////////////////////////////////
   public String toString() {
      String str = "";
      for (int i = 0; i < m_Items.size(); ++i) {
         str += m_Items.get(i);
         str += (i == m_POOL_SIZE - 1) ? '|' : ' ';
      }
      return str;
   }
   
   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private void add(ArrayList<ArrayList<T>> allItems) {
      int size = 0;
      for (int j = 0; j < allItems.size(); ++j) {
         size += allItems.get(j).size();
      }
      int times = 1;
      while (size * times < m_POOL_SIZE) {
         ++times;
      }         
      m_Items = new ArrayList<T>(size);
      int base = 0;
      for (int j = 0; j < allItems.size(); ++j) {
//System.out.printf("j %d: ", j);
         ArrayList<T> items = allItems.get(j);
         for (int i = 0; i < items.size(); ++i) {
            m_Items.add(items.get(i));
//System.out.print(m_Items.get(base + i) + " ");
         }
//System.out.println();
         // shuffle them
         for (int i = 0; i < items.size(); ++i) {
            int rand = m_Random.nextInt(items.size());
            T swap = m_Items.get(base + rand);
            m_Items.set(base + rand, m_Items.get(base + i));
            m_Items.set(base + i, swap);
         }
         base += items.size();
      }
//System.out.println(this);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   private T get(int pool) {
      int ix = (m_Next + m_Random.nextInt(pool)) % m_Items.size();
//System.out.printf("pool %d ix %d%n", pool, ix);
      T swap = m_Items.get(ix);
      m_Items.set(ix, m_Items.get(m_Next));
      m_Items.set(m_Next, swap);
      m_Next = (m_Next + 1) % m_Items.size();
      return swap;
   }
   
   // Data /////////////////////////////////////////////////////////////////////
   private Random m_Random;
   private List<T> m_Items;
   private final int m_POOL_SIZE;
   private int m_First;
   private int m_Next;
}
