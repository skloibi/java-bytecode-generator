package at.jku.ssw.java.bytecode.generator.metamodel.builders;

import at.jku.ssw.java.bytecode.generator.metamodel.Builder;
import at.jku.ssw.java.bytecode.generator.metamodel.expressions.Expression;
import at.jku.ssw.java.bytecode.generator.metamodel.expressions.operations.MethodCall;
import at.jku.ssw.java.bytecode.generator.types.base.MetaType;
import at.jku.ssw.java.bytecode.generator.types.base.RefType;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static at.jku.ssw.java.bytecode.generator.types.base.VoidType.VOID;

public interface MethodBuilder<T> extends Builder<T> {

    /**
     * The format pattern which is used when a method signature is printed.
     */
    String TO_STRING_FORMAT = "method %s %s %s.%s(%s)";

    /**
     * Returns the sender type of this method. This is either the type of the
     * instance on which the instance method may be called or the class
     * that defines the static method.
     *
     * @return a meta type that describes the sender reference type
     */
    RefType<?> sender();

    /**
     * Returns the name of the method (not to be confused with the descriptor
     * that also includes parameters).
     *
     * @return a string that describes the method's name
     */
    String name();

    /**
     * Returns the modifiers of this method (e.g. visibility, locks).
     *
     * @return an integer whose bit arrangement indicates the applied modifiers
     */
    int modifiers();

    /**
     * Checks whether this method returns {@code void}.
     *
     * @return {@code true} if this method is a {@code void} method;
     * {@code false} otherwise
     * @see at.jku.ssw.java.bytecode.generator.types.base.VoidType
     */
    default boolean isVoid() {
        return returns() == VOID;
    }

    /**
     * Checks whether this method is an instance method (non-static) or a class
     * method (static).
     *
     * @return {@code true} if this method is static; {@code false} otherwise
     */
    default boolean isStatic() {
        return Modifier.isStatic(modifiers());
    }

    /**
     * Returns the list of parameter types that this method requires.
     *
     * @return a list of meta types that describe this method's arguments
     */
    List<MetaType<?>> argumentTypes();

    /**
     * {@inheritDoc}
     */
    @Override
    default List<? extends MetaType<?>> requires() {
        return isStatic()
                ? argumentTypes()
                : Stream.concat(
                Stream.<MetaType<?>>of(sender()),
                argumentTypes().stream()
        ).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default Expression<T> build(List<? extends Expression<?>> params) {
        if (isStatic())
            return new MethodCall.Static<>(
                    name(),
                    returns(),
                    this::sender,
                    params
            );

        // at least one parameter must be given (the instance)
        assert params.size() > 0;

        return new MethodCall<>(
                name(),
                returns(),
                params.get(0),
                params.subList(1, params.size())
        );
    }
}
