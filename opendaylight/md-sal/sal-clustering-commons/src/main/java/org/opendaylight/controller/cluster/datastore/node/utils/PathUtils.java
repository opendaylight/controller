/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

public class PathUtils {
    public static String getParentPath(String currentElementPath){
        StringBuilder parentPath = new StringBuilder();

        if(currentElementPath != null){
            String[] parentPaths = currentElementPath.split("/");
            if(parentPaths.length > 2){
                for(int i=0;i<parentPaths.length-1;i++){
                    if(parentPaths[i].length() > 0){
                        parentPath.append("/");
                        parentPath.append(parentPaths[i]);
                    }
                }
            }
        }
        return parentPath.toString();
    }
}
