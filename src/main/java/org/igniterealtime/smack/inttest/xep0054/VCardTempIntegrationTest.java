/*
 * Copyright 2024 Guus der Kinderen
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
package org.igniterealtime.smack.inttest.xep0054;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.packet.id.StandardStanzaIdSource;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

// TODO: Find a way to set up a test fixture in which a user is guaranteed to _not_ have a VCard. This can then be used to test requesting a non-existing vcard.
@SpecificationReference(document = "XEP-0054", version = "1.2")
public class VCardTempIntegrationTest extends AbstractSmackIntegrationTest
{
    public VCardTempIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);

        if (!VCardManager.getInstanceFor(conOne).isSupported(conOne.getXMPPServiceDomain())) {
            throw new TestNotPossibleException("Domain does not seem support XEP-0054 vcard-temp.");
        }
    }

    /**
     * Asserts that the domain returns the user's own vcard for a user that has one.
     */
    @SmackIntegrationTest(section = "3.1", quote = "If a vCard exists for the user, the server MUST return [it] in an IQ-result")
    public void testRequestOwnExistingVCard() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final VCard vCard = new VCard();
        vCard.setFirstName("John");
        VCardManager.getInstanceFor(conOne).saveVCard(vCard);

        // Execute system-under-test.
        final VCard result = conOne.sendIqRequestAndWaitForResponse(new VCard());

        // Verify result.
        assertNotNull(result, "Expected the request of '" + conOne.getUser() + " for its VCard to be responded to by its domain with a VCard (but no VCard was returned).");
    }

    /**
     * Asserts that the domain accepts publishing a vcard (when the user does not have one yet).
     */
    @SmackIntegrationTest(section = "3.2", quote = "A user may publish [...] his or her vCard by sending an IQ of type \"set\" with no 'to' address, following the format in the previous use case.")
    public void testPublishVCard() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        try {
            final VCard preExistingVCard = VCardManager.getInstanceFor(conTwo).loadVCard();
            if (preExistingVCard != null && !preExistingVCard.equals(new VCard())) {
                throw new TestNotPossibleException("Test user '" + conTwo.getUser() + "' already has a VCard: unable to test initial publication of VCard.");
            }
        } catch (XMPPException.XMPPErrorException e) {
            // Any XMPP error can be returned (although it _should_ be 'item-not-found') when the user doesn't have a vcard.
        }

        final VCard publish = new VCard();
        publish.setType(IQ.Type.set);
        publish.setFirstName("John");

        // Execute system-under-test.
        final IQ result = conTwo.sendIqRequestAndWaitForResponse(publish);

        // Verify result.
        assertEquals(IQ.Type.result, result.getType(), "Expected the request of '" + conTwo.getUser() + " to set it's initial VCard to be responded to by its domain with a 'result' typed IQ stanza response (but it was not).");
    }

    /**
     * Asserts that the domain accepts updating a vcard (when the user already had one).
     */
    @SmackIntegrationTest(section = "3.2", quote = "A user may [...] update his or her vCard by sending an IQ of type \"set\" with no 'to' address, following the format in the previous use case.")
    public void testUpdateVCard() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final VCard original = new VCard();
        original.setType(IQ.Type.set);
        original.setFirstName("Jane");
        conOne.sendIqRequestAndWaitForResponse(original);

        final VCard update = new VCard();
        update.setType(IQ.Type.set);
        update.setFirstName("John");

        // Execute system-under-test.
        final IQ result = conOne.sendIqRequestAndWaitForResponse(update);

        // Verify result.
        assertEquals(IQ.Type.result, result.getType(), "Expected the request of '" + conOne.getUser() + " to replace its VCard to be responded to by its domain with a 'result' typed IQ stanza response (but it was not).");
    }

    /**
     * Asserts that an error is returned when a user tries to update a vcard that belongs to someone else.
     */
    @SmackIntegrationTest(section = "3.2", quote = "If a user attempts to perform an IQ set on another user's vCard (i.e., by setting a 'to' address to a JID other than the sending user's bare JID), the server MUST return a stanza error [...]")
    public void testUpdateOtherUsersVCard() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final VCard request = new VCard();
        request.setType(IQ.Type.set);
        request.setFirstName("Jake");

        // Execute system-under-test.
        request.setTo(conTwo.getUser().asBareJid());
        try {
            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            fail("Expected the server to return an error when '" + conOne.getUser() + "' tried to set a vcard for someone else ('" + request.getTo() + "') (but the server did not).");
        } catch (XMPPException.XMPPErrorException e) {
            assertNotNull(e.getStanzaError(), "Expected the server to return a non-empty error when '" + conOne.getUser() + "' tried to set a vcard for someone else ('" + request.getTo() + "') (but the server did not).");
        }
    }

    /**
     * Asserts that a request for someone else's VCard (requested from the bare JID) is answered by the server, even if
     * the target has no VCard.
     */
    @SmackIntegrationTest(section = "3.3", quote = "In accordance with XMPP Core, a compliant server MUST respond on behalf of the requestor and not forward the IQ to the requestee's connected resource.")
    public void testRequestAnsweredByServerWhenRecipientHasNoVCard() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        try {
            final VCard preExistingVCard = VCardManager.getInstanceFor(conThree).loadVCard();
            if (preExistingVCard != null && !preExistingVCard.equals(new VCard())) {
                throw new TestNotPossibleException("Test user '" + conThree.getUser() + "' already has a VCard: unable to test initial publication of VCard.");
            }
        } catch (XMPPException.XMPPErrorException e) {
            // Any XMPP error can be returned (although it _should_ be 'item-not-found') when the user doesn't have a vcard.
        }

        final VCard request = new VCard();
        request.setTo(conThree.getUser().asBareJid());

        final AtomicBoolean conThreeGotRequest = new AtomicBoolean(false);
        final StanzaListener stanzaListener = stanza -> conThreeGotRequest.set(true);
        conThree.addStanzaListener(stanzaListener, new AndFilter(IQTypeFilter.GET, FromMatchesFilter.createBare(conOne.getUser()), new StanzaExtensionFilter(VCard.ELEMENT, VCard.NAMESPACE)));
        try {
            // Execute system-under-test.
            try {
                conOne.sendIqRequestAndWaitForResponse(request);
            } catch (XMPPException.XMPPErrorException e) {
                // Any XMPP error can be returned (although it _should_ be 'item-not-found') when the user doesn't have a vcard.
            }

            // Verify result
            assertFalse(conThreeGotRequest.get(), "Did NOT expect '" + conThree.getUser() + "' to receive the request made by '" + conOne.getUser() + "' to the bare JID of '" + conThree.getUser() + "' (but the request was unexpectedly received anyway).");
        }
        finally {
            // Tear down test fixture.
            conThree.removeStanzaListener(stanzaListener);
        }
    }


    /**
     * Asserts that a request for someone else's VCard (requested from the bare JID) is answered by the server.
     */
    @SmackIntegrationTest(section = "3.3", quote = "In accordance with XMPP Core, a compliant server MUST respond on behalf of the requestor and not forward the IQ to the requestee's connected resource.")
    public void testRequestAnsweredByServerWhenRecipientHasVCard() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final VCard publish = new VCard();
        publish.setType(IQ.Type.set);
        publish.setFirstName("Jane");
        conOne.sendIqRequestAndWaitForResponse(publish);

        final VCard request = new VCard();
        request.setTo(conOne.getUser().asBareJid());

        final AtomicBoolean conOneGotRequest = new AtomicBoolean(false);
        final StanzaListener stanzaListener = stanza -> conOneGotRequest.set(true);
        conOne.addStanzaListener(stanzaListener, new AndFilter(IQTypeFilter.GET, FromMatchesFilter.createBare(conTwo.getUser()), new StanzaExtensionFilter(VCard.ELEMENT, VCard.NAMESPACE)));
        try {
            // Execute system-under-test.
            conTwo.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            assertFalse(conOneGotRequest.get(), "Did NOT expect '" + conOne.getUser() + "' to receive the request made by '" + conTwo.getUser() + "' to the bare JID of '" + conOne.getUser() + "' (but the request was unexpectedly received anyway).");
        } finally {
            // Tear down test fixture.
            conOne.removeStanzaListener(stanzaListener);
        }
    }

    /**
     * Asserts that a request for someone else's VCard that does not have a VCard is answered to with an error response.
     */
    @SmackIntegrationTest(section = "3.3", quote = "If no vCard exists [...] the server MUST return a stanza error,")
    public void testForErrorWhenRecipientHasNoVCard() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        // Setup test fixture.
        try {
            final VCard preExistingVCard = VCardManager.getInstanceFor(conThree).loadVCard();
            if (preExistingVCard != null && !preExistingVCard.equals(new VCard())) {
                throw new TestNotPossibleException("Test user '" + conThree.getUser() + "' already has a VCard: unable to test request a non-existing VCard.");
            }
        } catch (XMPPException.XMPPErrorException e) {
            // Any XMPP error can be returned (although it _should_ be 'item-not-found') when the user doesn't have a vcard.
        }
        final VCard request = new VCard();
        request.setTo(conThree.getUser().asBareJid());

        try {
            // Execute system under test
            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            fail("Expected the server to return an error when '" + conOne.getUser() + "' requested a vcard from a user that does not have one ('" + request.getTo() + "') (but the server did not).");
        } catch (XMPPException.XMPPErrorException e) {
            assertNotNull(e.getStanzaError(), "Expected the server to return a non-empty error when '" + conOne.getUser() + "' requested a vcard from a user that does not have one ('" + request.getTo() + "') (but the server did not).");
        }
    }

    /**
     * Asserts that a request for someone else's VCard for a user that does not exist is answered to with an error response.
     */
    @SmackIntegrationTest(section = "3.3", quote = "If [...] the user does not exist, the server MUST return a stanza error,")
    public void testForErrorWhenRecipientDoesntExist() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmppStringprepException
    {
        // Setup test fixture.
        final VCard request = new VCard();
        request.setTo(JidCreate.entityBareFrom(Localpart.from("non-existing-test-user-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain()));

        try {
            // Execute system under test
            conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result.
            fail("Expected the server to return an error when '" + conOne.getUser() + "' requested a vcard from a user that does not exist ('" + request.getTo() + "') (but the server did not).");
        } catch (XMPPException.XMPPErrorException e) {
            assertNotNull(e.getStanzaError(), "Expected the server to return a non-empty error when '" + conOne.getUser() + "' requested a vcard from a user that does not exist ('" + request.getTo() + "') (but the server did not).");
        }
    }

    /**
     * Asserts that a request for someone else's VCard that does not have a VCard is answered to with the same error
     * response as a request for a VCard from a non-existing user.
     */
    @SmackIntegrationTest(section = "3.3", quote = "If no vCard exists or the user does not exist, the server MUST return a stanza error, which SHOULD be either <service-unavailable/> or <item-not-found/> (but the server MUST return the same error condition in both cases to help prevent directory harvesting attacks).")
    public void testForErrorEquality() throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException, XmppStringprepException
    {
        // Setup test fixture.
        try {
            final VCard preExistingVCard = VCardManager.getInstanceFor(conThree).loadVCard();
            if (preExistingVCard != null && !preExistingVCard.equals(new VCard())) {
                throw new TestNotPossibleException("Test user '" + conThree.getUser() + "' already has a VCard: unable to test request a non-existing VCard.");
            }
        } catch (XMPPException.XMPPErrorException e) {
            // Any XMPP error can be returned (although it _should_ be 'item-not-found') when the user doesn't have a vcard.
        }
        final VCard requestNonExistingVCard = new VCard();
        requestNonExistingVCard.setTo(conThree.getUser().asBareJid());

        final VCard requestNonExistingUser = new VCard();
        requestNonExistingUser.setTo(JidCreate.entityBareFrom(Localpart.from("non-existing-test-user-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain()));

        // Execute system under test
        StanzaError errorNonExistingVCard = null;
        StanzaError errorNonExistingUser = null;
        try {
            conOne.sendIqRequestAndWaitForResponse(requestNonExistingVCard);
        } catch (XMPPException.XMPPErrorException e) {
            errorNonExistingVCard = e.getStanzaError();
        }

        try {
            // Execute system under test
            conOne.sendIqRequestAndWaitForResponse(requestNonExistingUser);
        } catch (XMPPException.XMPPErrorException e) {
            errorNonExistingUser = e.getStanzaError();
        }

        // Verify result
        assertEquals(errorNonExistingUser.getCondition(), errorNonExistingVCard.getCondition(), "Expected '" + conOne.getUser() + "' to receive the same error condition when requesting a VCard from a user that does not have one ('" + requestNonExistingVCard.getTo() + "') and when requesting a VCard from a user that does not exist ('" + requestNonExistingUser.getTo() + "'), but distinct conditions were received.");
    }

    /**
     * Asserts that a VCard that uses all fields (as specified in section 8) is accepted by the server.
     */
    // TODO verify that _all_ fields are actually used in this request.
    @SmackIntegrationTest(section = "8")
    public void testAllFieldsVCardUpdate() throws SmackException.NotConnectedException, InterruptedException
    {
        final String stanzaId = StandardStanzaIdSource.DEFAULT.getNewStanzaId();

        // Taken from the XEP.
        final String rawVCard = "<iq id='" + stanzaId + "' type='set'>\n" +
            "  <vCard xmlns='vcard-temp'>\n" +
            "    <FN>Peter Saint-Andre</FN>\n" +
            "    <N>\n" +
            "      <FAMILY>Saint-Andre</FAMILY>\n" +
            "      <GIVEN>Peter</GIVEN>\n" +
            "      <MIDDLE/>\n" +
            "    </N>\n" +
            "    <NICKNAME>stpeter</NICKNAME>\n" +
            "    <URL>http://www.xmpp.org/xsf/people/stpeter.shtml</URL>\n" +
            "    <BDAY>1966-08-06</BDAY>\n" +
            "    <ORG>\n" +
            "      <ORGNAME>XMPP Standards Foundation</ORGNAME>\n" +
            "      <ORGUNIT/>\n" +
            "    </ORG>\n" +
            "    <TITLE>Executive Director</TITLE>\n" +
            "    <ROLE>Patron Saint</ROLE>\n" +
            "    <TEL><WORK/><VOICE/><NUMBER>303-308-3282</NUMBER></TEL>\n" +
            "    <TEL><WORK/><FAX/><NUMBER/></TEL>\n" +
            "    <TEL><WORK/><MSG/><NUMBER/></TEL>\n" +
            "    <ADR>\n" +
            "      <WORK/>\n" +
            "      <EXTADD>Suite 600</EXTADD>\n" +
            "      <STREET>1899 Wynkoop Street</STREET>\n" +
            "      <LOCALITY>Denver</LOCALITY>\n" +
            "      <REGION>CO</REGION>\n" +
            "      <PCODE>80202</PCODE>\n" +
            "      <CTRY>USA</CTRY>\n" +
            "    </ADR>\n" +
            "    <TEL><HOME/><VOICE/><NUMBER>303-555-1212</NUMBER></TEL>\n" +
            "    <TEL><HOME/><FAX/><NUMBER/></TEL>\n" +
            "    <TEL><HOME/><MSG/><NUMBER/></TEL>\n" +
            "    <ADR>\n" +
            "      <HOME/>\n" +
            "      <EXTADD/>\n" +
            "      <STREET/>\n" +
            "      <LOCALITY>Denver</LOCALITY>\n" +
            "      <REGION>CO</REGION>\n" +
            "      <PCODE>80209</PCODE>\n" +
            "      <CTRY>USA</CTRY>\n" +
            "    </ADR>\n" +
            "    <EMAIL><INTERNET/><PREF/><USERID>stpeter@jabber.org</USERID></EMAIL>\n" +
            "    <JABBERID>stpeter@jabber.org</JABBERID>\n" +
            "    <DESC>\n" +
            "      Check out my blog at https://stpeter.im/\n" +
            "    </DESC>\n" +
            "  </vCard>\n" +
            "</iq>";
        final AdHocPacket rawPacket = new AdHocPacket(rawVCard.replace("\n", "").replace("    ", "").replace("  ", ""));

        try (final StanzaCollector collector = conOne.createStanzaCollectorAndSend(new StanzaIdFilter(stanzaId), rawPacket)) {
            final IQ response = collector.nextResult();
            assertEquals(IQ.Type.result, response.getType(), "Expected the VCard (that sets all known fields) as saved by '" + conOne.getUser() + "' to be accepted by the server (but it was not)");
        }

        // TODO retrieve vCard, see if all values were saved.
    }

    // Copied from Smack's debugger.
    private static final class AdHocPacket extends Stanza {

        private final String text;

        /**
         * Create a new AdHocPacket with the text to send. The passed text must be a valid text to
         * send to the server, no validation will be done on the passed text.
         *
         * @param text the whole text of the stanza to send
         */
        private AdHocPacket(String text) {
            this.text = text;
        }

        @Override
        public String toXML(XmlEnvironment enclosingNamespace) {
            return text;
        }

        @Override
        public String toString() {
            return toXML((XmlEnvironment) null);
        }

        @Override
        public String getElementName() {
            return null;
        }
    }
}
