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

   /////////////////////////////////////////////////////////////////////////////
   public ChordSource(KeyMap keyMap) {
      this(keyMap, null);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   public ChordSource(KeyMap keyMap, int[] counts) {
      m_KeyMap = keyMap;
      m_RandomChords = RandomChords.create(keyMap, counts);
      m_Counts = counts;
   }
   
   ////////////////////////////////////////////////////////////////////////////
   @Override // KeyPressListSource
   public KeyPressListSource clone() { return new ChordSource(m_KeyMap, m_Counts); }
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

   // Data /////////////////////////////////////////////////////////////////////
   private KeyMap m_KeyMap;
   private RandomChords m_RandomChords;
   private int[] m_Counts;
}
