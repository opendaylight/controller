package org.opendaylight.controller.md.sal.dom.broker.impl.legacy.rpc.adapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;

class DOMRpcListenerAdapter<T extends org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener>
        implements DOMRpcAvailabilityListener {

    private final T listener;

    DOMRpcListenerAdapter(T listener) {
        this.listener = listener;
    }

    @Override
    public void onRpcAvailable(Collection<DOMRpcIdentifier> rpcs) {
        listener.onRpcAvailable(transformRpc(rpcs));
    }

    @Override
    public void onRpcUnavailable(Collection<DOMRpcIdentifier> rpcs) {
        listener.onRpcAvailable(transformRpc(rpcs));
    }

    private static Collection<org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier> transformRpc(
            Collection<DOMRpcIdentifier> rpcs) {

        List<org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier> trList = new ArrayList<>(rpcs.size());
        for (DOMRpcIdentifier rpc : rpcs) {
            trList.add(org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier.create(rpc.getType(),
                    rpc.getContextReference()));
        }
        return Collections.unmodifiableCollection(trList);
    }

}
