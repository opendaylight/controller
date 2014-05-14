/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

/*package*/ class CompareExpression implements Expression {

    public static enum OP { RE, EQ, NE, GT, GE, LT, LE };

    private final OP _operation;
    private final String _selector;
    private final String _arg;

    public CompareExpression(OP op, String selector, String arg) {
        _operation = op;
        _selector = selector;
        _arg = unQuote(arg);
    }


    public OP getOperator() {
        return _operation;
    }

    public String getSelector() {
        return _selector;
    }

    public String getArgument() {
        return _arg;
    }

    @Override
    public boolean accept(Visitor visitor) throws QueryException {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "[" + _selector + " " + _operation + " " + _arg + "]";
    }

    private static String unQuote(String s) {
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length()-1);
        } else if (s.startsWith("\'") && s.endsWith("\'")) {
            s = s.substring(1, s.length()-1);
        }
        return s;
    }

}
