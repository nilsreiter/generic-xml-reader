package de.unistuttgart.ims.uima.io.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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

	/**
	 * Maps XML nodes to the character position where they start in the CAS
	 */
	protected Map<Node, Integer> beginMap = new HashMap<Node, Integer>();

	/**
	 * Maps CSS selectors to XMLelements
	 */
	protected Map<String, XMLElement> annotationMap = new HashMap<String, XMLElement>();

	/**
	 * An array of block elements. If {@link #preserveWhitespace} is not true,
	 * newline characters are introduced at the end of each block element
	 */
	protected String[] blockElements = new String[] { "l", "p", "sp" };

	/**
	 * Whether to preserve the whitespace exactly as it is in the original. This is
	 * needed for the XML->CAS->XML roundtrip. Defaults to false.
	 */
	protected boolean preserveWhitespace = false;

	protected Function<Element, Boolean> ignoreFunction = null;

	protected Visitor(JCas jcas) {
		this.builder = new JCasBuilder(jcas);
	}

	protected Visitor(JCas jcas, boolean preserveWhitespace) {
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
			if (node instanceof Element) {
				if (!skip((Element) node))
					beginMap.put(node, builder.getPosition());
			} else
				beginMap.put(node, builder.getPosition());
		}
	}

	@Override
	public void tail(Node node, int depth) {
		if (node instanceof Element) {
			Element elm = (Element) node;
			if (!skip(elm)) {
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
			}
			if (!this.preserveWhitespace)
				if (elm.isBlock() || ArrayUtils.contains(blockElements, elm.tagName()))
					builder.add("\n");
		} else if (node instanceof XmlDeclaration) {
			XmlDeclaration xmlDecl = (XmlDeclaration) node;
			XmlDeclarationAnnotation anno = builder.add(beginMap.get(node), XmlDeclarationAnnotation.class);
			anno.setOuterHtml(xmlDecl.outerHtml());
		}
	}

	protected JCas getJCas() {
		builder.close();
		return builder.getJCas();
	}

	protected Map<String, XMLElement> getAnnotationMap() {
		return annotationMap;
	}

	protected String[] getBlockElements() {
		return blockElements;
	}

	protected void setBlockElements(String[] blockElements) {
		this.blockElements = blockElements;
	}

	private boolean skip(Element e) {
		if (getIgnoreFunction() == null)
			return false;
		else
			return getIgnoreFunction().apply(e);
	}

	protected Function<Element, Boolean> getIgnoreFunction() {
		return ignoreFunction;
	}

	protected void setIgnoreFunction(Function<Element, Boolean> ignoreFunction) {
		this.ignoreFunction = ignoreFunction;
	}
}