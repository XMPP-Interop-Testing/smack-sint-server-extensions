/**
 * Copyright 2024 Guus der Kinderen
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
package org.igniterealtime.smack.inttest.xep0115;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.caps.CapsUtil;
import org.jivesoftware.smackx.caps.CapsVersionAndHash;
import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.caps.packet.CapsExtension;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import javax.xml.namespace.QName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for the XEP-0115: Entity Capabilities
 *
 * @see <a href="https://xmpp.org/extensions/xep-0115.html">XEP-0115: Entity Capabilities</a>
 */
@SpecificationReference(document = "XEP-0115", version = "1.6.0")
public class EntityCapabilitiesIntegrationTest extends AbstractSmackIntegrationTest
{
    public EntityCapabilitiesIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException, TestNotPossibleException
    {
        super(environment);

        if (!EntityCapsManager.getInstanceFor(conOne).areEntityCapsSupportedByServer()) {
            throw new TestNotPossibleException("Domain does not seem support XEP-0115 Entity Capabilities.");
        }

        if (!conOne.hasFeature(new QName(EntityCapsManager.NAMESPACE, EntityCapsManager.ELEMENT))) {
            throw new TestNotPossibleException("Domain does not advertise its entity capabilities in a stream feature element.");
        }
    }

    /**
     * Asserts that the entity capability element as advertised by the server as a stream feature includes the mandatory 'hash' attribute.
     */
    @SmackIntegrationTest(section = "9.3", quote = "If the value of the 'ver' attribute is a verification string as defined herein [...] inclusion of the 'hash' attribute is REQUIRED")
    public void testServerFeatureHasHashAttribute()
    {
        // Execute system under test
        final CapsExtension streamFeature = conOne.getFeature(new QName(EntityCapsManager.NAMESPACE, EntityCapsManager.ELEMENT));

        // Verify result
        assertNotNull(streamFeature.getHash(), "The stream feature as advertised by '" + conOne.getXMPPServiceDomain() + "' was expected to contain a 'hash' attribute (but it did not).");
    }

    /**
     * Asserts that the entity capability element as advertised by the server as a stream feature includes the mandatory 'node' attribute.
     */
    @SmackIntegrationTest(section = "13", quote = "For backwards-compatibility with the legacy format, the 'node' attribute is REQUIRED")
    public void testServerFeatureHasNodeAttribute()
    {
        // Execute system under test
        final CapsExtension streamFeature = conOne.getFeature(new QName(EntityCapsManager.NAMESPACE, EntityCapsManager.ELEMENT));

        // Verify result
        assertNotNull(streamFeature.getNode(), "The stream feature as advertised by '" + conOne.getXMPPServiceDomain() + "' was expected to contain a 'node' attribute (but it did not).");
    }

    /**
     * Asserts that the entity capability element as advertised by the server as a stream feature includes the mandatory 'ver' attribute.
     */
    @SmackIntegrationTest(section = "4", quote = "Entity capabilities are encapsulated in a <c/> element qualified by the 'http://jabber.org/protocol/caps' namespace. The attributes of the <c/> element are as follows. [...] ver [...] REQUIRED")
    public void testServerFeatureHasVerAttribute()
    {
        // Execute system under test
        final CapsExtension streamFeature = conOne.getFeature(new QName(EntityCapsManager.NAMESPACE, EntityCapsManager.ELEMENT));

        // Verify result
        assertNotNull(streamFeature.getVer(), "The stream feature as advertised by '" + conOne.getXMPPServiceDomain() + "' was expected to contain a 'ver' attribute (but it did not).");
    }

    @SmackIntegrationTest(section = "5.1", quote = "In order to help prevent poisoning of entity capabilities information, the value of the verification string MUST be generated according to the following method.")
    public void testServerVerificationStringCalculation() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final CapsExtension streamFeature = conOne.getFeature(new QName(EntityCapsManager.NAMESPACE, EntityCapsManager.ELEMENT));
        final ServiceDiscoveryManager manager = ServiceDiscoveryManager.getInstanceFor(conOne);
        final String node = streamFeature.getNode() + "#" + streamFeature.getVer();

        // Execute system under test.
        final DiscoverInfo discoveredInfo = manager.discoverInfo(conOne.getXMPPServiceDomain(), node);

        // Verify result.
        final CapsVersionAndHash expectedCapsVersionAndHash = CapsUtil.generateVerificationString(discoveredInfo);

        assertEquals(expectedCapsVersionAndHash.version, streamFeature.getVer());
        assertEquals(expectedCapsVersionAndHash.hash, streamFeature.getHash());
    }

    @SmackIntegrationTest(section = "6.2", quote = "The disco 'node' attribute MUST be included for backwards-compatibility. [and, per XEP-0030 section 3.2:] The disco 'node' attribute MUST be included for backwards-compatibility.")
    public void testDiscoInfoResponseMirrorsRequestedNode() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final CapsExtension streamFeature = conOne.getFeature(new QName(EntityCapsManager.NAMESPACE, EntityCapsManager.ELEMENT));
        final ServiceDiscoveryManager manager = ServiceDiscoveryManager.getInstanceFor(conOne);
        final String node = streamFeature.getNode() + "#" + streamFeature.getVer();

        // Execute system under test.
        final DiscoverInfo discoveredInfo = manager.discoverInfo(conOne.getXMPPServiceDomain(), node);

        // Verify result
        assertEquals(node, discoveredInfo.getNode(), "Expected the disco 'node' attribute in the response from '" + conOne.getXMPPServiceDomain() + "' sent to '" + conOne.getUser() + "' to include the original node value (but it did not).");
    }

    @SmackIntegrationTest(section = "9.1", quote = "The SHA-1 hashing algorithm is mandatory to implement. All implementations MUST support SHA-1.")
    public void testServerSupportsMandatoryAlgorithm() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException
    {
        // Setup test fixture.
        final CapsExtension streamFeature = conOne.getFeature(new QName(EntityCapsManager.NAMESPACE, EntityCapsManager.ELEMENT));

        // Verify result.
        final String algorithm = streamFeature.getHash();

        assertEquals("sha-1", algorithm.toLowerCase(), "Expected the domain '" + conOne.getXMPPServiceDomain()  + "' to advertise support for the SHA-1 hash algorithm in its stream feature (but it did not).");
    }
}
