/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IdentityValuesDTO {

    private final List<IdentityValue> elementData = new ArrayList<>();

    public IdentityValuesDTO(String namespace, String value, String prefix) {
        elementData.add(new IdentityValue(namespace, value, prefix));
    }
    
    public IdentityValuesDTO() {
        
    }

    public void add(String namespace, String value, String prefix) {
        elementData.add(new IdentityValue(namespace, value, prefix));
    }
    
    public void add(IdentityValue identityValue) {
        elementData.add(identityValue);
    }
    

    public List<IdentityValue> getValuesWithNamespaces() {
        return Collections.unmodifiableList(elementData);
    }
    
    @Override
    public String toString() {
        return elementData.toString();
    }

    public static final class IdentityValue {

        private final String namespace;
        private final String value;
        private final String prefix;
        private List<Predicate> predicates;

        public IdentityValue(String namespace, String value, String prefix) {
            this.namespace = namespace;
            this.value = value;
            this.prefix = prefix;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getValue() {
            return value;
        }

        public String getPrefix() {
            return prefix;
        }

        public List<Predicate> getPredicates() {
            if (predicates == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(predicates);
        }

        public void setPredicates(List<Predicate> predicates) {
            this.predicates = predicates;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (namespace != null) {
                sb.append(namespace);
            }
            if (prefix != null) {
                sb.append("(").append(prefix).append(")");
            }
            if (value != null) {
                sb.append(" - ").append(value);
            }
            if (predicates != null && !predicates.isEmpty()) {
                for (Predicate predicate : predicates) {
                    sb.append("[");
                    predicate.toString();
                    sb.append("]");
                }
            }
            return sb.toString();
        }

    }
    
    public static final class Predicate {
        
        private final IdentityValue name;
        private final String value;
        
        public Predicate(IdentityValue name, String value) {
            super();
            this.name = name;
            this.value = value;
        }
        
        public IdentityValue getName() {
            return name;
        }
        
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (name != null) {
                sb.append(name.toString());
            }
            if (value != null) {
                sb.append("=").append(value);
            }
            return sb.toString();
        }
        
        public boolean isLeafList() {
            return name == null ? true : false;
        }
        
    }
}
