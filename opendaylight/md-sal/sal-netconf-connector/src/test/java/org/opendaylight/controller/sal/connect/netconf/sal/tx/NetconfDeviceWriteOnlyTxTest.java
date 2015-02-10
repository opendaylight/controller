package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.DISCARD_CHANGES_RPC_CONTENT;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_CANDIDATE_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfBaseOps;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class NetconfDeviceWriteOnlyTxTest {

    private final RemoteDeviceId id = new RemoteDeviceId("test-mount");

    @Mock
    private RpcImplementation rpc;
    @Mock
    private DataNormalizer normalizer;
    private YangInstanceIdentifier yangIId;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        ListenableFuture<RpcResult<CompositeNode>> successFuture = Futures.immediateFuture(RpcResultBuilder.<CompositeNode>success().build());

        doReturn(successFuture)
                .doReturn(Futures.<RpcResult<CompositeNode>>immediateFailedFuture(new IllegalStateException("Failed tx")))
                .doReturn(successFuture)
                .when(rpc).invokeRpc(any(QName.class), any(CompositeNode.class));

        yangIId = YangInstanceIdentifier.builder().node(QName.create("namespace", "2012-12-12", "name")).build();
        doReturn(yangIId).when(normalizer).toLegacy(yangIId);
    }

    @Test
    public void testDiscardChanges() {
        final WriteCandidateTx tx = new WriteCandidateTx(id, new NetconfBaseOps(rpc), normalizer,
                NetconfSessionPreferences.fromStrings(Collections.<String>emptySet()));
        final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();
        try {
            submitFuture.checkedGet();
        } catch (final TransactionCommitFailedException e) {
            // verify discard changes was sent
            final InOrder inOrder = inOrder(rpc);
            inOrder.verify(rpc).invokeRpc(NetconfMessageTransformUtil.NETCONF_LOCK_QNAME, NetconfBaseOps.getLockContent(NETCONF_CANDIDATE_QNAME));
            inOrder.verify(rpc).invokeRpc(NetconfMessageTransformUtil.NETCONF_COMMIT_QNAME, NetconfMessageTransformUtil.COMMIT_RPC_CONTENT);
            inOrder.verify(rpc).invokeRpc(NetconfMessageTransformUtil.NETCONF_DISCARD_CHANGES_QNAME, DISCARD_CHANGES_RPC_CONTENT);
            inOrder.verify(rpc).invokeRpc(NetconfMessageTransformUtil.NETCONF_UNLOCK_QNAME, NetconfBaseOps.getUnLockContent(NETCONF_CANDIDATE_QNAME));
            return;
        }

        fail("Submit should fail");
    }

    @Test
    public void testDiscardChangesNotSentWithoutCandidate() {
        doReturn(Futures.immediateFuture(RpcResultBuilder.<CompositeNode>success().build()))
        .doReturn(Futures.<RpcResult<CompositeNode>>immediateFailedFuture(new IllegalStateException("Failed tx")))
                .when(rpc).invokeRpc(any(QName.class), any(CompositeNode.class));

        final WriteRunningTx tx = new WriteRunningTx(id, new NetconfBaseOps(rpc), normalizer,
                NetconfSessionPreferences.fromStrings(Collections.<String>emptySet()));
        try {
            tx.delete(LogicalDatastoreType.CONFIGURATION, yangIId);
        } catch (final Exception e) {
            // verify discard changes was sent
            final InOrder inOrder = inOrder(rpc);
            inOrder.verify(rpc).invokeRpc(NetconfMessageTransformUtil.NETCONF_LOCK_QNAME, NetconfBaseOps.getLockContent(NETCONF_RUNNING_QNAME));
            inOrder.verify(rpc).invokeRpc(same(NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME), any(CompositeNode.class));
            inOrder.verify(rpc).invokeRpc(NetconfMessageTransformUtil.NETCONF_UNLOCK_QNAME, NetconfBaseOps.getUnLockContent(NETCONF_RUNNING_QNAME));
            return;
        }

        fail("Delete should fail");
    }

}
