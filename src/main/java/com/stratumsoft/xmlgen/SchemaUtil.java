/*
 * Copyright (c) 2010-2014 Stratumsoft Technologies Pvt. Ltd.
 *
 * This file (SchemaUtil.java) is part of xsd2xml.
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

import org.apache.commons.lang.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.resolver.DefaultURIResolver;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * @author murakris@stratumsoft.com
 * @since 3/15/11
 */
public class SchemaUtil {

    /**
     * Get an instance of {@link XmlSchemaCollection} and read the given schema into it.
     *
     * @param schemaFilePath fully-qualified path to the schema file
     * @param baseUri        base uri to use to resolve any imports/includes; if not specified, the schemaFilePath
     *                       param will be used
     * @return
     * @throws java.io.FileNotFoundException if the input path does not resolve to an actual file on disk
     */
    public static XmlSchemaCollection getSchemaCollection(String schemaFilePath, String baseUri) throws FileNotFoundException {
        XmlSchemaCollection schColl = null;
        if (StringUtils.isNotEmpty(schemaFilePath)) {
            final File schFile = new File(schemaFilePath);
            final FileReader reader = new FileReader(schFile);

            schColl = new XmlSchemaCollection();
            schColl.setBaseUri(StringUtils.isNotEmpty(baseUri) ? baseUri : schemaFilePath);
            schColl.setSchemaResolver(new DefaultURIResolver());

            schColl.read(reader);
        }
        return schColl;
    }

    /**
     * Get the top-level element qname's for the given XmlSchema
     *
     * @param xmlSchema
     * @return
     */
    public static Collection<QName> getElements(XmlSchema xmlSchema) {
        Collection<QName> elNames = new HashSet<QName>();
        if (xmlSchema != null) {
            Map<QName, XmlSchemaElement> schElems = xmlSchema.getElements();
            for (QName name : schElems.keySet()) {
                elNames.add(name);
            }
        }

        return elNames;
    }

}
