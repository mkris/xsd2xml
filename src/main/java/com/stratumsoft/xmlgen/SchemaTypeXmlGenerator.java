/*
 * Copyright (c) 2010-2014 Stratumsoft Technologies Pvt. Ltd.
 *
 * This file (SchemaTypeXmlGenerator.java) is part of xsd2xml.
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
import org.apache.ws.commons.schema.*;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;
import org.apache.ws.commons.schema.utils.XmlSchemaRef;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.*;

/**
 * This class is responsible for processing an XmlSchema type and generating an xml instance for it.
 *
 * @author murakris@stratumsoft.com
 * @since 1.0 (Feb 22, 2010)
 */
public class SchemaTypeXmlGenerator {

    public static final String DEFAULT_PREFIX = "ns"; //NON-NLS

    private DocumentFactory factory;

    private XmlGenOptions options;

    private static final Logger logger = LoggerFactory.getLogger(SchemaTypeXmlGenerator.class);

    /**
     * to keep track of the types being processed in case of recursivity
     */
    private List<XmlSchemaComplexType> processedTypes;

    /**
     * keep track of the recursive count for each
     */
    private Map<QName, Integer> recursiveCount;

    private OutputFormat outputFormat;

    public XMLWriter writer;

    private XmlSchemaType lastRecursiveType;

    private XmlSchemaCollection schemaColl;

    /**
     * Keep track of the current schema whose element's /attributes are being processed
     */
    private Stack<XmlSchema> schemaStack;


    private NamespaceMap nsMap;

    private int prefixCounter = 1;

    public SchemaTypeXmlGenerator(XmlSchemaCollection schemaColl) {
        this(schemaColl, null);
    }

    public SchemaTypeXmlGenerator(XmlSchemaCollection schemaCollection, XmlGenOptions options) {
        this.schemaColl = schemaCollection;

        if (options == null) {
            this.options = new XmlGenOptions();
        } else this.options = options;

        init();
    }

    private void init() {
        factory = DocumentFactory.getInstance();

        outputFormat = options.getOutputFormat();
        outputFormat.setExpandEmptyElements(true);  //expand elements into form <a></a> instead of <a/>; TBD: expose

        processedTypes = new ArrayList<>();
        recursiveCount = new HashMap<>();

        schemaStack = new Stack<>();

        initNSMap();
    }

    private void initNSMap() {
        nsMap = new NamespaceMap();
        NamespacePrefixList nsCtx = schemaColl.getNamespaceContext();
        populateNSMap(nsCtx);

        XmlSchema[] xmlSchemas = schemaColl.getXmlSchemas();
        if (xmlSchemas != null && xmlSchemas.length > 0) {
            for (XmlSchema schema : xmlSchemas) {
                nsCtx = schema.getNamespaceContext();
                populateNSMap(nsCtx);
            }
        }
        logger.debug(MessageFormat.format("namespace map contains {0} entries", nsMap.size()));
    }

    private void populateNSMap(NamespacePrefixList nsCtx) {
        if (nsCtx != null) {
            String[] prefixes = nsCtx.getDeclaredPrefixes();
            if (prefixes != null) {
                for (String prefix : prefixes) {
                    if (StringUtils.isNotEmpty(prefix))
                        nsMap.add(prefix, nsCtx.getNamespaceURI(prefix));
                }
            }
        }
    }

    /**
     * Generate a dom4j element representing the dom structure for the given schema element qname
     *
     * @param elName qname of the element for which the xml structure must be generated
     */
    public Element generateElement(QName elName) {
        Element elem = null;
        XmlSchemaElement schEl = schemaColl.getElementByQName(elName);
        if (schEl != null)
            elem = handleElement(schEl);
        else {
            String err = MessageFormat.format("Could not get schema element for QName: {0}", elName);
            logger.error(err);
            throw new RuntimeException(err);
        }

        return elem;
    }

    /**
     * Generate an xml representation of the dom structure of the schema element
     *
     * @param elName the QName of the element for which the XML should be generated
     * @return the xml instance for the schema element, or empty string if no element with the given qname was found
     */

    public String generateXml(QName elName) {
        return generateXml(elName, false);
    }

    /**
     * Generate an xml representation of the dom structure of the schema element
     *
     * @param elName        the QName of the element for which the XML should be generated
     * @param isPrettyPrint if true formats and indents the generated xml
     * @return the xml instance for the schema element, or empty string if no element with the given qname was found
     */
    public String generateXml(QName elName, boolean isPrettyPrint) {
        String xml = "";
        if (elName != null && schemaColl != null) {

            Document doc = factory.createDocument("utf-8"); //NON-NLS
            Element el = generateElement(elName);
            if (el != null) {
                doc.add(el);
            } else {
                logger.warn("got null for element generated for qname: {}", elName);
            }

            if (isPrettyPrint)
                outputFormat = OutputFormat.createPrettyPrint();

            StringWriter sw = new StringWriter();
            writer = new XMLWriter(sw, outputFormat);
            try {
                writer.write(doc);
                xml = sw.toString();
                logger.trace("Serialized dom4j doc to xml string: {}", xml);

            } catch (IOException e) {
                logger.error("dom4j Document to xml creation error", e);
            }

        }

        return xml;
    }

    /**
     * Processes the given complex type and adds elements/attributes to the given root element
     *
     * @param typeName
     * @param rootEl
     */
    public void generateXmlForType(QName typeName, Element rootEl) {
        if (typeName != null && rootEl != null) {

            XmlSchemaType schemaType = schemaColl.getTypeByQName(typeName);
            if (schemaType != null) {

                if (schemaType instanceof XmlSchemaComplexType) {
                    handleComplexType((XmlSchemaComplexType) schemaType, rootEl);
                }
            } else {
                logger.warn("Could not locate any type with name: {}", typeName);
            }
        } else {
            logger.warn("Either type name or root element is null, cannot generate xml");
        }
    }

