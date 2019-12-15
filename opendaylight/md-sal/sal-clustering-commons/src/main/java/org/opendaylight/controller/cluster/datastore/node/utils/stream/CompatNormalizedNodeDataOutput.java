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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

@Deprecated
final class CompatNormalizedNodeDataOutput extends ForwardingObject implements NormalizedNodeDataOutput {
    private final org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput delegate;

    CompatNormalizedNodeDataOutput(
        final org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void write(final int b) throws IOException {
        delegate.write(b);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void write(final byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void write(final byte[] b, final int off, final int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void writeBoolean(final boolean v) throws IOException {
        delegate.writeBoolean(v);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void writeByte(final int v) throws IOException {
        delegate.writeByte(v);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void writeShort(final int v) throws IOException {
        delegate.writeShort(v);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void writeChar(final int v) throws IOException {
        delegate.writeChar(v);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void writeInt(final int v) throws IOException {
        delegate.writeInt(v);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void writeLong(final long v) throws IOException {
        delegate.writeLong(v);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void writeFloat(final float v) throws IOException {
        delegate.writeFloat(v);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void writeDouble(final double v) throws IOException {
        delegate.writeDouble(v);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void writeBytes(final String s) throws IOException {
        delegate.writeBytes(s);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void writeChars(final String s) throws IOException {
        delegate.writeChars(s);
    }

    @Override
    @SuppressWarnings("checkstyle:parameterName")
    public void writeUTF(final String s) throws IOException {
        delegate.writeUTF(s);
    }

    @Override
    public void writeQName(final QName qname) throws IOException {
        delegate.writeQName(qname);
    }

    @Override
    public void writeNormalizedNode(final NormalizedNode<?, ?> normalizedNode) throws IOException {
        delegate.writeNormalizedNode(normalizedNode);
    }

    @Override
    public void writePathArgument(final PathArgument pathArgument) throws IOException {
        delegate.writePathArgument(pathArgument);
    }

    @Override
    public void writeYangInstanceIdentifier(final YangInstanceIdentifier identifier) throws IOException {
        delegate.writeYangInstanceIdentifier(identifier);
    }

    @Override
    public void writeSchemaPath(final SchemaPath path) throws IOException {
        delegate.writeSchemaPath(path);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    protected Object delegate() {
        return delegate;
    }
}
