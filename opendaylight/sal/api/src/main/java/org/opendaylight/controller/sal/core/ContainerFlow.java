
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.SetDlType;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.action.SetTpDst;
import org.opendaylight.controller.sal.action.SetTpSrc;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;

/**
 * Express a container flow
 *
 *
 *
 */
public class ContainerFlow implements Serializable {
    private static final long serialVersionUID = 1L;
    private Match match;

    public ContainerFlow(Match match) {
        this.match = match;
    }

    /**
     * Returns a copy of the Match defined by this Container Flow
     *
     * @return Match
     */
    public Match getMatch() {
        return match.clone();
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
        return "Container Flow [" + match.toString() + "]";
    }

    /**
     * Returns whether the specified flow is allowed
     *
     * @return true if the flow is allowed, false otherwise
     */
    public boolean allowsFlow(Flow flow) {
        Match target = flow.getMatch();

        // Check if flow's match is allowed
        if (!this.allowsMatch(target)) {
            return false;
        }

        // Now check if the flow's actions are not allowed
        // Create a Match which summarizes the list of actions
        if (flow.getActions() == null) {
            return true;
        }
        Match actionMatch = new Match();
        for (Action action : flow.getActions()) {
            switch (action.getType()) {
            case SET_DL_TYPE:
                actionMatch.setField(MatchType.DL_TYPE,
                        ((Integer) ((SetDlType) action).getDlType())
                                .shortValue());
                break;
            case SET_NW_SRC:
                actionMatch.setField(MatchType.NW_SRC, ((SetNwSrc) action)
                        .getAddress());
                break;
            case SET_NW_DST:
                actionMatch.setField(MatchType.NW_DST, ((SetNwDst) action)
                        .getAddress());
                break;
            case SET_TP_SRC:
                actionMatch.setField(MatchType.TP_SRC,
                        ((Integer) ((SetTpSrc) action).getPort()).shortValue());
                break;
            case SET_TP_DST:
                actionMatch.setField(MatchType.TP_DST,
                        ((Integer) ((SetTpDst) action).getPort()).shortValue());
                break;
            default:
                // This action cannot conflict
            }
        }

        return this.allowsMatch(actionMatch);
    }

    /**
     * Returns whether the specified match is allowed
     *
     * @param match	the match to test
     * @return true if the match is allowed, false otherwise
     */
    public boolean allowsMatch(Match target) {
        return !target.conflictWithFilter(this.match);
    }
}