    private Element createDomElemFromSchemaElem(XmlSchemaElement schEl) {
        Element el = null;
        if (schEl != null) {
            QName elName = schEl.getQName();

            //get the value from the schema
            XmlSchema sch = getSchemaForElement(elName);

            //get the element form value
            XmlSchemaForm form;

            if (sch == null) {
                //not a global element 
                form = schEl.getForm();
                if (form == XmlSchemaForm.NONE) {
                    //get the default elem form value from the schema
                    String nsUri = elName.getNamespaceURI();
                    sch = getSchemaByTargetNamespace(nsUri);
                    if (sch != null) {
                        form = sch.getElementFormDefault();
                    } else {
                        form = XmlSchemaForm.UNQUALIFIED;
                    }
                }
            } else {
                //global elements must be qualified
                form = XmlSchemaForm.QUALIFIED;
            }

            org.dom4j.QName dom4jQName = createDom4jQName(elName, form);

            el = factory.createElement(dom4jQName);


        }
        return el;
    }

    public XmlSchema getSchemaByTargetNamespace(String namespaceURI) {
        if (namespaceURI != null) {

            XmlSchema[] xmlSchemas = schemaColl.getXmlSchemas();
            if (xmlSchemas != null) {
                for (XmlSchema schema : xmlSchemas) {
                    String tns = schema.getTargetNamespace();
                    if (tns != null && tns.equals(namespaceURI)) {
                        return schema;
                    }
                }
            }
        }
        return null;
    }

    private XmlSchema getSchemaForElement(QName elName) {
        XmlSchema[] xmlSchemas = schemaColl.getXmlSchemas();
        if (xmlSchemas != null && xmlSchemas.length > 0) {
            for (XmlSchema sch : xmlSchemas) {
                if (sch.getElementByName(elName) != null) {
                    return sch;

                }
            }
        }
        return null;
    }

    private org.dom4j.QName createDom4jQName(QName qname, XmlSchemaForm form) {
        org.dom4j.QName dom4jQname = null;

        if (qname != null) {
            String nsUri = qname.getNamespaceURI();
            Namespace ns = null;
            if (StringUtils.isNotEmpty(nsUri)) {
                if (form == XmlSchemaForm.QUALIFIED) {
                    String prefix = nsMap.getPrefix(nsUri);

                    if (StringUtils.isEmpty(prefix)) {
                        prefix = DEFAULT_PREFIX + prefixCounter++;
                        nsMap.add(prefix, nsUri);
                    }

                    ns = new Namespace(prefix, nsUri);
                }
            }
            dom4jQname = new org.dom4j.QName(qname.getLocalPart(), ns);
        }
        return dom4jQname;
    }

    /**
     * Return the maximum number of element's to generate based on the element's minOccurs, maxOccurs
     * and max repeating elements option
     *
     * @param minOccurs
     * @param maxOccurs
     * @return
     */
    private long getMaxElementsToGenerate(long minOccurs, long maxOccurs) {
        int maxRpt = options.getMaxRepeatingElements();

        return options.isGenOptionalElements() ?
                Math.max(minOccurs, Math.min(maxRpt, maxOccurs)) : minOccurs;
    }


    /**
     * Handle the XmlSchema Element by processing its type and returning a dom4j element equivalent
     * for it to be added to the parent dom4j element
     *
     * @param schEl the xml schema element to process
     * @return the fully constructed dom4j element equivalent
     */
    private Element handleElement(XmlSchemaElement schEl) {

        XmlSchemaRef<XmlSchemaElement> ref = schEl.getRef();
        XmlSchemaElement refEl = ref.getTarget();
        if (refEl != null) {
            logger.debug("Handling schema element reference {}", refEl.getName());

            schEl = refEl;
        }

        //keep track of the current schema we are working with
        //this is required when processing local attributes whose form value is set to 'qualified'
        XmlSchema sch = getSchemaByTargetNamespace(schEl.getQName().getNamespaceURI());

        //if the schema for this element is null, it could be a local element otherwise keep track of it
        if (sch != null) {
            logger.trace("---> Pushed schema with tns\\: {} into stack", sch.getTargetNamespace());
            schemaStack.push(sch);
        }

        //check for recursivity of this element
        XmlSchemaType type = schEl.getSchemaType();
        QName elName = schEl.getQName();

        if (type instanceof XmlSchemaComplexType) {

            //don't process this type again because this looks like a recursive call within another recursive call
            if (type == lastRecursiveType) {
                return null;
            }

            boolean isProcessed = false;
            for (XmlSchemaComplexType processedType : processedTypes) {
                if (type == processedType) {
                    isProcessed = true;
                    break;
                }
            }

            if (isProcessed) {
                //this may be start of a recursion
                processedTypes.clear();

                int count = recursiveCount.containsKey(elName) ? recursiveCount.get(elName) : 0;
                count++;
                recursiveCount.put(elName, count);

                if (count > options.getMaxRecursiveDepth()) {
                    logger.debug("recursive count exceeded max recursive depth! Resetting count; Schema el= {}", elName);
                    lastRecursiveType = type;

                    return null;
                }
            }
            processedTypes.add((XmlSchemaComplexType) type);
        }

        //create a dom4j element for this schema element
        Element dom4jEl = createDomElemFromSchemaElem(schEl);

        //process the schema type for this element
        if (type != null) {
            if (type instanceof XmlSchemaSimpleType) {
                logger.debug("Handling simple type: {}",
                        type.getName() != null ? type.getName() : "anonymous");
                handleSimpleType((XmlSchemaSimpleType) type, dom4jEl);

            } else if (type instanceof XmlSchemaComplexType) {
                logger.debug("Handling complex type: " + (type.getName() != null ? type.getName() : "anonymous")); //NON-NLS
                XmlSchemaComplexType complexType = (XmlSchemaComplexType) type;

                if (complexType.isAbstract()) {
                    //cannot instantiate an abstract type - so search for another type that
                    //'extends' it
                    //todo: handle abstract complex type
                    logger.warn("Cannot instantiate an abstract complext type!");
                } else {
                    handleComplexType(complexType, dom4jEl);
                }
            }
        }

        if (!schemaStack.isEmpty()) {
            XmlSchema sc = schemaStack.pop();
            logger.trace("<--- Popped schema with tns: {} from stack", sc.getTargetNamespace());
        }

        return dom4jEl;
    }

