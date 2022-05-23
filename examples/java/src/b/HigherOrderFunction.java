package b;

import java.util.ArrayList;

public class HigherOrderFunction {
  public static final void main(String[] _args) {
    ArrayList<Long> tmp1 = new ArrayList<Long>();
    tmp1.add(1L);
    tmp1.add(2L);
    tmp1.add(3L);
    tmp1.add(4L);
    tmp1.add(5L);
    final ArrayList<Long> x = tmp1;
    System.out.println(
        (""
            + "HELLO***"
            + x.stream()
                .map(
                    (y) -> {
                      return (y + 1L);
                    })
                .findFirst()
                .get()));
  }
}
