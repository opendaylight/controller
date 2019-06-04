/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import static com.google.common.base.Verify.verify;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Simplistic class for encoding an integer in shortest number of bytes.
 *
 * @author Robert Varga
 */
final class IntegerCoding {
    private static final int MAX_ONE   = 0x0000007F;
    private static final int MAX_TWO   = 0x00003FFF;
    private static final int MAX_THREE = 0x001FFFFF;
    private static final int MAX_FOUR  = 0x0FFFFFFF;
    private static final int NEXT_MASK = 0x80;

    private IntegerCoding() {

    }

    static int decodeInt(final DataInput input) throws IOException {
        int read = input.readByte();
        int masked = read & MAX_ONE;
        if (masked == read) {
            return masked;
        }

        int result = masked;
        read = input.readByte();
        masked = read & MAX_ONE;
        result |= masked << 7;
        if (masked != read) {
            read = input.readByte();
            masked = read & MAX_ONE;
            result |= masked << 14;
            if (masked != read) {
                read = input.readByte();
                masked = read & MAX_ONE;
                result |= masked << 21;
                if (masked != read) {
                    result |= (input.readByte() & MAX_ONE) << 28;
                }
            }
        }

        return result;
    }

    static void encodeInt(final DataOutput output, final int value) throws IOException {
        verify(value >= 0);
        if (value < MAX_ONE) {
            // Non-arithmetic special-case
            output.writeByte(value);
        } else if (writeByte(output, value, 7, MAX_TWO)
                && writeByte(output, value, 14, MAX_THREE)
                && writeByte(output, value, 21, MAX_FOUR)) {
            output.writeByte(value >>> 28 & MAX_ONE);
        }
    }

    private static boolean writeByte(final DataOutput output, final int value, final int shift, final int max)
            throws IOException {
        if (value < max) {
            output.write(value >>> shift);
            return false;
        }

        output.writeByte(value >>> shift & MAX_ONE | NEXT_MASK);
        return true;
    }
}
