package b;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class loop {
public static final int add(final int a, final int b) {
int i = 0;
while ((i < 10)) {
System.out.println(i);
i = ++i;
}
final ArrayList<Integer> tmp8 = new ArrayList<Integer>();
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
int tmp9;
if (true)
{
tmp9 = x;
}
else
{
tmp9 = (2 * (3 + 4) * 5 * 6 * 7);
}
return tmp9;
}
}
}
}