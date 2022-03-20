package c;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class Persistent {
public static final long testMap() {
HashMap<String,Long> tmp1 = new HashMap<String,Long>();
tmp1.put(":x", 11L);
tmp1.put(":y", 13L);
final HashMap<String,Long> a = tmp1;
HashMap<Object,Object> tmp2 = new HashMap<Object,Object>();
tmp2.put(":x", 11L);
tmp2.put(":y", 13L);
final HashMap<Object,Object> b = tmp2;
final io.lacuna.bifurcan.Map<String,Long> c = new io.lacuna.bifurcan.Map<String,Long>().put(":x", 11L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put(":y", 13L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
final io.lacuna.bifurcan.Map<Object,Object> d = new io.lacuna.bifurcan.Map<Object,Object>().put(":x", 11L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put(":y", 13L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
final Object e = {:y 13, :x 11};
{
System.out.println(("" + "key :y in mutable map a returns " + a.get(":y")));
{
final Object anyY = ":y";
final long getBAnyY = (long)b.get(anyY);
System.out.println(("" + "key :y in mutable map b returns " + getBAnyY));
}
System.out.println(("" + "key :y in persistent map c returns " + c.get(":y", null)));
{
final Object anyY = ":y";
final long getDAnyY = (long)d.get(anyY, null);
System.out.println(("" + "key :y in persistent map d returns " + getDAnyY));
}
{
final Object anyY = ":y";
final io.lacuna.bifurcan.Map<Object,Object> eMap = (io.lacuna.bifurcan.Map<Object,Object>)e;
final long getEAnyY = (long)eMap.get(anyY, null);
System.out.println(("" + "key :y in persistent map e returns " + getEAnyY));
}
return 3L;
}
}
public static final long testVector() {
ArrayList<Long> tmp3 = new ArrayList<Long>();
tmp3.add(11L);
tmp3.add(13L);
final ArrayList<Long> a = tmp3;
ArrayList<Object> tmp4 = new ArrayList<Object>();
tmp4.add(11L);
tmp4.add(13L);
final ArrayList<Object> b = tmp4;
final io.lacuna.bifurcan.List<Long> c = new io.lacuna.bifurcan.List<Long>().addLast(11L).addLast(13L);
final io.lacuna.bifurcan.List<Object> d = new io.lacuna.bifurcan.List<Object>().addLast(11L).addLast(13L);
{
System.out.println(("" + "size of mutable vector a returns " + a.size()));
System.out.println(("" + "size of mutable vector b returns " + b.size()));
System.out.println(("" + "size of persistent vector c returns " + c.size()));
System.out.println(("" + "size of persistent vector d returns " + d.size()));
return 5L;
}
}
public static final long testSet() {
HashSet<Long> tmp5 = new HashSet<Long>();
tmp5.add(11L);
tmp5.add(13L);
tmp5.add(15L);
final HashSet<Long> a = tmp5;
HashSet<Object> tmp6 = new HashSet<Object>();
tmp6.add(11L);
tmp6.add(13L);
tmp6.add(15L);
final HashSet<Object> b = tmp6;
final io.lacuna.bifurcan.Set<Long> c = new io.lacuna.bifurcan.Set<Long>().add(11L).add(13L).add(15L);
final io.lacuna.bifurcan.Set<Object> d = new io.lacuna.bifurcan.Set<Object>().add(11L).add(13L).add(15L);
{
System.out.println(("" + "size of mutable set a returns " + a.size()));
System.out.println(("" + "size of mutable set b returns " + b.size()));
System.out.println(("" + "size of persistent set c returns " + c.size()));
System.out.println(("" + "size of persistent set d returns " + d.size()));
return 7L;
}
}
public static final void main(String[] args) {
System.out.println(c.Persistent.testMap());
System.out.println(c.Persistent.testVector());
System.out.println(c.Persistent.testSet());
}
}
