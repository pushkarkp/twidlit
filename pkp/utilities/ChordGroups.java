/**
 * Copyright 2016 Pushkar Piggott
 *
 * ChordGroups.java
 */

package pkp.utilities;

import java.util.ArrayList;
import pkp.twiddle.Chord;

////////////////////////////////////////////////////////////////////////////////
public class ChordGroups {

   /////////////////////////////////////////////////////////////////////////////
   public ChordGroups(int fixMask, int acceptMask, ChordText chordText, boolean free, boolean showText, String priority) {
      m_Groups = new ArrayList<ChordGroup>();
      m_AcceptMask = acceptMask;
      m_ChordText = chordText;
      m_GroupFree = free;
      m_ShowText = showText;
      m_Priority = priority;
      for (int p = 0; p < sm_Permutations.length; ++p) {
         if (applies(sm_Permutations[p], fixMask, m_AcceptMask)) {
//System.out.printf("Permutation 0x%x & fixMask 0x%x = 0x%x%n", sm_Permutations[p], fixMask, sm_Permutations[p] & fixMask);
            m_Groups.addAll(findGroups(sm_Permutations[p] & fixMask, 0, 0));
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
         String group = m_Groups.get(i).toString(m_Priority);
         if (m_Groups.get(i).getSize() >= min) {
            str += group;
         }
      }
      return str;
   }

   // Private /////////////////////////////////////////////////////////////////

   /////////////////////////////////////////////////////////////////////////////
   // only accept permutations that avoid zero fingers
   private static boolean applies(int permutation, int fixMask, int acceptMask) {
      for (int finger = 0; finger < 4; ++finger) {
         if (ChordGroup.getMaskFinger(finger, permutation) == 0) {
            if (ChordGroup.getMaskFinger(finger, acceptMask) == 0) {
               return false;
            }
         } else if (ChordGroup.getMaskFinger(finger, fixMask) == 0) {
            return false;
         }
      }
      return true;
   }

   /////////////////////////////////////////////////////////////////////////////
   private ArrayList<ChordGroup> findGroups(int fixMask, int f, int buildMask) {
//System.out.printf("findGroups(0x%x, %d, 0x%x)%n", fixMask, f, buildMask);
      int finger = 0;
      int fingerMask = 0;
      for (; f < 4 && fingerMask == 0; ++f) {
         finger = Chord.getFinger(m_Priority, f);
         fingerMask = ChordGroup.getMaskFinger(finger, fixMask);
      }
      if (fingerMask == 0) {
         int mask = combineMasks(buildMask, m_AcceptMask);
//System.out.printf("0x%x + 0x%x = 0x%x %s%n", buildMask, m_AcceptMask, mask, ChordGroup.maskToString(mask));
         if (mask == 0) {
            return new ArrayList<ChordGroup>();
         }
         ArrayList<ChordGroup> groups = new ArrayList<ChordGroup>();
         groups.add(new ChordGroup(mask, m_ChordText, m_GroupFree, m_ShowText));
         return groups;
      }
      ArrayList<ChordGroup> groups = new ArrayList<ChordGroup>();
      for (int b = 0; b < ChordGroup.sm_Maskable.length; ++b) {
         if ((ChordGroup.sm_Maskable[b] & fingerMask) != 0) {
            groups.addAll(
               findGroups(
                  fixMask, 
                  f, 
                  buildMask | ChordGroup.sm_Maskable[b] << finger * ChordGroup.sm_MaskShift
            ));
         }
      }
      return groups;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static int combineMasks(int fixed, int accept) {
      int both = 0;
      for (int f = 3; f >= 0; --f) {
         both <<= ChordGroup.sm_MaskShift;
         int finger = ChordGroup.getMaskFinger(f, fixed);
         if (finger == 0) {
            finger = ChordGroup.getMaskFinger(f, accept);
            if (finger == 0) {
               return 0;
            }
         }
         both |= finger;
      }
      return both;
   }

   /////////////////////////////////////////////////////////////////////////////
   private static final int sm_Permutations[] = {
      0xFFFF,
      0x0FFF, 0xF0FF, 0xFF0F, 0xFFF0,
      0x00FF, 0x0F0F, 0xF00F, 0x0FF0, 0xF0F0, 0xFF00, 
      0x000F, 0x00F0, 0x0F00, 0xF000};

   private final String m_Priority;
   private final boolean m_GroupFree;
   private final boolean m_ShowText;
   private final int m_AcceptMask;
   private final ChordText m_ChordText;
   private ArrayList<ChordGroup> m_Groups;
}