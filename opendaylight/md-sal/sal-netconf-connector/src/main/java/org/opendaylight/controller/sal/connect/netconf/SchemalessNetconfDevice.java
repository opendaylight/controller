package org.opendaylight.controller.sal.connect.netconf;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceHandler;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.netconf.sal.SchemalessDataBroker;
import org.opendaylight.controller.sal.connect.netconf.sal.SchemalessNetconfDeviceRpc;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;

/**
 * Created by mmarsale on 4.11.2015.
 */
public class SchemalessNetconfDevice implements
    RemoteDevice<NetconfSessionPreferences, NetconfMessage, NetconfDeviceCommunicator> {

    private RemoteDeviceId id;
    private RemoteDeviceHandler<NetconfSessionPreferences> salFacade;

    public SchemalessNetconfDevice(final RemoteDeviceId id,
        final RemoteDeviceHandler<NetconfSessionPreferences> salFacade) {
        this.id = id;
        this.salFacade = salFacade;
    }

    @Override public void onRemoteSessionUp(final NetconfSessionPreferences remoteSessionCapabilities,
        final NetconfDeviceCommunicator netconfDeviceCommunicator) {

        // constructe RPC, DataBroker and manybe NotificationService
        final SchemalessNetconfDeviceRpc schemalessNetconfDeviceRpc = new SchemalessNetconfDeviceRpc(
            netconfDeviceCommunicator);
        final SchemalessDataBroker schemalessDataBroker = new SchemalessDataBroker(schemalessNetconfDeviceRpc);

        salFacade.onDeviceConnected(null, remoteSessionCapabilities, schemalessNetconfDeviceRpc);
    }

    @Override public void onRemoteSessionDown() {
        salFacade.onDeviceDisconnected();
    }

    @Override public void onRemoteSessionFailed(final Throwable throwable) {
        salFacade.onDeviceFailed(throwable);
    }

    @Override public void onNotification(final NetconfMessage notification) {
        // TODO
    }
}
