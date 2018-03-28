package de.unistuttgart.ims.uima.io.xml;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.jcas.JCas;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.XmlDeclaration;
import org.jsoup.select.NodeVisitor;

import de.unistuttgart.ims.uima.io.xml.type.XMLElement;
import de.unistuttgart.ims.uima.io.xml.type.XmlDeclarationAnnotation;

public class Visitor implements NodeVisitor {

	protected JCasBuilder builder;
	protected Map<Node, Integer> beginMap = new HashMap<Node, Integer>();

	protected Map<String, XMLElement> annotationMap = new HashMap<String, XMLElement>();

	protected String[] blockElements = new String[] { "l", "p", "sp" };

	protected boolean preserveWhitespace = false;

	public Visitor(JCas jcas) {
		this.builder = new JCasBuilder(jcas);
	}

	public Visitor(JCas jcas, boolean preserveWhitespace) {
		this.builder = new JCasBuilder(jcas);
		this.preserveWhitespace = preserveWhitespace;
	}

	@Override
	public void head(Node node, int depth) {
		if (node instanceof TextNode) {
			if (this.preserveWhitespace)
				builder.add(((TextNode) node).getWholeText());
			else
				builder.add(((TextNode) node).text());
		} else {
			beginMap.put(node, builder.getPosition());
		}
	}

	@Override
	public void tail(Node node, int depth) {
		if (node instanceof Element) {
			Element elm = (Element) node;
			XMLElement anno = builder.add(beginMap.get(node), XMLElement.class);
			anno.setTag(elm.tagName());
			anno.setId(elm.id());
			anno.setSelector(elm.cssSelector());
			anno.setAttributes(elm.attributes().html());
			if (elm.className().isEmpty())
				anno.setCls(elm.attr("type"));
			else
				anno.setCls(elm.className());
			annotationMap.put(elm.cssSelector(), anno);
			if (!this.preserveWhitespace)
				if (elm.isBlock() || ArrayUtils.contains(blockElements, elm.tagName()))
					builder.add("\n");
		} else if (node instanceof XmlDeclaration) {
			XmlDeclaration xmlDecl = (XmlDeclaration) node;
			XmlDeclarationAnnotation anno = builder.add(beginMap.get(node), XmlDeclarationAnnotation.class);
			anno.setOuterHtml(xmlDecl.outerHtml());
		}
	}

	public JCas getJCas() {
		builder.close();
		return builder.getJCas();
	}

	public Map<String, XMLElement> getAnnotationMap() {
		return annotationMap;
	}

	public String[] getBlockElements() {
		return blockElements;
	}

	public void setBlockElements(String[] blockElements) {
		this.blockElements = blockElements;
	}
}