    /**
     * Add the element to the parent branch. The min number of times to add it is determined by the element's minOccurs
     * value and the max no. of times to add it is determined by the minimum of the max repeating elements option
     * and the element's maxOccurs value
     * <p/>
     * Note: the element may again be added multiple times based on its container minOccurs and maxOccurs values
     *
     * @param branch
     * @param elemToAdd
     * @param schElemOfElemToAdd
     */

    private void addElement(Branch branch, Element elemToAdd, XmlSchemaElement schElemOfElemToAdd) {

        long minCount = schElemOfElemToAdd.getMinOccurs();
        long maxCount = schElemOfElemToAdd.getMaxOccurs();

        //determine how many times to add this element to the parent
        if (branch != null) {

            if (maxCount > 0) {
                long maxEls = getMaxElementsToGenerate(minCount, maxCount);

                for (long i = 1; i <= maxEls; i++) {
                    if (i > minCount) {     //anything > the min count but < max is optional
                        if (options.isGenCommentsForParticles()) {
                            branch.add(factory.createComment("optional"));
                        }
                    }
                    logger.trace("Adding dom4j element: {} to branch: {}", elemToAdd.getName(), branch.getName());
                    branch.add(elemToAdd);

                    elemToAdd = elemToAdd.createCopy();     //cannot add same element again, so create a copy

                    //if id attr is present need to set a new value for it in the copied el
                    Attribute idAttr = elemToAdd.attribute("id"); //NON-NLS
                    if (idAttr != null) {
                        idAttr.setValue(SampleValueProvider.get(Constants.XSD_ID));
                    }
                }
            }
        }


    }

    /**
     * Handle the passed in complex type. this involves processing any attributes defined in this type
     * and processing the content model (simple/complex) if any. If content model is absent, process
     * any particle that is part of this complex type
     *
     * @param complexType
     * @param dom4jEl
     */
    private void handleComplexType(XmlSchemaComplexType complexType, Element dom4jEl) {

        //first process the attributes for this type
        handleComplexTypeAttributes(dom4jEl, complexType);

        //process the content type for this complex type
        //content model can be complex or simple content
        XmlSchemaContentModel model = complexType.getContentModel();
        if (model != null) {
            if (model instanceof XmlSchemaSimpleContent) {
                logger.debug("Handling simple content model for complex type {}", complexType.getName());
                handleSimpleContent(((XmlSchemaSimpleContent) model), dom4jEl);
            } else {
                logger.debug("Handling complex content model for complex type {}", complexType.getName());
                handleComplexContent(((XmlSchemaComplexContent) model), dom4jEl);
            }
        } else {
            //check if content is a particle instead
            XmlSchemaParticle particle = complexType.getParticle();
            if (particle != null) {
                logger.debug("handling complex type particle");
                handleParticle(particle, dom4jEl);
            } else {
                logger.debug("complex type has no particle or content model!");
            }
        }
    }

    /**
     * Handle the complex type attributes - this can be a straight attribute or an attribute group reference
     *
     * @param dom4jEl
     * @param complexType
     */
    private void handleComplexTypeAttributes(Element dom4jEl, XmlSchemaComplexType complexType) {
        if (complexType != null) {
            List<XmlSchemaAttributeOrGroupRef> attributes = complexType.getAttributes();
            if (attributes != null) {
                for (XmlSchemaAttributeOrGroupRef o : attributes) {
                    if (o instanceof XmlSchemaAttribute) {
                        XmlSchemaAttribute attr = (XmlSchemaAttribute) o;
                        logger.debug("handling attribute {}", attr.getName());
                        handleAttribute(attr, dom4jEl);
                    } else if (o instanceof XmlSchemaAttributeGroupRef) {
                        XmlSchemaAttributeGroupRef attrGrpRef = (XmlSchemaAttributeGroupRef) o;
                        logger.debug("handling attribute group ref");
                        handleAttributeGroupRef(attrGrpRef, dom4jEl);
                    }
                }
            }
        }
    }

    /**
     * Handle the complex contentModel for the complex type. This can be either an extension or restriction
     *
     * @param contentModel
     * @param dom4jEl
     */
    private void handleComplexContent(XmlSchemaComplexContent contentModel, Element dom4jEl) {
        if (contentModel != null) {

            XmlSchemaContent content = contentModel.getContent();
            if (content != null) {

                if (content instanceof XmlSchemaComplexContentExtension) {
                    logger.debug("complex type content model content is 'extension'");
                    handleComplexContentExtension((XmlSchemaComplexContentExtension) content, dom4jEl);

                } else if (content instanceof XmlSchemaComplexContentRestriction) {
                    logger.debug("complex type content model content is 'restriction'");
                    handleComplexContentRestriction((XmlSchemaComplexContentRestriction) content, dom4jEl);

                }
            } else {
                logger.warn("Complex type content model content is null!");
            }
        }
    }

