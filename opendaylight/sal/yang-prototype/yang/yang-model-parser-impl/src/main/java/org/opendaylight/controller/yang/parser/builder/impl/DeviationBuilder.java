/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.model.api.Deviation;
import org.opendaylight.controller.yang.model.api.Deviation.Deviate;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.parser.builder.api.AbstractBuilder;
import org.opendaylight.controller.yang.parser.util.Comparators;
import org.opendaylight.controller.yang.parser.util.ParserListenerUtils;
import org.opendaylight.controller.yang.parser.util.YangParseException;

public final class DeviationBuilder extends AbstractBuilder {
    private final String targetPathStr;
    private boolean isBuilt;
    private final DeviationImpl instance;

    private SchemaPath targetPath;
    private String reference;
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    DeviationBuilder(final String moduleName, final int line, final String targetPathStr) {
        super(moduleName, line);
        if (!targetPathStr.startsWith("/")) {
            throw new YangParseException(moduleName, line,
                    "Deviation argument string must be an absolute schema node identifier.");
        }
        this.targetPathStr = targetPathStr;
        this.targetPath = ParserListenerUtils.parseAugmentPath(targetPathStr);
        instance = new DeviationImpl();
    }

    @Override
    public Deviation build() {
        if (targetPath == null) {
            throw new YangParseException(moduleName, line, "Unresolved deviation target");
        }

        if (!isBuilt) {
            instance.setTargetPath(targetPath);
            instance.setReference(reference);

            // UNKNOWN NODES
            List<UnknownSchemaNode> unknownNodes = new ArrayList<UnknownSchemaNode>();
            for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
                unknownNodes.add(b.build());
            }
            Collections.sort(unknownNodes, Comparators.SCHEMA_NODE_COMP);
            instance.setUnknownSchemaNodes(unknownNodes);

            isBuilt = true;
        }

        return instance;
    }

    public SchemaPath getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(final SchemaPath targetPath) {
        this.targetPath = targetPath;
    }

    public void setDeviate(final String deviate) {
        if ("not-supported".equals(deviate)) {
            instance.setDeviate(Deviate.NOT_SUPPORTED);
        } else if ("add".equals(deviate)) {
            instance.setDeviate(Deviate.ADD);
        } else if ("replace".equals(deviate)) {
            instance.setDeviate(Deviate.REPLACE);
        } else if ("delete".equals(deviate)) {
            instance.setDeviate(Deviate.DELETE);
        } else {
            throw new YangParseException(moduleName, line, "Unsupported type of 'deviate' statement: " + deviate);
        }
    }

    public void setReference(final String reference) {
        this.reference = reference;
    }

    @Override
    public String toString() {
        return "deviation " + targetPathStr;
    }

    private final class DeviationImpl implements Deviation {
        private SchemaPath targetPath;
        private Deviate deviate;
        private String reference;
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();

        private DeviationImpl() {
        }

        @Override
        public SchemaPath getTargetPath() {
            return targetPath;
        }

        private void setTargetPath(final SchemaPath targetPath) {
            this.targetPath = targetPath;
        }

        @Override
        public Deviate getDeviate() {
            return deviate;
        }

        private void setDeviate(final Deviate deviate) {
            this.deviate = deviate;
        }

        @Override
        public String getReference() {
            return reference;
        }

        private void setReference(final String reference) {
            this.reference = reference;
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return unknownNodes;
        }

        private void setUnknownSchemaNodes(List<UnknownSchemaNode> unknownSchemaNodes) {
            if (unknownSchemaNodes != null) {
                this.unknownNodes = unknownSchemaNodes;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((targetPath == null) ? 0 : targetPath.hashCode());
            result = prime * result + ((deviate == null) ? 0 : deviate.hashCode());
            result = prime * result + ((reference == null) ? 0 : reference.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            DeviationImpl other = (DeviationImpl) obj;
            if (targetPath == null) {
                if (other.targetPath != null) {
                    return false;
                }
            } else if (!targetPath.equals(other.targetPath)) {
                return false;
            }
            if (deviate == null) {
                if (other.deviate != null) {
                    return false;
                }
            } else if (!deviate.equals(other.deviate)) {
                return false;
            }
            if (reference == null) {
                if (other.reference != null) {
                    return false;
                }
            } else if (!reference.equals(other.reference)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(DeviationImpl.class.getSimpleName());
            sb.append("[");
            sb.append("targetPath=" + targetPath);
            sb.append(", deviate=" + deviate);
            sb.append(", reference=" + reference);
            sb.append("]");
            return sb.toString();
        }
    }

}
