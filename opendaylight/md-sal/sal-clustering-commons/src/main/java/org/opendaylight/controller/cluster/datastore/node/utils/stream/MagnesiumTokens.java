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
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Token types used in Magnesium encoding streams.
 */
final class MagnesiumTokens {
    /**
     * Leaf value types, encoded as a single byte.
     */
    static final class LeafValue {
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
         * A {@link BigDecimal}, encoded through {@link DataOutput#writeUTF(String)}.
         */
        static final byte DECIMAL64      = 0x0B;

        /**
         * A {@link String}, encoded through {@link DataOutput#writeUTF(String)}. Note this is generally true of any
         * string with less then 16384 characters.
         */
        static final byte STRING_UTF     = 0x0C;
        /**
         * A {@link String}, encoded as an {@code unsigned short} followed by that many UTF8-encoded bytes.
         */
        static final byte STRING_2B      = 0x0D;
        /**
         * A {@link String}, encoded as an {@code int >= 0} followed by that many UTF8-encoded bytes.
         */
        static final byte STRING_4B      = 0x0E;
        /**
         * A {@link String}, encoded as an {@code int >= 0} followed by that many UTF16 characters, i.e. as produced by
         * {@link DataOutput#writeChars(String)}.
         */
        static final byte STRING_CHARS   = 0x0F;

        /**
         * A {@code byte[])}, encoded as a single {@code unsigned byte} followed by 128-383 bytes. Note that smaller
         * arrays are encoded via {@link #BINARY_0} - {@link #BINARY_127} range.
         */
        static final byte BINARY_1B      = 0x10;
        /**
         * A {@code byte[])}, encoded as a single {@code unsigned short} followed by 384-65919 bytes. See also
         * {@link #BINARY_1B}.
         */
        static final byte BINARY_2B      = 0x11;
        /**
         * A {@code byte[])}, encoded as a single {@code int} followed by that many bytes bytes. See also
         * {@link #BINARY_2B}.
         */
        static final byte BINARY_4B      = 0x12;

        /**
         * A {@link YangInstanceIdentifier}, encoded as a single {@code int}, followed by that many components. See
         * also {@link #YIID_0}, which offers optimized encoding for up to 31 components.
         */
        // FIXME: document component encoding
        static final byte YIID           = 0x13;

        // FIXME: revise these

        // Literal QName with revision, assigns next available code
        static final byte QNAME          = 0x14;
        // Literal QName without revision, assigns next available code
        static final byte QNAME_NOREV    = 0x15;
        // Note: single-byte code (unsigned!)
        static final byte QNAME_CODE1    = 0x16;
        // Note: two-byte code (unsigned!) + 256
        static final byte QNAME_CODE2    = 0x17;
        // Note: four-byte code (signed!)
        static final byte QNAME_CODE4    = 0x18;

        // 0x19 reserved
        // 0x1A reserved
        // 0x1B reserved
        // 0x1C reserved
        // 0x1D reserved
        // 0x1E reserved
        // 0x1F reserved

        /**
         * Byte value {@code 0}.
         */
        static final byte INT8_0         = 0x20;
        /**
         * Byte value {@code 1}.
         */
        static final byte INT8_1         = 0x21;
        /**
         * Byte value {@link Byte#MIN_VALUE}.
         */
        static final byte INT8_MIN       = 0x22;
        /**
         * Byte value {@link Byte#MAX_VALUE}.
         */
        static final byte INT8_MAX       = 0x23;
        /**
         * Short value {@code 0}.
         */
        static final byte INT16_0        = 0x24;
        /**
         * Short value {@code 1}.
         */
        static final byte INT16_1        = 0x25;
        /**
         * Short value {@link Short#MIN_VALUE}.
         */
        static final byte INT16_MIN      = 0x26;
        /**
         * Short value {@link Short#MAX_VALUE}.
         */
        static final byte INT16_MAX      = 0x27;
        /**
         * Integer value {@code 0}.
         */
        static final byte INT32_0        = 0x28;
        /**
         * Integer value {@code 1}.
         */
        static final byte INT32_1        = 0x29;
        /**
         * Integer value {@link Integer#MIN_VALUE}.
         */
        static final byte INT32_MIN      = 0x2A;
        /**
         * Integer value {@link Integer#MAX_VALUE}.
         */
        static final byte INT32_MAX      = 0x2B;
        /**
         * Long value {@code 0}.
         */
        static final byte INT64_0        = 0x2C;
        /**
         * Long value {@code 1}.
         */
        static final byte INT64_1        = 0x2D;
        /**
         * Long value {@link Long#MAX_VALUE}.
         */
        static final byte INT64_MIN      = 0x2E;
        /**
         * Long value {@link Long#MAX_VALUE}.
         */
        static final byte INT64_MAX      = 0x2F;
        /**
         * {@link Uint8#ZERO} value.
         */
        static final byte UINT8_0        = 0x30;
        /**
         * {@link Uint8#ONE} value.
         */
        static final byte UINT8_1        = 0x31;
        /**
         * {@link Uint8#MAX_VALUE} value.
         */
        static final byte UINT8_MAX      = 0x32;
        /**
         * {@link Uint16#ZERO} value.
         */
        static final byte UINT16_0       = 0x33;
        /**
         * {@link Uint16#ONE} value.
         */
        static final byte UINT16_1       = 0x34;
        /**
         * {@link Uint16#MAX_VALUE} value.
         */
        static final byte UINT16_MAX     = 0x35;
        /**
         * {@link Uint32#ZERO} value.
         */
        static final byte UINT32_0       = 0x36;
        /**
         * {@link Uint32#ONE} value.
         */
        static final byte UINT32_1       = 0x37;
        /**
         * {@link Uint32#MAX_VALUE} value.
         */
        static final byte UINT32_MAX     = 0x38;
        /**
         * {@link Uint64#ZERO} value.
         */
        static final byte UINT64_0       = 0x39;
        /**
         * {@link Uint64#ONE} value.
         */
        static final byte UINT64_1       = 0x3A;
        /**
         * {@link Uint64#MAX_VALUE} value.
         */
        static final byte UINT64_MAX     = 0x3B;
        /**
         * Empty String value ({@code ""}).
         */
        static final byte STRING_EMPTY   = 0x3C;

        // 0x3D reserved
        // 0x3E reserved

        /**
         * Bits value, comprising of 32 individual values.
         */
        // FIXME: document that the individual bits should be encoded using interning String references
        static final byte BITS_32        = 0x3F;
        /**
         * Empty bits value. This code point starts the range ending with {@link #BITS_31}, where the number of bits can
         * be extracted as {@code code & 0x1F)}.
         */
        static final byte BITS_0         = 0x40;
        /**
         * Bits value, comprising of 31 individual values. See {@link #BITS_0}.
         */
        static final byte BITS_31        = 0x5F;

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

        private LeafValue() {

        }
    }

