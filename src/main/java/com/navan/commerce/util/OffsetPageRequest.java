package com.navan.commerce.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;

/**
 * True offset/limit {@link Pageable}. Spring's {@code PageRequest} is page-index based and cannot
 * express an arbitrary {@code offset} query param; this emits {@code LIMIT ? OFFSET ?} directly,
 * so pagination is always pushed to the database — never in memory (spec §5).
 */
public class OffsetPageRequest implements Pageable {

    private final int limit;
    private final long offset;
    private final Sort sort;

    public OffsetPageRequest(int limit, long offset, Sort sort) {
        this.limit = limit;
        this.offset = offset;
        this.sort = sort;
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(limit, offset + limit, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return offset >= limit ? new OffsetPageRequest(limit, offset - limit, sort) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(limit, 0, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest(limit, (long) pageNumber * limit, sort);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OffsetPageRequest other)) {
            return false;
        }
        return limit == other.limit && offset == other.offset && sort.equals(other.sort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit, offset, sort);
    }
}
