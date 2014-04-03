/*
 * Copyright (c) 2010-2014 Stratumsoft Technologies Pvt. Ltd.
 *
 * This file (ChoiceOptions.java) is part of xsd2xml.
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

/**
 * @author murakris@stratumsoft.com
 */

public enum ChoiceOptions {
    FIRST,        // the first branch of xs:choice or the head element of substitutionGroup will be chosen
    RANDOM        //a random branch of xs:choice or a substitute element or the head element of a substitutionGroup will be used.
}
