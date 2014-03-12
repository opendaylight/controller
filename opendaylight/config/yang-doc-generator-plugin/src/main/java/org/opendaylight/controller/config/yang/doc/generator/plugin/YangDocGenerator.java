package org.opendaylight.controller.config.yang.doc.generator.plugin;

import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.H2;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.TR;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addColumnToTableRow;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addSummary;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.loadModuleTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleImport;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class YangDocGenerator {

    private final SchemaContext context;
    private final File outputBaseDir;
    private Map<String, Set<String>> serviceToModuleMapping;
    private Set<File> generatedFiles = Sets.newHashSet();

    public YangDocGenerator(SchemaContext context, final File outputBaseDir) {
        this.context = context;
        this.outputBaseDir = outputBaseDir;
    }

    public Collection<File> generateDoc(Set<Module> currentModules) {
        if (!outputBaseDir.exists())
            outputBaseDir.mkdirs();
        this.serviceToModuleMapping = getServiceMapping();
        for (final Module module : currentModules) {
            try {
                final Document doc = loadModuleTemplate("yang-module-template.html");
                fillHtml(module, doc);
                final File file = DocGeneratorUtil.writeToFile(outputBaseDir, module.getName() + "_yang.html", doc);
                this.generatedFiles.add(file);
            } catch (IOException e) {
                // TODO log error
                throw new IllegalStateException("Failed to read/write file.", e);
            }
        }
        return this.generatedFiles;
    }

    private void fillHtml(final Module module, final Document doc) {
        addModuleInfo(module, doc);
        addSummaries(module, doc);
    }

    private void addModuleInfo(final Module module, final Document doc) {
        doc.title("Yang Module | " + module.getName());
        doc.select(H2).first().text("Yang Module: " + module.getName());

        final Element moduleInfoTable = doc.getElementById("module-info");
        addColumnToTableRow(moduleInfoTable.getElementById("description").parent(), module.getDescription());
        addColumnToTableRow(moduleInfoTable.getElementById("namespace").parent(), module.getNamespace().toString());
        addColumnToTableRow(moduleInfoTable.getElementById("prefix").parent(), module.getPrefix());
        addColumnToTableRow(moduleInfoTable.getElementById("revision").parent(), module.getRevision().toString());

        if (module.getImports() != null && !module.getImports().isEmpty()) {
            // TODO use string builder or put every import to the new line
            String imports = "";
            for (ModuleImport inport : module.getImports()) {
                String fullImport = inport.getPrefix() + ":" + inport.getModuleName() + ", " + inport.getRevision();
                imports += fullImport + "; ";
            }
            addColumnToTableRow(moduleInfoTable.getElementById("imports").parent(), imports);
        }
    }

    private void addSummaries(final Module module, final Document doc) {
        for (final IdentitySchemaNode identity : module.getIdentities()) {
            final IdentitySchemaNode base = identity.getBaseIdentity();
            if (base != null) {
                String localName = base.getQName().getLocalName();
                if (localName.equalsIgnoreCase("service-type")) {
                    addServiceSummary(module, identity, doc);
                    final File file = ServiceDocGeneratorUtil.generateServiceDoc(module, identity, outputBaseDir,
                            this.serviceToModuleMapping.get(identity.getQName().getLocalName()));
                    this.generatedFiles.add(file);
                } else if (localName.equalsIgnoreCase("module-type")) {
                    addModuleSummary(module, identity, doc);
                    final File file = ModuleDocGenerator.generateModule(module, identity, outputBaseDir);
                    this.generatedFiles.add(file);
                }
                // TODO add rpc summary
            }
        }
    }

    private static String getProvidedService(final IdentitySchemaNode identity) {
        List<UnknownSchemaNode> usns = identity.getUnknownSchemaNodes();
        if (usns != null && !usns.isEmpty()) {
            for (final UnknownSchemaNode usn : usns) {
                if (usn.getNodeType().getLocalName().contains("provided-service")) {
                    return usn.getNodeParameter();
                }
            }
        }
        return "";
    }

    private static void addModuleSummary(final Module module, final IdentitySchemaNode identity, final Document doc) {
        Element servicesTable = doc.getElementById("module-summary").child(0).appendElement(TR);
        addSummary(module, identity, servicesTable, "module");
    }

    private static void addServiceSummary(final Module module, final IdentitySchemaNode identity, final Document doc) {
        Element servicesTable = doc.getElementById("service-summary").child(0).appendElement(TR);
        addSummary(module, identity, servicesTable, "service");
    }

    private Map<String, Set<String>> getServiceMapping() {
        Map<String, Set<String>> serviceToModuleMap = Maps.newHashMap();
        for (final Module module : this.context.getModules()) {
            for (final IdentitySchemaNode identity : module.getIdentities()) {
                final IdentitySchemaNode base = identity.getBaseIdentity();
                if (base != null) {
                    String localName = base.getQName().getLocalName();
                    if (localName.equalsIgnoreCase("module-type")) {
                        addToServiceMap(getProvidedService(identity), identity.getQName().getLocalName(),
                                serviceToModuleMap);
                    }
                }
            }
        }
        System.out.println("service mapping: " + serviceToModuleMap);
        return serviceToModuleMap;
    }

    private static void addToServiceMap(final String serviceName, final String moduleName, Map<String, Set<String>> map) {
        if (map.containsKey(serviceName)) {
            map.get(serviceName).add(moduleName);
        } else {
            map.put(serviceName, Sets.newHashSet(moduleName));
        }
    }

}
