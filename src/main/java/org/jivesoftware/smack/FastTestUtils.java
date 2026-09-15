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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.fast.element.FastElements;
import org.jivesoftware.smack.sasl.ht.SaslHtMechanism;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.igniterealtime.smack.inttest.TestNotPossibleException;

/**
 * Shared guard checks and raw-SASL2/FAST helpers for tests that depend on XEP-0484: Fast Authentication Streamlining
 * Tokens (FAST), which in turn depends on XEP-0388: Extensible SASL Profile (SASL2). See {@link Sasl2TestUtils} for
 * why "not supported" and "TLS disabled for this run" need to be told apart.
 *
 * The hand-construction helpers below exist because Smack's own high-level FAST client code
 * ({@code Sasl2Module}) unconditionally sends a hardcoded, non-UUID {@code <user-agent id="smack-client-instance">}
 * whenever FAST is enabled. Openfire (correctly, per XEP-0388's "The contents of the 'id' attribute MUST be a UUID
 * v4") rejects this, so any test that needs a *working* FAST exchange must build its own SASL2 {@code
 * <authenticate/>} with a spec-compliant, per-client-stable {@code <user-agent/>} rather than go through
 * {@code connection.login()}.
 */
public final class FastTestUtils {

    private static final byte[] INITIATOR_PREFIX = "Initiator".getBytes(StandardCharsets.US_ASCII);
    private static final Pattern HT_MECHANISM = Pattern.compile("HT-(SHA-256|SHA-512|SHA3-256|SHA3-512)-(NONE|ENDP|UNIQ|EXPR)");

    private FastTestUtils() {
    }

    /**
     * Asserts that the given, already connected, connection has FAST inline within SASL2, throwing a
     * {@link TestNotPossibleException} with an actionable explanation otherwise. Returns the advertised
     * {@code <fast/>} inline feature.
     */
    public static FastElements.Fast requireFast(AbstractXMPPConnection connection) throws TestNotPossibleException {
        final Sasl2Feature sasl2Feature = Sasl2TestUtils.requireSasl2(connection);

        final FastElements.Fast fast = sasl2Feature.getInlineFeature(FastElements.Fast.class);
        if (fast == null) {
            throw new TestNotPossibleException("XEP-0484: FAST is not supported by service");
        }
        return fast;
    }

    /**
     * As {@link #requireFast(AbstractXMPPConnection)}, additionally requiring that a specific FAST (HT-*) mechanism
     * is offered.
     */
    public static FastElements.Fast requireFast(AbstractXMPPConnection connection, String requiredMechanism) throws TestNotPossibleException {
        final FastElements.Fast fast = requireFast(connection);
        if (!fast.getMechanisms().contains(requiredMechanism)) {
            throw new TestNotPossibleException("XEP-0484: FAST is supported, but the '" + requiredMechanism
                + "' mechanism is not offered by service");
        }
        return fast;
    }

    /**
     * The FAST mechanisms advertised by the service that this version of Smack can actually drive: names starting
     * with 'HT-' (Smack does not implement the alternate 'HT2-' construction some servers also offer), and using a
     * channel-binding type Smack implements (NONE or ENDP - EXPR and UNIQ are not currently supported by
     * {@code SaslHtMechanism}).
     */
    public static List<String> drivableMechanisms(FastElements.Fast fast) {
        return fast.getMechanisms().stream()
            .filter(mechanism -> mechanism.startsWith("HT-"))
            .filter(mechanism -> mechanism.endsWith("-NONE") || mechanism.endsWith("-ENDP"))
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * A fresh, spec-compliant (UUID v4) {@code <user-agent/>} element. Openfire's server-side FAST handling
     * ({@code FastRequest.from()}) requires a {@code <user-agent/>} with a valid id on any request that carries a
     * FAST {@code <request-token/>} or uses a FAST ('HT-*') mechanism; a missing (or non-UUID-v4) id is rejected
     * with 'malformed-request'. Callers MUST reuse the same instance across every request that belongs to one
     * logical "client installation" (e.g. requesting a token and later redeeming it): Openfire keys stored tokens by
     * (username, mechanism, clientID), so a fresh id on a later call looks like a different, unrelated client.
     */
    public static Sasl2Nonza.UserAgent newUserAgent() {
        return new Sasl2Nonza.UserAgent(UUID.randomUUID(), "smack-sint-server-extensions", null);
    }

    /** Builds the base64 SASL PLAIN {@code <initial-response/>}. */
    public static String plainInitialResponse(CharSequence username, String password) {
        final String plainPayload = '\0' + username.toString() + '\0' + password;
        return Base64.encodeToString(plainPayload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Maps e.g. "HT-SHA-256-NONE" to the JCA HMAC algorithm name "HmacSHA256". Delegates to Smack's own
     * {@link SaslHtMechanism.HashAlgorithm} rather than hand-deriving the name, since the mapping is not a uniform
     * "strip all hyphens" transform (e.g. "SHA3-256" maps to "HmacSHA3-256", keeping its hyphen).
     */
    private static String hmacAlgorithmFor(String htMechanismName) {
        final Matcher matcher = HT_MECHANISM.matcher(htMechanismName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a recognised 'HT-*' mechanism: " + htMechanismName);
        }
        final String ianaName = matcher.group(1);
        for (SaslHtMechanism.HashAlgorithm hashAlgorithm : SaslHtMechanism.HashAlgorithm.values()) {
            if (hashAlgorithm.getIanaName().equals(ianaName)) {
                return hashAlgorithm.getHmacAlgorithm();
            }
        }
        throw new IllegalArgumentException("Unrecognised hash algorithm '" + ianaName + "' in mechanism: " + htMechanismName);
    }

    /**
     * Builds the base64 SASL-HT {@code <initial-response/>}: {@code authcid NUL initiator-hashed-token}, where
     * {@code initiator-hashed-token = HMAC(key=tokenSecret, data="Initiator" || channelBindingData)}. Pass an empty
     * array for {@code channelBindingData} for the 'NONE' channel-binding type.
     */
    public static String htInitialResponse(String htMechanismName, String username, String tokenSecret, byte[] channelBindingData)
                    throws NoSuchAlgorithmException, InvalidKeyException {
        final String hmacAlgorithm = hmacAlgorithmFor(htMechanismName);
        final Mac mac = Mac.getInstance(hmacAlgorithm);
        mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), hmacAlgorithm));
        mac.update(INITIATOR_PREFIX);
        final byte[] initiatorHashedToken = mac.doFinal(channelBindingData);

        final byte[] authcidBytes = username.getBytes(StandardCharsets.UTF_8);
        final byte[] initialResponseBytes = new byte[authcidBytes.length + 1 + initiatorHashedToken.length];
        System.arraycopy(authcidBytes, 0, initialResponseBytes, 0, authcidBytes.length);
        // byte at authcidBytes.length is already 0 (NUL separator)
        System.arraycopy(initiatorHashedToken, 0, initialResponseBytes, authcidBytes.length + 1, initiatorHashedToken.length);

        return Base64.encodeToString(initialResponseBytes);
    }
}
