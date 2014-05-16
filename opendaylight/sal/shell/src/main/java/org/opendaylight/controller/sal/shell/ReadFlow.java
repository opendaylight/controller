package org.opendaylight.controller.sal.shell;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadServiceShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "sal", name = "readFlow", description="Reads the flow from the sal flow programmer service")
public class ReadFlow extends OsgiCommandSupport{

    private IReadServiceShell salReader;
    @Argument(index = 0, name = "argument", description = "The nodeId passed to the readFlow command", required = false, multiValued = false)
    String name = null;

    @Argument(index = 1, name = "argument", description = "The cacheReq passed to the readFlow command", required = false, multiValued = false)
    String name2 = null;

    protected static final Logger logger = LoggerFactory.getLogger(ReadFlow.class);

    @Override
    protected Object doExecute() throws Exception {
        String nodeId = name;
        String cacheReq = name2;
        boolean cached;
        if (nodeId == null) {
            System.out.print("Node id not specified");
            return null;
        }
        cached = (cacheReq == null) ? true : cacheReq.equals("true");
        Node node = null;
        try {
            node = new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            logger.error("",e);
        } catch (ConstructionException e) {
            logger.error("",e);
        }
        Flow flow = salReader.getFlow(node);
        FlowOnNode flowOnNode = (cached) ? salReader.readFlow(node, flow) : salReader.nonCachedReadFlow(node, flow);
        if (flowOnNode != null) {
            System.out.println(flowOnNode.toString());
        } else {
            System.out.println("null");
        }
        return null;
    }

    public void setSalReader(IReadServiceShell salReader){
        this.salReader = salReader;
    }
}