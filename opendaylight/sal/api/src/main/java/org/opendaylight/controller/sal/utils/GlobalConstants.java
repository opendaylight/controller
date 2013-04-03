
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

/**
 * Global Constants
 *
 */
public enum GlobalConstants {
    DEFAULT("default"), 
    CONTAINERMANAGER("containermanager"), 
    CONTAINERNAME("name"), 
    STATICVLAN("staticvlan"), 
    CLUSTERINGSERVICES("clusteringservices"), 
    STARTUPHOME("configuration/startup/");

    private GlobalConstants(String name) {
        this.name = name;
    }

    private String name;

    public String toString() {
        return name;
    }
}