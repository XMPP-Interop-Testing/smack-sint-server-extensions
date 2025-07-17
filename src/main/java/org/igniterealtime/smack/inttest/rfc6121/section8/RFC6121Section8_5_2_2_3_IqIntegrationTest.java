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
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.AccountUtilities;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests that verify that behavior defined in section 8.5.2.2.3 "Local User / localpart@domainpart / No Available or Connected Resources / IQ" of section 8 "Server Rules for Processing XML Stanzas" of RFC6121.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section8_5_2_2_3_IqIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    /**
     * Address of an XMPP entity that is known to exist, but will not have any available or connected resources (it
     * doesn't have a connection to the server). This address is used as the target of the stanzas that are sent by
     * the tests in this class.
     */
    private final EntityBareJid entityWithoutResources;

    public RFC6121Section8_5_2_2_3_IqIntegrationTest(SmackIntegrationTestEnvironment environment) throws TestNotPossibleException
    {
        super(environment);
        this.environment = environment;

        try {
            final String userName = "tmp-test-user-" + StringUtils.randomString(5);
            AccountUtilities.createNonConnectedLocalUser(environment, userName, "secret");
            entityWithoutResources = JidCreate.entityBareFrom(Localpart.from(userName), environment.configuration.service);
        } catch (Throwable t) {
            throw new TestNotPossibleException("Unable to provision a test account.", t);
        }
    }

    @AfterClass
    public void tearDown()
    {
        try {
            AccountUtilities.removeNonConnectedLocalUser(environment, entityWithoutResources.getLocalpart().asUnescapedString(), "secret");
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SmackIntegrationTest(section = "8.5.2.2.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there are no available resources or connected resources associated with the user, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGet() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get);
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there are no available resources or connected resources associated with the user, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSet() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set);
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there are no available resources or connected resources associated with the user, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGet() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get);
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there are no available resources or connected resources associated with the user, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSet() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set);
    }

    /**
     * Executes a test in which conOne sends an IQ stanza of type 'get' or 'set' to the bare JID of conTwo.
     * conTwo has no resources online.
     *
     * The IQ request is almost guaranteed to not be understood by the server (as we've made a namespace up ourselves).
     * As such, the server is expected to respond with a specific error condition.
     *
     * @param iqType the type used in the IQ request stanza.
     */
    public void doTestUnsupportedIQ(final IQ.Type iqType) throws Exception
    {
        // Setup test fixture.
        final TestIQ testStanza = new TestIQ();
        testStanza.setType(iqType);
        final String needle = StringUtils.randomString(9);
        testStanza.setValue(needle);
        testStanza.setTo(entityWithoutResources);

        // Execute system under test.
        IQ testResponse;
        try {
            testResponse = conOne.sendIqRequestAndWaitForResponse(testStanza);
        } catch (XMPPException.XMPPErrorException e) {
            testResponse = (IQ) e.getStanza();
        }

        // Verify result.
        assertNotNull(testResponse, "Expected '" + conOne.getUser() + "' to have received a response (presumably generated by the server) to the IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that it sent to the bare JID of '" + entityWithoutResources + "' that does not have any online resources (but no response was received).");
        assertEquals(IQ.Type.error, testResponse.getType(), "Expected '" + conOne.getUser() + "' to have received an IQ error response (as the semantics of the qualifying namespace - which we've made up - cannot be understood by the server) to the IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that it sent to the bare JID of '" + entityWithoutResources + "' that does not have any online resources.");
        assertNotNull(testResponse.getError(), "Expected '" + conOne.getUser() + "' to have received an error response (as the semantics of the qualifying namespace - which we've made up - cannot be understood by the server) to the IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that it sent to the bare JID of '" + entityWithoutResources + "' that does not have any online resources(but the response did not contain an error).");
        assertEquals(StanzaError.Condition.service_unavailable, testResponse.getError().getCondition(), "Unexpected error condition in the expected error response received by '" + conOne.getUser() + "' after it sent an IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' using a qualifying namespace - which we've made up - that cannot be understood by the server) with ID '" + testStanza.getStanzaId() + "' to the bare JID of '" + entityWithoutResources + "' that does not have any online resources.");
    }

    /**
     * Executes a test in which conOne sends an IQ stanza of type 'get' or 'set' to the bare JID of conTwo.
     * conTwo has no resources online.
     *
     * The IQ request is guaranteed to be understood by the server (if it is not, a TestNotPossible exception is thrown).
     *
     * Verifies that none of the resources of the addressee received the request (the server is expected to answer on
     * behalf of the addressee).
     *
     * @param iqType the type used in the IQ request stanza.
     */
    public void doTestSupportedIQ(final IQ.Type iqType) throws Exception
    {
        if (!ServiceDiscoveryManager.getInstanceFor(conOne).supportsFeature(conOne.getXMPPServiceDomain(), "jabber:iq:roster")) {
            throw new TestNotPossibleException("The 'jabber:iq:roster' feature is not advertised by the server");
        }

        // Setup test fixture.
        final RosterPacket testStanza = new RosterPacket();
        testStanza.setType(iqType); // Neither a 'set' nor 'get' roster request to another person's JID is expected to change state of their roster (so it can be considered quite safe to use for testing purposes).
        testStanza.setTo(entityWithoutResources);

        // Execute system under test.
        IQ testResponse;
        try {
            testResponse = conOne.sendIqRequestAndWaitForResponse(testStanza);
        } catch (XMPPException.XMPPErrorException e) {
            testResponse = (IQ) e.getStanza();
        }

        // Verify result.
        assertNotNull(testResponse, "Expected '" + conOne.getUser() + "' to have received a response (presumably generated by the server) to the IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that it sent to the bare JID of '" + entityWithoutResources + "' that does not have any online resources (but no response was received).");
        assertTrue(testResponse.isResponseIQ(), "Expected '" + conOne.getUser() + "' to have received an IQ response to the IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that it sent to the bare JID of '" + entityWithoutResources + "' that does not have any online resources.");
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

    static class InternalProvider extends IqProvider<RFC6121Section8_5_2_1_3_IqIntegrationTest.TestIQ>
    {
        public InternalProvider() {
        }

        public RFC6121Section8_5_2_1_3_IqIntegrationTest.TestIQ parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException
        {
            RFC6121Section8_5_2_1_3_IqIntegrationTest.TestIQ answer = new RFC6121Section8_5_2_1_3_IqIntegrationTest.TestIQ();
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

