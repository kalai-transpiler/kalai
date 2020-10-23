package b;
import java.util.Vector;
import java.util.Map;
import java.util.HashSet;
public class loop {
public static final int add(final int a, final int b) {
int i = 0;
while ((i < 10)) {
System.out.println(i);
i = ++i;
}
final Vector<Integer> tmp8 = new Vector<Integer>();
tmp8.add(1);
tmp8.add(2);
tmp8.add(3);
for (int ii : tmp8) {
System.out.println(ii);
}
{
int x = 0;
{
while ((x < 10)) {
System.out.println(++x);
}
if (true)
{
return x;
}
else
{
return (2 * (3 + 4) * 5 * 6 * 7);
}
}
}
}
}