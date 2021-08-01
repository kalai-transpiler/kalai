package b;

public class Variable {
  public static final long sideEffect() {
    long y = 2L;
    {
      y = 3L;
      return y;
    }
  }

  public static final void main(String[] args) {
    System.out.println(b.Variable.sideEffect());
  }
}
