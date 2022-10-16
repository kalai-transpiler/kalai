package b;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class HigherOrderFunction {
public static final void main(String[] _args) {
{
ArrayList<Long> tmp1 = new ArrayList<Long>();
tmp1.add(1L);
tmp1.add(2L);
tmp1.add(3L);
tmp1.add(4L);
tmp1.add(5L);
final ArrayList<Long> x = tmp1;
System.out.println(("" + "HELLO***" + x.stream().map((y) -> {
return (y + 1L);
}).findFirst().get()));
}
{
ArrayList<Long> tmp2 = new ArrayList<Long>();
tmp2.add(1L);
tmp2.add(2L);
tmp2.add(3L);
tmp2.add(4L);
tmp2.add(5L);
final ArrayList<Long> x = tmp2;
System.out.println(("" + "HELLO***" + x.stream().map((a) -> {
return (a + 1L);
}).findFirst().get()));
}
{
ArrayList<Long> tmp3 = new ArrayList<Long>();
tmp3.add(1L);
tmp3.add(2L);
tmp3.add(3L);
tmp3.add(4L);
tmp3.add(5L);
final ArrayList<Long> y = tmp3;
final long z = y.stream().reduce((a, b) -> {
return (a + b);
}).get();
final String z2 = kalai.Kalai.foldLeft(y, "", (a, b) -> {
return ("" + a + b);
});
{
System.out.println(("" + "z =" + z));
System.out.println(("" + "z2 =" + z2));
}
}
{
ArrayList<Long> tmp4 = new ArrayList<Long>();
tmp4.add(1L);
tmp4.add(2L);
tmp4.add(3L);
tmp4.add(4L);
tmp4.add(5L);
final ArrayList<Long> y = tmp4;
final long z = y.stream().reduce((a, b) -> {
return (a + b);
}).get();
final String z2 = kalai.Kalai.foldLeft(y, "", (a, b) -> {
return ("" + a + b);
});
{
System.out.println(("" + "z =" + z));
System.out.println(("" + "z2 =" + z2));
}
}
}
}
