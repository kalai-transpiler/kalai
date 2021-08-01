package b;

public class Simple {
  public static final long add(final long a, final long b) {
    return (a + b);
  }

  public static final void main(String[] args) {
    System.out.println(b.Simple.add(1L, 2L));
  }
}
