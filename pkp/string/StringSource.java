/**
 * Copyright 2015 Pushkar Piggott
 *
 * StringSource.java
 */
package pkp.string;

///////////////////////////////////////////////////////////////////////////////
public interface StringSource {
   public String getName();
   public String getFullName();
   public StringSource getSource();
   public String getNextString();
   public void close();
}
