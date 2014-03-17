package org.opendaylight.controller.config.yang.doc.generator.plugin;

import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.H3;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.ID;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.TABLE;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addColumnToTableRow;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.loadModuleTemplate;

import java.io.IOException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;

public class RpcDocGeneratorUtil {

    public static Document generateRpcDoc(final RpcDefinition rpcDefinition) {
        try {
            final Document doc = loadModuleTemplate("rpc-template.html");
            addRpcInfo(rpcDefinition, doc);
            return doc;
        } catch (IOException e) {
            // TODO log error
            throw new IllegalStateException("Failed to read/write file.", e);
        }
    }

    private static void addRpcInfo(final RpcDefinition rpcDefinition, final Document doc) {
        doc.select(H3).first().text("Service: " + rpcDefinition.getQName().getLocalName());

        final Element moduleInfoTable = doc.select(TABLE).first();
        moduleInfoTable.attr(ID, rpcDefinition.getQName().getLocalName());
        addColumnToTableRow(moduleInfoTable.getElementById("description").parent(), rpcDefinition.getDescription());
        addColumnToTableRow(moduleInfoTable.getElementById("input").parent(), getInput(rpcDefinition));
        addColumnToTableRow(moduleInfoTable.getElementById("output").parent(), getOutput(rpcDefinition));
    }

    //TODO finish implementation
    private static String getInput(RpcDefinition rpcDefinition) {
        ContainerSchemaNode imputNode = rpcDefinition.getInput();
        if(imputNode != null && imputNode.getQName() != null) {
            return imputNode.getQName().getLocalName();
        }
        return "";
    }

    //TODO finish implementation
    private static String getOutput(RpcDefinition rpcDefinition) {
        ContainerSchemaNode outputNode = rpcDefinition.getOutput();
        if(outputNode != null && outputNode.getQName() != null) {
            return outputNode.getQName().getLocalName();
        }
        return "";
    }

}
