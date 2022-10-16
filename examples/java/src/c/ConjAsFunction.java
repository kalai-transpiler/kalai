package c;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class ConjAsFunction {
public static final long testMap() {
final Object a = new io.lacuna.bifurcan.Map<Object,Object>().put(":a", 1L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
final Object b = new io.lacuna.bifurcan.Map<Object,Object>().put(":b", 2L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
final Object c = clojure.Core.conj(a, b);
return 3L;
}
public static final void main(String[] _args) {
System.out.println(c.ConjAsFunction.testMap());
}
}
