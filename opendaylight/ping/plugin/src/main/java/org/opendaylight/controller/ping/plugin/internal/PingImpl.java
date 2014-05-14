package org.opendaylight.controller.ping.plugin.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.PingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.SendEchoInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.SendEchoOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.SendEchoOutput.EchoResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.SendEchoOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.util.concurrent.Futures;

public class PingImpl implements PingService {

    private EchoResult pingHost(InetAddress destination) throws IOException {
        if (destination.isReachable(5000)) {
            return EchoResult.Reachable;
        } else {
            return EchoResult.Unreachable;
        }
    }

    @Override
    public Future<RpcResult<SendEchoOutput>> sendEcho(SendEchoInput destination) {
        try {
            InetAddress dst = InetAddress.getByName(destination
                    .getDestination().getValue());
            EchoResult result = this.pingHost(dst);

            /* Build the result and return it. */
            SendEchoOutputBuilder ob = new SendEchoOutputBuilder();
            ob.setEchoResult(result);
            RpcResult<SendEchoOutput> rpcResult =
                    Rpcs.<SendEchoOutput> getRpcResult(true, ob.build(),
                            Collections.<RpcError> emptySet());

            return Futures.immediateFuture(rpcResult);
        } catch (Exception e) {

            /* Return error result. */
            SendEchoOutputBuilder ob = new SendEchoOutputBuilder();
            ob.setEchoResult(EchoResult.Error);
            RpcResult<SendEchoOutput> rpcResult =
                    Rpcs.<SendEchoOutput> getRpcResult(true, ob.build(),
                            Collections.<RpcError> emptySet());
            return Futures.immediateFuture(rpcResult);
        }
    }

}
