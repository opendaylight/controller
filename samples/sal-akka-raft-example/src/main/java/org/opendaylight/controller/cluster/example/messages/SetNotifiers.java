/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.example.messages;

import java.util.List;

/**
 * Created by kramesha on 11/18/14.
 */
public class SetNotifiers {
    private List<String> notifierList;

    public SetNotifiers(List<String> notifierList) {
        this.notifierList = notifierList;
    }

    public List<String> getNotifierList() {
        return notifierList;
    }
}
