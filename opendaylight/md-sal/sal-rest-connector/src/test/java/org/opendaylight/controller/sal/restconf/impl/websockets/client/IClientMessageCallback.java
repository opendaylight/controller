package org.opendaylight.controller.sal.restconf.impl.websockets.client;

/**
 * Created by mbobak on 1/22/14.
 */
public interface IClientMessageCallback {

    public void onMessageReceived(Object message);
}
