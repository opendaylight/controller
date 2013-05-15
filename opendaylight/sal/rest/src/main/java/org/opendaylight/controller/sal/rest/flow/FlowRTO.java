/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.rest.flow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.rest.action.ActionRTO;

/**
 * Represent a flow: match + actions + flow specific properties
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class FlowRTO implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    @XmlElement
    private Match match;
    @XmlElement
    private List<ActionRTO> actions;
    @XmlElement
    private short priority;
    @XmlElement
    private short idleTimeout;
    @XmlElement
    private short hardTimeout;
    @XmlElement
    private long id; // unique identifier for this flow

    public FlowRTO() {
        match = null;
        actions = null;
    }

    public FlowRTO(Match match, List<ActionRTO> actions) {
        this.match = match;
        this.actions = actions;
    }

    /**
     * Return a copy of the Match configured on this flow
     * 
     * @return
     */
    public Match getMatch() {
        return match.clone();
    }

    /**
     * Set the Match for this flow This operation will overwrite an existing
     * Match if present
     * 
     * @param match
     */
    public void setMatch(Match match) {
        this.match = match;
    }

    /**
     * Returns a copy of the actions list of this flow
     * 
     * @return
     */
    public List<ActionRTO> getActions() {
        return (actions == null) ? null : new ArrayList<ActionRTO>(actions);
    }

    /**
     * Set the actions list for this flow If a list is already present, it will
     * be replaced with the passed one. During addition, only the valid actions
     * will be added It is a no op if the passed actions is null An empty
     * actions is a vlaid input
     * 
     * @param actions
     */
    public void setActions(List<ActionRTO> actions) {
        if (actions == null) {
            return;
        }
        this.actions = new ArrayList<ActionRTO>(actions);
    }

    public FlowRTO clone() throws CloneNotSupportedException {
        FlowRTO cloned = null;

        cloned = (FlowRTO) super.clone();
        cloned.match = this.getMatch();
        cloned.actions = this.getActions();

        return cloned;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((actions == null) ? 0 : actions.hashCode());
        result = prime * result + hardTimeout;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + idleTimeout;
        result = prime * result + ((match == null) ? 0 : match.hashCode());
        result = prime * result + priority;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FlowRTO other = (FlowRTO) obj;
        if (actions == null) {
            if (other.actions != null)
                return false;
        } else if (!actions.equals(other.actions))
            return false;
        if (hardTimeout != other.hardTimeout)
            return false;
        if (id != other.id)
            return false;
        if (idleTimeout != other.idleTimeout)
            return false;
        if (match == null) {
            if (other.match != null)
                return false;
        } else if (!match.equals(other.match))
            return false;
        if (priority != other.priority)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Flow[match = " + match + ", actions = " + actions
                + ", priority = " + priority + ", id = " + id
                + ", idleTimeout = " + idleTimeout + ", hardTimeout = "
                + hardTimeout + "]";
    }

    public short getPriority() {
        return priority;
    }

    public void setPriority(short priority) {
        this.priority = priority;
    }

    public short getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(short idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public short getHardTimeout() {
        return hardTimeout;
    }

    public void setHardTimeout(short hardTimeout) {
        this.hardTimeout = hardTimeout;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
