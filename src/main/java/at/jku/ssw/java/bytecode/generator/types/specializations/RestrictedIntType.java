package at.jku.ssw.java.bytecode.generator.types.specializations;

import at.jku.ssw.java.bytecode.generator.types.base.MetaType;
import at.jku.ssw.java.bytecode.generator.types.base.PrimitiveType;
import at.jku.ssw.java.bytecode.generator.utils.IntRange;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a restricted version of an integer.
 * To check whether a value is within this type's range, the following checks
 * are performed in order:
 * <ul>
 * <li>if a value is given as an option it is valid</li>
 * <li>if a value is below the minimum it is invalid</li>
 * <li>if a value is above the maximum it is invalid</li>
 * <li>if a value is not excluded it is valid</li>
 * </ul>
 * E.g. assuming
 * {@code options == {-100, 100}}
 * {@code min == 0}
 * {@code max == 100}
 * {@code exclusions == {0, 100}}
 * (where {@code {a, b}} denotes a set where a and b are set)
 * (where {@code {a, b}} denotes a set where a and b are set)
 * then -100, 100, 1, 2, 99 are valid, while {@code -1, 0, 101} are excluded.
 */
public class RestrictedIntType extends PrimitiveType<Integer> {
    //-------------------------------------------------------------------------
    // region Type constants

    /**
     * "Restricted" integer that is actually comparable to integer.
     */
    public static final RestrictedIntType INT = RestrictedIntType.of(
            IntRange.rangeIncl(Integer.MIN_VALUE, Integer.MAX_VALUE)
    );

    // endregion
    //-------------------------------------------------------------------------
    // region Properties

    /**
     * Optional value range.
     */
    private final IntRange range;

    /**
     * Optional specific exclusions (e.g. excluding {@code 0}).
     */
    private final Set<Integer> exclusions;

    /**
     * Optional specific inclusions
     */
    private final Set<Integer> inclusions;

    // endregion
    //-------------------------------------------------------------------------
    // region Initialization

    /**
     * Generates a new restricted int type based on the given properties.
     *
     * @param range      The value range
     * @param exclusions Optional exclusions
     * @param inclusions Optional restrictions
     */
    private RestrictedIntType(IntRange range,
                              Set<Integer> exclusions,
                              Set<Integer> inclusions) {
        super(int.class, Kind.RINT);

        if (range == null && (inclusions == null || inclusions.isEmpty()))
            throw new IllegalArgumentException("Cannot create restricted type with neither range nor options");

        this.range = range;
        this.exclusions = exclusions == null
                ? new HashSet<>()
                : exclusions;
        this.inclusions = inclusions == null
                ? new HashSet<>()
                : inclusions;
    }

    /**
     * Creates a new restricted integer type that is specified by a range.
     * The range must not be {@code null}.
     *
     * @param range The value range
     * @return a new restricted type that limits the integer type to the given
     * value range
     */
    public static RestrictedIntType of(IntRange range) {
        assert range != null;
        return new RestrictedIntType(range, null, null);
    }

    /**
     * Creates a new restricted integer type that only consists of the given
     * values. The set must neither be {@code null} nor empty.
     *
     * @param inclusions The inclusions to restrict this type to
     * @return a new restricted type that limits the integer type to the given
     * values
     */
    public static RestrictedIntType of(Set<Integer> inclusions) {
        assert inclusions != null;
        assert !inclusions.isEmpty();
        return new RestrictedIntType(null, null, inclusions);
    }

    /**
     * Creates a new restricted integer type based on the given options.
     * In order for the type to be valid, either an inclusion or a range must
     * be provided.
     *
     * @param range      The optional value range
     * @param exclusions The optional exlusions.
     * @param inclusions The optional inclusions.
     * @return a new restricted type that limits the integer type to
     * the given values
     */
    public static RestrictedIntType of(IntRange range,
                                       Set<Integer> exclusions,
                                       Set<Integer> inclusions) {
        return new RestrictedIntType(range, exclusions, inclusions);
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Object methods

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RestrictedIntType that = (RestrictedIntType) o;
        return Objects.equals(range, that.range) &&
                Objects.equals(exclusions, that.exclusions) &&
                Objects.equals(inclusions, that.inclusions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return descriptor().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String descriptor() {
        StringBuilder str = new StringBuilder(super.descriptor());

        if (range != null) {
            str.append("[")
                    .append(range.min)
                    .append(", ")
                    .append(range.max)
                    .append("]");
        }

        if (inclusions != null) {
            str.append(" w/ ")
                    .append("[")
                    .append(inclusions.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(" ")))
                    .append("]");
        }

        if (exclusions != null) {
            str.append(" w/o ")
                    .append("[")
                    .append(exclusions.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(" ")))
                    .append("]");
        }

        return str.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return descriptor();
    }

    // endregion
    //-------------------------------------------------------------------------
    // region MetaType methods

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAssignableFrom(MetaType<?> other) {
        if (!(other instanceof RestrictedIntType))
            return false;

        RestrictedIntType o = (RestrictedIntType) other;

        /*
            If this type exceeds the other type in every aspect,
            it is assignable:
            - the other type's range is included in this range (or is null)
            - there are no exclusions that are valid in the other type
            - this maximum must be at least the other maximum
            - the other minimum must not be less than this minimum
            - the other options must all be valid in this type
        */
        return (o.range == null || range != null && range.contains(o.range)) &&
                exclusions.stream().noneMatch(o::isValid) &&
                o.inclusions.stream().allMatch(this::isValid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends RestrictedIntType> getAssignableTypes() {
        // since restricted types are compatible with potentially infinitely
        // many other restricted types, only the same type is returned here
        return Collections.singletonList(this);
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Utilities

    /**
     * Determines whether this integer is valid for this restricted type
     * (i.e. if it is either especially selected as an option or not
     * restricted and within the given boundaries).
     *
     * @param i The integer to check
     * @return {@code true} if this integer is valid for this type;
     * {@code false} otherwise
     */
    public final boolean isValid(int i) {
        return inclusions.contains(i) ||
                range != null && range.contains(i) && !exclusions.contains(i);
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Property accessors

    /**
     * Returns the value range (if any)
     *
     * @return the range or {@code null} if this type has no range
     */
    public IntRange getRange() {
        return range;
    }

    /**
     * Returns the excluded values.
     *
     * @return a set of all excluded values or {@code null} if
     * this type does not feature exclusions
     */
    public Set<Integer> getExclusions() {
        return new HashSet<>(exclusions);
    }

    /**
     * Returns the explicitly included values.
     *
     * @return a set of all special inclusions or {@code null} if
     * this type does not feature inclusions
     */
    public Set<Integer> getInclusions() {
        return new HashSet<>(inclusions);
    }

    // endregion
    //-------------------------------------------------------------------------
}
