package org.opendaylight.controller.protocol_plugin.openflow.core;

import java.util.List;


public interface IControllerShell {
    public List<String> controllerShowQueueSize();
    public List<String> controllerShowSwitches();
    public List<String> controllerReset();
    public List<String> controllerShowConnConfig();
}
