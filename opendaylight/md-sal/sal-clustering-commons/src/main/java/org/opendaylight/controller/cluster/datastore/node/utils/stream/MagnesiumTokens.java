/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

/**
 * Token types used in Magnesium encoding streams.
 */
final class MagnesiumTokens {
    /**
     * Node token types. Encoded as a single byte.
     */
    static final byte NODE_END             = 0x00;
    static final byte NODE_LEAF            = 0x01;
    static final byte NODE_CONTAINER       = 0x02;
    static final byte NODE_LIST            = 0x03;
    static final byte NODE_MAP             = 0x04;
    static final byte NODE_MAP_ORDERED     = 0x05;
    static final byte NODE_LEAFSET         = 0x06;
    static final byte NODE_LEAFSET_ORDERED = 0x07;
    static final byte NODE_CHOICE          = 0x08;
    static final byte NODE_AUGMENTATION    = 0x09;
    static final byte NODE_ANYXML          = 0x0A;
    static final byte NODE_ANYXML_MODELED  = 0x0B;

    static final byte NODE_LIST_ENTRY      = 0x0C;
    static final byte NODE_LEAFSET_ENTRY   = 0x0D;
    // Note: number of keys encoded as an additional int
    static final byte NODE_MAP_ENTRY       = 0x0E;
    // Note: NODE_LEAF without a value
    static final byte NODE_MAP_ENTRY_KEY   = 0x0F;

    // 0x10 - 0x7F reserved

    // Note: number of keys encoded within the field as (& 0x7F)
    static final byte NODE_MAP_ENTRY_0     = (byte) 0x80;
    static final byte NODE_MAP_ENTRY_127   = (byte) 0xFF;

    /**
     * Leaf value types.
     */
    static final byte VALUE_BOOLEAN_FALSE  = 0x00;
    static final byte VALUE_BOOLEAN_TRUE   = 0x01;
    static final byte VALUE_EMPTY          = 0x02;
    static final byte VALUE_INT8           = 0x03;
    static final byte VALUE_INT16          = 0x04;
    static final byte VALUE_INT32          = 0x05;
    static final byte VALUE_INT64          = 0x06;
    static final byte VALUE_UINT8          = 0x07;
    static final byte VALUE_UINT16         = 0x08;
    static final byte VALUE_UINT32         = 0x09;
    static final byte VALUE_UINT64         = 0x0A;
    static final byte VALUE_DECIMAL64      = 0x0B;

    // <= 16383 characters, as per DataOutput.writeUTF()
    static final byte VALUE_STRING_UTF     = 0x0C;
    // >= 16384 characters, encoded as unsigned short + UTF8 bytes
    static final byte VALUE_STRING_2B      = 0x0D;
    // >= 16384 characters, encoded as int + UTF8 bytes
    static final byte VALUE_STRING_4B      = 0x0E;
    // >= 16384 characters, int + DataOut.writeChars()
    static final byte VALUE_STRING_CHARS   = 0x0F;

    // Note: number of bytes encoded as 128 + unsigned byte
    static final byte VALUE_BINARY_1B      = 0x10;
    // Note: number of bytes encoded as 384 + unsigned short
    static final byte VALUE_BINARY_2B      = 0x11;
    // Note: number of bytes encoded as unsigned int
    static final byte VALUE_BINARY_4B      = 0x12;
    // Note: number of entries encoded as signed int
    static final byte VALUE_YIID           = 0x13;

    // Literal QName with revision, assigns next available code
    static final byte VALUE_QNAME          = 0x14;
    // Literal QName without revision, assigns next available code
    static final byte VALUE_QNAME_NOREV    = 0x15;
    // Note: single-byte code (unsigned!)
    static final byte VALUE_QNAME_CODE1    = 0x16;
    // Note: two-byte code (unsigned!)
    static final byte VALUE_QNAME_CODE2    = 0x17;
    // Note: four-byte code (signed!)
    static final byte VALUE_QNAME_CODE4    = 0x18;

    // 0x19 reserved
    // 0x1A reserved
    // 0x1B reserved
    // 0x1C reserved
    // 0x1D reserved
    // 0x1E reserved
    // 0x1F reserved

    // Special values
    static final byte VALUE_INT8_0         = 0x20;
    static final byte VALUE_INT8_1         = 0x21;
    static final byte VALUE_INT8_MIN       = 0x22;
    static final byte VALUE_INT8_MAX       = 0x23;
    static final byte VALUE_INT16_0        = 0x24;
    static final byte VALUE_INT16_1        = 0x25;
    static final byte VALUE_INT16_MIN      = 0x26;
    static final byte VALUE_INT16_MAX      = 0x27;
    static final byte VALUE_INT32_0        = 0x28;
    static final byte VALUE_INT32_1        = 0x29;
    static final byte VALUE_INT32_MIN      = 0x2A;
    static final byte VALUE_INT32_MAX      = 0x2B;
    static final byte VALUE_INT64_0        = 0x2C;
    static final byte VALUE_INT64_1        = 0x2D;
    static final byte VALUE_INT64_MIN      = 0x2E;
    static final byte VALUE_INT64_MAX      = 0x2F;
    static final byte VALUE_UINT8_0        = 0x30;
    static final byte VALUE_UINT8_1        = 0x31;
    static final byte VALUE_UINT8_MAX      = 0x32;
    static final byte VALUE_UINT16_0       = 0x33;
    static final byte VALUE_UINT16_1       = 0x34;
    static final byte VALUE_UINT16_MAX     = 0x35;
    static final byte VALUE_UINT32_0       = 0x36;
    static final byte VALUE_UINT32_1       = 0x37;
    static final byte VALUE_UINT32_MAX     = 0x38;
    static final byte VALUE_UINT64_0       = 0x39;
    static final byte VALUE_UINT64_1       = 0x3A;
    static final byte VALUE_UINT64_MAX     = 0x3B;
    static final byte VALUE_STRING_EMPTY   = 0x3C;

    // 0x3D reserved
    // 0x3E reserved

    // Note: number of bits = 32
    static final byte VALUE_BITS_32        = 0x3F;
    // Note: number of components encoded in (& 0x1F), _0 means empty
    static final byte VALUE_BITS_0         = 0x40;
    static final byte VALUE_BITS_31        = 0x5F;

    // Note: number of components encoded in (& 0x1F), _0 means empty
    static final byte VALUE_YIID_0         = 0x60;
    static final byte VALUE_YIID_31        = 0x7F;

    // Note: number of components encoded in (& 0x7F), _0 means empty
    static final byte VALUE_BINARY_0       = (byte) 0x80;
    static final byte VALUE_BINARY_127     = (byte) 0xFF;

    /*
     * QName coding details.
     */
    static final byte QNAME_NS_CODE_0      = 0x00;
    static final byte QNAME_NS_CODE_247    = (byte) 0xF7;

    // 0xF8 reserved
    // 0xF9 reserved
    // 0xFA reserved
    // 0xFB reserved

    // Note: code encoded as 248 + unsigned byte
    static final byte QNAME_NS_CODE_1B     = (byte) 0xFC;
    // Note: code encoded as 504 + unsigned short
    static final byte QNAME_NS_CODE_2B     = (byte) 0xFD;
    // Note: code encoded as signed int
    static final byte QNAME_NS_CODE_4B     = (byte) 0xFE;
    static final byte QNAME_NS             = (byte) 0xFF;

    private MagnesiumTokens() {

    }
}
