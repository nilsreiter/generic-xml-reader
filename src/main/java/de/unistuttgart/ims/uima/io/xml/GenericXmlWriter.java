package de.unistuttgart.ims.uima.io.xml;

import de.unistuttgart.ims.uima.io.xml.type.XmlNodeAnnotation;

public class GenericXmlWriter extends GenericInlineWriter<XmlNodeAnnotation> {

	public GenericXmlWriter() {
		super(XmlNodeAnnotation.class);
		tagFactory = new XmlTagFactory();
	}

}
