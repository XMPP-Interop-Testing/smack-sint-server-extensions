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
package org.igniterealtime.smack.inttest.xep0060;

import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.junit.platform.commons.util.StringUtils;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Various re-usable utility methods for PubSub testing
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class PubSubUtils
{
    /**
     * Verifies that at least one of the provided service discovery items is related to a node with a given ID.
     *
     * This validates the conditions defined in section 4.6 "Addressing" of XEP-0060. This method returns true if any
     * of the provided items match either the JID or the JID+NodeID addressing schema.
     *
     * @param expectedNodeId The ID of the node that is expected to be represented by an item
     * @param items service discovery items
     * @param message error message used when the assertion fails.
     * @see <a href="https://xmpp.org/extensions/xep-0060.html#addressing">XEP-0060 Publish-Subscribe, Section 6.8 Addressing</a>
     */
    public static void assertContainsItemRepresentingNode(final String expectedNodeId, final Collection<DiscoverItems.Item> items, final String message)
    {
        if (expectedNodeId == null) {
            throw new IllegalArgumentException("Argument 'expectedNodeId' cannot be null");
        }
        if (items == null) {
            throw new IllegalArgumentException("Argument 'items' cannot be null");
        }

        for (final DiscoverItems.Item item : items) {
            try {
                assertItemRepresentsNode(expectedNodeId, item, null);
                return;
            } catch (AssertionError e) {
                // Try next.
            }
        }
        fail(message);
    }

    /**
     * Asserts that the provided service discovery item is a representation of a pub/sub node with a particular ID,
     * using any of the addressing mechanisms defined in section 6.8 of XEP-0060.
     *
     * @param expectedNodeId The ID of the node that is expected to be represented by the item
     * @param item a service discovery item
     * @param message error message used when the assertion fails.
     * @see <a href="https://xmpp.org/extensions/xep-0060.html#addressing">XEP-0060 Publish-Subscribe, Section 6.8 Addressing</a>
     */
    public static void assertItemRepresentsNode(final String expectedNodeId, final DiscoverItems.Item item, final String message)
    {
        if (expectedNodeId == null) {
            throw new IllegalArgumentException("Argument 'expectedNodeId' cannot be null");
        }
        if (item == null) {
            throw new IllegalArgumentException("Argument 'item' cannot be null");
        }

        if (item.getNode() != null) {
            assertValidJidPlusNodeIdAddressing(expectedNodeId, item, buildPrefix(message, "") + "[JID+Node addressing detected]" );
        } else {
            assertValidJidAddressing(expectedNodeId, item, buildPrefix(message, "") + "[JID addressing detected]");
        }
    }

    /**
     * Asserts that the provided service discovery item is a representation of a pub/sub node with a particular ID,
     * using the 'JID addressing' mechanism, as described in section 6.8.1 of XEP-0060.
     *
     * @param expectedNodeId The ID of the node that is expected to be represented by the item
     * @param item a service discovery item
     * @param message error message used when the assertion fails.
     * @see <a href="https://xmpp.org/extensions/xep-0060.html#addressing-jid">XEP-0060 Publish-Subscribe, Section 6.8.1 JID</a>
     */
    public static void assertValidJidAddressing(final String expectedNodeId, final DiscoverItems.Item item, final String message)
    {
        if (expectedNodeId == null) {
            throw new IllegalArgumentException("Argument 'expectedNodeId' cannot be null");
        }
        if (item == null) {
            throw new IllegalArgumentException("Argument 'item' cannot be null");
        }

        if (item.getEntityID() == null) {
            fail(buildPrefix(message) + "Disco#item without jid attribute value");
        }

        final String resource = item.getEntityID().getResourceOrEmpty().toString();
        if (resource.isEmpty()) {
            fail(buildPrefix(message) + "Disco#item jid attribute value has no resource-part");
        }

        if (!resource.equals(expectedNodeId)) {
            fail(buildPrefix(message) + "Disco#item jid attribute value resource-part expected: " + expectedNodeId + " but was: " + resource);
        }
    }

    /**
     * Asserts that the provided service discovery item is a representation of a pub/sub node with a particular ID,
     * using the 'JID+NodeID addressing' mechanism, as described in section 6.8.2 of XEP-0060.
     *
     * @param expectedNodeId The ID of the node that is expected to be represented by the item
     * @param item a service discovery item
     * @param message error message used when the assertion fails.
     * @see <a href="https://xmpp.org/extensions/xep-0060.html#addressing-jidnode">XEP-0060 Publish-Subscribe, Section 6.8.2 JID+NodeID</a>
     */
    public static void assertValidJidPlusNodeIdAddressing(final String expectedNodeId, final DiscoverItems.Item item, final String message)
    {
        if (expectedNodeId == null) {
            throw new IllegalArgumentException("Argument 'expectedNodeId' cannot be null");
        }
        if (item == null) {
            throw new IllegalArgumentException("Argument 'item' cannot be null");
        }

        if (item.getEntityID() == null) {
            fail(buildPrefix(message) + "Disco#item without jid attribute value");
        }

        final String node = item.getNode();
        if (node == null || node.isEmpty()) {
            fail(buildPrefix(message) + "Disco#item without node attribute value");
        }

        if (!node.equals(expectedNodeId)) {
            fail(buildPrefix(message) + "Disco#item node attribute value expected: " + expectedNodeId + " but was: " + node);
        }
    }

    static String buildPrefix(String message) {
        return buildPrefix(message, " ==> ");
    }

    static String buildPrefix(String message, String separator) {
        return (StringUtils.isNotBlank(message) ? message + separator : "");
    }
}
