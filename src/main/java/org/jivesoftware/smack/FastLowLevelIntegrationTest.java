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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.fast.FastToken;
import org.jivesoftware.smack.fast.element.FastElements;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.sasl.ht.SaslHtMechanism;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

/**
 * Tests that verify the behavior of a server around XEP-0484: Fast Authentication Streamlining Tokens (FAST), for
 * checks that need to hand-construct a SASL-HT exchange rather than go through Smack's own {@code SaslHtMechanism}.
 * Smack's client-side code actively guards against sending a token under a mismatched mechanism (see
 * {@code FastModule#getSkipReason}), so provoking that scenario - and a couple of others where we need to see the
 * server's raw reaction rather than Smack's own transparent fallback handling - needs the same raw-nonza approach
 * used throughout this project for XEP-0388/XEP-0386.
 *
 * Only the 'NONE' (no channel binding) HT mechanism family is used here, to avoid needing to replicate Smack's own
 * TLS channel-binding data extraction; the channel-binding-capable path is covered by
 * {@link FastNegotiationIntegrationTest#testChannelBindingCapableMechanismAuthenticatesSuccessfully}, which goes
 * through Smack's own (already correct) implementation instead.
 */
@SpecificationReference(document = "XEP-0484", version = "0.2.0")
public class FastLowLevelIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    private static final byte[] INITIATOR_PREFIX = "Initiator".getBytes(StandardCharsets.US_ASCII);
    private static final Pattern HT_NONE_MECHANISM = Pattern.compile("HT-(SHA-256|SHA-512|SHA3-256|SHA3-512)-NONE");

    @SuppressWarnings("this-escape")
    public FastLowLevelIntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            FastTestUtils.requireFast(connection, "HT-SHA-256-NONE");
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Maps e.g. "HT-SHA-256-NONE" to the JCA HMAC algorithm name "HmacSHA256". Delegates to Smack's own
     * {@link SaslHtMechanism.HashAlgorithm} rather than hand-deriving the name, since the mapping is not a uniform
     * "strip all hyphens" transform (e.g. "SHA3-256" maps to "HmacSHA3-256", keeping its hyphen).
     */
    private static String hmacAlgorithmFor(String htMechanismName) {
        final Matcher matcher = HT_NONE_MECHANISM.matcher(htMechanismName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a recognised 'HT-*-NONE' mechanism: " + htMechanismName);
        }
        final String ianaName = matcher.group(1);
        for (SaslHtMechanism.HashAlgorithm hashAlgorithm : SaslHtMechanism.HashAlgorithm.values()) {
            if (hashAlgorithm.getIanaName().equals(ianaName)) {
                return hashAlgorithm.getHmacAlgorithm();
            }
        }
        throw new IllegalArgumentException("Unrecognised hash algorithm '" + ianaName + "' in mechanism: " + htMechanismName);
    }

    /** Builds the base64 <initial-response/> for a SASL-HT ('NONE' channel binding) exchange. */
    private static String htNoneInitialResponse(String htMechanismName, String username, String tokenSecret)
                    throws NoSuchAlgorithmException, InvalidKeyException {
        final String hmacAlgorithm = hmacAlgorithmFor(htMechanismName);
        final Mac mac = Mac.getInstance(hmacAlgorithm);
        mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), hmacAlgorithm));
        final byte[] initiatorHashedToken = mac.doFinal(INITIATOR_PREFIX); // channel-binding data is empty for 'NONE'

        final byte[] authcidBytes = username.getBytes(StandardCharsets.UTF_8);
        final byte[] initialResponseBytes = new byte[authcidBytes.length + 1 + initiatorHashedToken.length];
        System.arraycopy(authcidBytes, 0, initialResponseBytes, 0, authcidBytes.length);
        // byte at authcidBytes.length is already 0 (NUL separator)
        System.arraycopy(initiatorHashedToken, 0, initialResponseBytes, authcidBytes.length + 1, initiatorHashedToken.length);

        return Base64.encodeToString(initialResponseBytes);
    }

    private static String plainInitialResponse(CharSequence username, String password) {
        final String plainPayload = '\0' + username.toString() + '\0' + password;
        return Base64.encodeToString(plainPayload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A fresh, spec-compliant (UUID v4) {@code <user-agent/>} element. Openfire's server-side FAST handling
     * ({@code FastRequest.from()}) requires a {@code <user-agent/>} with a valid id on any request that carries a
     * FAST {@code <request-token/>} or uses a FAST ('HT-*') mechanism; a missing (or, per
     * {@code UserAgentInfo.extract()}, non-UUID-v4) id is rejected with 'malformed-request'.
     */
    private static Sasl2Nonza.UserAgent newUserAgent() {
        return new Sasl2Nonza.UserAgent(UUID.randomUUID().toString(), "smack-sint-server-extensions", null);
    }

    @SmackIntegrationTest(section = "Server provides token to client", quote =
        "The server MUST NOT provide a token unless the client has been successfully and fully authenticated, "
      + "including any necessary post-authentication tasks (such as multi-factor authentication).")
    public void testTokenNotIssuedOnFailedAuthentication() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();

            final CharSequence username = connection.getConfiguration().getUsername();
            final String invalidPassword = "invalid-password-" + StringUtils.insecureRandomString(16);
            final FastElements.RequestToken requestToken = new FastElements.RequestToken("HT-SHA-256-NONE");
            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("PLAIN",
                plainInitialResponse(username, invalidPassword), newUserAgent(), java.util.Collections.singletonList(requestToken));

            final FailedNonzaException e = assertThrows(FailedNonzaException.class,
                () -> connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class),
                "Expected the failed authentication attempt (with a FAST '<request-token/>' also present) to result in a '<failure/>' element.");

            final Sasl2Nonza.Failure failure = (Sasl2Nonza.Failure) e.getNonza();
            final FastElements.Token token = failure.getExtension(FastElements.Token.ELEMENT, FastElements.NAMESPACE);
            assertNull(token,
                "Expected the '<failure/>' element to not contain a FAST '<token/>' element, since authentication did not succeed.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "Client authenticates using FAST", quote =
        "Servers MUST bind tokens to the mechanism selected by the client in its original request, and reject "
      + "attempts to use them with other mechanisms. For example, if the client selected a mechanism capable of "
      + "channel binding, an attempt to use a mechanism without channel binding MUST fail even if the token would "
      + "otherwise be accepted by that mechanism.")
    public void testTokenRejectedUnderDifferentMechanism() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            final FastElements.Fast fast = FastTestUtils.requireFast(connection);
            final List<String> noneMechanisms = new ArrayList<>();
            for (String mechanism : fast.getMechanisms()) {
                if (HT_NONE_MECHANISM.matcher(mechanism).matches()) {
                    noneMechanisms.add(mechanism);
                }
            }
            if (noneMechanisms.size() < 2) {
                throw new TestNotPossibleException("Service does not offer at least two distinct 'HT-*-NONE' FAST "
                    + "mechanisms that this version of Smack can use, needed to request a token under one mechanism "
                    + "and attempt to reuse it under another.");
            }
            final String issuingMechanism = noneMechanisms.get(0);
            final String otherMechanism = noneMechanisms.get(1);

            // The user-agent id must stay stable across the request/redeem exchange: Openfire keys stored tokens by
            // (username, mechanism, clientID), so a fresh id on the second call would look like a different client
            // asking about a token it never requested, rather than the same client presenting the wrong mechanism.
            final Sasl2Nonza.UserAgent userAgent = newUserAgent();

            // Obtain a token bound to 'issuingMechanism', via a normal password-based login.
            final CharSequence username = connection.getConfiguration().getUsername();
            final String password = connection.getConfiguration().getPassword();
            final Sasl2Nonza.Authenticate initialAuthenticate = new Sasl2Nonza.Authenticate("PLAIN",
                plainInitialResponse(username, password), userAgent,
                java.util.Collections.singletonList(new FastElements.RequestToken(issuingMechanism)));
            final Sasl2Nonza.Success initialSuccess = connection.sendAndWaitForResponse(initialAuthenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(initialSuccess, "Expected the service to authenticate '" + username + "' using PLAIN.");
            final FastElements.Token token = initialSuccess.getExtension(FastElements.Token.class);
            assertNotNull(token, "Expected a FAST token for mechanism '" + issuingMechanism + "' to have been issued.");
            connection.disconnect();

            // Reconnect (same account) and attempt to use that same token's secret, but under a *different*
            // mechanism than the one it was issued for. This must fail.
            connection.connect();
            final String mismatchedInitialResponse = htNoneInitialResponse(otherMechanism, username.toString(), token.getToken());
            final Sasl2Nonza.Authenticate mismatchedAuthenticate = new Sasl2Nonza.Authenticate(otherMechanism, mismatchedInitialResponse,
                userAgent, java.util.Collections.singletonList(new FastElements.Fast(1L, null)));
            assertThrows(FailedNonzaException.class,
                () -> connection.sendAndWaitForResponse(mismatchedAuthenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class),
                "Expected the service to reject a token issued for '" + issuingMechanism + "' when it is instead "
                    + "presented (with a cryptographically valid exchange) under the different mechanism '" + otherMechanism + "'.");
            assertFalse(connection.isAuthenticated(), "Expected the connection to not be authenticated after the mismatched-mechanism attempt.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "Server responsibilities", quote =
        "If the server no longer trusts a token, it MUST instead fail the authentication (returning the SASL "
      + "'credentials-expired' error condition), and then allow the client to authenticate using other mechanisms "
      + "(e.g. password based).")
    public void testInvalidatedTokenFailsWithCredentialsExpiredAndStreamStaysUsable() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            final CharSequence username = connection.getConfiguration().getUsername();
            final String password = connection.getConfiguration().getPassword();

            // The user-agent id must stay stable across every step below: Openfire keys stored tokens by
            // (username, mechanism, clientID), so a fresh id on a later call would look like a different client.
            final Sasl2Nonza.UserAgent userAgent = newUserAgent();

            // Obtain a genuine token, then immediately invalidate it (rather than testing with a token that was
            // never issued at all): this is the scenario the quoted MUST actually describes ("no longer trusts a
            // token"), and is distinguishable server-side from a token that is merely unrecognised/malformed.
            final Sasl2Nonza.Authenticate requestAuthenticate = new Sasl2Nonza.Authenticate("PLAIN",
                plainInitialResponse(username, password), userAgent,
                java.util.Collections.singletonList(new FastElements.RequestToken("HT-SHA-256-NONE")));
            final Sasl2Nonza.Success requestSuccess = connection.sendAndWaitForResponse(requestAuthenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(requestSuccess, "Expected the service to authenticate '" + username + "' using PLAIN.");
            final FastElements.Token token = requestSuccess.getExtension(FastElements.Token.class);
            assertNotNull(token, "Expected a FAST token to have been issued.");
            connection.disconnect();

            connection.connect();
            final String invalidatingInitialResponse = htNoneInitialResponse("HT-SHA-256-NONE", username.toString(), token.getToken());
            final Sasl2Nonza.Authenticate invalidatingAuthenticate = new Sasl2Nonza.Authenticate("HT-SHA-256-NONE",
                invalidatingInitialResponse, userAgent, java.util.Collections.singletonList(new FastElements.Fast(1L, true)));
            final Sasl2Nonza.Success invalidatingSuccess = connection.sendAndWaitForResponse(invalidatingAuthenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(invalidatingSuccess, "Expected the token-invalidating authentication attempt to succeed.");
            connection.disconnect();

            // Reconnect and attempt to reuse the now-invalidated token: the server no longer trusts it.
            connection.connect();
            final String initialResponse = htNoneInitialResponse("HT-SHA-256-NONE", username.toString(), token.getToken());
            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("HT-SHA-256-NONE", initialResponse,
                userAgent, java.util.Collections.singletonList(new FastElements.Fast(1L, null)));

            final FailedNonzaException e = assertThrows(FailedNonzaException.class,
                () -> connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class),
                "Expected authentication with an invalidated FAST token to result in a '<failure/>' element.");
            final Sasl2Nonza.Failure failure = (Sasl2Nonza.Failure) e.getNonza();
            assertEquals(SASLError.credentials_expired, failure.getSASLError(), "Expected the failure condition for "
                + "an invalidated ('no longer trusted') FAST token to be 'credentials-expired' (but it was '" + failure.getSASLErrorString() + "').");

            // The stream must remain usable: a normal password-based attempt should still succeed.
            final Sasl2Nonza.Authenticate passwordAuthenticate = new Sasl2Nonza.Authenticate("PLAIN", plainInitialResponse(username, password), null);
            final Sasl2Nonza.Success success = connection.sendAndWaitForResponse(passwordAuthenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(success, "Expected the service to still allow password-based authentication on the same "
                + "stream, after the earlier FAST attempt failed with 'credentials-expired'.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "Server advertises support for FAST", quote =
        "These mechanisms MUST support authenticating with a token (instead of a password) [...]")
    public void testEveryAdvertisedNoneMechanismAuthenticatesSuccessfully() throws Exception
    {
        // Note: this does not independently verify the accompanying "MUST result in success or failure within a
        // single round-trip" clause; doing so meaningfully would need to distinguish a genuine mid-exchange
        // <challenge/> from the mutual-auth data SASL-HT delivers via <success/>'s additional-data, which Smack's
        // own Sasl2Authentication already handles transparently. This test only confirms each mechanism is usable.
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            final FastElements.Fast fast = FastTestUtils.requireFast(connection);
            final List<String> noneMechanisms = new ArrayList<>();
            for (String mechanism : fast.getMechanisms()) {
                if (HT_NONE_MECHANISM.matcher(mechanism).matches()) {
                    noneMechanisms.add(mechanism);
                }
            }
            if (noneMechanisms.isEmpty()) {
                throw new TestNotPossibleException("Service does not offer any 'HT-*-NONE' FAST mechanism that this version of Smack can use.");
            }

            final CharSequence username = connection.getConfiguration().getUsername();
            final String password = connection.getConfiguration().getPassword();

            // The user-agent id must stay stable across the request/redeem exchange for a given mechanism: Openfire
            // keys stored tokens by (username, mechanism, clientID), so a fresh id on redemption would look like a
            // different client asking about a token it never requested.
            final Sasl2Nonza.UserAgent userAgent = newUserAgent();

            for (String mechanism : noneMechanisms) {
                // Request a token for this mechanism.
                final Sasl2Nonza.Authenticate requestAuthenticate = new Sasl2Nonza.Authenticate("PLAIN",
                    plainInitialResponse(username, password), userAgent,
                    java.util.Collections.singletonList(new FastElements.RequestToken(mechanism)));
                final Sasl2Nonza.Success requestSuccess = connection.sendAndWaitForResponse(requestAuthenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
                assertNotNull(requestSuccess, "Expected the service to authenticate '" + username + "' using PLAIN.");
                final FastElements.Token token = requestSuccess.getExtension(FastElements.Token.class);
                assertNotNull(token, "Expected a FAST token for mechanism '" + mechanism + "' to have been issued.");

                // Reconnect before redeeming the token: XEP-0388 § 4.8 requires a stream error on any second
                // <authenticate/> sent on an already-succeeded stream, so the redemption below needs a fresh stream.
                connection.disconnect();
                connection.connect();

                // Now use that token to authenticate via the mechanism it was issued for.
                final String initialResponse = htNoneInitialResponse(mechanism, username.toString(), token.getToken());
                final Sasl2Nonza.Authenticate fastAuthenticate = new Sasl2Nonza.Authenticate(mechanism, initialResponse,
                    userAgent, java.util.Collections.singletonList(new FastElements.Fast(1L, true))); // invalidate=true: leave no token behind
                final Sasl2Nonza.Success fastSuccess = connection.sendAndWaitForResponse(fastAuthenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
                assertNotNull(fastSuccess, "Expected mechanism '" + mechanism + "' to successfully authenticate using its issued FAST token.");

                // Reconnect again, so the next mechanism's request-token leg (if any) also starts on a fresh stream.
                connection.disconnect();
                connection.connect();
            }
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "Server initiates token rotation", quote =
        "Upon successful use of any token, the server MUST invalidate all tokens issued to the same client with an "
      + "earlier expiry than the current token (even if those tokens have not yet reached their expiry time).")
    public void testUsingNewerTokenInvalidatesOlderUnusedToken() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            final CharSequence username = connection.getConfiguration().getUsername();
            final String password = connection.getConfiguration().getPassword();

            // The user-agent id must stay stable across every step below, so the server treats every request as
            // coming from the same "client installation".
            final Sasl2Nonza.UserAgent userAgent = newUserAgent();

            // Step 1: obtain a first token T1, via a normal password-based login.
            final Sasl2Nonza.Authenticate requestT1 = new Sasl2Nonza.Authenticate("PLAIN",
                plainInitialResponse(username, password), userAgent,
                java.util.Collections.singletonList(new FastElements.RequestToken("HT-SHA-256-NONE")));
            final Sasl2Nonza.Success successT1 = connection.sendAndWaitForResponse(requestT1, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(successT1, "Expected the service to authenticate '" + username + "' using PLAIN.");
            final FastElements.Token tokenT1 = successT1.getExtension(FastElements.Token.class);
            assertNotNull(tokenT1, "Expected a first FAST token (T1) to have been issued.");
            connection.disconnect();

            // Step 2: redeem T1, and in the same exchange, request a second token T2. T1 is thereby established as
            // a genuinely-used, verified token, while T2 is issued but not yet used - so at this point both are
            // live: T1 (used, earlier expiry) and T2 (unused, later expiry, since it was issued afterwards).
            connection.connect();
            final String initialResponseT1 = htNoneInitialResponse("HT-SHA-256-NONE", username.toString(), tokenT1.getToken());
            final Sasl2Nonza.Authenticate redeemT1AndRequestT2 = new Sasl2Nonza.Authenticate("HT-SHA-256-NONE", initialResponseT1,
                userAgent, java.util.Arrays.asList(new FastElements.Fast(1L, null), new FastElements.RequestToken("HT-SHA-256-NONE")));
            final Sasl2Nonza.Success successT2 = connection.sendAndWaitForResponse(redeemT1AndRequestT2, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(successT2, "Expected redemption of T1 (with an inline request for a second token) to succeed.");
            final FastElements.Token tokenT2 = successT2.getExtension(FastElements.Token.class);
            assertNotNull(tokenT2, "Expected a second FAST token (T2) to have been issued alongside T1's redemption.");
            connection.disconnect();

            // Step 3: redeem T2. Per the quoted MUST, this must invalidate T1 - which has an earlier expiry than T2
            // - even though T1 itself was never misused, and has not yet reached its own expiry time.
            connection.connect();
            final String initialResponseT2 = htNoneInitialResponse("HT-SHA-256-NONE", username.toString(), tokenT2.getToken());
            final Sasl2Nonza.Authenticate redeemT2 = new Sasl2Nonza.Authenticate("HT-SHA-256-NONE", initialResponseT2,
                userAgent, java.util.Collections.singletonList(new FastElements.Fast(1L, null)));
            final Sasl2Nonza.Success redeemT2Success = connection.sendAndWaitForResponse(redeemT2, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class);
            assertNotNull(redeemT2Success, "Expected redemption of T2 to succeed.");
            connection.disconnect();

            // Step 4: attempt to redeem T1 again. It must now be rejected, since T2 (a token with a later expiry)
            // has since been successfully used.
            connection.connect();
            final String initialResponseT1Again = htNoneInitialResponse("HT-SHA-256-NONE", username.toString(), tokenT1.getToken());
            final Sasl2Nonza.Authenticate redeemT1Again = new Sasl2Nonza.Authenticate("HT-SHA-256-NONE", initialResponseT1Again,
                userAgent, java.util.Collections.singletonList(new FastElements.Fast(1L, null)));
            assertThrows(FailedNonzaException.class,
                () -> connection.sendAndWaitForResponse(redeemT1Again, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class),
                "Expected token T1 (earlier expiry than T2, which has since been successfully used) to have been "
                    + "invalidated, and its reuse rejected.");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "Client authenticates using FAST", quote =
        "To indicate that it is providing a token, the client MUST include a <fast/> element qualified by the "
      + "'urn:xmpp:fast:0' namespace, within its SASL2 authentication request.")
    public void testFastMechanismWithoutFastElementFailsWithMalformedRequest() throws Exception
    {
        // This exercises the server's enforcement of the quoted client MUST: an HT-mechanism authentication
        // attempt that omits the required '<fast/>' element altogether. Openfire's FastRequest.from() validates
        // this before the SASL mechanism itself ever evaluates the initial-response, so the response below need
        // not be cryptographically meaningful - only well-formed enough to reach that check.
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            final CharSequence username = connection.getConfiguration().getUsername();

            final String initialResponse = htNoneInitialResponse("HT-SHA-256-NONE", username.toString(),
                "irrelevant-token-secret-" + StringUtils.insecureRandomString(16));
            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("HT-SHA-256-NONE", initialResponse, newUserAgent());

            final FailedNonzaException e = assertThrows(FailedNonzaException.class,
                () -> connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class),
                "Expected an HT-mechanism authentication attempt with no '<fast/>' element to be rejected.");
            final Sasl2Nonza.Failure failure = (Sasl2Nonza.Failure) e.getNonza();
            assertEquals(SASLError.malformed_request, failure.getSASLError(), "Expected the failure condition to "
                + "be 'malformed-request' (but it was '" + failure.getSASLErrorString() + "').");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "Client responsibilities", quote =
        "Clients wishing to use FAST authentication MUST provide the authenticating JID in the secure stream's "
      + "'from' attribute. They MUST also provide the a SASL2 <user-agent> element with an 'id' attribute (both "
      + "of these values are discussed in more detail in XEP-0388).")
    public void testRequestTokenWithoutUserAgentFailsWithMalformedRequest() throws Exception
    {
        // Exercises only the '<user-agent/>' half of this MUST: the stream 'from' half is supplied automatically
        // by Smack's own connection setup for every test in this project (see the raw-nonza test pattern notes),
        // and there is no straightforward way to suppress it via this connection API without constructing an
        // entirely separate raw-socket stream (as testFastNotOfferedPreTls does). Both sub-conditions are enforced
        // by the exact same check server-side (FastRequest.from()'s single 'userAgentId == null || expected.isEmpty()'
        // guard), so exercising one half is sufficient to confirm that check fires as expected.
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            final CharSequence username = connection.getConfiguration().getUsername();
            final String password = connection.getConfiguration().getPassword();

            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("PLAIN",
                plainInitialResponse(username, password), null, // no <user-agent/>
                java.util.Collections.singletonList(new FastElements.RequestToken("HT-SHA-256-NONE")));

            final FailedNonzaException e = assertThrows(FailedNonzaException.class,
                () -> connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class),
                "Expected a FAST '<request-token/>' sent without a '<user-agent/>' element to be rejected.");
            final Sasl2Nonza.Failure failure = (Sasl2Nonza.Failure) e.getNonza();
            assertEquals(SASLError.malformed_request, failure.getSASLError(), "Expected the failure condition to "
                + "be 'malformed-request' (but it was '" + failure.getSASLErrorString() + "').");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest(section = "Client performs initial authentication", quote =
        "To request a FAST token, a client MUST include a <request-token/> element qualified by the "
      + "'urn:xmpp:fast:0' namespace. The element MUST contain a 'mechanism' attribute, the value of which MUST "
      + "be one of the FAST mechanisms advertised by the server.")
    public void testRequestTokenForUnadvertisedMechanismFailsWithInvalidMechanism() throws Exception
    {
        final ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            final FastElements.Fast fast = FastTestUtils.requireFast(connection);

            // A recognised FAST mechanism name (defined by the SASL-HT draft's naming convention), but one this
            // deployment does not advertise - it does not offer the 'tls-unique' channel-binding type (only NONE
            // and ENDP mechanisms were observed advertised). Guarded below rather than assumed, since this is an
            // environment fact, not a protocol guarantee.
            final String candidateMechanism = "HT-SHA-256-UNIQ";
            if (fast.getMechanisms().contains(candidateMechanism)) {
                throw new TestNotPossibleException("Service unexpectedly advertises '" + candidateMechanism
                    + "'; this test needs a recognised-but-unadvertised FAST mechanism name to name in a "
                    + "'<request-token/>' request.");
            }

            final CharSequence username = connection.getConfiguration().getUsername();
            final String password = connection.getConfiguration().getPassword();
            final Sasl2Nonza.Authenticate authenticate = new Sasl2Nonza.Authenticate("PLAIN",
                plainInitialResponse(username, password), newUserAgent(),
                java.util.Collections.singletonList(new FastElements.RequestToken(candidateMechanism)));

            final FailedNonzaException e = assertThrows(FailedNonzaException.class,
                () -> connection.sendAndWaitForResponse(authenticate, Sasl2Nonza.Success.class, Sasl2Nonza.Failure.class),
                "Expected a '<request-token/>' naming an unadvertised (but recognised) mechanism to be rejected.");
            final Sasl2Nonza.Failure failure = (Sasl2Nonza.Failure) e.getNonza();
            assertEquals(SASLError.invalid_mechanism, failure.getSASLError(), "Expected the failure condition to "
                + "be 'invalid-mechanism' (but it was '" + failure.getSASLErrorString() + "').");
        } finally {
            connection.disconnect();
        }
    }
}
