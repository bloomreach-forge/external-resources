/*
 * Copyright 2010 Hippo
 *
 *   Licensed under the Apache License, Version 2.0 (the  "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS"
 *   BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package parser;

import org.onehippo.forge.externalresource.api.HippoYoutubeResourceManager;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.html.VelocityParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * VelocityParserTest
 */
public class VelocityParserTest {
    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(VelocityParserTest.class);

    @Test
    public void testGetHtml() throws Exception {
        final VelocityParser parser =  VelocityParser.getInstance();

        ResourceManager processor = new HippoYoutubeResourceManager();

        final String html = parser.populateFromProcessor(processor);
        assertTrue(html !=null, "html shouldn't be null" );
    }
}
