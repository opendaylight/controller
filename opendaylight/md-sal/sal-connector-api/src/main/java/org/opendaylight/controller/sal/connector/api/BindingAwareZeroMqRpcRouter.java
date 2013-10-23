package org.opendaylight.controller.sal.connector.api;

import java.util.concurrent.Future;

public class BindingAwareZeroMqRpcRouter implements BindingAwareRpcRouter {

    BindingAwareRpcRouter mdSalRouter;
    
    public BindingAwareRpcRouter getMdSalRouter() {
        return mdSalRouter;
    }


    public void setMdSalRouter(BindingAwareRpcRouter mdSalRouter) {
        this.mdSalRouter = mdSalRouter;
    }


    @Override
    public Future<RpcReply<byte[]>> sendRpc(RpcRequest<String, String, String, byte[]> input) {
        // Write message down to the wire
        return null;
    }
    
    // Receiver part - invoked when request is received and deserialized
    private Future<RpcReply<byte[]>> receivedRequest(RpcRequest<String, String, String, byte[]> input) {
        
        return mdSalRouter.sendRpc(input);
    }

}
