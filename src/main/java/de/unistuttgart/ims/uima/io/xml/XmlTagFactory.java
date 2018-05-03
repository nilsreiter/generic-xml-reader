package de.unistuttgart.ims.uima.io.xml;

import de.unistuttgart.ims.uima.io.xml.type.XMLElement;
import de.unistuttgart.ims.uima.io.xml.type.XmlDeclarationAnnotation;
import de.unistuttgart.ims.uima.io.xml.type.XmlNodeAnnotation;

public class XmlTagFactory implements InlineTagFactory<XmlNodeAnnotation> {

	@Override
	public String getBeginTag(XmlNodeAnnotation anno) {
		if (anno instanceof XMLElement) {
			return getBeginTag((XMLElement) anno);
		}
		return "";
	}

	protected String getBeginTag(XMLElement h) {
		if (h.getTag() == "#root")
			return "";

		return "<" + h.getTag() + h.getAttributes() + ">";
	}

	@Override
	public String getEmptyTag(XmlNodeAnnotation anno) {
		if (anno instanceof XMLElement)
			return getEmptyTag((XMLElement) anno);
		if (anno instanceof XmlDeclarationAnnotation)
			return getEmptyTag((XmlDeclarationAnnotation) anno);
		return "";
	}

	protected String getEmptyTag(XMLElement h) {
		return "<" + h.getTag() + h.getAttributes() + "/>";
	}

	protected String getEmptyTag(XmlDeclarationAnnotation h) {
		return h.getOuterHtml();
	}

	@Override
	public String getEndTag(XmlNodeAnnotation anno) {
		if (anno instanceof XMLElement)
			return getEndTag((XMLElement) anno);
		return "";
	}

	protected String getEndTag(XMLElement h) {
		if (h.getTag() == "#root")
			return "";
		return "</" + h.getTag() + ">";
	}

}
