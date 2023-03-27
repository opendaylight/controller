/*
 * Copyright 2015-2021 Open Networking Foundation
 * Copyright 2023 PANTHEON.tech, s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.storage.journal;

/**
 * Log exception.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class StorageException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public StorageException() {
    }

    public StorageException(final String message) {
        super(message);
    }

    public StorageException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public StorageException(final Throwable cause) {
        super(cause);
    }

    /**
     * Exception thrown when an entry being stored is too large.
     */
    public static class TooLarge extends StorageException {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        public TooLarge(final String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when storage runs out of disk space.
     */
    public static class OutOfDiskSpace extends StorageException {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        public OutOfDiskSpace(final String message) {
            super(message);
        }
    }
}
