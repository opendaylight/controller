@XmlSchema(
        elementFormDefault = XmlNsForm.QUALIFIED,
//        xmlns = {
//                @XmlNs(namespaceURI = MonitoringConstants.NAMESPACE, prefix = "")
//        }
        namespace = MonitoringConstants.NAMESPACE
)
package org.opendaylight.controller.netconf.monitoring.xml.model;

import org.opendaylight.controller.netconf.monitoring.MonitoringConstants;

import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;