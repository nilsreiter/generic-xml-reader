![Maven](https://img.shields.io/maven-central/v/de.unistuttgart.ims.uima.io/generic-xml-reader.svg)
[![Build Status](https://travis-ci.org/nilsreiter/generic-xml-reader.svg?branch=master)](https://travis-ci.org/nilsreiter/generic-xml-reader)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


# generic-xml-reader
A library to read in arbitrary XML content (including TEI) into UIMA, translating structural annotation to stand off

# Installation
```xml
<dependency>
  <groupId>de.unistuttgart.ims.uima.io</groupId>
  <artifactId>generic-xml-reader</artifactId>
  <version>1.3.0</version>
</dependency>
```

# Usage
This package converts provides a few classes to convert inline XML into 
UIMA-based stand-off annotation. How to map inline XML elements onto UIMA 
annotation types can be specified with rules.

Within certain limits, the package can also be used to export into inline XML.

# Example

Let's consider an example XML snippet

```xml
<sp who="#der_prinz">
    <speaker>DER PRINZ</speaker>
    <stage>
        <hi>an einem Arbeitstische, voller Briefschaften und Papiere, 
            deren einige er durchläuft.</hi>
    </stage>
    <p> Klagen, nichts als Klagen! Bittschriften, nichts als 
       Bittschriften! – Die traurigen Geschäfte; und man beneidet uns 
       noch! – Das glaub' ich; wenn wir allen helfen könnten: dann 
       wären wir zu beneiden. – Emilia? <hi>Indem er noch eine von den 
       Bittschriften aufschlägt, und nach dem unterschriebnen Namen 
       sieht.</hi>
    </p>
</sp>
```

We now create a new object of the class GenericXmlReader, specify a 
few rules, and read in the XML string (assuming it's in a variable called `xmlString`):

```java
GenericXmlReader<DocumentMetaData> gxr = new GenericXmlReader<DocumentMetaData>(DocumentMetaData.class);
gxr.addRule("speaker", Speaker.class);
gxr.addRule("stage", StageDirection.class);
gxr.addRule("hi", StageDirection.class);
gxr.addRule("sp", Utterance.class, (utterance, xmlElement) -> {
	utterance.setWho(xmlElement.attributeValue("who");
});
JCas jcas = gxr.read(IOUtils.toInputStream(xmlString, "UTF-8"));

```

The JCas now contains the entire text of the snippet, and several annotation layers according to the mapping rules. Plus, we have set a feature value of a UIMA annotation based on the attribute value of an XML element.