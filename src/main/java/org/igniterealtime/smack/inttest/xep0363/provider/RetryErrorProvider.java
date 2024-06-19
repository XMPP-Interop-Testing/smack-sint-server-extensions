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
package org.igniterealtime.smack.inttest.xep0363.provider;

import org.igniterealtime.smack.inttest.xep0363.element.RetryError;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;

/**
 * A provider of HTTP File Upload 'retry' error elements.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 */
public class RetryErrorProvider extends ExtensionElementProvider<RetryError>
{
    @Override
    public RetryError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) {
        String stamp = parser.getAttributeValue("stamp");
        return new RetryError(stamp);
    }
}
