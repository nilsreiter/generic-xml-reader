package de.unistuttgart.ims.uima.io.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.XmlDeclaration;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import de.unistuttgart.ims.uima.io.xml.type.XMLElement;
import de.unistuttgart.ims.uima.io.xml.type.XMLParsingDescription;

/**
 * This class is used to generate a UIMA document from arbitrary XML. The core
 * idea is to put all text content of the XML document in the document text of
 * the JCas, and to create annotations for each XML element, covering the exact
 * string the element contains. Consider, as an example, the XML fragment
 * <code>&lt;s&gt;&lt;det&gt;the&lt;/det&gt; &lt;n&gt;dog&lt;/n&gt;&lt;/s&gt;</code>.
 * In the JCas, this will be represented as the document text "the dog", with
 * three annotations of the type {@link XMLElement}: One annotation covers the
 * entire string (and has the tag name <code>s</code> as a feature), one
 * annotation covers "the" (tag name: <code>det</code>), and one annotation
 * covers "dog" (tag name: <code>n</code>). In addition, we store a CSS selector
 * for each annotation, which allows finding the element in the DOM tree. After
 * the initial conversion, rules can be applied to convert some XML elements to
 * other UIMA annotations. Rules are expressed in CSS-like syntax.
 * 
 * <h2>Text content</h2>There are two modes in which the reader can operate. By
 * default, the <i>entire</i> text content of all XML elements is considered to
 * be the text. This can be changed by setting a "root selector", using the
 * method {@link #setTextRootSelector(String)}. Setting a CSS selector with the
 * method then retrieves <i>only</i> the text within the selected element as the
 * text content of the UIMA document. If a root text root selector has been set,
 * the distinction between global and regular rules becomes relevant. Global
 * rules are applied on all XML nodes, while regular rules are only applied on
 * the XML nodes below the root selector.
 * 
 * 
 * <h2>Rule syntax</h2> The CSS selectors are interpreted by the JSoup library.
 * See {@link org.jsoup.select.Selector} for a detailed description. Classes
 * implementing {@link de.unistuttgart.quadrama.io.core.AbstractDramaUrlReader}
 * contain usage examples.
 * 
 * <h3>Mapping rules</h3> The most common rule type is a mapping rule. Mapping
 * rules map an inline XML element onto a UIMA annotation type. Specifying, for
 * instance, <code>reader.addRule("token", Token.class)</code> (i.e., calling
 * {@link #addRule(String, Class)}) as a rule would result in UIMA annotations
 * of the type <code>Token</code> to be added on top of <i>every</i>
 * &lt;token&gt;-element in the XML source. In many cases, code should be
 * executed while mapping. This code can be added in the form of a lambda
 * expression, using {@link #addRule(String, Class, BiConsumer)}.
 * 
 * <h2>Whitespace</h2> The converter can operate in two modes that can be
 * switched with the method {@link #setPreserveWhitespace(boolean)}. If this is
 * set to true, the whitespace is preserved <i>exactly</i> as in the original
 * XML. This is what you want if the goal is to re-export XML that is as similar
 * as possible. If that's not the case, the CAS can be made much nicer by
 * setting the option to false, which is also the default. In this case, block
 * elements (as defined in {@link Visitor#blockElements}) get an extra newline
 * at the end.
 * 
 * @since 1.0.0
 */
public class GenericXmlReader<D extends TOP> {

	/**
	 * The DOM
	 */
	Document doc;

	/**
	 * A CSS expression to specify the root for the documentText
	 */
	String textRootSelector = null;

	protected Function<Element, Boolean> ignoreFunction = null;

	boolean preserveWhitespace = false;

	@SuppressWarnings("rawtypes")
	List<Rule> elementMapping = new LinkedList<Rule>();

	Map<String, Map.Entry<Element, FeatureStructure>> idRegistry = new HashMap<String, Map.Entry<Element, FeatureStructure>>();

	Class<D> documentClass;

	boolean skipEmptyElements = false;

	public GenericXmlReader(Class<D> documentClass) {
		this.documentClass = documentClass;
	}

	private static final Logger logger = LogManager.getLogger(GenericXmlReader.class);

	/**
	 * Runs the conversion and executes all rules. Produces a new JCas.
	 * 
	 * @param xmlStream The stream offering the XML data.
	 * @return The populated JCas object
	 * @throws IOException   If the input stream errors
	 * @throws UIMAException If there is an issue with creating the JCas.
	 */
	public JCas read(InputStream xmlStream) throws IOException, UIMAException {
		return read(JCasFactory.createJCas(), xmlStream);
	}

