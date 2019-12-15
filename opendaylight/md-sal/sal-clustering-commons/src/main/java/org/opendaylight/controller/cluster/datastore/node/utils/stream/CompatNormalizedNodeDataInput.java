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
    @SuppressWarnings("checkstyle:parameterName")
    public void readFully(final byte[] b) throws IOException {
        try {
            delegate.readFully(b);
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        try {
            delegate.readFully(b, off, len);
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public int skipBytes(final int n) throws IOException {
        try {
            return delegate.skipBytes(n);
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public boolean readBoolean() throws IOException {
        try {
            return delegate.readBoolean();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public byte readByte() throws IOException {
        try {
            return delegate.readByte();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public int readUnsignedByte() throws IOException {
        try {
            return delegate.readUnsignedByte();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public short readShort() throws IOException {
        try {
            return delegate.readShort();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public int readUnsignedShort() throws IOException {
        try {
            return delegate.readUnsignedShort();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public char readChar() throws IOException {
        try {
            return delegate.readChar();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public int readInt() throws IOException {
        try {
            return delegate.readInt();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public long readLong() throws IOException {
        try {
            return delegate.readLong();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public float readFloat() throws IOException {
        try {
            return delegate.readFloat();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public double readDouble() throws IOException {
        try {
            return delegate.readDouble();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public String readLine() throws IOException {
        try {
            return delegate.readLine();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public String readUTF() throws IOException {
        try {
            return delegate.readUTF();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public void streamNormalizedNode(final NormalizedNodeStreamWriter writer) throws IOException {
        try {
            delegate.streamNormalizedNode(writer);
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public YangInstanceIdentifier readYangInstanceIdentifier() throws IOException {
        try {
            return delegate.readYangInstanceIdentifier();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public QName readQName() throws IOException {
        try {
            return delegate.readQName();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public PathArgument readPathArgument() throws IOException {
        try {
            return delegate.readPathArgument();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public SchemaPath readSchemaPath() throws IOException {
        try {
            return delegate.readSchemaPath();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }
    }

    @Override
    public NormalizedNodeStreamVersion getVersion() throws IOException {
        final org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeStreamVersion version;
        try {
            version = delegate.getVersion();
        } catch (org.opendaylight.yangtools.yang.data.codec.binfmt.InvalidNormalizedNodeStreamException e) {
            throw new InvalidNormalizedNodeStreamException(e.getMessage(), e);
        }

        switch (version) {
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
