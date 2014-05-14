package org.opendaylight.controller.ping.plugin.internal;

import java.net.InetAddress;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.PingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.SendEchoInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.SendEchoOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.SendEchoOutput.EchoResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.SendEchoOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class PingImpl implements PingService {
    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    private Future<RpcResult<SendEchoOutput>> startPingHost(final SendEchoInput destination) {
        return pool.submit(new Callable<RpcResult<SendEchoOutput>>() {
            @Override
            public RpcResult<SendEchoOutput> call() throws Exception {
                SendEchoOutputBuilder ob = new SendEchoOutputBuilder();
                try {
                    InetAddress dst = InetAddress.getByName(destination.getDestination().getValue());
                    /* Build the result and return it. */
                    ob.setEchoResult(dst.isReachable(5000) ? EchoResult.Reachable : EchoResult.Unreachable);
                } catch (Exception e) {
                    /* Return error result. */
                    ob.setEchoResult(EchoResult.Error);
                }
                return Rpcs.<SendEchoOutput>getRpcResult(true, ob.build(), Collections.<RpcError>emptySet());
            }
        });
    }

    @Override
    public Future<RpcResult<SendEchoOutput>> sendEcho(SendEchoInput destination) {
        return this.startPingHost(destination);
    }

}
