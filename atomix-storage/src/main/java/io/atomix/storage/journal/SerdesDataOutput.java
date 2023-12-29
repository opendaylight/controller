/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static java.util.Objects.requireNonNull;

import java.io.DataOutput;
import java.io.IOException;

/**
 *
 */
public record SerdesDataOutput(SerdesBinding serdesBinding, ScatteringDataOutput delegate) implements DataOutput {
    public SerdesDataOutput {
        requireNonNull(serdesBinding);
        requireNonNull(delegate);
    }

    public <O> void writeObject(final Class<O> typeClass, final O obj) throws IOException {
        final var reader = serdesBinding.ioFor(typeClass);
        if (reader == null) {
            throw new IOException("No serdes for " + typeClass);
        }
        reader.writeObject(this, obj);
    }

    @Override
    public void write(final int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException {
        delegate.writeBoolean(v);
    }

    @Override
    public void writeByte(final int v) throws IOException {
        delegate.writeByte(v);
    }

    @Override
    public void writeShort(final int v) throws IOException {
        delegate.writeShort(v);
    }

    @Override
    public void writeChar(final int v) throws IOException {
        delegate.writeChar(v);
    }

    @Override
    public void writeInt(final int v) throws IOException {
        delegate.writeInt(v);
    }

    @Override
    public void writeLong(final long v) throws IOException {
        delegate.writeLong(v);
    }

    @Override
    public void writeFloat(final float v) throws IOException {
        delegate.writeFloat(v);
    }

    @Override
    public void writeDouble(final double v) throws IOException {
        delegate.writeDouble(v);
    }

    @Override
    public void writeBytes(final String s) throws IOException {
        delegate.writeBytes(s);
    }

    @Override
    public void writeChars(final String s) throws IOException {
        delegate.writeChars(s);
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        delegate.writeUTF(s);
    }
}
