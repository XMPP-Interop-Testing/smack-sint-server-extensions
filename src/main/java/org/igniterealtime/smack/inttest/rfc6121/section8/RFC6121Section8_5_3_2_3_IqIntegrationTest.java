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
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 8.5.3.2.3 "Local User / localpart@domainpart/resourcepart / No Resource Matches / IQ" of section 8 "Server Rules for Processing XML Stanzas" of RFC6121.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section8_5_3_2_3_IqIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    public RFC6121Section8_5_3_2_3_IqIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        this.environment = environment;
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetOneResourcePrioPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetOneResourcePrioZero() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetOneResourcePrioNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetMultipleResourcesPrioPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetMultipleResourcesPrioZero() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedGetMultipleResourcesPrioNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.get, List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetOneResourcePrioPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetOneResourcePrioZero() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetOneResourcePrioNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetMultipleResourcesPrioPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetMultipleResourcesPrioZero() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testUnsupportedSetMultipleResourcesPrioNegative() throws Exception
    {
        doTestUnsupportedIQ(IQ.Type.set, List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetOneResourcePrioPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetOneResourcePrioZero() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetOneResourcePrioNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetMultipleResourcesPrioPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetMultipleResourcesPrioZero() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedGetMultipleResourcesPrioNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.get, List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetOneResourcePrioPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetOneResourcePrioZero() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetOneResourcePrioNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetMultipleResourcesPrioPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetMultipleResourcesPrioZero() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.3.2.3", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For an IQ stanza, the server MUST return a <service-unavailable/> stanza error to the sender.")
    public void testSupportedSetMultipleResourcesPrioNegative() throws Exception
    {
        doTestSupportedIQ(IQ.Type.set, List.of(-1,-1,-1));
    }

    /**
     * Executes a test in which conOne sends an IQ stanza of type 'get' or 'set' to the bare JID of conTwo.
     *
     * conTwo has a number of resources online that matches the amount of entries in the provided list (which can be
     * zero).
     *
     * The IQ request is almost guaranteed to not be understood by the server (as we've made a namespace up ourselves).
     * As such, the server is expected to respond with a specific error condition.
     *
     * Verifies that an error is returned to the sender.
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
        doTest(stanzaToSend, resourcePriorities);
    }

    /**
     * Executes a test in which conOne sends an IQ stanza of type 'get' or 'set' to the bare JID of conTwo.
     *
     * conTwo has a number of resources online that matches the amount of entries in the provided list (which can be
     * zero).
     *
     * The IQ request is guaranteed to be understood by the server (if it is not, a TestNotPossible exception is thrown).
     *
     * Verifies that an error is returned to the sender.
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
        doTest(stanzaToSend, resourcePriorities);
    }

    /**
     * Executes a test in which conOne sends an IQ stanza to a full JID of conTwo for which the resource does not match
     * a session that's currently online for that user, after having logged in a number of resources for conTwo.
     *
     * conTwo has a number of resources online that matches the amount of entries in the provided list (which can be
     * zero).
     *
     * Verifies that the IQ stanza is _not_ delivered to _any_ resource of conTwo.
     *
     * Verifies that an error is returned to the sender.
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
     * </ol>
     *
     * Finally, the test fixture is torn down. This involves resetting state, releasing all event listeners, and
     * shutting down any conTwo connections that were created as part of the test fixture setup, or restarting the one
     * that was disconnected.
     *
     * @param testStanza the stanza that is sent to the system under test.
     * @param resourcePriorities The presence priority values of each of the resources of conTwo that will be online during the test.
     */
    public void doTest(final IQ testStanza, final List<Integer> resourcePriorities) throws Exception
    {
        // Setup test fixture.
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

            final Map<EntityFullJid, Map<IQRequestHandler, IQRequestHandler>> receivedHandlers = new HashMap<>();
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
                    try (final StanzaCollector presenceUpdateDetected = resourceConnection.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(FromMatchesFilter.createFull(resourceConnection.getUser()), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                        resourceConnection.sendStanza(prioritySet);
                        presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
                    }
                }

                // Setup test fixture: prepare for any resource to collect the stanza that is sent (even if it's not expected to be received).
                final Map<EntityFullJid, Stanza> receivedBy = new ConcurrentHashMap<>(); // This is what will be evaluated by this test's assertions.
                for (int i = 0; i < resourcePriorities.size(); i++) {
                    final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);

                    final IQRequestHandler needleDetector = new AbstractIqRequestHandler(testStanza.getChildElementName(), testStanza.getChildElementNamespace(), testStanza.getType(), IQRequestHandler.Mode.sync) {
                        @Override
                        public IQ handleIQRequest(IQ iq) {
                            receivedBy.put(resourceConnection.getUser(), iq);
                            return iq.isRequestIQ() ? IQ.createErrorResponse(iq, StanzaError.Condition.undefined_condition) : null;
                        }
                    };
                    final IQRequestHandler oldHandler = resourceConnection.registerIQRequestHandler(needleDetector);

                    // keep track so that the handler can be removed again.
                    final Map<IQRequestHandler, IQRequestHandler> receivedHandler = new HashMap<>();
                    if (receivedHandler.put(needleDetector, oldHandler) != null) {
                        throw new IllegalStateException("Bug in code: unexpectedly have more than one IQ handler with the same detector for the same connection.");
                    }
                    receivedHandlers.put(resourceConnection.getUser(), receivedHandler);
                }

                // Setup test fixture: construct the address of the user (that does exist) for a resource that is not online.
                final EntityFullJid conTwoOfflineResource = JidCreate.entityFullFrom( conTwo.getUser().asEntityBareJid(), Resourcepart.from("not-online-" + StringUtils.randomString(7)) );

                // Execute system under test.
                testStanza.setTo(conTwoOfflineResource);
                IQ response;
                try {
                    response = conOne.sendIqRequestAndWaitForResponse(testStanza);
                } catch (XMPPException.XMPPErrorException e) {
                    response = (IQ) e.getStanza();
                }
                
                // Verify result.
                assertTrue(receivedBy.isEmpty(), "Expected the IQ stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to '" + conTwoOfflineResource + "' (a resource of an existing user that is not online) to be answered by the server and thus to NOT have been received by any other of that user's resources. However, it was received by: " + receivedBy.keySet().stream().map(Object::toString).collect(Collectors.joining(", ")));
                assertNotNull(response, "Expected the IQ stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to '" + conTwoOfflineResource + "' (a resource of an existing user that is not online) to be answered by the server (but no response was received).");
                assertEquals(IQ.Type.error, response.getType(), "Expected the IQ stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to '" + conTwoOfflineResource + "' (a resource of an existing user that is not online) to be answered by the server with an error (but the response was not of the error type).");
                assertNotNull(response.getError(), "Expected the IQ stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to '" + conTwoOfflineResource + "' (a resource of an existing user that is not online) to be answered by the server with an error (but the response did not contain an error element).");
                assertEquals(StanzaError.Condition.service_unavailable, response.getError().getCondition(), "Expected the IQ stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to '" + conTwoOfflineResource + "' (a resource of an existing user that is not online) to be answered by the server with a specific error condition (but the response did not contain that condition).");
            } finally {
                // Tear down test fixture.
                for (int i = 0; i < resourcePriorities.size(); i++) {
                    final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                    final Map<IQRequestHandler, IQRequestHandler> handlersNewAndOld = receivedHandlers.remove(resourceConnection.getUser());
                    if (handlersNewAndOld != null) {
                        for (final Map.Entry<IQRequestHandler, IQRequestHandler> handlerNewAndOld : handlersNewAndOld.entrySet()) {
                            resourceConnection.unregisterIQRequestHandler(handlerNewAndOld.getKey());
                            if (handlerNewAndOld.getValue() != null) { // Restore the old handler, if there was one.
                                resourceConnection.registerIQRequestHandler(handlerNewAndOld.getValue());
                            }
                        }
                    }
                }
                additionalConnections.forEach(AbstractXMPPConnection::disconnect);
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
