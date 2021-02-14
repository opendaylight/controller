/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

@Deprecated(forRemoval = true)
public class ClientTransactionCursorTest {

    private static final QName NODE_1 = QName.create("ns-1", "node-1");
    private static final QName NODE_2 = QName.create(NODE_1, "node-2");
    private static final QName NODE_3 = QName.create(NODE_1, "node-3");

    @Mock
    private ClientTransaction transaction;
    private ClientTransactionCursor cursor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        cursor = new ClientTransactionCursor(transaction);
    }

    @Test
    public void testEnterOneNode() {
        cursor.enter(YangInstanceIdentifier.NodeIdentifier.create(NODE_1));
        cursor.delete(YangInstanceIdentifier.NodeIdentifier.create(NODE_2));
        final YangInstanceIdentifier expected = createId(NODE_1, NODE_2);
        verify(transaction).delete(expected);
    }

    @Test
    public void testEnterNodeIterables() {
        final Iterable<YangInstanceIdentifier.PathArgument> collect = toPathArg(NODE_1, NODE_2);
        cursor.enter(collect);
        cursor.delete(YangInstanceIdentifier.NodeIdentifier.create(NODE_3));
        final YangInstanceIdentifier expected = createId(NODE_1, NODE_2, NODE_3);
        verify(transaction).delete(expected);
    }

    @Test
    public void testEnterNodeVarargs() {
        cursor.enter(YangInstanceIdentifier.NodeIdentifier.create(NODE_1),
                YangInstanceIdentifier.NodeIdentifier.create(NODE_2));
        cursor.delete(YangInstanceIdentifier.NodeIdentifier.create(NODE_3));
        final YangInstanceIdentifier expected = createId(NODE_1, NODE_2, NODE_3);
        verify(transaction).delete(expected);
    }

    @Test
    public void testExitOneLevel() {
        cursor.enter(toPathArg(NODE_1, NODE_2));
        cursor.exit();
        cursor.delete(YangInstanceIdentifier.NodeIdentifier.create(NODE_2));
        final YangInstanceIdentifier expected = createId(NODE_1, NODE_2);
        verify(transaction).delete(expected);
    }

    @Test
    public void testExitTwoLevels() {
        cursor.enter(toPathArg(NODE_1, NODE_2, NODE_3));
        cursor.exit(2);
        cursor.delete(YangInstanceIdentifier.NodeIdentifier.create(NODE_2));
        final YangInstanceIdentifier expected = createId(NODE_1, NODE_2);
        verify(transaction).delete(expected);
    }

    @Test
    public void testClose() {
        cursor.close();
        verify(transaction).closeCursor(cursor);
    }

    @Test
    public void testDelete() {
        cursor.delete(YangInstanceIdentifier.NodeIdentifier.create(NODE_1));
        final YangInstanceIdentifier expected = createId(NODE_1);
        verify(transaction).delete(expected);
    }

    @Test
    public void testMerge() {
        final YangInstanceIdentifier.NodeIdentifier path = YangInstanceIdentifier.NodeIdentifier.create(NODE_1);
        final ContainerNode data = createData(path.getNodeType());
        cursor.merge(path, data);
        final YangInstanceIdentifier expected = createId(NODE_1);
        verify(transaction).merge(expected, data);
    }

    @Test
    public void testWrite() {
        final YangInstanceIdentifier.NodeIdentifier path = YangInstanceIdentifier.NodeIdentifier.create(NODE_1);
        final ContainerNode data = createData(path.getNodeType());
        cursor.write(path, data);
        final YangInstanceIdentifier expected = createId(NODE_1);
        verify(transaction).write(expected, data);
    }

    private static Iterable<YangInstanceIdentifier.PathArgument> toPathArg(final QName... pathArguments) {
        return Arrays.stream(pathArguments)
                .map(YangInstanceIdentifier.NodeIdentifier::create)
                .collect(Collectors.toList());
    }

    private static YangInstanceIdentifier createId(final QName... pathArguments) {
        return YangInstanceIdentifier.create(toPathArg(pathArguments));
    }

    private static ContainerNode createData(final QName id) {
        return Builders.containerBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(id))
                .build();
    }

}