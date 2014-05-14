package org.opendaylight.controller.ping.service.impl;

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

    private PingService ping;
    private ConsumerContext session;

    private Ipv4Address currDestination;
    private Future<RpcResult<SendEchoOutput>> currResult;

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        this.session = session;
    }

    @Override
    protected void startImpl(BundleContext context) {
        context.registerService(PingServiceAPI.class, this, null);
    }

    @Override
    public PingResult pingDestinationSync(String address) {
        return pingDestinationCommon(address, true);
    }

    @Override
    public PingResult pingDestinationAsync(String address) {
        return pingDestinationCommon(address, false);
    }

    private PingResult pingDestinationCommon(String address, boolean isSync) {

        if (ping == null) {
            ping = this.session.getRpcService(PingService.class);
            if (ping == null) {

                /* No ping service found. */
                return PingResult.Error;
            }
        }

        Ipv4Address newDestination = new Ipv4Address(address);
        if (isSync || currDestination == null || !currDestination.equals(newDestination)) {
            SendEchoInputBuilder ib = new SendEchoInputBuilder();
            ib.setDestination(newDestination);
            currDestination = newDestination;
            currResult = ping.sendEcho(ib.build());
        }

        try {
            if (!isSync && !currResult.isDone()) {
                return PingResult.InProgress;
            }

            /** If we made it here, we know that the result is completed. Being so, we will
             * clear currDestination to trigger a new build() cycle next time this call is made.
             */
            currDestination = Ipv4Address.getDefaultInstance("0.0.0.0");

            // Translate echoResult to pingResult
            switch (currResult.get().getResult().getEchoResult()) {
            case Reachable:
                return PingResult.GotResponse;
            case Unreachable:
                return PingResult.NoResponse;
            case Error:
            default:
                return PingResult.Error;
            }
        } catch (InterruptedException ie) {
        } catch (ExecutionException ee) {
        }

        return PingResult.Error;
    }

}
