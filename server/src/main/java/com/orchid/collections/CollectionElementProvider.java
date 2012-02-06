package com.orchid.collections;

/**
 * User: Igor Petruk
 * Date: 26.12.11
 * Time: 10:37
 */
public interface CollectionElementProvider<T> {
    public T allocate();
    public void free(T buffer);
}
