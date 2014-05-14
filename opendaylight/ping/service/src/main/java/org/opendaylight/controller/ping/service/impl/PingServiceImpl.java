package org.opendaylight.controller.ping.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.ping.service.api.PingServiceAPI;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.PingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.SendEchoInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ping.rev130911.SendEchoOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingServiceImpl extends AbstractBindingAwareConsumer implements
        BundleActivator, BindingAwareConsumer, PingServiceAPI {
    private static Logger log = LoggerFactory.getLogger(PingServiceImpl.class);

    private PingService pingService;
    private ConsumerContext session;
    private Map<Ipv4Address, Future<RpcResult<SendEchoOutput>>> asyncPingEntryMap;

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        this.session = session;
        this.pingService = this.session.getRpcService(PingService.class);
    }

    @Override
    protected void startImpl(BundleContext context) {
        asyncPingEntryMap = new HashMap<>();
        context.registerService(PingServiceAPI.class, this, null);
    }

    @Override
    protected void stopImpl(BundleContext context) {
        asyncPingEntryMap.clear();
    }

    @Override
    public PingResult pingDestinationSync(String address) {
        return _pingDestinationSync(address);
    }

    @Override
    public PingResult pingDestinationAsync(String address) {
        return _pingDestinationAsync(address);
    }

    @Override
    public void pingAsyncClear(String address) {
        _pingAsyncClear(address);
    }

    private PingResult _pingDestinationSync(String address) {
        if (pingService == null) { return PingResult.Error; } // No pingService service found.

        try {
            Ipv4Address destination = new Ipv4Address(address);
            SendEchoInputBuilder ib = new SendEchoInputBuilder();
            ib.setDestination(destination);
            return mapResult(pingService.sendEcho(ib.build()).get().getResult().getEchoResult());
        } catch (InterruptedException ie) {
            log.warn("InterruptedException received by pingDestinationSync: {} from: {}",
                    address, ie.getMessage());
        } catch (ExecutionException ee) {
            log.warn("ExecutionException received by pingDestinationSync: {} from: {}",
                    address, ee.getMessage());
        }
        return PingResult.Error;
    }

    private synchronized PingResult _pingDestinationAsync(String address) {
        if (pingService == null) { return PingResult.Error; } // No pingService service found.

        /** Look for destination in asyncPingEntryMap. If none is found, create a new entry
         * and return "in progress". This will happen on the very first time async is requested.
         *
         * NOTE: In a real scenario, you would want to consider a cache which automatically drops
         * entries, so implementation does not need to rely on users calling async clear to remove
         * the entries from the map. An example for doing such would be Google's caches or a
         * weakhash map; which we deliberately chose not to use here for sake of simplicity.
         */
        final Ipv4Address destination = new Ipv4Address(address);
        final Future<RpcResult<SendEchoOutput>> rpcResultFuture = asyncPingEntryMap.get(destination);
        if (rpcResultFuture == null) {
            SendEchoInputBuilder ib = new SendEchoInputBuilder();
            ib.setDestination(destination);
            asyncPingEntryMap.put(destination, pingService.sendEcho(ib.build()));
            log.info("Starting pingDestinationAsync: {}", address);
            return PingResult.InProgress;
        }

        /** Pending result may not be ready to be consumed. In such case, use "in progress".
         */
        if (!rpcResultFuture.isDone()) {
            log.info("pingDestinationAsync: {} get result is not ready (ie. inProgress)", address);
            return PingResult.InProgress;
        }

        /** If we made it this far, we know that rpcResultFuture is ready for consumption.
         */
        try {
            PingResult pingResult = mapResult(rpcResultFuture.get().getResult().getEchoResult());
            log.info("pingDestinationAsync: {} get result is {}", address, pingResult);
            return pingResult;
        } catch (InterruptedException ie) {
            log.warn("InterruptedException received by pingDestinationAsync: {} from: {}",
                    address, ie.getMessage());
        } catch (ExecutionException ee) {
            log.warn("ExecutionException received by pingDestinationAsync: {} from: {}",
                    address, ee.getMessage());
        }
        return PingResult.Error;
    }

    private synchronized void _pingAsyncClear(String address) {
        asyncPingEntryMap.remove(new Ipv4Address(address));
        log.info("Removing pingDestinationAsync: {}", address);
    }

    private static PingResult mapResult(SendEchoOutput.EchoResult echoResult) {
        // Translate echoResult to pingResult
        switch (echoResult) {
            case Reachable:
                return PingResult.GotResponse;
            case Unreachable:
                return PingResult.NoResponse;
            case Error:
            default:
                break;
        }
        return PingResult.Error;
    }
}
