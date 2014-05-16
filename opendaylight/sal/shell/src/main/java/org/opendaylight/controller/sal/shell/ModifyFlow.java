package org.opendaylight.controller.sal.shell;

import java.net.InetAddress;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerServiceShell;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "sal", name = "modifyFlow", description="Modifies a sal programmer service flow")
public class ModifyFlow extends OsgiCommandSupport{

    private IFlowProgrammerServiceShell salFlow;
    @Argument(index = 0, name = "argument", description = "The argument passed to the modify flow command", required = false, multiValued = false)
    String name = null;
    protected static final Logger logger = LoggerFactory.getLogger(ModifyFlow.class);

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
        Flow flowA = salFlow.getFlow(node);
        Flow flowB = salFlow.getFlow(node);
        Match matchB = flowB.getMatch();
        matchB.setField(MatchType.NW_DST,
                InetAddress.getByName("190.190.190.190"));
        flowB.setMatch(matchB);
        System.out.println(salFlow.modifyFlow(node, flowA, flowB));
        return null;
    }

    public void setSalFlow(IFlowProgrammerServiceShell salFlow){
        this.salFlow = salFlow;
    }
}