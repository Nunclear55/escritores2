package com.nunclear.escritores.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

/**
 * Helper methods to construct {@link Pageable} instances based on page, size,
 * and sort parameters.  This utility centralizes paging logic that was
 * previously duplicated across several service classes.  Clients supply a
 * mapping function to convert the requested sort field into the corresponding
 * database column name.
 */
public final class PaginationUtils {
    private PaginationUtils() {
        // prevent instantiation
    }

    /**
     * Builds a {@link Pageable} with the given page number, page size, and sort
     * specification.  The sort string should be of the form "field,direction"
     * (e.g., "name,asc" or "createdAt,desc").  If no direction is provided,
     * ascending order is assumed.  The {@code fieldMapper} is used to map the
     * requested field name to the corresponding persistent property.
     *
     * @param page the zero-based page index
     * @param size the number of elements in a page
     * @param sort the sort specification
     * @param fieldMapper function mapping API sort fields to entity property names
     * @return a pageable object configured with sorting
     */
    public static Pageable buildPageable(int page, int size, String sort, Function<String, String> fieldMapper) {
        String[] sortParts = sort.split(",");
        String field = sortParts[0];
        Sort.Direction direction =
                sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
        String mapped = fieldMapper != null ? fieldMapper.apply(field) : field;
        return PageRequest.of(page, size, Sort.by(direction, mapped));
    }
}