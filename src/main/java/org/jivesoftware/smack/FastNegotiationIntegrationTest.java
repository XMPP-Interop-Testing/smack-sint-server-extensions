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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.fast.FastModule;
import org.jivesoftware.smack.fast.FastModuleDescriptor;
import org.jivesoftware.smack.fast.FastToken;
import org.jivesoftware.smack.fast.element.FastElements;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Authentication.Sasl2AuthenticationResult;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Module;
import org.jivesoftware.smack.sasl.sasl2.Sasl2ModuleDescriptor;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

/**
 * Tests that verify the behavior of a server around the XEP-0484: Fast Authentication Streamlining Tokens (FAST)
 * token lifecycle, driven through Smack's normal connection/module API (unlike {@link FastLowLevelIntegrationTest},
 * these do not need to provoke behavior Smack's own client-side code actively guards against, so there is no need
 * to hand-construct the SASL-HT exchange).
 */
@SpecificationReference(document = "XEP-0484", version = "0.2.0")
public class FastNegotiationIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    @SuppressWarnings("this-escape")
    public FastNegotiationIntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            FastTestUtils.requireFast(connection, "HT-SHA-256-NONE");
        } finally {
            connection.disconnect();
        }
    }

    private static Sasl2AuthenticationResult authenticationResultOf(ModularXmppClientToServerConnection connection) {
        final Sasl2Module sasl2Module = connection.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
        assertNotNull(sasl2Module, "Sasl2Module should be present on connection");
        final Sasl2AuthenticationResult result = sasl2Module.getSasl2AuthenticationResult();
        assertNotNull(result, "Sasl2AuthenticationResult should be set after successful SASL2 authentication");
        return result;
    }

    @SmackIntegrationTest(section = "Server provides token to client", quote =
        "The <token/> element MUST possess the following attributes: 'token', 'expiry'.")
    public void testTokenElementHasRequiredAttributes() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection(builder -> {
            final ModularXmppClientToServerConnectionConfiguration.Builder modularBuilder =
                (ModularXmppClientToServerConnectionConfiguration.Builder) builder;
            modularBuilder.with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .setAutoRequestToken(true)
                .buildModule();
            // Restrict to PLAIN for the initial, password-based leg: SCRAM (any variant) currently fails with
            // 'malformed-request' in this environment (a local Smack SCRAM regression, unrelated to FAST).
            modularBuilder.addEnabledSaslMechanism("PLAIN");
            modularBuilder.addEnabledSaslMechanism("HT-SHA-256-NONE");
        });
        try {
            connection.connect();
            connection.login();

            final FastElements.Token token = authenticationResultOf(connection).getSuccessExtension(FastElements.Token.class);
            assertNotNull(token, "Expected the '<success/>' element to contain a '<token/>' element, since a token was requested.");
            assertNotNull(token.getToken(), "Expected the '<token/>' element to have a 'token' attribute.");
            assertNotNull(token.getExpiry(), "Expected the '<token/>' element to have an 'expiry' attribute.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "Client requests token invalidation", quote =
        "Upon successful authentication with the 'invalidate' attribute set, the server MUST immediately invalidate "
      + "the token and prevent its use for future authentication attempts. The server MUST NOT include a new token "
      + "in the response (even if the token was due for rotation), unless the client also included a FAST "
      + "<request-token/> element in its authentication request.")
    public void testInvalidatedTokenCannotBeReusedAndNoNewTokenIssued() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection(builder -> {
            final ModularXmppClientToServerConnectionConfiguration.Builder modularBuilder =
                (ModularXmppClientToServerConnectionConfiguration.Builder) builder;
            modularBuilder.with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .setAutoRequestToken(true)
                .buildModule();
            // Restrict to PLAIN for the initial, password-based leg: SCRAM (any variant) currently fails with
            // 'malformed-request' in this environment (a local Smack SCRAM regression, unrelated to FAST).
            modularBuilder.addEnabledSaslMechanism("PLAIN");
            modularBuilder.addEnabledSaslMechanism("HT-SHA-256-NONE");
        });
        try {
            // Obtain an initial token.
            connection.connect();
            connection.login();
            final FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
            assertNotNull(fastModule, "FastModule should be present on connection");
            final FastToken issuedToken = fastModule.getFastToken();
            assertNotNull(issuedToken, "Expected a FAST token to have been issued.");
            connection.disconnect();

            // Reconnect, authenticate with that token, and request its invalidation (without requesting a new one).
            connection.connect();
            fastModule.setInvalidateToken(true);
            connection.login();
            assertTrue(connection.isAuthenticated(), "Expected authentication using the token, with invalidation requested, to succeed.");

            final FastElements.Token newToken = authenticationResultOf(connection).getSuccessExtension(FastElements.Token.class);
            assertNull(newToken, "Expected the '<success/>' element to not contain a new '<token/>' element, since "
                + "invalidation was requested without also requesting a new token.");
            connection.disconnect();

            // Reconnect once more, and specifically probe whether the now-invalidated token is still accepted.
            //
            // This is deliberately sent as a raw nonza rather than via connection.login(): per XEP-0484's own
            // client-responsibility guidance ("If a client attempts authentication using a token, but the server
            // returns a SASL <failure/>, the client SHOULD discard the token and automatically fall back to
            // alternative authentication mechanisms"), Smack's login() correctly falls back to a fresh PLAIN +
            // <request-token/> attempt after the FAST mechanism fails - which would make login() as a whole succeed
            // regardless of whether the invalidated token itself was accepted, masking exactly what this test needs
            // to observe. The raw attempt reuses the connection's own Sasl2Module user-agent id, so the server sees
            // the same "client installation" that requested and then invalidated the token.
            connection.connect();
            final Sasl2Module sasl2Module = connection.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
            final Sasl2Nonza.UserAgent userAgent = new Sasl2Nonza.UserAgent(sasl2Module.getUserAgentId(), "Smack", null);
            final CharSequence username = connection.getConfiguration().getUsername();
            final String initialResponse = FastTestUtils.htInitialResponse("HT-SHA-256-NONE", username.toString(), issuedToken.getToken(), new byte[0]);
            final Sasl2Nonza.Authenticate reuseAuthenticate = new Sasl2Nonza.Authenticate("HT-SHA-256-NONE", initialResponse,
                userAgent, Collections.singletonList(new FastElements.Fast(1L, null)));
            assertThrows(FailedNonzaException.class,
                () -> connection.sendAndWaitForResponse(reuseAuthenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class),
                "Expected the service to reject reuse of a token that was previously invalidated.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "Server advertises support for FAST", quote =
        "There SHOULD be at least one mechanism capable of channel binding [...] A set of compatible mechanisms can "
      + "be found in [XEP-0824: SASL HT].")
    public void testChannelBindingCapableMechanismAuthenticatesSuccessfully() throws Exception
    {
        final ModularXmppClientToServerConnection probeConnection = getSpecificUnconnectedConnection();
        try {
            probeConnection.connect();
            final FastElements.Fast fast = FastTestUtils.requireFast(probeConnection);
            final boolean hasEndp = fast.getMechanisms().stream().anyMatch(m -> m.startsWith("HT-") && m.endsWith("-ENDP"));
            if (!hasEndp) {
                throw new TestNotPossibleException("Service does not advertise a channel-binding-capable ('-ENDP') "
                    + "FAST mechanism that this version of Smack can use (EXPR/UNIQ channel-binding types are not "
                    + "yet implemented by Smack).");
            }
        } finally {
            probeConnection.disconnect();
        }

        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection(builder -> {
            final ModularXmppClientToServerConnectionConfiguration.Builder modularBuilder =
                (ModularXmppClientToServerConnectionConfiguration.Builder) builder;
            modularBuilder.with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-ENDP")
                .setAutoRequestToken(true)
                .buildModule();
            modularBuilder.addEnabledSaslMechanism("PLAIN");
            modularBuilder.addEnabledSaslMechanism("HT-SHA-256-ENDP");
        });
        try {
            connection.connect();
            connection.login();
            connection.disconnect();

            connection.connect();
            connection.login();
            assertTrue(connection.isAuthenticated(), "Expected re-authentication using a channel-binding-capable "
                + "('HT-SHA-256-ENDP') FAST token to succeed.");
            assertTrue(authenticationResultOf(connection).getUsedSaslMechanism().getName().endsWith("-ENDP"),
                "Expected the channel-binding-capable mechanism to actually have been used for the second connection.");
        } finally {
            connection.disconnect();
        }
    }
}
