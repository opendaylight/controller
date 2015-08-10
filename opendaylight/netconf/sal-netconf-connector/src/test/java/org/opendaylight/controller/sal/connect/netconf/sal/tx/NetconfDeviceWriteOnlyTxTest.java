package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_CANDIDATE_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_FILTER_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toPath;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.sal.connect.netconf.NetconfDevice;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfBaseOps;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class NetconfDeviceWriteOnlyTxTest {

    private final RemoteDeviceId id = new RemoteDeviceId("test-mount", new InetSocketAddress(99));

    @Mock
    private DOMRpcService rpc;
    private YangInstanceIdentifier yangIId;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final CheckedFuture<DefaultDOMRpcResult, Exception> successFuture =
                Futures.immediateCheckedFuture(new DefaultDOMRpcResult(((NormalizedNode<?, ?>) null)));

        doReturn(successFuture)
                .doReturn(Futures.immediateFailedCheckedFuture(new IllegalStateException("Failed tx")))
                .doReturn(successFuture)
                .when(rpc).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));

        yangIId = YangInstanceIdentifier.builder().node(NetconfState.QNAME).build();
    }

    @Test
    public void testIgnoreNonVisibleData() {
        final WriteCandidateTx tx = new WriteCandidateTx(id, new NetconfBaseOps(rpc, mock(SchemaContext.class)),
                false, 60000L);
        final MapNode emptyList = ImmutableNodes.mapNodeBuilder(NETCONF_FILTER_QNAME).build();
        tx.merge(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(NETCONF_FILTER_QNAME)), emptyList);
        tx.put(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(NETCONF_FILTER_QNAME)), emptyList);

        verify(rpc, atMost(1)).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));
    }

    @Test
    public void testDiscardChanges() {
        final WriteCandidateTx tx = new WriteCandidateTx(id, new NetconfBaseOps(rpc, mock(SchemaContext.class)),
                false, 60000L);
        final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();
        try {
            submitFuture.checkedGet();
        } catch (final TransactionCommitFailedException e) {
            // verify discard changes was sent
            final InOrder inOrder = inOrder(rpc);
            inOrder.verify(rpc).invokeRpc(toPath(NetconfMessageTransformUtil.NETCONF_LOCK_QNAME), NetconfBaseOps.getLockContent(NETCONF_CANDIDATE_QNAME));
            inOrder.verify(rpc).invokeRpc(toPath(NetconfMessageTransformUtil.NETCONF_COMMIT_QNAME), NetconfMessageTransformUtil.COMMIT_RPC_CONTENT);
            inOrder.verify(rpc).invokeRpc(eq(toPath(NetconfMessageTransformUtil.NETCONF_DISCARD_CHANGES_QNAME)), any(NormalizedNode.class));
            inOrder.verify(rpc).invokeRpc(toPath(NetconfMessageTransformUtil.NETCONF_UNLOCK_QNAME), NetconfBaseOps.getUnLockContent(NETCONF_CANDIDATE_QNAME));
            return;
        }

        fail("Submit should fail");
    }

    @Test
    public void testFailedCommit() throws Exception {
        final CheckedFuture<DefaultDOMRpcResult, Exception> rpcErrorFuture =
                Futures.immediateCheckedFuture(new DefaultDOMRpcResult(RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION, "a", "m")));

        doReturn(Futures.immediateCheckedFuture(new DefaultDOMRpcResult(((NormalizedNode<?, ?>) null))))
        .doReturn(rpcErrorFuture).when(rpc).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));

        final WriteCandidateTx tx = new WriteCandidateTx(id, new NetconfBaseOps(rpc, mock(SchemaContext.class)),
                false, 60000L);

        final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();
        try {
            submitFuture.checkedGet();
        } catch (final TransactionCommitFailedException e) {
            return;
        }

        fail("Submit should fail");
    }

    @Test
    public void testDiscardChangesNotSentWithoutCandidate() {
        doReturn(Futures.immediateCheckedFuture(new DefaultDOMRpcResult(((NormalizedNode<?, ?>) null))))
                .doReturn(Futures.immediateFailedCheckedFuture(new IllegalStateException("Failed tx")))
                .when(rpc).invokeRpc(any(SchemaPath.class), any(NormalizedNode.class));

        final WriteRunningTx tx = new WriteRunningTx(id, new NetconfBaseOps(rpc, NetconfDevice.INIT_SCHEMA_CTX),
                false, 60000L);
        try {
            tx.delete(LogicalDatastoreType.CONFIGURATION, yangIId);
        } catch (final Exception e) {
            // verify discard changes was sent
            final InOrder inOrder = inOrder(rpc);
            inOrder.verify(rpc).invokeRpc(toPath(NetconfMessageTransformUtil.NETCONF_LOCK_QNAME), NetconfBaseOps.getLockContent(NETCONF_RUNNING_QNAME));
            inOrder.verify(rpc).invokeRpc(eq(toPath(NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME)), any(NormalizedNode.class));
            inOrder.verify(rpc).invokeRpc(toPath(NetconfMessageTransformUtil.NETCONF_UNLOCK_QNAME), NetconfBaseOps.getUnLockContent(NETCONF_RUNNING_QNAME));
            return;
        }

        fail("Delete should fail");
    }

}
