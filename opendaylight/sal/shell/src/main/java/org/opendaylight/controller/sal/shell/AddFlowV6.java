package org.opendaylight.controller.sal.shell;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerServiceShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "sal", name = "addFlowv6", description="Adds a flow to the sal flow programmer service")
public class AddFlowV6 extends OsgiCommandSupport{

    private IFlowProgrammerServiceShell salFlow;
    @Argument(index = 0, name = "argument", description = "The argument passed to the printNode command", required = false, multiValued = false)
    String name = null;
    protected static final Logger logger = LoggerFactory.getLogger(AddFlowV6.class);

    @Override
    protected Object doExecute() throws Exception {
        Node node = null;
        String nodeId = name;
        if (nodeId == null) {
            System.out.print("Node id not specified");
            return null;
        }
        try {
            node = new Node(NodeIDType.OPENFLOW, Long.valueOf(nodeId));
        } catch (NumberFormatException e) {
            logger.error("",e);
        } catch (ConstructionException e) {
            logger.error("",e);
        }
        System.out.print(salFlow.addFlow(node, salFlow.getFlowV6(node)));
        return null;
    }

    public void setSalFlow(IFlowProgrammerServiceShell salFlow){
        this.salFlow = salFlow;
    }
}