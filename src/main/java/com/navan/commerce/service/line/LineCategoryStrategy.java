package com.navan.commerce.service.line;

import com.navan.commerce.domain.LineCategory;
import com.navan.commerce.dto.LineItem;
import com.navan.commerce.dto.CreateTransactionRequest;
import com.navan.commerce.dto.LineEntry;
import com.navan.commerce.dto.PatchTransactionRequest;
import com.navan.commerce.entity.TransactionLineEntity;

import java.util.List;

/**
 * Strategy: EVERYTHING the system knows about one line category lives in one implementation —
 * its wire list and DTO, entity conversion (including its category-specific fields in
 * {@code attributes} JSONB), response conversion, and invoice rollup rule. A new line category
 * (discount, …) = one new {@code @Component} implementing this interface; registry, services,
 * mappers, and the invoice flow pick it up automatically (OCP).
 *
 * The only thing a new category additionally needs is its wire field name (e.g. a future
 * {@code discount_lines} list) — that part is the API contract and cannot auto-appear.
 *
 * @param <T> this category's wire DTO (a {@link LineEntry} implementation)
 */
public interface LineCategoryStrategy<T extends LineEntry> {

    /** The line category this strategy owns (its registry key). */
    LineCategory category();

    /** This category's wire list from a create request (required list, may be empty). */
    List<T> extract(CreateTransactionRequest request);

    /** From a PATCH request — null means "field absent, leave stored rows untouched". */
    List<T> extract(PatchTransactionRequest request);

    /** Wire element → stored line row (category + per-category attributes applied). */
    TransactionLineEntity toEntity(T wire, String transactionId);

    /** Stored row → canonical {@link LineItem}; category-specific fields read back from attributes. */
    LineItem toLineItem(TransactionLineEntity line);

    /** Canonical line → wire response element (per-category shape, e.g. fees omit rate). */
    LineEntry toResponseItem(LineItem item);

    /** Invoice rollup rule over the invoice's frozen snapshot items (D9) — never live rows. */
    List<LineEntry> rollup(List<LineItem> items);

    /** Create flow: every wire element of this category → stored rows. */
    default List<TransactionLineEntity> toEntities(CreateTransactionRequest request, String transactionId) {
        return extract(request).stream().map(wire -> toEntity(wire, transactionId)).toList();
    }

    /** PATCH flow: null means "field absent, leave stored rows untouched". */
    default List<TransactionLineEntity> toEntities(PatchTransactionRequest request, String transactionId) {
        List<T> wire = extract(request);
        return wire == null ? null : wire.stream().map(w -> toEntity(w, transactionId)).toList();
    }
}
