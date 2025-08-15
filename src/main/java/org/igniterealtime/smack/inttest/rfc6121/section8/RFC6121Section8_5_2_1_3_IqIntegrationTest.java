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
import org.jivesoftware.smack.roster.Roster;
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
 * Integration tests that verify that behavior defined in section 8.5.2.1.3 "Local User / localpart@domainpart / Available or Connected Resources / IQ" of section 8 "Server Rules for Processing XML Stanzas" of RFC6121.
 *
 * The specification mentions both 'Available' and 'Connected' resources in its titles, but it does not seem to have any specifications for Connected Resources, apart from a more global description in RFC 6120 (section 10.5).
 * There, the conditions described are mostly based on 'SHOULD' keywords. As such, this implementation tests mostly scenarios with 'Available' resources, and not 'Connected' ones. 
 * See https://mail.jabber.org/hyperkitty/list/standards@xmpp.org/thread/L2JTVXQVXW4EQGFM56H5HHJBU6HVPVXK/
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section8_5_2_1_3_IqIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    public RFC6121Section8_5_2_1_3_IqIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        this.environment = environment;

        ProviderManager.addIQProvider(TestIQ.ELEMENT, TestIQ.NAMESPACE, new InternalProvider());
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetOneResourcePrioPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetOneResourcePrioZero() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetOneResourcePrioNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetMultipleResourcesPrioPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetMultipleResourcesPrioZero() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedGetMultipleResourcesPrioNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetOneResourcePrioPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetOneResourcePrioZero() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetOneResourcePrioNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetMultipleResourcesPrioPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetMultipleResourcesPrioZero() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user [...] if not, then the server MUST reply with a <service-unavailable/> stanza error.")
    public void testUnsupportedSetMultipleResourcesPrioNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetOneResourcePrioPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetOneResourcePrioZero() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetOneResourcePrioNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetMultipleResourcesPrioPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetMultipleResourcesPrioZero() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedGetMultipleResourcesPrioNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetOneResourcePrioPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetOneResourcePrioZero() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetOneResourcePrioNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetMultipleResourcesPrioPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetMultipleResourcesPrioZero() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.3", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server itself MUST reply on behalf of the user with either an IQ result or an IQ error, and MUST NOT deliver the IQ stanza to any of the user's available resources. Specifically, if the semantics of the qualifying namespace define a reply that the server can provide on behalf of the user, then the server MUST reply to the stanza on behalf of the user by returning either an IQ stanza of type \"result\" or an IQ stanza of type \"error\" that is appropriate to the original payload")
    public void testSupportedSetMultipleResourcesPrioNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(-1,-1,-1));
    }

    /**
     * Executes a test in which conOne sends an IQ stanza of type 'get' or 'set' to the bare JID of conTwo.
     * conTwo has a number of resources online that matches the amount of entries in the provided list.
     *
     * The IQ request is almost guaranteed to not be understood by the server (as we've made a namespace up ourselves).
     * As such, the server is expected to respond with a specific error condition.
     *
     * Verifies that none of the resources of the addressee received the request (the server is expected to answer on
     * behalf of the addressee).
     *
     * @param iqType the type used in the IQ request stanza.
     * @param resourcePriorities the presence priority value for each of conTwo's resources.
     */
    public void doTestUnsupportedIQ(final IQ.Type iqType, final List<Integer> resourcePriorities) throws Exception
    {
        final TestIQ stanzaToSend = new TestIQ();
        stanzaToSend.setType(iqType);
        final String needle = StringUtils.randomString(9);
        stanzaToSend.setValue(needle);
        doTest(stanzaToSend, resourcePriorities, (stanzasReceivedByRecipient, testStanza, testResponse) ->
        {
            assertTrue(stanzasReceivedByRecipient.isEmpty(), "Expected the IQ of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to NOT have been received by any of their resources. Instead the stanza was received by: [" + stanzasReceivedByRecipient.keySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", "))+ "]" );
            assertNotNull(testResponse, "Expected '" + conOne.getUser() + "' to have received a response (presumably generated by the server) to the IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that it sent to the bare JID of '" + conTwo.getUser().asBareJid() + "' (but no response was received).");
            assertEquals(IQ.Type.error, testResponse.getType(), "Expected '" + conOne.getUser() + "' to have received an IQ error response (as the semantics of the qualifying namespace - which we've made up - cannot be understood by the server) to the IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that it sent to the bare JID of '" + conTwo.getUser().asBareJid() + "'.");
            assertNotNull(testResponse.getError(), "Expected '" + conOne.getUser() + "' to have received an error response (as the semantics of the qualifying namespace - which we've made up - cannot be understood by the server) to the IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that it sent to the bare JID of '" + conTwo.getUser().asBareJid() + "' (but the response did not contain an error).");
            assertEquals(StanzaError.Condition.service_unavailable, testResponse.getError().getCondition(), "Unexpected error condition in the expected error response received by '" + conOne.getUser() + "' after it sent an IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' using a qualifying namespace - which we've made up - that cannot be understood by the server) with ID '" + testStanza.getStanzaId() + "' to the bare JID of '" + conTwo.getUser().asBareJid() + "'.");
            return null;
        });
    }

    /**
     * Executes a test in which conOne sends an IQ stanza of type 'get' or 'set' to the bare JID of conTwo.
     * conTwo has a number of resources online that matches the amount of entries in the provided list.
     *
     * The IQ request is guaranteed to be understood by the server (if it is not, a TestNotPossible exception is thrown).
     *
     * Verifies that none of the resources of the addressee received the request (the server is expected to answer on
     * behalf of the addressee).
     *
     * @param iqType the type used in the IQ request stanza.
     * @param resourcePriorities the presence priority value for each of conTwo's resources.
     */
    public void doTestSupportedIQ(final IQ.Type iqType, final List<Integer> resourcePriorities) throws Exception
    {
        if (!ServiceDiscoveryManager.getInstanceFor(conOne).supportsFeature(conOne.getXMPPServiceDomain(), "jabber:iq:roster")) {
            throw new TestNotPossibleException("The 'jabber:iq:roster' feature is not advertised by the server");
        }

        final RosterPacket stanzaToSend = new RosterPacket();
        stanzaToSend.setType(iqType); // Neither a 'set' nor 'get' roster request to another person's JID is expected to change state of their roster (so it can be considered quite safe to use for testing purposes).
        doTest(stanzaToSend, resourcePriorities, (stanzasReceivedByRecipient, testStanza, testResponse) ->
        {
            assertTrue(stanzasReceivedByRecipient.isEmpty(), "Expected the IQ of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to NOT have been received by any of their resources. Instead the stanza was received by: [" + stanzasReceivedByRecipient.keySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", "))+ "]" );
            assertNotNull(testResponse, "Expected '" + conOne.getUser() + "' to have received a response (presumably generated by the server) to the IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that it sent to the bare JID of '" + conTwo.getUser().asBareJid() + "' (but no response was received).");
            assertTrue(testResponse.isResponseIQ(), "Expected '" + conOne.getUser() + "' to have received an IQ response to the IQ request of type '" + testStanza.getType() + "' with ID '" + testStanza.getStanzaId() + "' that it sent to the bare JID of '" + conTwo.getUser().asBareJid() + "'.");
            return null;
        });
    }

    /**
     * Manages (sets up, executes and tears down) a test fixture in which conOne sends an IQ stanza to the bare JID of
     * conTwo, after having logged in a number of resources for conTwo.
     *
     * The amount of resources logged in for conTwo is equal to the number of resourcePriorities provided in the second
     * argument to this method (the test suite always guarantees one pre-existing connection for conTwo, this method
     * will create additional connections as needed). Each of the conTwo connection will have its presence state updated
     * with a priority value taken from the second argument to this method.
     *
     * The steps taken to set up the test fixture and execute the test, in detail:
     * <ol>
     * <li>
     *     First, additional resources for conTwo are created, so that this user has as many resources online as the
     *     number of priorities provided to this method.
     * <li>
     *     For all of these resources, a presence update is sent, to set a particular prio value (from the provided
     *     method argument)
     * <li>
     *     Then, the stanza that's the subject of the test is sent to the _bare_ JID of the conTwo user (from the
     *     conOne user).
     * <li>
     *     Finally, a message stanza is sent to the _full_ JID of each of the resources to signal the end of the test.
     *     This is done to avoid having to depend on timeouts in scenarios where it is expected that _no_ stanza arrives.
     *     After receiving the message stanza sent to the full JID, the original stanza is expected to have been processed
     *     (as it was sent later and stanzas are processed in order).
     * </ol>
     *
     * After a test fixture has been created, and the stanza that is the subject of this test has been sent (and should
     * have been processed by the server), the assertions provided as the third argument to this method will be executed.
     *
     * Finally, the test fixture is torn down. This involves resetting state, releasing all event listeners, and
     * shutting down any conTwo connections that were created as part of the test fixture setup.
     *
     * @param resourcePriorities The presence priority values of each of the resources of conTwo that will be online during the test.
     */
    public <T extends IQ> void doTest(final T testStanza, final List<Integer> resourcePriorities, final Assertions<Map<EntityFullJid, Stanza>, IQ, Void> assertions) throws Exception
    {
        if (resourcePriorities.isEmpty()) {
            throw new IllegalArgumentException("The resource priorities must contain at least one element.");
        }

        // Setup test fixture.
        final List<AbstractXMPPConnection> additionalConnections = new ArrayList<>(resourcePriorities.size()-1);
        for (int i = 0; i < resourcePriorities.size()-1; i++) {
            additionalConnections.add(environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration));
        }

        final Set<FullJid> allResources = new HashSet<>();
        StanzaListener stopListenerRecipients = null;
        final Collection<IQRequestHandler> receivedHandlers = new HashSet<>();
        try {
            // Setup test fixture: create connections for the additional resources (based on the user used for 'conTwo').
            for (final AbstractXMPPConnection additionalConnection : additionalConnections) {
                additionalConnection.connect();
                additionalConnection.login(((AbstractXMPPConnection) conTwo).getConfiguration().getUsername(), ((AbstractXMPPConnection) conTwo).getConfiguration().getPassword(), Resourcepart.from(StringUtils.randomString(7)));
            }

            // Setup test fixture: configure the desired resource priority for each of the resource connections.
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i-1);
                final int resourcePriority = resourcePriorities.get(i);

                final Presence prioritySet = PresenceBuilder.buildPresence(StringUtils.randomString(9)).setPriority(resourcePriority).build();
                try (final StanzaCollector presenceUpdateDetected = resourceConnection.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(new FromMatchesFilter(resourceConnection.getUser(), false), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                    resourceConnection.sendStanza(prioritySet);
                    presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
                }

                allResources.add(resourceConnection.getUser());
            }

            // Setup test fixture: prepare for the IQ request that is sent to the bare JID to be sent, and collected if being received by the various resources of the recipient.
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

                final IQRequestHandler needleDetector = new AbstractIqRequestHandler(TestIQ.ELEMENT, TestIQ.NAMESPACE, testStanza.getType(), IQRequestHandler.Mode.sync) {
                    @Override
                    public IQ handleIQRequest(IQ iq) {
                        receivedBy.put(resourceConnection.getUser(), iq);
                        return iq.isRequestIQ() ? IQ.createErrorResponse(iq, StanzaError.Condition.undefined_condition) : null;
                    }
                };
                receivedHandlers.add(needleDetector); // keep track so that the handler can be removed again.
                resourceConnection.addStanzaListener(stopListenerRecipients, stopDetectorRecipients);
                resourceConnection.registerIQRequestHandler(needleDetector);
            }

            // Execute system under test.
            IQ testResponse;
            try {
                testStanza.setTo(conTwo.getUser().asBareJid());
                testResponse = conOne.sendIqRequestAndWaitForResponse(testStanza);
            } catch (XMPPException.XMPPErrorException e) {
                testResponse = (IQ) e.getStanza();
            }

            // Informs intended recipients that the test is over.
            for (final FullJid recipient : allResources) {
                conOne.sendStanza(StanzaBuilder.buildMessage().setBody(stopNeedleRecipients).to(recipient).build());
            }

            try {
                // Wait for all recipients to have received the 'test is over' stanza.
                testStanzaProcessedSyncPoint.waitForResult(timeout);
            } catch (TimeoutException e) {
                // This is dodgy (concurrency issue? server misbehaving?) but let's not fail the test just yet. After the timeout has expired, it is likely that the test is ready to be evaluated.
                e.printStackTrace();
            }

            // Verify result.
            assertions.test(receivedBy, testStanza, testResponse);
        } finally {
            // Tear down test fixture.
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                if (stopListenerRecipients != null) { resourceConnection.removeStanzaListener(stopListenerRecipients); }
                receivedHandlers.forEach(resourceConnection::unregisterIQRequestHandler); // Only one of these will match.
            }
            additionalConnections.forEach(AbstractXMPPConnection::disconnect);
            conTwo.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // This intends to mimic the 'initial presence'.
        }
    }

    @FunctionalInterface
    public interface Assertions<A, B, R> {
        R test(A a, B b, B c);
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
