package org.opendaylight.controller.sal.restconf.rpc.impl;

import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.Response.Status;

import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Provides an implementation which invokes rpc methods via a mounted yang data model.
 * @author Devin Avery
 *
 */
public class MountPointRpcExecutor extends AbstractRpcExecutor {
    private final MountInstance mountPoint;

    public MountPointRpcExecutor(RpcDefinition rpcDef, MountInstance mountPoint) {
        super( rpcDef );
        this.mountPoint = mountPoint;
        if( mountPoint == null )
        {
            throw new NullPointerException( "MountInstance is null" );
        }
    }

    @Override
    public RpcResult<CompositeNode> invokeRpc( CompositeNode rpcRequest ) throws ResponseException {
        ListenableFuture<RpcResult<CompositeNode>> rpcFuture =
                mountPoint.rpc( getRpcDefinition().getQName(), rpcRequest);
        try {
            return rpcFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ResponseException(Status.INTERNAL_SERVER_ERROR,
                                                            "Exception while waiting on future.");
        }
    }
}