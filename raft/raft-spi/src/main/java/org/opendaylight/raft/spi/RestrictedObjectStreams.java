/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.spi;

import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Support for {@link ObjectInputStream}s backed by an explicit ordered set of {@link ClassLoader}s plus the
 * {@link ClassLoader#getPlatformClassLoader()}.
 */
@NonNullByDefault
public final class RestrictedObjectStreams {
    // Note: we use 'List', de-deuplicated below, to retain iteration order
    private static final LoadingCache<List<ClassLoader>, RestrictedObjectStreams> CACHE =
        CacheBuilder.newBuilder().weakValues().build(new CacheLoader<>() {
            @Override
            public RestrictedObjectStreams load(final List<ClassLoader> key) {
                return new RestrictedObjectStreams(key);
            }
        });

    private final RestrictedClassLoader classLoader;

    private RestrictedObjectStreams(final List<ClassLoader> delegates) {
        classLoader = new RestrictedClassLoader(delegates);
    }

    /**
     * Return an instance backed by specified {@link ClassLoader}s.
     *
     * @param classLoaders the {@link ClassLoader}s
     * @return a shared {@link RestrictedObjectStreams} instance
     */
    public static RestrictedObjectStreams of(final ClassLoader... classLoaders) {
        return of(List.of(classLoaders));
    }

    /**
     * Return an instance backed by specified {@link ClassLoader}s.
     *
     * @param classLoaders the {@link ClassLoader}s
     * @return a shared {@link RestrictedObjectStreams} instance
     */
    public static RestrictedObjectStreams of(final List<? extends ClassLoader> classLoaders) {
        // Note: dedup before talking to the cache
        return CACHE.getUnchecked(classLoaders.stream().distinct().collect(Collectors.toUnmodifiableList()));
    }

    /**
     * Return an instance backed by {@link ClassLoader}s of specified classes, as returned by
     * {@link Class#getClassLoader()}.
     *
     * @param classes the {@link Class}es
     * @return a shared {@link RestrictedObjectStreams} instance
     */
    public static RestrictedObjectStreams ofClassLoaders(final Class<?>... classes) {
        return ofClassLoaders(List.of(classes));
    }

    /**
     * Return an instance backed by {@link ClassLoader}s of specified classes, as returned by
     * {@link Class#getClassLoader()}.
     *
     * @param classes the {@link Class}es
     * @return a shared {@link RestrictedObjectStreams} instance
     */
    public static RestrictedObjectStreams ofClassLoaders(final List<? extends Class<?>> classes) {
        return of(classes.stream().map(Class::getClassLoader).filter(Objects::nonNull).toList());
    }

    /**
     * Return a new {@link ObjectInputStream} backed by specified {@link InputStream}.
     *
     * @param in the {@link InputStream}
     * @return a new {@link ObjectInputStream}
     * @throws IOException if an I/O error occurs
     */
    public ObjectInputStream newObjectInputStream(final InputStream in) throws IOException {
        return new RestrictedObjectInputStream(in, classLoader);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("classLoader", classLoader).toString();
    }
}
