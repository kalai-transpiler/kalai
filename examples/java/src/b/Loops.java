package b;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class Loops {
public static final void main(String[] args) {
long i = 0;
while ((i < 10L)) {
System.out.println(i);
i = (i + 1);
}
ArrayList<Long> tmp1 = new ArrayList<Long>();
tmp1.add(1L);
tmp1.add(2L);
tmp1.add(3L);
for (long ii : tmp1) {
System.out.println(ii);
}
{
long x = 0L;
while ((x < 10L)) {
x = (x + 1L);
System.out.println(x);
}
}
}
}
