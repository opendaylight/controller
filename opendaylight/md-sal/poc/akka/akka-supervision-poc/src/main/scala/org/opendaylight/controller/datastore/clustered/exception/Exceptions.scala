package org.opendaylight.controller.datastore.clustered.exception

/**
 *
 * @author: syedbahm
 *          Date: 6/6/14
 *
 *
 *
 */
/**
 * This is thrown from shard to indicate the supervisor that resume action needs to be
 * taken
 */
class ShardResumeException(message: String = null, cause: Throwable = null) extends RuntimeException(message, cause)

/**
 * This is thrown from shard to indicate the supervisor that restart action needs to be
 * taken
 */
class ShardRestartException(message: String = null, cause: Throwable = null) extends Exception(message, cause)