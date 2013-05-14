/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.validator;

import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.opendaylight.controller.yang.parser.util.YangValidationException;

/**
 * Exposed basic yang validation.
 *
 * Every file is validated using {@link YangModelBasicValidationListener}.
 */
public final class YangModelBasicValidator {

    private final ParseTreeWalker walker;

    public YangModelBasicValidator(ParseTreeWalker walker) {
        this.walker = walker;
    }

    public YangModelBasicValidator() {
        this.walker = new ParseTreeWalker();
    }

    public void validate(List<ParseTree> trees) {
        for (int i = 0; i < trees.size(); i++) {
            try {
                final YangModelBasicValidationListener yangModelParser = new YangModelBasicValidationListener();
                walker.walk(yangModelParser, trees.get(i));
            } catch (YangValidationException e) {
                // wrap exception to add information about which file failed
                throw new YangValidationException(
                        "Yang validation failed for file" + e);
            }
        }
    }

}
