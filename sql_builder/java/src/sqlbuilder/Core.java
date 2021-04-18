package sqlbuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class Core {
public static final TYPE_MISSING format(final TYPE_MISSING queryMap, final TYPE_MISSING options) {
TYPE_MISSING tmp1 = new TYPE_MISSING();
tmp1.add("a");
tmp1.add("b");
tmp1.add("3");
return tmp1;
}
public static final TYPE_MISSING formatNoOpts(final TYPE_MISSING queryMap) {
return sqlbuilder.Core.format(queryMap, new TYPE_MISSING());
}
}