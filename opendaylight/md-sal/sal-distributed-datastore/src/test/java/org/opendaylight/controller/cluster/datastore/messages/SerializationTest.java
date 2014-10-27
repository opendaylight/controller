/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.dom.store.impl.DOMImmutableDataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;

/**
 * Tests serialization of message classes.
 *
 * @author Thomas Pantelis
 */
public class SerializationTest {

    @Test
    public void testDataExists() {
        DataExists expected = new DataExists(TestModel.TEST_PATH);
        DataExists actual = (DataExists) SerializationUtils.clone(expected);
        assertEquals("getPath", expected.getPath(), actual.getPath());
    }

    @Test
    public void testDataExistsReply() {
        DataExistsReply expected = new DataExistsReply(true);
        DataExistsReply actual = (DataExistsReply) SerializationUtils.clone(expected);
        assertEquals("exists", expected.exists(), actual.exists());
    }

    @Test
    public void testDeleteData() {
        DeleteData expected = new DeleteData(TestModel.TEST_PATH);
        DeleteData actual = (DeleteData) SerializationUtils.clone(expected);
        assertEquals("getPath", expected.getPath(), actual.getPath());
    }

    @Test
    public void testDeleteDataReply() {
        SerializationUtils.clone(new DeleteDataReply());
    }

    @Test
    public void testMergeData() {
        MergeData expected = new MergeData(TestModel.TEST_PATH,
                ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                        withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build());

        MergeData actual = (MergeData) SerializationUtils.clone(expected);
        assertEquals("getPath", expected.getPath(), actual.getPath());
        assertEquals("getData", expected.getData(), actual.getData());
    }

    @Test
    public void testMergeDataReply() {
        SerializationUtils.clone(new MergeDataReply());
    }

    @Test
    public void testReadData() {
        ReadData expected = new ReadData(TestModel.TEST_PATH);
        ReadData actual = (ReadData) SerializationUtils.clone(expected);
        assertEquals("getPath", expected.getPath(), actual.getPath());
    }

    @Test
    public void testReadDataReply() {
        ReadDataReply expected = new ReadDataReply(
                ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                        withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build());

        ReadDataReply actual = (ReadDataReply) SerializationUtils.clone(expected);
        assertEquals("getNormalizedNode", expected.getNormalizedNode(), actual.getNormalizedNode());

        expected = new ReadDataReply(null);
        actual = (ReadDataReply) SerializationUtils.clone(expected);
        assertNull("Expected null", actual.getNormalizedNode());
    }

    @Test
    public void testReadyTransaction() {
        SerializationUtils.clone(new ReadyTransaction());
    }

    @Test
    public void testReadyTransactionReply() {
        ReadyTransactionReply expected = new ReadyTransactionReply("akka.tcp://system@127.0.0.1:2550/");
        ReadyTransactionReply actual = (ReadyTransactionReply) SerializationUtils.clone(expected);
        assertEquals("getCohortPath", expected.getCohortPath(), actual.getCohortPath());
    }

    @Test
    public void testWriteData() {
        WriteData expected = new WriteData(TestModel.TEST_PATH,
                ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                        withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build());

        WriteData actual = (WriteData) SerializationUtils.clone(expected);
        assertEquals("getPath", expected.getPath(), actual.getPath());
        assertEquals("getData", expected.getData(), actual.getData());
    }

    @Test
    public void testWriteDataReply() {
        SerializationUtils.clone(new WriteDataReply());
    }

    @Test
    public void testCanCommitTransaction() {
        CanCommitTransaction expected = new CanCommitTransaction("txn-1");
        CanCommitTransaction actual = (CanCommitTransaction) SerializationUtils.clone(expected);
        assertEquals("getTransactionID", expected.getTransactionID(), actual.getTransactionID());
    }

    @Test
    public void testCanCommitTransactionReply() {
        CanCommitTransactionReply expected = new CanCommitTransactionReply(true);
        CanCommitTransactionReply actual = (CanCommitTransactionReply) SerializationUtils.clone(expected);
        assertEquals("getCanCommit", expected.getCanCommit(), actual.getCanCommit());
    }

    @Test
    public void testCommitTransaction() {
        CommitTransaction expected = new CommitTransaction("txn-1");
        CommitTransaction actual = (CommitTransaction) SerializationUtils.clone(expected);
        assertEquals("getTransactionID", expected.getTransactionID(), actual.getTransactionID());
    }

    @Test
    public void testCommitTransactionReply() {
        SerializationUtils.clone(new CommitTransactionReply());
    }

    @Test
    public void testAbortTransaction() {
        AbortTransaction expected = new AbortTransaction("txn-1");
        AbortTransaction actual = (AbortTransaction) SerializationUtils.clone(expected);
        assertEquals("getTransactionID", expected.getTransactionID(), actual.getTransactionID());
    }

    @Test
    public void testAbortTransactionReply() {
        SerializationUtils.clone(new AbortTransactionReply());
    }

    @Test
    public void testCreateTransaction() {
        CreateTransaction expected = new CreateTransaction("txn-1", 1, "tx-chain");
        CreateTransaction actual = (CreateTransaction) SerializationUtils.clone(expected);
        assertEquals("getTransactionId", expected.getTransactionId(), actual.getTransactionId());
        assertEquals("getTransactionChainId", expected.getTransactionChainId(), actual.getTransactionChainId());
        assertEquals("getTransactionType", expected.getTransactionType(), actual.getTransactionType());
    }

