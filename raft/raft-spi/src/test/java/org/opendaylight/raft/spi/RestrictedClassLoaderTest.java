/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RestrictedClassLoaderTest {
    private static final String FQCN = RestrictedClassLoaderTest.class.getName();
    private static final String UNRESOLVABLE = "com.example.unresolvable";

    @ParameterizedTest
    @MethodSource
    void emptyResolvesPrimitives(final String name, final Class<?> expected) throws Exception {
        final var loader = new RestrictedClassLoader(List.of());
        assertSame(expected, loader.loadClass(name));
    }

    static List<Arguments> emptyResolvesPrimitives() {
        return List.of(
            arguments("boolean", boolean.class),
            arguments("byte", byte.class),
            arguments("char", char.class),
            arguments("short", short.class),
            arguments("int", int.class),
            arguments("long", long.class),
            arguments("float", float.class),
            arguments("double", double.class),
            arguments("void", void.class),
            arguments("java.lang.Object", Object.class));
    }

    @Test
    void emptyDoesNotResolveJavaLangObject() {
        final var loader = new RestrictedClassLoader(List.of());
        final var ex = assertThrows(ClassNotFoundException.class, () -> loader.loadClass(FQCN));
        assertEquals(FQCN, ex.getMessage());
        assertNull(ex.getCause());
        assertArrayEquals(new Throwable[0], ex.getSuppressed());
    }

    @Test
    void delegationToTestLoader() throws Exception {
        final var myLoader = RestrictedClassLoaderTest.class.getClassLoader();
        final var loader = new RestrictedClassLoader(List.of(myLoader));
        assertEquals("RestrictedClassLoader{delegates=[" + myLoader.toString() + "]}", loader.toString());

        assertSame(RestrictedClassLoaderTest.class, loader.loadClass(FQCN));
        final var ex = assertThrows(ClassNotFoundException.class, () -> loader.loadClass(UNRESOLVABLE));
        assertEquals(UNRESOLVABLE, ex.getMessage());
        assertNull(ex.getCause());

        final var suppressed = ex.getSuppressed();
        assertEquals(1, suppressed.length);
        assertInstanceOf(ClassNotFoundException.class, suppressed[0]);
    }
}
