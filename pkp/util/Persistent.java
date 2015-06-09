/**
 * Copyright 2015 Pushkar Piggott
 *
 * Persistent.java
 */
package pkp.util;

public interface Persistent {
   /// Tag can distinguish multiple instances.
   public void persist(String tag);
}

