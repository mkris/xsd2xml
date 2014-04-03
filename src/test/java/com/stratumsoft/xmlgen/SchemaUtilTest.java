/*
 * Copyright (c) 2010-2014 Stratumsoft Technologies Pvt. Ltd.
 *
 * This file (SchemaUtilTest.java) is part of xsd2xml.
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

package com.stratumsoft.xmlgen;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.junit.Test;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author murakris@stratumsoft.com
 * @since 3/15/11
 */
public class SchemaUtilTest {

    private String personalXsd = "/schemas/personal.xsd";
    private String elementFormXsd = "/schemas/TestElementForm.xsd";

    @Test
    public void testGetNames() throws Exception {
        final InputStream is = getClass().getResourceAsStream(personalXsd);
        assertNotNull(is);

        final StreamSource source = new StreamSource(is);
        final XmlSchema schema = new XmlSchemaCollection().read(source);
        assertNotNull(schema);

        final Collection<QName> elements = SchemaUtil.getElements(schema);
        assertNotNull(elements);
        assertFalse(elements.isEmpty());

        System.out.println("Got the following elements from schema:");
        CollectionUtils.forAllDo(elements, new Closure() {
            @Override
            public void execute(Object input) {
                System.out.println(input);
            }
        });

    }

    @Test
    public void testGetNamesFromSchemaWithImport() throws Exception {
        final InputStream is = getClass().getResourceAsStream(elementFormXsd);
        assertNotNull(is);

        final StreamSource source = new StreamSource(is);
        final XmlSchemaCollection schemaColl = new XmlSchemaCollection();
        final URL baseUrl = SchemaUtilTest.class.getResource(elementFormXsd);
        final String baseUri = baseUrl.toURI().toString();

        schemaColl.setBaseUri(baseUri);


        final XmlSchema schema = schemaColl.read(source);
        assertNotNull(schema);

        System.out.println("There are " + schemaColl.getXmlSchemas().length + " schemas present");

        final Collection<QName> elements = SchemaUtil.getElements(schema);
        assertNotNull(elements);
        assertFalse(elements.isEmpty());

        System.out.println("Got the following elements from schema:");
        CollectionUtils.forAllDo(elements, new Closure() {
            @Override
            public void execute(Object input) {
                System.out.println(input);
            }
        });

    }

}
