import java.util.function.Function;
public class foo {
	public static void main(String[] args) {
		System.out.println("hi");
		if (true) {
			Function<Integer,Integer> f = (Integer x)->2*x;
			f.apply(3);
		}
		Function<Boolean,Integer> test = (x)->{if (x) return 1; else return 2;};
		int z = test.apply(true);

		int i = (true?1:(false?2:3));
		System.out.println("hi" + i);
	}
}
