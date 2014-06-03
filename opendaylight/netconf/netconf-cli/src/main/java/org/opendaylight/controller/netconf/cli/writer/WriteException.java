package org.opendaylight.controller.netconf.cli.writer;

public class WriteException extends Exception {

    private static final long serialVersionUID = 8401242676753560336L;

    public WriteException(final String msg, final Exception e) {
        super(msg, e);
    }

    public WriteException(final String msg) {
        super(msg);
    }

    public static class IncorrectNumberOfNodes extends WriteException {
        private static final long serialVersionUID = 8910285140705622920L;

        public IncorrectNumberOfNodes(final String msg) {
            super(msg);
        }
    }
}
