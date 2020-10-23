package a;
import java.util.Vector;
import java.util.Map;
import java.util.HashSet;
public class demo01 {
public static final String format(final Integer num) {
Integer i = num;
final StringBuffer result = new StringBuffer();
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
}