package a;

import java.util.ArrayList;
import java.util.HashMap;

public class Demo04 {
  public static final HashMap<Character, Integer> getDigitsMap() {
    HashMap<Character, Integer> tmp1 = new HashMap<Character, Integer>();
    tmp1.put('0', 0);
    tmp1.put('1', 1);
    tmp1.put('2', 2);
    tmp1.put('3', 3);
    tmp1.put('4', 4);
    tmp1.put('5', 5);
    tmp1.put('6', 6);
    tmp1.put('7', 7);
    tmp1.put('8', 8);
    tmp1.put('9', 9);
    tmp1.put('٠', 0);
    tmp1.put('١', 1);
    tmp1.put('٢', 2);
    tmp1.put('٣', 3);
    tmp1.put('٤', 4);
    tmp1.put('٥', 5);
    tmp1.put('٦', 6);
    tmp1.put('٧', 7);
    tmp1.put('٨', 8);
    tmp1.put('٩', 9);
    tmp1.put('০', 0);
    tmp1.put('১', 1);
    tmp1.put('২', 2);
    tmp1.put('৩', 3);
    tmp1.put('৪', 4);
    tmp1.put('৫', 5);
    tmp1.put('৬', 6);
    tmp1.put('৭', 7);
    tmp1.put('৮', 8);
    tmp1.put('৯', 9);
    return tmp1;
  }

  static final HashMap<Character, Integer> digitsMap = getDigitsMap();

  public static final int parse(final String s) {
    int result = 0;
    final int strLength = s.length();
    {
      int i = 0;
      while ((i < strLength)) {
        {
          final char digit = s.charAt(i);
          {
            boolean tmp2 = digitsMap.containsKey(digit);
            if (tmp2) {
              final int digitVal = digitsMap.get(digit);
              result = ((10 * result) + digitVal);
            }
          }
        }
        i = (i + 1);
      }
      return result;
    }
  }

  public static final HashMap<String, ArrayList<Character>> getNumberSystemsMap() {
    HashMap<String, ArrayList<Character>> tmp3 = new HashMap<String, ArrayList<Character>>();
    ArrayList<Character> tmp4 = new ArrayList<Character>();
    tmp4.add('٠');
    tmp4.add('١');
    tmp4.add('٢');
    tmp4.add('٣');
    tmp4.add('٤');
    tmp4.add('٥');
    tmp4.add('٦');
    tmp4.add('٧');
    tmp4.add('٨');
    tmp4.add('٩');
    tmp3.put("ARABIC", tmp4);
    ArrayList<Character> tmp5 = new ArrayList<Character>();
    tmp5.add('০');
    tmp5.add('১');
    tmp5.add('২');
    tmp5.add('৩');
    tmp5.add('৪');
    tmp5.add('৫');
    tmp5.add('৬');
    tmp5.add('৭');
    tmp5.add('৮');
    tmp5.add('৯');
    tmp3.put("BENGALI", tmp5);
    ArrayList<Character> tmp6 = new ArrayList<Character>();
    tmp6.add('0');
    tmp6.add('1');
    tmp6.add('2');
    tmp6.add('3');
    tmp6.add('4');
    tmp6.add('5');
    tmp6.add('6');
    tmp6.add('7');
    tmp6.add('8');
    tmp6.add('9');
    tmp3.put("LATIN", tmp6);
    final HashMap<String, ArrayList<Character>> m = tmp3;
    return m;
  }

  static final HashMap<String, ArrayList<Character>> numberSystemsMap = getNumberSystemsMap();

  public static final HashMap<String, Character> getGroupingSeparatorsMap() {
    HashMap<String, Character> tmp7 = new HashMap<String, Character>();
    tmp7.put("ARABIC", '٬');
    tmp7.put("BENGALI", ',');
    tmp7.put("LATIN", ',');
    return tmp7;
  }

  static final HashMap<String, Character> groupingSeparatorsMap = getGroupingSeparatorsMap();

  public static final ArrayList<Integer> getSeparatorPositions(
      final int numLength, final String groupingStrategy) {
    ArrayList<Integer> result = new ArrayList<Integer>();
    {
      boolean tmp8 = groupingStrategy.equals("NONE");
      if (tmp8) {
        return result;
      } else {
        boolean tmp9 = groupingStrategy.equals("ON_ALIGNED_3_3");
        if (tmp9) {
          int i = (numLength - 3);
          {
            while ((0 < i)) {
              result.add(i);
              i = (i - 3);
            }
            return result;
          }
        } else {
          boolean tmp10 = groupingStrategy.equals("ON_ALIGNED_3_2");
          if (tmp10) {
            int i = (numLength - 3);
            {
              while ((0 < i)) {
                result.add(i);
                i = (i - 2);
              }
              return result;
            }
          } else {
            boolean tmp11 = groupingStrategy.equals("MIN_2");
            if (tmp11) {
              boolean tmp12 = (numLength <= 4);
              if (tmp12) {
                return result;
              } else {
                int i = (numLength - 3);
                {
                  while ((0 < i)) {
                    result.add(i);
                    i = (i - 3);
                  }
                  return result;
                }
              }
            } else {
              return result;
            }
          }
        }
      }
    }
  }

  public static final String format(
      final int num, final String numberSystem, final String groupingStrategy) {
    int i = num;
    java.lang.StringBuffer result = new StringBuffer();
    {
      while (!(i == 0)) {
        final int quotient = (i / 10);
        final int remainder = (i % 10);
        final ArrayList<Character> numberSystemDigits = numberSystemsMap.get(numberSystem);
        final char localDigit = numberSystemDigits.get(remainder);
        {
          result.insert(0, localDigit);
          i = quotient;
        }
      }
      {
        final char sep = groupingSeparatorsMap.get(numberSystem);
        final int numLength = result.length();
        final ArrayList<Integer> separatorPositions =
            getSeparatorPositions(numLength, groupingStrategy);
        final int numPositions = separatorPositions.size();
        int idx = 0;
        while ((idx < numPositions)) {
          {
            final int position = separatorPositions.get(idx);
            result.insert(position, sep);
          }
          idx = (idx + 1);
        }
      }
      return result.toString();
    }
  }

  public static final void main(String[] _args) {
    System.out.println(parse("٥٠٣٠١"));
    System.out.println(parse("৫০৩০১"));
    System.out.println(parse("7,654,321"));
    System.out.println(parse("76,54,321"));
    System.out.println(format(7654321, "LATIN", "ON_ALIGNED_3_2"));
    System.out.println(format(7654321, "ARABIC", "ON_ALIGNED_3_3"));
    System.out.println(format(7654321, "BENGALI", "ON_ALIGNED_3_3"));
  }
}
