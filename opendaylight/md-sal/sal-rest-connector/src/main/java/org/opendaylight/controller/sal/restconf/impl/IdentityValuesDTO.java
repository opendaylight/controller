package org.opendaylight.controller.sal.restconf.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IdentityValuesDTO {

    private final List<IdentityValue> elementData = new ArrayList<>();

    public IdentityValuesDTO(String namespace, String value, String prefix) {
        elementData.add(new IdentityValue(namespace, value, prefix));
    }

    public void add(String namespace, String value, String prefix) {
        elementData.add(new IdentityValue(namespace, value, prefix));
    }

    public List<IdentityValue> getValuesWithNamespaces() {
        return Collections.unmodifiableList(elementData);
    }

    public static final class IdentityValue {

        private String namespace;
        private String value;
        private String prefix;

        public IdentityValue(String namespace, String value, String prefix) {
            this.namespace = namespace;
            this.value = value;
            this.prefix = prefix;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

    }
}
