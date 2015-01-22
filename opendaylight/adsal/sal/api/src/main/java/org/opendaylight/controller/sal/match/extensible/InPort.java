package org.opendaylight.controller.sal.match.extensible;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.NodeConnector;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class InPort extends MatchField<NodeConnector> {
    private static final long serialVersionUID = 1L;
    public static final String TYPE = "IN_PORT";
    private NodeConnector port;

    /**
     * Creates a Match field for the input port
     *
     * @param port
     *            the input port
     */
    public InPort(NodeConnector port) {
        super(TYPE);
        this.port = port;
    }

    // To satisfy JAXB
    private InPort() {
        super(TYPE);
    }

    @Override
    public NodeConnector getValue() {
        return port;
    }

    @Override
    @XmlElement(name = "value")
    protected String getValueString() {
        return port.toString();
    }

    @Override
    public NodeConnector getMask() {
        return null;
    }

    @Override
    protected String getMaskString() {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean hasReverse() {
        return false;
    }

    @Override
    public InPort getReverse() {
        return this.clone();
    }

    @Override
    public InPort clone() {
        return new InPort(port);
    }

    @Override
    public boolean isV6() {
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((port == null) ? 0 : port.hashCode());
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
        if (!(obj instanceof InPort)) {
            return false;
        }
        InPort other = (InPort) obj;
        if (port == null) {
            if (other.port != null) {
                return false;
            }
        } else if (!port.equals(other.port)) {
            return false;
        }
        return true;
    }
}