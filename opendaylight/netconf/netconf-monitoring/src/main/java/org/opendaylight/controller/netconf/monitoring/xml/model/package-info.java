/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
@XmlSchema(
        elementFormDefault = XmlNsForm.QUALIFIED,
        xmlns = {
                @XmlNs(namespaceURI = MonitoringConstants.EXTENSION_NAMESPACE, prefix = MonitoringConstants.EXTENSION_NAMESPACE_PREFIX),
                @XmlNs(namespaceURI = MonitoringConstants.NAMESPACE, prefix = "")
        },
        namespace = MonitoringConstants.NAMESPACE
)
package org.opendaylight.controller.netconf.monitoring.xml.model;

import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;