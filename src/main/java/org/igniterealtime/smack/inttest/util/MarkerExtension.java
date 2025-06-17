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
package org.igniterealtime.smack.inttest.util;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * A ExtensionElement that adds a simple text node.
 *
 * This extension is intended to be used to be able to 'mark' a stanza, by adding some kind of random value that can
 * easily be detected by a stanza listener.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class MarkerExtension implements ExtensionElement
{
    public static final String ELEMENT_NAME = "marker";
    public static final String NAMESPACE = "urn:xmpp-interop:marker:0";
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT_NAME);
    private final String value;

    public MarkerExtension(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public String toXML(XmlEnvironment enclosingNamespace) {
        StringBuilder buf = new StringBuilder();
        buf.append('<').append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\">");
        buf.append(this.getValue());
        buf.append("</" + ELEMENT_NAME + ">");
        return buf.toString();
    }

    public static class Provider extends ExtensionElementProvider<MarkerExtension>
    {
        public Provider() {
        }

        public MarkerExtension parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException
        {
            final String value = ParserUtils.getRequiredNextText(parser);
            return new MarkerExtension(value);
        }
    }
}
