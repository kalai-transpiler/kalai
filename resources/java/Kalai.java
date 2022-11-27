package kalai;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import io.lacuna.bifurcan.*;

/**
 * Kalai helper code for Java.
 */
public class Kalai {

    /**
     * Not currently supported in the Java Stream interface set of methods (as of Java 18), but is necessary
     * to support `reduce`, `merge`, etc. in Clojure.
     * `reduce` in Clojure is effectively `foldLeft` in other languages like Scala/Rust/Haskell, etc.
     * @param sequence
     * @param initial
     * @param accumulator
     * @param <U>
     * @param <T>
     * @return
     */
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
     * The Java Stream interface has a `map` method, but that implicitly operates on a
     * single Stream (which maps to a Clojure sequence). But Clojure also supports `map`
     * over many sequences. We at least would like to support `map` over 2 sequences
     * (which means the provided function accepts 2 arguments), which requires the extra work
     * below.
     *
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

    /**
     * `conj` can operate on many types of collections and new element types.
     * This implementation function of `conj` supports when `conj` accepts a Map type
     * collection with a new element type of Map.
     * @param m1
     * @param m2
     * @param <K>
     * @param <V>
     * @return
     */
    public static io.lacuna.bifurcan.Map conjImpl(io.lacuna.bifurcan.Map m1, io.lacuna.bifurcan.Map m2) {
        m1.merge(m2, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
        return m1;
    }

    public static io.lacuna.bifurcan.Map conjImpl(io.lacuna.bifurcan.Map m1, io.lacuna.bifurcan.List v1) {
        Object k = v1.nth(0);
        Object v = v1.nth(1);
        m1.put(k, v);
        return m1;
    }

    public static io.lacuna.bifurcan.List conjImpl(io.lacuna.bifurcan.List v1, Object elem) {
        v1.addLast(elem);
        return v1;
    }

    public static io.lacuna.bifurcan.Set conjImpl(io.lacuna.bifurcan.Set s1, Object elem) {
        s1.add(elem);
        return s1;
    }

    public static Object conj(Object o1, Object o2) {
        if ((o1 instanceof io.lacuna.bifurcan.Map) && (o2 instanceof io.lacuna.bifurcan.Map)) {
            return conjImpl((io.lacuna.bifurcan.Map) o1, (io.lacuna.bifurcan.Map) o2);
        } else if ((o1 instanceof io.lacuna.bifurcan.Map) && (o2 instanceof io.lacuna.bifurcan.List)) {
            return conjImpl((io.lacuna.bifurcan.Map) o1, (io.lacuna.bifurcan.List) o2);
        } else if (o1 instanceof io.lacuna.bifurcan.List) {
            return conjImpl((io.lacuna.bifurcan.List) o1, o2);
        } else if (o1 instanceof io.lacuna.bifurcan.Set) {
            return conjImpl((io.lacuna.bifurcan.Set) o1, o2);
        } else {
            return null;
        }
    }

    public static boolean empty(Iterable coll) {
        return coll.iterator().hasNext();
    }

    public static boolean empty(java.util.Collection coll) {
        return coll.isEmpty();
    }

    public static boolean notEmpty(Iterable coll) {
        return !empty(coll);
    }

    public static boolean notEmpty(java.util.Collection coll) {
        return !empty(coll);
    }
}
