/**
 * Copyright 2015 Pushkar Piggott
 *
 * Lookup.java
 */
package pkp.lookup;

///////////////////////////////////////////////////////////////////////////////
public interface Lookup {
   public int get(int key);
   public int get(int key1, int key2);
   public int[] getAll(int key1, int key2);
   public String toString();
}
