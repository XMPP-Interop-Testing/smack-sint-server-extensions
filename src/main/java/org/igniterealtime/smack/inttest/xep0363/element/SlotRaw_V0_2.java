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

import org.jivesoftware.smack.packet.IQ;

import java.util.Map;

/**
 * A duplicate of Smack's Slot_V0_2 implementation, but one that stores the URLs as raw strings.
 *
 * Note that in most cases, it is preferable to use {@link org.jivesoftware.smackx.httpfileupload.element.Slot_V0_2} instead
 * of this class. It is functionally equivalent, but provides better type-safety.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 * @see org.jivesoftware.smackx.httpfileupload.element.Slot_V0_2
 */
public class SlotRaw_V0_2 extends SlotRaw {
    public static final String NAMESPACE = "urn:xmpp:http:upload";

    public SlotRaw_V0_2(String putUrl, String getUrl) {
        super(putUrl, getUrl, (Map)null, "urn:xmpp:http:upload");
    }

    protected IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(IQ.IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.element("put", this.putUrl);
        xml.element("get", this.getUrl);
        return xml;
    }
}
