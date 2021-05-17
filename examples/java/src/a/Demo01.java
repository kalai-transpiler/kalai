package a;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class Demo01 {
public static final String format(final int num) {
int i = num;
java.lang.StringBuffer result = new StringBuffer();
{
while (!(i == 0L)) {
final int quotient = (i / 10);
final int remainder = (i % 10);
{
result.insert(0, remainder);
i = quotient;
}
}
return result.toString();
}
}
public static final void main(String[] args) {
a.Demo01.format(2345);
System.out.println(a.Demo01.format(2345));
}
}
