package sqlbuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class Core {
public static final ArrayList<String> format(final HashMap<String,String> queryMap, final HashMap<String,String> options) {
ArrayList<String> tmp1 = new ArrayList<String>();
tmp1.add("a");
tmp1.add("b");
tmp1.add("3");
return tmp1;
}
public static final ArrayList<String> formatNoOpts(final HashMap<String,String> queryMap) {
return sqlbuilder.Core.format(queryMap, new HashMap<String,String>());
}
}
