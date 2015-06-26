/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler;


import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.Writer;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Custom BufferedWriter optimized for netconf pipeline implemented instead of default BufferedWriter provided by jdk.
 * <p/>
 * The line separator instance field in java.io.BufferedWriter is
 * assigned using AccessController and takes considerable amount of time especially
 * if lots of BufferedWriters are created in the system.
 * <p/>
 * This implementation should only be used if newLine method is not required
 * such as netconf message to XML encoders.
 * Methods in this implementation are not synchronized.
 */
@NotThreadSafe
public final class BufferedWriter extends Writer {

    private static final int DEFAULT_CHAR_BUFFER_SIZE = 8192;

    private final Writer writer;
    private final char buffer[];
    private final int bufferSize;

    private int nextChar = 0;

    public BufferedWriter(final Writer writer) {
        this(writer, DEFAULT_CHAR_BUFFER_SIZE);
    }

    public BufferedWriter(final Writer writer, final int bufferSize) {
        super(writer);
        Preconditions.checkArgument(bufferSize > 0, "Buffer size <= 0");
        this.writer = writer;
        this.buffer = new char[bufferSize];
        this.bufferSize = bufferSize;
    }

    private void flushBuffer() throws IOException {
        if (nextChar == 0)
            return;
        writer.write(buffer, 0, nextChar);
        nextChar = 0;
    }

    @Override
    public void write(final int c) throws IOException {
        if (nextChar >= bufferSize)
            flushBuffer();
        buffer[nextChar++] = (char) c;
    }

    @Override
    public void write(final char[] buffer, final int offset, final int length) throws IOException {
        if ((offset < 0) || (offset > buffer.length) || (length < 0) ||
                ((offset + length) > buffer.length) || ((offset + length) < 0)) {
            throw new IndexOutOfBoundsException(String.format("Buffer size: %d, Offset: %d, Length: %d", buffer.length, offset, length));
        } else if (length == 0) {
            return;
        }

        if (length >= bufferSize) {
            flushBuffer();
            writer.write(buffer, offset, length);
            return;
        }

        int b = offset;
        final int t = offset + length;
        while (b < t) {
            final int d = Math.min(bufferSize - nextChar, t - b);
            System.arraycopy(buffer, b, this.buffer, nextChar, d);
            b += d;
            nextChar += d;
            if (nextChar >= bufferSize)
                flushBuffer();
        }
    }

    @Override
    public void write(final String string, final int offset, final int length) throws IOException {
        int b = offset;
        final int t = offset + length;
        while (b < t) {
            final int d = Math.min(bufferSize - nextChar, t - b);
            string.getChars(b, b + d, buffer, nextChar);
            b += d;
            nextChar += d;
            if (nextChar >= bufferSize)
                flushBuffer();
        }
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            flushBuffer();
        } finally {
            writer.close();
        }
    }
}
