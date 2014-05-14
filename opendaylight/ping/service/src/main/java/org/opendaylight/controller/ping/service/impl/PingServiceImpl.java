package org.opendaylight.controller.ping.service.impl;

import java.util.concurrent.ExecutionException;

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

    @Override
    public void onSessionInitialized(ConsumerContext session) {
        this.session = session;
    }

    @Override
    protected void startImpl(BundleContext context) {
        context.registerService(PingServiceAPI.class, this, null);
    }

    @Override
    public boolean pingDestination(String address) {

        if (ping == null) {
            ping = this.session.getRpcService(PingService.class);
            if (ping == null) {

                /* No ping service found. */
                return false;
            }
        }

        Ipv4Address destination = new Ipv4Address(address);

        SendEchoInputBuilder ib = new SendEchoInputBuilder();
        ib.setDestination(destination);
        try {
            RpcResult<SendEchoOutput> result = ping.sendEcho(ib.build()).get();
            switch (result.getResult().getEchoResult()) {
            case Reachable:
                return true;
            case Unreachable:
            case Error:
            default:
                return false;
            }
        } catch (InterruptedException ie) {
        } catch (ExecutionException ee) {
        }

        return false;
    }

}