    /**
     * Handle complex type complex restriction model
     *
     * @param restriction
     * @param dom4jEl
     */
    private void handleComplexContentRestriction(XmlSchemaComplexContentRestriction restriction, Element dom4jEl) {
        logger.debug("Handling complex content restriction...");

        if (restriction != null) {
            XmlSchemaParticle particle = restriction.getParticle();
            if (particle != null) {
                handleParticle(particle, dom4jEl);
            }

            //attributes from base type only need to be specified if they are being restricted in some way.
            //so to generate all the attributes, we have to process the parent complex types and generate all the
            //attributes they have, unless they are being restricted in some way in this restriction, in which
            //case the restricted attr should be generated

            QName baseTypeName = restriction.getBaseTypeName();
            XmlSchemaType type = schemaColl.getTypeByQName(baseTypeName);

            handleComplexTypeAttributes(dom4jEl, (XmlSchemaComplexType) type);

            List<XmlSchemaAttributeOrGroupRef> attributeOrGroupRefs = restriction.getAttributes();
            for (XmlSchemaAttributeOrGroupRef o : attributeOrGroupRefs) {
                if (o instanceof XmlSchemaAttribute) {
                    XmlSchemaAttribute attribute = (XmlSchemaAttribute) o;
                    addRestrictedAttributesToElement(dom4jEl, attribute);

                } else if (o instanceof XmlSchemaAttributeGroupRef) {
                    XmlSchemaAttributeGroupRef groupRef = (XmlSchemaAttributeGroupRef) o;
                    XmlSchemaRef<XmlSchemaAttributeGroup> ref = groupRef.getRef();

                    XmlSchemaAttributeGroup attrGrp = ref.getTarget();

                    if (attrGrp != null) {
                        List<XmlSchemaAttributeGroupMember> grpMembers = attrGrp.getAttributes();
                        if (grpMembers != null) {
                            for (XmlSchemaAttributeGroupMember grpMember : grpMembers) {
                                if (grpMember instanceof XmlSchemaAttribute) {
                                    addRestrictedAttributesToElement(dom4jEl, (XmlSchemaAttribute) grpMember);
                                }
                            }
                        }
                    }//end if
                }//end else if
            }//end for
        }//end if

    }

    /* private XmlSchemaAttributeGroup getAttributeGroup(QName name) {
        XmlSchemaAttributeGroup attrGrp = null;
        XmlSchema[] xmlSchemas = schemaColl.getXmlSchemas();
        for (XmlSchema schema : xmlSchemas) {
            attrGrp = (XmlSchemaAttributeGroup) schema.getAttributeGroups().getItem(name);
            if (attrGrp != null) {
                break;
            }
        }
        return attrGrp;
    }*/

    private void addRestrictedAttributesToElement(Element elementToAddOn, XmlSchemaAttribute attribute) {
        logger.debug("Adding restricted attribute: {}", attribute.getName());
        QName attrQName = attribute.getQName();
        if (attrQName != null) {
            org.dom4j.QName dom4jQName = createDom4jQName(attrQName, attribute.getForm());
            Attribute attr = elementToAddOn.attribute(dom4jQName);
            if (attr != null) {
                //already exists, so remove it, so we can add the restricted version of it
                logger.debug("Removing existing attribute\\: {} to add the restricted attribute", dom4jQName.getName());
                elementToAddOn.remove(attr);
            }

            attr = factory.createAttribute(elementToAddOn, dom4jQName, "");
            elementToAddOn.add(attr);
        }

    }


    /**
     * Handle complex type complex content extension model
     *
     * @param extension
     * @param dom4jEl
     */
    private void handleComplexContentExtension(XmlSchemaComplexContentExtension extension, Element dom4jEl) {

        logger.debug("Handling complex content extension...");

        if (extension != null) {
            //handle the attributes and attributeRef
            List<XmlSchemaAttributeOrGroupRef> attributeOrGroupRefs = extension.getAttributes();
            processAttributeCollection(dom4jEl, attributeOrGroupRefs);

            QName baseTypeName = extension.getBaseTypeName();
            if (baseTypeName != null) {
                logger.debug("Processing complex content base type: {}", baseTypeName);
                XmlSchemaType type = schemaColl.getTypeByQName(baseTypeName);
                handleComplexType((XmlSchemaComplexType) type, dom4jEl);
            } else {
                logger.warn("Complex content base type is null!");
            }

            //handle the complex content particle
            XmlSchemaParticle particle = extension.getParticle();
            handleParticle(particle, dom4jEl);

        }

    }

    /**
     * Handle the given xml schema particle - the particle can be all, any, choice, sequence,
     * group ref or an element
     *
     * @param particle
     * @param dom4jEl
     */
    private void handleParticle(XmlSchemaParticle particle, Element dom4jEl) {

        if (particle != null) {
            if (particle instanceof XmlSchemaAll) {
                logger.debug("handling particle 'all'...");
                handleParticleAll(((XmlSchemaAll) particle), dom4jEl);

            } else if (particle instanceof XmlSchemaAny) {
                handleParticleAny(((XmlSchemaAny) particle), dom4jEl);
                logger.debug("handling particle 'any'...");

            } else if (particle instanceof XmlSchemaChoice) {
                logger.debug("handling particle 'choice'...");
                handleParticleChoice(((XmlSchemaChoice) particle), dom4jEl);

            } else if (particle instanceof XmlSchemaSequence) {
                logger.debug("handling particle 'sequence'...");
                handleParticleSequence(((XmlSchemaSequence) particle), dom4jEl);

            } else if (particle instanceof XmlSchemaGroupRef) {
                logger.debug("handling particle 'GroupRef'...");
                handleParticleGroupRef(((XmlSchemaGroupRef) particle), dom4jEl);

            } else if (particle instanceof XmlSchemaElement) {
                XmlSchemaElement schEl = (XmlSchemaElement) particle;
                logger.debug("handling particle 'element' {}", schEl.getName());
                Element elem = handleElement(schEl);
                if (elem != null) {
                    addElement(dom4jEl, elem, schEl);
                }
            }

        } else {
            logger.warn("Schema particle is null!");
        }
    }

