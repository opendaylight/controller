/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector.ops.get;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

public class FilterSchema {

    private FilterNode root;

    protected FilterSchema() {
    }

    public FilterSchema(final XmlElement element) {
        root = createFilteringSchema(element);
    }

    protected FilterNode createFilteringSchema(XmlElement element) {

        if (element.getChildElements().size() == 0) {
            return new FilterNode(element.getName());
        } else {
            return new FilterNode(element);
        }
    }

    public YangInstanceIdentifier getReadPointFromSchema(final DataSchemaNode schemaNode) {
        return root.getReadPointFromSchema(YangInstanceIdentifier.builder(), schemaNode);
    }

    private class FilterNode extends FilterSchema {

        private List<FilterNode> children;

        private final String name;

        public String getName() {
            return name;
        }

        public FilterNode(final String name) {
            this.name = name;
            children = new ArrayList<>();

        }

        private FilterNode(final XmlElement element) {
            this(element.getName());

            for (XmlElement node : element.getChildElements()) {
                children.add(createFilteringSchema(node));
            }
        }

        public YangInstanceIdentifier getReadPointFromSchema(final InstanceIdentifierBuilder pathBuilder, final DataSchemaNode schemaNode) {
            pathBuilder.node(schemaNode.getQName());
            if(children.size() == 1 || allChildrenListItems(schemaNode)) {
                if (schemaNode instanceof ContainerSchemaNode) {
                    final DataSchemaNode childSchemaNode = ((ContainerSchemaNode) schemaNode).getDataChildByName(children.get(0).getName());
                    return children.get(0).getReadPointFromSchema(pathBuilder, childSchemaNode);
                }
            }
            return pathBuilder.build();
        }

        public boolean allChildrenListItems(final DataSchemaNode schemaNode) {
            if (children.isEmpty() || !(schemaNode instanceof ContainerSchemaNode)) {
                return false;
            }
            String value = children.get(0).getName();
            //if child schema node is not list node we dont care if children are the same
            if (!(((ContainerSchemaNode) schemaNode).getDataChildByName(value) instanceof ListSchemaNode)) {
                return false;
            }
            //if children are not equal they cant be a part of the same list - read above that node
            for (int i = 1; i < children.size(); i++) {
                if (!value.equals(children.get(i).getName())) {
                    return false;
                }
            }
            return true;
        }

    }

}
