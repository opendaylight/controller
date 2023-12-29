/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import java.io.DataInput;
import java.io.IOException;

/**
 * Serialization overlay on top of a {@link GatheringDataInput} based on a particular instance {@link SerdesBinding} of.
 * Note that the binding remains constant for the entire entry.
 *
 * <p>
 * Aftering providing a suitable set of {@link SerdesSupport}s, this interface provides a light-weight serialization
 * interface -- without mucking around with copy things around.
 */
public record SerdesDataInput(SerdesBinding serdesBinding, GatheringDataInput delegate) implements DataInput {
    public SerdesDataInput {
        requireNonNull(serdesBinding);
        requireNonNull(delegate);
    }

    public <O> O readObject(final Class<O> typeClass) throws IOException {
        final var io = serdesBinding.ioFor(typeClass);
        if (io == null) {
            throw new IOException("No serdes for " + typeClass);
        }
        try {
            return typeClass.cast(io.readObject(this));
        } catch (ClassCastException e) {
            throw new IOException(io.support() + " produceced incompatible object", e);
        }
    }

    @Override
    public void readFully(final byte[] b) throws IOException {
        delegate.readFully(b);
    }

    @Override
    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        delegate.readFully(b, off, len);
    }

    @Override
    public int skipBytes(final int n) throws IOException {
        return delegate.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return delegate.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return delegate.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return delegate.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return delegate.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return delegate.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return delegate.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return delegate.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return delegate.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return delegate.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return delegate.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        return delegate.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return delegate.readUTF();
    }
}
