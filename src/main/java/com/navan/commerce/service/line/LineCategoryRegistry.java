package com.navan.commerce.service.line;

import com.navan.commerce.domain.LineCategory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry over the per-category strategies. Spring injects every {@link LineCategoryStrategy}
 * bean, keyed by its category — adding a strategy class registers it automatically.
 * Callers iterate {@link #all()} (uniform handling) or address one category via {@link #of}.
 */
@Component
public class LineCategoryRegistry {

    private final Map<LineCategory, LineCategoryStrategy<?>> byCategory;

    public LineCategoryRegistry(Collection<LineCategoryStrategy<?>> strategies) {
        this.byCategory = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(LineCategoryStrategy::category, Function.identity()));
    }

    public Collection<LineCategoryStrategy<?>> all() {
        return byCategory.values();
    }

    public LineCategoryStrategy<?> of(LineCategory category) {
        // A missing strategy is a wiring bug, not user input — fail loud at development time.
        return Objects.requireNonNull(byCategory.get(category),
                "No LineCategoryStrategy registered for category " + category);
    }
}
