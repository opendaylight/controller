package org.opendaylight.controller.netconf.cli.reader;

public class ReadingException extends Exception {

    private static final long serialVersionUID = -298382323286156591L;

    public ReadingException(final String msg, final Exception e) {
        super(msg, e);
    }

    public ReadingException(final String msg) {
        super(msg);
    }

    public static class IncorrectValueException extends ReadingException {

        private static final long serialVersionUID = 164168437058431592L;

        public IncorrectValueException(final String msg) {
            super(msg);
        }

    }
}