    private void handleParticleGroupRef(XmlSchemaGroupRef groupRef, Element dom4jEl) {
        if (groupRef != null) {
            long minCount = groupRef.getMinOccurs();
            long maxCount = groupRef.getMaxOccurs();

            logger.debug("Group reference minOccurs = {} and maxOccurs = {}", minCount, maxCount);
            QName refName = groupRef.getRefName();
            XmlSchemaGroup group = getGroup(refName);
            if (group != null) {
                logger.debug("Processing group with name: {}", refName);

                XmlSchemaGroupParticle grpParticle = group.getParticle();

                long maxEls = getMaxElementsToGenerate(minCount, maxCount);
                logger.debug("Max group ref generations will be {}", maxEls);

                for (int i = 0; i < maxEls; i++) {
                    handleParticle(grpParticle, dom4jEl);
                }
            }
        }
    }

    private XmlSchemaGroup getGroup(QName name) {
        XmlSchemaGroup group = null;
        for (XmlSchema schema : schemaColl.getXmlSchemas()) {
            group = schema.getGroups().get(name);
            if (group != null) break;
        }
        return group;
    }

    /**
     * Handle the choice particle - Depending on the option {@link XmlGenOptions#getChoiceOptions()} set,
     * either the first child particle or a random particle within choice will be processed.
     * Other elements may be generated as comments if the option
     * {@link XmlGenOptions#isGenChoiceOptionsAsComments()} is set to do so
     *
     * @param choice
     * @param dom4jEl
     */
    private void handleParticleChoice(XmlSchemaChoice choice, Element dom4jEl) {
        if (choice != null) {

            List<XmlSchemaObject> choiceItems = choice.getItems();
            int count = choiceItems.size();
            if (count > 0) {
                XmlSchemaParticle childParticle;
                switch (options.getChoiceOptions()) {
                    case FIRST:
                        childParticle = (XmlSchemaParticle) choiceItems.get(0);
                        break;
                    case RANDOM:
                        int i = new Random().nextInt(count);
                        childParticle = (XmlSchemaParticle) choiceItems.get(i);
                        break;
                    default:
                        childParticle = (XmlSchemaParticle) choiceItems.get(0);
                        break;
                }

                long minOccurs = choice.getMinOccurs();
                long maxOccurs = choice.getMaxOccurs();
                logger.debug("Choice minOccurs = {} maxOccurs = {}", minOccurs, maxOccurs);

                long maxCount = getMaxElementsToGenerate(minOccurs, maxOccurs);
                logger.debug("Adding choice particle contents: {}  times", maxCount);

                for (int x = 0; x < maxCount; x++) {

                    handleParticle(childParticle, dom4jEl);

                    //generate other elements as comments?
                    if (options.isGenChoiceOptionsAsComments()) {
                        logger.trace("Adding other elements in choice as comments");
                        for (XmlSchemaObject obj : choiceItems) {
                            if (obj != childParticle) {     //already handled
                                //generating only if the other choice is an element!
                                if (obj instanceof XmlSchemaElement) {
                                    Element optEl = handleElement((XmlSchemaElement) obj);
                                    if (optEl != null) {
                                        logger.trace("Adding element: {} as comment to choice compositor", optEl.getName());

                                        String comment = optEl.asXML();
                                        comment = comment.replace("--", "- -");  // -- is invalid within a comment, so escape it
                                        dom4jEl.addComment(comment);
                                    }
                                }
                            }
                        }

                    }
                }

            }

        }
    }

    /**
     * Handle the 'any' particle - since any element can be present, this just adds a commented
     * element if the gen comments option is set
     *
     * @param any
     * @param dom4jEl
     */
    private void handleParticleAny(XmlSchemaAny any, Element dom4jEl) {
        if (any != null) {
            if (options.isGenCommentsForParticles()) {
                dom4jEl.addComment("Any element can be present here");

                Element el = factory.createElement("SomeElement");
                dom4jEl.addComment(el.asXML());
            }
        }
    }

    /**
     * Handle the 'sequence' particle - sequence can inturn contain XmlSchemaElement, XmlSchemaGroupRef, XmlSchemaChoice,
     * XmlSchemaSequence, or XmlSchemaAny.
     *
     * @param seq
     * @param dom4jEl
     */
    private void handleParticleSequence(XmlSchemaSequence seq, Element dom4jEl) {
        if (seq != null) {
            List<XmlSchemaSequenceMember> seqItems = seq.getItems();

            if (seqItems.size() > 0) {

                if (options.isGenCommentsForParticles()) {
                    dom4jEl.addComment("sequence");
                }

                for (XmlSchemaSequenceMember seqMember : seqItems) {
                    logger.trace("Processing sequence collection particle");

                    if (seqMember instanceof XmlSchemaParticle) {

                        long min = seq.getMinOccurs();
                        long max = seq.getMaxOccurs();
                        logger.debug("sequence particle minOccurs = {}; maxOccurs = {}", min, max);

                        long maxCnt = getMaxElementsToGenerate(min, max);
                        logger.debug("handling sequence particles {} times", maxCnt);

                        for (int i = 0; i < maxCnt; i++) {
                            handleParticle((XmlSchemaParticle) seqMember, dom4jEl);
                        }

                    } else {
                        logger.error("sequence collection particle is not an instanceof XmlSchemaParticle!");
                    }
                }
            } else {
                logger.warn("sequence compositor is empty!");
            }
        }
    }

