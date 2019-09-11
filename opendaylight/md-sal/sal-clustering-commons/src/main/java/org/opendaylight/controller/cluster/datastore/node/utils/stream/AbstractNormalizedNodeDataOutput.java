/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static java.util.Objects.requireNonNull;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Abstract base class for implementing {@link NormalizedNodeDataOutput} contract. This class uses
 * {@link NormalizedNodeStreamWriter} as an internal interface for performing the actual NormalizedNode writeout,
 * i.e. it will defer to a {@link NormalizedNodeWriter} instance.
 *
 * <p>
 * As such, this is an implementation detail not exposed from this package, hence implementations can rely on the
 * stream being initialized with a header and version.
 */
abstract class AbstractNormalizedNodeDataOutput implements NormalizedNodeDataOutput, NormalizedNodeStreamWriter {
    // Visible for subclasses
    final DataOutput output;

    private NormalizedNodeWriter normalizedNodeWriter;
    private boolean headerWritten;

    AbstractNormalizedNodeDataOutput(final DataOutput output) {
        this.output = requireNonNull(output);
    }


    private void ensureHeaderWritten() throws IOException {
        if (!headerWritten) {
            output.writeByte(TokenTypes.SIGNATURE_MARKER);
            output.writeShort(streamVersion());
            headerWritten = true;
        }
    }

    @Override
    public final void write(final int value) throws IOException {
        ensureHeaderWritten();
        output.write(value);
    }

    @Override
    public final void write(final byte[] bytes) throws IOException {
        ensureHeaderWritten();
        output.write(bytes);
    }

    @Override
    public final void write(final byte[] bytes, final int off, final int len) throws IOException {
        ensureHeaderWritten();
        output.write(bytes, off, len);
    }

    @Override
    public final void writeBoolean(final boolean value) throws IOException {
        ensureHeaderWritten();
        output.writeBoolean(value);
    }

    @Override
    public final void writeByte(final int value) throws IOException {
        ensureHeaderWritten();
        output.writeByte(value);
    }

    @Override
    public final void writeShort(final int value) throws IOException {
        ensureHeaderWritten();
        output.writeShort(value);
    }

    @Override
    public final void writeChar(final int value) throws IOException {
        ensureHeaderWritten();
        output.writeChar(value);
    }

    @Override
    public final void writeInt(final int value) throws IOException {
        ensureHeaderWritten();
        output.writeInt(value);
    }

    @Override
    public final void writeLong(final long value) throws IOException {
        ensureHeaderWritten();
        output.writeLong(value);
    }

    @Override
    public final void writeFloat(final float value) throws IOException {
        ensureHeaderWritten();
        output.writeFloat(value);
    }

    @Override
    public final void writeDouble(final double value) throws IOException {
        ensureHeaderWritten();
        output.writeDouble(value);
    }

    @Override
    public final void writeBytes(final String str) throws IOException {
        ensureHeaderWritten();
        output.writeBytes(str);
    }

    @Override
    public final void writeChars(final String str) throws IOException {
        ensureHeaderWritten();
        output.writeChars(str);
    }

    @Override
    public final void writeUTF(final String str) throws IOException {
        ensureHeaderWritten();
        output.writeUTF(str);
    }

    @Override
    public final void writeQName(final QName qname) throws IOException {
        ensureHeaderWritten();
        writeQNameInternal(qname);
    }

    @Override
    public final void writeNormalizedNode(final NormalizedNode<?, ?> node) throws IOException {
        ensureHeaderWritten();
        if (normalizedNodeWriter == null) {
            normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(this);
        }
        normalizedNodeWriter.write(node);
    }

    @Override
    public final void writePathArgument(final PathArgument pathArgument) throws IOException {
        ensureHeaderWritten();
        writePathArgumentInternal(pathArgument);
    }

    @Override
    public final void writeYangInstanceIdentifier(final YangInstanceIdentifier identifier) throws IOException {
        ensureHeaderWritten();
        writeYangInstanceIdentifierInternal(identifier);
    }

    @Override
    public final void writeSchemaPath(final SchemaPath path) throws IOException {
        ensureHeaderWritten();

        output.writeBoolean(path.isAbsolute());
        final List<QName> qnames = path.getPath();
        output.writeInt(qnames.size());
        for (QName qname : qnames) {
            writeQNameInternal(qname);
        }
    }

    @Override
    public final void close() throws IOException {
        flush();
    }

    @Override
    public void flush() throws IOException {
        if (output instanceof OutputStream) {
            ((OutputStream)output).flush();
        }
    }

    final void writeYangInstanceIdentifierInternal(final YangInstanceIdentifier identifier) throws IOException {
        List<PathArgument> pathArguments = identifier.getPathArguments();
        output.writeInt(pathArguments.size());

        for (PathArgument pathArgument : pathArguments) {
            writePathArgumentInternal(pathArgument);
        }
    }

    abstract short streamVersion();

    abstract void writeQNameInternal(@NonNull QName qname) throws IOException;

    abstract void writePathArgumentInternal(PathArgument pathArgument) throws IOException;
}
