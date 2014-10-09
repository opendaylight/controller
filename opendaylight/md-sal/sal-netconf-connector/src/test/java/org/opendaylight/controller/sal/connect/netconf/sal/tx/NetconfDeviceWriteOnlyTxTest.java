package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Executor;
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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.DISCARD_CHANGES_RPC_CONTENT;

public class NetconfDeviceWriteOnlyTxTest {

    private final RemoteDeviceId id = new RemoteDeviceId("test-mount");

    @Mock
    private RpcImplementation rpc;
    @Mock
    private DataNormalizer normalizer;
    @Mock
    private RemoteDeviceId remoteDeviceId;
    @Mock
    private CompositeNode compositeNode;
    @Mock
    private NormalizedNode normalizedNode;
    @Mock
    private ListenableFuture listenableFuture;
    @Mock
    private RpcResult rpcResult;
    @Mock
    private Throwable cause;

    private YangInstanceIdentifier yangIId;
    private NetconfDeviceWriteOnlyTx deviceWriteOnlyTx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        yangIId = YangInstanceIdentifier.builder().node(QName.create("namespace", "2012-12-12", "name")).build();
        doReturn(compositeNode).when(normalizer).toLegacy(any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        doReturn(yangIId).when(normalizer).toLegacy(yangIId);
        List children = Lists.newArrayList();
        doReturn("remoteDev").when(remoteDeviceId).toString();
        doReturn(children).when(compositeNode).getValue();
        doReturn("normNode").when(normalizedNode).toString();
        doReturn(rpcResult).when(listenableFuture).get();
        doNothing().when(listenableFuture).addListener(any(Runnable.class), any(Executor.class));
        doReturn(listenableFuture).when(rpc).invokeRpc(any(QName.class), any(CompositeNode.class));
        deviceWriteOnlyTx = new NetconfDeviceWriteOnlyTx(remoteDeviceId, rpc, normalizer, true, true);
    }

    @Test
    public void testDiscardChanges() {
        doReturn(Futures.<RpcResult<CompositeNode>>immediateFailedFuture(new IllegalStateException("Failed tx")))
        .doReturn(Futures.immediateFuture(RpcResultBuilder.<CompositeNode>success().build()))
                .when(rpc).invokeRpc(any(QName.class), any(CompositeNode.class));


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
    public void testDiscardChangesNotSentWithoutCandidate() {
        doReturn(Futures.immediateFuture(RpcResultBuilder.<CompositeNode>success().build()))
        .doReturn(Futures.<RpcResult<CompositeNode>>immediateFailedFuture(new IllegalStateException("Failed tx")))
                .when(rpc).invokeRpc(any(QName.class), any(CompositeNode.class));

        final NetconfDeviceWriteOnlyTx tx = new NetconfDeviceWriteOnlyTx(id, rpc, normalizer, false, true);
        tx.delete(LogicalDatastoreType.CONFIGURATION, yangIId);
        verify(rpc).invokeRpc(eq(NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME), any(CompositeNode.class));
        verifyNoMoreInteractions(rpc);
    }

    @Test(expected = RuntimeException.class)
    public void testPut() throws Exception {
        deviceWriteOnlyTx.put(LogicalDatastoreType.CONFIGURATION, yangIId, null);
    }

    @Test
    public void testMerge() throws Exception {
        doReturn(true).when(rpcResult).isSuccessful();
        deviceWriteOnlyTx.merge(LogicalDatastoreType.CONFIGURATION, yangIId, normalizedNode);
        verify(normalizer, times(1)).toLegacy(any(YangInstanceIdentifier.class));
    }

    @Test(expected = RuntimeException.class)
    public void testMerge2() throws Exception {
        doReturn(true).when(rpcResult).isSuccessful();
        doReturn(Sets.newHashSet()).when(rpcResult).getErrors();
        doReturn(false).when(rpcResult).isSuccessful();
        deviceWriteOnlyTx.merge(LogicalDatastoreType.CONFIGURATION, yangIId, normalizedNode);
    }

    @Test(expected = RuntimeException.class)
    public void testDelete() throws Exception {
        doReturn(false).when(rpcResult).isSuccessful();
        deviceWriteOnlyTx.delete(LogicalDatastoreType.CONFIGURATION, yangIId);
    }
}
