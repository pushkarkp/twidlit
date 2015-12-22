/**
 * Copyright 2015 Pushkar Piggott
 *
 * ChordSource.java
 *
 * A wrapper on a RandomChords to return strings.
 */

package pkp.source;

import pkp.twiddle.KeyMap;
import pkp.twiddle.KeyPressList;

////////////////////////////////////////////////////////////////////////////////
public class ChordSource implements KeyPressListSource {

   ////////////////////////////////////////////////////////////////////////////
   public static ChordSource create(KeyMap keyMap) {
      return create(keyMap, null);
   }

   ////////////////////////////////////////////////////////////////////////////
   public static ChordSource create(KeyMap keyMap, int[] counts) {
      return new ChordSource(keyMap, RandomChords.create(keyMap, counts));
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // KeyPressListSource
   public String getName() { return "RandomChords:"; }
   @Override // KeyPressListSource
   public String getFullName() { return getName(); }
   @Override // KeyPressListSource
   public KeyPressListSource getSource() { return null;  }
   @Override // KeyPressListSource
   public void close() {}

   /////////////////////////////////////////////////////////////////////////////
   @Override // KeyPressListSource
   public KeyPressList getNext() {
      return m_KeyMap.getKeyPressList(m_RandomChords.getTwiddle());
   }

   /////////////////////////////////////////////////////////////////////////////
   @Override // KeyPressListSource
   public KeyPressListSource.Message send(KeyPressListSource.Message m) {
      m_RandomChords.next();
      return null;
   }

   // Private //////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private ChordSource(KeyMap keyMap, RandomChords cs) {
      m_KeyMap = keyMap;
      m_RandomChords = cs;
   }
   
   // Data /////////////////////////////////////////////////////////////////////
   private KeyMap m_KeyMap;
   private RandomChords m_RandomChords;
}
