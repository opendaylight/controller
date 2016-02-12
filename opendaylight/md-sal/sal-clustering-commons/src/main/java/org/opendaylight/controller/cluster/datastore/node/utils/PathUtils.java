/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;

public class PathUtils {
    private static final Splitter SLASH_SPLITTER = Splitter.on('/').omitEmptyStrings();

    /**
     * Given a YangInstanceIdentifier return a serialized version of the same
     * as a String
     *
     * @param path
     * @return
     */
    public static String toString(YangInstanceIdentifier path) {
        final Iterator<PathArgument> it = path.getPathArguments().iterator();
        if (!it.hasNext()) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        for (;;) {
            sb.append(toString(it.next()));
            if (!it.hasNext()) {
                break;
            }
            sb.append('/');
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
    public static String toString(PathArgument pathArgument){
        if(pathArgument instanceof NodeIdentifier){
            return toString((NodeIdentifier) pathArgument);
        } else if(pathArgument instanceof AugmentationIdentifier){
            return toString((AugmentationIdentifier) pathArgument);
        } else if(pathArgument instanceof NodeWithValue){
            return toString((NodeWithValue<?>) pathArgument);
        } else if(pathArgument instanceof NodeIdentifierWithPredicates){
            return toString((NodeIdentifierWithPredicates) pathArgument);
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
        List<PathArgument> pathArguments = new ArrayList<>();
        for (String segment : SLASH_SPLITTER.split(path)) {
            pathArguments.add(NodeIdentifierFactory.getArgument(segment));
        }
        return YangInstanceIdentifier.create(pathArguments);
    }

    private static String toString(NodeIdentifier pathArgument){
        return pathArgument.getNodeType().toString();
    }

    private static String toString(AugmentationIdentifier pathArgument){
        Set<QName> childNames = pathArgument.getPossibleChildNames();
        final StringBuilder sb = new StringBuilder("AugmentationIdentifier{");
        sb.append("childNames=").append(childNames).append('}');
        return sb.toString();

    }

    private static String toString(NodeWithValue<?> pathArgument) {
        return pathArgument.getNodeType().toString() + "[" + pathArgument.getValue() + "]";
    }

    private static String toString(NodeIdentifierWithPredicates pathArgument){
        return pathArgument.getNodeType().toString() + '[' + pathArgument.getKeyValues() + ']';
    }

}
