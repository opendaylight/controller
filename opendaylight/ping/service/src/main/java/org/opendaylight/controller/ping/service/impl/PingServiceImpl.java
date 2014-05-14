package org.opendaylight.controller.ping.service.impl;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

public class PingServiceImpl extends AbstractBindingAwareConsumer implements
        BundleActivator, BindingAwareConsumer, PingServiceAPI {

    private PingService pingService;
    private ConsumerContext session;

    private class AsyncPingEntry {
        public Future<RpcResult<SendEchoOutput>> pendingResult;
        public SendEchoOutput.EchoResult lastEchoResult;
        public int countSent = 0;     // debug use
        public int countReceived = 0; // debug use

        public AsyncPingEntry(Ipv4Address destination, PingService pingService, AsyncPingEntry previousAsyncPingEntry) {
            if (previousAsyncPingEntry != null) {
                this.lastEchoResult = previousAsyncPingEntry.lastEchoResult;
                this.countSent = previousAsyncPingEntry.countSent;
                this.countReceived = previousAsyncPingEntry.countReceived;
            }

            /** Tickle the ping service to go ahead and send the echo packet.
             */
            try {
                SendEchoInputBuilder ib = new SendEchoInputBuilder();
                ib.setDestination(destination);
                this.pendingResult = pingService.sendEcho(ib.build());
                this.countSent++;
            } catch (Exception e) {
                this.pendingResult = null;
            }
        }
    }
    private ConcurrentMap<Ipv4Address, AsyncPingEntry> asyncPingEntryConcurrentMap;
    private Timer asyncPingTimer;
    private class AsyncPingTimerHandler extends TimerTask {
        /** The purpose of AsyncPingTimerHandler's life is to look for async entries that have been
         * finished with their RPC calls to the ping plugin. When such entries are found, it simply
         * restarts them, by replacing the 'data' wiht a new AsyncPingEntry instance. While invoking
         * the AsyncPingEntry's constructor, the new RPC call to the ping plugin backend is executed.
         */
        @Override
        public void run() {
            Iterator<ConcurrentMap.Entry<Ipv4Address, AsyncPingEntry>> iterator =
                    asyncPingEntryConcurrentMap.entrySet().iterator();
            while (iterator.hasNext()) {
                try {
                    ConcurrentHashMap.Entry<Ipv4Address, AsyncPingEntry> entry = iterator.next();
                    Ipv4Address destination = entry.getKey();
                    AsyncPingEntry asyncPingEntry = entry.getValue();

                    /** Re-initiate ping, as long as last attempt for doing it is finished
                     */
                    if (asyncPingEntry.pendingResult == null || asyncPingEntry.pendingResult.isDone()) {
                        /** Bump receive count if destination was reachable
                         */
                        if (asyncPingEntry != null) {
                            final SendEchoOutput.EchoResult echoResult =
                                    asyncPingEntry.pendingResult.get().getResult().getEchoResult();

                            asyncPingEntry.lastEchoResult = echoResult;
                            if (echoResult == SendEchoOutput.EchoResult.Reachable) { asyncPingEntry.countReceived++; }
                        }

                        /** Replace the entry for a key only if currently mapped to some value.
                         * This will protect the concurrent map against a race where this thread
                         * would be re-adding an entry that just got taken out.
                         *
                         * Note that by constructing a new AsyncPingEntry instance, the ping plugin is
                         * triggered and the cycle restarts by waiting for asyncPingEntry.pendingResult
                         * of the new instance to be done.
                         */
                        asyncPingEntryConcurrentMap.replace(destination,
                                new AsyncPingEntry(destination, pingService, asyncPingEntry));
                    }
                } catch (InterruptedException ie) {
                } catch (ExecutionException ee) {
                }
            }
        }
    }

    private PingResult mapResult(SendEchoOutput.EchoResult echoResult) {
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

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        this.session = session;
        this.pingService = this.session.getRpcService(PingService.class);
    }

    @Override
    protected void startImpl(BundleContext context) {
        asyncPingEntryConcurrentMap = new ConcurrentHashMap<Ipv4Address, AsyncPingEntry>();

        /* Async Ping Timer to go off every 1 second for periodic pingService behavior */
        asyncPingTimer = new Timer();
        asyncPingTimer.schedule(new AsyncPingTimerHandler(), 1000, 1000);

        context.registerService(PingServiceAPI.class, this, null);
    }

    @Override
    protected void stopImpl(BundleContext context) {
        asyncPingTimer.cancel();
        asyncPingEntryConcurrentMap.clear();
    }

    @Override
    public PingResult pingDestinationSync(String address) {
        if (pingService == null) { return PingResult.Error; } // No pingService service found.

        try {
            Ipv4Address destination = new Ipv4Address(address);
            SendEchoInputBuilder ib = new SendEchoInputBuilder();
            ib.setDestination(destination);
            return mapResult(pingService.sendEcho(ib.build()).get().getResult().getEchoResult());
        } catch (InterruptedException ie) {
        } catch (ExecutionException ee) {
        }
        return PingResult.Error;
    }

    @Override
    public PingResult pingDestinationAsync(String address) {
        if (pingService == null) { return PingResult.Error; } // No pingService service found.

        /** Look for destination in asyncPingEntryConcurrentMap. If none is found, create a new entry
         * and return "in progress". This will happen on the very first time async is requested.
         */
        Ipv4Address destination = new Ipv4Address(address);
        AsyncPingEntry asyncPingEntry = asyncPingEntryConcurrentMap.get(destination);
        if (asyncPingEntry == null) {
            asyncPingEntryConcurrentMap.put(destination, new AsyncPingEntry(destination, pingService, null));
            return PingResult.InProgress;
        }

        /** Pending result may not be ready to be consumed. In such case, use lastResult. If there has
         * not been a lastResult, then the only choice is to use "in progress".
         */
        if (!asyncPingEntry.pendingResult.isDone()) {
            return asyncPingEntry.lastEchoResult == null ?
                    PingResult.InProgress : mapResult(asyncPingEntry.lastEchoResult);
        }

        /** If we made it this far, we know that pendingResult contains the latest and greatest result
         */
        try {
            return mapResult(asyncPingEntry.pendingResult.get().getResult().getEchoResult());
        } catch (InterruptedException ie) {
        } catch (ExecutionException ee) {
        }
        return PingResult.Error;
    }

    @Override
    public void pingAsyncStop(String address) {
        asyncPingEntryConcurrentMap.remove( new Ipv4Address(address) );
    }
}
