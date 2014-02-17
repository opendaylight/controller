/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;

/**
 * Netconf message header is used only when chunked framing mechanism is
 * supported. The header consists of only the length field.
 */
public final class NetconfMessageHeader {

    private long length;

    // \n#<length>\n
    private static final byte[] HEADER_BEGIN = new byte[] { (byte) 0x0a, (byte) 0x23 };

    private static final byte HEADER_END = (byte) 0x0a;

    private boolean parsed = false;

    public NetconfMessageHeader() {

    }

    public NetconfMessageHeader fromBytes(final byte[] bytes) {
        // the length is variable therefore bytes between HEADER_BEGIN and
        // HEADER_END mark the length
        // the length should be only numbers and therefore easily parsed with
        // ASCII
        this.length = Long.parseLong(Charsets.US_ASCII.decode(
                ByteBuffer.wrap(bytes, HEADER_BEGIN.length, bytes.length - HEADER_BEGIN.length - 1)).toString());
        Preconditions.checkState(this.length < Integer.MAX_VALUE && this.length > 0);
        this.parsed = true;
        return this;
    }

    public byte[] toBytes() {
        final byte[] l = String.valueOf(this.length).getBytes(Charsets.US_ASCII);
        final byte[] h = new byte[HEADER_BEGIN.length + l.length + 1];
        System.arraycopy(HEADER_BEGIN, 0, h, 0, HEADER_BEGIN.length);
        System.arraycopy(l, 0, h, HEADER_BEGIN.length, l.length);
        System.arraycopy(new byte[] {HEADER_END}, 0, h, HEADER_BEGIN.length + l.length, 1);
        return h;
    }

    // FIXME: improve precision to long
    public int getLength() {
        return (int) this.length;
    }

    public void setLength(final int length) {
        this.length = length;
    }

    /**
     * @return the parsed
     */
    public boolean isParsed() {
        return this.parsed;
    }

    /**
     * @param parsed
     *            the parsed to set
     */
    public void setParsed() {
        this.parsed = false;
    }
}
