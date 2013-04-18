package org.opendaylight.controller.protocol_plugin.openflow;

import org.opendaylight.controller.sal.flowprogrammer.IPluginOutFlowProgrammerService;

/**
 * Interface which defines the methods exposed by the Flow Programmer Notifier.
 * Their implementation relays the asynchronous messages received from the
 * network nodes to the the SAL Flow Programmer Notifier Service on the proper
 * container.
 */
public interface IFlowProgrammerNotifier extends
        IPluginOutFlowProgrammerService {

}
