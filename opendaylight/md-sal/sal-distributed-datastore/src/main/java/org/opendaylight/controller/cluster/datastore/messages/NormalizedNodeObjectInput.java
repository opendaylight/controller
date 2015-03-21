/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeInputStreamReader;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeStreamReader;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Utility class unifying an {@link ObjectInput} and a {@link NormalizedNodeStreamReader},
 * allowing access to both methods.
 */
final class NormalizedNodeObjectInput implements NormalizedNodeStreamReader, ObjectInput {
    private final NormalizedNodeStreamReader reader;
    private final ObjectInput input;

    public NormalizedNodeObjectInput(final ObjectInput input) throws IOException {
        this.reader = new NormalizedNodeInputStreamReader(input);
        this.input = Preconditions.checkNotNull(input);
    }

    @Override
    public void readFully(final byte[] b) throws IOException {
        input.readFully(b);
    }

    @Override
    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        input.readFully(b, off, len);
    }

    @Override
    public int skipBytes(final int n) throws IOException {
        return input.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return input.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return input.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return input.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return input.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return input.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return input.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return input.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return input.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return input.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return input.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        return input.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return input.readUTF();
    }

    @Override
    public Object readObject() throws ClassNotFoundException, IOException {
        return input.readObject();
    }

    @Override
    public int read() throws IOException {
        return input.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return input.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return input.read(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        return input.skip(n);
    }

    @Override
    public int available() throws IOException {
        return input.available();
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

    @Override
    public NormalizedNode<?, ?> readNormalizedNode() throws IOException {
        return reader.readNormalizedNode();
    }
}