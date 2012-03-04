package com.orchid.collections;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

/**
 * User: Igor Petruk
 * Date: 26.12.11
 * Time: 10:27
 */
public class  ArrayBackedList<T> extends AbstractList<T> {
    T[] array;
    int size;
    CollectionElementProvider<T> elementProvider;
    Class<T> klass;

    @SuppressWarnings("unchecked")
    public ArrayBackedList(Class<T> klass,
                           CollectionElementProvider<T> elementProvider,
                           int capacity) {
        array = (T[])Array.newInstance(klass,capacity);
        for (int i = 0; i<capacity; i++){
            array[i] = elementProvider.allocate();
        }
        this.elementProvider = elementProvider;
        size = capacity;
        this.klass = klass;
    }

    @Override
    public T get(int i) {
        return array[i];
    }

    public T extend() {
        T[] newArray = Arrays.copyOf(array, size+1);
        T o = newArray[size] = elementProvider.allocate();
        size++;
        array = newArray;
        return o;
    }

    @Override
    public T set(int index, T element) {
        T ar = array[index];
        array[index] = element;
        return ar;
    }

    @Override
    public int size() {
        return size;
    }

    public T[] getBackingArray(){
        return array;
    }

    public void dispose(){
        for (int i = 0; i<size; i++){
            elementProvider.free(array[i]);
        }
    }
}
