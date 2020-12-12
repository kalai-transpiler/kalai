package a;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class demo01 {
public static final String format(final int num) {
int i = num;
final java.lang.StringBuffer result = new StringBuffer();
{
while (!(i == 0)) {
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
format(2345);
System.out.println(format(2345));
}
}