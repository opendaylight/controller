/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.math.BigInteger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public class Ctrl1919Test {
    private static final QName LST = QName.create("urn:odl-ctlr1919", "lst");
    private static final QName A = QName.create("urn:odl-ctlr1919", "a");
    private static final QName B = QName.create("urn:odl-ctlr1919", "b");
    private static final QName C = QName.create("urn:odl-ctlr1919", "c");
    private static final QName D = QName.create("urn:odl-ctlr1919", "d");
    private static final QName E = QName.create("urn:odl-ctlr1919", "e");
    private static final QName F = QName.create("urn:odl-ctlr1919", "f");
    private static final QName G = QName.create("urn:odl-ctlr1919", "g");
    private static final QName H = QName.create("urn:odl-ctlr1919", "h");
    private static final NodeIdentifier LST_ID = NodeIdentifier.create(LST);

    private static EffectiveModelContext CONTEXT;

    @BeforeClass
    public static void beforeClass() {
        CONTEXT = YangParserTestUtils.parseYangResource("/ctrl1919/odl-ctlr1919.yang");
    }

    @Test
    public void testListTranslation() throws IOException {
        ReusableNormalizedNodePruner pruner = ReusableNormalizedNodePruner.forSchemaContext(CONTEXT)
                .withUintAdaption();
        pruner.initializeForPath(YangInstanceIdentifier.empty());
        NormalizedNodeWriter writer = NormalizedNodeWriter.forStreamWriter(pruner);
        writer.write(Builders.mapBuilder()
            .withNodeIdentifier(LST_ID)
            .withChild(Builders.mapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(LST,  ImmutableMap.<QName, Object>builder()
                    .put(A, (byte) 1)
                    .put(B, (short) 1)
                    .put(C, 1)
                    .put(D, 1L)
                    .put(E, (short) 1)
                    .put(F, 1)
                    .put(G, 1L)
                    .put(H, BigInteger.ONE)
                    .build()))
                .withChild(ImmutableNodes.leafNode(A, (byte) 1))
                .withChild(ImmutableNodes.leafNode(B, (short) 1))
                .withChild(ImmutableNodes.leafNode(C, 1))
                .withChild(ImmutableNodes.leafNode(D, 1L))
                .withChild(ImmutableNodes.leafNode(E, (short) 1))
                .withChild(ImmutableNodes.leafNode(F, 1))
                .withChild(ImmutableNodes.leafNode(G, 1L))
                .withChild(ImmutableNodes.leafNode(H, BigInteger.ONE))
                .build())
            .build());

        pruner.close();
        final NormalizedNode<?, ?> outputNode = pruner.getResult().get();

        assertEquals(Builders.mapBuilder()
            .withNodeIdentifier(LST_ID)
            .withChild(Builders.mapEntryBuilder()
                .withNodeIdentifier(NodeIdentifierWithPredicates.of(LST, ImmutableMap.<QName, Object>builder()
                    .put(A, (byte) 1)
                    .put(B, (short) 1)
                    .put(C, 1)
                    .put(D, 1L)
                    .put(E, Uint8.ONE)
                    .put(F, Uint16.ONE)
                    .put(G, Uint32.ONE)
                    .put(H, Uint64.ONE)
                    .build()))
                .withChild(ImmutableNodes.leafNode(A, (byte) 1))
                .withChild(ImmutableNodes.leafNode(B, (short) 1))
                .withChild(ImmutableNodes.leafNode(C, 1))
                .withChild(ImmutableNodes.leafNode(D, 1L))
                .withChild(ImmutableNodes.leafNode(E, Uint8.ONE))
                .withChild(ImmutableNodes.leafNode(F, Uint16.ONE))
                .withChild(ImmutableNodes.leafNode(G, Uint32.ONE))
                .withChild(ImmutableNodes.leafNode(H, Uint64.ONE))
                .build())
            .build(), outputNode);
    }
}
