package org.opendaylight.controller.md.sal.common.api.data;

/**
 *
 * Failed commit of asynchronous transaction
 *
 * This exception is raised and returned when transaction commit
 * failed.
 *
 */
public class TransactionCommitFailedException extends Exception {

    private static final long serialVersionUID = -6138306275373237068L;

    public TransactionCommitFailedException() {
        super();
    }

    public TransactionCommitFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public TransactionCommitFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransactionCommitFailedException(String message) {
        super(message);
    }

    public TransactionCommitFailedException(Throwable cause) {
        super(cause);
    }

}
