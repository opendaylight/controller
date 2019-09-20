/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

/**
 * Magnesium encoding Node types. Encoded as a single byte, split as follows:
 * <pre>
 *   7 6 5 4 3 2 1 0
 *  +-+-+-+-+-+-+-+-+
 *  | P | A |  Type |
 *  +-+-+-+-+-+-+-+-+
 * </pre>
 * The fields being:
 * <ul>
 *   <li>Bits 7 and 6 (most significant): predicate presence. Only valid for NODE_MAP_ENTRY and NODE_LEAF</li>
 *   <li>Bits 5 and 4: addressing mode</li>
 *   <li>Bits 3-0 (least significant) node type</li>
 * </ul>
 */
// TODO: restructure this into some concrete examples
//- a leaf referencing a previously-encoded NodeIdentifier would take
//6 bytes:
//  (byte)    NodeTypes.LEAF_NODE
//  (byte)    TokenTypes.IS_QNAME_CODE
//  (int)     code value
//where as new tokens can do that in as few as 2 bytes:
//  (byte)    NodeType.(NODE_LEAF | ADDR_LOOKUP_1B)
//  (byte)    code value
//with worst-case being 5 bytes:
//  (byte)    NodeType.(NODE_LEAF | ADDR_LOOKUP_4B)
//  (int)     code value
//- a map entry node referencing previously-encoded QNames and a single
//predicate would take a base of 15 bytes (not counting value object):
//  (byte)    NodeTypes.MAP_ENTRY_NODE
//  (byte)    TokenTypes.IS_QNAME_CODE
//  (int)     code value
//  (int)     size of predicates
//  (byte)    TokenTypes.IS_QNAME_CODE
//  (int)     code value
//whereas new tokens can do that in as few as 3 bytes:
//  (byte)    NodeType.(NODE_MAP_ENTRY | ADDR_LOOKUP_1B | PREDICATE_ONE)
//  (byte)    code value
//  (byte)    code value
//this ability is maintained for up to 255 predicates with:
//  (byte)    NodeType.(NODE_MAP_ENTRY | ADDR_LOOKUP_1B | PREDICATE_1B)
//  (byte)    code value
//  (byte)    size of predicates
//  (byte)    code value [0-255]
//- a leaf representing a key inside a map entry has the ability to skip
//value encoding by being as simple as:
//  (byte)    NodeTYpe.(NODE_LEAF | ADDR_LOOKUP_1B | PREDICATE_ONE)
//  (byte)    code value
//
final class MagnesiumNode {
    /**
     * End of node marker. Does not support addressing modes.
     */
    static final byte NODE_END             = 0x00; // N/A
    /**
     * A leaf node. Encoding can specify {@link #PREDICATE_ONE}, which indicates the value is skipped as the encoder
     * has emitted a parent MapNode, whose identifier contains the value.
     */
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
    static final byte NODE_LIST_ENTRY      = 0x0B;
    static final byte NODE_LEAFSET_ENTRY   = 0x0C;
    static final byte NODE_MAP_ENTRY       = 0x0D;

    // TODO: either implement or remove this coding. While Lithium has emit code, it lacks the code do read such nodes,
    //       which most probably means we do not need to bother ...
    static final byte NODE_ANYXML_MODELED  = 0x0E;
    // 0x0F reserved for anydata
    static final byte TYPE_MASK            = 0x0F;


    /**
     * Inherit identifier from parent. This addressing mode is applicable in:
     * <ul>
     *   <li>{@link #NODE_END}, where an identifier is not applicable
     *   <li>{@link #NODE_LIST_ENTRY}, where the NodeIdentifier is inherited from parent {@link #NODE_LIST}</li>
     *   <li>{@link #NODE_MAP_ENTRY}, where the NodeIdentifier is inherited from parent {@link #NODE_MAP} or
     *       {@link #NODE_MAP_ORDERED}</li>
     *   <li>{@link #NODE_LEAFSET_ENTRY}, where the QName inherited from parent and the value is inferred from the
     *       next {@link MagnesiumValue} encoded</li>
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
    static final byte ADDR_LOOKUP_4B  = 0x30;
    static final byte ADDR_MASK       = ADDR_LOOKUP_4B;

    /**
     * Predicate encoding: no predicates are present in a {@link #NODE_MAP_ENTRY}.
     */
    static final byte PREDICATE_ZERO = 0x00;

    /**
     * Predicate encoding: a single predicate is present in a {@link #NODE_MAP_ENTRY}. In case of {@link #NODE_LEAF}
     * encoded as part of a {@link #NODE_MAP_ENTRY} this bit indicates the <strong>value</strong> is not encoded and
     * should be looked up from the map entry's predicates.
     *
     * <p>
     * The predicate is encoded as a {@link #ADDR_DEFINE} or {@link #ADDR_LOOKUP_1B}/{@link #ADDR_LOOKUP_4B},
     * followed by an encoded {@link MagnesiumValue}.
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
    static final byte PREDICATE_4B    = (byte) (PREDICATE_ONE | PREDICATE_1B);
    static final byte PREDICATE_MASK  = PREDICATE_4B;

    private MagnesiumNode() {

    }
}