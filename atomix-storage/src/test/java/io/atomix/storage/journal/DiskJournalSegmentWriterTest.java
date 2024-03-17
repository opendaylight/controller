/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.atomix.storage.journal;

import static io.atomix.storage.journal.DiskJournalSegmentWriter.prepareNextEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiskJournalSegmentWriterTest {
    private static final int BUFFER_SIZE = 56;

    @Mock
    private SeekableByteChannel channel;

    private final ByteBuffer buffer = ByteBuffer.wrap(new byte[BUFFER_SIZE]);

    @Test
    void testReadFastPath() throws Exception {
        buffer.putInt(42).putInt(0).put(new byte[42]).flip();

        assertEquals(42, prepareNextEntry(channel, buffer));
        assertEquals(4, buffer.position());
        assertEquals(46, buffer.remaining());
    }

    @Test
    void testEmptyBufferEndOfFile() throws Exception {
        buffer.position(BUFFER_SIZE);

        prepareRead(buf -> {
            assertEquals(0, buf.position());
            return 0;
        });

        assertEquals(0, prepareNextEntry(channel, buffer));
        assertEquals(0, buffer.remaining());
    }

    @Test
    void testEmptyBuffer() throws Exception {
        buffer.position(BUFFER_SIZE);

        prepareRead(buf -> {
            assertEquals(0, buf.position());
            buf.putInt(20).putInt(0).put(new byte[20]);
            return 28;
        });

        assertEquals(20, prepareNextEntry(channel, buffer));
        assertEquals(4, buffer.position());
        assertEquals(24, buffer.remaining());
    }

    @Test
    void testEmptyBufferNotEnough() throws Exception {
        buffer.position(BUFFER_SIZE);

        prepareRead(buf -> {
            assertEquals(0, buf.position());
            buf.putInt(42).putInt(0).put(new byte[20]);
            return 28;
        });

        assertEquals(0, prepareNextEntry(channel, buffer));
        assertEquals(0, buffer.position());
        assertEquals(28, buffer.remaining());
    }

    @Test
    void testHeaderWithNotEnough() throws Exception {
        buffer.putInt(42).putInt(0).put(new byte[20]).flip();

        prepareRead(buf -> {
            assertEquals(28, buf.position());
            return 0;
        });

        assertEquals(0, prepareNextEntry(channel, buffer));
        assertEquals(28, buffer.remaining());
        assertEquals(0, buffer.position());
    }

    final void prepareRead(final ToIntFunction<ByteBuffer> onRead) throws Exception {
        doAnswer(invocation -> onRead.applyAsInt(invocation.getArgument(0))).when(channel).read(any());
    }
}
