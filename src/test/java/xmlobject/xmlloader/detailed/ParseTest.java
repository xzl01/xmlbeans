/*   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package xmlobject.xmlloader.detailed;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import xmlcursor.common.BasicCursorTestCase;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ParseTest extends BasicCursorTestCase {
    private final XmlOptions m_map = new XmlOptions();

    @Test
    public void testLoadStripWhitespace() throws Exception {
        m_map.setLoadStripWhitespace();
        m_xo = XmlObject.Factory.parse("<foo>01234   <bar>text</bar>   chars \r\n</foo>  ",
            m_map);
        m_xc = m_xo.newCursor();
        assertEquals("<foo>01234<bar>text</bar>chars</foo>", m_xc.xmlText());
    }


    @Test
    public void testLoadDiscardDocumentElement() throws Exception {
        m_map.setLoadReplaceDocumentElement(new QName(""));
        XmlObject.Factory.parse("<foo>01234   <bar>text</bar>   chars </foo>  ", m_map);
    }

    @Test(expected = XmlException.class)
    public void testPrefixNotDefined() throws Exception {
        String sXml = "<Person xmlns=\"person\"><pre1:Name>steve</pre1:Name></Person>";
        XmlObject.Factory.parse(sXml);
    }

    @Test(expected = XmlException.class)
    public void testErrorListener() throws Exception {
        Vector vErrors = new Vector();
        m_map.setErrorListener(vErrors);
        XmlObject.Factory.parse("<foo>text<foo>", m_map);  // improper end tag
    }

    @Test
    public void testParsingDOMWithDTD() throws Exception {
        final String svgDocumentString = "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"\n" +
                                         "\"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n" +
                                         "<svg />";
        assertNotNull(XmlObject.Factory.parse(svgDocumentString));
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        final Document parse = documentBuilder.parse(new InputSource(new StringReader(svgDocumentString)));
        assertNotNull(XmlObject.Factory.parse(parse));
    }
}

