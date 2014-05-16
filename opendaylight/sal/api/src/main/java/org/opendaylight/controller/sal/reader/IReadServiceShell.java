package org.opendaylight.controller.sal.reader;

import java.net.UnknownHostException;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;

public interface IReadServiceShell extends IReadService{
    public Flow getFlow(Node node) throws UnknownHostException;
}
