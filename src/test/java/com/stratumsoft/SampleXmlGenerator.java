/*
 * Copyright (c) 2010-2014 Stratumsoft Technologies Pvt. Ltd.
 *
 * This file (SampleXmlGenerator.java) is part of xsd2xml.
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author murakris@stratumsoft.com
 * @since 9/7/11 (1.0)
 */
public class SampleXmlGenerator {

     String BASE_URI = "/schemas/";

    XmlSchemaCollection coll;
    private XmlSchema schema;
    private XmlGenOptions options;

    private SchemaTypeXmlGenerator generator;

    private static final Logger logger = LoggerFactory.getLogger(SchemaTypeXmlGenerator.class);


    @Test
     public void testGenXml() throws Exception {
        String xsdPath = "catalog.xsd";

        String path = BASE_URI + xsdPath;
        InputStream is = this.getClass().getResourceAsStream(path);
        assertNotNull(is);

        URL xsdUrl = this.getClass().getResource(path);

        coll = new XmlSchemaCollection();
        coll.setBaseUri(xsdUrl.toString());

        StreamSource source = new StreamSource(is);
        schema = coll.read(source);
        assertNotNull(schema);

        options = new XmlGenOptions();
        options.setGenCommentsForParticles(true);
        options.setGenChoiceOptionsAsComments(false);
        options.setMaxRecursiveDepth(1);
        options.setMaxRepeatingElements(2);
        options.setDefVals(DefaultValues.DEFAULT);

        generator = new SchemaTypeXmlGenerator(coll, options);

        QName elName = new QName("", "catalog");

        String xml = generator.generateXml(elName, true);

        assertNotNull(xml);
        assertTrue(xml.length() > 0);

    }
}
