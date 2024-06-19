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
package org.igniterealtime.smack.inttest.xep0363.element;

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * A representation of a 'retry' error element, as specified in XEP-0363.
 *
 * This implementation deliberately does not attempt to validate the provided timestamp, as such validation is subject
 * of a test implementation that depends on this class.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 */
public class RetryError implements XmlElement
{
    public static String ELEMENT = "retry";
    public static String NAMESPACE = "urn:xmpp:http:upload:0";

    private final String stamp; // Do not use a more specific type, as the integration test wants to use the raw value.

    public RetryError(final String stamp)
    {
        this.stamp = stamp;
    }

    public String getStamp() {
        return stamp;
    }

    @Override
    public String getNamespace()
    {
        return NAMESPACE;
    }

    @Override
    public String getElementName()
    {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("stamp", stamp);
        xml.closeEmptyElement();
        return xml;
    }
}
