/**
 * Copyright 2026 Ignite Realtime Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smack;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;

import org.igniterealtime.smack.inttest.TestNotPossibleException;

/**
 * Shared guard checks for tests that depend on XEP-0388: Extensible SASL Profile (SASL2), which in turn depends on
 * TLS having been negotiated on the connection (as SASL2 is only offered by a compliant server after TLS is in
 * place). A service that simply does not support SASL2 is indistinguishable, from a bare 'feature is absent' check,
 * from a test run that has TLS turned off through configuration. This class tells the two apart, so that a test run
 * with TLS disabled reports an actionable reason instead of the misleading "not supported by service".
 */
public final class Sasl2TestUtils {

    private Sasl2TestUtils() {
    }

    /**
     * Asserts that the given, already connected, connection has negotiated SASL2, throwing a
     * {@link TestNotPossibleException} with an actionable explanation otherwise.
     */
    public static Sasl2Feature requireSasl2(AbstractXMPPConnection connection) throws TestNotPossibleException {
        return requireSasl2(connection, null);
    }

    /**
     * As {@link #requireSasl2(AbstractXMPPConnection)}, additionally requiring that a specific SASL mechanism is
     * offered through SASL2.
     */
    public static Sasl2Feature requireSasl2(AbstractXMPPConnection connection, String requiredMechanism) throws TestNotPossibleException {
        final Sasl2Feature sasl2Feature = connection.getFeature(Sasl2Feature.class);
        if (sasl2Feature == null) {
            if (connection.getConfiguration().getSecurityMode() == SecurityMode.disabled) {
                throw new TestNotPossibleException(
                    "XEP-0388: Extensible SASL Profile (SASL2) requires TLS to have been negotiated first, but this "
                  + "test run has TLS disabled. To enable this (and other TLS-dependent) tests, pass "
                  + "'-Dsinttest.securityMode=required' (or 'ifpossible') when invoking the test runner directly, or "
                  + "'--securityMode=required' when using the container/entrypoint.sh. If the server under test uses "
                  + "a self-signed certificate, also add '-Dsinttest.acceptAllCertificates=true' (or "
                  + "'--acceptAllCertificates').");
            }
            throw new TestNotPossibleException("XEP-0388: Extensible SASL Profile (SASL2) is not supported by service");
        }

        if (requiredMechanism != null && !sasl2Feature.getMechanisms().contains(requiredMechanism)) {
            throw new TestNotPossibleException("XEP-0388: Extensible SASL Profile (SASL2) is supported, but the '"
                + requiredMechanism + "' mechanism is not offered by service");
        }

        return sasl2Feature;
    }
}
