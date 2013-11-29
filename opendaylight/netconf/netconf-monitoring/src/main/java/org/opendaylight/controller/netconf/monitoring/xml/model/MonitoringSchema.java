/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.monitoring.xml.model;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import java.util.Collection;

final class MonitoringSchema {

    private final Schema schema;

    public MonitoringSchema(Schema schema) {
        this.schema = schema;
    }

    @XmlElement(name = "identifier")
    public String getIdentifier() {
        return schema.getIdentifier();
    }

    @XmlElement(name = "namespace")
    public String getNamespace() {
        return schema.getNamespace().getValue().toString();
    }

    @XmlElement(name = "location")
    public Collection<String> getLocation() {
        return Collections2.transform(schema.getLocation(), new Function<Schema.Location, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Schema.Location input) {
                return input.getEnumeration().toString();
            }
        });
    }

    @XmlElement(name = "version")
    public String getVersion() {
        return schema.getVersion();
    }

    @XmlElement(name = "format")
    public String getFormat() {
        Preconditions.checkState(schema.getFormat() == Yang.class, "Only yang format permitted, but was %s", schema.getFormat());
        return "yang";
    }
}
