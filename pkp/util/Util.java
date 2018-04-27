/**
 * Copyright 2016 Pushkar Piggott
 *
 * Util.java
 */
package pkp.util;

import java.util.List;

///////////////////////////////////////////////////////////////////////////////
public class Util {
   
   /////////////////////////////////////////////////////////////////////////////
   public static int getOptionPosition(String opt, String[] argv) {
      for (int i = 0; i < argv.length; ++i) {
         if (opt.equals(argv[i])) {
            return i;
         }
      }
      return -1;
   }

   /////////////////////////////////////////////////////////////////////////////
   public static String getOptionValue(String opt, String def, String[] argv) {
      int i = getOptionPosition(opt, argv);
      if (i == -1 || argv.length <= i + 1) {
         return def;
      }
      return argv[i + 1];
   }

   ////////////////////////////////////////////////////////////////////////////
   // Exhaustive search.
   public static <T> int find(T e, List<T> le) {
//System.out.printf("find %s %s %d%n", e.getClass().getSimpleName(), e.toString(), le.size());
      for (int i = 0; i < le.size(); ++i) {
         if (e.equals(le.get(i))) {
            return i;
         }
      }
      return -1;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int find(String e, String[] le) {
      for (int i = 0; i < le.length; ++i) {
         if (e.equals(le[i])) {
            return i;
         }
      }
      return -1;
   }

   ////////////////////////////////////////////////////////////////////////////
   public static int findStartsWith(String e, String[] le) {
      for (int i = 0; i < le.length; ++i) {
         if (le[i].startsWith(e)) {
            return i;
         }
      }
      return -1;
   }

   ////////////////////////////////////////////////////////////////////////////
   // Sort a list of integers and a parallel list.
   public static <T> void sortAscending(List<Integer> li, List<T> lt) {
      sort(li, lt, ArithmeticComparison.GREATER);
   }

   ////////////////////////////////////////////////////////////////////////////
   // Sort a list of integers and a parallel list.
   public static <T> void sortDescending(List<Integer> li, List<T> lt) {
      sort(li, lt, ArithmeticComparison.LESS);
   }

   ////////////////////////////////////////////////////////////////////////////
   public enum ArithmeticComparison {
      EQUAL() {
         @Override boolean apply(int lhs, int rhs) { return lhs == rhs; }
      },
      NOT_EQUAL() {
         @Override boolean apply(int lhs, int rhs) { return lhs != rhs; }
      },
      GREATER_OR_EQUAL() {
         @Override boolean apply(int lhs, int rhs) { return lhs >= rhs; }
      },
      LESS_OR_EQUAL() {
         @Override boolean apply(int lhs, int rhs) { return lhs <= rhs; }
      },
      GREATER() {
         @Override boolean apply(int lhs, int rhs) { return lhs > rhs; }
      },
      LESS() {
         @Override boolean apply(int lhs, int rhs) { return lhs < rhs; }
      };
      abstract boolean apply(int lhs, int rhs);
   }

   ////////////////////////////////////////////////////////////////////////////
   // Sort a list of integers and a parallel list.
   public static <T> void sort(List<Integer> li, List<T> lt, ArithmeticComparison cmp) {
      for (int i = 0; i < li.size(); ++i) {
         int v = li.get(i);
         for (int k = i + 1; k < li.size(); ++k) {
            if (cmp.apply(v, li.get(k))) {
               li.set(i, li.get(k));
               li.set(k, v);
               v = li.get(i);
               T t = lt.get(i);
               lt.set(i, lt.get(k));
               lt.set(k, t);
            }
         }
      }
   }
}
