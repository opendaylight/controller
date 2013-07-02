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
import org.opendaylight.controller.yang.parser.builder.api.Builder;
import org.opendaylight.controller.yang.parser.util.Comparators;
import org.opendaylight.controller.yang.parser.util.YangModelBuilderUtil;
import org.opendaylight.controller.yang.parser.util.YangParseException;

public final class DeviationBuilder implements Builder {
    private final int line;
    private Builder parent;
    private final DeviationImpl instance;
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    DeviationBuilder(final String targetPathStr, final int line) {
        this.line = line;
        final SchemaPath targetPath = YangModelBuilderUtil
                .parseAugmentPath(targetPathStr);
        instance = new DeviationImpl(targetPath);
    }

    @Override
    public Deviation build() {
        // UNKNOWN NODES
        List<UnknownSchemaNode> unknownNodes = new ArrayList<UnknownSchemaNode>();
        for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
            unknownNodes.add(b.build());
        }
        Collections.sort(unknownNodes, Comparators.SCHEMA_NODE_COMP);
        instance.setUnknownSchemaNodes(unknownNodes);

        return instance;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public Builder getParent() {
        return parent;
    }

    @Override
    public void setParent(final Builder parent) {
        this.parent = parent;
    }

    @Override
    public void addUnknownSchemaNode(UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
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
            throw new YangParseException(line,
                    "Unsupported type of 'deviate' statement: " + deviate);
        }
    }

    public void setReference(final String reference) {
        instance.setReference(reference);
    }

    private final class DeviationImpl implements Deviation {
        private final SchemaPath targetPath;
        private Deviate deviate;
        private String reference;
        private List<UnknownSchemaNode> unknownNodes = Collections.emptyList();

        private DeviationImpl(final SchemaPath targetPath) {
            this.targetPath = targetPath;
        }

        @Override
        public SchemaPath getTargetPath() {
            return targetPath;
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
            result = prime * result
                    + ((targetPath == null) ? 0 : targetPath.hashCode());
            result = prime * result
                    + ((deviate == null) ? 0 : deviate.hashCode());
            result = prime * result
                    + ((reference == null) ? 0 : reference.hashCode());
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
            StringBuilder sb = new StringBuilder(
                    DeviationImpl.class.getSimpleName());
            sb.append("[");
            sb.append("targetPath=" + targetPath);
            sb.append(", deviate=" + deviate);
            sb.append(", reference=" + reference);
            sb.append("]");
            return sb.toString();
        }
    }

}