    /**
     * Handle the 'all' particle
     *
     * @param all
     * @param dom4jEl
     */
    private void handleParticleAll(XmlSchemaAll all, Element dom4jEl) {
        if (all != null) {
            List<XmlSchemaElement> allItems = all.getItems();

            if (options.isGenCommentsForParticles()) {
                //add a comment to indicate that this is an 'all' particle
                dom4jEl.addComment("following elements can appear in any order (all)");
            }

            logger.debug("Handling [{}] elements in 'all' particle", allItems.size());
            for (XmlSchemaElement schEl : allItems) {
                logger.trace("Processing element {}", schEl.getName());

                Element elem = handleElement(schEl);
                if (elem != null) {
                    addElement(dom4jEl, elem, schEl);
                }
            }

        }
    }


    /**
     * Handle the complex type simple content
     *
     * @param contentModel
     * @param dom4jEl
     */
    private void handleSimpleContent(XmlSchemaSimpleContent contentModel, Element dom4jEl) {
        if (contentModel != null) {
            XmlSchemaContent content = contentModel.getContent();
            if (content != null) {
                if (content instanceof XmlSchemaSimpleContentExtension) {
                    handleSimpleContentExtension((XmlSchemaSimpleContentExtension) content, dom4jEl);
                } else if (content instanceof XmlSchemaSimpleContentRestriction) {
                    handleSimpleContentRestriction(((XmlSchemaSimpleContentRestriction) content), dom4jEl);
                }
            }
        }
    }

    /**
     * Handle the complex type simple content restriction
     *
     * @param restriction
     * @param dom4jEl
     */
    private void handleSimpleContentRestriction(XmlSchemaSimpleContentRestriction restriction, Element dom4jEl) {
        logger.debug("Handling simple content restriction");

//        restriction.getBaseType(); //todo: process the base schema simpletype if present

        QName baseTypeName = restriction.getBaseTypeName();
        logger.debug("Simple content restriction base type name = {}", baseTypeName);

        XmlSchemaType type = schemaColl.getTypeByQName(baseTypeName);
        if (type != null) {
            if (type instanceof XmlSchemaSimpleType) {
                logger.trace("Simple content restriction base type is simple type");
                handleSimpleType((XmlSchemaSimpleType) type, dom4jEl);
            } else if (type instanceof XmlSchemaComplexType) {
                logger.trace("Simple content restriction base type is complex type");
                handleComplexType((XmlSchemaComplexType) type, dom4jEl);

                //for restriction if there are any attributes specified, they are restrictions of the
                //base complex type attributes - so removing any attributes added in the prev step
                //and specified again and re-process it

                List<XmlSchemaAttributeOrGroupRef> attributeOrGroupRefs = restriction.getAttributes();
                if (attributeOrGroupRefs != null) {
                    logger.debug("simple content restriction is overriding base type attributes");

                    //collect all attributes that are being overridden
                    List<XmlSchemaAttribute> attributes = new ArrayList<>();

                    for (XmlSchemaAttributeOrGroupRef o : attributeOrGroupRefs) {

                        if (o instanceof XmlSchemaAttribute) {
                            attributes.add((XmlSchemaAttribute) o);

                        } else if (o instanceof XmlSchemaAttributeGroupRef) {

                            XmlSchemaRef<XmlSchemaAttributeGroup> ref = ((XmlSchemaAttributeGroupRef) o).getRef();
                            if (ref != null) {
                                XmlSchemaAttributeGroup attrGroup = ref.getTarget();
                                if (attrGroup != null) {
                                    List<XmlSchemaAttributeGroupMember> attrGroupMembers = attrGroup.getAttributes();
                                    for (XmlSchemaAttributeGroupMember attrGroupMember : attrGroupMembers) {
                                        if (attrGroupMember instanceof XmlSchemaAttribute) {
                                            attributes.add((XmlSchemaAttribute) attrGroupMember);
                                        }
                                        //else todo: check if we need to handle group refs within group?
                                    }
                                }
                            }

                        }// end else if
                    }//end while

                    //now if any attribute with the same name as the overridden attribute was added from the base type
                    //remove it and add it again
                    for (XmlSchemaAttribute schAttr : attributes) {
                        QName qname = schAttr.getQName();
                        org.dom4j.QName dom4jQName = createDom4jQName(qname, schAttr.getForm());
                        logger.debug("Created dom4j qname: {} from XmlSchema qname: {}", dom4jQName, qname);

                        //remove this attribute
                        Attribute dom4jAttr = dom4jEl.attribute(dom4jQName);
                        dom4jEl.remove(dom4jAttr);

                        //handle this attribute again - but this time with the restrictions defined for it
                        handleAttribute(schAttr, dom4jEl);
                    }

                }//end if

            }//end else if
        }//end if
    }

    private void handleSimpleContentExtension(XmlSchemaSimpleContentExtension extension, Element dom4jEl) {
        logger.debug("Handling simple content extension");

        //process the attributes if any
        List<XmlSchemaAttributeOrGroupRef> attributeOrGroupRefs = extension.getAttributes();
        if (attributeOrGroupRefs != null) {
            logger.debug("Processing attributes for simple content extension");
            processAttributeCollection(dom4jEl, attributeOrGroupRefs);
        } else {
            logger.trace("Simple content extension does not have any attributes");
        }

        //process the base type
        QName baseTypeName = extension.getBaseTypeName();
        XmlSchemaType type = schemaColl.getTypeByQName(baseTypeName);
        if (type != null) {
            if (type instanceof XmlSchemaSimpleType) {
                logger.debug("Processing base simple type {} for simple content extension", type.getName());
                handleSimpleType((XmlSchemaSimpleType) type, dom4jEl);
            } else if (type instanceof XmlSchemaComplexType) {
                logger.debug("Processing base complex {} type for simple content extension", type.getName());
                handleComplexType((XmlSchemaComplexType) type, dom4jEl);
            }
        }

    }

