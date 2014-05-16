package org.opendaylight.controller.sal.shell;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.reader.IReadServiceShell;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "sal", name = "readPort", description="Read the port of the sal read service")
public class ReadPort extends OsgiCommandSupport{

    private IReadServiceShell salReader;
    @Argument(index = 0, name = "argument", description = "The nodeId passed to the readPort command", required = false, multiValued = false)
    String arg0 = null;

    @Argument(index = 1, name = "argument", description = "The portId passed to the readPort command", required = false, multiValued = false)
    String arg1 = null;

    @Argument(index = 2, name = "argument", description = "The cacheReq passed to the readPort command", required = false, multiValued = false)
    String arg2 = null;

    protected static final Logger logger = LoggerFactory.getLogger(ReadPort.class);

    @Override
    protected Object doExecute() throws Exception {
        String nodeId = arg0;
        String portId = arg1;
        String cacheReq = arg2;
        boolean cached;
        if (nodeId == null) {
            System.out.print("Node id not specified");
            return null;
        }
        if (portId == null) {
            System.out.print("Port id not specified");
            return null;
        }
        cached = (cacheReq == null) ? true : cacheReq.equals("true");
        NodeConnector nodeConnector = null;
        Node node = NodeCreator.createOFNode(Long.parseLong(nodeId));
        nodeConnector = NodeConnectorCreator.createNodeConnector(Short
                .valueOf(portId), node);
        NodeConnectorStatistics stats = (cached) ? salReader
                .readNodeConnector(nodeConnector) : salReader
                .nonCachedReadNodeConnector(nodeConnector);
                if (stats != null) {
                    System.out.println(stats.toString());
                } else {
                    System.out.println("null");
                }
        return null;
    }

    public void setSalReader(IReadServiceShell salReader){
        this.salReader = salReader;
    }
}