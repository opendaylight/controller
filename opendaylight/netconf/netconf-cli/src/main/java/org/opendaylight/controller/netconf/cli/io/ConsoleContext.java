package org.opendaylight.controller.netconf.cli.io;

import jline.console.completer.Completer;

public interface ConsoleContext {

    Completer getCompleter();

    String getPrompt();

}
