/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import org.opendaylight.controller.yang.model.api.Deviation;
import org.opendaylight.controller.yang.model.api.Deviation.Deviate;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.parser.builder.api.Builder;
import org.opendaylight.controller.yang.model.parser.util.YangModelBuilderUtil;

public class DeviationBuilder implements Builder {

    private final DeviationImpl instance;

    DeviationBuilder(String targetPathStr) {
        SchemaPath targetPath = YangModelBuilderUtil
                .parseAugmentPath(targetPathStr);
        instance = new DeviationImpl(targetPath);
    }

    @Override
    public Deviation build() {
        return instance;
    }

    public void setDeviate(String deviate) {
        if (deviate.equals("not-supported")) {
            instance.setDeviate(Deviate.NOT_SUPPORTED);
        } else if (deviate.equals("add")) {
            instance.setDeviate(Deviate.ADD);
        } else if (deviate.equals("replace")) {
            instance.setDeviate(Deviate.REPLACE);
        } else if (deviate.equals("delete")) {
            instance.setDeviate(Deviate.DELETE);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported type of 'deviate' statement: " + deviate);
        }
    }

    public void setReference(String reference) {
        instance.setReference(reference);
    }

    private static class DeviationImpl implements Deviation {

        private SchemaPath targetPath;
        private Deviate deviate;
        private String reference;

        private DeviationImpl(SchemaPath targetPath) {
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

        private void setDeviate(Deviate deviate) {
            this.deviate = deviate;
        }

        @Override
        public String getReference() {
            return reference;
        }

        private void setReference(String reference) {
            this.reference = reference;
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
            StringBuilder sb = new StringBuilder(DeviationImpl.class.getSimpleName());
            sb.append("[");
            sb.append("targetPath="+ targetPath);
            sb.append(", deviate="+ deviate);
            sb.append(", reference="+ reference);
            sb.append("]");
            return sb.toString();
        }
    }

}
