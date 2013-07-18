/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.util.RevisionAwareXPathImpl;
import org.opendaylight.controller.yang.parser.builder.api.AbstractBuilder;
import org.opendaylight.controller.yang.parser.util.YangParseException;

public final class ConstraintsBuilder extends AbstractBuilder {
    private final ConstraintDefinitionImpl instance;
    private final Set<MustDefinition> mustDefinitions;
    private String whenCondition;
    private boolean mandatory;
    private Integer min;
    private Integer max;

    ConstraintsBuilder(final String moduleName, final int line) {
        super(moduleName, line);
        instance = new ConstraintDefinitionImpl();
        mustDefinitions = new HashSet<MustDefinition>();
    }

    @Override
    public ConstraintDefinition build() {
        RevisionAwareXPath whenStmt;
        if (whenCondition == null) {
            whenStmt = null;
        } else {
            whenStmt = new RevisionAwareXPathImpl(whenCondition, false);
        }
        instance.setWhenCondition(whenStmt);
        instance.setMustConstraints(mustDefinitions);
        instance.setMandatory(mandatory);
        instance.setMinElements(min);
        instance.setMaxElements(max);
        return instance;
    }

    @Override
    public void addUnknownNodeBuilder(UnknownSchemaNodeBuilder unknownNode) {
        throw new YangParseException(moduleName, line, "Can not add unknown node to constraints.");
    }

    @Override
    public List<UnknownSchemaNodeBuilder> getUnknownNodeBuilders() {
        return Collections.emptyList();
    }

    public Integer getMinElements() {
        return min;
    }

    public void setMinElements(Integer minElements) {
        this.min = minElements;
    }

    public Integer getMaxElements() {
        return max;
    }

    public void setMaxElements(Integer maxElements) {
        this.max = maxElements;
    }

    public Set<MustDefinition> getMustDefinitions() {
        return mustDefinitions;
    }

    public void addMustDefinition(MustDefinition must) {
        mustDefinitions.add(must);
    }

    public String getWhenCondition() {
        return whenCondition;
    }

    public void addWhenCondition(String whenCondition) {
        this.whenCondition = whenCondition;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    private final class ConstraintDefinitionImpl implements ConstraintDefinition {
        private RevisionAwareXPath whenCondition;
        private Set<MustDefinition> mustConstraints;
        private boolean mandatory;
        private Integer minElements;
        private Integer maxElements;

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
            result = prime * result + ((whenCondition == null) ? 0 : whenCondition.hashCode());
            result = prime * result + ((mustConstraints == null) ? 0 : mustConstraints.hashCode());
            result = prime * result + ((minElements == null) ? 0 : minElements.hashCode());
            result = prime * result + ((maxElements == null) ? 0 : maxElements.hashCode());
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
            StringBuilder sb = new StringBuilder(ConstraintDefinitionImpl.class.getSimpleName());
            sb.append("[");
            sb.append("whenCondition=" + whenCondition);
            sb.append(", mustConstraints=" + mustConstraints);
            sb.append(", mandatory=" + mandatory);
            sb.append(", minElements=" + minElements);
            sb.append(", maxElements=" + maxElements);
            sb.append("]");
            return sb.toString();
        }
    }

}
