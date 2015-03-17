/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.NormalizedNodeOutputStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Utility class unifying an {@link ObjectOutput} and a {@link NormalizedNodeOutputStreamWriter},
 * allowing access to both methods.
 */
final class NormalizedNodeObjectOutput implements ObjectOutput {
    private final NormalizedNodeOutputStreamWriter writer;
    private final ObjectOutput output;

    public NormalizedNodeObjectOutput(final ObjectOutput output) {
        this.output = Preconditions.checkNotNull(output);
        this.writer = new NormalizedNodeOutputStreamWriter(output);
    }

    @Override
    public void writeBoolean(final boolean v) throws IOException {
        output.writeBoolean(v);
    }

    @Override
    public void writeByte(final int v) throws IOException {
        output.writeByte(v);
    }

    @Override
    public void writeShort(final int v) throws IOException {
        output.writeShort(v);
    }

    @Override
    public void writeChar(final int v) throws IOException {
        output.writeChar(v);
    }

    @Override
    public void writeInt(final int v) throws IOException {
        output.writeInt(v);
    }

    @Override
    public void writeLong(final long v) throws IOException {
        output.writeLong(v);
    }

    @Override
    public void writeFloat(final float v) throws IOException {
        output.writeFloat(v);
    }

    @Override
    public void writeDouble(final double v) throws IOException {
        output.writeDouble(v);
    }

    @Override
    public void writeBytes(final String s) throws IOException {
        output.writeBytes(s);
    }

    @Override
    public void writeChars(final String s) throws IOException {
        output.writeChars(s);
    }

    @Override
    public void writeUTF(final String s) throws IOException {
        output.writeUTF(s);
    }

    @Override
    public void writeObject(final Object obj) throws IOException {
        output.writeObject(obj);
    }

    @Override
    public void write(final int b) throws IOException {
        output.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
        output.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        output.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        output.flush();
    }

    @Override
    public void close() throws IOException {
        output.close();
    }

    public void writeNormalizedNode(final NormalizedNode<?, ?> node) throws IOException {
        writer.writeNormalizedNode(node);
    }
}
