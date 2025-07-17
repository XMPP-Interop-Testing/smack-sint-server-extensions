/**
 * Copyright 2025 Guus der Kinderen
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
package org.igniterealtime.smack.inttest.rfc6121.section8;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 8.5.1 "Local User / No Such User" of section 8 "Server Rules for Processing XML Stanzas" of RFC6121.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section8_5_1_IntegrationTest extends AbstractSmackIntegrationTest
{
    public RFC6121Section8_5_1_IntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a bare JID <localpart@domainpart> [...] where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testLocalDomainNoSuchUserIQBareJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final BareJid localDomainNoSuchUser = JidCreate.bareFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain());
        final IQ outboundStanza = new Ping();
        outboundStanza.setTo(localDomainNoSuchUser);

        // Execute system under test.
        try {
            final IQ response = conOne.sendIqRequestAndWaitForResponse(outboundStanza);

            // Verify result.
            fail("Expected '" + conOne.getUser() + "' to receive a stanza error after sending an IQ request addressed to a bare JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server. A (non-error) IQ result was received instead.");
        } catch (XMPPException.XMPPErrorException e) {
            assertEquals(StanzaError.Condition.service_unavailable, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was received by '" + conOne.getUser() + "' after sending an IQ request addressed to a bare JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server.");
        } catch (SmackException.NoResponseException e) {
            fail("Expected '" + conOne.getUser() + "' to receive a stanza error after sending an IQ request addressed to a bare JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server. Instead, no response was received at all.");
        }
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a [...] full JID <localpart@domainpart/resourcepart> where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testLocalDomainNoSuchUserIQFullJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final FullJid localDomainNoSuchUser = JidCreate.fullFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain(), Resourcepart.from(StringUtils.randomString(5)));
        final IQ outboundStanza = new Ping();
        outboundStanza.setTo(localDomainNoSuchUser);

        // Execute system under test.
        try {
            final IQ response = conOne.sendIqRequestAndWaitForResponse(outboundStanza);

            // Verify result.
            fail("Expected '" + conOne.getUser() + "' to receive a stanza error after sending an IQ request addressed to a full JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server. A (non-error) IQ result was received instead.");
        } catch (XMPPException.XMPPErrorException e) {
            assertEquals(StanzaError.Condition.service_unavailable, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was received by '" + conOne.getUser() + "' after sending an IQ request addressed to a full JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server.");
        } catch (SmackException.NoResponseException e) {
            fail("Expected '" + conOne.getUser() + "' to receive a stanza error after sending an IQ request addressed to a full JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server. Instead, no response was received at all.");
        }
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a bare JID <localpart@domainpart> [...] where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a message stanza, the server MUST either (a) silently ignore the message or (b) return a <service-unavailable/> stanza error to the sender.")
    public void testLocalDomainNoSuchUserMessageBareJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final BareJid localDomainNoSuchUser = JidCreate.bareFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain());
        final Message outboundStanza = MessageBuilder.buildMessage().setBody("Test message " + StringUtils.randomString(3)).to(localDomainNoSuchUser).build().asBuilder(StringUtils.randomString(9)).build();

        // Execute system under test.
        final StanzaCollector collector = conOne.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(outboundStanza), new FromMatchesFilter(localDomainNoSuchUser, true)), outboundStanza);

        // Verify result.
        final IQ request = new Ping();
        request.setTo(conOne.getUser());
        conOne.sendIqRequestAndWaitForResponse(request); // Wait until the _next_ stanza is processed. The original stanza must have been processed by then.

        collector.cancel();

        final List<Stanza> collected = collector.getCollectedStanzasAfterCancelled();
        if (!collected.isEmpty()) {
            if (collected.size() > 1) {
                throw new IllegalStateException("Unexpected amount of stanzas collected!"); // This likely is a bug in the implementation of this test.
            }
            final Stanza received = collected.get(0);
            assertNotNull(received.getError(), "Expected the stanza received by '" + conOne.getUser() + "' to contain an error, after it sent a message addressed to a bare JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server (but the stanza that was received didn't contain an error).");
            assertEquals(StanzaError.Condition.service_unavailable, received.getError().getCondition(), "Unexpected error condition in the (expected) error that was received by '" + conOne.getUser() + "', after it sent a message addressed to a bare JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server (but the stanza that was received didn't contain an error).");
        }
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a [...] full JID <localpart@domainpart/resourcepart> where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a message stanza, the server MUST either (a) silently ignore the message or (b) return a <service-unavailable/> stanza error to the sender.")
    public void testLocalDomainNoSuchUserMessageFullJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final FullJid localDomainNoSuchUser = JidCreate.fullFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain(), Resourcepart.from(StringUtils.randomString(5)));
        final Message outboundStanza = MessageBuilder.buildMessage().setBody("Test message " + StringUtils.randomString(3)).to(localDomainNoSuchUser).build().asBuilder(StringUtils.randomString(9)).build();

        // Execute system under test.
        final StanzaCollector collector = conOne.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(outboundStanza), new FromMatchesFilter(localDomainNoSuchUser, true)), outboundStanza);

        // Verify result.
        final IQ request = new Ping();
        request.setTo(conOne.getUser());
        conOne.sendIqRequestAndWaitForResponse(request); // Wait until the _next_ stanza is processed. The original stanza must have been processed by then.

        collector.cancel();

        final List<Stanza> collected = collector.getCollectedStanzasAfterCancelled();
        if (!collected.isEmpty()) {
            if (collected.size() > 1) {
                throw new IllegalStateException("Unexpected amount of stanzas collected!"); // This likely is a bug in the implementation of this test.
            }
            final Stanza received = collected.get(0);
            assertNotNull(received.getError(), "Expected the stanza received by '" + conOne.getUser() + "' to contain an error, after it sent a message addressed to a full JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server (but the stanza that was received didn't contain an error).");
            assertEquals(StanzaError.Condition.service_unavailable, received.getError().getCondition(), "Unexpected error condition in the (expected) error that was received by '" + conOne.getUser() + "', after it sent a message addressed to a full JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server (but the stanza that was received didn't contain an error).");
        }
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a bare JID <localpart@domainpart> [...] where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceNoTypeBareJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final BareJid localDomainNoSuchUser = JidCreate.bareFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain());
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, null);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a [...] full JID <localpart@domainpart/resourcepart> where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceNoTypeFullJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final FullJid localDomainNoSuchUser = JidCreate.fullFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain(), Resourcepart.from(StringUtils.randomString(5)));
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, null);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a bare JID <localpart@domainpart> [...] where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\" the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceUnavailableTypeBareJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final BareJid localDomainNoSuchUser = JidCreate.bareFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain());
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, Presence.Type.unavailable);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a [...] full JID <localpart@domainpart/resourcepart> where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\" the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceUnavailableTypeFullJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        final FullJid localDomainNoSuchUser = JidCreate.fullFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain(), Resourcepart.from(StringUtils.randomString(5)));
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, Presence.Type.unavailable);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a bare JID <localpart@domainpart> [...] where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"subscribe\" [...] the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceSubscribeTypeBareJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final BareJid localDomainNoSuchUser = JidCreate.bareFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain());
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, Presence.Type.subscribe);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a [...] full JID <localpart@domainpart/resourcepart> where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"subscribe\" [...] the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceSubscribeTypeFullJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        final FullJid localDomainNoSuchUser = JidCreate.fullFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain(), Resourcepart.from(StringUtils.randomString(5)));
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, Presence.Type.subscribe);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a bare JID <localpart@domainpart> [...] where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"subscribed\" [...] the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceSubscribedTypeBareJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final BareJid localDomainNoSuchUser = JidCreate.bareFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain());
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, Presence.Type.subscribed);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a [...] full JID <localpart@domainpart/resourcepart> where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"subscribed\" [...] the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceSubscribedTypeFullJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        final FullJid localDomainNoSuchUser = JidCreate.fullFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain(), Resourcepart.from(StringUtils.randomString(5)));
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, Presence.Type.subscribed);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a bare JID <localpart@domainpart> [...] where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...] the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceUnsubscribeTypeBareJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final BareJid localDomainNoSuchUser = JidCreate.bareFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain());
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, Presence.Type.unsubscribe);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a [...] full JID <localpart@domainpart/resourcepart> where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...] the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceUnsubscribeTypeFullJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        final FullJid localDomainNoSuchUser = JidCreate.fullFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain(), Resourcepart.from(StringUtils.randomString(5)));
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, Presence.Type.unsubscribe);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a bare JID <localpart@domainpart> [...] where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unsubscribed\" the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceUnsubscribedTypeBareJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final BareJid localDomainNoSuchUser = JidCreate.bareFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain());
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, Presence.Type.unsubscribed);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a [...] full JID <localpart@domainpart/resourcepart> where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unsubscribed\" the server MUST silently ignore the stanza.")
    public void testLocalDomainNoSuchUserPresenceUnsubscribedTypeFullJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        final FullJid localDomainNoSuchUser = JidCreate.fullFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain(), Resourcepart.from(StringUtils.randomString(5)));
        doTestLocalDomainNoSuchUserPresenceOfType(localDomainNoSuchUser, Presence.Type.unsubscribed);
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a bare JID <localpart@domainpart> [...] where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"probe\", the server MUST either (a) silently ignore the stanza or (b) return a presence stanza of type \"unsubscribed\".")
    public void testLocalDomainNoSuchUserPresenceProbeBareJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        // Setup test fixture.
        final BareJid localDomainNoSuchUser = JidCreate.bareFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain());
        final Presence outboundStanza = PresenceBuilder.buildPresence(StringUtils.randomString(9)).ofType(Presence.Type.probe).to(localDomainNoSuchUser).build();

        // Execute system under test.
        final StanzaCollector collector = conOne.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(outboundStanza), new FromMatchesFilter(localDomainNoSuchUser, true)), outboundStanza);

        // Verify result.
        final IQ request = new Ping();
        request.setTo(conOne.getUser());
        conOne.sendIqRequestAndWaitForResponse(request); // Wait until the _next_ stanza is processed. The original stanza must have been processed by then.

        collector.cancel();

        final List<Stanza> collected = collector.getCollectedStanzasAfterCancelled();
        if (!collected.isEmpty()) {
            if (collected.size() > 1) {
                throw new IllegalStateException("Unexpected amount of stanzas collected!"); // This likely is a bug in the implementation of this test.
            }
            final Presence received = (Presence) collected.get(0);
            assertEquals(Presence.Type.unsubscribed, received.getType(), "Unexpected type in the presence stanza that was received by '" + conOne.getUser() + "', after it sent a presence probe to a full JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server.");
        }
    }

    @SmackIntegrationTest(section = "8.5.1", quote = "If the 'to' address specifies a [...] full JID <localpart@domainpart/resourcepart> where the domainpart of the JID matches a configured domain that is serviced by the server itself, the server MUST proceed as follows. [...] If the user account identified by the 'to' attribute does not exist, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"probe\", the server MUST either (a) silently ignore the stanza or (b) return a presence stanza of type \"unsubscribed\".")
    public void testLocalDomainNoSuchUserPresenceProbeFullJid() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException
    {
        final FullJid localDomainNoSuchUser = JidCreate.fullFrom(Localpart.from("nonexistingusername-" + StringUtils.randomString(5)), conOne.getXMPPServiceDomain(), Resourcepart.from(StringUtils.randomString(5)));
        final Presence outboundStanza = PresenceBuilder.buildPresence(StringUtils.randomString(9)).ofType(Presence.Type.probe).to(localDomainNoSuchUser).build();

        // Execute system under test.
        final StanzaCollector collector = conOne.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(outboundStanza), new FromMatchesFilter(localDomainNoSuchUser, true)), outboundStanza);

        // Verify result.
        final IQ request = new Ping();
        request.setTo(conOne.getUser());
        conOne.sendIqRequestAndWaitForResponse(request); // Wait until the _next_ stanza is processed. The original stanza must have been processed by then.

        collector.cancel();

        final List<Stanza> collected = collector.getCollectedStanzasAfterCancelled();
        if (!collected.isEmpty()) {
            if (collected.size() > 1) {
                throw new IllegalStateException("Unexpected amount of stanzas collected!"); // This likely is a bug in the implementation of this test.
            }
            final Presence received = (Presence) collected.get(0);
            assertEquals(Presence.Type.unsubscribed, received.getType(), "Unexpected type in the presence stanza that was received by '" + conOne.getUser() + "', after it sent a presence probe to a bare JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server.");
        }
    }

    void doTestLocalDomainNoSuchUserPresenceOfType(final Jid localDomainNoSuchUser, final Presence.Type type) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final PresenceBuilder presenceBuilder = PresenceBuilder.buildPresence(StringUtils.randomString(9)).to(localDomainNoSuchUser);
        if (type != null) {
            presenceBuilder.ofType(type);
        }
        final Presence outboundStanza = presenceBuilder.build();

        // Execute system under test.
        final StanzaCollector collector = conOne.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(outboundStanza), new FromMatchesFilter(localDomainNoSuchUser, true)), outboundStanza);

        // Verify result.
        final IQ request = new Ping();
        request.setTo(conOne.getUser());
        conOne.sendIqRequestAndWaitForResponse(request); // Wait until the _next_ stanza is processed. The original stanza must have been processed by then.

        collector.cancel();

        if (collector.getCollectedCount() != 0) {
            fail("Expected the presence stanza (" + (type == null ? "without a 'type' attribute" : ("of type '" + type + "'")) + ") addressed to a " + (localDomainNoSuchUser.hasResource() ? "full" : "bare") + " JID '" + outboundStanza.getTo() + "', which is referencing a non-existing user account on the local server, sent by '" + conOne.getUser() + "' to be ignored by the server (but a response was received).");
        }
    }
}