    private void processAttributeCollection(Element dom4jEl, List<XmlSchemaAttributeOrGroupRef> attributeOrGroupRefList) {
        if (attributeOrGroupRefList != null) {

            for (XmlSchemaAttributeOrGroupRef attributeOrGroupRef : attributeOrGroupRefList) {
                if (attributeOrGroupRef instanceof XmlSchemaAttribute) {
                    XmlSchemaAttribute attr = (XmlSchemaAttribute) attributeOrGroupRef;
                    logger.debug("Handling attribute {}", attr.getName());
                    handleAttribute(attr, dom4jEl);
                } else {
                    XmlSchemaAttributeGroupRef attrGrpRef = (XmlSchemaAttributeGroupRef) attributeOrGroupRef;
                    logger.debug("Handling attribute group reference {}", attrGrpRef.getTargetQName());
                    handleAttributeGroupRef(attrGrpRef, dom4jEl);
                }
            }
        }
    }

    /**
     * Handle the attribute ref present within the complex type
     *
     * @param attrGroupRef
     * @param dom4jEl
     */
    private void handleAttributeGroupRef(XmlSchemaAttributeGroupRef attrGroupRef, Element dom4jEl) {

        XmlSchemaRef<XmlSchemaAttributeGroup> ref = attrGroupRef.getRef();
        if (ref != null) {
            logger.debug("Processing attribute group ref: {}", ref.getTargetQName());

            XmlSchemaAttributeGroup attrGrp = ref.getTarget();
            if (attrGrp != null) {
                List<XmlSchemaAttributeGroupMember> attributeGroupMembers = attrGrp.getAttributes();

                for (XmlSchemaAttributeGroupMember attributeGroupMember : attributeGroupMembers) {
                    if (attributeGroupMember instanceof XmlSchemaAttribute) {
                        XmlSchemaAttribute attr = (XmlSchemaAttribute) attributeGroupMember;
                        logger.debug("Processing attribute[{}] within attribute group", attr.getName());
                        handleAttribute(attr, dom4jEl);
                    }
                }
            }
        }

    }

    /**
     * Handle the attribute for the schema type
     *
     * @param attribute
     * @param dom4jEl
     */
    private void handleAttribute(XmlSchemaAttribute attribute, Element dom4jEl) {
        Attribute dom4jAttr = null;
        if (attribute != null) {
            if (attribute.getRef().getTarget() != null) {
                dom4jAttr = handleAttributeRef(attribute, dom4jEl);
            } else {
                dom4jAttr = handleLocalAttribute(attribute, dom4jEl);
            }

            //add this attr to this element
            if (dom4jAttr != null) {
                dom4jEl.add(dom4jAttr);
            }
        }
    }

    private Attribute handleLocalAttribute(XmlSchemaAttribute attribute, Element dom4jEl) {
        Attribute dom4jAttr = null;

        XmlSchemaUse use = attribute.getUse();
        if (use != null) {
            String name = attribute.getName();

            if (use == XmlSchemaUse.PROHIBITED) {
                logger.debug("Attribute {}'s 'use' attribute value is 'prohibited'", name);
                return null;

            } else if (use == XmlSchemaUse.REQUIRED ||
                    ((use == XmlSchemaUse.OPTIONAL || use == XmlSchemaUse.NONE)
                            && options.isGenOptionalAttributes())
                    ) {

                //if form value is NONE, check the schema's attributeFormDefault
                //if that is also NONE, default to 'unqualified'
                XmlSchemaForm form = attribute.getForm();
                logger.debug("Handling local attribute {} ; form = {}", new Object[]{name, form});

                if (form == XmlSchemaForm.NONE) {
                    //check the schema attributeFormDefault value
                    XmlSchema currentSchema = (!schemaStack.isEmpty() ? schemaStack.peek() : null);
                    if (currentSchema != null) {
                        form = currentSchema.getAttributeFormDefault();
                        logger.debug("Default attribute form to '{}'", form);
                    }
                }

                org.dom4j.QName qname = null;
                if (form == XmlSchemaForm.QUALIFIED) {
                    qname = getDom4jQNameForAttribute(attribute);
                } else {
                    qname = org.dom4j.QName.get(name);
                }

                String attrVal = getAttributeValue(attribute);
                dom4jAttr = factory.createAttribute(dom4jEl, qname, attrVal);
            }

        }


        return dom4jAttr;
    }

    /**
     * Handle a reference inside a complex type to a globally declared attribute
     *
     * @param attribute the attribute declaration with reference to another attr
     * @param dom4jEl
     * @return
     */
    private Attribute handleAttributeRef(XmlSchemaAttribute attribute, Element dom4jEl) {
        Attribute dom4jAttr = null;
        XmlSchemaUse use = attribute.getUse();
        String name = attribute.getName();
        if (use != null) {

            if (use == XmlSchemaUse.PROHIBITED) {
                logger.debug("Attribute {}'s 'use' attribute value is 'prohibited'", name);
                dom4jAttr = null;

            } else if (use == XmlSchemaUse.REQUIRED ||
                    ((use == XmlSchemaUse.OPTIONAL || use == XmlSchemaUse.NONE)
                            && options.isGenOptionalAttributes())
                    ) {

                XmlSchemaRef<XmlSchemaAttribute> ref = attribute.getRef();

                if (ref.getTarget() != null) {
                    logger.trace("Processing attribute reference");

                    XmlSchema refSchema = getSchemaByTargetNamespace(ref.getTargetQName().getNamespaceURI());
                    boolean isPushed = false;
                    if (refSchema != null) {
                        logger.trace("---> Pushed schema with tns: {} into stack", refSchema.getTargetNamespace());
                        schemaStack.push(refSchema);
                        isPushed = true;
                    }

                    logger.debug("Handling attribute reference {} with use value  {}", name, use);

                    XmlSchemaAttribute refAttr = ref.getTarget();

                    org.dom4j.QName qname = getDom4jQNameForAttribute(refAttr);
                    String attrVal = getAttributeValue(refAttr);
                    dom4jAttr = factory.createAttribute(dom4jEl, qname, attrVal);

                    if (isPushed) {
                        XmlSchema sc = schemaStack.pop();
                        logger.trace("<--- Popped schema with tns\\: {} from stack", sc.getTargetNamespace());
                    }

                }
            }

        }
        return dom4jAttr;
    }

