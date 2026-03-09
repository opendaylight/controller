/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.journal;

import static java.util.Objects.requireNonNull;

import io.netty.util.internal.PlatformDependent;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MappedFileSupport {
    @NonNullByDefault
    sealed interface Impl {

        MappedFile<?> map(FileChannel channel, MapMode mode, long offset, int size) throws IOException;
    }

    @NonNullByDefault
    private record Netty() implements Impl {
        @Override
        public NettyMappedFile map(final FileChannel channel, final MapMode mode, final long offset, final int size)
                throws IOException {
            return new NettyMappedFile(channel.map(mode, offset, size));
        }
    }

    /**
     * A {@link MappedFile} implementation for Java <22, where we operate on plain {@link ByteBuffer}.
     */
    @NonNullByDefault
    static final class NettyMappedFile extends MappedFile<MappedByteBuffer> {
        private NettyMappedFile(final MappedByteBuffer buffer) {
            super(buffer);
        }

        @Override
        void unmap(final MappedByteBuffer buffer) {
            PlatformDependent.freeDirectBuffer(buffer);
        }

        @Override
        void sync(final MappedByteBuffer buffer) {
            buffer.force();
        }
    }

    @NonNullByDefault
    static final class ReflectImpl implements Impl {
        private final MethodHandle arenaOfShared;
        private final MethodHandle arenaClose;
        private final MethodHandle segmentAsByteBuffer;
        private final MethodHandle segmentForce;
        private final MethodHandle fileChannelMap;

        ReflectImpl(final MethodHandle arenaOfShared, final MethodHandle arenaClose,
                final MethodHandle segmentAsByteBuffer, final MethodHandle segmentForce,
                final MethodHandle fileChannelMap) {
            this.arenaOfShared = requireNonNull(arenaOfShared);
            this.arenaClose = requireNonNull(arenaClose);
            this.segmentAsByteBuffer = requireNonNull(segmentAsByteBuffer);
            this.segmentForce = requireNonNull(segmentForce);
            this.fileChannelMap = requireNonNull(fileChannelMap);
        }

        @Override
        @SuppressWarnings("checkstyle:illegalCatch")
        public ReflectMappedFile map(final FileChannel channel, final MapMode mode, final long offset, final int size)
                throws IOException {
            try {
                return mapImpl(channel, mode, offset, size);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }

        @SuppressWarnings("checkstyle:illegalThrows")
        private ReflectMappedFile mapImpl(final FileChannel channel, final MapMode mode, final long offset,
                final long size) throws Throwable {
            final var arena = arenaOfShared.invokeExact();
            final var segment = fileChannelMap.invokeExact(channel, mode, offset, size, arena);
            final var byfeBuffer = (ByteBuffer) segmentAsByteBuffer.invokeExact(segment);

            return new ReflectMappedFile(byfeBuffer, () -> sync(segment), () -> unmap(arena));
        }

        @SuppressWarnings("checkstyle:illegalCatch")
        private void sync(final Object segment) {
            try {
                segmentForce.invokeExact(segment);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }

        @SuppressWarnings("checkstyle:illegalCatch")
        private void unmap(final Object arena) {
            try {
                arenaClose.invokeExact(arena);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * A {@link MappedFile} implementation using reflection where we have {@code Arena} available.
     */
    @NonNullByDefault
    static final class ReflectMappedFile extends MappedFile<ByteBuffer> {
        private final Runnable sync;
        private final Runnable unmap;

        private ReflectMappedFile(final ByteBuffer byteBuffer, final Runnable sync, final Runnable unmap) {
            super(byteBuffer);
            this.sync = requireNonNull(sync);
            this.unmap = requireNonNull(unmap);
        }

        @Override
        void sync(final ByteBuffer buffer) {
            sync.run();
        }

        @Override
        void unmap(final ByteBuffer buffer) {
            unmap.run();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(MappedFileSupport.class);
    private static final Impl IMPL;

    static {
        try {
            IMPL = selectImplementation();
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Impl selectImplementation() throws IllegalAccessException {
        final Class<?> arenaClass;
        try {
            arenaClass = Class.forName("java.lang.foreign.Arena");
        } catch (ClassNotFoundException e) {
            LOG.debug("java.lang.foreign.Arena not found", e);
            return legacyImplementation();
        }

        final var lookup = MethodHandles.publicLookup();
        final MethodHandle arenaOfShared;
        try {
            final var mh = lookup.unreflect(arenaClass.getMethod("ofShared"));
            arenaOfShared = mh.asType(mh.type().changeReturnType(Object.class));
        } catch (NoSuchMethodException e) {
            LOG.debug("Arena.ofShared() not found", e);
            return legacyImplementation();
        }

        final MethodHandle arenaClose;
        try {
            final var mh = lookup.unreflect(arenaClass.getMethod("close"));
            arenaClose = mh.asType(mh.type().changeParameterType(0, Object.class));
        } catch (NoSuchMethodException e) {
            LOG.debug("Arena.close() not found", e);
            return legacyImplementation();
        }

        final Class<?> segmentClass;
        try {
            segmentClass = Class.forName("java.lang.foreign.MemorySegment");
        } catch (ClassNotFoundException e) {
            LOG.debug("java.lang.foreign.MemorySegment not found", e);
            return legacyImplementation();
        }

        final MethodHandle segmentAsByteBuffer;
        try {
            final var mh = lookup.unreflect(segmentClass.getMethod("asByteBuffer"));
            segmentAsByteBuffer = mh.asType(mh.type().changeParameterType(0, Object.class));
        } catch (NoSuchMethodException e) {
            LOG.debug("Segment.force() method not found", e);
            return legacyImplementation();
        }

        final MethodHandle segmentForce;
        try {
            final var mh = lookup.unreflect(segmentClass.getMethod("force"));
            segmentForce = mh.asType(mh.type().changeParameterType(0, Object.class));
        } catch (NoSuchMethodException e) {
            LOG.debug("Segment.force() method not found", e);
            return legacyImplementation();
        }

        final MethodHandle fileChannelMap;
        try {
            final var mh = lookup.unreflect(
                FileChannel.class.getMethod("map", MapMode.class, long.class, long.class, arenaClass));
            fileChannelMap = mh.asType(mh.type().changeParameterType(4, Object.class).changeReturnType(Object.class));
        } catch (NoSuchMethodException e) {
            LOG.debug("FileChannel.map(MapMode, long, long, Arena) method not found", e);
            return legacyImplementation();
        }

        LOG.info("Using java.lang.foreign.Arena for ByteBuffer cleanup");
        return new ReflectImpl(arenaOfShared, arenaClose, segmentAsByteBuffer, segmentForce, fileChannelMap);
    }

    private static Impl legacyImplementation() {
        LOG.info("Using io.netty.util.internal.PlatformDependent for ByteBuffer cleanup");
        return new Netty();
    }

    private MappedFileSupport() {
        // Hidden on purpose
    }

    @NonNullByDefault
    static MappedFile<?> map(final FileChannel channel, final MapMode mode, final long offset, final int size)
            throws IOException {
        return IMPL.map(channel, mode, offset, size);
    }
}
