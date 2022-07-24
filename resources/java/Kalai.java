package kalai;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Kalai {
    public static <U, T> U foldLeft(Collection<T> sequence, U initial, BiFunction<U, ? super T, U> accumulator) {
        U result = initial;
        for (T element : sequence)
            result = accumulator.apply(result, element);
        return result;
    }

//    static <A, B> Stream<Pair<A,B>> zip(Stream<A> as, Stream<B> bs) {
//        Iterator<A> i1 = as.iterator();
//        Iterator<B> i2 = bs.iterator();
//        Iterable<Pair<A,B>> i=()->new Iterator<Pair<A,B>>() {
//            public boolean hasNext() {
//                return i1.hasNext() && i2.hasNext();
//            }
//            public Pair<A,B> next() {
//                return new Pair<A,B>(i1.next(), i2.next());
//            }
//        };
//        return StreamSupport.stream(i.spliterator(), false);
//    }

    /**
     * Can be viewed either as:
     * 1. a Clojure `map` invocation that takes 2 sequences
     * 2. a combination of a Rust/Scala `zip` that combines 2 sequences into one,
     *   followed by a map of the resulting sequence.
     * @param zipper the function applied to an element of `a` and an element of `b`
     *               to obtain the result of type `C` in the result output Stream.
     * @param a
     * @param b
     * @param <A>
     * @param <B>
     * @param <C>
     * @return
     */
    public static<A, B, C> Stream<C> map(BiFunction<? super A, ? super B, ? extends C> zipper,
                                         Stream<? extends A> a,
                                         Stream<? extends B> b) {
        Objects.requireNonNull(zipper);
        Spliterator<? extends A> aSpliterator = Objects.requireNonNull(a).spliterator();
        Spliterator<? extends B> bSpliterator = Objects.requireNonNull(b).spliterator();

        // Zipping looses DISTINCT and SORTED characteristics
        int characteristics = aSpliterator.characteristics() & bSpliterator.characteristics() &
                ~(Spliterator.DISTINCT | Spliterator.SORTED);

        long zipSize = ((characteristics & Spliterator.SIZED) != 0)
                ? Math.min(aSpliterator.getExactSizeIfKnown(), bSpliterator.getExactSizeIfKnown())
                : -1;

        Iterator<A> aIterator = Spliterators.iterator(aSpliterator);
        Iterator<B> bIterator = Spliterators.iterator(bSpliterator);
        Iterator<C> cIterator = new Iterator<C>() {
            @Override
            public boolean hasNext() {
                return aIterator.hasNext() && bIterator.hasNext();
            }

            @Override
            public C next() {
                return zipper.apply(aIterator.next(), bIterator.next());
            }
        };

        Spliterator<C> split = Spliterators.spliterator(cIterator, zipSize, characteristics);
        return (a.isParallel() || b.isParallel())
                ? StreamSupport.stream(split, true)
                : StreamSupport.stream(split, false);
    }
}
