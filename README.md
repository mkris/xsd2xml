xsd2xml
=======

Java-based XML Schema (XSD) to XML instance generator

# Introduction

_xsd2xml_ is a Java-based XML Schema document to XML instance generator. Unlike the approach used by JAXB,
there is no schema compilation step to generate any code. Instead, xsd2xml uses
the [Apache XMLSchema](http://ws.apache.org/commons/XmlSchema/ "Apache XMLSchema") library to
parse the given schema document and takes the root element for which the XML document must
be generated.

_xsd2xml_ originated as a component in the [Examine](http://www.stratumsoft.com/) Web Services Testing tool
and is used to generate SOAP request message payloads using the XML Schema present in a WSDL 1.1
document

## Features

* Generate attributes marked optional
* Generate elements with minOccurs = 0
* Generate the other branches in choice as comments
* Generate comments when processing particles
* Generate repeating elements based on minOccurs/maxOccurs value
* Generate recursive elements upto a configurable depth
* Generate default values for the different XmlSchema types
* Generate compact XML or pretty-printed/formatted XML

# Usage

### Load the XML Schema file from which to generate XML instances

    String path = "..."
    InputStream is = this.getClass().getResourceAsStream(path);
    URL xsdUrl = this.getClass().getResource(path);

### Create an XmlSchemaCollection and XmlSchema instance

    XmlSchemaCollection coll = new XmlSchemaCollection();
    coll.setBaseUri(xsdUrl.toString());

    StreamSource source = new StreamSource(is);
    XmlSchema schema = coll.read(source);

### Configure the XML generation options

    XmlGenOptions options = new XmlGenOptions();
    options.setGenCommentsForParticles(true);
    options.setGenChoiceOptionsAsComments(false);
    options.setMaxRecursiveDepth(1);
    options.setMaxRepeatingElements(2);
    options.setDefVals(DefaultValues.DEFAULT);

### Create an instance of SchemaTypeXmlGenerator

    SchemaTypeXmlGenerator generator = new SchemaTypeXmlGenerator(coll, options);

### Generate XML by specifying the root element QName

    boolean isPretty = true;
    String xml = generator.generateXml(elName, isPretty);

# License

*xsd2xml* is being distributed with dual-license:

* [GPLv3](http://www.gnu.org/licenses/gpl-3.0.html "GPLv3") license for open-source usage
* Commercial license for proprietary/closed-source/commercial products that require this dynamic XML generation
    functionality. Please visit [http://www.stratumsoft.com](http://www.stratumsoft.com) or contact Stratumsoft Support
    at <support@stratumsoft.com> for information about how to obtain a commercial license and its usage.