    @Test
    public void testCreateTransactionReply() {
        CreateTransactionReply expected = new CreateTransactionReply("txn-path", "tx-1");
        CreateTransactionReply actual = (CreateTransactionReply) SerializationUtils.clone(expected);
        assertEquals("getTransactionId", expected.getTransactionId(), actual.getTransactionId());
        assertEquals("getTransactionPath", expected.getTransactionPath(), actual.getTransactionPath());
    }

    @Test
    public void testCloseTransactionChain() {
        CloseTransactionChain expected = new CloseTransactionChain("tx-chain");
        CloseTransactionChain actual = (CloseTransactionChain) SerializationUtils.clone(expected);
        assertEquals("getTransactionChainId", expected.getTransactionChainId(), actual.getTransactionChainId());
    }

    @Test
    public void testCloseTransactionChainReply() {
        SerializationUtils.clone(new CloseTransactionChainReply());
    }

    @Test
    public void testCloseTransaction() {
        SerializationUtils.clone(new CloseTransaction());
    }

    @Test
    public void testCloseTransactionReply() {
        SerializationUtils.clone(new CloseTransactionReply());
    }

    @Test
    public void testFindPrimary() {
        FindPrimary expected = new FindPrimary("shard1", true);
        FindPrimary actual = (FindPrimary) SerializationUtils.clone(expected);
        assertEquals("getShardName", expected.getShardName(), actual.getShardName());
        assertEquals("isWaitUntilInitialized", true, actual.isWaitUntilInitialized());
    }

    @Test
    public void testPrimaryFound() {
        PrimaryFound expected = new PrimaryFound("path");
        PrimaryFound actual = (PrimaryFound) SerializationUtils.clone(expected);
        assertEquals("getShardName", expected.getPrimaryPath(), actual.getPrimaryPath());
    }

    @Test
    public void testPrimaryNotFound() {
        PrimaryNotFound expected = new PrimaryNotFound("shard");
        PrimaryNotFound actual = (PrimaryNotFound) SerializationUtils.clone(expected);
        assertEquals("getShardName", expected.getShardName(), actual.getShardName());
    }

    @Test
    public void testCloseDataChangeListenerRegistration() {
        SerializationUtils.clone(new CloseDataChangeListenerRegistration());
    }

    @Test
    public void testCloseDataChangeListenerRegistrationReply() {
        SerializationUtils.clone(new CloseDataChangeListenerRegistrationReply());
    }

    @Test
    public void testRegisterChangeListener() {
        RegisterChangeListener expected = new RegisterChangeListener(TestModel.TEST_PATH, "path",
                AsyncDataBroker.DataChangeScope.SUBTREE);
        RegisterChangeListener actual = (RegisterChangeListener) SerializationUtils.clone(expected);
        assertEquals("getDataChangeListenerPath", expected.getDataChangeListenerPath(),
                actual.getDataChangeListenerPath());
        assertEquals("getPath", expected.getPath(), actual.getPath());
        assertEquals("getScope", expected.getScope(), actual.getScope());
    }

    @Test
    public void testRegisterChangeListenerReply() {
        RegisterChangeListenerReply expected = new RegisterChangeListenerReply("path");
        RegisterChangeListenerReply actual = (RegisterChangeListenerReply) SerializationUtils.clone(expected);
        assertEquals("getListenerRegistrationPath", expected.getListenerRegistrationPath(),
                actual.getListenerRegistrationPath());
    }

    @Test
    public void testDataChanged() {
        DOMImmutableDataChangeEvent change = DOMImmutableDataChangeEvent.builder(DataChangeScope.SUBTREE).
                addCreated(TestModel.TEST_PATH, ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                        withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build()).
                addUpdated(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME),
                        ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                            new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                            withChild(ImmutableNodes.leafNode(TestModel.NAME_QNAME, "bar")).build()).
                addRemoved(TestModel.OUTER_LIST_PATH,
                       ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build()).
                setBefore(ImmutableNodes.containerNode(TestModel.TEST_QNAME)).
                setAfter(ImmutableContainerNodeBuilder.create().withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME)).
                        withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).
                        withChild(ImmutableNodes.leafNode(TestModel.NAME_QNAME, "bar")).build()).build();

        DataChanged expected = new DataChanged(change);
        DataChanged actual = (DataChanged) SerializationUtils.clone(expected);
        //DataChanged actual = DataChanged.fromSerialize(null, expected.toSerializable(), null);

        assertEquals("getCreatedData", change.getCreatedData(), actual.getChange().getCreatedData());
        assertEquals("getOriginalData", change.getOriginalData(), actual.getChange().getOriginalData());
        assertEquals("getOriginalSubtree", change.getOriginalSubtree(), actual.getChange().getOriginalSubtree());
        assertEquals("getRemovedPaths", change.getRemovedPaths(), actual.getChange().getRemovedPaths());
        assertEquals("getUpdatedData", change.getUpdatedData(), actual.getChange().getUpdatedData());
        assertEquals("getUpdatedSubtree", change.getUpdatedSubtree(), actual.getChange().getUpdatedSubtree());
    }

    @Test
    public void testDataChangedReply() {
        SerializationUtils.clone(new DataChangedReply());
    }
}
