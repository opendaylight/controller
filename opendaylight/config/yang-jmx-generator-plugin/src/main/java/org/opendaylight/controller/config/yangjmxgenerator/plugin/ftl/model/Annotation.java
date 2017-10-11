/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.config.api.annotations.Description;
import org.opendaylight.controller.config.api.annotations.RequireInterface;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.yangtools.yang.binding.annotations.ModuleQName;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;

public class Annotation {
    final String name;
    final List<Parameter> params;

    public Annotation(final String name, final List<Parameter> params) {
        this.name = name;
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public List<Parameter> getParams() {
        return params;
    }

    public static Annotation createFromMap(final Class<?> annotationClass, final Map<String, String> parameters) {
        List<Parameter> parameterList = new ArrayList<>();
        for(Entry<String, String> entry: parameters.entrySet()) {
            parameterList.add(new Parameter(entry.getKey(), entry.getValue()));
        }
        return new Annotation(annotationClass.getCanonicalName(), parameterList);
    }

    public static Annotation createDescriptionAnnotation(final String description) {
        Preconditions.checkNotNull(description,
                "Cannot create annotation from null description");
        return new Annotation(Description.class.getCanonicalName(),
                Lists.newArrayList(new Parameter("value", q(description))));
    }

    public static Annotation createModuleQNameANnotation(final QName qName) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("namespace", q(qName.getNamespace().toString()));
        parameters.put("revision", q(qName.getRevision().map(Revision::toString).orElse(null)));
        parameters.put("name", q(qName.getLocalName()));
        return Annotation.createFromMap(ModuleQName.class, parameters);
    }

    public static Collection<Annotation> createSieAnnotations(final ServiceInterfaceEntry sie){

        String exportedClassName = sie.getExportedOsgiClassName();
        Preconditions.checkNotNull(sie.getQName(),
                "Cannot create annotation from null qname");
        Preconditions.checkNotNull(exportedClassName,
                "Cannot create annotation from null exportedClassName");
        List<Annotation> result = new ArrayList<>();
        {
            List<Parameter> params = Lists.newArrayList(new Parameter("value", q(sie.getQName().toString())));
            params.add(new Parameter("osgiRegistrationType", exportedClassName + ".class"));
            params.add(new Parameter("registerToOsgi", Boolean.toString(sie.isRegisterToOsgi())));
            params.add(new Parameter("namespace", q(sie.getQName().getNamespace().toString())));
            params.add(new Parameter("revision", q(sie.getQName().getRevision().map(Revision::toString).orElse(null))));
            params.add(new Parameter("localName", q(sie.getQName().getLocalName())));

            Annotation sieAnnotation = new Annotation(ServiceInterfaceAnnotation.class.getCanonicalName(), params);
            result.add(sieAnnotation);

        }
        {
            List<Parameter> params = new ArrayList<>();
            params.add(new Parameter("namespace", q(sie.getYangModuleQName().getNamespace().toString())));
            params.add(new Parameter("revision", q(sie.getYangModuleQName().getRevision()
                .map(Revision::toString).orElse(null))));
            params.add(new Parameter("name", q(sie.getYangModuleQName().getLocalName())));

            Annotation moduleQNameAnnotation = new Annotation(ModuleQName.class.getCanonicalName(), params);
            result.add(moduleQNameAnnotation);
        }
        return result;
    }

    public static Annotation createRequireIfcAnnotation(
            final ServiceInterfaceEntry sie) {
        String reqIfc = sie.getFullyQualifiedName() + ".class";
        return new Annotation(RequireInterface.class.getCanonicalName(),
                Lists.newArrayList(new Parameter("value", reqIfc)));
    }

    private static final String quote = "\"";

    public static String q(final String nullableDescription) {
        return nullableDescription == null ? null : quote + nullableDescription + quote;
    }

    public static class Parameter {
        private final String key, value;

        public Parameter(final String key, final String value) {
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

    @Override
    public String toString() {
        return AnnotationSerializer.toString(this);
    }
}
