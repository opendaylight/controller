/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.DataOutput;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Magnesium encoding value types. Serialized as a single byte.
 */
/*
 * Note these constants are organized by their absolute value, which is slightly counter-intuitive when trying to make
 * sense of what is going on.
 *
 * TODO: create some sort of facility which would provide symbolic names for debugging and documentation purposes.
 */
final class MagnesiumValue {
    /**
     * {@link Boolean#FALSE} value.
     */
    static final byte BOOLEAN_FALSE  = 0x00;
    /**
     * {@link Boolean#TRUE} value.
     */
    static final byte BOOLEAN_TRUE   = 0x01;
    /**
     * An {@link Empty} value.
     */
    static final byte EMPTY          = 0x02;
    /**
     * A Byte, followed by a byte holding the value.
     */
    static final byte INT8           = 0x03;
    /**
     * A Short, followed by a {@code short} holding the value.
     */
    static final byte INT16          = 0x04;
    /**
     * An Integer, followed by an {@code int} holding the value.
     */
    static final byte INT32          = 0x05;
    /**
     * A Long, followed by an {@code long} holding the value.
     */
    static final byte INT64          = 0x06;
    /**
     * A Uint8, followed by an {@code unsigned byte} holding the value.
     */
    static final byte UINT8          = 0x07;
    /**
     * A Uint16, followed by a {@code unsigned short} holding the value.
     */
    static final byte UINT16         = 0x08;
    /**
     * A Uint32, followed by an {@code unsigned int} holding the value.
     */
    static final byte UINT32         = 0x09;
    /**
     * A Uint64, followed by an {@code unsigned long} holding the value.
     */
    static final byte UINT64         = 0x0A;
    /**
     * A {@link String}, encoded through {@link DataOutput#writeUTF(String)}. Note this is generally true of any
     * string with less then 16384 characters.
     */
    static final byte STRING_UTF     = 0x0B;
    /**
     * A {@link String}, encoded as an {@code unsigned short} followed by that many UTF8-encoded bytes.
     */
    static final byte STRING_2B      = 0x0C;
    /**
     * A {@link String}, encoded as an {@code int >= 0} followed by that many UTF8-encoded bytes.
     */
    static final byte STRING_4B      = 0x0D;
    /**
     * A {@link String}, encoded as an {@code int >= 0} followed by that many UTF16 characters, i.e. as produced by
     * {@link DataOutput#writeChars(String)}.
     */
    static final byte STRING_CHARS   = 0x0E;
    /**
     * Utility 'reference coding' codepoint with {@code unsigned byte} offset. This is not a value type, but is used in
     * context of various schema-related encodings like constant strings, QNameModule and similar.
     */
    static final byte STRING_REF_1B  = 0x0F;
    /**
     * Utility 'reference coding' codepoint with {@code unsigned short} offset. This is not a value type, but is used in
     * context of various schema-related encodings like constant strings, QNameModule and similar.
     */
    static final byte STRING_REF_2B  = 0x10;
    /**
     * Utility 'reference coding' codepoint with {@code int} offset. This is not a value type, but is used in context of
     * various schema-related encodings like constant strings, QNameModule and similar.
     */
    static final byte STRING_REF_4B  = 0x11;
    /**
     * A {@code byte[])}, encoded as a single {@code unsigned byte} followed by 128-383 bytes. Note that smaller
     * arrays are encoded via {@link #BINARY_0} - {@link #BINARY_127} range.
     */
    static final byte BINARY_1B      = 0x12;
    /**
     * A {@code byte[])}, encoded as a single {@code unsigned short} followed by 384-65919 bytes. See also
     * {@link #BINARY_1B}.
     */
    static final byte BINARY_2B      = 0x13;
    /**
     * A {@code byte[])}, encoded as a single {@code int} followed by that many bytes bytes. See also
     * {@link #BINARY_2B}.
     */
    static final byte BINARY_4B      = 0x14;
    /**
     * A {@link YangInstanceIdentifier}, encoded as a single {@code int}, followed by that many components. See
     * also {@link #YIID_0}, which offers optimized encoding for up to 31 components. Components are encoded using
     * {@link MagnesiumPathArgument} coding.
     */
    static final byte YIID           = 0x15;
    /**
     * A QName literal. Encoded as QNameModule + String. This literal is expected to be memoized on receiver side, which
     * assigns the next linear integer identifier. The sender will memoize it too and further references to this QName
     * will be made via {@link #QNAME_REF_1B}, {@link #QNAME_REF_2B} or {@link #QNAME_REF_4B}.
     *
     * <p>
     * Note that QNameModule (and String in this context) encoding works similarly -- it can only occur as part of a
     * QName (coming from here or {@link MagnesiumPathArgument}) and is subject to the same memoization.
     *
     * <p>
     * For example, given two QNames {@code foo = QName.create("foo", "abc")} and
     * {@code bar = QName.create("foo", "def")}, if they are written in order {@code foo, bar, foo}, then the following
     * events are emitted:
     * <pre>
     *   QNAME                (define QName, assign shorthand Q0)
     *   STRING_UTF   "foo"   ("foo", assign shorthand S0, implies define QNameModule, assign shorthand M0)
     *   STRING_EMPTY         (foo's non-existent revision)
     *   STRING_UTF   "abc"   ("abc", assign shorthand S1)
     *   QNAME                (define QName, assign shorthand Q1)
     *   MODREF_1B    (byte)0 (reference M0)
     *   STRING_UTF   "def"   ("def", assign shorthand S2)
     *   QNAME_REF_1B (byte)0 (reference Q0)
     * </pre>
     */
    // Design note: STRING_EMPTY is required to *NOT* establish a shortcut, as that is less efficient (and hence does
    //              not make sense from the sender, the receiver or the serialization protocol itself.
    static final byte QNAME          = 0x16;
    /**
     * Reference a QName previously defined via {@link #QNAME}. Reference number is encoded as {@code unsigned byte}.
     */
    static final byte QNAME_REF_1B   = 0x17;
    /**
     * Reference a QName previously defined via {@link #QNAME}. Reference number is encoded as {@code unsigned short}.
     */
    static final byte QNAME_REF_2B   = 0x18;
    /**
     * Reference a QName previously defined via {@link #QNAME}. Reference number is encoded as {@code int}.
     */
    static final byte QNAME_REF_4B   = 0x19;
    /**
     * Reference a previously defined QNameModule. Reference number is encoded as {@code unsigned byte}.
     */
    static final byte MODREF_1B      = 0x1A;
    /**
     * Reference a previously defined QNameModule. Reference number is encoded as {@code unsigned short}.
     */
    static final byte MODREF_2B      = 0x1B;
    /**
     * Reference a previously defined QNameModule. Reference number is encoded as {@code int}.
     */
    static final byte MODREF_4B      = 0x1C;

