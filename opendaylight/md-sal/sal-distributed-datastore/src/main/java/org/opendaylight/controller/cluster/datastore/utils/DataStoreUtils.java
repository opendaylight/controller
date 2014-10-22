/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

public class DataStoreUtils {

    /**
     * Checks if the two paths contain the same ip and port. if yes, they co-exist
     * Remote paths are of the format: akka.tcp://system@127.0.0.1:2550/
     * @param path1 - remote path
     * @param path2 - remote path
     * @return
     */
    public static boolean doPathsCoExist(String path1, String path2) {
        if (path1 == null || path2 == null) {
            return false;
        }

        int atIndex1 = path1.indexOf("@");
        int atIndex2 = path2.indexOf("@");

        if (atIndex1 == -1 || atIndex2 == -1) {
            return false;
        }

        int slashIndex1 = path1.indexOf("/", atIndex1);
        int slashIndex2 = path2.indexOf("/", atIndex2);

        if (slashIndex1 == -1 || slashIndex2 == -1) {
            return false;
        }

        String hostPort1 = path1.substring(atIndex1, slashIndex1);
        String hostPort2 = path2.substring(atIndex2, slashIndex2);

        return hostPort1.equals(hostPort2);
    }

}
