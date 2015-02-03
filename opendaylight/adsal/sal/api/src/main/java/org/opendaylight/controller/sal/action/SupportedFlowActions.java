/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.opendaylight.controller.sal.core.Property;

/**
 * @file SupportedFlowActions.java
 *
 * @brief Class representing the supported flow actions
 *
 *        Describes the supported flow actions
 */

@XmlAccessorType(XmlAccessType.NONE)
@Deprecated
public class SupportedFlowActions extends Property {
    private static final long serialVersionUID = 1L;
    public static final String SupportedFlowActionsPropName = "supportedFlowActions";
    private List<Class<? extends Action>> actions;

    private SupportedFlowActions() {
        super(SupportedFlowActionsPropName);
        this.actions = new ArrayList<Class<? extends Action>>();
    }

    public SupportedFlowActions(List<Class<? extends Action>> actions) {
        super(SupportedFlowActionsPropName);
        this.actions = new ArrayList<Class<? extends Action>>(actions);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((actions == null) ? 0 : actions.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SupportedFlowActions other = (SupportedFlowActions) obj;
        if (actions == null) {
            if (other.actions != null) {
                return false;
            }
        } else if (!actions.equals(other.actions)) {
            return false;
        }
        return true;
    }

    public List<Class<? extends Action>> getActions() {
        return new ArrayList<Class<? extends Action>>(this.actions);
    }

    @XmlElement(name = "value")
    @Override
    public String getStringValue() {
        List<String> nameList = new ArrayList<String>();
        for (Class<? extends Action> clazz : actions) {
            nameList.add(clazz.getSimpleName());
        }
        Collections.sort(nameList);
        return nameList.toString();
    }

    @Override
    public Property clone() {
        return new SupportedFlowActions(this.actions);
    }

    @Override
    public String toString() {
        return this.getStringValue();
    }
}
