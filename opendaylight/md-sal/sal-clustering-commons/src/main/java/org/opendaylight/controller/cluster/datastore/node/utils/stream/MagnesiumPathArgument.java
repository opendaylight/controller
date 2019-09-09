/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

/**
 * Path Argument types used in Magnesium encoding. These are encoded as a single byte, three bits of which are reserved
 * for PathArgument type itself:
 * <pre>
 *   7 6 5 4 3 2 1 0
 *  +-+-+-+-+-+-+-+-+
 *  |         | Type|
 *  +-+-+-+-+-+-+-+-+
 * </pre>
 * There are for type defined:
 * <ul>
 *   <li>{@link #AUGMENTATION_IDENTIFIER}, which additionally holds the number of QName elements encoded:
 *     <pre>
 *        7 6 5 4 3 2 1 0
 *       +-+-+-+-+-+-+-+-+
 *       |  Count  |0 0 0|
 *       +-+-+-+-+-+-+-+-+
 *     </pre>
 *     Where count is coded as an unsigned integer, with {@link #AID_COUNT_1B} and {@link #AID_COUNT_2B} and
 *     {@link #AID_COUNT_4B} indicating extended coding with up to 4 additional bytes. This byte is followed by
 *     {@code count} {@link MagnesiumValue} QNames.
 *     <pre>
 *       7 6 5 4 3 2 1 0
 *      +-+-+-+-+-+-+-+-+
 *      |0 0 0| Q |0 0 1|
 *      +-+-+-+-+-+-+-+-+
 *     </pre>
 *     Where QName coding is achieved via {@link #QNAME_DEF}, {@link #QNAME_REF_1B}, {@link #QNAME_REF_2B} and
 *     {@link #QNAME_REF_4B}.
 *   </li>
 *   <li>{@link #NODE_IDENTIFIER_WITH_PREDICATES}, which encodes a QName same way NodeIdentifier does:
 *     <pre>
 *       7 6 5 4 3 2 1 0
 *      +-+-+-+-+-+-+-+-+
 *      | Size| Q |0 1 0|
 *      +-+-+-+-+-+-+-+-+
 *      </pre>
 *      but additionally encodes number of predicates contained using {@link #SIZE_0} through {@link #SIZE_4}. If that
 *      number cannot be expressed, {@link #SIZE_1B}, {@value #SIZE_2B} and {@link #SIZE_4B} indicate number and format
 *      of additional bytes that hold number of predicates.
 *
 *      <p>
 *      This is then followed by the specified number of QName/Object key/value pairs based on {@link MagnesiumValue}
 *      encoding.
 *   </li>
 *   <li>{@link #NODE_WITH_VALUE}, which encodes a QName same way NodeIdentifier does:
 *     <pre>
 *       7 6 5 4 3 2 1 0
 *      +-+-+-+-+-+-+-+-+
 *      |0 0 0| Q |0 1 1|
 *      +-+-+-+-+-+-+-+-+
 *     </pre>
 *     but is additionally followed by a single encoded value, as per {@link MagnesiumValue}.
 *   </li>
 * </ul>
 */
final class MagnesiumPathArgument {
    // 3 bits reserved for type...
    static final byte AUGMENTATION_IDENTIFIER         = 0x00;
    static final byte NODE_IDENTIFIER                 = 0x01;
    static final byte NODE_IDENTIFIER_WITH_PREDICATES = 0x02;
    static final byte NODE_WITH_VALUE                 = 0x03;

    // ... leaving four values (or one bit) currently unused
    // 0x04 reserved
    // 0x05 reserved
    // 0x06 reserved
    // 0x07 reserved

    // In case of AUGMENTATION_IDENTIFIER, top 5 bits are used to encode the number of path arguments, except last three
    // values. This means that up to AugmentationIdentifiers with up to 28 components have this length encoded inline,
    // otherwise we encode them in following 1, 2 or 4 bytes
    static final byte AID_COUNT_1B                    = (byte) 0xE8; // Unsigned
    static final byte AID_COUNT_2B                    = (byte) 0xF0; // Unsigned
    static final byte AID_COUNT_4B                    = (byte) 0xF8;
    static final byte AID_COUNT_MASK                  = AID_COUNT_4B;
    static final byte AID_COUNT_SHIFT                 = 3;

    static final byte QNAME_DEF                       = 0x00;
    static final byte QNAME_REF_1B                    = 0x08; // Unsigned
    static final byte QNAME_REF_2B                    = 0x10; // Unsigned
    static final byte QNAME_REF_4B                    = 0x18; // Signed
    static final byte QNAME_MASK                      = QNAME_REF_4B;

    static final byte SIZE_0                          = 0x00;
    static final byte SIZE_1                          = 0x20;
    static final byte SIZE_2                          = 0x40;
    static final byte SIZE_3                          = 0x60;
    static final byte SIZE_4                          = (byte) 0x80;
    static final byte SIZE_1B                         = (byte) 0xA0;
    static final byte SIZE_2B                         = (byte) 0xC0;
    static final byte SIZE_4B                         = (byte) 0xE0;
    static final byte SIZE_MASK                       = SIZE_4B;
    static final byte SIZE_SHIFT                      = 5;

    private MagnesiumPathArgument() {

    }
}