    /**
     * Generate an appropriate value for the given xmlschema attribute based on either
     * its default / fixed attribute. If neither are present and the xml gen options indicates
     * generation of optional attribute (#DefaultValues.DEFAULT), then create a value based
     * on the attribute base type
     *
     * @param attribute
     * @return
     */
    private String getAttributeValue(XmlSchemaAttribute attribute) {
        String defVal = attribute.getDefaultValue();
        String fixedVal = attribute.getFixedValue();
        String name = attribute.getName();
        String attrVal = "";
        //if attr has a fixed value, set it
        if (StringUtils.isNotEmpty(fixedVal)) {
            logger.debug("using fixed value {} for attr: {}", fixedVal, name);
            attrVal = fixedVal;
        } else {
            //if there is already a default value
            if (StringUtils.isNotEmpty(defVal)) {
                logger.debug("using default value {} for attr: {}", defVal, name);
                attrVal = defVal;
            } else {
                //generate a default value if needed
                if (options.getDefVals() == DefaultValues.DEFAULT) {
                    attrVal = SampleValueProvider.get(attribute.getSchemaTypeName());
                    logger.debug("generating new value {}  for attr {}:", attrVal, name);
                }
            }
        }
        return attrVal;
    }

    /**
     * Create a Dom4j QName value using the given XMLSchema attribute instance data
     * The attribute's form value is qualified or is a globally-declared attribute
     *
     * @param attribute local attribute with form = qualified / global attribute
     * @return
     */

    private org.dom4j.QName getDom4jQNameForAttribute(XmlSchemaAttribute attribute) {
        QName attrQName = attribute.getQName();
        String name = attribute.getName();
        String nsUri = attrQName.getNamespaceURI();

        XmlSchema currentSchema = (!schemaStack.isEmpty() ? schemaStack.peek() : null);

        if (StringUtils.isEmpty(nsUri)) {
            nsUri = (currentSchema != null ? currentSchema.getTargetNamespace() : "");
            logger.debug("Attribute ns was empty; Setting it to current schema tns [{}]", nsUri);
        }

        org.dom4j.QName dom4jQName = null;
        if (StringUtils.isEmpty(nsUri)) {
            dom4jQName = org.dom4j.QName.get(name);
        } else {

            String prefix = nsMap.getPrefix(nsUri);
            logger.debug("Got prefix [{}] for ns uri: [{}]", prefix, nsUri);

            if (StringUtils.isEmpty(prefix)) {
                prefix = DEFAULT_PREFIX + prefixCounter++;
                nsMap.put(prefix, nsUri);
                logger.debug("Generated prefix {} for ns uri: {}", prefix, nsUri);
            }

            logger.trace("Qualifying attribute with prefix [{}] and ns [{}]", prefix, nsUri);
            dom4jQName = org.dom4j.QName.get(name, prefix, nsUri);
        }

        return dom4jQName;
    }


    /**
     * Handle the simple type
     *
     * @param simpleType
     * @param dom4jEl
     */
    private void handleSimpleType(XmlSchemaSimpleType simpleType, Element dom4jEl) {

       /* if (content != null) {
            logger.debug(I18nHelper.str("simple.type.has.content"));

            if (content instanceof XmlSchemaSimpleTypeRestriction) {
                baseTypeName = ((XmlSchemaSimpleTypeRestriction) content).getBaseTypeName();
                //todo: handle base type which is itself another declared simple type
            }

        } else {
            logger.trace(I18nHelper.str("simple.type.has.no.content.must.be.a.base.schema.type"));
            baseTypeName = new QName(Constants.URI_2001_SCHEMA_XSD, simpleType.getName());
        }
*/

        XmlSchemaSimpleTypeContent content = simpleType.getContent();
        QName baseTypeName = null;

        QName name = simpleType.getQName();
        if (name != null && name.getNamespaceURI().equals(Constants.URI_2001_SCHEMA_XSD)) {
            baseTypeName = name;
        } else if (content != null) {
            logger.debug("simple type has content");

            if (content instanceof XmlSchemaSimpleTypeRestriction) {
                baseTypeName = ((XmlSchemaSimpleTypeRestriction) content).getBaseTypeName();
                //todo: handle base type which is itself another declared simple type
            }

        }

        if (options.getDefVals().equals(DefaultValues.DEFAULT)) {
            String val = SampleValueProvider.get(baseTypeName);
            if (StringUtils.isNotEmpty(val)) {
                dom4jEl.setText(val);
                logger.debug("Adding sample value '{}' for simple type base {}", val, baseTypeName);
            } else {
                logger.warn("Could not get sample value for base type name {}", baseTypeName);
            }
        }

    }

    public XmlSchemaCollection getSchemaColl() {
        return schemaColl;
    }

    public void setSchemaColl(XmlSchemaCollection schemaColl) {
        this.schemaColl = schemaColl;
    }

    /////////////////////////////////////// Getters & Setters ///////////////////////////////////////

    public XmlGenOptions getOptions() {
        return options;
    }

    public void setOptions(XmlGenOptions defOpts) {
        this.options = defOpts;
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////

}
