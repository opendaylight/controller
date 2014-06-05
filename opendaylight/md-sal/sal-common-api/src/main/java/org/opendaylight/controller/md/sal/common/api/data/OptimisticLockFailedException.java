package org.opendaylight.controller.md.sal.common.api.data;

/**
*
* Failure of asynchronous transaction commit caused by failure
* of optimistic locking.
*
* This exception is raised and returned when transaction commit
* failed, because other transaction finished successfully
* and modified same data as failed transaction.
*
*  Clients may recover from this error condition by
*  retrieving current state and submitting new updated
*  transaction.
*
*/
public class OptimisticLockFailedException extends TransactionCommitFailedException {

    private static final long serialVersionUID = -3662843508421208264L;

    public OptimisticLockFailedException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public OptimisticLockFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public OptimisticLockFailedException(String message) {
        super(message);
    }

    public OptimisticLockFailedException(Throwable cause) {
        super(cause);
    }

}
