package org.opendaylight.controller.sal.shell;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadServiceShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "sal", name = "readFlows", description="Read the flows within the sal service")
public class ReadFlows extends OsgiCommandSupport{

    private IReadServiceShell salReader;
    @Argument(index = 0, name = "argument", description = "The nodeId passed to the readFlows command", required = false, multiValued = false)
    String arg0 = null;

    @Argument(index = 1, name = "argument", description = "The cacheReq passed to the readFlows command", required = false, multiValued = false)
    String arg1 = null;

    protected static final Logger logger = LoggerFactory.getLogger(ReadFlows.class);

    @Override
    protected Object doExecute() throws Exception {
        String nodeId = arg0;
        String cacheReq = arg1;
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
        List<FlowOnNode> list = (cached) ? salReader.readAllFlows(node) : salReader.nonCachedReadAllFlows(node);
        if (list != null) {
            System.out.println(list.toString());
        } else {
            System.out.println("null");
        }
        return null;
    }

    public void setSalReader(IReadServiceShell salReader){
        this.salReader = salReader;
    }
}