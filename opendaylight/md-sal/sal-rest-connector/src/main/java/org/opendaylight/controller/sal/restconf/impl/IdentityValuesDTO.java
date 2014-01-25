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
            return predicates;
        }

        public void setPredicates(List<Predicate> predicates) {
            this.predicates = predicates;
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
        
    }
}
