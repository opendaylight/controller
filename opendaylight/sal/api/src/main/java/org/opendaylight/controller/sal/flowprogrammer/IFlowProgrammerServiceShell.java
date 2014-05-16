package org.opendaylight.controller.sal.flowprogrammer;

import java.net.UnknownHostException;
import org.opendaylight.controller.sal.core.Node;

public interface IFlowProgrammerServiceShell extends IFlowProgrammerService{
    public Flow getFlow(Node node) throws UnknownHostException ;
    public Flow getFlowV6(Node node) throws UnknownHostException;
}
