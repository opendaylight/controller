/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * NormalizedNodeInputStreamReader reads the byte stream and constructs the normalized node including its children nodes.
 * This process goes in recursive manner, where each NodeTypes object signifies the start of the object, except END_NODE.
 * If a node can have children, then that node's end is calculated based on appearance of END_NODE.
 *
 */

public class NormalizedNodeInputStreamReader extends AbstractNormalizedNodeDataInput implements NormalizedNodeStreamReader {

    private boolean readSignatureMarker = true;

    /**
     * @deprecated Use {@link #NormalizedNodeInputStreamReader(DataInput)} instead.
     */
    @Deprecated
    public NormalizedNodeInputStreamReader(final InputStream stream) throws IOException {
        this((DataInput) new DataInputStream(Preconditions.checkNotNull(stream)));
    }

    /**
     * @deprecated Use {@link NormalizedNodeInputOutput#newDataInput(DataInput)} instead.
     */
    @Deprecated
    public NormalizedNodeInputStreamReader(final DataInput input) {
        this(input, false);
    }

    NormalizedNodeInputStreamReader(final DataInput input, final boolean versionChecked) {
        super(input, new NormalizedNodeInputDictionary());
        readSignatureMarker = !versionChecked;
    }

    @Override
    public NormalizedNode<?, ?> readNormalizedNode() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readNormalizedNode();
    }

    private void readSignatureMarkerAndVersionIfNeeded() throws IOException {
        if(readSignatureMarker) {
            readSignatureMarker = false;

            final byte marker = input().readByte();
            if (marker != TokenTypes.SIGNATURE_MARKER) {
                throw new InvalidNormalizedNodeStreamException(String.format(
                        "Invalid signature marker: %d", marker));
            }

            final short version = input().readShort();
            if (version != TokenTypes.LITHIUM_VERSION) {
                throw new InvalidNormalizedNodeStreamException(String.format("Unhandled stream version %s", version));
            }
        }
    }

    @Override
    public YangInstanceIdentifier readYangInstanceIdentifier() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readYangInstanceIdentifier();
    }

    @Override
    public void readFully(final byte[] b) throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        super.readFully(b);
    }

    @Override
    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        super.readFully(b, off, len);
    }

    @Override
    public int skipBytes(final int n) throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readChar();
    }

    @Override
    public int readInt() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readInt();
    }

    @Override
    public long readLong() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        readSignatureMarkerAndVersionIfNeeded();
        return super.readUTF();
    }
}
