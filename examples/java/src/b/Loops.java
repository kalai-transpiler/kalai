package b;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class Loops {
public static final void main(String[] args) {
long i = 0;
while ((i < 10)) {
System.out.println(i);
i = (i + 1);
}
ArrayList<Integer> tmp1 = new ArrayList<Integer>();
tmp1.add(1);
tmp1.add(2);
tmp1.add(3);
for (int ii : tmp1) {
System.out.println(ii);
}
{
int x = 0;
while ((x < 10)) {
x = (x + 1);
System.out.println(x);
}
}
}
}