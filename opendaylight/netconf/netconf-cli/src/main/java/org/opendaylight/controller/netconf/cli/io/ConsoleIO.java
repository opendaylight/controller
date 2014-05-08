/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.io;

import java.io.IOException;

/**
 * Definition of IO interface
 */
public interface ConsoleIO {

    String read() throws IOException;

    String read(Character mask) throws IOException;

    void write(CharSequence data) throws IOException;

    void writeLn(CharSequence data) throws IOException;

    void formatLn(String format, Object... args) throws IOException;

    void enterContext(ConsoleContext consoleContext);

    void enterRootContext(ConsoleContext consoleContext);

    void leaveContext();

    void leaveRootContext();

    void complete();

}
