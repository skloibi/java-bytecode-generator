package at.jku.ssw.java.bytecode.generator.utils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Provides functions to randomize code generation.
 */
public class Randomizer {

    private static final Random rand = new Random();

    /**
     * Executes the given number of suppliers in any order until a non-null
     * result is found. Each supplier is thereby executed at most once.
     *
     * @param suppliers The functions that are executed
     * @param <T>       The expected return type
     * @return The first result of the calls that is not null
     * or {@link Optional#EMPTY} if all results are null
     */
    @SafeVarargs
    public static <T> Optional<T> oneNotNullOf(Supplier<T>... suppliers) {
        return stream(suppliers)
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .findAny();
    }

    /**
     * Returns one of the given values.
     * If the picked values is null, a {@link NullPointerException} is
     * thrown.
     *
     * @param values The values
     * @param <T>    The type of the values
     * @return one of the given values, or {@link Optional#EMPTY} if
     * no values are given
     */
    @SafeVarargs
    public static <T> Optional<T> oneOf(T... values) {
        return skipRandom(values)
                .findAny();
    }

    /**
     * Executes one of the given procedures and returns the result.
     *
     * @param suppliers The functions that are executed
     * @param <T>       The expected return type
     * @return The result of the first executed supplier
     */
    @SafeVarargs
    public static <T> Optional<T> oneOf(Supplier<T>... suppliers) {
        return skipRandom(suppliers)
                .map(Supplier::get)
                .map(Optional::ofNullable)
                .findAny()
                .flatMap(Function.identity());
    }

    /**
     * Executes one of the given procedures.
     *
     * @param runnables The functions that are executed
     */
    public static void oneOf(Runnable... runnables) {
        skipRandom(runnables)
                .findAny()
                .ifPresent(Runnable::run);
    }

    /**
     * Executes on of the given procedures but uses the given number of
     * potential options to calculate the probability.
     * If the defined number of options exceeds the actually passed arguments,
     * the last argument is repeated to increase its chances.
     * If the defined number of options is lower than the passed arguments,
     * only the given number of functions are considered.
     *
     * @param options   The number of potential options
     * @param runnables The functions that are executed
     */
    public static void oneOfOptions(int options, Runnable... runnables) {
        if (runnables.length > 0 && options > 0) {
            Runnable repeated = runnables[runnables.length - 1];

            IntStream.range(0, options - runnables.length)
                    .mapToObj(__ -> Stream.of(repeated))
                    .reduce(Arrays.stream(runnables), Stream::concat)
                    .limit(options)
                    .skip(rand.nextInt(options))
                    .findAny()
                    .ifPresent(Runnable::run);
        }
    }

    /**
     * Returns a random stream of the given values.
     *
     * @param args The values to stream
     * @param <T>  The type of the elements
     * @return a random stream containing the given values
     */
    @SafeVarargs
    public static <T> Stream<T> stream(T... args) {
        List<T> l = Arrays.asList(args);

        Collections.shuffle(l);

        return l.stream();
    }

    /**
     * Skips a random number of the given values and returns a stream
     *
     * @param args The values to stream
     * @param <T>  The type of the elements
     * @return a random stream that skips a random amount of the given elements
     */
    @SafeVarargs
    public static <T> Stream<T> skipRandom(T... args) {
        return args.length == 0
                ? Stream.empty()
                : Arrays.stream(args).skip(rand.nextInt(args.length));
    }
}
