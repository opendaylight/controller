package org.opendaylight.controller.sal.connect.netconf;

import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class SchemalessNetconfFacade implements RemoteDeviceHandler<NetconfSessionPreferences> {

    @Override public void onDeviceConnected(final SchemaContext remoteSchemaContext,
        final NetconfSessionPreferences netconfSessionPreferences, final DOMRpcService deviceRpc) {


    }

    @Override public void onDeviceDisconnected() {

    }

    @Override public void onDeviceFailed(final Throwable throwable) {

    }

    @Override public void onNotification(final DOMNotification domNotification) {

    }

    @Override public void close() {

    }
}
