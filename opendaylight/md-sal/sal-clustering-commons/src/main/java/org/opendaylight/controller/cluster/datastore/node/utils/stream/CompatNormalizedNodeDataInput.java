/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ForwardingObject;
import java.io.IOException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Deprecated
final class CompatNormalizedNodeDataInput extends ForwardingObject implements NormalizedNodeDataInput {
    private final org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput delegate;

    CompatNormalizedNodeDataInput(
            final org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput delegate) {
        this.delegate = requireNonNull(delegate);
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

    @Override
    public void streamNormalizedNode(final NormalizedNodeStreamWriter writer) throws IOException {
        delegate.streamNormalizedNode(writer);
    }

    @Override
    public YangInstanceIdentifier readYangInstanceIdentifier() throws IOException {
        return delegate.readYangInstanceIdentifier();
    }

    @Override
    public QName readQName() throws IOException {
        return delegate.readQName();
    }

    @Override
    public PathArgument readPathArgument() throws IOException {
        return delegate.readPathArgument();
    }

    @Override
    public SchemaPath readSchemaPath() throws IOException {
        return delegate.readSchemaPath();
    }

    @Override
    public NormalizedNodeStreamVersion getVersion() throws IOException {
        switch (delegate.getVersion()) {
            case LITHIUM:
                return NormalizedNodeStreamVersion.LITHIUM;
            case MAGNESIUM:
                return NormalizedNodeStreamVersion.MAGNESIUM;
            case NEON_SR2:
                return NormalizedNodeStreamVersion.NEON_SR2;
            case SODIUM_SR1:
                return NormalizedNodeStreamVersion.SODIUM_SR1;
            default:
                throw new IOException("Unhandled version " + delegate.getVersion());
        }
    }

    @Override
    protected org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataInput delegate() {
        return delegate;
    }
}
