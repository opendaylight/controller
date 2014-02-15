/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import java.nio.ByteBuffer;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

/**
 * Netconf message header is used only when chunked framing mechanism is
 * supported. The header consists of only the length field.
 */
@Deprecated
public final class NetconfMessageHeader {
    // \n#<length>\n
    private static final byte[] HEADER_START = new byte[] { (byte) 0x0a, (byte) 0x23 };
    private static final byte HEADER_END = (byte) 0x0a;
    private final long length;

    public NetconfMessageHeader(final long length) {
        Preconditions.checkArgument(length < Integer.MAX_VALUE && length > 0);
        this.length = length;
    }

    public byte[] toBytes() {
        return toBytes(this.length);
    }

    // FIXME: improve precision to long
    public int getLength() {
        return (int) this.length;
    }

    public static NetconfMessageHeader fromBytes(final byte[] bytes) {
        // the length is variable therefore bytes between headerBegin and
        // headerEnd mark the length
        // the length should be only numbers and therefore easily parsed with
        // ASCII
        long length = Long.parseLong(Charsets.US_ASCII.decode(
                ByteBuffer.wrap(bytes, HEADER_START.length, bytes.length - HEADER_START.length - 1)).toString());

        return new NetconfMessageHeader(length);
    }

    public static byte[] toBytes(final long length) {
        final byte[] l = String.valueOf(length).getBytes(Charsets.US_ASCII);
        final byte[] h = new byte[HEADER_START.length + l.length + 1];
        System.arraycopy(HEADER_START, 0, h, 0, HEADER_START.length);
        System.arraycopy(l, 0, h, HEADER_START.length, l.length);
        System.arraycopy(new byte[] { HEADER_END }, 0, h, HEADER_START.length + l.length, 1);
        return h;
    }
}
