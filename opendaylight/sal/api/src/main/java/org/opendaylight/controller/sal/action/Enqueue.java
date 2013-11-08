package org.opendaylight.controller.sal.action;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.NodeConnector;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Enqueue extends Action {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private NodeConnector port;

    /* Dummy constructor for JAXB */
    @SuppressWarnings("unused")
    private Enqueue() {
    }

    public Enqueue(NodeConnector port) {
        type = ActionType.ENQUEUE;
        this.port = port;
    }

    public NodeConnector getPort() {
        return port;
    }
}
