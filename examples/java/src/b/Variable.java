package b;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class Variable {
public static final long sideEffect() {
long y = 2;
{
y = 3;
return (y + 4);
}
}
}