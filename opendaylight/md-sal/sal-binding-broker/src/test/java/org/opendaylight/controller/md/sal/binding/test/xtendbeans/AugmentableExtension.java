/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.xtendbeans;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.eclipse.xtext.xbase.lib.util.ReflectExtensions;
import org.opendaylight.yangtools.binding.data.codec.util.AugmentationReader;
import org.opendaylight.yangtools.yang.binding.Augmentable;
import org.opendaylight.yangtools.yang.binding.Augmentation;

/**
 * Adds an {@link #getAugmentations(Augmentable)} method to {@link Augmentable}.
 *
 * <p>Note that the generated *Impl classes in the *Builder do not implement
 * {@link AugmentationReader}, only the {@link LazyDataObject} does.
 *
 * @see Augmentable
 * @see AugmentationReader
 *
 * @author Michael Vorburger
 */
public class AugmentableExtension {

    private static final ReflectExtensions REFLECT_EXTENSIONS = new ReflectExtensions();

    public ClassToInstanceMap<Augmentation<?>> getAugmentations(Augmentable<?> augmentable) {
        if (augmentable instanceof AugmentationReader) {
            AugmentationReader augmentationReader = (AugmentationReader) augmentable;
            Map<Class<? extends Augmentation<?>>, Augmentation<?>> augmentations = augmentationReader.getAugmentations(augmentable);
            return ImmutableClassToInstanceMap.copyOf(augmentations);
        } else if (Proxy.isProxyClass(augmentable.getClass())) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(augmentable);
            // class LazyDataObject implements InvocationHandler, AugmentationReader
            AugmentationReader augmentationReader = (AugmentationReader) invocationHandler;
            Map<Class<? extends Augmentation<?>>, Augmentation<?>> augmentations = augmentationReader.getAugmentations(augmentable);
            return ImmutableClassToInstanceMap.copyOf(augmentations);
        } else {
            try {
                Map<Class<? extends Augmentation<?>>, Augmentation<?>> augmentations = REFLECT_EXTENSIONS.get(augmentable, "augmentation");
                return ImmutableClassToInstanceMap.copyOf(augmentations);
            } catch (ClassCastException | SecurityException | NoSuchFieldException | IllegalArgumentException
                    | IllegalAccessException e) {
                throw new IllegalArgumentException("TODO Implement getAugmentations() for an Augmentable which "
                        + "is neither a (Proxy of an) AugmentationReader nor has an internal field named 'augmentation': "
                        + augmentable.getClass(), e);
            }
        }
    }

}
