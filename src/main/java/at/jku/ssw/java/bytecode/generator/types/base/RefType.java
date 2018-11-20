package at.jku.ssw.java.bytecode.generator.types.base;

import at.jku.ssw.java.bytecode.generator.logger.FieldVarLogger;
import at.jku.ssw.java.bytecode.generator.metamodel.base.Builder;
import at.jku.ssw.java.bytecode.generator.metamodel.base.DefaultConstructorBuilder;
import at.jku.ssw.java.bytecode.generator.metamodel.base.NullBuilder;
import at.jku.ssw.java.bytecode.generator.types.TypeCache;
import at.jku.ssw.java.bytecode.generator.utils.JavassistUtils;
import javassist.CtClass;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static at.jku.ssw.java.bytecode.generator.types.base.MetaType.Kind.INSTANCE;
import static at.jku.ssw.java.bytecode.generator.utils.StatementDSL.Conditions.notNull;
import static at.jku.ssw.java.bytecode.generator.utils.StatementDSL.method;
import static at.jku.ssw.java.bytecode.generator.utils.StatementDSL.ternary;

/**
 * Meta type which generally describes reference types such as objects
 * of various kinds and arrays.
 *
 * @param <T> The actual Java class associated with this type
 */
public class RefType<T> extends MetaType<T> {
    //-------------------------------------------------------------------------
    // region Initialization

    /**
     * Initializes a new reference type based on the given class.
     *
     * @param clazz The class to base the reference type on
     * @param <T>   The type corresponding to the Java class type
     */
    public static <T> RefType<T> of(Class<T> clazz) {
        assert !clazz.isPrimitive();
        return new RefType<>(clazz);
    }

    /**
     * Generates a new reference type by inferring the remaining properties
     * from the given class type.
     *
     * @param clazz The actual Java class type instance corresponding to
     *              this {@link MetaType}.
     */
    protected RefType(Class<T> clazz) {
        super(clazz, JavassistUtils.toCtClass(clazz), INSTANCE);
    }

    /**
     * Creates a new reference type with the given properties.
     *
     * @param clazz     The actual Java class type instance.
     * @param clazzType The Javassist equivalent
     * @param kind      The kind of the type
     */
    protected RefType(Class<T> clazz, CtClass clazzType, Kind kind) {
        super(clazz, clazzType, kind);
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Overridden methods

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHashCode(FieldVarLogger<T> variable) {
        String name = variable.access();

        if (clazz.equals(String.class)) {
            return ternary(
                    notNull(name),
                    method(name, "hashCode"),
                    "0L"
            );
        } else if (clazz.equals(Date.class)) {
            return ternary(
                    notNull(name),
                    method(name, "getTime"),
                    "0L"
            );
        }

        // otherwise get the hash code of the class name
        return ternary(
                notNull(name),
                method(method(method(name, "getClass"), "getSimpleName"), "hashCode"),
                "0L"
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAssignableFrom(MetaType<?> other) {
        return other instanceof RefType && clazz.isAssignableFrom(other.clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends RefType<?>> getAssignableTypes() {
        return TypeCache.INSTANCE.refTypes()
                .filter(this::isAssignableFrom)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isPrimitive() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isRef() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isArray() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isVoid() {
        return false;
    }

    // endregion
    //-------------------------------------------------------------------------
    // region Builder methods

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Builder<T>> builders() {
        // TODO dynamically determine (via reflection?)!
        return Arrays.asList(
                new NullBuilder<>(this),
                new DefaultConstructorBuilder<>(this)
        );
    }

    // endregion
    //-------------------------------------------------------------------------
}
