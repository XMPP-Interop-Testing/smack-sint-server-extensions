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
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify that behavior defined in section 8.5.2.1.1 "Local User / localpart@domainpart / Available or Connected Resources / Message" of section 8 "Server Rules for Processing XML Stanzas" of RFC6121.
 *
 * The specification mentions both 'Available' and 'Connected' resources in its titles, but it does not seem to have any specifications for Connected Resources, apart from a more global description in RFC 6120 (section 10.5).
 * There, the conditions described are mostly based on 'SHOULD' keywords. As such, this implementation tests mostly scenarios with 'Available' resources, and not 'Connected' ones. 
 * See https://mail.jabber.org/hyperkitty/list/standards@xmpp.org/thread/L2JTVXQVXW4EQGFM56H5HHJBU6HVPVXK/
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
@SpecificationReference(document = "RFC6121")
public class RFC6121Section8_5_2_1_1_MessageIntegrationTest extends AbstractSmackIntegrationTest
{
    private final SmackIntegrationTestEnvironment environment;

    public RFC6121Section8_5_2_1_1_MessageIntegrationTest(SmackIntegrationTestEnvironment environment)
    {
        super(environment);
        this.environment = environment;
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is one available resource with a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalOneResourcePrioPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is one available resource with a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalOneResourcePrioZero() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testNormalOneResourcePrioNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"normal\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testNormalMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testNormalMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.normal, List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If the only available resource has a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatOneResourcePrioPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If the only available resource has a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatOneResourcePrioZero() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testChatOneResourcePrioNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"chat\": [...] If there is more than one resource with a non-negative presence priority then the server MUST either (a) deliver the message to the \"most available\" resource or resources (according to the server's implementation-specific algorithm, e.g., treating the resource or resources with the highest presence priority as \"most available\") or (b) deliver the message to all of the non-negative resources that have opted in to receive chat messages. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testChatMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testChatMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageNormalOrChat(Message.Type.chat, List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatOneResourcePrioPositive() throws Exception
    {
        doTestMessageGroupchat(List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatOneResourcePrioZero() throws Exception
    {
        doTestMessageGroupchat(List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testGroupchatOneResourcePrioNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageGroupchat(List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageGroupchat(List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageGroupchat(List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"groupchat\", the server MUST NOT deliver the stanza to any of the available resources but instead MUST return a stanza error to the sender [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testGroupchatMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testGroupchatMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageGroupchat(List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If the only available resource has a negative presence priority then the server MUST silently ignore the stanza.")
    public void testHeadlineOneResourcePrioNegative() throws Exception
    {
        doTestMessageHeadline(List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If the only available resource has a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineOneResourcePrioZero() throws Exception
    {
        doTestMessageHeadline(List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If the only available resource has a non-negative presence priority then the server MUST deliver the message to that resource. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineOneResourcePrioPositive() throws Exception
    {
        doTestMessageHeadline(List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageHeadline(List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageHeadline(List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageHeadline(List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestMessageHeadline(List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageHeadline(List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageHeadline(List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageHeadline(List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"headline\": [...] If there is more than one resource with a non-negative presence priority then the server MUST deliver the message to all of the non-negative resources. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testHeadlineMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageHeadline(List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testHeadlineMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageHeadline(List.of(-1,-1,-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorOneResourcePrioPositive() throws Exception
    {
        doTestMessageError(List.of(1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorOneResourcePrioZero() throws Exception
    {
        doTestMessageError(List.of(0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testErrorOneResourcePrioNegative() throws Exception
    {
        doTestMessageError(List.of(-1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMultipleResourcesPrioPositive() throws Exception
    {
        doTestMessageError(List.of(1,1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMultipleResourcesPrioPositiveAndNegative() throws Exception
    {
        doTestMessageError(List.of(1,1,-1,1));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMultipleResourcesPrioDifferentPositive() throws Exception
    {
        doTestMessageError(List.of(3,1,2));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMultipleResourcesPrioDifferentPositiveAndNegative() throws Exception
    {
        doTestMessageError(List.of(1,2,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMultipleResourcesPrioZero() throws Exception
    {
        doTestMessageError(List.of(0,0,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMultipleResourcesPrioZeroAndNegative() throws Exception
    {
        doTestMessageError(List.of(0,0,-1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMultipleResourcesPrioDifferentNonNegative() throws Exception
    {
        doTestMessageError(List.of(3,1,0));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] If there is at least one available resource [...], how the stanza is processed depends on the stanza type. [...] For a message stanza of type \"error\", the server MUST silently ignore the message. [...] In all cases, the server MUST NOT rewrite the 'to' attribute (i.e., it MUST leave it as <localpart@domainpart> rather than change it to <localpart@domainpart/resourcepart>).")
    public void testErrorMultipleResourcesPrioDifferentNonNegativeAndNegative() throws Exception
    {
        doTestMessageError(List.of(1,0,-1,3));
    }

    @SmackIntegrationTest(section = "8.5.2.1.1", quote = "If the JID contained in the 'to' attribute is of the form <localpart@domainpart>, then the server MUST adhere to the following rules. [...] for any message type the server MUST NOT deliver the stanza to any available resource with a negative priority")
    public void testErrorMultipleResourcesPrioNegative() throws Exception
    {
        doTestMessageError(List.of(-1,-1,-1));
    }

    /**
     * Executes a test in which conOne sends a message stanza of type 'normal' or 'chat' to the bare JID of conTwo.
     * conTwo has a number of resources online that matches the amount of entries in the provided list.
     *
     * Verifies that a message stanza is either delivered at _all_ resources with non-negative priorities, _or_ at exactly one of them.
     * When negative priorities are provided, the test asserts that the message stanza has _not_ been delivered to the corresponding
     * resource.
     *
     * @param resourcePriorities the presence priority value for each of conTwo's resources.
     */
    public void doTestMessageNormalOrChat(final Message.Type messageType, final List<Integer> resourcePriorities) throws Exception
    {
        if (!Set.of(Message.Type.normal, Message.Type.chat).contains(messageType)) {
            throw new IllegalArgumentException("Invalid 'messageType' argument value: " + messageType);
        }
        doTest(messageType, resourcePriorities, (allRecipientResources, allNonNegativeRecipientResources, stanzasReceivedByRecipient, testStanza, errorReceivedBySender) ->
        {
            final Set<EntityFullJid> nonNegativeRecipients = stanzasReceivedByRecipient.keySet().stream().filter(allNonNegativeRecipientResources::contains).collect(Collectors.toSet());
            final Set<EntityFullJid> negativeRecipients = stanzasReceivedByRecipient.keySet().stream().filter(o -> allRecipientResources.contains(o) && !allNonNegativeRecipientResources.contains(o)).collect(Collectors.toSet());

            switch (allNonNegativeRecipientResources.size()) {
                case 0:
                    // Do nothing to assert.
                    break;
                case 1:
                    assertEquals(1, nonNegativeRecipients.size(), "Expected the message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to have been received by the single resource that had a non-negative resource: [" + allNonNegativeRecipientResources.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "]. Instead the message stanza was received by: [" + stanzasReceivedByRecipient.keySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", "))+ "]" );
                    break;
                default:
                    if (messageType == Message.Type.chat) {
                        // Message stanza type 'chat' can be influenced by an unspecified (and for the purpose of this test, undetectable) 'opt-in' mechanism. When such an opt-in mechanism is _not_ offered, then the message stanza should be sent to either the highest priority, or _all_ resources that are non-negative. When such an opt-in is offered, it could also be sent to all those that opted-in (which may be zero resources). See https://logs.xmpp.org/xsf/2025-06-06#2025-06-06-44f4ded1943dad29 for more context.
                        assertTrue(nonNegativeRecipients.isEmpty() || nonNegativeRecipients.size() == 1 || nonNegativeRecipients.size() == allNonNegativeRecipientResources.size(), "Expected the message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to have been received by either exactly one, zero (in case an opt-in mechanism is provided by the server, but not used by any of the clients), or all (in case no opt-in mechanism is provided by the server) resources that have non-negative presence: [" + allNonNegativeRecipientResources.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "]. Instead the message stanza was received by: [" + stanzasReceivedByRecipient.keySet().stream().sorted().collect(Collectors.joining(", "))+ "]"  );
                    } else {
                        assertTrue(nonNegativeRecipients.size() == 1 || nonNegativeRecipients.size() == allNonNegativeRecipientResources.size(), "Expected the message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to have been received by either exactly one, or all resources that have non-negative presence: [" + allNonNegativeRecipientResources.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "]. Instead the message stanza was received by: [" + stanzasReceivedByRecipient.keySet().stream().sorted().collect(Collectors.joining(", "))+ "]"  );
                    }
                    break;
            }

            assertTrue(negativeRecipients.isEmpty(), "Expected the message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to NOT have been received by resources that have a negative priority. Instead, it was received by this/these resource(s) that had a negative priority: [" + negativeRecipients.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "].");

            final Map<EntityFullJid, Jid> invalidAddressees = stanzasReceivedByRecipient.entrySet().stream().filter((entry) -> !entry.getValue().getTo().equals(testStanza.getTo())).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTo()));
            final String errorMessage = invalidAddressees.entrySet().stream().map(entry -> "resource '" + entry.getKey() + "' received a stanza addressed to '" + entry.getValue() + "'").collect(Collectors.joining(", "));
            assertTrue(invalidAddressees.isEmpty(), "Expected the 'to' attribute of the message stanza sent by '" + conOne.getUser() + "' to remain unchanged ('" + testStanza.getTo() + "'). Instead, these resources received attribute values that were modified: " + errorMessage + ".");

            return null;
        });
    }

    /**
     * Executes a test in which conOne sends a message stanza of type 'groupchat' to the bare JID of conTwo. conTwo has
     * a number of resources online that matches the amount of entries in the provided list.
     *
     * @param resourcePriorities the presence priority value for each of conTwo's resources.
     */
    public void doTestMessageGroupchat(final List<Integer> resourcePriorities) throws Exception
    {
        doTest(Message.Type.groupchat, resourcePriorities, (allRecipientResources, allNonNegativeRecipientResources, stanzasReceivedByRecipient, testStanza, errorReceivedBySender) ->
        {
            assertTrue(stanzasReceivedByRecipient.isEmpty(), "Expected the message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to NOT have been received by by any resource. Instead the message stanza was received by: [" + stanzasReceivedByRecipient.keySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", "))+ "]" );
            assertTrue(errorReceivedBySender.isPresent(), "Expected '" + conOne.getUser() + "' to receive an error after trying to send a message stanza of type '" + testStanza.getType() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' (but no error was received)." );

            return null;
        });
    }

    /**
     * Executes a test in which conOne sends a message stanza of type 'error' to the bare JID of conTwo. conTwo has
     * a number of resources online that matches the amount of entries in the provided list.
     *
     * @param resourcePriorities the presence priority value for each of conTwo's resources.
     */
    public void doTestMessageError(final List<Integer> resourcePriorities) throws Exception {
        doTest(Message.Type.error,  resourcePriorities, (allRecipientResources, allNonNegativeRecipientResources, stanzasReceivedByRecipient, testStanza, errorReceivedBySender) ->
        {
            assertTrue(stanzasReceivedByRecipient.isEmpty(), "Expected the message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to NOT have been received by by any resource. Instead the message stanza was received by: [" + stanzasReceivedByRecipient.keySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", "))+ "]" );
            assertFalse(errorReceivedBySender.isPresent(), "After '" + conOne.getUser() + "' sent a message stanza of type '" + testStanza.getType() + "' to the bare JID of '" + testStanza.getTo() + "', it was expected that the server would silently ignore the stanza. Instead, '" + conOne.getUser() + "' received an error: ");

            return null;
        });
    }

    /**
     * Executes a test in which conOne sends a message stanza of type 'headline' to the bare JID of conTwo. conTwo has
     * a number of resources online that matches the amount of entries in the provided list.
     *
     * @param resourcePriorities the presence priority value for each of conTwo's resources.
     */
    public void doTestMessageHeadline(final List<Integer> resourcePriorities) throws Exception {
        doTest(Message.Type.headline, resourcePriorities, (allRecipientResources, allNonNegativeRecipientResources, stanzasReceivedByRecipient, testStanza, errorReceivedBySender) ->
        {
            final Set<EntityFullJid> missing = new HashSet<>(allNonNegativeRecipientResources);
            stanzasReceivedByRecipient.keySet().forEach(missing::remove);
            final Set<EntityFullJid> negativeRecipients = stanzasReceivedByRecipient.keySet().stream().filter(o -> allRecipientResources.contains(o) && !allNonNegativeRecipientResources.contains(o)).collect(Collectors.toSet());

            assertTrue(missing.isEmpty(), "Expected the message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() + "' to have been received by all resources of that user that have a non-negative presence priority. However, it was not received by [" + missing.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "]");
            assertTrue(negativeRecipients.isEmpty(), "Expected the message stanza of type '" + testStanza.getType() + "' that was sent by '" + conOne.getUser() + "' to the bare JID of '" + conTwo.getUser().asBareJid() +"' to NOT have been received by resources that have a negative priority. Instead, it was received by this/these resource(s) that had a negative priority: [" + negativeRecipients.stream().map(Object::toString).sorted().collect(Collectors.joining(", ")) + "].");

            final Map<EntityFullJid, Jid> invalidAddressees = stanzasReceivedByRecipient.entrySet().stream().filter((entry) -> !entry.getValue().getTo().equals(testStanza.getTo())).collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTo()));
            final String errorMessage = invalidAddressees.entrySet().stream().map(entry -> "resource '" + entry.getKey() + "' received a stanza addressed to '" + entry.getValue() + "'").collect(Collectors.joining(", "));
            assertTrue(invalidAddressees.isEmpty(), "Expected the 'to' attribute of the message stanza sent by '" + conOne.getUser() + "' to remain unchanged ('" + testStanza.getTo() + "'). Instead, these resources received attribute values that were modified: " + errorMessage + ".");

            // Asserts that a message stanza is not responded to with an error (is silently dropped) when it is _not_ delivered.
            if (allNonNegativeRecipientResources.isEmpty()) {
                assertFalse(errorReceivedBySender.isPresent(), "After '" + conOne.getUser() + "' sent a message stanza of type '" + testStanza.getType() + "' to the bare JID of '" + testStanza.getTo() + "', a user that had only resources online that have negative presence priority, it was expected that the server would silently ignore the stanza. Instead, '" + conOne.getUser() + "' received an error.");
            }
            return null;
        });
    }

    /**
     * Manages (sets up, executes and tears down) a test fixture in which conOne sends a message stanza (of a type
     * determined by a parameter) to the bare JID of conTwo, after having logged in a number of resources for conTwo.
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
     *     Then, a message stanza is sent to the _full_ JID of each of the resources to signal the end of the test. This
     *     is done to avoid having to depend on timeouts in scenarios where it is expected that _no_ stanza arrives.
     *     After receiving the message stanza sent to the full JID, the original stanza is expected to have been processed
     *     (as it was sent later and stanzas are processed in order).
     * <li>
     *     Finally, a similar 'stop' message  stanzais sent back from 'conTwo' to 'conOne'. Upon receiving this, 'conOne'
     *     is guaranteed to have received any errors that possibly would have been sent.
     * </ol>
     *
     * After a test fixture has been created, and the stanza that is the subject of this test has been sent (and should
     * have been processed by the server), the assertions provided as the third argument to this method will be executed.
     *
     * Finally, the test fixture is torn down. This involves resetting state, releasing all event listeners, and
     * shutting down any conTwo connections that were created as part of the test fixture setup.
     *
     * @param messageType the type of message stanza that is the subject of the test. The message stanza will be sent by conOne to the bare JID of conTwo.
     * @param resourcePriorities The presence priority values of each of the resources of conTwo that will be online during the test.
     * @param assertions A functional method that implements the assertions that are to be verified. The test will invoke this function with the following arguments:
     *                   <ol>
     *                   <li>A collection of full JIDs, each representing one of the connections of conTwo (the addressee of the stanza that is sent during the test</li>
     *                   <li>Another collection of full JIDs which is a subset of the first set, for each of the connections of conTwo that have a non-negative presence priority value during the test</li>
     *                   <li>A map that includes an entry for each resource (of conTwo) that received the stanza that is sent during the test (key: full JID of the recipient, value: the stanza that is received)</li>
     *                   <li>The stanza that was sent to invoke the system-under-test</li>
     *                   <li>An optional error stanza that was received by conOne as a result of sending the stanza to conTwo</li>
     *                   </ol>
     */
    public void doTest(final Message.Type messageType, final List<Integer> resourcePriorities, final Assertions<Set<FullJid>, Set<EntityFullJid>, Map<EntityFullJid, Stanza>, Message, Optional<Stanza>, Void> assertions) throws Exception
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
        final Set<EntityFullJid> allNonNegativeResources = new HashSet<>();
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
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i-1);
                final int resourcePriority = resourcePriorities.get(i);

                final Presence prioritySet = PresenceBuilder.buildPresence(StringUtils.randomString(9)).setPriority(resourcePriority).build();
                try (final StanzaCollector presenceUpdateDetected = resourceConnection.createStanzaCollectorAndSend(new OrFilter(new StanzaIdFilter(prioritySet), new AndFilter(FromMatchesFilter.createFull(resourceConnection.getUser()), (s -> s instanceof Presence && ((Presence) s).getPriority() == resourcePriority))), prioritySet)) {
                    resourceConnection.sendStanza(prioritySet);
                    presenceUpdateDetected.nextResult(); // Wait for echo, to be sure that presence update was processed by the server.
                }

                allResources.add(resourceConnection.getUser());
                if (resourcePriority >= 0) {
                    allNonNegativeResources.add(resourceConnection.getUser());
                }
            }

            // Setup test fixture: prepare for the message stanza that is sent to the bare JID to be sent, and collected while being received by the various resources.
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

            // Setup test fixture: detect the message stanza that's sent to signal the sender need not wait any longer for any potential stanza delivery errors.
            final String stopNeedleSender = "STOP LISTENING, ALL RECIPIENTS ARE DONE " + StringUtils.randomString(7);
            final StanzaFilter stopDetectorSender = new AndFilter(FromMatchesFilter.createBare(conTwo.getUser()), (s -> s instanceof Message && stopNeedleSender.equals(((Message) s).getBody())));
            final SimpleResultSyncPoint stopListenerSenderSyncPoint = new SimpleResultSyncPoint();
            stopListenerSender = (e) -> stopListenerSenderSyncPoint.signal();
            conOne.addStanzaListener(stopListenerSender, stopDetectorSender);

            // Setup test fixture: detect an error that is sent back to the sender.
            final StanzaFilter errorDetector = new AndFilter((s -> s instanceof Message && ((Message) s).getType() == Message.Type.error && needle.equals(((Message) s).getBody())));
            final Stanza[] errorReceived = { null };
            errorListener = (stanza) -> errorReceived[0] = stanza;
            conOne.addStanzaListener(errorListener, errorDetector);

            // Execute system under test.
            final Message testStanza = StanzaBuilder.buildMessage()
                .ofType(messageType)
                .to(conTwo.getUser().asBareJid())
                .setBody(needle)
                .build();

            conOne.sendStanza(testStanza);

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

            try {
                // Send a message stanza to the sender, saying that the 'test is over' too.
                conTwo.sendStanza(StanzaBuilder.buildMessage().setBody(stopNeedleSender).to(conOne.getUser()).build());

                // Wait for the sender to have received the 'test is over' stanza.
                stopListenerSenderSyncPoint.waitForResult(timeout);
            } catch (TimeoutException e) {
                // This is dodgy (concurrency issue? server misbehaving?) but let's not fail the test just yet. After the timeout has expired, it is likely that the test is ready to be evaluated.
                e.printStackTrace();
            }

            // Verify result.
            assertions.test(allResources, allNonNegativeResources, receivedBy, testStanza, Optional.ofNullable(errorReceived[0]));
        } finally {
            // Tear down test fixture.
            if (errorListener != null) { conOne.removeStanzaListener(errorListener); }
            for (int i = 0; i < resourcePriorities.size(); i++) {
                final XMPPConnection resourceConnection = i == 0 ? conTwo : additionalConnections.get(i - 1);
                if (stopListenerRecipients != null) { resourceConnection.removeStanzaListener(stopListenerRecipients); }
                receivedListeners.forEach(resourceConnection::removeStanzaListener); // Only one of these will match.
            }
            additionalConnections.forEach(AbstractXMPPConnection::disconnect);
            conTwo.sendStanza(PresenceBuilder.buildPresence().ofType(Presence.Type.available).build()); // This intends to mimic the 'initial presence'.
            if (stopListenerSender != null) { conOne.removeStanzaListener(stopListenerSender); }
        }
    }

    @FunctionalInterface
    public interface Assertions<A, B, C, D, E, R> {
        R test(A a, B b, C c, D d, E e);
    }
}
