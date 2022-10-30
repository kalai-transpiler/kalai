package c;

public class ConjAsFunction {
  public static final long conjMapMap() {
    final Object a =
        new io.lacuna.bifurcan.Map<Object, Object>()
            .put(":a", 1L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
    final Object b =
        new io.lacuna.bifurcan.Map<Object, Object>()
            .put(":b", 2L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
    final Object c = clojure.Core.conj(a, b);
    return 3L;
  }

  public static final long conjMapVec() {
    final Object a =
        new io.lacuna.bifurcan.Map<Object, Object>()
            .put(":a", 1L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
    final Object b = new io.lacuna.bifurcan.List<Object>().addLast(":b").addLast(2L);
    final Object c = clojure.Core.conj(a, b);
    return 11L;
  }

  public static final long conjSet() {
    final Object a = new io.lacuna.bifurcan.Set<Object>().add(":a").add(":b");
    final Object newValue = ":c";
    final Object c = clojure.Core.conj(a, newValue);
    return 5L;
  }

  public static final long conjVector() {
    final Object a = new io.lacuna.bifurcan.List<Object>().addLast(":a").addLast(":b");
    final Object newValue = ":c";
    final Object c = clojure.Core.conj(a, newValue);
    return 7L;
  }

  public static final long typeConversions() {
    final io.lacuna.bifurcan.Map<Object, Object> a =
        new io.lacuna.bifurcan.Map<Object, Object>()
            .put(":a", 1L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
    final io.lacuna.bifurcan.Map<Object, Object> b =
        new io.lacuna.bifurcan.Map<Object, Object>()
            .put(":b", 1L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
    {
      clojure.Core.conj((Object) a, (Object) b);
      return 4L;
    }
  }

  public static final void main(String[] _args) {
    System.out.println(c.ConjAsFunction.conjMapMap());
    System.out.println(c.ConjAsFunction.conjMapVec());
    System.out.println(c.ConjAsFunction.conjSet());
    System.out.println(c.ConjAsFunction.conjVector());
    System.out.println(c.ConjAsFunction.typeConversions());
  }
}