    /**
     * A {@link BigDecimal}, encoded through {@link DataOutput#writeUTF(String)}.
     */
    // This is legacy compatibility. At some point we will remove support for writing these.
    static final byte BIGDECIMAL     = 0x1D;
    /**
     * A {@link BigInteger}, encoded through {@link DataOutput#writeUTF(String)}.
     */
    // This is legacy compatibility. At some point we will remove support for writing these.
    static final byte BIGINTEGER     = 0x1E;

    // 0x1F reserved

    /**
     * Byte value {@code 0}.
     */
    static final byte INT8_0         = 0x20;
    /**
     * Short value {@code 0}.
     */
    static final byte INT16_0        = 0x21;
    /**
     * Integer value {@code 0}.
     */
    static final byte INT32_0        = 0x22;
    /**
     * Long value {@code 0}.
     */
    static final byte INT64_0        = 0x23;
    /**
     * {@link Uint8#ZERO} value.
     */
    static final byte UINT8_0        = 0x24;
    /**
     * {@link Uint16#ZERO} value.
     */
    static final byte UINT16_0       = 0x25;
    /**
     * {@link Uint32#ZERO} value.
     */
    static final byte UINT32_0       = 0x26;
    /**
     * {@link Uint64#ZERO} value.
     */
    static final byte UINT64_0       = 0x27;
    /**
     * Empty String value ({@code ""}).
     */
    static final byte STRING_EMPTY   = 0x28;
    /**
     * {@link #INT32} with a 2-byte operand.
     */
    static final byte INT32_2B       = 0x29;
    /**
     * {@link #UINT32} with a 2-byte operand.
     */
    static final byte UINT32_2B      = 0x2A;
    /**
     * {@link #INT64} with a 4-byte operand.
     */
    static final byte INT64_4B       = 0x2B;
    /**
     * {@link #UINT64} with a 4-byte operand.
     */
    static final byte UINT64_4B      = 0x2C;

    // 0x2D - 0x39 reserved

    /**
     * Empty bits value. This code point starts the range, where the number of bits can be extracted as
     * {@code code & 0x1F)}. Last three values of this range are used to encode more than 28 entries.
     */
    static final byte BITS_0         = 0x40;
    /**
     * A bits value of up to 255 entries. Number of values is encoded as the following {@code unsigned byte}.
     */
    static final byte BITS_1B        = 0x5D;
    /**
     * A bits value of up to 65535 entries. Number of values is encoded as the following {@code unsigned short}.
     */
    static final byte BITS_2B        = 0x5E;
    /**
     * A bits value. Number of values is encoded as the following {@code int}.
     */
    static final byte BITS_4B        = 0x5F;

    /**
     * {@link YangInstanceIdentifier} with zero components. This code point starts the range ending with
     * {@link #YIID_31}, where the number of components can be extracted as {@code code & 0x1F}. Identifiers with
     * more than 31 components are encoded using {@link #YIID}.
     */
    static final byte YIID_0         = 0x60;
    /**
     * {@link YangInstanceIdentifier} with 31 components. See {@link #YIID_0}.
     */
    static final byte YIID_31        = 0x7F;

    /**
     * A {@code byte[]} with 0 bytes. This code point starts the range ending with {@link #BINARY_127}, where
     * the number of bytes can be extracted as {@code code & 0x7F}. Arrays longer than 127 bytes are encoded using
     * {@link #BINARY_1B}, {@link #BINARY_2B} and {@link #BINARY_4B} as needed.
     */
    static final byte BINARY_0       = (byte) 0x80;
    /**
     * A {@code byte[]} with 127 bytes. See {@link #BINARY_0}.
     */
    static final byte BINARY_127     = (byte) 0xFF;

    private MagnesiumValue() {

    }
}
