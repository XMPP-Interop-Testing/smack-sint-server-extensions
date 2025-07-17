/*
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
package org.igniterealtime.smack.inttest.xep0410;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.muc.MucConfigFormManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Integration Tests for XEP-0410: MUC Self-Ping (Schrödinger's Chat)
 *
 * @see <a href="https://xmpp.org/extensions/xep-0410.html">XEP-0410</a>
 */
@SpecificationReference(document = "XEP-0410", version = "1.1.0")
public class MucSelfPingIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    private EntityBareJid testRoomAddress;
    private MultiUserChat ownerRoom;

    public MucSelfPingIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        this.environment = environment;
    }

    public void createRoom() throws TestNotPossibleException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        createRoom(false);
    }

    public void createRoom(boolean moderated) throws TestNotPossibleException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        final MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(conThree);
        final DomainBareJid mucDomain = mucManager.getMucServiceDomains().stream().findFirst().orElseThrow(() -> new TestNotPossibleException("Unable to find a MUC service domain"));

        try {
            final String roomNameLocal = String.join("-", "smack-inttest-xep0410", StringUtils.randomString(5));
            testRoomAddress = JidCreate.entityBareFrom(Localpart.from(roomNameLocal), mucDomain);
            ownerRoom = mucManager.getMultiUserChat(testRoomAddress);
            final MucConfigFormManager configFormManager = ownerRoom.create(Resourcepart.from("test-admin")).getConfigFormManager();
            if (moderated) {
                configFormManager.makeModerated();
            }
            configFormManager.submitConfigurationForm();

            final boolean supportsFeature = ServiceDiscoveryManager.getInstanceFor(conThree).supportsFeature(ownerRoom.getRoom(), "http://jabber.org/protocol/muc#self-ping-optimization");
            if (!supportsFeature) {
                throw new TestNotPossibleException("Rooms created on the service do not support the 'urn:xmpp:occupant-id:0' feature.");
            }
        } catch (Exception e) {
            if (e instanceof TestNotPossibleException) {
                throw (TestNotPossibleException) e;
            }
            throw new TestNotPossibleException("Unable to create MUC room.", e);
        }
    }

    public void removeRoom() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XmppStringprepException
    {
        if (ownerRoom != null) {
            ownerRoom.destroy();
        }
    }

    @SmackIntegrationTest(section = "3.3", quote = "a MUC service supporting this protocol may directly respond to a occupant's Ping request to the occupant's own nickname, as opposed to routing it to any of the occupant's clients. [...] it MUST respond to a self-ping request as follows: [...] Successful IQ response: the client is joined to the MUC.")
    public void testPingHandledByService() throws IOException, XMPPException, SmackException, InterruptedException, TimeoutException, InvocationTargetException, InstantiationException, IllegalAccessException, TestNotPossibleException
    {
        IQRequestHandler oldPingHandler = null;
        final Resourcepart nick = Resourcepart.from("nickA");

        try {
            // Setup test fixture.
            createRoom();
            MultiUserChatManager.getInstanceFor(conOne).getMultiUserChat(testRoomAddress).join(nick);

            final FullJid addressInRoom = JidCreate.fullFrom(testRoomAddress, nick);
            final Ping request = new Ping(addressInRoom);
            final AtomicBoolean wasInvoked = new AtomicBoolean(false);
            oldPingHandler = conOne.registerIQRequestHandler(new IQRequestHandler()
            {
                @Override
                public IQ handleIQRequest(IQ iqRequest) {
                    wasInvoked.set(true);
                    return new EmptyResultIQ(iqRequest);
                }

                @Override
                public Mode getMode() {
                    return Mode.sync;
                }

                @Override
                public IQ.Type getType() {
                    return IQ.Type.get;
                }

                @Override
                public String getElement() {
                    return Ping.ELEMENT;
                }

                @Override
                public String getNamespace() {
                    return Ping.NAMESPACE;
                }
            });

            // Execute system under test.
            final IQ response = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify results
            assertEquals(IQ.Type.result, response.getType(), "Expected a non-error response to the ping request that '" + conOne.getUser() + "' sent to its own nickname in room '" + addressInRoom + "' (but an error response was received.");
            assertFalse(wasInvoked.get(), "After sending a 'self-ping' to its own nickname in room '" + addressInRoom + "' the server should have processed the request on behalf of the user. Instead, '" + conOne.getUser() + "' received the ping request from the server.");
        } finally {
            removeRoom();
            if (oldPingHandler != null) {
                conOne.registerIQRequestHandler(oldPingHandler);
            }
        }
    }

    @SmackIntegrationTest(section = "3.3", quote = "a MUC service supporting this protocol may directly respond to a occupant's Ping request to the occupant's own nickname, as opposed to routing it to any of the occupant's clients. [...] it MUST respond to a self-ping request as follows: [...] Successful IQ response: the client is joined to the MUC.")
    public void testPingHandledByServiceMultiSessionNick() throws IOException, XMPPException, SmackException, InterruptedException, TimeoutException, InvocationTargetException, InstantiationException, IllegalAccessException, TestNotPossibleException
    {
        final AbstractXMPPConnection conOneSecondary = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
        final Resourcepart nickA = Resourcepart.from("nickA");
        final Resourcepart nickB = Resourcepart.from("nickB");

        IQRequestHandler oldPingHandlerA = null;
        IQRequestHandler oldPingHandlerB = null;

        try {
            // Setup test fixture.
            createRoom();

            conOneSecondary.connect();
            conOneSecondary.login(((AbstractXMPPConnection)conOne).getConfiguration().getUsername(), ((AbstractXMPPConnection)conOne).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));

            MultiUserChatManager.getInstanceFor(conOne).getMultiUserChat(testRoomAddress).join(nickA);
            MultiUserChatManager.getInstanceFor(conOneSecondary).getMultiUserChat(testRoomAddress).join(nickB);

            final FullJid addressInRoom = JidCreate.fullFrom(testRoomAddress, nickA);

            final AtomicBoolean wasInvokedA = new AtomicBoolean(false);
            oldPingHandlerA = conOne.registerIQRequestHandler(new IQRequestHandler()
            {
                @Override
                public IQ handleIQRequest(IQ iqRequest) {
                    wasInvokedA.set(true);
                    return new EmptyResultIQ(iqRequest);
                }

                @Override
                public Mode getMode() {
                    return Mode.sync;
                }

                @Override
                public IQ.Type getType() {
                    return IQ.Type.get;
                }

                @Override
                public String getElement() {
                    return Ping.ELEMENT;
                }

                @Override
                public String getNamespace() {
                    return Ping.NAMESPACE;
                }
            });

            final AtomicBoolean wasInvokedB = new AtomicBoolean(false);
            oldPingHandlerB = conOneSecondary.registerIQRequestHandler(new IQRequestHandler()
            {
                @Override
                public IQ handleIQRequest(IQ iqRequest) {
                    wasInvokedB.set(true);
                    return new EmptyResultIQ(iqRequest);
                }

                @Override
                public Mode getMode() {
                    return Mode.sync;
                }

                @Override
                public IQ.Type getType() {
                    return IQ.Type.get;
                }

                @Override
                public String getElement() {
                    return Ping.ELEMENT;
                }

                @Override
                public String getNamespace() {
                    return Ping.NAMESPACE;
                }
            });

            // Execute system under test.
            final IQ response = conOne.sendIqRequestAndWaitForResponse(new Ping(addressInRoom));

            // Verify results
            assertEquals(IQ.Type.result, response.getType(), "Expected a non-error response to the ping request that '" + conOne.getUser() + "' sent to its own nickname in room '" + addressInRoom + "' (but an error response was received.");
            assertFalse(wasInvokedA.get(), "After sending a 'self-ping' to its own nickname in room '" + addressInRoom + "' the server should have processed the request on behalf of the user. Instead, '" + conOne.getUser() + "' received the ping request from the server.");
            assertFalse(wasInvokedB.get(), "After sending a 'self-ping' to its own nickname in room '" + addressInRoom + "' the server should have processed the request on behalf of the user. Instead, '" + conOneSecondary.getUser() + "' (a different session for the same user, that joined the room using the same nickname) received the ping request from the server.");
        } finally {
            removeRoom();
            if (oldPingHandlerA != null) {
                conOne.registerIQRequestHandler(oldPingHandlerA);
            }
            if (oldPingHandlerB != null) {
                conOneSecondary.registerIQRequestHandler(oldPingHandlerB);
            }
            if (conOneSecondary != null) {
                conOneSecondary.disconnect();
            }
        }
    }

    @SmackIntegrationTest(section = "3.3", quote = "a MUC service supporting this protocol may directly respond to a occupant's Ping request to the occupant's own nickname, as opposed to routing it to any of the occupant's clients. [...] it MUST respond to a self-ping request as follows: [...] Error (<not-acceptable>): the client is not joined to the MUC.")
    public void testPingForNonOccupant() throws IOException, XMPPException, SmackException, InterruptedException, TestNotPossibleException
    {
        final Resourcepart nick = Resourcepart.from("nickA");

        try {
            // Setup test fixture.
            createRoom();

            final FullJid addressInRoom = JidCreate.fullFrom(testRoomAddress, nick);
            final Ping request = new Ping(addressInRoom);

            // Execute system under test & Verify results
            final XMPPException.XMPPErrorException error = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(request), "Expected an error response to the ping request that '" + conOne.getUser() + "' sent to an occupant-JID in room '" + addressInRoom + "' without being an actual occupant of that room(but no error was received.");
            assertEquals(StanzaError.Condition.not_acceptable, error.getStanzaError().getCondition(), "Unexpected condition in the (expected) error that was received to the ping request that '" + conOne.getUser() + "' sent to an occupant-JID in room '" + addressInRoom + "' without being an actual occupant of that room.");
        } finally {
            removeRoom();
        }
    }
}
