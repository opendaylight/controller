package org.opendaylight.controller.config.yang.doc.generator.plugin;

import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.H3;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.ID;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.TABLE;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addColumnToTableRow;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.loadModuleTemplate;

import java.io.IOException;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

public class ServiceDocGeneratorUtil {

    public static Document generateServiceDoc(final IdentitySchemaNode identity) {
        try {
            final Document doc = loadModuleTemplate("service-template.html");
            addServiceInfo(identity, doc);
            return doc;
        } catch (IOException e) {
            // TODO log error
            throw new IllegalStateException("Failed to read/write file.", e);
        }
    }

    private static void addServiceInfo(final IdentitySchemaNode identity, final Document doc) {
        doc.select(H3).first().text("Service: " + identity.getQName().getLocalName());

        final Element moduleInfoTable = doc.select(TABLE).first();
        moduleInfoTable.attr(ID, identity.getQName().getLocalName());
        addColumnToTableRow(moduleInfoTable.getElementById("description").parent(), identity.getDescription());
        addColumnToTableRow(moduleInfoTable.getElementById("java-class").parent(), getOsgiRegType(identity));
    }

    private static String getOsgiRegType(final IdentitySchemaNode identity) {
        for (UnknownSchemaNode usn : identity.getUnknownSchemaNodes()) {
            if (usn.getNodeType().getLocalName().contains("java-class")) {
                return usn.getNodeParameter();
            }
        }
        return "";
    }

}
