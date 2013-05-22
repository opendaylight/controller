package org.opendaylight.controller.yang.binding;

public interface Augmentable<T> {

    <E extends Augmentation<T>> E getAugmentation(Class<E> augmentationType);
}
