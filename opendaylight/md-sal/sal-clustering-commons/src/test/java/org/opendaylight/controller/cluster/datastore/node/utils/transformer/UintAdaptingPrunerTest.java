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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

public class UintAdaptingPrunerTest {
    private static final QName CONT = QName.create("urn:odl-ctlr1923", "cont");
    private static final QName LST = QName.create(CONT, "lst");
    private static final QName LFLST8 = QName.create(CONT, "lf-lst8");
    private static final QName LFLST16 = QName.create(CONT, "lf-lst16");
    private static final QName LFLST32 = QName.create(CONT, "lf-lst32");
    private static final QName LFLST64 = QName.create(CONT, "lf-lst64");
    private static final QName A = QName.create(LST, "a");
    private static final QName B = QName.create(LST, "b");
    private static final QName C = QName.create(LST, "c");
    private static final QName D = QName.create(LST, "d");
    private static final QName E = QName.create(LST, "e");
    private static final QName F = QName.create(LST, "f");
    private static final QName G = QName.create(LST, "g");
    private static final QName H = QName.create(LST, "h");

    private static EffectiveModelContext CONTEXT;

    @BeforeClass
    public static void beforeClass() {
        CONTEXT = YangParserTestUtils.parseYangResource("/odl-ctlr1923.yang");
    }

    @Test
    public void testListTranslation() throws IOException {
        assertEquals(Builders.mapBuilder()
            .withNodeIdentifier(new NodeIdentifier(LST))
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
            .build(),
            prune(Builders.mapBuilder()
                .withNodeIdentifier(new NodeIdentifier(LST))
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
                .build()));
    }

    @Test
    public void testContainerTranslation() throws IOException {
        assertEquals(Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(CONT))
            .withChild(ImmutableNodes.leafNode(A, (byte) 1))
            .withChild(ImmutableNodes.leafNode(B, (short) 1))
            .withChild(ImmutableNodes.leafNode(C, 1))
            .withChild(ImmutableNodes.leafNode(D, 1L))
            .withChild(ImmutableNodes.leafNode(E, Uint8.ONE))
            .withChild(ImmutableNodes.leafNode(F, Uint16.ONE))
            .withChild(ImmutableNodes.leafNode(G, Uint32.ONE))
            .withChild(ImmutableNodes.leafNode(H, Uint64.ONE))
            .build(),
            prune(Builders.containerBuilder()
                .withNodeIdentifier(new NodeIdentifier(CONT))
                .withChild(ImmutableNodes.leafNode(A, (byte) 1))
                .withChild(ImmutableNodes.leafNode(B, (short) 1))
                .withChild(ImmutableNodes.leafNode(C, 1))
                .withChild(ImmutableNodes.leafNode(D, 1L))
                .withChild(ImmutableNodes.leafNode(E, (short) 1))
                .withChild(ImmutableNodes.leafNode(F, 1))
                .withChild(ImmutableNodes.leafNode(G, 1L))
                .withChild(ImmutableNodes.leafNode(H, BigInteger.ONE))
                .build()));
    }

    @Test
    public void testLeafList8() throws IOException {
        assertEquals(Builders.leafSetBuilder()
            .withNodeIdentifier(new NodeIdentifier(LFLST8))
            .withChild(Builders.leafSetEntryBuilder()
                .withNodeIdentifier(new NodeWithValue<>(LFLST8, Uint8.ONE))
                .withValue(Uint8.ONE)
                .build())
            .build(),
            prune(Builders.leafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(LFLST8))
                .withChild(Builders.leafSetEntryBuilder()
                    .withNodeIdentifier(new NodeWithValue<>(LFLST8, (short) 1))
                    .withValue((short) 1)
                    .build())
                .build()));
    }

    @Test
    public void testLeafList16() throws IOException {
        assertEquals(Builders.leafSetBuilder()
            .withNodeIdentifier(new NodeIdentifier(LFLST16))
            .withChild(Builders.leafSetEntryBuilder()
                .withNodeIdentifier(new NodeWithValue<>(LFLST16, Uint16.ONE))
                .withValue(Uint16.ONE)
                .build())
            .build(),
            prune(Builders.leafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(LFLST16))
                .withChild(Builders.leafSetEntryBuilder()
                    .withNodeIdentifier(new NodeWithValue<>(LFLST16,  1))
                    .withValue(1)
                    .build())
                .build()));
    }

    @Test
    public void testLeafList32() throws IOException {
        assertEquals(Builders.leafSetBuilder()
            .withNodeIdentifier(new NodeIdentifier(LFLST32))
            .withChild(Builders.leafSetEntryBuilder()
                .withNodeIdentifier(new NodeWithValue<>(LFLST32, Uint32.ONE))
                .withValue(Uint32.ONE)
                .build())
            .build(),
            prune(Builders.leafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(LFLST32))
                .withChild(Builders.leafSetEntryBuilder()
                    .withNodeIdentifier(new NodeWithValue<>(LFLST32, 1L))
                    .withValue(1L)
                    .build())
                .build()));
    }

    @Test
    public void testLeafList64() throws IOException {
        assertEquals(Builders.leafSetBuilder()
            .withNodeIdentifier(new NodeIdentifier(LFLST64))
            .withChild(Builders.leafSetEntryBuilder()
                .withNodeIdentifier(new NodeWithValue<>(LFLST64, Uint64.ONE))
                .withValue(Uint64.ONE)
                .build())
            .build(),
            prune(Builders.leafSetBuilder()
                .withNodeIdentifier(new NodeIdentifier(LFLST64))
                .withChild(Builders.leafSetEntryBuilder()
                    .withNodeIdentifier(new NodeWithValue<>(LFLST64, BigInteger.ONE))
                    .withValue(BigInteger.ONE)
                    .build())
                .build()));
    }

    private static NormalizedNode<?, ?> prune(final NormalizedNode<?, ?> node) throws IOException {
        final ReusableNormalizedNodePruner pruner = ReusableNormalizedNodePruner.forSchemaContext(CONTEXT)
                .withUintAdaption();
        pruner.initializeForPath(YangInstanceIdentifier.create(node.getIdentifier()));

        try (NormalizedNodeWriter writer = NormalizedNodeWriter.forStreamWriter(pruner)) {
            writer.write(node);
        }
        pruner.close();
        return pruner.getResult().get();
    }
}
