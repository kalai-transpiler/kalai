package b;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class TypeAlias {
static final HashMap<Long,String> x = new HashMap<Long,String>();
public static final HashMap<Long,String> f(final HashMap<Long,String> y) {
final HashMap<Long,String> z = y;
return z;
}
public static final void main(String[] args) {
System.out.println("OK");
}
}
