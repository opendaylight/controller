package org.opendaylight.controller.config.yang.doc.generator.plugin;

import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.H2;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.TD;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.TR;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addColumnToTableRow;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addLink;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.addSummary;
import static org.opendaylight.controller.config.yang.doc.generator.plugin.DocGeneratorUtil.loadModuleTemplate;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.opendaylight.yangtools.yang.model.api.IdentitySchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleImport;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class YangDocGenerator {

    private final File outputBaseDir;
    private Set<File> generatedFiles = Sets.newHashSet();
    private final Map<String, String> importsMap = Maps.newHashMap();

    public YangDocGenerator(final File outputBaseDir) {
        this.outputBaseDir = outputBaseDir;
    }

    public Collection<File> generateDoc(Set<Module> currentModules) {
        if (!outputBaseDir.exists())
            outputBaseDir.mkdirs();
        for (final Module module : currentModules) {
            try {
                final Document doc = loadModuleTemplate("yang-module-template.html");
                fillHtml(module, doc);
                final File file = DocGeneratorUtil.writeToFile(outputBaseDir, module.getName() + ".html", doc);
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
        addColumnToTableRow(moduleInfoTable.getElementById("revision").parent(), getDate(module.getRevision()));

        if (module.getImports() != null && !module.getImports().isEmpty()) {
            Element elm = moduleInfoTable.getElementById("imports").parent().appendElement(TD);
            for (final ModuleImport importModule : module.getImports()) {
                final String importPrefix = importModule.getPrefix();
                final String importName = importModule.getModuleName();
                this.importsMap.put(importPrefix, importName);
                String fullImport = importPrefix + ":" + importName + " " + getDate(importModule.getRevision());
                addLink(elm, fullImport, importName).appendElement("br");
            }
        }
    }


    private void addSummaries(final Module module, final Document doc) {
        for (final IdentitySchemaNode identity : module.getIdentities()) {
            final IdentitySchemaNode base = identity.getBaseIdentity();
            if (base != null) {
                String localName = base.getQName().getLocalName();
                if (localName.equalsIgnoreCase("service-type")) {
                    addServiceSummary(identity, doc);
                    final Document serviceDoc = ServiceDocGeneratorUtil.generateServiceDoc(identity);
                    doc.getElementById("services").append(serviceDoc.outerHtml());
                } else if (localName.equalsIgnoreCase("module-type")) {
                    addModuleSummary(identity, doc);
                    final Document moduleDoc = ModuleDocGeneratorUtil.generateModule(module, identity, this.importsMap);
                    doc.getElementById("modules").append(moduleDoc.outerHtml());
                }
            }
        }
        for(RpcDefinition rpcDef : module.getRpcs()) {
            addRpcSummary(rpcDef, doc);
            final Document rpcDoc = RpcDocGeneratorUtil.generateRpcDoc(rpcDef);
            doc.getElementById("rpcs").append(rpcDoc.outerHtml());
        }
    }

    private static void addModuleSummary(final SchemaNode schemaNode, final Document doc) {
        Element servicesTable = doc.getElementById("module-summary").child(0).appendElement(TR);
        addSummary(schemaNode, servicesTable, "module");
    }

    private static void addServiceSummary(final SchemaNode schemaNode, final Document doc) {
        Element servicesTable = doc.getElementById("service-summary").child(0).appendElement(TR);
        addSummary(schemaNode, servicesTable, "service");
    }

    private static void addRpcSummary(final SchemaNode schemaNode, final Document doc) {
        Element servicesTable = doc.getElementById("rpc-summary").child(0).appendElement(TR);
        addSummary(schemaNode, servicesTable, "rpc");
    }

    private static String getDate(final Date revisionDate) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(revisionDate);
    }

}
