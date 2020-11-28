package b;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class typeAlias {
static final HashMap<Long,String> x;
public static final HashMap<Long,String> f(final HashMap<Long,String> y) {
final HashMap<Long,String> z = y;
return z;
}
}