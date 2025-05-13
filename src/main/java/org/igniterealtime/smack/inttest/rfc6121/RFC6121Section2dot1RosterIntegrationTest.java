package org.igniterealtime.smack.inttest.rfc6121;

import com.sun.jdi.connect.TransportTimeoutException;
import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.XmppConnectionManager;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.parsing.ParsingExceptionCallback;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 2.1 "Syntax and Semantics" of section 2 "Managing the Roster" of RFC6121.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section2dot1RosterIntegrationTest extends AbstractSmackIntegrationTest
{
    public RFC6121Section2dot1RosterIntegrationTest(SmackIntegrationTestEnvironment environment) throws SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);

        try {
            conOne.sendIqRequestAndWaitForResponse(new RosterPacket());
        } catch (XMPPException.XMPPErrorException e) {
            if (e.getStanzaError().getCondition() == StanzaError.Condition.service_unavailable) {
                throw new TestNotPossibleException("Server does not support the roster namespace."); // This error is defined in RFC6121 Section 2.2
            }
        }
    }

    @SmackIntegrationTest(section = "2.1.5", quote = "The following rules apply to roster sets: [...] The server MUST ignore any value of the 'subscription' attribute other than \"remove\"")
    public void testRosterSetWithInvalidSubscriptionAttribute() throws XmppStringprepException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final SimpleIQ request = new SimpleIQ(RosterPacket.ELEMENT, RosterPacket.NAMESPACE) { // Smack's RosterPacket doesn't allow us to set the same multiple items.
            @Override
            protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
                xml.rightAngleBracket();

                xml.halfOpenElement(RosterPacket.Item.ELEMENT);
                xml.attribute("jid", target);
                xml.attribute("subscription", "both");
                xml.closeEmptyElement();
                return xml;
            }
        };
        request.setType(IQ.Type.set);
        final Roster rosterOne = Roster.getInstanceFor(conOne);

        // Execute system under test
        try {
            final IQ result = conOne.sendIqRequestAndWaitForResponse(request);

            // Verify result
            assertEquals(IQ.Type.result, result.getType(), "Unexpected response type received by '" + conOne.getUser() + "' after it sent a Roster Set that included a subscription attribute with a value of 'both'. It was expected that the server would ignore this value.");

            final RosterEntry entry = rosterOne.getEntry(target);
            assertNotNull(entry, "Expected the roster of '" + conOne.getUser() + "' to contain an entry for '" + target + "', even if it was added with an invalid subscription attribute. The expectation is for the server to ignore this value. However, the roster entry was not found.");
            assertNotEquals(RosterPacket.ItemType.both, entry.getType(), "Expected the roster of '" + conOne.getUser() + "' to contain an entry for '" + target + "' that doesn't have the value used while setting the roster item. The expectation is for the server to ignore this value. However, the roster entry uses to value that was expected to be ignored.");
        } catch (XMPPException.XMPPErrorException e) {
            fail("Unexpected response type received by '" + conOne.getUser() + "' after it sent a Roster Set that included a subscription attribute with a value of 'both'. It was expected that the server would ignore this value (instead, an error was received): " + e.getStanzaError());
        } finally {
            // Tear down test fixture
            final Roster roster = Roster.getInstanceFor(conOne);
            final RosterEntry entry = roster.getEntry(target);
            if (entry != null) {
                roster.removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.1.5", quote = "The following rules apply to roster sets: [...] The server MUST ignore any value of the 'subscription' attribute other than \"remove\"")
    public void testRosterSetWithIrregularSubscriptionAttribute() throws Exception
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final SimpleIQ request = new SimpleIQ(RosterPacket.ELEMENT, RosterPacket.NAMESPACE) { // Smack's RosterPacket doesn't allow us to set the same multiple items.
            @Override
            protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
                xml.rightAngleBracket();

                xml.halfOpenElement(RosterPacket.Item.ELEMENT);
                xml.attribute("jid", target);
                xml.attribute("subscription", "foobar");
                xml.closeEmptyElement();
                return xml;
            }
        };
        request.setType(IQ.Type.set);

        // We will wait for one of two events: Smack throwing an unparsable stanza exception, or the server returning a stanza (that _can_ be parsed by Smack)
        final UnparseableStanza[] unparsableData = {null}; // Set if Smack CAN NOT parse the server response
        final IQ[] result = {null}; // Set if Smack CAN parse the server response
        final Exception[] exception = {null};

        final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint(); // Triggered by either event.

        final ParsingExceptionCallback oldParsingExceptionCallback = ((AbstractXMPPConnection) conOne).getParsingExceptionCallback();
        ((AbstractXMPPConnection) conOne).setParsingExceptionCallback(stanzaData -> {
            unparsableData[0] = stanzaData;
            syncPoint.signal();
        });

        // Execute system under test
        try {
            final SmackFuture<IQ, Exception> iqExceptionSmackFuture = conOne.sendIqRequestAsync(request);
            iqExceptionSmackFuture.onCompletion(r -> {
               result[0] = r.getIfAvailable();
               exception[0] = r.getExceptionIfAvailable();
               syncPoint.signal();
            });

            syncPoint.waitForResult(timeout);

            // Verify result
            if (unparsableData[0] != null) {
                throw new TestNotPossibleException("Smack was unable to parse the data sent by the server: " + unparsableData[0].getContent());
            } else if (exception[0] != null) {
                fail("Unexpected response type received by '" + conOne.getUser() + "' after it sent a Roster Set that included a subscription attribute with a value of 'both'. It was expected that the server would ignore this value (instead, an error was received): " + exception[0]);
            } else {
                assertEquals(IQ.Type.result, result[0].getType(), "Unexpected response type received by '" + conOne.getUser() + "' after it sent a Roster Set that included a subscription attribute with a value of 'foobar'. It was expected that the server would ignore this value.");
                final RosterEntry entry = Roster.getInstanceFor(conOne).getEntry(target);
                assertNotNull(entry, "Expected the roster of '" + conOne.getUser() + "' to contain an entry for '" + target + "', even if it was added with an invalid subscription attribute. The expectation is for the server to ignore this value. However, the roster entry was not found.");
            }
        } finally {
            // Tear down test fixture
            ((AbstractXMPPConnection) conOne).setParsingExceptionCallback(oldParsingExceptionCallback);

            final Roster roster = Roster.getInstanceFor(conOne);
            final RosterEntry entry = roster.getEntry(target);
            if (entry != null) {
                roster.removeEntry(entry);
            }
        }
    }

    @SmackIntegrationTest(section = "2.1.5", quote = "the entity that processes a roster set MUST verify that the sender of the roster set is authorized to update the roster, and if not return a <forbidden/> error.")
    public void testRosterSetAddressedToDifferentUser() throws XmppStringprepException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, SmackException.NotLoggedInException, InterruptedException
    {
        // Setup test fixture
        final BareJid target = JidCreate.bareFrom( Localpart.from("test-target-" + StringUtils.randomString(5) ), conOne.getXMPPServiceDomain() );

        final RosterPacket request = new RosterPacket();
        request.setType(IQ.Type.set);
        request.addRosterItem(new RosterPacket.Item(target, null));
        request.setTo(conTwo.getUser().asBareJid()); // Address to a _different_ user than the one that will be sending the request.

        try {
            // Execute system under test / verify result
            final XMPPException.XMPPErrorException e = assertThrows(XMPPException.XMPPErrorException.class, () -> conOne.sendIqRequestAndWaitForResponse(request),
                "Expected user '" + conOne.getUser() + "' to receive an error in response to the Roster Set stanza that it sent that was addressed to '" + request.getTo() + "' (where it is assumed that '" + conOne.getUser().asBareJid() + "' is not authorized to update the roster of '" + request.getTo().asBareJid() + "') but the server did not return an error.");
            assertEquals(StanzaError.Condition.forbidden, e.getStanzaError().getCondition(), "Unexpected error condition in the (expected) error that was returned to '" + conOne.getUser() + "' after it sent a Roster Set that was addressed to '" + request.getTo() + "' (where it is assumed that '" + conOne.getUser().asBareJid() + "' is not authorized to update the roster of '" + request.getTo().asBareJid() + "'");
        } finally {
            // Tear down test fixture
            final Roster rosterOne = Roster.getInstanceFor(conOne);
            final RosterEntry entryA = rosterOne.getEntry(target);
            if (entryA != null) {
                rosterOne.removeEntry(entryA);
            }
            final Roster rosterTwo = Roster.getInstanceFor(conTwo);
            final RosterEntry entryB = rosterTwo.getEntry(target);
            if (entryB != null) {
                rosterTwo.removeEntry(entryB);
            }
        }
    }
}
