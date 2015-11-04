package org.opendaylight.controller.sal.connect.netconf;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;

/**
 * Created by mmarsale on 4.11.2015.
 */
public class SchemalessNetconfDevice implements
    RemoteDevice<NetconfSessionPreferences, NetconfMessage, NetconfDeviceCommunicator> {


    @Override public void onRemoteSessionUp(final NetconfSessionPreferences remoteSessionCapabilities,
        final NetconfDeviceCommunicator netconfDeviceCommunicator) {

    }

    @Override public void onRemoteSessionDown() {

    }

    @Override public void onRemoteSessionFailed(final Throwable throwable) {

    }

    @Override public void onNotification(final NetconfMessage notification) {

    }
}
