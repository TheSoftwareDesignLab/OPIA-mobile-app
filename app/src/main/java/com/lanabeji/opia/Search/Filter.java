package com.lanabeji.opia.Search;

/**
 * Created by lanabeji on 30/03/19.
 */

import java.util.LinkedList;

/** Filters objects of type T. */
public abstract class Filter<T> {
    /**
     * Returns whether the specified object matches the filter.
     *
     * @param obj The object to filter.
     * @return {@code true} if the object is accepted.
     */
    public abstract boolean accept(T obj);

    /**
     * Returns the logical AND of this and the specified filter.
     *
     * @param filter The filter to AND this filter with.
     * @return A filter where calling <code>accept()</code> returns the result of <code>
     *     (this.accept() &amp;&amp; filter.accept())</code>.
     */
    public Filter<T> and(Filter<T> filter) {
        if (filter == null) {
            return this;
        }

        return new FilterAnd<T>(this, filter);
    }

    /**
     * Returns the logical OR of this and the specified filter.
     *
     * @param filter The filter to OR this filter with.
     * @return A filter where calling <code>accept()</code> returns the result of <code>
     *     (this.accept() || filter.accept())</code>.
     */
    public Filter<T> or(Filter<T> filter) {
        if (filter == null) {
            return this;
        }

        return new FilterOr<T>(this, filter);
    }

    private static class FilterAnd<T> extends Filter<T> {
        private final LinkedList<Filter<T>> mFilters = new LinkedList<>();

        public FilterAnd(Filter<T> lhs, Filter<T> rhs) {
            mFilters.add(lhs);
            mFilters.add(rhs);
        }

        @Override
        public boolean accept(T obj) {
            for (Filter<T> filter : mFilters) {
                if (!filter.accept(obj)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public Filter<T> and(Filter<T> filter) {
            mFilters.add(filter);

            return this;
        }
    }

    private static class FilterOr<T> extends Filter<T> {
        private final LinkedList<Filter<T>> mFilters = new LinkedList<>();

        public FilterOr(Filter<T> lhs, Filter<T> rhs) {
            mFilters.add(lhs);
            mFilters.add(rhs);
        }

        @Override
        public boolean accept(T obj) {
            for (Filter<T> filter : mFilters) {
                if (filter.accept(obj)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public Filter<T> or(Filter<T> filter) {
            mFilters.add(filter);

            return this;
        }
    }
}
