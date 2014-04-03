/*
 * Copyright (c) 2010-2014 Stratumsoft Technologies Pvt. Ltd.
 *
 * This file (XmlGenOptions.java) is part of xsd2xml.
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

import org.dom4j.io.OutputFormat;

/**
 * Schema instance generator options
 *
 * @author murakris@stratumsoft.com
 * @since 1.0 (Feb 28, 2010)
 */
public class XmlGenOptions {

    private boolean isGenOptionalAttributes = true,       //generate attributes marked optional?
            isGenOptionalElements = true,          //generate elements with minOccurs = 0?
            isGenChoiceOptionsAsComments = false,   //generate the other branches in choice as comments?
            isGenCommentsForParticles = false;      //generate comments when processing particles?

    private int maxRepeatingElements = 3;           //how many elements to generate if minOccurs=0? (must be <= maxOccurs)

    private int maxRecursiveDepth = 1;              //how many recursive elements to create?

    private ChoiceOptions choiceOptions = ChoiceOptions.FIRST;

    private DefaultValues defVals = DefaultValues.DEFAULT;

    private OutputFormat outputFormat;

    public OutputFormat getOutputFormat() {
        if (outputFormat == null) {
            outputFormat = OutputFormat.createCompactFormat();   //default to compact format
        }
        return outputFormat;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public boolean isGenCommentsForParticles() {
        return isGenCommentsForParticles;
    }

    public void setGenCommentsForParticles(boolean genCommentsForParticles) {
        isGenCommentsForParticles = genCommentsForParticles;
    }

    public boolean isGenOptionalAttributes() {
        return isGenOptionalAttributes;
    }

    public void setGenOptionalAttributes(boolean genOptionalAttributes) {
        isGenOptionalAttributes = genOptionalAttributes;
    }

    public boolean isGenOptionalElements() {
        return isGenOptionalElements;
    }

    public void setGenOptionalElements(boolean genOptionalElements) {
        isGenOptionalElements = genOptionalElements;
    }

    public int getMaxRepeatingElements() {
        return maxRepeatingElements;
    }

    public void setMaxRepeatingElements(int maxRepeatingElements) {
        this.maxRepeatingElements = maxRepeatingElements;
    }

    public int getMaxRecursiveDepth() {
        return maxRecursiveDepth;
    }

    public void setMaxRecursiveDepth(int maxRecursiveDepth) {
        this.maxRecursiveDepth = maxRecursiveDepth;
    }

    public ChoiceOptions getChoiceOptions() {
        return choiceOptions;
    }

    public void setChoiceOptions(ChoiceOptions choiceOptions) {
        this.choiceOptions = choiceOptions;
    }

    public DefaultValues getDefVals() {
        return defVals;
    }

    public void setDefVals(DefaultValues defVals) {
        this.defVals = defVals;
    }

    public boolean isGenChoiceOptionsAsComments() {
        return isGenChoiceOptionsAsComments;
    }

    public void setGenChoiceOptionsAsComments(boolean genChoiceOptionsAsComments) {
        isGenChoiceOptionsAsComments = genChoiceOptionsAsComments;
    }
}
