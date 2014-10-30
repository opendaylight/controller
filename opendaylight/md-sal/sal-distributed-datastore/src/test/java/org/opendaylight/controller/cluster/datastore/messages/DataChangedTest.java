/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static org.junit.Assert.assertEquals;
import java.io.Serializable;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 * Unit tests for DataChanged.
 *
 * @author Thomas Pantelis
 */
public class DataChangedTest {

    @Test
    public void testSerialization() {
        DOMImmutableDataChangeEvent change = DOMImmutableDataChangeEvent.builder(DataChangeScope.SUBTREE).
                addCreated(TestModel.TEST_PATH, ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                        withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build()).
                addUpdated(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME),
                        ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                            new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                            withChild(ImmutableNodes.leafNode(TestModel.NAME_QNAME, "bar")).build())
.
                addRemoved(TestModel.OUTER_LIST_PATH,
                       ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build()).
                setBefore(ImmutableNodes.containerNode(TestModel.TEST_QNAME)).
                setAfter(ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                        withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).
                        withChild(ImmutableNodes.leafNode(TestModel.NAME_QNAME, "bar")).build()).build();

        DataChanged expected = new DataChanged(change);

        Object serialized = expected.toSerializable();
        assertEquals("Serialized type", ExternalizableDataChanged.class, serialized.getClass());
        assertEquals("Version", DataStoreVersions.CURRENT_VERSION,
                ((ExternalizableDataChanged)serialized).getVersion());

        Object clone = SerializationUtils.clone((Serializable) serialized);
        assertEquals("Version", DataStoreVersions.CURRENT_VERSION,
                ((ExternalizableDataChanged)clone).getVersion());

        DataChanged actual = DataChanged.fromSerializable(serialized);

        assertEquals("getCreatedData", change.getCreatedData(), actual.getChange().getCreatedData());
        assertEquals("getOriginalData", change.getOriginalData(), actual.getChange().getOriginalData());
        assertEquals("getOriginalSubtree", change.getOriginalSubtree(), actual.getChange().getOriginalSubtree());
        assertEquals("getRemovedPaths", change.getRemovedPaths(), actual.getChange().getRemovedPaths());
        assertEquals("getUpdatedData", change.getUpdatedData(), actual.getChange().getUpdatedData());
        assertEquals("getUpdatedSubtree", change.getUpdatedSubtree(), actual.getChange().getUpdatedSubtree());
    }
}
