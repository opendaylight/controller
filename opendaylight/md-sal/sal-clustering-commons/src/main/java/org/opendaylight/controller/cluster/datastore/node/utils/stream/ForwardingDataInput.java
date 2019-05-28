/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.DataInput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;

// Not a ForwardingObject because delegate() can legally throw and we do not want redirect toString()
abstract class ForwardingDataInput implements DataInput {

    abstract @NonNull DataInput delegate() throws IOException;

    @Override
    public final void readFully(final byte[] b) throws IOException {
        delegate().readFully(b);
    }

    @Override
    public final void readFully(final byte[] b, final int off, final int len) throws IOException {
        delegate().readFully(b, off, len);
    }

    @Override
    public final int skipBytes(final int n) throws IOException {
        return delegate().skipBytes(n);
    }

    @Override
    public final boolean readBoolean() throws IOException {
        return delegate().readBoolean();
    }

    @Override
    public final byte readByte() throws IOException {
        return delegate().readByte();
    }

    @Override
    public final int readUnsignedByte() throws IOException {
        return delegate().readUnsignedByte();
    }

    @Override
    public final short readShort() throws IOException {
        return delegate().readShort();
    }

    @Override
    public final int readUnsignedShort() throws IOException {
        return delegate().readUnsignedShort();
    }

    @Override
    public final char readChar() throws IOException {
        return delegate().readChar();
    }

    @Override
    public final int readInt() throws IOException {
        return delegate().readInt();
    }

    @Override
    public final long readLong() throws IOException {
        return delegate().readLong();
    }

    @Override
    public final float readFloat() throws IOException {
        return delegate().readFloat();
    }

    @Override
    public final double readDouble() throws IOException {
        return delegate().readDouble();
    }

    @Override
    public final String readLine() throws IOException {
        return delegate().readLine();
    }

    @Override
    public final String readUTF() throws IOException {
        return delegate().readUTF();
    }
}
