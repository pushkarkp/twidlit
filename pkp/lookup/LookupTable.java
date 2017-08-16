/**
 * Copyright 2015 Pushkar Piggott
 *
 * LookupTable.java
 * 
 */
package pkp.lookup;

///////////////////////////////////////////////////////////////////////////////
public interface LookupTable {

   ////////////////////////////////////////////////////////////////////////////
   public static final int sm_NO_VALUE = 0x80000000;

   ////////////////////////////////////////////////////////////////////////////
   public boolean isSingleKeyed();

   ////////////////////////////////////////////////////////////////////////////
   public int get(int key);
   public int get(int key1, int key2);
   // returns an array with element 0 the valid length
   // key2 may be NO_VALUE
   public int[] getAll(int key1, int key2);
   public String toString();
}
