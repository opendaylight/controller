package org.opendaylight.controller.config.yang.doc.generator.plugin;

import static com.google.common.base.Preconditions.checkState;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.H2;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.TR;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addColumnToTableRow;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addColumnWithLinkToTableRow;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.loadModuleTemplate;

import java.io.File;
import java.io.IOException;

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

public class ModuleDocGenerator {

    public static File generateModule(final Module module, final IdentitySchemaNode identity, final File outputBaseDir) {
        try {
            final Document doc = loadModuleTemplate("module-template.html");
            addModuleInfo(module, identity, doc);
            addConfigArgumentsSummary(module, identity, doc);
            addSateSummary(module, identity, doc);
            return DocGeneratorUtil
                    .writeToFile(outputBaseDir, identity.getQName().getLocalName() + "_module.html", doc);
        } catch (IOException e) {
            // TODO log error
            throw new IllegalStateException("Failed to read/write file.", e);
        }
    }

    private static void addModuleInfo(final Module module, final IdentitySchemaNode identity, final Document doc) {
        doc.title("Module | " + module.getName());
        doc.select(H2).first().text("Module: " + module.getName());

        final Element moduleInfoTable = doc.getElementById("module-info");
        addColumnWithLinkToTableRow(moduleInfoTable.getElementById("module").parent(), module.getName(), "yang");
        addColumnToTableRow(moduleInfoTable.getElementById("description").parent(), identity.getDescription());
        addColumnToTableRow(moduleInfoTable.getElementById("provided-service").parent(), getProvidedService(identity));
        addColumnToTableRow(moduleInfoTable.getElementById("java-prefix").parent(), getJavaPrefix(identity));
    }

    private static void addConfigArgumentsSummary(final Module module, final IdentitySchemaNode identity,
            final Document doc) {
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
                                        addColumnToTableRow(servicesTable, requiredIdentity.getNodeParameter()
                                                + " (container)");
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

}
