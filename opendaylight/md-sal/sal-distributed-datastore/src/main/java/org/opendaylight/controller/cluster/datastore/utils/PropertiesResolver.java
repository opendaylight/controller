/*
 * Copyright (c) 2017 Pantheon technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import javax.annotation.Nullable;

public final class PropertiesResolver {

    private PropertiesResolver() {
        throw new UnsupportedOperationException("Util class.");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static Dictionary<String, Object> resolveProps(final Object propsResolver) {
        Dictionary<String, Object> props = null;
        try {
            final Field field = propsResolver.getClass().getDeclaredField("properties");
            AccessController.doPrivileged(new PrivilegedAction<Object>() {

                @Override
                public Object run() {
                    field.setAccessible(true);
                    return null;
                }
            });

            props = (Dictionary<String, Object>) field.get(propsResolver);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalArgumentException("Input object has to contain properties field.", e);
        }
        return props;
    }
}
