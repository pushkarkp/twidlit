/**
 * Copyright 2016 Pushkar Piggott
 *
 * ChordGroups.java
 */

package pkp.utilities;

import java.util.ArrayList;
import pkp.twiddle.Assignments;
import pkp.twiddle.Chord;
import pkp.util.Log;

////////////////////////////////////////////////////////////////////////////////
public class ChordGroups {

   /////////////////////////////////////////////////////////////////////////////
   public ChordGroups(int fixMask, int acceptMask, Assignments asgs, boolean free, boolean showMappings) {
      m_Groups = new ArrayList<ChordGroup>();
      m_GroupFree = free;
      m_ShowMappings = free && showMappings;
      m_Assignments = asgs;
      m_AcceptMask = acceptMask;
      for (int p = 0; p < sm_Permutations.length; ++p) {
         if (applies(sm_Permutations[p], fixMask)) {
            add(findGroups(sm_Permutations[p] & fixMask, 5, 0), m_Groups);
         }
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   public ArrayList<ChordGroup> getGroups() {
      return m_Groups;
   }

   /////////////////////////////////////////////////////////////////////////////
   public String toString() {
      return toString(1);
   }

   /////////////////////////////////////////////////////////////////////////////
   public String toString(int min) {
      String str = "";
      for (int i = 0; i < m_Groups.size(); ++i) {
         if (m_Groups.get(i).getSize() >= min) {
            str += m_Groups.get(i).toString() + '\n';
         }
      }
      return str;
   }

   // Private /////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   private static boolean applies(int permutation, int mask) {
      for (int finger = 0; finger < 4; ++finger) {
         if (ChordGroup.getMaskFinger(finger, mask) == 0
          && ChordGroup.getMaskFinger(finger, permutation) != 0) {
            return false;
         }
      }
      return true;
   }

   /////////////////////////////////////////////////////////////////////////////
   private void add(ChordGroup g, ArrayList<ChordGroup> groups) {
      if (g.getSize() > 0) {
         groups.add(g);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private void add(ArrayList<ChordGroup> gs, ArrayList<ChordGroup> groups) {
      for (ChordGroup g: gs) {
         add(g, groups);
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   private ArrayList<ChordGroup> findGroups(int fixMask, int finger, int buildMask) {
      ArrayList<ChordGroup> groups = new ArrayList<ChordGroup>();
      for (;;) {
         --finger;
         buildMask <<= 4;
         if (ChordGroup.getMaskFinger(finger, fixMask) != 0) {
            break;
         }
         if (finger == 0) {
            add(getGroup(buildMask), groups);
            return groups;
         }
      }
      final int fingerMask = ChordGroup.getMaskFinger(finger, fixMask);
      for (int b = 0; b < ChordGroup.sm_Maskable.length; ++b) {
         if ((ChordGroup.sm_Maskable[b] & fingerMask) != 0) {
            if (finger > 0) {
               add(findGroups(fixMask, finger, ChordGroup.sm_Maskable[b] | buildMask), groups);
            } else {
               add(getGroup(ChordGroup.sm_Maskable[b] | buildMask), groups);
            }
         }
      }
      return groups;
   }

   /////////////////////////////////////////////////////////////////////////////
   public ChordGroup getGroup(int fixMask) {
      int bothMask = 0;
      for (int finger = 3; finger >= 0; --finger) {
         bothMask <<= 4;
         final int fixMaskFinger = ChordGroup.getMaskFinger(finger, fixMask);
         bothMask |= fixMaskFinger != 0
                   ? fixMaskFinger
                   : ChordGroup.getMaskFinger(finger, m_AcceptMask);
      }
      return new ChordGroup(bothMask, m_Assignments, m_GroupFree, m_ShowMappings);
   }

   /////////////////////////////////////////////////////////////////////////////
   private static final int sm_Permutations[] = {
      0xFFFF,
      0x0FFF, 0xF0FF, 0xFF0F, 0xFFF0,
      0x00FF, 0x0F0F, 0xF00F, 0x0FF0, 0xF0F0, 0xFF00, 
      0x000F, 0x00F0, 0x0F00, 0xF000};

   private final boolean m_GroupFree;
   private final boolean m_ShowMappings;
   private final int m_AcceptMask;
   private Assignments m_Assignments;
   private ArrayList<ChordGroup> m_Groups;
}