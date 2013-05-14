/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.util;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.parser.builder.impl.ModuleBuilder;
import org.opendaylight.controller.yang.parser.impl.YangParserListenerImpl;
import org.opendaylight.controller.yang.parser.util.TopologicalSort.Node;
import org.opendaylight.controller.yang.parser.util.TopologicalSort.NodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Creates a module dependency graph from provided {@link ModuleBuilder}s and
 * provides a {@link #sort()} method. It is topological sort and returns modules
 * in order in which they should be processed (e.g. if A imports B, sort returns
 * {B, A}).
 */
public final class ModuleDependencySort {

    private static final Date DEFAULT_REVISION = new Date(0);
    private static final Logger logger = LoggerFactory
            .getLogger(ModuleDependencySort.class);

    private final Map<String, Map<Date, ModuleNodeImpl>> moduleGraph;

    /**
     *
     * @param builders
     *            Source for module dependency graph.
     * @throws YangValidationException
     *             if 1. there are module:revision duplicates 2. there is
     *             imported not existing module 3. module is imported twice
     */
    public ModuleDependencySort(ModuleBuilder... builders) {
        this.moduleGraph = createModuleGraph(builders);
    }

    @VisibleForTesting
    Map<String, Map<Date, ModuleNodeImpl>> getModuleGraph() {
        return moduleGraph;
    }

    /**
     * Topological sort of module dependency graph.
     *
     * @return Sorted list of modules. Modules can be further processed in
     *         returned order.
     */
    public List<ModuleSimple> sort() {
        Set<Node> nodes = Sets.newHashSet();
        for (Map<Date, ModuleNodeImpl> map : moduleGraph.values()) {
            for (ModuleNodeImpl node : map.values()) {
                nodes.add(node);
            }
        }

        // Cast to ModuleNode from Node
        return Lists.transform(TopologicalSort.sort(nodes),
                new Function<Node, ModuleSimple>() {

                    @Override
                    public ModuleSimple apply(Node input) {
                        return (ModuleSimple) input;
                    }
                });
    }

    private Map<String, Map<Date, ModuleNodeImpl>> createModuleGraph(
            ModuleBuilder... builders) {
        Map<String, Map<Date, ModuleNodeImpl>> moduleGraph = Maps.newHashMap();

        processModules(moduleGraph, builders);
        processDependencies(moduleGraph, builders);

        return moduleGraph;
    }

    /**
     * Extract module:revision from module builders
     */
    private void processDependencies(
            Map<String, Map<Date, ModuleNodeImpl>> moduleGraph,
            ModuleBuilder... builders) {
        Map<String, Date> imported = Maps.newHashMap();

        // Create edges in graph
        for (ModuleBuilder mb : builders) {
            String fromName = mb.getName();
            Date fromRevision = mb.getRevision() == null ? DEFAULT_REVISION
                    : mb.getRevision();
            for (ModuleImport imprt : mb.getModuleImports()) {
                String toName = imprt.getModuleName();
                Date toRevision = imprt.getRevision() == null ? DEFAULT_REVISION
                        : imprt.getRevision();

                ModuleNodeImpl from = moduleGraph.get(fromName).get(
                        fromRevision);

                ModuleNodeImpl to = getModuleByNameAndRevision(moduleGraph,
                        fromName, fromRevision, toName, toRevision);

                /*
                 * Check imports: If module is imported twice with different
                 * revisions then throw exception
                 */
                if (imported.get(toName) != null
                        && !imported.get(toName).equals(toRevision))
                    ex(String
                            .format("Module:%s imported twice with different revisions:%s, %s",
                                    toName,
                                    formatRevDate(imported.get(toName)),
                                    formatRevDate(toRevision)));
                imported.put(toName, toRevision);

                from.addEdge(to);
            }
        }
    }

    /**
     * Get imported module by its name and revision from moduleGraph
     */
    private ModuleNodeImpl getModuleByNameAndRevision(
            Map<String, Map<Date, ModuleNodeImpl>> moduleGraph,
            String fromName, Date fromRevision, String toName, Date toRevision) {
        ModuleNodeImpl to = null;

        if (moduleGraph.get(toName) == null
                || !moduleGraph.get(toName).containsKey(toRevision)) {
            // If revision is not specified in import, but module exists
            // with different revisions, take first
            if (moduleGraph.get(toName) != null
                    && !moduleGraph.get(toName).isEmpty()
                    && toRevision.equals(DEFAULT_REVISION)) {
                to = moduleGraph.get(toName).values().iterator().next();
                logger.warn(String
                        .format("Import:%s:%s by module:%s:%s does not specify revision, using:%s:%s for module dependency sort",
                                toName, formatRevDate(toRevision), fromName,
                                formatRevDate(fromRevision), to.getName(),
                                formatRevDate(to.getRevision())));
            } else
                ex(String.format("Not existing module imported:%s:%s by:%s:%s",
                        toName, formatRevDate(toRevision), fromName,
                        formatRevDate(fromRevision)));
        } else {
            to = moduleGraph.get(toName).get(toRevision);
        }
        return to;
    }

    private void ex(String message) {
        throw new YangValidationException(message);
    }

    /**
     * Extract dependencies from module builders to fill dependency graph
     */
    private void processModules(
            Map<String, Map<Date, ModuleNodeImpl>> moduleGraph,
            ModuleBuilder... builders) {

        // Process nodes
        for (ModuleBuilder mb : builders) {
            String name = mb.getName();

            Date rev = mb.getRevision();
            if (rev == null)
                rev = DEFAULT_REVISION;

            if (moduleGraph.get(name) == null)
                moduleGraph.put(name, Maps.<Date, ModuleNodeImpl> newHashMap());

            if (moduleGraph.get(name).get(rev) != null)
                ex(String.format("Module:%s with revision:%s declared twice",
                        name, formatRevDate(rev)));

            moduleGraph.get(name).put(rev, new ModuleNodeImpl(name, rev));
        }
    }

    private static String formatRevDate(Date rev) {
        return rev == DEFAULT_REVISION ? "default"
                : YangParserListenerImpl.simpleDateFormat.format(rev);
    }

    /**
     * Simple representation of module. Contains name and revision.
     */
    public interface ModuleSimple {
        String getName();

        Date getRevision();
    }

    static class ModuleNodeImpl extends NodeImpl implements ModuleSimple {
        private final String name;
        private final Date revision;

        public ModuleNodeImpl(String name, Date revision) {
            this.name = name;
            this.revision = revision;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Date getRevision() {
            return revision;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result
                    + ((revision == null) ? 0 : revision.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ModuleNodeImpl other = (ModuleNodeImpl) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (revision == null) {
                if (other.revision != null)
                    return false;
            } else if (!revision.equals(other.revision))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Module [name=" + name + ", revision="
                    + formatRevDate(revision) + "]";
        }

    }

}
