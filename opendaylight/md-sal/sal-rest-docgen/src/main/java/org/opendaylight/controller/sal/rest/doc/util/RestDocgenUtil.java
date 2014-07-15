/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.util;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

public class RestDocgenUtil {

    private RestDocgenUtil() {
    }

    private static Map<URI, Map<Date, Module>> namespaceAndRevisionToModule = new HashMap<URI, Map<Date, Module>>();

    /**
     * Resolve name for {@code node}.
     *
     * The name can contain also prefix which consists of module name followed by colon. The module prefix is presented
     * if {@code writeModuleName} parameter is set to true or if it is set to false and namespace of {@code node
     * } and its
     * parent is different. In other cases only name of {@code node} is returned.
     *
     * @return name of {@code node}
     */
    public static String resolveName(final SchemaNode node, final SchemaContext schemaContext,
            final boolean writeModuleName) {
        Iterable<QName> schemaPath = node.getPath().getPathTowardsRoot();
        Iterator<QName> it = schemaPath.iterator();
        QName nodeQName = it.next();

        QName parentQName = null;
        if (it.hasNext()) {
            parentQName = it.next();
        }
        if (isEqualNamespaceAndRevision(parentQName, nodeQName) && !writeModuleName) {
            return node.getQName().getLocalName();
        } else {
            Module module = resolveModuleNameFromNode(node, schemaContext);
            if (module != null) {
                return module.getName() + ":" + node.getQName().getLocalName();
            }
            return node.getQName().getLocalName();
        }
    }

    private static Module resolveModuleNameFromNode(final SchemaNode node, final SchemaContext schemaContext) {
        final URI namespace = node.getQName().getNamespace();
        final Date revision = node.getQName().getRevision();

        Map<Date, Module> revisionToModule = namespaceAndRevisionToModule.get(namespace);
        if (revisionToModule == null) {
            revisionToModule = new HashMap<>();
            namespaceAndRevisionToModule.put(namespace, revisionToModule);
        }
        Module module = revisionToModule.get(revision);
        if (module == null) {
            module = schemaContext.findModuleByNamespaceAndRevision(namespace, revision);
            revisionToModule.put(revision, module);
        }
        return module;
    }

    private static boolean isEqualNamespaceAndRevision(QName parentQName, QName nodeQName) {
        if (parentQName == null) {
            if (nodeQName == null) {
                return true;
            }
            return false;
        }
        return parentQName.getNamespace().equals(nodeQName.getNamespace())
                && parentQName.getRevision().equals(nodeQName.getRevision());
    }
}
