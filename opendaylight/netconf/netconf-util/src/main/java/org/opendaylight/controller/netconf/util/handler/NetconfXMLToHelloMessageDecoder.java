/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.handler;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.w3c.dom.Document;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

/**
 * Customized NetconfXMLToMessageDecoder that reads additional header with
 * session metadata from
 * {@link org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage}
 * . Used by netconf server to retrieve information about session metadata.
 */
public class NetconfXMLToHelloMessageDecoder extends NetconfXMLToMessageDecoder {

    private static final List<byte[]> POSSIBLE_ENDS = ImmutableList.of(
            new byte[] { ']', '\n' },
            new byte[] { ']', '\r', '\n' });
    private static final List<byte[]> POSSIBLE_STARTS = ImmutableList.of(
            new byte[] { '[' },
            new byte[] { '\r', '\n', '[' },
            new byte[] { '\n', '[' });

    private String additionalHeaderCache;

    @Override
    protected byte[] preprocessMessageBytes(byte[] bytes) {
        // Extract bytes containing header with additional metadata

        if (startsWithAdditionalHeader(bytes)) {
            // Auth information containing username, ip address... extracted for monitoring
            int endOfAuthHeader = getAdditionalHeaderEndIndex(bytes);
            if (endOfAuthHeader > -1) {
                byte[] additionalHeaderBytes = Arrays.copyOfRange(bytes, 0, endOfAuthHeader + 2);
                additionalHeaderCache = additionalHeaderToString(additionalHeaderBytes);
                bytes = Arrays.copyOfRange(bytes, endOfAuthHeader + 2, bytes.length);
            }
        }

        return bytes;
    }

    @Override
    protected void cleanUpAfterDecode() {
        additionalHeaderCache = null;
    }

    @Override
    protected NetconfMessage buildNetconfMessage(Document doc) {
        return new NetconfHelloMessage(doc, additionalHeaderCache == null ? null
                : NetconfHelloMessageAdditionalHeader.fromString(additionalHeaderCache));
    }

    private int getAdditionalHeaderEndIndex(byte[] bytes) {
        for (byte[] possibleEnd : POSSIBLE_ENDS) {
            int idx = findByteSequence(bytes, possibleEnd);

            if (idx != -1) {
                return idx;
            }
        }

        return -1;
    }

    private static int findByteSequence(final byte[] bytes, final byte[] sequence) {
        if (bytes.length < sequence.length) {
            throw new IllegalArgumentException("Sequence to be found is longer than the given byte array.");
        }
        if (bytes.length == sequence.length) {
            if (Arrays.equals(bytes, sequence)) {
                return 0;
            } else {
                return -1;
            }
        }
        int j = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == sequence[j]) {
                j++;
                if (j == sequence.length) {
                    return i - j + 1;
                }
            } else {
                j = 0;
            }
        }
        return -1;
    }

    private boolean startsWithAdditionalHeader(byte[] bytes) {
        for (byte[] possibleStart : POSSIBLE_STARTS) {
            int i = 0;
            for (byte b : possibleStart) {
                if(bytes[i++] != b)
                    break;

                if(i == possibleStart.length)
                    return true;
            }
        }

        return false;
    }

    private String additionalHeaderToString(byte[] bytes) {
        return Charsets.UTF_8.decode(ByteBuffer.wrap(bytes)).toString();
    }

}
