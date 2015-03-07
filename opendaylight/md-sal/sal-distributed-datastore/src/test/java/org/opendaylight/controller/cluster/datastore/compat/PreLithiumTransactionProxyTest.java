/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType.READ_WRITE;
import static org.opendaylight.controller.cluster.datastore.TransactionProxy.TransactionType.WRITE_ONLY;
import akka.actor.ActorRef;
import akka.dispatch.Futures;
import com.google.common.base.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.AbstractTransactionProxyTest;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.ThreePhaseCommitCohortProxy;
import org.opendaylight.controller.cluster.datastore.TransactionProxy;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.DeleteDataReply;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.MergeDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.messages.WriteDataReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * Unit tests for backwards compatibility with pre-Lithium versions.
 *
 * @author Thomas Pantelis
 */
public class PreLithiumTransactionProxyTest extends AbstractTransactionProxyTest {

    private WriteData eqLegacyWriteData(final NormalizedNode<?, ?> nodeToWrite) {
        ArgumentMatcher<WriteData> matcher = new ArgumentMatcher<WriteData>() {
            @Override
            public boolean matches(Object argument) {
                if(ShardTransactionMessages.WriteData.class.equals(argument.getClass())) {
                    WriteData obj = WriteData.fromSerializable(argument);
                    return obj.getPath().equals(TestModel.TEST_PATH) && obj.getData().equals(nodeToWrite);
                }

                return false;
            }
        };

        return argThat(matcher);
    }

    private MergeData eqLegacyMergeData(final NormalizedNode<?, ?> nodeToWrite) {
        ArgumentMatcher<MergeData> matcher = new ArgumentMatcher<MergeData>() {
            @Override
            public boolean matches(Object argument) {
                if(ShardTransactionMessages.MergeData.class.equals(argument.getClass())) {
                    MergeData obj = MergeData.fromSerializable(argument);
                    return obj.getPath().equals(TestModel.TEST_PATH) && obj.getData().equals(nodeToWrite);
                }

                return false;
            }
        };

        return argThat(matcher);
    }

    private DeleteData eqLegacyDeleteData(final YangInstanceIdentifier expPath) {
        ArgumentMatcher<DeleteData> matcher = new ArgumentMatcher<DeleteData>() {
            @Override
            public boolean matches(Object argument) {
                return ShardTransactionMessages.DeleteData.class.equals(argument.getClass()) &&
                       DeleteData.fromSerializable(argument).getPath().equals(expPath);
            }
        };

        return argThat(matcher);
    }

    private ActorRef testCompatibilityWithHeliumVersion(short version) throws Exception {
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), READ_WRITE, version);

        NormalizedNode<?, ?> testNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(readSerializedDataReply(testNode, version)).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), eqSerializedReadData(TestModel.TEST_PATH));

        doReturn(Futures.successful(new WriteDataReply().toSerializable(version))).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqLegacyWriteData(testNode));

        doReturn(Futures.successful(new MergeDataReply().toSerializable(version))).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqLegacyMergeData(testNode));

        doReturn(Futures.successful(new DeleteDataReply().toSerializable(version))).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqLegacyDeleteData(TestModel.TEST_PATH));

        doReturn(readySerializedTxReply(actorRef.path().toString())).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(ReadyTransaction.SERIALIZABLE_CLASS));

        doReturn(actorRef.path().toString()).when(mockActorContext).resolvePath(eq(actorRef.path().toString()),
                eq(actorRef.path().toString()));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, READ_WRITE);

        Optional<NormalizedNode<?, ?>> readOptional = transactionProxy.read(TestModel.TEST_PATH).
                get(5, TimeUnit.SECONDS);

        assertEquals("NormalizedNode isPresent", true, readOptional.isPresent());
        assertEquals("Response NormalizedNode", testNode, readOptional.get());

        transactionProxy.write(TestModel.TEST_PATH, testNode);

        transactionProxy.merge(TestModel.TEST_PATH, testNode);

        transactionProxy.delete(TestModel.TEST_PATH);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyCohortFutures(proxy, getSystem().actorSelection(actorRef.path()));

        return actorRef;
    }

    @Test
    public void testCompatibilityWithBaseHeliumVersion() throws Exception {
        ActorRef actorRef = testCompatibilityWithHeliumVersion(DataStoreVersions.BASE_HELIUM_VERSION);

        verify(mockActorContext).resolvePath(eq(actorRef.path().toString()),
                eq(actorRef.path().toString()));
    }

    @Test
    public void testCompatibilityWithHeliumR1Version() throws Exception {
        ActorRef actorRef = testCompatibilityWithHeliumVersion(DataStoreVersions.HELIUM_1_VERSION);

        verify(mockActorContext, Mockito.never()).resolvePath(eq(actorRef.path().toString()),
                eq(actorRef.path().toString()));
    }

    @Test
    @Ignore
    // FIXME: disabled until we can get the primary shard version from the ShardManager as we now skip
    // creating transaction actors for write-only Tx's.
    public void testWriteOnlyCompatibilityWithHeliumR2Version() throws Exception {
        short version = DataStoreVersions.HELIUM_2_VERSION;
        ActorRef actorRef = setupActorContextWithInitialCreateTransaction(getSystem(), WRITE_ONLY, version);

        NormalizedNode<?, ?> testNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(Futures.successful(new WriteDataReply().toSerializable(version))).when(mockActorContext).
                executeOperationAsync(eq(actorSelection(actorRef)), eqLegacyWriteData(testNode));

        doReturn(readySerializedTxReply(actorRef.path().toString())).when(mockActorContext).executeOperationAsync(
                eq(actorSelection(actorRef)), isA(ReadyTransaction.SERIALIZABLE_CLASS));

        doReturn(actorRef.path().toString()).when(mockActorContext).resolvePath(eq(actorRef.path().toString()),
                eq(actorRef.path().toString()));

        TransactionProxy transactionProxy = new TransactionProxy(mockActorContext, WRITE_ONLY);

        transactionProxy.write(TestModel.TEST_PATH, testNode);

        DOMStoreThreePhaseCommitCohort ready = transactionProxy.ready();

        assertTrue(ready instanceof ThreePhaseCommitCohortProxy);

        ThreePhaseCommitCohortProxy proxy = (ThreePhaseCommitCohortProxy) ready;

        verifyCohortFutures(proxy, getSystem().actorSelection(actorRef.path()));
    }
}
