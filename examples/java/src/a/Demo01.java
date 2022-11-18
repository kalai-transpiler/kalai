package a;

import kalai.Kalai.*;

public class Demo01 {
  public static final String format(final int num) {
    int i = num;
    java.lang.StringBuffer result = new StringBuffer();
    {
      while (!(i == 0)) {
        final int quotient = (i / 10);
        final int remainder = (i % 10);
        {
          result.insert(0, remainder);
          i = quotient;
        }
      }
      return result.toString();
    }
  }

  public static final void main(String[] _args) {
    format(2345);
    System.out.println(format(2345));
  }
}
