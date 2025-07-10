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

import org.igniterealtime.smack.inttest.*;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.AccountUtilities;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 8.5.2.2.1 "Local User / localpart@domainpart / No Available or Connected Resources / Message" of section 8 "Server Rules for Processing XML Stanzas" of RFC6121.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section8_5_2_2_1_MessageIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    /**
     * Address of an XMPP entity that is known to exist, but will not have any available or connected resources (it
     * doesn't have a connection to the server). This address is used as the target of the stanzas that are sent by
     * the tests in this class.
     */
    private final EntityBareJid entityWithoutResources;

    public RFC6121Section8_5_2_2_1_MessageIntegrationTest(SmackIntegrationTestEnvironment environment) throws TestNotPossibleException
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
    public void tearDown() {
        try {
            AccountUtilities.removeNonConnectedLocalUser(environment, entityWithoutResources.getLocalpart().asUnescapedString(), "secret");
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // 'normal' and 'chat' types have a specification that is defined as a SHOULD (as opposed to a MUST) and is therefor not tested by this implementation.

    @SmackIntegrationTest(section = "8.5.2.2.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there are no available resources or connected resources associated with the user, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST return an error to the sender")
    public void testGroupchat() throws Exception
    {
        // Setup test fixture: detect an error that is sent back to the sender.
        StanzaListener errorListener = null;
        try {
            final String needle = StringUtils.randomString(9);

            final StanzaFilter errorDetector = new AndFilter((s -> s instanceof Message && ((Message) s).getType() == Message.Type.error && ((Message) s).getBody().equals(needle)));
            final SimpleResultSyncPoint errorReceivedBySender = new SimpleResultSyncPoint();
            errorListener = (stanza) -> errorReceivedBySender.signal();
            conOne.addStanzaListener(errorListener, errorDetector);

            // Execute system under test.
            final Message testStanza = StanzaBuilder.buildMessage()
                .ofType(Message.Type.groupchat)
                .to(entityWithoutResources)
                .setBody(needle)
                .build();

            conOne.sendStanza(testStanza);

            // Verify result
            assertResult(errorReceivedBySender, "Expected '" + conOne.getUser() + "' to receive an error after trying to send a message stanza of type '" + testStanza.getType() + "' to the bare JID of '" + entityWithoutResources + "' that is known to not have any available or connected resources (but no error was received)." );
        } finally {
            // Tear down test fixture.
            if (errorListener != null) { conOne.removeStanzaListener(errorListener); }
        }
    }

    @SmackIntegrationTest(section = "8.5.2.2.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there are no available resources or connected resources associated with the user, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\" [...], the server MUST silently ignore the message.")
    public void testHeadline() throws Exception
    {
        doTestExpectingSilentIgnore(Message.Type.headline);
    }

    @SmackIntegrationTest(section = "8.5.2.2.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there are no available resources or connected resources associated with the user, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"error\", the server MUST silently ignore the message.")
    public void testError() throws Exception
    {
        doTestExpectingSilentIgnore(Message.Type.error);
    }

    public void doTestExpectingSilentIgnore(final Message.Type messageType) throws Exception
    {
        // Setup test fixture: detect an error that is sent back to the sender.
        StanzaListener errorListener = null;
        try {
            final String needle = StringUtils.randomString(9);

            // Setup test fixture: detect an error that is sent back to the sender.
            final StanzaFilter errorDetector = new AndFilter((s -> s instanceof Message && ((Message) s).getType() == Message.Type.error && ((Message) s).getBody().equals(needle)));
            final Stanza[] errorReceivedBySender = {null};
            errorListener = (stanza) -> errorReceivedBySender[0] = stanza;
            conOne.addStanzaListener(errorListener, errorDetector);

            // Execute system under test.
            final Message testStanza = StanzaBuilder.buildMessage()
                .ofType(messageType)
                .to(entityWithoutResources)
                .setBody(needle)
                .build();

            conOne.sendStanza(testStanza);

            PingManager.getInstanceFor(conOne).pingMyServer(); // No matter if the server supports ping or not, when the request gets responded to, processing of testStanza must already have been concluded.

            // Verify result
            assertNull(errorReceivedBySender[0], "Expected the stanza that was sent by '" + conOne.getUser() + "',a message stanza of type '" + testStanza.getType() + "' sent to the bare JID of '" + entityWithoutResources + "' that is known to not have any available or connected resources, to be silently ignored. However the sender received an error.");
        } finally {
            // Tear down test fixture.
            if (errorListener != null) {
                conOne.removeStanzaListener(errorListener);
            }
        }
    }
}
