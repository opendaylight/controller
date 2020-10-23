/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.common.util;

@Deprecated(forRemoval = true)
public final class Arguments {
    private Arguments() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Checks if value is instance of provided class.
     *
     * @param value Value to check
     * @param type Type to check
     * @return Reference which was checked
     * @deprecated This utility is not used in this project, please brew your own or
     *             use {@code Preconditions.checkArgument()}.
     */
    @Deprecated(forRemoval = true)
    public static <T> T checkInstanceOf(final Object value, final Class<T> type) {
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(String.format("Value %s is not of type %s", value, type));
        }
        return type.cast(value);
    }
}
