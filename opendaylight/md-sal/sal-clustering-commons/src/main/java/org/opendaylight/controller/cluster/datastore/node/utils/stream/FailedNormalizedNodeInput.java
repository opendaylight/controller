/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import java.io.IOException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

final class FailedNormalizedNodeInput implements NormalizedNodeDataInput {
    private final IOException e;

    public FailedNormalizedNodeInput(final IOException e) {
        this.e = Preconditions.checkNotNull(e);
    }

    @Override
    public void readFully(final byte[] b) throws IOException {
        throw e;
    }

    @Override
    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        throw e;
    }

    @Override
    public int skipBytes(final int n) throws IOException {
        throw e;
    }

    @Override
    public boolean readBoolean() throws IOException {
        throw e;
    }

    @Override
    public byte readByte() throws IOException {
        throw e;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        throw e;
    }

    @Override
    public short readShort() throws IOException {
        throw e;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        throw e;
    }

    @Override
    public char readChar() throws IOException {
        throw e;
    }

    @Override
    public int readInt() throws IOException {
        throw e;
    }

    @Override
    public long readLong() throws IOException {
        throw e;
    }

    @Override
    public float readFloat() throws IOException {
        throw e;
    }

    @Override
    public double readDouble() throws IOException {
        throw e;
    }

    @Override
    public String readLine() throws IOException {
        throw e;
    }

    @Override
    public String readUTF() throws IOException {
        throw e;
    }

    @Override
    public NormalizedNode<?, ?> readNormalizedNode() throws IOException {
        throw e;
    }

    @Override
    public YangInstanceIdentifier readYangInstanceIdentifier() throws IOException {
        throw e;
    }

    @Override
    public PathArgument readPathArgument() throws IOException {
        throw e;
    }
}
