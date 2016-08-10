/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import java.util.Optional;

/**
 * Something to look up Object instances in.
 *
 * <p>
 * Elsewhere also known as a Guice Injector, Context in the Spring Framework or
 * JNDI, Dagger Component, etc.
 *
 * @author Michael Vorburger
 */
public interface ObjectRegistry {

    interface Builder {
        <T> void putInstance(T object, Class<T> lookupType) throws IllegalArgumentException;

        ObjectRegistry build();
    }

    <T> Optional<T> getInstanceOptional(Class<T> expectedType);

    default <T> T getInstanceOrException(Class<T> expectedType) throws IllegalStateException {
        return this.getInstanceOptional(expectedType).orElseThrow(
            () -> new IllegalStateException("No object of this type registered: " + expectedType.getName()));
    }

    class SimpleObjectRegistry implements ObjectRegistry, Builder {

        private final ClassToInstanceMap<Object> map;

        public SimpleObjectRegistry() {
            map = MutableClassToInstanceMap.create();
        }

        SimpleObjectRegistry(SimpleObjectRegistry original) {
            map = ImmutableClassToInstanceMap.copyOf(original.map);
        }

        @Override
        public ObjectRegistry build() {
            return this;
        }

        @Override
        public final <T> void putInstance(T object, Class<T> lookupType) throws IllegalArgumentException {
            if (map.containsKey(lookupType)) {
                throw new IllegalArgumentException("Registry already has an Object for type " + lookupType.getName()
                        + ": " + map.getInstance(lookupType).toString());
            }
            map.putInstance(lookupType, object);
        }

        @Override
        public <T> Optional<T> getInstanceOptional(Class<T> expectedType) {
            return Optional.ofNullable(map.getInstance(expectedType));
        }

    }

/*
    class MikityObjectRegistry extends SimpleObjectRegistry {
        @Override
        public <T> Optional<T> getInstanceOptional(Class<T> expectedType) {
            return Optional.of(super.getInstanceOptional(expectedType).orElseGet(() -> Mikito.stub(expectedType)));
        }
    }
 */
}
