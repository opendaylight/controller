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

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class PathUtils {
    private static final Splitter SLASH_SPLITTER = Splitter.on('/').omitEmptyStrings();

    /**
     * Given a YangInstanceIdentifier return a serialized version of the same
     * as a String
     *
     * @param path
     * @return
     */
    public static String toString(YangInstanceIdentifier path){
        StringBuilder sb = new StringBuilder();
        Iterator<YangInstanceIdentifier.PathArgument> iterator =
            path.getPathArguments().iterator();

        while(iterator.hasNext()){
            sb.append(toString(iterator.next()));
            if(iterator.hasNext()){
                sb.append("/");
            }
        }
        return sb.toString();
    }

    /**
     * Given a YangInstanceIdentifier.PathArgument return a serialized version
     * of the same as a String
     *
     * @param pathArgument
     * @return
     */
    public static String toString(YangInstanceIdentifier.PathArgument pathArgument){
        if(pathArgument instanceof YangInstanceIdentifier.NodeIdentifier){
            return toString((YangInstanceIdentifier.NodeIdentifier) pathArgument);
        } else if(pathArgument instanceof YangInstanceIdentifier.AugmentationIdentifier){
            return toString((YangInstanceIdentifier.AugmentationIdentifier) pathArgument);
        } else if(pathArgument instanceof YangInstanceIdentifier.NodeWithValue){
            return toString((YangInstanceIdentifier.NodeWithValue) pathArgument);
        } else if(pathArgument instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates){
            return toString((YangInstanceIdentifier.NodeIdentifierWithPredicates) pathArgument);
        }

        return pathArgument.toString();
    }

    /**
     * Given a serialized string version of a YangInstanceIdentifier convert
     * to a YangInstanceIdentifier
     *
     * @param path
     * @return
     */
    public static YangInstanceIdentifier toYangInstanceIdentifier(String path){
        List<YangInstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();
        for (String segment : SLASH_SPLITTER.split(path)) {
            pathArguments.add(NodeIdentifierFactory.getArgument(segment));
        }
        return YangInstanceIdentifier.create(pathArguments);
    }

    private static String toString(YangInstanceIdentifier.NodeIdentifier pathArgument){
        return pathArgument.getNodeType().toString();
    }

    private static String toString(YangInstanceIdentifier.AugmentationIdentifier pathArgument){
        Set<QName> childNames = pathArgument.getPossibleChildNames();
        final StringBuilder sb = new StringBuilder("AugmentationIdentifier{");
        sb.append("childNames=").append(childNames).append('}');
        return sb.toString();

    }

    private static String toString(YangInstanceIdentifier.NodeWithValue pathArgument){
        return pathArgument.getNodeType().toString() + "[" + pathArgument.getValue() + "]";
    }

    private static String toString(YangInstanceIdentifier.NodeIdentifierWithPredicates pathArgument){
        return pathArgument.getNodeType().toString() + '[' + pathArgument.getKeyValues() + ']';
    }

}
