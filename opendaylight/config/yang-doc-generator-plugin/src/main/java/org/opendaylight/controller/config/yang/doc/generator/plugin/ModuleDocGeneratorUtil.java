package org.opendaylight.controller.config.yang.doc.generator.plugin;

import static com.google.common.base.Preconditions.checkState;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.H3;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.ID;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.TABLE;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.TR;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addColumnToTableRow;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addColumnWithAnchorLink;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.loadModuleTemplate;

import java.io.IOException;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

public class ModuleDocGeneratorUtil {

    public static Document generateModule(final Module module, final IdentitySchemaNode identity, final Map<String, String> importsMap) {
        try {
            final Document doc = loadModuleTemplate("module-template.html");
            addModuleInfo(module, identity, doc, importsMap);
            addConfigArgumentsSummary(module, identity, doc, importsMap);
            addSateSummary(module, identity, doc);
            return doc;
        } catch (IOException e) {
            // TODO log error
            throw new IllegalStateException("Failed to read/write file.", e);
        }
    }

    private static void addModuleInfo(final Module module, final IdentitySchemaNode identity, final Document doc, final Map<String, String> importsMap) {
        doc.select(H3).first().text("Module: " + identity.getQName().getLocalName());

        final Element moduleInfoTable = doc.select(TABLE).first();
        moduleInfoTable.attr(ID, identity.getQName().getLocalName());
        addColumnToTableRow(moduleInfoTable.getElementById("description").parent(), identity.getDescription());
        final String providedIdentity = getProvidedService(identity);
        addColumnWithAnchorLink(moduleInfoTable.getElementById("provided-service").parent(), providedIdentity, importsMap.get(getPrefix(providedIdentity)), removePrefix(providedIdentity));
        addColumnToTableRow(moduleInfoTable.getElementById("java-prefix").parent(), getJavaPrefix(identity));
    }

    //TODO refactor
    private static void addConfigArgumentsSummary(final Module module, final IdentitySchemaNode identity,
            final Document doc, final Map<String, String> importsMap) {
        for (final AugmentationSchema augmentation : module.getAugmentations()) {
            if (augmentation.getTargetPath().toString().contains("configuration")) {
                if (augmentation.getChildNodes() != null && !augmentation.getChildNodes().isEmpty()) {
                    for (final DataSchemaNode childNode : augmentation.getChildNodes()) {
                        if (identity.getQName().getLocalName().equalsIgnoreCase(childNode.getQName().getLocalName())) {
                            if (childNode instanceof ChoiceCaseNode) {
                                ChoiceCaseNode caseNode = (ChoiceCaseNode) childNode;
                                for (final DataSchemaNode choiceChildNode : caseNode.getChildNodes()) {
                                    if (choiceChildNode instanceof LeafSchemaNode) {
                                        final LeafSchemaNode schemaNode = (LeafSchemaNode) choiceChildNode;
                                        Element servicesTable = doc.getElementById("config-summary").child(0)
                                                .appendElement(TR);
                                        addColumnToTableRow(servicesTable, schemaNode.getType().getQName()
                                                .getLocalName()
                                                + " (leaf)");
                                        addColumnToTableRow(servicesTable, schemaNode.getQName().getLocalName());
                                    } else if (choiceChildNode instanceof LeafListSchemaNode) {
                                        final LeafListSchemaNode schemaNode = (LeafListSchemaNode) choiceChildNode;
                                        Element servicesTable = doc.getElementById("config-summary").child(0)
                                                .appendElement(TR);
                                        addColumnToTableRow(servicesTable, schemaNode.getType().getQName()
                                                .getLocalName()
                                                + " (leaf-list)");
                                        addColumnToTableRow(servicesTable, schemaNode.getQName().getLocalName());
                                    } else if (choiceChildNode instanceof ListSchemaNode) {
                                        final ListSchemaNode schemaNode = (ListSchemaNode) choiceChildNode;
                                        Element servicesTable = doc.getElementById("config-summary").child(0)
                                                .appendElement(TR);
                                        addColumnToTableRow(servicesTable, getJavaPrefix(schemaNode) + " (list)");
                                        addColumnToTableRow(servicesTable, schemaNode.getQName().getLocalName());
                                    } else if (choiceChildNode instanceof ContainerSchemaNode) {
                                        final ContainerSchemaNode schemaNode = (ContainerSchemaNode) choiceChildNode;
                                        Element servicesTable = doc.getElementById("config-summary").child(0)
                                                .appendElement(TR);
                                        LeafSchemaNode refine = (LeafSchemaNode) schemaNode.getUses().iterator().next()
                                                .getRefines().values().iterator().next();
                                        checkState(refine.getUnknownSchemaNodes().size() == 1,
                                                "Unexpected unknown schema node size of " + refine);
                                        UnknownSchemaNode requiredIdentity = refine.getUnknownSchemaNodes().iterator()
                                                .next();
                                        final String nodeName = requiredIdentity.getNodeParameter();
                                        addColumnWithAnchorLink(servicesTable, nodeName + " (container)", importsMap.get(getPrefix(nodeName)), removePrefix(nodeName));
                                        addColumnToTableRow(servicesTable, schemaNode.getQName().getLocalName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void addSateSummary(final Module module, final IdentitySchemaNode identity, final Document doc) {

    }

    private static String getProvidedService(final IdentitySchemaNode identity) {
        if (identity.getUnknownSchemaNodes() != null && !identity.getUnknownSchemaNodes().isEmpty()) {
            for (UnknownSchemaNode usn : identity.getUnknownSchemaNodes()) {
                if (usn.getNodeType().getLocalName().contains("provided-service")) {
                    return usn.getNodeParameter();
                }
            }
        }
        return "";
    }

    private static String getJavaPrefix(final IdentitySchemaNode identity) {
        if (identity.getUnknownSchemaNodes() != null && !identity.getUnknownSchemaNodes().isEmpty()) {
            for (UnknownSchemaNode usn : identity.getUnknownSchemaNodes()) {
                if (usn.getNodeType().getLocalName().contains("java-name-prefix")) {
                    return usn.getNodeParameter();
                }
            }
        }
        return "";
    }

    private static String getJavaPrefix(final ListSchemaNode schemaNode) {
        if (schemaNode.getUnknownSchemaNodes() != null && !schemaNode.getUnknownSchemaNodes().isEmpty()) {
            for (UnknownSchemaNode usn : schemaNode.getUnknownSchemaNodes()) {
                if (usn.getNodeType().getLocalName().contains("java-name-prefix")) {
                    return usn.getNodeParameter();
                }
            }
        }
        return "";
    }

    private static String getPrefix(final String serviceName) {
        int index = serviceName.indexOf(":");
        if(index != -1) {
            return serviceName.substring(0, index);
        }
        return "";
    }

    private static String removePrefix(final String serviceName) {
        int index = serviceName.indexOf(":");
        if(index != -1) {
            return serviceName.substring(index+1);
        }
        return serviceName;
    }

}
