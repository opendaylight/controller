package org.opendaylight.controller.config.yang.doc.generator.plugin;

import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.H2;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addColumnToTableRow;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addColumnWithLinkToTableRow;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.loadModuleTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

public class ServiceDocGeneratorUtil {

    public static File generateServiceDoc(final Module module, final IdentitySchemaNode identity,
            final File outputBaseDir, Set<String> modules) {
        try {
            final Document doc = loadModuleTemplate("service-template.html");
            addServiceInfo(module, identity, doc, modules);
            return DocGeneratorUtil.writeToFile(outputBaseDir, identity.getQName().getLocalName() + "_service.html",
                    doc);
        } catch (IOException e) {
            // TODO log error
            throw new IllegalStateException("Failed to read/write file.", e);
        }
    }

    private static void addServiceInfo(final Module module, final IdentitySchemaNode identity, final Document doc,
            Set<String> modules) {
        doc.title("Service | " + module.getName());
        doc.select(H2).first().text("Service: " + module.getName());

        final Element moduleInfoTable = doc.getElementById("service-info");
        addColumnWithLinkToTableRow(moduleInfoTable.getElementById("module").parent(), module.getName(), "yang");
        addColumnToTableRow(moduleInfoTable.getElementById("description").parent(), identity.getDescription());
        addColumnToTableRow(moduleInfoTable.getElementById("java-class").parent(), getOsgiRegType(identity));
        addColumnToTableRow(moduleInfoTable.getElementById("implementations").parent(), modulesToString(modules));
    }

    private static String getOsgiRegType(final IdentitySchemaNode identity) {
        for (UnknownSchemaNode usn : identity.getUnknownSchemaNodes()) {
            if (usn.getNodeType().getLocalName().contains("java-class")) {
                return usn.getNodeParameter();
            }
        }
        return "";
    }

    private static String modulesToString(Set<String> modules) {
        // TODO string builder
        String implModules = "";
        if (modules != null && !modules.isEmpty()) {
            for (final String module : modules) {
                implModules += module + " ";
            }
        }
        return implModules;
    }

}
