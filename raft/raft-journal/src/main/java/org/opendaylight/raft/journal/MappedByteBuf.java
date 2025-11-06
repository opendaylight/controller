/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opendaylight.raft.journal;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import io.netty.buffer.AbstractReferenceCountedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.internal.PlatformDependent;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link ByteBuf} backed by a {@link MappedByteBuffer}.
 */
final class MappedByteBuf extends AbstractReferenceCountedByteBuf implements Flushable {
    private final ByteBufAllocator alloc;

    private MappedByteBuffer byteBuffer;
    private ByteBuffer internalNio;

    @VisibleForTesting
    MappedByteBuf(final ByteBufAllocator alloc, final MappedByteBuffer byteBuffer) {
        super(byteBuffer.limit());
        this.alloc = requireNonNull(alloc);
        this.byteBuffer = requireNonNull(byteBuffer);
    }

    @NonNullByDefault
    static MappedByteBuf of(final SegmentFile file) throws IOException {
        return new MappedByteBuf(file.allocator(), file.channel().map(MapMode.READ_WRITE, 0, file.maxSize()));
    }

    @Override
    @SuppressWarnings("checkstyle:avoidHidingCauseException")
    public void flush() throws IOException {
        ensureAccessible();
        try {
            byteBuffer.force();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    protected void deallocate() {
        final var local = byteBuffer;
        if (local != null) {
            byteBuffer = null;
            PlatformDependent.freeDirectBuffer(local);
        }
    }

    @Override
    public ByteBufAllocator alloc() {
        return alloc;
    }

    @Override
    public boolean isContiguous() {
        return true;
    }

    @Override
    public boolean hasArray() {
        return false;
    }

    @Override
    public byte[] array() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int arrayOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMemoryAddress() {
        return false;
    }

    @Override
    public long memoryAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int capacity() {
        return maxCapacity();
    }

    @Override
    public ByteBuf capacity(final int newCapacity) {
        throw new UnsupportedOperationException("capacity cannot be set");
    }

    @Override
    @Deprecated
    public ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }

    @Override
    public ByteBuf unwrap() {
        return null;
    }

    @Override
    public ByteBuf copy(final int index, final int length) {
        ensureAccessible();
        return alloc.heapBuffer(length).writeBytes(byteBuffer.slice(index, length));
    }

    @Override
    public boolean isDirect() {
        return true;
    }

    @Override
    public int nioBufferCount() {
        return 1;
    }

    @Override
    public ByteBuffer nioBuffer(final int index, final int length) {
        checkIndex(index, length);
        return byteBuffer.slice(index, length);
    }

    @Override
    public ByteBuffer internalNioBuffer(final int index, final int length) {
        checkIndex(index, length);
        return internalNio().limit(index + length).position(index);
    }

    private ByteBuffer internalNio() {
        var local = internalNio;
        if (local == null) {
            internalNio = local = byteBuffer.duplicate();
        }
        return local;
    }

    @Override
    public ByteBuffer[] nioBuffers(final int index, final int length) {
        return new ByteBuffer[] { nioBuffer(index, length) };
    }

    @Override
    public byte getByte(final int index) {
        ensureAccessible();
        return _getByte(index);
    }

    @Override
    protected byte _getByte(final int index) {
        return byteBuffer.get(index);
    }

    @Override
    public short getShort(final int index) {
        ensureAccessible();
        return _getShort(index);
    }

    @Override
    protected short _getShort(final int index) {
        return byteBuffer.getShort(index);
    }

    @Override
    protected short _getShortLE(final int index) {
        return ByteBufUtil.swapShort(byteBuffer.getShort(index));
    }

    @Override
    public int getUnsignedMedium(final int index) {
        ensureAccessible();
        return _getUnsignedMedium(index);
    }

    @Override
    protected int _getUnsignedMedium(final int index) {
        return (_getByte(index) & 0xff) << 16 | (_getByte(index + 1) & 0xff) << 8 | _getByte(index + 2) & 0xff;
    }

    @Override
    protected int _getUnsignedMediumLE(final int index) {
        return _getByte(index) & 0xff | (_getByte(index + 1) & 0xff) << 8 | (_getByte(index + 2) & 0xff) << 16;
    }

    @Override
    public int getInt(final int index) {
        ensureAccessible();
        return _getInt(index);
    }

    @Override
    protected int _getInt(final int index) {
        return byteBuffer.getInt(index);
    }

    @Override
    protected int _getIntLE(final int index) {
        return ByteBufUtil.swapInt(byteBuffer.getInt(index));
    }

    @Override
    public long getLong(final int index) {
        ensureAccessible();
        return _getLong(index);
    }

    @Override
    protected long _getLong(final int index) {
        return byteBuffer.getLong(index);
    }

    @Override
    protected long _getLongLE(final int index) {
        return ByteBufUtil.swapLong(byteBuffer.getLong(index));
    }

    @Override
    public ByteBuf setByte(final int index, final int value) {
        ensureAccessible();
        _setByte(index, value);
        return this;
    }

    @Override
    protected void _setByte(final int index, final int value) {
        byteBuffer.put(index, (byte) value);
    }

    @Override
    public ByteBuf setShort(final int index, final int value) {
        ensureAccessible();
        _setShort(index, value);
        return this;
    }

    @Override
    protected void _setShort(final int index, final int value) {
        byteBuffer.putShort(index, (short) value);
    }

    @Override
    protected void _setShortLE(final int index, final int value) {
        byteBuffer.putShort(index, ByteBufUtil.swapShort((short) value));
    }

    @Override
    public ByteBuf setMedium(final int index, final int value) {
        ensureAccessible();
        _setMedium(index, value);
        return this;
    }

    @Override
    protected void _setMedium(final int index, final int value) {
        setByte(index, (byte) (value >>> 16));
        setByte(index + 1, (byte) (value >>> 8));
        setByte(index + 2, (byte) value);
    }

    @Override
    protected void _setMediumLE(final int index, final int value) {
        setByte(index, (byte) value);
        setByte(index + 1, (byte) (value >>> 8));
        setByte(index + 2, (byte) (value >>> 16));
    }

    @Override
    public ByteBuf setInt(final int index, final int value) {
        ensureAccessible();
        _setInt(index, value);
        return this;
    }

    @Override
    protected void _setInt(final int index, final int value) {
        byteBuffer.putInt(index, value);
    }

    @Override
    protected void _setIntLE(final int index, final int value) {
        byteBuffer.putInt(index, ByteBufUtil.swapInt(value));
    }

    @Override
    public ByteBuf setLong(final int index, final long value) {
        ensureAccessible();
        _setLong(index, value);
        return this;
    }

    @Override
    protected void _setLong(final int index, final long value) {
        byteBuffer.putLong(index, value);
    }

    @Override
    protected void _setLongLE(final int index, final long value) {
        byteBuffer.putLong(index, ByteBufUtil.swapLong(value));
    }

    @Override
    public ByteBuf getBytes(final int index, final ByteBuf dst, final int dstIndex, final int length) {
        checkDstIndex(index, length, dstIndex, dst.capacity());
        if (dst.hasArray()) {
            byteBuffer.get(index, dst.array(), dst.arrayOffset() + dstIndex, length);
        } else {
            dst.setBytes(dstIndex, this, index, length);
        }
        return this;
    }

    @Override
    public ByteBuf getBytes(final int index, final byte[] dst, final int dstIndex, final int length) {
        checkDstIndex(index, length, dstIndex, dst.length);
        byteBuffer.get(index, dst, dstIndex, length);
        return this;
    }

    @Override
    public ByteBuf getBytes(final int index, final ByteBuffer dst) {
        final var remaining = dst.remaining();
        checkIndex(index, remaining);
        dst.put(byteBuffer.slice(index, remaining));
        return this;
    }

    @Override
    public ByteBuf getBytes(final int index, final OutputStream out, final int length) throws IOException {
        Channels.newChannel(out).write(internalNioBuffer(index, length));
        return this;
    }

    @Override
    public int getBytes(final int index, final GatheringByteChannel out, final int length) throws IOException {
        return out.write(internalNioBuffer(index, length));
    }

    @Override
    public int getBytes(final int index, final FileChannel out, final long position, final int length)
            throws IOException {
        return out.write(internalNioBuffer(index, length), position);
    }

    @Override
    public ByteBuf setBytes(final int index, final ByteBuf src, final int srcIndex, final int length) {
        checkSrcIndex(index, length, srcIndex, src.capacity());
        src.getBytes(srcIndex, this, index, length);
        return this;
    }

    @Override
    public ByteBuf setBytes(final int index, final byte[] src, final int srcIndex, final int length) {
        checkSrcIndex(index, length, srcIndex, src.length);
        byteBuffer.put(index, src, srcIndex, length);
        return this;
    }

    @Override
    public ByteBuf setBytes(final int index, final ByteBuffer src) {
        ensureAccessible();
        byteBuffer.put(index, src, src.position(), src.remaining());
        return this;
    }

    @Override
    public int setBytes(final int index, final InputStream in, final int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setBytes(final int index, final ScatteringByteChannel in, final int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setBytes(final int index, final FileChannel in, final long position, final int length)
            throws IOException {
        throw new UnsupportedOperationException();
    }
}