	/**
	 * Runs the conversion and executes all rules.
	 * 
	 * @param jcas
	 * @param xmlStream
	 * @return
	 * @throws IOException If the input stream errors
	 * @deprecated
	 */
	@Deprecated
	public JCas read(JCas jcas, InputStream xmlStream) throws IOException {

		// parse the input
		doc = Jsoup.parse(xmlStream, "UTF-8", "", Parser.xmlParser());

		// prepare traversing the DOM
		Visitor vis = new Visitor(jcas, isPreserveWhitespace());

		// set ignore function if needed
		if (getIgnoreFunction() != null)
			vis.setIgnoreFunction(getIgnoreFunction());

		// select the root element
		Element root;
		if (textRootSelector == null)
			root = doc;
		else
			root = doc.select(textRootSelector).first();

		// this populates the JCas, and creates XML annotations
		root.traverse(vis);

		// closes the CAS
		vis.getJCas();

		// process rules
		for (Rule<?> mapping : elementMapping) {
			applyRule(jcas, (mapping.isGlobal() ? doc : root), vis.getAnnotationMap(), mapping);
		}

		// store xml declarations
		XMLParsingDescription parsingDescription = new XMLParsingDescription(jcas);
		parsingDescription.setEncoding(doc.charset().name());
		Node rootNode = doc.root();
		List<String> declarations = new LinkedList<String>();
		for (Node topNode : rootNode.childNodes()) {
			if (topNode instanceof XmlDeclaration) {
				XmlDeclaration xmlDecl = (XmlDeclaration) topNode;
				declarations.add(xmlDecl.getWholeDeclaration());
			}
		}
		parsingDescription.setXmlDeclarations(ArrayUtil.toStringArray(jcas, declarations));
		parsingDescription.addToIndexes();
		return jcas;
	}

	public void addRule(Rule<?> rule) {
		elementMapping.add(rule);
	}

	/**
	 * This function adds a mapping between elements as expressed in the selector
	 * and annotations given by the targetClass
	 * 
	 * @param selector    The CSS selector
	 * @param targetClass The class to use for the annotations
	 */
	public <T extends TOP> void addRule(String selector, Class<T> targetClass) {
		elementMapping.add(new Rule<T>(selector, targetClass, null));
	}

	/**
	 * This function adds a mapping between elements as expressed in the selector
	 * and annotations given by the targetClass. In addition, a function can be
	 * defined to do something with the annotation and the element.
	 * 
	 * @param selector    The CSS selector, {@link org.jsoup.select.Selector} for
	 *                    syntax.
	 * @param targetClass The class to use for the annotations
	 * @param callback    A function to be executed
	 */
	public <T extends TOP> void addRule(String selector, Class<T> targetClass, BiConsumer<T, Element> callback) {
		elementMapping.add(new Rule<T>(selector, targetClass, callback));
	}

	public void addGlobalRule(String selector, BiConsumer<D, Element> callback) {
		Rule<D> r = new Rule<D>(selector, documentClass, callback, true);
		r.setUnique(true);
		elementMapping.add(r);
	}

	public <T extends TOP> void addGlobalRule(String selector, Class<T> targetClass, BiConsumer<T, Element> callback) {
		elementMapping.add(new Rule<T>(selector, targetClass, callback, true));
	}

	/**
	 * Retrieves an annotation by XML id
	 * 
	 * @param id The id
	 * @return The feature structure
	 */
	public Map.Entry<Element, FeatureStructure> getAnnotation(String id) {
		return idRegistry.get(id);
	}

	/**
	 * Checks whether an XML id is defined
	 * 
	 * @param id The id
	 * @return a boolean
	 */
	public boolean exists(String id) {
		return idRegistry.containsKey(id);
	}

	protected <T extends TOP> T getFeatureStructure(JCas jcas, XMLElement hAnno, Element elm, Rule<T> mapping) {
		T annotation = null;
		if (mapping.isUnique()) {
			annotation = getOrCreate(jcas, mapping.getTargetClass());
		} else {
			annotation = jcas.getCas().createFS(JCasUtil.getType(jcas, mapping.getTargetClass()));
			jcas.getCas().addFsToIndexes(annotation);
			if (Annotation.class.isAssignableFrom(mapping.getTargetClass())) {
				((Annotation) annotation).setBegin(hAnno.getBegin());
				((Annotation) annotation).setEnd(hAnno.getEnd());
			}

			if (elm.hasAttr("xml:id") && !exists(elm.attr("xml:id"))) {
				String id = elm.attr("xml:id");
				idRegistry.put(id, new AbstractMap.SimpleEntry<Element, FeatureStructure>(elm, annotation));
			}

		}
		return annotation;
	}

