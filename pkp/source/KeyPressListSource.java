/**
 * KeyPressListSource.java
 */
package pkp.source;

import pkp.twiddle.KeyPressList;

///////////////////////////////////////////////////////////////////////////////
public interface KeyPressListSource {
   public String getName();
   public String getFullName();
   public KeyPressListSource getSource();
   public void close();
   public KeyPressList getNext();
   public interface Message {}
   public Message send(Message m);
}
