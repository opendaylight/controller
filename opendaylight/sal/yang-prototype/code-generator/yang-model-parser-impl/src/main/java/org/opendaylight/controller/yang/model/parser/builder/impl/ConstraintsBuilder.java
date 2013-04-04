/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.parser.builder.api.Builder;
import org.opendaylight.controller.yang.model.util.RevisionAwareXPathImpl;

public class ConstraintsBuilder implements Builder {

    private final ConstraintDefinitionImpl instance;
    private final Set<MustDefinition> mustDefinitions;
    private String whenCondition;

    ConstraintsBuilder() {
        instance = new ConstraintDefinitionImpl();
        mustDefinitions = new HashSet<MustDefinition>();
    }

    @Override
    public ConstraintDefinition build() {
        RevisionAwareXPath whenStmt = new RevisionAwareXPathImpl(whenCondition,
                false);
        instance.setWhenCondition(whenStmt);
        instance.setMustConstraints(mustDefinitions);
        return instance;
    }

    public void setMinElements(Integer minElements) {
        instance.setMinElements(minElements);
    }

    public void setMaxElements(Integer maxElements) {
        instance.setMaxElements(maxElements);
    }

    public void addMustDefinition(String mustStr, String description,
            String reference) {
        MustDefinition must = new MustDefinitionImpl(mustStr, description,
                reference);
        mustDefinitions.add(must);
    }

    public void addWhenCondition(String whenCondition) {
        this.whenCondition = whenCondition;
    }

    public void setMandatory(boolean mandatory) {
        instance.setMandatory(mandatory);
    }

    private static class ConstraintDefinitionImpl implements
            ConstraintDefinition {

        private DataSchemaNode parent;
        private RevisionAwareXPath whenCondition;
        private Set<MustDefinition> mustConstraints;
        private boolean mandatory;
        private Integer minElements;
        private Integer maxElements;

        @Override
        public DataSchemaNode getParent() {
            return parent;
        }

        @Override
        public RevisionAwareXPath getWhenCondition() {
            return whenCondition;
        }

        private void setWhenCondition(RevisionAwareXPath whenCondition) {
            this.whenCondition = whenCondition;
        }

        @Override
        public Set<MustDefinition> getMustConstraints() {
            if (mustConstraints == null) {
                return Collections.emptySet();
            } else {
                return mustConstraints;
            }
        }

        private void setMustConstraints(Set<MustDefinition> mustConstraints) {
            if (mustConstraints != null) {
                this.mustConstraints = mustConstraints;
            }
        }

        @Override
        public boolean isMandatory() {
            return mandatory;
        }

        private void setMandatory(boolean mandatory) {
            this.mandatory = mandatory;
        }

        @Override
        public Integer getMinElements() {
            return minElements;
        }

        private void setMinElements(Integer minElements) {
            this.minElements = minElements;
        }

        @Override
        public Integer getMaxElements() {
            return maxElements;
        }

        private void setMaxElements(Integer maxElements) {
            this.maxElements = maxElements;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((parent == null) ? 0 : parent.hashCode());
            result = prime * result
                    + ((whenCondition == null) ? 0 : whenCondition.hashCode());
            result = prime
                    * result
                    + ((mustConstraints == null) ? 0 : mustConstraints
                            .hashCode());
            result = prime * result
                    + ((minElements == null) ? 0 : minElements.hashCode());
            result = prime * result
                    + ((maxElements == null) ? 0 : maxElements.hashCode());
            result = prime * result + (mandatory ? 1231 : 1237);
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
            ConstraintDefinitionImpl other = (ConstraintDefinitionImpl) obj;
            if (parent == null) {
                if (other.parent != null) {
                    return false;
                }
            } else if (!parent.equals(other.parent)) {
                return false;
            }
            if (whenCondition == null) {
                if (other.whenCondition != null) {
                    return false;
                }
            } else if (!whenCondition.equals(other.whenCondition)) {
                return false;
            }
            if (mustConstraints == null) {
                if (other.mustConstraints != null) {
                    return false;
                }
            } else if (!mustConstraints.equals(other.mustConstraints)) {
                return false;
            }
            if (mandatory != other.mandatory) {
                return false;
            }
            if (minElements == null) {
                if (other.minElements != null) {
                    return false;
                }
            } else if (!minElements.equals(other.minElements)) {
                return false;
            }
            if (maxElements == null) {
                if (other.maxElements != null) {
                    return false;
                }
            } else if (!maxElements.equals(other.maxElements)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(
                    ConstraintDefinitionImpl.class.getSimpleName());
            sb.append("[");
            sb.append("parent=" + parent);
            sb.append(", whenCondition=" + whenCondition);
            sb.append(", mustConstraints=" + mustConstraints);
            sb.append(", mandatory=" + mandatory);
            sb.append(", minElements=" + minElements);
            sb.append(", maxElements=" + maxElements);
            sb.append("]");
            return sb.toString();
        }

    }

    private static class MustDefinitionImpl implements MustDefinition {

        private final String mustStr;
        private final String description;
        private final String reference;

        private MustDefinitionImpl(String mustStr, String description,
                String reference) {
            this.mustStr = mustStr;
            this.description = description;
            this.reference = reference;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getErrorAppTag() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getErrorMessage() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getReference() {
            return reference;
        }

        @Override
        public RevisionAwareXPath getXpath() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((mustStr == null) ? 0 : mustStr.hashCode());
            result = prime * result
                    + ((description == null) ? 0 : description.hashCode());
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
            MustDefinitionImpl other = (MustDefinitionImpl) obj;
            if (mustStr == null) {
                if (other.mustStr != null) {
                    return false;
                }
            } else if (!mustStr.equals(other.mustStr)) {
                return false;
            }
            if (description == null) {
                if (other.description != null) {
                    return false;
                }
            } else if (!description.equals(other.description)) {
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
            return MustDefinitionImpl.class.getSimpleName() + "[mustStr="
                    + mustStr + "]";
        }
    }

}
