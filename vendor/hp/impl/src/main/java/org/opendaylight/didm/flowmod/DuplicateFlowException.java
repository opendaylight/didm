/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.flowmod;


public class DuplicateFlowException extends RuntimeException {

        private static final long serialVersionUID = -990164411340073190L;

        /**
         * Constructs an exception with no message and no underlying cause.
         */
        public DuplicateFlowException() {
        }

        /**
         * Constructs an exception with the specified message.
         *
         * @param message the message describing the specific nature of the error
         */
        public DuplicateFlowException(String message) {
            super(message);
        }

        /**
         * Constructs an exception with the specified message.
         *
         * @param message the message describing the specific nature of the error
         * @param cause the underlying cause of this error
         */
        public DuplicateFlowException(String message, Throwable cause) {
            super(message, cause);
        }
}

