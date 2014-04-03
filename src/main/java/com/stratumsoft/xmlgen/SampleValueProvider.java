/*
 * Copyright (c) 2010-2014 Stratumsoft Technologies Pvt. Ltd.
 *
 * This file (SampleValueProvider.java) is part of xsd2xml.
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

import org.apache.commons.codec.binary.Base64;
import org.apache.ws.commons.schema.constants.Constants;

import javax.xml.namespace.QName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author murakris@stratumsoft.com
 * @since 7/8/11
 */
@SuppressWarnings("HardCodedStringLiteral")
public class SampleValueProvider {

    static Map<QName, String> qnVal = new HashMap<QName, String>();

    static final SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
    static final SimpleDateFormat sdfYearMonth = new SimpleDateFormat("yyyy-MM");
    static final SimpleDateFormat sdfMonthDay = new SimpleDateFormat("MM-dd");
    static final SimpleDateFormat sdfMonth = new SimpleDateFormat("MM");
    static final SimpleDateFormat sdfDay = new SimpleDateFormat("dd");
    static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
    static final SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm:ss");
    static final SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    static {
        qnVal.put(Constants.XSD_STRING, "string");
        qnVal.put(Constants.XSD_BOOLEAN, "true");
        qnVal.put(Constants.XSD_DOUBLE, "1.0");
        qnVal.put(Constants.XSD_FLOAT, "1.0");
        qnVal.put(Constants.XSD_INT, "1");
        qnVal.put(Constants.XSD_UNSIGNEDINT, "1");
        qnVal.put(Constants.XSD_INTEGER, "1");
        qnVal.put(Constants.XSD_POSITIVEINTEGER, "1");
        qnVal.put(Constants.XSD_NEGATIVEINTEGER, "-1");
        qnVal.put(Constants.XSD_NONNEGATIVEINTEGER, "1");
        qnVal.put(Constants.XSD_NONPOSITIVEINTEGER, "-1");
        qnVal.put(Constants.XSD_SHORT, "1");
        qnVal.put(Constants.XSD_UNSIGNEDSHORT, "1");
        qnVal.put(Constants.XSD_LONG, "1");
        qnVal.put(Constants.XSD_UNSIGNEDLONG, "1");
        qnVal.put(Constants.XSD_BYTE, "1");
        qnVal.put(Constants.XSD_UNSIGNEDBYTE, "1");
        qnVal.put(Constants.XSD_DECIMAL, "1.0");
        qnVal.put(Constants.XSD_BASE64, Base64.encodeBase64String("test".getBytes()));
        qnVal.put(Constants.XSD_HEXBIN, "0xCAFEBABE");

        qnVal.put(Constants.XSD_DATE, getDateValue());
        qnVal.put(Constants.XSD_TIME, getTimeValue());
        qnVal.put(Constants.XSD_DATETIME, getDateTimeValue());
        qnVal.put(Constants.XSD_YEAR, getYear());
        qnVal.put(Constants.XSD_YEARMONTH, getYearMonth());
        qnVal.put(Constants.XSD_MONTH, getMonth());
        qnVal.put(Constants.XSD_MONTHDAY, getMonthDay());
        qnVal.put(Constants.XSD_DAY, getDay());

        qnVal.put(Constants.XSD_NORMALIZEDSTRING, "normalized string");
        qnVal.put(Constants.XSD_STRING, "string value");
        qnVal.put(Constants.XSD_TOKEN, "token");
        qnVal.put(Constants.XSD_NAME, "Name");
        qnVal.put(Constants.XSD_NCNAME, "NCName");
        qnVal.put(Constants.XSD_NMTOKEN, "token");
        qnVal.put(Constants.XSD_NMTOKENS, "token1 token2");
        qnVal.put(Constants.XSD_LANGUAGE, "en");

    }

    public static void set(QName qname, String value) {
        qnVal.put(qname, value);
    }

    public static String get(QName qn) {
        String val = "";
        if (qn != null) {
            if (qn.equals(Constants.XSD_ID)) {  //need to return unique values
                val = "id-" + new Random().nextInt(Integer.MAX_VALUE);
            } else
                val = qnVal.get(qn);
        }
        return val;
    }


    private static String getDateValue() {
        return sdfDate.format(new Date());
    }

    private static String getTimeValue() {
        return sdfTime.format(new Date());
    }

    private static String getDateTimeValue() {
        return sdfDateTime.format(new Date());
    }

    private static String getYear() {
        return sdfYear.format(new Date());
    }

    private static String getYearMonth() {
        return sdfYearMonth.format(new Date());
    }

    private static String getMonth() {
        return sdfMonth.format(new Date());
    }

    private static String getMonthDay() {
        return sdfMonthDay.format(new Date());
    }

    private static String getDay() {
        return sdfDay.format(new Date());
    }

}
