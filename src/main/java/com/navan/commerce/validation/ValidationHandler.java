package com.navan.commerce.validation;

/**
 * One rule in a validation chain (Chain of Responsibility, Spring-idiomatic: handlers are
 * {@code @Component}s ordered by {@link #order()}, executed in sequence by the flow's chain).
 * A handler throws an {@code ApiException} to fail the chain; otherwise it enriches the context
 * and the chain proceeds.
 *
 * @param <C> the flow's context type (accumulates what earlier handlers established)
 */
public interface ValidationHandler<C> {

    void handle(C context);

    /** Execution order within the chain (ascending). */
    int order();
}
