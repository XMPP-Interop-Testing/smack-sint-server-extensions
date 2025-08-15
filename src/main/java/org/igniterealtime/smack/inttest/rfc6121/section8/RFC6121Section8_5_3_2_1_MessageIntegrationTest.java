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
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 8.5.3.2.1 "Local User / localpart@domainpart/resourcepart / No Resource Matches / Message" of section 8 "Server Rules for Processing XML Stanzas" of RFC6121.
 *
 * The specification mentions both 'Available' and 'Connected' resources in its titles, but it does not seem to have any specifications for Connected Resources, apart from a more global description in RFC 6120 (section 10.5).
 * There, the conditions described are mostly based on 'SHOULD' keywords. As such, this implementation tests mostly scenarios with 'Available' resources, and not 'Connected' ones. 
 * See https://mail.jabber.org/hyperkitty/list/standards@xmpp.org/thread/L2JTVXQVXW4EQGFM56H5HHJBU6HVPVXK/
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section8_5_3_2_1_MessageIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    public RFC6121Section8_5_3_2_1_MessageIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        this.environment = environment;
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalNoResource() throws Exception
    {
        // TODO This doesn't assert anything! The implementation asserts only if 'other resources' do not receive a stanza. For this test, there are no 'other resources'.
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, Collections.emptyList());
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalOneResourcePrioPositive() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalOneResourcePrioZero() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalOneResourcePrioNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, List.of(1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, List.of(1, -1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, List.of(3,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, List.of(0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, List.of(0,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, List.of(3,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, List.of(1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testNormalMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.normal, List.of(-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatNoResource() throws Exception
    {
        // TODO This doesn't assert anything! The implementation asserts only if 'other resources' do not receive a stanza. For this test, there are no 'other resources'.
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, Collections.emptyList());
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatOneResourcePrioPositive() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatOneResourcePrioZero() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatOneResourcePrioNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, List.of(1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, List.of(1, -1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, List.of(3,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, List.of(0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, List.of(0,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, List.of(3,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, List.of(1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"groupchat\" [...], the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testGroupchatMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.groupchat, List.of(-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineNoResource() throws Exception
    {
        // TODO This doesn't assert anything! The implementation asserts only if 'other resources' do not receive a stanza. For this test, there are no 'other resources'.
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, Collections.emptyList());
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineOneResourcePrioPositive() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineOneResourcePrioZero() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineOneResourcePrioNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, List.of(1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, List.of(1, -1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, List.of(3,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, List.of(0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, List.of(0,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, List.of(3,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, List.of(1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type [...] \"headline\", the server MUST either (a) silently ignore the stanza or (b) return an error stanza to the sender")
    public void testHeadlineMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.headline, List.of(-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is no available or connected resource, the server MUST either (a) store the message offline for later delivery or (b) return an error stanza to the sender")
    public void testChatNoResource() throws Exception
    {
        // Behavior without any resources MUST be equal to that when processing Normal/Groupchat/Headline. Re-use that implementation.
        // TODO This doesn't assert anything! The implementation asserts only if 'other resources' do not receive a stanza. For this test, there are no 'other resources'.
        doTestMessageNormalOrGroupchatOrHeadline(Message.Type.chat, Collections.emptyList());
    }

    /**
     * Executes a test in which conOne sends a message stanza of type 'normal', 'groupchat' or 'headline' to a full JID
     * of conTwo for which the resource does not match a session that's currently online for that user.
     *
     * conTwo has a number of resources online that matches the amount of entries in the provided list (which can be
     * zero).
     *
     * Verifies that a message stanza is _not_ delivered to _any_ resource of conTwo.
     *
     * To verify that a stanza is _not_ delivered, this test will send each connected resource a stanza after the test
     * stanza is sent. As servers must process stanzas in order, receiving this stanza indicates that the original
     * stanza was not delivered (as it should have been delivered already)
     *
     * @param resourcePriorities the presence priority value for each of conTwo's resources.
     */
    public void doTestMessageNormalOrGroupchatOrHeadline(final Message.Type messageType, final List<Integer> resourcePriorities) throws Exception
    {
        try {
            // Setup test fixture.
            final List<AbstractXMPPConnection> additionalConnections;
            switch (resourcePriorities.size()) {
                case 0:
                    // ConTwo should have _no_ resources online. We must log out the one that we get by default!
                    additionalConnections = Collections.emptyList();
                    ((AbstractXMPPConnection) conTwo).disconnect();
                    break;
                case 1:
                    // ConTwo should have 1 resource online, which we get by default. Nothing to do here.
                    additionalConnections = Collections.emptyList();
                    break;
                default:
                    // ConTwo should have more than one. We get one connection by default. Create the additional ones.
                    additionalConnections = new ArrayList<>(resourcePriorities.size() - 1);
                    for (int i = 0; i < resourcePriorities.size() - 1; i++) {
                        additionalConnections.add(environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration));
                    }
                    break;
            }

            final Set<FullJid> allResources = new HashSet<>();
            StanzaListener stopListenerRecipients = null;
            final Collection<StanzaListener> receivedListeners = new HashSet<>();
            try {
                // Setup test fixture: create connections for the additional resources (based on the user used for 'conTwo').
                for (final AbstractXMPPConnection additionalConnection : additionalConnections) {
                    additionalConnection.connect();
                    additionalConnection.login(((AbstractXMPPConnection) conTwo).getConfiguration().getUsername(), ((AbstractXMPPConnection) conTwo).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));
                }

                // Setup test fixture: configure the desired resource priority for each of the resource connections.
                for (int i = 0; i < resourcePriorities.size(); i++) {
                    final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                    final int resourcePriority = resourcePriorities.get(i);

                    final Presence prioritySet = PresenceBuilder.buildPresence(StringUtils.randomString(9)).setPriority(resourcePriority).build();
                    try (final StanzaCollector presenceUpdateDetected = resourceConnection.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(new FromMatchesFilter(resourceConnection.getUser(), false), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                        resourceConnection.sendStanza(prioritySet);
                        presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
                    }

                    allResources.add(resourceConnection.getUser());
                }

                // Setup test fixture: prepare for the message stanza that is sent to the full JID (that has no online resource) to be sent, and collected while being received by the various resources.
                final String needle = StringUtils.randomString(9);
                final StanzaFilter needleDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getType() == messageType && needle.equals(((Message) s).getBody())));
                final Map<EntityFullJid, Stanza> receivedBy = new ConcurrentHashMap<>(); // This is what will be evaluated by this test's assertions.

                // Setup test fixture: detect the message stanza that's sent to signal that the test stanza has been sent and processed
                final SimpleResultSyncPoint testStanzaProcessedSyncPoint = new SimpleResultSyncPoint();
                stopListenerRecipients = new StanzaListener() {
                    final Set<Jid> recipients = new HashSet<>(allResources);

                    @Override
                    public void processStanza(Stanza packet) {
                        recipients.remove(packet.getTo());
                        if (recipients.isEmpty()) { // When having received a 'stop' on all resources, the test stanza should already have been processed.
                            testStanzaProcessedSyncPoint.signal();
                        }
                    }
                };

                final String stopNeedleRecipients = "STOP LISTENING, STANZAS HAVE BEEN PROCESSED " + StringUtils.randomString(7);
                final StanzaFilter stopDetectorRecipients = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && stopNeedleRecipients.equals(((Message) s).getBody())));

                for (int i = 0; i < resourcePriorities.size(); i++) {
                    final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                    final StanzaListener stanzaListener = (stanza) -> receivedBy.put(resourceConnection.getUser(), stanza);
                    receivedListeners.add(stanzaListener); // keep track so that the listener can be removed again.
                    resourceConnection.addStanzaListener(stanzaListener, needleDetector);
                    resourceConnection.addStanzaListener(stopListenerRecipients, stopDetectorRecipients);
                }

                // Setup test fixture: construct the address of the user (that does exist) for a resource that is not online.
                final EntityFullJid conTwoOfflineResource = JidCreate.entityFullFrom( conTwo.getUser().asEntityBareJid(), Resourcepart.from("not-online-" + StringUtils.randomString(7)) );

                // Execute system under test.
                final Message testStanza = StanzaBuilder.buildMessage()
                    .ofType(messageType)
                    .to(conTwoOfflineResource)
                    .setBody(needle)
                    .build();

                conOne.sendStanza(testStanza);

                // Informs intended recipients that the test is over.
                for (final FullJid recipient : allResources) {
                    conOne.sendStanza(StanzaBuilder.buildMessage().setBody(stopNeedleRecipients).to(recipient).build());
                }

                try {
                    // Wait for all recipients to have received the 'test is over' stanza.
                    if (!allResources.isEmpty()) {
                        testStanzaProcessedSyncPoint.waitForResult(timeout);
                    }
                } catch (TimeoutException e) {
                    // This is dodgy (concurrency issue? server misbehaving?) but let's not fail the test just yet. After the timeout has expired, it is likely that the test is ready to be evaluated.
                    e.printStackTrace();
                }

                // Verify result.
                assertTrue(receivedBy.isEmpty(), "Expected the Message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to '" + conTwoOfflineResource + "' (a resource of an existing user that is not online) to NOT have been received by any other of that user's resources. However, it was received by: " + receivedBy.keySet().stream().map(Object::toString).collect(Collectors.joining(", ")));
            }
            finally
            {
                // Tear down test fixture.
                for (int i = 0; i < resourcePriorities.size(); i++) {
                    final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                    if (stopListenerRecipients != null) {
                        resourceConnection.removeStanzaListener(stopListenerRecipients);
                    }
                    receivedListeners.forEach(resourceConnection::removeStanzaListener); // Only one of these will match.
                }
                additionalConnections.forEach(AbstractXMPPConnection::disconnect);
            }
        }
        finally
        {
            // Tear down test fixture.
            if (!conTwo.isConnected()) {
                // If conTwo was disconnected in the setup of this test, reconnect it now!
                ((AbstractXMPPConnection) conTwo).connect();
                ((AbstractXMPPConnection) conTwo).login(((AbstractXMPPConnection) conTwo).getConfiguration().getUsername(), ((AbstractXMPPConnection) conTwo).getConfiguration().getPassword(), ((AbstractXMPPConnection) conTwo).getConfiguration().getResource());
            } else {
                conTwo.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // This intends to reset presence to mimic the 'initial presence'.
            }
        }
    }

    // 8.5.3.2.1. 'Message': "If all of the available resources have a negative presence priority" only has 'SHOULD' conditions. Per convention, those aren't tested by this implementation.

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is one available resource with a non-negative presence priority then the server MUST deliver the message to that resource.")
    public void testChatOneResourcePrioPositive() throws Exception
    {
        doTestMessageChatExactlyOneNonNegativeResource(List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is one available resource with a non-negative presence priority then the server MUST deliver the message to that resource.")
    public void testChatOneResourcePrioZero() throws Exception
    {
        doTestMessageChatExactlyOneNonNegativeResource(List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is one available resource with a non-negative presence priority then the server MUST deliver the message to that resource.")
    public void testChatlineMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageChatExactlyOneNonNegativeResource(List.of(1, -1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is one available resource with a non-negative presence priority then the server MUST deliver the message to that resource.")
    public void testChatMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageChatExactlyOneNonNegativeResource(List.of(0,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is one available resource with a non-negative presence priority then the server MUST deliver the message to that resource.")
    public void testChatMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageChatExactlyOneNonNegativeResource(List.of(1,-1));
    }

    /**
     * Executes a test in which conOne sends a message stanza of type 'chat', to a full JID
     * of conTwo for which the resource does not match a session that's currently online for that user.
     *
     * conTwo has a number of resources online that matches the amount of entries in the provided list. Exactly one of
     * those resources will have a non-negative priority
     *
     * Verifies that a message stanza is delivered to the resource of conTwo that has a non-negative priority.
     *
     * @param resourcePriorities the presence priority value for each of conTwo's resources.
     */
    public void doTestMessageChatExactlyOneNonNegativeResource(final List<Integer> resourcePriorities) throws Exception
    {
        if (resourcePriorities.stream().filter(r -> r >= 0).count() != 1) {
            throw new IllegalArgumentException("Must be called with resources of which exactly one is non-negative.");
        }
        final int theNonNegativePriorityValue = resourcePriorities.stream().filter(r -> r >= 0).findAny().orElseThrow();

        // Setup test fixture.
        final Message.Type messageType = Message.Type.chat;

        final List<AbstractXMPPConnection> additionalConnections = new ArrayList<>(resourcePriorities.size() - 1);
        for (int i = 0; i < resourcePriorities.size() - 1; i++) {
            additionalConnections.add(environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration));
        }

        EntityFullJid theNonNegativeResource = null;

        try {
            // Setup test fixture: prepare for the message stanza that is sent to the full JID (that has no online resource) to be sent.
            final String needle = StringUtils.randomString(9);
            final StanzaFilter needleDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getType() == messageType && needle.equals(((Message) s).getBody())));
            final SimpleResultSyncPoint stanzaReceived = new SimpleResultSyncPoint();

            // Setup test fixture: create connections for the additional resources (based on the user used for 'conTwo').
            for (final AbstractXMPPConnection additionalConnection : additionalConnections) {
                additionalConnection.connect();
                additionalConnection.login(((AbstractXMPPConnection) conTwo).getConfiguration().getUsername(), ((AbstractXMPPConnection) conTwo).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));
            }

            // Setup test fixture: configure the desired resource priority for each of the resource connections.
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                final int resourcePriority = resourcePriorities.get(i);

                final Presence prioritySet = PresenceBuilder.buildPresence(StringUtils.randomString(9)).setPriority(resourcePriority).build();
                try (final StanzaCollector presenceUpdateDetected = resourceConnection.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(new FromMatchesFilter(resourceConnection.getUser(), false), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                    resourceConnection.sendStanza(prioritySet);
                    presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
                }

                // Setup test fixture: add a listener to the resource with non-negative presence to be able to detect that it has received the stanza.
                if (resourcePriority >= 0) {
                    resourceConnection.addStanzaListener((s) -> stanzaReceived.signal(), needleDetector);
                    assert theNonNegativeResource == null : "The input validation to this method guarantees that there is exactly one resource with a non-negative presence priority value.";
                    theNonNegativeResource = resourceConnection.getUser();
                }
            }
            assert theNonNegativeResource != null : "The input validation to this method guarantees that there is exactly one resource with a non-negative presence priority value.";

            // Setup test fixture: construct the address of the user (that does exist) for a resource that is not online.
            final EntityFullJid conTwoOfflineResource = JidCreate.entityFullFrom( conTwo.getUser().asEntityBareJid(), Resourcepart.from("not-online-" + StringUtils.randomString(7)) );

            // Execute system under test.
            final Message testStanza = StanzaBuilder.buildMessage()
                .ofType(messageType)
                .to(conTwoOfflineResource)
                .setBody(needle)
                .build();

            conOne.sendStanza(testStanza);

            // Verify result.
            assertResult(stanzaReceived, "Expected the Message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to '" + conTwoOfflineResource + "' (a resource of an existing user that is not online) to have been received by the only other resource of that user that has a non-negative resource priority (of value '" + theNonNegativePriorityValue + "'): '" + theNonNegativeResource + "'. However, the stanza was not received.");
        }
        finally
        {
            // Tear down test fixture.
            conTwo.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // This intends to reset presence to mimic the 'initial presence'.
            additionalConnections.forEach(AbstractXMPPConnection::disconnect);
        }
    }

// NOTE: For messages of type "chat" in a scenario with more than one non-negative resources, a server SHOULD NOT deliver messages to applicable clients unless clients can explicitly opt in to receiving all chat messages. This opt-in cannot be detected by this implementation, thus these tests cannot be implemented.
//
//    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources [...] or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages.")
//    public void testChatMultipleResourcesPrioPositive() throws Exception
//    {
//        doTestMessageChatMultipleNonNegativeResources(Message.Type.headline, List.of(1,1));
//    }
//
//    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources [...] or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages.")
//    public void testChatMultipleResourcesPrioDifferentPositive() throws Exception
//    {
//        doTestMessageChatMultipleNonNegativeResources(Message.Type.headline, List.of(3,1));
//    }
//
//    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources [...] or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages.")
//    public void testChatMultipleResourcesPrioZero() throws Exception
//    {
//        doTestMessageChatMultipleNonNegativeResources(Message.Type.headline, List.of(0,0));
//    }
//
//    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources [...] or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages.")
//    public void testChatMultipleResourcesPrioDifferentNonNegative() throws Exception
//    {
//        doTestMessageChatMultipleNonNegativeResources(Message.Type.headline, List.of(3,0));
//    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorNoResource() throws Exception
    {
        doTestMessageError(Collections.emptyList());
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorOneResourcePrioPositive() throws Exception
    {
        doTestMessageError(List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorOneResourcePrioZero() throws Exception
    {
        doTestMessageError(List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorOneResourcePrioNegative() throws Exception
    {
        doTestMessageError(List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageError(List.of(1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageError(List.of(1, -1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageError(List.of(3,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageError(List.of(0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageError(List.of(0,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageError(List.of(3,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageError(List.of(1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.1", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the stanza.")
    public void testErrorMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageError(List.of(-1,-1));
    }

    /**
     * Executes a test in which conOne sends a message stanza of type 'chat', to a full JID
     * of conTwo for which the resource does not match a session that's currently online for that user, after having
     * logged in a number of resources for conTwo.
     *
     * conTwo has a number of resources online that matches the amount of entries in the provided list (which can be
     * zero).
     *
     * Verifies that a message stanza is _not_ delivered to _any_ resource of conTwo.
     *
     * Verifies that no error is returned to the sender.
     *
     * To verify that a stanza is _not_ delivered, this test will send each connected resource a stanza after the test
     * stanza is sent. As servers must process stanzas in order, receiving this stanza indicates that the original
     * stanza was not delivered (as it should have been delivered already).
     *
     * To verify that no error is returned to the sender, a similar 'stop' indicator is sent to the sender.
     *
     * The steps taken to set up the test fixture and execute the test, in detail:
     * <ol>
     * <li>
     *     First, additional resources for conTwo are created, so that this user has as many resources online as the
     *     number of priorities provided to this method. Possibly, the pre-existing connection is disconnected when no
     *     resources are desired.
     * <li>
     *     For all of these resources, a presence update is sent, to set a particular prio value (from the provided
     *     method argument)
     * <li>
     *     Then, the stanza that's the subject of the test is sent to a _full_ JID of the conTwo user (from the
     *     conOne user). The full JID identifies a resource that is known to be offline.
     * <li>
     *     Then, a message stanza is sent to the _full_ JID of each of the resources to signal the end of the test. This
     *     is done to avoid having to depend on timeouts in scenarios where it is expected that _no_ stanza arrives.
     *     After receiving the message stanza sent to the full JID, the original stanza is expected to have been processed
     *     (as it was sent later and stanzas are processed in order).
     * <li>
     *     Finally, a similar 'stop' message stanza is sent back  to 'conOne'. Upon receiving this, 'conOne'
     *     is guaranteed to have received any errors that possibly would have been sent.
     * </ol>
     *
     * Finally, the test fixture is torn down. This involves resetting state, releasing all event listeners, and
     * shutting down any conTwo connections that were created as part of the test fixture setup, or restarting the one
     * that was disconnected.
     *
     * @param resourcePriorities The presence priority values of each of the resources of conTwo that will be online during the test.
     */
    public void doTestMessageError(final List<Integer> resourcePriorities) throws Exception
    {
        // Setup test fixture.
        final Message.Type messageType = Message.Type.error;

        try {
            // Setup test fixture.
            final List<AbstractXMPPConnection> additionalConnections;
            switch (resourcePriorities.size()) {
                case 0:
                    // ConTwo should have _no_ resources online. We must log out the one that we get by default!
                    additionalConnections = Collections.emptyList();
                    ((AbstractXMPPConnection) conTwo).disconnect();
                    break;
                case 1:
                    // ConTwo should have 1 resource online, which we get by default. Nothing to do here.
                    additionalConnections = Collections.emptyList();
                    break;
                default:
                    // ConTwo should have more than one. We get one connection by default. Create the additional ones.
                    additionalConnections = new ArrayList<>(resourcePriorities.size() - 1);
                    for (int i = 0; i < resourcePriorities.size() - 1; i++) {
                        additionalConnections.add(environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration));
                    }
                    break;
            }

            final Set<FullJid> allResources = new HashSet<>();
            StanzaListener stopListenerRecipients = null;
            StanzaListener stopListenerSender = null;
            final Collection<StanzaListener> receivedListeners = new HashSet<>();
            StanzaListener errorListener = null;
            try {
                // Setup test fixture: create connections for the additional resources (based on the user used for 'conTwo').
                for (final AbstractXMPPConnection additionalConnection : additionalConnections) {
                    additionalConnection.connect();
                    additionalConnection.login(((AbstractXMPPConnection) conTwo).getConfiguration().getUsername(), ((AbstractXMPPConnection) conTwo).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));
                }

                // Setup test fixture: configure the desired resource priority for each of the resource connections.
                for (int i = 0; i < resourcePriorities.size(); i++) {
                    final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                    final int resourcePriority = resourcePriorities.get(i);

                    final Presence prioritySet = PresenceBuilder.buildPresence(StringUtils.randomString(9)).setPriority(resourcePriority).build();
                    try (final StanzaCollector presenceUpdateDetected = resourceConnection.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(new FromMatchesFilter(resourceConnection.getUser(), false), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                        resourceConnection.sendStanza(prioritySet);
                        presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
                    }

                    allResources.add(resourceConnection.getUser());
                }

                // Setup test fixture: prepare for the message stanza that is sent to the bare JID to be sent, and collected while being received by the various resources.
                final String needle = StringUtils.randomString(9);
                final StanzaFilter needleDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && ((Message) s).getType() == messageType && needle.equals(((Message) s).getBody())));
                final Map<EntityFullJid, Stanza> receivedBy = new ConcurrentHashMap<>(); // This is what will be evaluated by this test's assertions.

                // Setup test fixture: detect the message stanza that's sent to signal that the test stanza has been sent and processed
                final SimpleResultSyncPoint testStanzaProcessedSyncPoint = new SimpleResultSyncPoint();
                stopListenerRecipients = new StanzaListener()
                {
                    final Set<Jid> recipients = new HashSet<>(allResources);

                    @Override
                    public void processStanza(Stanza packet)
                    {
                        recipients.remove(packet.getTo());
                        if (recipients.isEmpty()) { // When having received a 'stop' on all resources, the test stanza should already have been processed.
                            testStanzaProcessedSyncPoint.signal();
                        }
                    }
                };
                final String stopNeedleRecipients = "STOP LISTENING, STANZAS HAVE BEEN PROCESSED " + StringUtils.randomString(7);
                final StanzaFilter stopDetectorRecipients = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), (s -> s instanceof Message && stopNeedleRecipients.equals(((Message) s).getBody())));

                for (int i = 0; i < resourcePriorities.size(); i++) {
                    final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                    final StanzaListener stanzaListener = (stanza) -> receivedBy.put(resourceConnection.getUser(), stanza);
                    receivedListeners.add(stanzaListener); // keep track so that the listener can be removed again.
                    resourceConnection.addStanzaListener(stanzaListener, needleDetector);
                    resourceConnection.addStanzaListener(stopListenerRecipients, stopDetectorRecipients);
                }

                // Setup test fixture: detect the message stanza that's sent to signal the sender need not wait any longer for any potential stanza delivery errors.
                final String stopNeedleSender = "STOP LISTENING, ALL RECIPIENTS ARE DONE " + StringUtils.randomString(7);
                final StanzaFilter stopDetectorSender = new AndFilter(FromMatchesFilter.createBare(conThree.getUser()), (s -> s instanceof Message && stopNeedleSender.equals(((Message) s).getBody())));
                final SimpleResultSyncPoint stopListenerSenderSyncPoint = new SimpleResultSyncPoint();
                stopListenerSender = (e) -> stopListenerSenderSyncPoint.signal();
                conOne.addStanzaListener(stopListenerSender, stopDetectorSender);

                // Setup test fixture: detect an error that is sent back to the sender.
                final StanzaFilter errorDetector = new AndFilter((s -> s instanceof Message && ((Message) s).getType() == Message.Type.error && needle.equals(((Message) s).getBody())));
                final Stanza[] errorReceived = {null};
                errorListener = (stanza) -> errorReceived[0] = stanza;
                conOne.addStanzaListener(errorListener, errorDetector);

                // Setup test fixture: construct the address of the user (that does exist) for a resource that is not online.
                final EntityFullJid conTwoOfflineResource = JidCreate.entityFullFrom( conTwo.getUser().asEntityBareJid(), Resourcepart.from("not-online-" + StringUtils.randomString(7)) );

                // Execute system under test.
                final Message testStanza = StanzaBuilder.buildMessage()
                    .ofType(messageType)
                    .to(conTwoOfflineResource)
                    .setBody(needle)
                    .build();

                conOne.sendStanza(testStanza);

                // Informs intended recipients that the test is over.
                for (final FullJid recipient : allResources) {
                    conOne.sendStanza(StanzaBuilder.buildMessage().setBody(stopNeedleRecipients).to(recipient).build());
                }

                try {
                    // Wait for all recipients to have received the 'test is over' stanza.
                    if (!allResources.isEmpty()) {
                        testStanzaProcessedSyncPoint.waitForResult(timeout);
                    }
                } catch (TimeoutException e) {
                    // This is dodgy (concurrency issue? server misbehaving?) but let's not fail the test just yet. After the timeout has expired, it is likely that the test is ready to be evaluated.
                    e.printStackTrace();
                }

                try {
                    // Send a message stanza to the sender, saying that the 'test is over' too.
                    conThree.sendStanza(StanzaBuilder.buildMessage().setBody(stopNeedleSender).to(conOne.getUser()).build());

                    // Wait for the sender to have received the 'test is over' stanza.
                    stopListenerSenderSyncPoint.waitForResult(timeout);
                } catch (TimeoutException e) {
                    // This is dodgy (concurrency issue? server misbehaving?) but let's not fail the test just yet. After the timeout has expired, it is likely that the test is ready to be evaluated.
                    e.printStackTrace();
                }

                // Verify result.
                assertTrue(receivedBy.isEmpty(), "Expected the Message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to '" + conTwoOfflineResource + "' (a resource of an existing user that is not online) to be silently ignored and thus to NOT have been received by any other of that user's resources. However, it was received by: " + receivedBy.keySet().stream().map(Object::toString).collect(Collectors.joining(", ")));
                assertNull(errorReceived[0], "Expected the Message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to '" + conTwoOfflineResource + "' (a resource of an existing user that is not online) to be silently ignored and thus to NOT cause an error to be sent back to the sender. Howver, such an error was received.");
            } finally {
                // Tear down test fixture.
                if (errorListener != null) {
                    conOne.removeStanzaListener(errorListener);
                }
                for (int i = 0; i < resourcePriorities.size(); i++) {
                    final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                    if (stopListenerRecipients != null) {
                        resourceConnection.removeStanzaListener(stopListenerRecipients);
                    }
                    receivedListeners.forEach(resourceConnection::removeStanzaListener); // Only one of these will match.
                }
                additionalConnections.forEach(AbstractXMPPConnection::disconnect);
                if (stopListenerSender != null) {
                    conOne.removeStanzaListener(stopListenerSender);
                }
            }
        } finally {
            // Tear down test fixture.
            if (!conTwo.isConnected()) {
                // If conTwo was disconnected in the setup of this test, reconnect it now!
                ((AbstractXMPPConnection) conTwo).connect();
                ((AbstractXMPPConnection) conTwo).login(((AbstractXMPPConnection) conTwo).getConfiguration().getUsername(), ((AbstractXMPPConnection) conTwo).getConfiguration().getPassword(), ((AbstractXMPPConnection) conTwo).getConfiguration().getResource());
            } else {
                conTwo.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // This intends to reset presence to mimic the 'initial presence'.
            }
        }
    }
}
