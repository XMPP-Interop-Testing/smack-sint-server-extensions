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
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 8.5.3.1 "Local User / localpart@domainpart/resourcepart / Resource Matches" of section 8 "Server Rules for Processing XML Stanzas" of RFC6121.
 *
 * Note: This implementation contains tests for all stanza types (IQ, Message, Presence). For Presence, this <em>only</em> tests the processing of presence stanzas with no type or of type 'unavailable'. All other types are subject of tests under other sections (Section 3 and Section 4.3), to which section 8.5.3.1. references (and which have a much more detailed definition of the desired functionality).
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section8_5_3_1_IntegrationTest extends AbstractSmackIntegrationTest
{
    public RFC6121Section8_5_3_1_IntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);

        ProviderManager.addIQProvider(TestIQ.ELEMENT, TestIQ.NAMESPACE, new InternalProvider());
    }

    @SmackIntegrationTest(section = "8.5.3.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If an available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza of type \"result\" or \"error\", the server MUST deliver the stanza to the resource.")
    public void testIqErrorUnsupported() throws Exception
    {
        // Setup test fixture.
        final TestIQ stanzaToSend = new TestIQ();
        stanzaToSend.setType(IQ.Type.error);
        stanzaToSend.setError(StanzaError.getBuilder(StanzaError.Condition.undefined_condition).build());
        final String needle = StringUtils.randomString(9);
        stanzaToSend.setStanzaId(needle);
        stanzaToSend.setValue(needle);
        stanzaToSend.setTo(conTwo.getUser().asFullJidOrThrow());

        final SimpleResultSyncPoint stanzaReceived = new SimpleResultSyncPoint();
        final StanzaListener stanzaListener = packet -> stanzaReceived.signal();
        conTwo.addStanzaListener(stanzaListener, new AndFilter(StanzaTypeFilter.IQ, IQTypeFilter.ERROR, new StanzaIdFilter(stanzaToSend.getStanzaId())));
        try
        {
            // Execute System Under Test.
            conOne.sendStanza(stanzaToSend);

            // Verify result.
            assertResult(stanzaReceived, "Expected '" + conTwo.getUser() + "' to receive the IQ stanza of type '" + stanzaToSend.getType() + "' with stanza ID '" + stanzaToSend.getStanzaId() + "' that was sent to its full JID by '" + conOne.getUser() + "' (but the stanza was not received).");
        } finally {
            // Tear down test fixture.
            conTwo.removeStanzaListener(stanzaListener);
        }
    }

    @SmackIntegrationTest(section = "8.5.3.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If an available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza of type \"result\" or \"error\", the server MUST deliver the stanza to the resource.")
    public void testIqResultUnsupported() throws Exception
    {
        // Setup test fixture.
        final TestIQ stanzaToSend = new TestIQ();
        stanzaToSend.setType(IQ.Type.result);
        final String needle = StringUtils.randomString(9);
        stanzaToSend.setStanzaId(needle);
        stanzaToSend.setValue(needle);
        stanzaToSend.setTo(conTwo.getUser().asFullJidOrThrow());

        final SimpleResultSyncPoint stanzaReceived = new SimpleResultSyncPoint();
        final StanzaListener stanzaListener = packet -> stanzaReceived.signal();
        conTwo.addStanzaListener(stanzaListener, new AndFilter(StanzaTypeFilter.IQ, IQTypeFilter.RESULT, new StanzaIdFilter(stanzaToSend.getStanzaId())));
        try
        {
            // Execute System Under Test.
            conOne.sendStanza(stanzaToSend);

            // Verify result.
            assertResult(stanzaReceived, "Expected '" + conTwo.getUser() + "' to receive the IQ stanza of type '" + stanzaToSend.getType() + "' with stanza ID '" + stanzaToSend.getStanzaId() + "' that was sent to its full JID by '" + conOne.getUser() + "' (but the stanza was not received).");
        } finally {
            // Tear down test fixture.
            conTwo.removeStanzaListener(stanzaListener);
        }
    }

    @SmackIntegrationTest(section = "8.5.3.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If an available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza of type \"result\" or \"error\", the server MUST deliver the stanza to the resource.")
    public void testIqErrorSupported() throws Exception
    {
        if (!ServiceDiscoveryManager.getInstanceFor(conOne).supportsFeature(conOne.getXMPPServiceDomain(), "jabber:iq:roster")) {
            throw new TestNotPossibleException("The 'jabber:iq:roster' feature is not advertised by the server");
        }

        // Setup test fixture.
        final RosterPacket stanzaToSend = new RosterPacket();
        stanzaToSend.setType(IQ.Type.error);
        stanzaToSend.setError(StanzaError.getBuilder(StanzaError.Condition.undefined_condition).build());
        final String needle = StringUtils.randomString(9);
        stanzaToSend.setStanzaId(needle);
        stanzaToSend.setTo(conTwo.getUser().asFullJidOrThrow());

        final SimpleResultSyncPoint stanzaReceived = new SimpleResultSyncPoint();
        final StanzaListener stanzaListener = packet -> stanzaReceived.signal();
        conTwo.addStanzaListener(stanzaListener, new AndFilter(StanzaTypeFilter.IQ, IQTypeFilter.ERROR, new StanzaIdFilter(stanzaToSend.getStanzaId())));
        try
        {
            // Execute System Under Test.
            conOne.sendStanza(stanzaToSend);

            // Verify result.
            assertResult(stanzaReceived, "Expected '" + conTwo.getUser() + "' to receive the IQ stanza of type '" + stanzaToSend.getType() + "' with stanza ID '" + stanzaToSend.getStanzaId() + "' that was sent to its full JID by '" + conOne.getUser() + "' (but the stanza was not received).");
        } finally {
            // Tear down test fixture.
            conTwo.removeStanzaListener(stanzaListener);
        }
    }

    @SmackIntegrationTest(section = "8.5.3.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If an available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza of type \"result\" or \"error\", the server MUST deliver the stanza to the resource.")
    public void testIqResultSupported() throws Exception
    {
        if (!ServiceDiscoveryManager.getInstanceFor(conOne).supportsFeature(conOne.getXMPPServiceDomain(), "jabber:iq:roster")) {
            throw new TestNotPossibleException("The 'jabber:iq:roster' feature is not advertised by the server");
        }

        // Setup test fixture.
        final RosterPacket stanzaToSend = new RosterPacket();
        stanzaToSend.setType(IQ.Type.result);
        final String needle = StringUtils.randomString(9);
        stanzaToSend.setStanzaId(needle);
        stanzaToSend.setTo(conTwo.getUser().asFullJidOrThrow());

        final SimpleResultSyncPoint stanzaReceived = new SimpleResultSyncPoint();
        final StanzaListener stanzaListener = packet -> stanzaReceived.signal();
        conTwo.addStanzaListener(stanzaListener, new AndFilter(StanzaTypeFilter.IQ, IQTypeFilter.RESULT, new StanzaIdFilter(stanzaToSend.getStanzaId())));
        try
        {
            // Execute System Under Test.
            conOne.sendStanza(stanzaToSend);

            // Verify result.
            assertResult(stanzaReceived, "Expected '" + conTwo.getUser() + "' to receive the IQ stanza of type '" + stanzaToSend.getType() + "' with stanza ID '" + stanzaToSend.getStanzaId() + "' that was sent to its full JID by '" + conOne.getUser() + "' (but the stanza was not received).");
        } finally {
            // Tear down test fixture.
            conTwo.removeStanzaListener(stanzaListener);
        }
    }

    @SmackIntegrationTest(section = "8.5.3.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If an available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza, the server MUST deliver the stanza to the resource.")
    public void testMessageNormal() throws Exception
    {
        doTestMessage(Message.Type.normal);
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If an available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza, the server MUST deliver the stanza to the resource.")
    public void testMessageHeadline() throws Exception
    {
        doTestMessage(Message.Type.headline);
    }

    @SmackIntegrationTest(section = "8.5.3.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If an available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza, the server MUST deliver the stanza to the resource.")
    public void testMessageError() throws Exception
    {
        doTestMessage(Message.Type.error);
    }

    @SmackIntegrationTest(section = "8.5.3.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If an available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza, the server MUST deliver the stanza to the resource.")
    public void testMessageChat() throws Exception
    {
        doTestMessage(Message.Type.chat);
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If an available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza, the server MUST deliver the stanza to the resource.")
    public void testMessageGroupchat() throws Exception
    {
        doTestMessage(Message.Type.groupchat);
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If an available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...], the server MUST deliver the stanza to the resource.")
    public void testPresenceAvailable() throws Exception
    {
        doTestPresence(Presence.Type.available);
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If an available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST deliver the stanza to the resource.")
    public void testPresenceUnavailable() throws Exception
    {
        doTestPresence(Presence.Type.unavailable);
    }

    private void doTestMessage(final Message.Type type) throws SmackException.NotConnectedException, InterruptedException
    {
        // Setup test fixture.
        final String needle = StringUtils.randomString(9);
        final Message stanzaToSend = MessageBuilder.buildMessage(needle)
            .ofType(type)
            .to(conTwo.getUser().asFullJidOrThrow())
            .addBody("en", "Message sent by an integration test.")
            .build();

        final SimpleResultSyncPoint stanzaReceived = new SimpleResultSyncPoint();
        final StanzaListener stanzaListener = packet -> stanzaReceived.signal();
        conTwo.addStanzaListener(stanzaListener, new AndFilter(StanzaTypeFilter.MESSAGE, new StanzaIdFilter(stanzaToSend.getStanzaId())));
        try
        {
            // Execute System Under Test.
            conOne.sendStanza(stanzaToSend);

            // Verify result.
            assertResult(stanzaReceived, "Expected '" + conTwo.getUser() + "' to receive the Message stanza of type '" + stanzaToSend.getType() + "' with stanza ID '" + stanzaToSend.getStanzaId() + "' that was sent to its full JID by '" + conOne.getUser() + "' (but the stanza was not received).");
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            // Tear down test fixture.
            conTwo.removeStanzaListener(stanzaListener);
        }
    }

    private void doTestPresence(final Presence.Type type) throws SmackException.NotConnectedException, InterruptedException
    {
        // Setup test fixture.
        final String needle = StringUtils.randomString(9);
        final Presence stanzaToSend = PresenceBuilder.buildPresence(needle)
            .ofType(type)
            .to(conTwo.getUser().asFullJidOrThrow())
            .setStatus("Integration test")
            .build();

        final SimpleResultSyncPoint stanzaReceived = new SimpleResultSyncPoint();
        final StanzaListener stanzaListener = packet -> stanzaReceived.signal();
        conTwo.addStanzaListener(stanzaListener, new AndFilter(StanzaTypeFilter.PRESENCE, new StanzaIdFilter(stanzaToSend.getStanzaId())));
        try
        {
            // Execute System Under Test.
            conOne.sendStanza(stanzaToSend);

            // Verify result.
            assertResult(stanzaReceived, "Expected '" + conTwo.getUser() + "' to receive the Presence stanza of type '" + stanzaToSend.getType() + "' with stanza ID '" + stanzaToSend.getStanzaId() + "' that was sent to its full JID by '" + conOne.getUser() + "' (but the stanza was not received).");
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            // Tear down test fixture.
            conTwo.removeStanzaListener(stanzaListener);
        }
    }

    static class TestIQ extends SimpleIQ
    {
        public static final String ELEMENT = "test";
        public static final String NAMESPACE = "urn:xmpp-interop:test:0";

        private String value;

        public TestIQ() {
            super(ELEMENT, NAMESPACE);
        }

        @SuppressWarnings("this-escape")
        public TestIQ(final Jid to) {
            this();
            setTo(to);
            setType(IQ.Type.get);
        }

        public TestIQ(final XMPPConnection connection, final Jid to) {
            this(connection.getStanzaFactory().buildIqData(), to);
        }

        public TestIQ(final IqData iqBuilder, final Jid to) {
            super(iqBuilder.to(to).ofType(IQ.Type.get), ELEMENT, NAMESPACE);
        }

        public TestIQ(final IqData iqBuilder) {
            super(iqBuilder, ELEMENT, NAMESPACE);
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    static class InternalProvider extends IqProvider<TestIQ>
    {
        public InternalProvider() {
        }

        public TestIQ parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException
        {
            TestIQ answer = new TestIQ();
            boolean done = false;

            while(!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    String value = parser.nextText();
                    answer.setValue(value);
                    PacketParserUtils.addExtensionElement(answer, parser, xmlEnvironment);
                } else if (eventType == XmlPullParser.Event.END_ELEMENT && parser.getName().equals("test")) {
                    done = true;
                }
            }

            return answer;
        }
    }
}
