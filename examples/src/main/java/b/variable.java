package b;
import java.util.Vector;
import java.util.Map;
import java.util.HashSet;
public class variable {
public static final Long sideEffect() {
long y = 2;
{
y = 3;
y = (y + 4);
return y;
}
}
}