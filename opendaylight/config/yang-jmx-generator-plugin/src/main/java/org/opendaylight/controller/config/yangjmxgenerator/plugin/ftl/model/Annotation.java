/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import java.util.List;

import org.opendaylight.controller.config.api.annotations.Description;
import org.opendaylight.controller.config.api.annotations.RequireInterface;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.yangtools.yang.common.QName;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class Annotation {
    final String name;
    final List<Parameter> params;

    public Annotation(String name, List<Parameter> params) {
        this.name = name;
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public List<Parameter> getParams() {
        return params;
    }

    public static Annotation createDescriptionAnnotation(String description) {
        Preconditions.checkNotNull(description,
                "Cannot create annotation from null description");
        return new Annotation(Description.class.getCanonicalName(),
                Lists.newArrayList(new Parameter("value", q(description))));
    }

    public static Annotation createSieAnnotation(QName qname,
            String exportedClassName) {
        Preconditions.checkNotNull(qname,
                "Cannot create annotation from null qname");
        Preconditions.checkNotNull(exportedClassName,
                "Cannot create annotation from null exportedClassName");

        List<Parameter> params = Lists.newArrayList(new Parameter("value",
                q(qname.getLocalName())));
        params.add(new Parameter("osgiRegistrationType", exportedClassName
                + ".class"));
        return new Annotation(
                ServiceInterfaceAnnotation.class.getCanonicalName(), params);
    }

    public static Annotation createRequireIfcAnnotation(
            ServiceInterfaceEntry sie) {
        String reqIfc = sie.getFullyQualifiedName() + ".class";
        return new Annotation(RequireInterface.class.getCanonicalName(),
                Lists.newArrayList(new Parameter("value", reqIfc)));
    }

    private static final String quote = "\"";

    private static String q(String nullableDescription) {
        return nullableDescription == null ? null : quote + nullableDescription
                + quote;
    }

    public static class Parameter {
        private final String key, value;

        public Parameter(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

}
