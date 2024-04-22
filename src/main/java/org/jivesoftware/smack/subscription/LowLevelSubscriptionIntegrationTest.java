/**
 *
 * Copyright 2023-2024 Guus der Kinderen
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
package org.jivesoftware.smack.subscription;

import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.PresenceTypeFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StandardExtensionElement;

import org.igniterealtime.smack.inttest.AbstractSmackLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests that verify that sent presence subscription requests are received as intended.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class LowLevelSubscriptionIntegrationTest extends AbstractSmackLowLevelIntegrationTest {
    public LowLevelSubscriptionIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    /**
     * This test verifies that a subscription request is received, in a scenario where the intended recipient was
     * offline when the request was made.
     *
     * @param conOne Connection used to receive subscription request.
     * @param conTwo Conenction used to send subscription request.
     * @throws Exception on anything unexpected or undesired.
     */
    @SmackIntegrationTest(section = "3.1.3", quote =
        "[...] if the contact has no available resources when the subscription request is received by the contact's " +
        "server, then the contact's server MUST keep a record of the complete presence stanza comprising the " +
        "subscription request [...]")
    public void testSubscriptionRequestOffline(final AbstractXMPPConnection conOne,
                                               final AbstractXMPPConnection conTwo) throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final Presence subscriptionRequest = conTwo.getStanzaFactory().buildPresenceStanza()
            .ofType(Presence.Type.subscribe)
            .to(conOne.getUser().asBareJid())
            .build();

        conOne.disconnect();

        conTwo.sendStanza(subscriptionRequest);

        conOne.connect();

        final SimpleResultSyncPoint received = new SimpleResultSyncPoint();

        final StanzaFilter resultFilter = new AndFilter(
            PresenceTypeFilter.SUBSCRIBE,
            FromMatchesFilter.createBare(conTwo.getUser())
        );

        conOne.addAsyncStanzaListener(p -> received.signal(), resultFilter);

        conOne.login();
        assertResult(received, "Expected '" + conOne.getUser() + "' to receive the subscription request sent to them while they were offline by '" + conTwo.getUser() + "' (but did not).");
    }

    /**
     * When a subscription request is made, the stanza can have additional extension elements. This test verifies that
     * such extension elements are received, in a scenario where the intended recipient was offline when the request
     * was made.
     *
     * @param conOne Connection used to receive subscription request.
     * @param conTwo Conenction used to send subscription request.
     * @see <a href="https://igniterealtime.atlassian.net/browse/OF-2244">Openfire issue OF-2244</a>
     * @throws Exception on anything unexpected or undesired.
     */
    @SmackIntegrationTest(section = "3.1.3", quote =
        "[...] if the contact has no available resources when the subscription request is received by the contact's " +
        "server, then the contact's server MUST keep a record of the complete presence stanza comprising the " +
        "subscription request, including any extended content contained therein [...]")
    public void testSubscriptionRequestOfflineWithExtension(final AbstractXMPPConnection conOne,
                                                            final AbstractXMPPConnection conTwo) throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final Presence subscriptionRequest = conTwo.getStanzaFactory().buildPresenceStanza()
            .ofType(Presence.Type.subscribe)
            .to(conOne.getUser().asBareJid())
            .addExtension(new StandardExtensionElement("test", "org.example.test"))
            .build();

        conOne.disconnect();

        conTwo.sendStanza(subscriptionRequest);

        conOne.connect();

        final ResultSyncPoint<Presence, ?> received = new ResultSyncPoint<>();

        final StanzaFilter resultFilter = new AndFilter(
            PresenceTypeFilter.SUBSCRIBE,
            FromMatchesFilter.createBare(conTwo.getUser())
        );

        conOne.addAsyncStanzaListener(p -> received.signal((Presence) p), resultFilter);

        conOne.login();
        final Presence result = assertResult(received, "Expected '" + conOne.getUser() + "' to receive the subscription request sent to them while they were offline by '" + conTwo.getUser() + "' (but did not).");
        assertTrue(result.hasExtension("test", "org.example.test"), "Expected the subscription request received by '" + conOne.getUser() + "' from '" + conTwo.getUser() + "' (sent while the intended recipient was offline) to include the custom extension that was in the original request (but that extension was not received).");
    }
}
