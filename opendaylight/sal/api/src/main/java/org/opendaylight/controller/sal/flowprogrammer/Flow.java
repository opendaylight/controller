
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.flowprogrammer;

import java.io.Serializable;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.ActionType;
import org.opendaylight.controller.sal.action.SetDlType;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.utils.EtherTypes;

/**
 * Represent a flow: match + actions + flow specific properties
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Flow implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	@XmlElement
    private Match match;
    @XmlElement
    private List<Action> actions;
    @XmlElement
    private short priority;
    @XmlElement
    private short idleTimeout;
    @XmlElement
    private short hardTimeout;
    @XmlElement
    private long id; // unique identifier for this flow

    public Flow() {
        match = null;
        actions = null;
    }

    public Flow(Match match, List<Action> actions) {
        if (match.isIPv4() && actionsAreIPv6()) {
            try {
                throw new Exception("Conflicting Match and Action list");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            this.match = match;
            this.actions = actions;
        }
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
     * Set the Match for this flow
     * This operation will overwrite an existing Match if present
     *
     * @param match
     */
    public void setMatch(Match match) {
        this.match = match;
    }

    /**
     * Returns a copy of the actions list of this flow
     * @return
     */
    public List<Action> getActions() {
        return (actions == null) ? null : new ArrayList<Action>(actions);
    }

    /**
     * Set the actions list for this flow
     * If a list is already present, it will be
     * replaced with the passed one. During
     * addition, only the valid actions will be added
     * It is a no op if the passed actions is null
     * An empty actions is a vlaid input
     *
     * @param actions
     */
    public void setActions(List<Action> actions) {
        if (actions == null) {
            return;
        }

        this.actions = new ArrayList<Action>(actions.size());
        for (Action action : actions) {
            if (action.isValid()) {
                this.actions.add(action);
            }
        }
    }

    /**
     * Returns whether the Flow is for IPv4 or IPv6
     * Information is derived from match and actions list
     *
     * @return
     */
    public boolean isIPv6() {
        return (match.isIPv6()) ? true : actionsAreIPv6();
    }

    /**
     * Returns true if it finds at least one action which is for IPv6
     * in the list of actions for this Flow
     *
     * @return
     */
    private boolean actionsAreIPv6() {
        if (this.actions != null) {
            for (Action action : actions) {
                switch (action.getType()) {
                case SET_NW_SRC:
                    if (((SetNwSrc) action).getAddress() instanceof Inet6Address) {
                        return true;
                    }
                    break;
                case SET_NW_DST:
                    if (((SetNwDst) action).getAddress() instanceof Inet6Address) {
                        return true;
                    }
                    break;
                case SET_DL_TYPE:
                    if (((SetDlType) action).getDlType() == EtherTypes.IPv6
                            .intValue()) {
                        return true;
                    }
                    break;
                default:
                }
            }
        }
        return false;
    }

    @Override
    public Flow clone() {
        Flow cloned = null;
        try {
            cloned = (Flow) super.clone();
            cloned.match = this.getMatch();
            cloned.actions = this.getActions();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return cloned;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
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

    /**
     * Adds the specified action to the list of action of this flow
     *
     * @param action
     * @return false if the passed action is null or not valid or if it fails to add it
     */
    public boolean addAction(Action action) {
        if (action == null || !action.isValid()) {
            return false;
        }
        return actions.add(action);
    }

    public boolean removeAction(Action action) {
        if (action == null) {
            return false;
        }
        return actions.remove(action);
    }

    /**
     * remove ALL actions of type actionType from the list of actions of this flow
     *
     * @param actionType
     * @return false if an action of that type is present and it fails to remove it
     */
    public boolean removeAction(ActionType actionType) {
        Iterator<Action> actionIter = this.getActions().iterator();
        while (actionIter.hasNext()) {
            Action action = actionIter.next();
            if (action.getType() == actionType) {
                if (!this.removeAction(action))
                    return false;
            }
        }
        return true;
    }
}
