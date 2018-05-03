package de.unistuttgart.ims.uima.io.xml;

import de.unistuttgart.ims.uima.io.xml.type.XMLElement;
import de.unistuttgart.ims.uima.io.xml.type.XmlDeclarationAnnotation;
import de.unistuttgart.ims.uima.io.xml.type.XmlNodeAnnotation;

public class XmlTagFactory implements InlineTagFactory<XmlNodeAnnotation> {

	@Override
	public String getBeginTag(XmlNodeAnnotation anno) {
		if (anno instanceof XMLElement) {
			XMLElement h = (XMLElement) anno;
			if (h.getBegin() == h.getEnd())
				return "<" + h.getTag() + h.getAttributes() + "/>";
			else
				return "<" + h.getTag() + h.getAttributes() + ">";
		} else if (anno instanceof XmlDeclarationAnnotation) {
			XmlDeclarationAnnotation h = (XmlDeclarationAnnotation) anno;
			return h.getOuterHtml();
		}
		return "";
	}

	@Override
	public String getEndTag(XmlNodeAnnotation anno) {
		if (anno instanceof XMLElement && anno.getBegin() != anno.getEnd()) {
			XMLElement h = (XMLElement) anno;
			return "</" + h.getTag() + ">";
		}
		return "";
	}

}
