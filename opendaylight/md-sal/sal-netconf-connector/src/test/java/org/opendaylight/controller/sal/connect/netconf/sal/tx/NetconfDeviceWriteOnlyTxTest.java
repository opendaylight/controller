package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.DISCARD_CHANGES_RPC_CONTENT;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
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

        doReturn(Futures.<RpcResult<CompositeNode>>immediateFailedFuture(new IllegalStateException("Failed tx")))
        .doReturn(Futures.immediateFuture(RpcResultBuilder.<CompositeNode>success().build()))
                .when(rpc).invokeRpc(any(QName.class), any(CompositeNode.class));

        yangIId = YangInstanceIdentifier.builder().node(QName.create("namespace", "2012-12-12", "name")).build();
        doReturn(yangIId).when(normalizer).toLegacy(yangIId);
    }

    @Test
    public void testDiscardCahnges() {
        final NetconfDeviceWriteOnlyTx tx = new NetconfDeviceWriteOnlyTx(id, rpc, normalizer, true, true);
        final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();
        try {
            submitFuture.checkedGet();
        } catch (final TransactionCommitFailedException e) {
            // verify discard changes was sent
            verify(rpc).invokeRpc(NetconfMessageTransformUtil.NETCONF_DISCARD_CHANGES_QNAME, DISCARD_CHANGES_RPC_CONTENT);
            return;
        }

        fail("Submit should fail");
    }


    @Test
    public void testDiscardCahngesNotSentWithoutCandidate() {
        doReturn(Futures.immediateFuture(RpcResultBuilder.<CompositeNode>success().build()))
        .doReturn(Futures.<RpcResult<CompositeNode>>immediateFailedFuture(new IllegalStateException("Failed tx")))
                .when(rpc).invokeRpc(any(QName.class), any(CompositeNode.class));

        final NetconfDeviceWriteOnlyTx tx = new NetconfDeviceWriteOnlyTx(id, rpc, normalizer, false, true);
        tx.delete(LogicalDatastoreType.CONFIGURATION, yangIId);
        verify(rpc).invokeRpc(eq(NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME), any(CompositeNode.class));
        verifyNoMoreInteractions(rpc);
    }

}
