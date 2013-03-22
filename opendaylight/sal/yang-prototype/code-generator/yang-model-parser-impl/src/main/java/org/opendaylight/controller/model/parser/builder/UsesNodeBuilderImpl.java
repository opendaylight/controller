/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.builder;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.model.parser.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.model.parser.api.Builder;
import org.opendaylight.controller.model.parser.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.UsesNode;

public class UsesNodeBuilderImpl implements UsesNodeBuilder, Builder {

    private final String groupingPathStr;

    UsesNodeBuilderImpl(String groupingPathStr) {
        this.groupingPathStr = groupingPathStr;
    }

    @Override
    public UsesNode build() {
        SchemaPath groupingPath = parseUsesPath(groupingPathStr);
        final UsesNodeImpl instance = new UsesNodeImpl(groupingPath);
        return instance;
    }

    public void addAugment(AugmentationSchemaBuilder augmentBuilder) {
        // TODO:
    }

    private SchemaPath parseUsesPath(String augmentPath) {
        String[] splittedPath = augmentPath.split("/");
        List<QName> path = new ArrayList<QName>();
        QName name;
        for (String pathElement : splittedPath) {
            String[] splittedElement = pathElement.split(":");
            if (splittedElement.length == 1) {
                name = new QName(null, null, null, splittedElement[0]);
            } else {
                name = new QName(null, null, splittedElement[0],
                        splittedElement[1]);
            }
            path.add(name);
        }
        // TODO: absolute vs relative?
        return new SchemaPath(path, false);
    }

    private static class UsesNodeImpl implements UsesNode {

        private final SchemaPath groupingPath;

        private UsesNodeImpl(SchemaPath groupingPath) {
            this.groupingPath = groupingPath;
        }

        @Override
        public SchemaPath getGroupingPath() {
            return groupingPath;
        }

        @Override
        public String toString() {
            return UsesNodeImpl.class.getSimpleName() + "[groupingPath="
                    + groupingPath + "]";
        }

    }

}
