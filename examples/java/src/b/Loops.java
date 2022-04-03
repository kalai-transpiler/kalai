package b;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class Loops {
public static final void main(String[] args) {
int i1 = 0;
while ((i1 < 10)) {
System.out.println(i1);
i1 = (i1 + 1);
}
long i2 = 0L;
while ((i2 < 10L)) {
System.out.println(i2);
i2 = (i2 + 1L);
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
