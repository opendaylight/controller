package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.collect.ForwardingObject;

final class KilledPingPongTransaction extends ForwardingObject {
    private PingPongTransaction delegate;

    @Override
    public PingPongTransaction delegate() {
        return delegate;
    }

}
