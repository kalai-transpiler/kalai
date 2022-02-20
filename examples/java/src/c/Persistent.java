package c;

import java.util.HashMap;

public class Persistent {
  public static final long testConj() {
    HashMap<String, Long> tmp1 = new HashMap<String, Long>();
    tmp1.put(":x", 11L);
    tmp1.put(":y", 13L);
    final HashMap<String, Long> a = tmp1;
    HashMap<Object, Object> tmp2 = new HashMap<Object, Object>();
    tmp2.put(":x", 11L);
    tmp2.put(":y", 13L);
    final HashMap<Object, Object> b = tmp2;
    io.lacuna.bifurcan.Map<String, Long> tmp3 = new io.lacuna.bifurcan.Map<String, Long>();
    tmp3.put(":x", 11L);
    tmp3.put(":y", 13L);
    final io.lacuna.bifurcan.Map<String, Long> c = tmp3;
    io.lacuna.bifurcan.Map<Object, Object> tmp4 = new io.lacuna.bifurcan.Map<Object, Object>();
    tmp4.put(":x", 11L);
    tmp4.put(":y", 13L);
    final io.lacuna.bifurcan.Map<Object, Object> d = tmp4;
    return 3L;
  }

  public static final void main(String[] args) {
    System.out.println(c.Persistent.testConj());
  }
}
