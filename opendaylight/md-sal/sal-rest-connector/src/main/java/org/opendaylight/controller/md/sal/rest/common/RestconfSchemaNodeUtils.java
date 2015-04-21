/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.common;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.common
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Feb 5, 2015
 */
public class RestconfSchemaNodeUtils {

    private RestconfSchemaNodeUtils () {
        throw new UnsupportedOperationException("Utility class");
    }

    public static DataSchemaNode findInstanceDataChildByNameAndNamespace(final DataNodeContainer container, final String name,
            @CheckForNull final URI namespace) {
        Preconditions.<URI> checkNotNull(namespace);

        final List<DataSchemaNode> potentialSchemaNodes = findInstanceDataChildrenByName(container, name);

        final Predicate<DataSchemaNode> filter = new Predicate<DataSchemaNode>() {
            @Override
            public boolean apply(final DataSchemaNode node) {
                return Objects.equal(node.getQName().getNamespace(), namespace);
            }
        };

        final Iterable<DataSchemaNode> result = Iterables.filter(potentialSchemaNodes, filter);
        return Iterables.getFirst(result, null);
    }

    public static DataSchemaNode findInstanceDataChildByName(final DataNodeContainer container, final String nodeName) {
        Preconditions.<DataNodeContainer> checkNotNull(container);
        Preconditions.<String> checkNotNull(nodeName);

        final List<DataSchemaNode> instantiatedDataNodeContainers = new ArrayList<>();
        collectInstanceDataNodeContainers(instantiatedDataNodeContainers, container, nodeName);

        if (instantiatedDataNodeContainers.size() > 1) {
            final StringBuilder errMsgBuilder = new StringBuilder("URI has bad format. Node \"");
            errMsgBuilder.append(nodeName).append("\" is added as augment from more than one module. ")
            .append("\" is added as augment from more than one module. ")
            .append("Therefore the node must have module name and it has to be in format \"moduleName:nodeName\".")
            .append("\nThe node is added as augment from modules with namespaces:\n");
            for (final DataSchemaNode potentialNodeSchema : instantiatedDataNodeContainers) {
                errMsgBuilder.append("   ").append(potentialNodeSchema.getQName().getNamespace()).append("\n");
            }
            throw new RestconfDocumentedException(errMsgBuilder.toString(), ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
        }
        RestconfValidationUtils.checkDocumentedError(( ! instantiatedDataNodeContainers.isEmpty()), ErrorType.PROTOCOL,
                ErrorTag.UNKNOWN_ELEMENT, "\"" + nodeName + "\" in URI was not found in parent data node");
        return instantiatedDataNodeContainers.get(0);
    }

    public static List<DataSchemaNode> findInstanceDataChildrenByName(final DataNodeContainer container, final String name) {
        Preconditions.<DataNodeContainer> checkNotNull(container);
        Preconditions.<String> checkNotNull(name);

        final List<DataSchemaNode> instantiatedDataNodeContainers = new ArrayList<>();
        collectInstanceDataNodeContainers(instantiatedDataNodeContainers, container, name);
        return instantiatedDataNodeContainers;
    }

    public static boolean isListOrContainer(final DataSchemaNode node) {
        return node instanceof ListSchemaNode || node instanceof ContainerSchemaNode;
    }

    public static boolean isInstantiatedDataSchema(final DataSchemaNode node) {
        return node instanceof LeafSchemaNode || node instanceof LeafListSchemaNode
                || node instanceof ContainerSchemaNode || node instanceof ListSchemaNode
                || node instanceof AnyXmlSchemaNode;
    }

    private static void collectInstanceDataNodeContainers(final List<DataSchemaNode> potentialSchemaNodes,
            final DataNodeContainer container, final String name) {

        final Predicate<DataSchemaNode> filter = new Predicate<DataSchemaNode>() {
            @Override
            public boolean apply(final DataSchemaNode node) {
                return Objects.equal(node.getQName().getLocalName(), name);
            }
        };

        final Iterable<DataSchemaNode> nodes = Iterables.filter(container.getChildNodes(), filter);

        // Can't combine this loop with the filter above because the filter is
        // lazily-applied by Iterables.filter.
        for (final DataSchemaNode potentialNode : nodes) {
            if (isInstantiatedDataSchema(potentialNode)) {
                potentialSchemaNodes.add(potentialNode);
            }
        }

        final Iterable<ChoiceSchemaNode> choiceNodes = Iterables.filter(container.getChildNodes(), ChoiceSchemaNode.class);
        final Iterable<Set<ChoiceCaseNode>> map = Iterables.transform(choiceNodes, CHOICE_FUNCTION);

        final Iterable<ChoiceCaseNode> allCases = Iterables.<ChoiceCaseNode> concat(map);
        for (final ChoiceCaseNode caze : allCases) {
            collectInstanceDataNodeContainers(potentialSchemaNodes, caze, name);
        }
    }

    private static final Function<ChoiceSchemaNode, Set<ChoiceCaseNode>> CHOICE_FUNCTION = new Function<ChoiceSchemaNode, Set<ChoiceCaseNode>>() {
        @Override
        public Set<ChoiceCaseNode> apply(final ChoiceSchemaNode node) {
            return node.getCases();
        }
    };
}
