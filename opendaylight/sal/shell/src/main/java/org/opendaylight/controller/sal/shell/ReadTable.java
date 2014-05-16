package org.opendaylight.controller.sal.shell;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.reader.IReadServiceShell;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.NodeTableCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "sal", name = "readTable", description="Reads the table of the sal read service")
public class ReadTable extends OsgiCommandSupport{

    private IReadServiceShell salReader;

    @Argument(index = 0, name = "argument", description = "The nodeId passed to the readTable command", required = false, multiValued = false)
    String arg0 = null;

    @Argument(index = 1, name = "argument", description = "The tableId passed to the readTable command", required = false, multiValued = false)
    String arg1 = null;

    @Argument(index = 2, name = "argument", description = "The cacheReq passed to the readTable command", required = false, multiValued = false)
    String arg2 = null;

    protected static final Logger logger = LoggerFactory.getLogger(ReadTable.class);
    @Override
    protected Object doExecute() throws Exception {
        String nodeId = arg0;
        String tableId = arg1;
        String cacheReq = arg2;
        boolean cached;
        if (nodeId == null) {
            System.out.print("Node id not specified");
            return null;
        }
        if (tableId == null) {
            System.out.print("Table id not specified");
            return null;
        }
        cached = (cacheReq == null) ? true : cacheReq.equals("true");
        NodeTable nodeTable = null;
        Node node = NodeCreator.createOFNode(Long.parseLong(nodeId));
        nodeTable = NodeTableCreator.createNodeTable(Byte
                .valueOf(tableId), node);
        NodeTableStatistics stats = (cached) ? salReader
                .readNodeTable(nodeTable) : salReader
                .nonCachedReadNodeTable(nodeTable);
                if (stats != null) {
                    System.out.println(stats.toString());
                } else {
                    System.out.println("null");
                }
        return null;
    }

    public void setSal(IReadServiceShell salReader){
        this.salReader = salReader;
    }
}