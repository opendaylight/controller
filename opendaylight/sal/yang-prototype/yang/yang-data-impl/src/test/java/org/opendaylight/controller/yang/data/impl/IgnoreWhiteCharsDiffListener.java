/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.data.impl;

import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.DifferenceListener;

/**
 * Implementatin of {@link DifferenceListener} ignoring white characters around text elements
 * @author mirehak
 *
 */
public class IgnoreWhiteCharsDiffListener implements DifferenceListener {
    
    @Override
    public void skippedComparison(org.w3c.dom.Node control,
            org.w3c.dom.Node test) {
        // do nothing                
    }

    @Override
    public int differenceFound(Difference diff) {
        
        if (diff.getId() == DifferenceConstants.TEXT_VALUE.getId()) {
            
            String control = diff.getControlNodeDetail().getValue();
            if (control != null) {
                control = control.trim();
                if (diff.getTestNodeDetail().getValue() != null
                    && control.equals(diff.getTestNodeDetail().getValue().trim())) {
                    return
                        DifferenceListener.RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
                }
            }
        }
        return RETURN_ACCEPT_DIFFERENCE;
    }
}