    /**
     * Node types. Encoded as a single byte, split as follows:
     * <pre>
     *   7 6 5 4 3 2 1 0
     *  +-+-+-+-+-+-+-+-+
     *  | P | A |  Type |
     *  +-+-+-+-+-+-+-+-+
     * <pre>
     * The fields being:
     * <ul>
     *   <li>Bits 7 and 6 (most significant): predicate presence. Only valid for NODE_MAP_ENTRY and NODE_LEAF</li>
     *   <li>Bits 5 and 4: addressing mode</li>
     *   <li>Bits 3-0 (least significant) node type</li>
     * </ul>
     */
    static final class NodeType {
        /**
         * End of node marker. Does not support addressing modes.
         */
        static final byte NODE_END             = 0x00; // N/A
        /**
         * A leaf node.
         */
        static final byte NODE_LEAF            = 0x01; // QName, skip value for map-entry
        static final byte NODE_CONTAINER       = 0x02; // QName
        static final byte NODE_LIST            = 0x03; // QName
        static final byte NODE_MAP             = 0x04; // QName
        static final byte NODE_MAP_ORDERED     = 0x05; // QName
        static final byte NODE_LEAFSET         = 0x06; // QName
        static final byte NODE_LEAFSET_ORDERED = 0x07; // QName
        static final byte NODE_CHOICE          = 0x08; // QName
        static final byte NODE_AUGMENTATION    = 0x09; // AugmentationIdentifier
        static final byte NODE_ANYXML          = 0x0A; // QName
        static final byte NODE_ANYXML_MODELED  = 0x0B; // QName
        static final byte NODE_LIST_ENTRY      = 0x0C; // QName/inherit
        static final byte NODE_LEAFSET_ENTRY   = 0x0D; // QName/inherit
        static final byte NODE_MAP_ENTRY       = 0x0E; // QName/inherit + predicates
        // 0x0F reserved

        /**
         * Inherit identifier from parent. This addressing mode is applicable in:
         * <ul>
         *   <li>{@link #NODE_END}, where an identifier is not applicable
         *   <li>{@link #NODE_LIST_ENTRY}, where the NodeIdentifier is inherited from parent {@link #NODE_LIST}</li>
         *   <li>{@link #NODE_LEAFSET_ENTRY}, where the QName inherited from parent and the value is inferred from the
         *       next {@link LeafValue} encoded</li>
         * </ul>
         */
        static final byte ADDR_PARENT     = 0x00;
        /**
         * Define a new QName-based identifier constant. For {@link #NODE_AUGMENTATION} this is a set of QNames. Assign
         * a new linear key to this constant.
         */
        static final byte ADDR_DEFINE     = 0x10;
        /**
         * Reference a previously {@link #ADDR_DEFINE}d identifier constant. This node byte is followed by an unsigned
         * byte, which holds the linear key previously defined (i.e. 0-255).
         */
        static final byte ADDR_LOOKUP_1B  = 0x20;
        /**
         * Reference a previously {@link #ADDR_DEFINE}d identifier constant. This node byte is followed by a signed int,
         * which holds the linear key previously defined.
         */
        static final byte ADDR_LOOKUP_4B = 0x30;

        /**
         * Predicate encoding: a single predicate is present in a {@link #NODE_MAP_ENTRY}. In case of {@link #NODE_LEAF}
         * encoded as part of a {@link #NODE_MAP_ENTRY} this bit indicates the <strong>value</strong> is not encoded and
         * should be looked up from the map entry's predicates.
         *
         * <p>
         * The predicate is encoded as a {@link #ADDR_DEFINE} or {@link #ADDR_LOOKUP_1B}/{@link #ADDR_LOOKUP_4B},
         * followed by an encoded {@link LeafValue}.
         */
        static final byte PREDICATE_ONE   = 0x40;
        /**
         * Predicate encoding: 0-255 predicates are present, as specified by the following {@code unsigned byte}. This
         * encoding is expected to be exceedingly rare. This should not be used to encode 0 or 1 predicate, those cases
         * should be encoded as:
         * <ul>
         *   <li>no PREDICATE_* set when there are no predicates (probably not valid anyway)</li>
         *   <li><{@link #PREDICATE_ONE} if there is only one predicate</li>
         * </ul>
         */
        static final byte PREDICATE_1B    = (byte) 0x80;
        /**
         * Predicate encoding 0 - {@link Integer#MAX_VALUE} predicates are present, as specified by the following
         * {@code int}. This should not be used where 0-255 predicates are present.
         */
        static final byte PREDICATE_4B    = (byte) 0xC0;

        private NodeType() {

        }
    }

    private MagnesiumTokens() {

    }
}
