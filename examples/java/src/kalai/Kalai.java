package kalai;

import java.util.Collection;
import java.util.function.BiFunction;

public class Kalai {
  public static <U, T> U foldLeft(
      Collection<T> sequence, U initial, BiFunction<U, ? super T, U> accumulator) {
    U result = initial;
    for (T element : sequence) result = accumulator.apply(result, element);
    return result;
  }
}
