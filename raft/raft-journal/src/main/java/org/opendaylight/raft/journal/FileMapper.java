/*
 * Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.raft.journal;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File mapper implementation. This class acts as a runtime-constant indirection to a backing implementation.
 */
@NonNullByDefault
final class FileMapper {
    /**
     * Internal implementation interface.
     */
    sealed interface Impl permits ArenaFileMapper, NettyFileMapper {

        MappedFile map(FileChannel channel, MapMode mode, long offset, int size) throws IOException;
    }

    private static final Logger LOG = LoggerFactory.getLogger(FileMapper.class);
    private static final Impl IMPL = selectImplementation();

    private static Impl selectImplementation() {
        // FFM-provided ByteBuffers are not usable before Java 25 due to
        // https://bugs.openjdk.org/browse/JDK-8357145 and https://bugs.openjdk.org/browse/JDK-8357268
        if (Runtime.version().feature() < 25) {
            return nettyFallback();
        }

        final Class<?> arenaClass;
        try {
            arenaClass = Class.forName("java.lang.foreign.Arena");
        } catch (ClassNotFoundException e) {
            LOG.debug("java.lang.foreign.Arena not found", e);
            return nettyFallback();
        }

        final var lookup = MethodHandles.publicLookup();
        final MethodHandle arenaOfShared;
        try {
            final var mh = lookup.unreflect(arenaClass.getMethod("ofShared"));
            arenaOfShared = mh.asType(mh.type().changeReturnType(Object.class));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            LOG.debug("Arena.ofShared() not found", e);
            return nettyFallback();
        }

        final MethodHandle arenaClose;
        try {
            final var mh = lookup.unreflect(arenaClass.getMethod("close"));
            arenaClose = mh.asType(mh.type().changeParameterType(0, Object.class));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            LOG.debug("Arena.close() not found", e);
            return nettyFallback();
        }

        final Class<?> segmentClass;
        try {
            segmentClass = Class.forName("java.lang.foreign.MemorySegment");
        } catch (ClassNotFoundException e) {
            LOG.debug("java.lang.foreign.MemorySegment not found", e);
            return nettyFallback();
        }

        final MethodHandle segmentAsByteBuffer;
        try {
            final var mh = lookup.unreflect(segmentClass.getMethod("asByteBuffer"));
            segmentAsByteBuffer = mh.asType(mh.type().changeParameterType(0, Object.class));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            LOG.debug("Segment.force() method not found", e);
            return nettyFallback();
        }

        final MethodHandle segmentForce;
        try {
            final var mh = lookup.unreflect(segmentClass.getMethod("force"));
            segmentForce = mh.asType(mh.type().changeParameterType(0, Object.class));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            LOG.debug("Segment.force() method not found", e);
            return nettyFallback();
        }

        final MethodHandle fileChannelMap;
        try {
            final var mh = lookup.unreflect(
                FileChannel.class.getMethod("map", MapMode.class, long.class, long.class, arenaClass));
            fileChannelMap = mh.asType(mh.type().changeParameterType(4, Object.class).changeReturnType(Object.class));
        } catch (IllegalAccessException | NoSuchMethodException e) {
            LOG.debug("FileChannel.map(MapMode, long, long, Arena) method not found", e);
            return nettyFallback();
        }

        LOG.info("Using java.lang.foreign.Arena for ByteBuffer cleanup");
        return new ArenaFileMapper(arenaOfShared, arenaClose, segmentAsByteBuffer, segmentForce, fileChannelMap);
    }

    private static Impl nettyFallback() {
        LOG.info("Using io.netty.util.internal.PlatformDependent for ByteBuffer cleanup");
        return new NettyFileMapper();
    }

    private FileMapper() {
        // Hidden on purpose
    }

    static MappedFile map(final FileChannel channel, final MapMode mode, final long offset, final int size)
            throws IOException {
        return IMPL.map(channel, mode, offset, size);
    }
}
