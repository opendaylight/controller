/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.serialization;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;

/**
 * Implementation of the QNameSerializationContext interface.
 *
 * @author Thomas Pantelis
 */
public class QNameSerializationContextImpl implements QNameSerializationContext {

    private final Map<Object, Integer> codeMap = new HashMap<>();
    private final List<String> codes = new ArrayList<>();

    public List<String> getCodes() {
        return codes;
    }

    @Override public int addNamespace(URI namespace) {
        int namespaceInt = getCode(namespace);

        if(namespaceInt == -1) {
            namespaceInt = addCode(namespace, namespace.toString());
        }
        return namespaceInt;
    }

    @Override public int addRevision(Date revision) {
        if(revision == null){
            return -1;
        }

        int revisionInt = getCode(revision);
        if(revisionInt == -1) {
            String formattedRevision =
                SimpleDateFormatUtil.getRevisionFormat().format(revision);
            revisionInt = addCode(revision, formattedRevision);
        }
        return revisionInt;
    }

    @Override public int addLocalName(String localName) {
        int localNameInt = getCode(localName);
        if(localNameInt == -1) {
            localNameInt = addCode(localName, localName);
        }
        return localNameInt;

    }

    private int addCode(Object code, String codeStr){
        int count = codes.size();
        codes.add(codeStr);
        codeMap.put(code, Integer.valueOf(count));
        return count;
    }

    private int getCode(Object code){
        Integer value = codeMap.get(code);
        return value == null ? -1 : value.intValue();
    }
}
