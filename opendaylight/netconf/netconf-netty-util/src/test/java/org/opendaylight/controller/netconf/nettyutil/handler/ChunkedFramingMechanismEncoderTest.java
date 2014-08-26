/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageConstants;

public class ChunkedFramingMechanismEncoderTest {

    private int chunkSize;
    @Mock
    private ChannelHandlerContext ctx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        chunkSize = 256;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSize() throws Exception {
        new ChunkedFramingMechanismEncoder(10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSizeMax() throws Exception {
        new ChunkedFramingMechanismEncoder(Integer.MAX_VALUE);
    }

    @Test
    public void testEncode() throws Exception {
        final List<ByteBuf> chunks = Lists.newArrayList();
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                chunks.add((ByteBuf) invocation.getArguments()[0]);
                return null;
            }
        }).when(ctx).write(anyObject());

        final ChunkedFramingMechanismEncoder encoder = new ChunkedFramingMechanismEncoder(chunkSize);
        final int lastChunkSize = 20;
        final ByteBuf src = Unpooled.wrappedBuffer(getByteArray(chunkSize * 4 + lastChunkSize));
        final ByteBuf destination = Unpooled.buffer();
        encoder.encode(ctx, src, destination);
        assertEquals(4, chunks.size());

        final int framingSize = "#256\n".getBytes().length + 1/* new line at end */;

        for (final ByteBuf chunk : chunks) {
            assertEquals(chunkSize + framingSize, chunk.readableBytes());
        }

        final int lastFramingSize = "#20\n".length() + NetconfMessageConstants.END_OF_CHUNK.length + 1/* new line at end */;
        assertEquals(lastChunkSize + lastFramingSize, destination.readableBytes());
    }

    private byte[] getByteArray(final int size) {
        final byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = 'a';
        }
        return bytes;
    }
}
