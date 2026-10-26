package com.navan.commerce.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Bounded page envelope. The shape hides whether offset or cursor drives pagination underneath,
 * so a cursor upgrade later would not break clients.
 */
@Getter
@Builder
public class PageResponse<T> {

    private final List<T> items;
    private final int limit;
    private final int offset;
    private final long total;
}