	protected <T extends TOP> void applyRule(JCas jcas, Element rootElement, Map<String, XMLElement> annoMap,
			Rule<T> mapping) {
		Elements elms = rootElement.select(mapping.getSelector());
		for (Element elm : elms) {
			XMLElement hAnno = annoMap.get(elm.cssSelector());
			if (getIgnoreFunction() != null && getIgnoreFunction().apply(elm)) {
				logger.error(
						"You are about to apply a rule that involves an XML element that has been skipped. If this works, it likely has unintended side effects.");
			}
			if (!skipEmptyElements || elm.hasText() || elm.childNodeSize() > 0) {
				T annotation = getFeatureStructure(jcas, hAnno, elm, mapping);
				if (mapping.getCallback() != null && annotation != null)
					mapping.getCallback().accept(annotation, elm);
			}
		}
	}

	/**
	 * This class represents the rules we apply
	 * 
	 *
	 * @param <T> Rules are specific for a UIMA type
	 */
	public static class Rule<T extends TOP> {
		String selector;
		BiConsumer<T, Element> callback;
		Class<T> targetClass;
		boolean global;
		boolean unique = false;

		/**
		 * 
		 * @param selector    The CSS selector
		 * @param targetClass The target class
		 * @param callback    A function to be called for every instance. Can be null.
		 * @param global      Whether to apply the rule globally or just for the text
		 *                    part
		 */
		public Rule(String selector, Class<T> targetClass, BiConsumer<T, Element> callback, boolean global) {
			this.selector = selector;
			this.callback = callback;
			this.targetClass = targetClass;
			this.global = global;
		}

		public Rule(String selector, Class<T> targetClass, BiConsumer<T, Element> callback) {
			this.selector = selector;
			this.callback = callback;
			this.targetClass = targetClass;
			this.global = false;
		}

		public Class<T> getTargetClass() {
			return this.targetClass;
		}

		public String getSelector() {
			return selector;
		}

		boolean isGlobal() {
			return this.global;
		};

		@Override
		public String toString() {
			return getSelector() + " -> " + getTargetClass().getName();
		}

		public BiConsumer<T, Element> getCallback() {
			return callback;
		}

		public boolean isUnique() {
			return unique;
		}

		public void setUnique(boolean singleton) {
			this.unique = singleton;
		}

	}

	public String getTextRootSelector() {
		return textRootSelector;
	}

	public void setTextRootSelector(String textRootSelector) {
		this.textRootSelector = textRootSelector;
	}

	public Document getDocument() {
		return doc;
	}

	public boolean isPreserveWhitespace() {
		return preserveWhitespace;
	}

	public void setPreserveWhitespace(boolean preserveWhitespace) {
		this.preserveWhitespace = preserveWhitespace;
	}

	protected static <T extends TOP> T getOrCreate(JCas jcas, Class<T> targetClass) {
		if (JCasUtil.exists(jcas, targetClass)) {
			return JCasUtil.selectSingle(jcas, targetClass);
		} else {
			T annotation = jcas.getCas().createFS(JCasUtil.getType(jcas, targetClass));
			jcas.getCas().addFsToIndexes(annotation);
			return annotation;
		}
	}

	/**
	 * Returns the set ignore function. {@see #setIgnoreFunction(Function)} for
	 * details.
	 * 
	 * @return The ignore function.
	 */
	public Function<Element, Boolean> getIgnoreFunction() {
		return ignoreFunction;
	}

	/**
	 * The specified function is applied on each element. It can be used to skip
	 * some XML elements entirely. Skipped elements will not be represented in the
	 * JCas at all, and can not be used in rules.
	 * 
	 * The main reason for using this function is to make processing faster if the
	 * XML file contains a large number of fine-grained, but unneeded tags.
	 * 
	 * @param ignoreFunction The function maps from an Element to a boolean. Note
	 *                       that the function defines which elements to skip. By
	 *                       default, all elements are included.
	 */
	public void setIgnoreFunction(Function<Element, Boolean> ignoreFunction) {
		this.ignoreFunction = ignoreFunction;
	}

	public boolean isSkipEmptyElements() {
		return skipEmptyElements;
	}

	public void setSkipEmptyElements(boolean skipEmptyElements) {
		this.skipEmptyElements = skipEmptyElements;
	}
}
