/*
 * Copyright (c) 2010-2014 Stratumsoft Technologies Pvt. Ltd.
 *
 * This file (SchemaTypeXmlGeneratorTest.java) is part of xsd2xml.
 *
 * xsd2xml is a Java program to generate XML instances from an XML Schema document
 *
 * xsd2xml is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * xsd2xml is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 *
 * To use xsd2xml in your non-GPL licensed software, contact Stratumsoft Technologies
 * support at support@stratumsoft.com or visit http://www.stratumsoft.com to obtain
 * a commercial license.
 */

package com.stratumsoft;

import com.stratumsoft.xmlgen.DefaultValues;
import com.stratumsoft.xmlgen.SchemaTypeXmlGenerator;
import com.stratumsoft.xmlgen.XmlGenOptions;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author murakris@stratumsoft.com
 * @since 1.0 (Feb 22, 2010)
 */
@RunWith(Parameterized.class)
public class SchemaTypeXmlGeneratorTest {

    String BASE_URI = "/schemas/";

    XmlSchemaCollection coll;

    String xsdPath;
    QName elName;

    private SchemaTypeXmlGenerator generator;

    private static SchemaFactory schFac;

    private Schema sch;

    private static final Logger logger = LoggerFactory.getLogger(SchemaTypeXmlGenerator.class);

    public SchemaTypeXmlGeneratorTest(String schemaName, QName elName) {
        this.xsdPath = schemaName;
        this.elName = elName;
    }

    @BeforeClass
    public static void setupClass() throws Exception {
        schFac = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        assertNotNull(schFac);
    }

    @Before
    public void setup() throws Exception {
        String path = BASE_URI + this.xsdPath;
        InputStream is = this.getClass().getResourceAsStream(path);
        assertNotNull(is);

        URL xsdUrl = this.getClass().getResource(path);

        coll = new XmlSchemaCollection();
        coll.setBaseUri(xsdUrl.toString());

        StreamSource source = new StreamSource(is);
        XmlSchema schema = coll.read(source);
        assertNotNull(schema);

        XmlGenOptions options = new XmlGenOptions();
        options.setGenCommentsForParticles(true);
        options.setGenChoiceOptionsAsComments(false);
        options.setMaxRecursiveDepth(1);
        options.setMaxRepeatingElements(2);
        options.setDefVals(DefaultValues.DEFAULT);

        generator = new SchemaTypeXmlGenerator(coll, options);

        sch = schFac.newSchema(new File(xsdUrl.toURI()));
    }

    @Test
    public void testGenXml() throws Exception {

        logger.info("--- Processing schema: " + xsdPath + "; element: " + elName);

        String xml = generator.generateXml(elName, true);

        System.out.println("xml = " + xml);

        assertNotNull(xml);
        assertTrue(xml.length() > 0);

        validateGenXml(xml);
    }

    /**
     * validate the generated xml to make sure it complies with the schema!
     *
     * @param xml
     * @throws Exception
     */
    private void validateGenXml(String xml) throws Exception {
        Validator validator = sch.newValidator();
        validator.setErrorHandler(new MyErrorHandler());
        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes())));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"TestElementForm.xsd", new QName("http://example.com/", "TestElFormImport")},
                {"TestElementForm.xsd", new QName("http://www.XMLSchemaReference.com/examples", "formElementDemo")},
                {"TestAttributeForm.xsd", new QName("http://www.XMLSchemaReference.com/examples", "formAttributeDemo")},
                {"personal.xsd", new QName("http://example.com", "personnel")},
//                {"catalog.xsd", new QName("", "catalog")},     //fails due to wrong value, not structure
                {"person.xsd", new QName("", "character")},
//                {"pricing.xsd", new QName("", "price")},      //wrong value err
                {"SimpleStockQuote.xsd", new QName("http://services.samples/xsd", "PlaceOrder")},
                {"TestGroup.xsd", new QName("", "test")},
                {"TestSimpleContentRestriction.xsd", new QName("", "title")},
                {"TestRepeats.xsd", new QName("", "test")},
//                {"TestRecursion.xsd", new QName("", "root")},   //structurally correct, but since it is recursive, parser errors due to missing el
                {"TestRecursion.xsd", new QName("", "root2")},
                {"company/Company.xsd", new QName("http://www.company.org", "Company")}
        });
    }

    class MyErrorHandler extends DefaultHandler {
        @Override
        public void error(SAXParseException e) throws SAXException {
            logger.info("Error: ");
            printInfo(e);
            fail(e.getMessage());
        }

        public void warning(SAXParseException e) throws SAXException {
            logger.info("Warning: ");
            printInfo(e);
        }

        public void fatalError(SAXParseException e) throws SAXException {
            logger.info("Fatal error: ");
            printInfo(e);
            fail(e.getMessage());
        }

        private void printInfo(SAXParseException e) {
            logger.info("   Line number: " + e.getLineNumber());
            logger.info("   Column number: " + e.getColumnNumber());
            logger.info("   Message: " + e.getMessage());
        }

    }

}
