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
import org.igniterealtime.smack.inttest.util.AccountUtilities;
import org.igniterealtime.smack.inttest.util.MarkerExtension;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.*;
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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests that verify that behavior defined in section 8.5.3.2.2 "Local User / localpart@domainpart/resourcepart / No Resource Matches / Presence" of section 8 "Server Rules for Processing XML Stanzas" of RFC6121.
 *
 * Note: this <em>does not</em> tests the processing of presence stanzas with of type 'subscribe' or 'probe'. Those types are subject of tests under other sections (Section 3 and Section 4.3), to which section 8.5.3.2.2 references (and which have a much more detailed definition of the desired functionality).
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section8_5_3_2_2_PresenceIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    public RFC6121Section8_5_3_2_2_PresenceIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        this.environment = environment;
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableNoResource() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, Collections.emptyList());
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableOneResourcePrioPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableOneResourcePrioZero() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableOneResourcePrioNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableMultipleResourcesPrioPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, List.of(1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, List.of(1, -1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, List.of(3,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableMultipleResourcesPrioZero() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, List.of(0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, List.of(0,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, List.of(3,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, List.of(1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with no 'type' attribute [...] the server MUST silently ignore the stanza.")
    public void testAvailableMultipleResourcesPrioNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.available, List.of(-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableNoResource() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, Collections.emptyList());
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableOneResourcePrioPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableOneResourcePrioZero() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableOneResourcePrioNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableMultipleResourcesPrioPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, List.of(1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, List.of(1, -1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, List.of(3,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableMultipleResourcesPrioZero() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, List.of(0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, List.of(0,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, List.of(3,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, List.of(1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza with [...] a 'type' attribute of \"unavailable\", the server MUST silently ignore the stanza.")
    public void testUnavailableMultipleResourcesPrioNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unavailable, List.of(-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedNoResource() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, Collections.emptyList());
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedOneResourcePrioPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedOneResourcePrioZero() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedOneResourcePrioNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedMultipleResourcesPrioPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, List.of(1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, List.of(1, -1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, List.of(3,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedMultipleResourcesPrioZero() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, List.of(0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, List.of(0,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, List.of(3,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, List.of(1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type \"subscribed\" [...] the server MUST ignore the stanza.")
    public void testSubscribedMultipleResourcesPrioNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.subscribed, List.of(-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeNoResource() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, Collections.emptyList());
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeOneResourcePrioPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeOneResourcePrioZero() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeOneResourcePrioNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeMultipleResourcesPrioPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, List.of(1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, List.of(1, -1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, List.of(3,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeMultipleResourcesPrioZero() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, List.of(0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, List.of(0,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, List.of(3,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, List.of(1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribe\" [...], the server MUST ignore the stanza.")
    public void testUnsubscribeMultipleResourcesPrioNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribe, List.of(-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedNoResource() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, Collections.emptyList());
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedOneResourcePrioPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedOneResourcePrioZero() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedOneResourcePrioNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedMultipleResourcesPrioPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, List.of(1,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, List.of(1, -1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, List.of(3,1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedMultipleResourcesPrioZero() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, List.of(0,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, List.of(0,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, List.of(3,0));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, List.of(1,-1));
    }

    @SmackIntegrationTest(section = "8.5.3.2.2", quote = "If the domainpart of the JID contained in the 'to' attribute of an inbound stanza matches one of the configured domains of the server itself and the JID contained in the 'to' attribute is of the form <localpart@domainpart/resourcepart>, then the server MUST adhere to the following rules. [...] If no available resource or connected resource exactly matches the full JID, how the stanza is processed depends on the stanza type. [...] For a presence stanza of type [...] \"unsubscribed\", the server MUST ignore the stanza.")
    public void testUnsubscribedMultipleResourcesPrioNegative() throws Exception
    {
        doTestForSilentlyIgnore(Presence.Type.unsubscribed, List.of(-1,-1));
    }

    /**
     * Executes a test in which conOne sends a presence stanza of a particular type, to a full JID of conTwo for which
     * the resource does not match a session that's currently online for that user, after having logged in a number of
     * resources for conTwo.
     *
     * conTwo has a number of resources online that matches the amount of entries in the provided list (which can be
     * zero).
     *
     * Verifies that a presence stanza is _not_ delivered to _any_ resource of conTwo.
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
     * @param presenceType The type of the stanza that is sent to the system under test.
     * @param resourcePriorities The presence priority values of each of the resources of conTwo that will be online during the test.
     */
    public void doTestForSilentlyIgnore(final Presence.Type presenceType, final List<Integer> resourcePriorities) throws Exception
    {
        if (Set.of(Presence.Type.subscribe, Presence.Type.probe).contains(presenceType)) {
            // This test asserts things that are not applicable to these types.
            throw new IllegalArgumentException("This test should not be called with presence type " + presenceType);
        }

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
                        additionalConnections.add(AccountUtilities.spawnNewConnection(environment, sinttestConfiguration));
                    }
                    break;
            }

            final Set<FullJid> allResources = new HashSet<>();
            final Collection<ListenerHandle> listenerHandles = new HashSet<>(); // keep track so that the associated listener can be deregistered after the test is done.
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

                    allResources.add(resourceConnection.getUser());
                }

                // Setup test fixture: prepare for the message stanza that is sent to the bare JID to be sent, and collected while being received by the various resources.
                final String needle = StringUtils.randomString(9);
                final StanzaFilter needleDetector = new AndFilter(FromMatchesFilter.createFull(conOne.getUser()), new ExtensionElementFilter<>(MarkerExtension.class), (s -> s instanceof Presence && ((Presence) s).getType() == presenceType && s.getExtension(MarkerExtension.class).getValue().equals(needle)));
                final Map<EntityFullJid, Stanza> receivedBy = new ConcurrentHashMap<>(); // This is what will be evaluated by this test's assertions.

                // Setup test fixture: detect the message stanza that's sent to signal that the test stanza has been sent and processed
                final SimpleResultSyncPoint testStanzaProcessedSyncPoint = new SimpleResultSyncPoint();
                final StanzaListener stopListenerRecipients = new StanzaListener()
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
                    listenerHandles.add(resourceConnection.addStanzaListener((stanza) -> receivedBy.put(resourceConnection.getUser(), stanza), needleDetector));
                    listenerHandles.add(resourceConnection.addStanzaListener(stopListenerRecipients, stopDetectorRecipients));
                }

                // Setup test fixture: detect the message stanza that's sent to signal the sender need not wait any longer for any potential stanza delivery errors.
                final String stopNeedleSender = "STOP LISTENING, ALL RECIPIENTS ARE DONE " + StringUtils.randomString(7);
                final StanzaFilter stopDetectorSender = new AndFilter(FromMatchesFilter.createBare(conThree.getUser()), (s -> s instanceof Message && stopNeedleSender.equals(((Message) s).getBody())));
                final SimpleResultSyncPoint stopListenerSenderSyncPoint = new SimpleResultSyncPoint();
                listenerHandles.add(conOne.addStanzaListener((stanza) -> stopListenerSenderSyncPoint.signal(), stopDetectorSender));

                // Setup test fixture: detect an error that is sent back to the sender.
                final StanzaFilter errorDetector = new AndFilter((s -> s instanceof Message && ((Message) s).getType() == Message.Type.error && needle.equals(((Message) s).getBody())));
                final Stanza[] errorReceived = {null};
                listenerHandles.add(conOne.addStanzaListener((stanza) -> errorReceived[0] = stanza, errorDetector));

                // Setup test fixture: construct the address of the user (that does exist) for a resource that is not online.
                final EntityFullJid conTwoOfflineResource = JidCreate.entityFullFrom( conTwo.getUser().asEntityBareJid(), Resourcepart.from("not-online-" + StringUtils.randomString(7)) );

                // Execute system under test.
                final Presence testStanza = StanzaBuilder.buildPresence()
                    .ofType(presenceType)
                    .to(conTwoOfflineResource)
                    .addExtension(new MarkerExtension(needle))
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
                assertTrue(receivedBy.isEmpty(), "Expected the Presence stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to '" + conTwoOfflineResource + "' (a resource of an existing user that is not online) to be silently ignored and thus to NOT have been received by any other of that user's resources. However, it was received by: " + receivedBy.keySet().stream().map(Object::toString).collect(Collectors.joining(", ")));
                assertNull(errorReceived[0], "Expected the Presence stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to '" + conTwoOfflineResource + "' (a resource of an existing user that is not online) to be silently ignored and thus to NOT cause an error to be sent back to the sender. However, such an error was received.");
            } finally {
                // Tear down test fixture.
                listenerHandles.forEach(ListenerHandle::close);
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
}
