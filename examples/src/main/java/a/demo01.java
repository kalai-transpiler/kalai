package a;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
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