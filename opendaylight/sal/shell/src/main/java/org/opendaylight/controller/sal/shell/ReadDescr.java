package org.opendaylight.controller.sal.shell;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.reader.IReadServiceShell;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "sal", name = "readDescr", description="Read the description")
public class ReadDescr extends OsgiCommandSupport{

    private IReadServiceShell salReader;
    @Argument(index = 0, name = "argument", description = "The nodeId passed to the printNode command", required = false, multiValued = false)
    String arg0 = null;
    @Argument(index = 0, name = "argument", description = "The cacheReq passed to the printNode command", required = false, multiValued = false)
    String arg1 = null;
    protected static final Logger logger = LoggerFactory.getLogger(ReadDescr.class);

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
        NodeDescription desc = (cached) ? salReader.readDescription(node) : salReader
                .nonCachedReadDescription(node);
        if (desc != null) {
            System.out.println(desc.toString());
        } else {
            System.out.println("null");
        }
        return null;
    }

    public void setSalReader(IReadServiceShell salReader){
        this.salReader = salReader;
    }
}