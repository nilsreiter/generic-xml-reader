package de.unistuttgart.ims.uima.io.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.unistuttgart.ims.uima.io.xml.type.XMLElement;
import de.unistuttgart.ims.uima.io.xml.type.XMLParsingDescription;

public class TEIWriter extends JCasConsumer_ImplBase {

	public static final String PARAM_OUTPUT = "Output";

	@ConfigurationParameter(name = PARAM_OUTPUT)
	String outputDirectory;

	@Override
	public void initialize(final UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		StringBuilder b = new StringBuilder(jcas.getDocumentText());

		Collection<XMLElement> htmls = JCasUtil.select(jcas, XMLElement.class);
		Map<Integer, List<XMLElement>> positions = new HashMap<Integer, List<XMLElement>>();

		for (XMLElement h : htmls) {
			if (!positions.containsKey(h.getBegin())) {
				positions.put(h.getBegin(), new LinkedList<XMLElement>());
			}
			positions.get(h.getBegin()).add(h);
			if (h.getBegin() != h.getEnd()) {

				if (!positions.containsKey(h.getEnd())) {
					positions.put(h.getEnd(), new LinkedList<XMLElement>());
				}
				positions.get(h.getEnd()).add(h);
			}

		}

		for (int i = b.length() + 10; i >= 0; i--) {
			final int currentPos = i;
			if (positions.containsKey(i)) {
				TreeSet<XMLElement> ts = new TreeSet<XMLElement>(new AnnotationChooser(currentPos));
				ts.addAll(positions.get(i));
				for (XMLElement h : ts) {
					if (h.getEnd() == h.getBegin()) {
						b.insert(i, "<" + h.getTag() + h.getAttributes() + "/>");
					} else {
						if (h.getEnd() == i) {
							b.insert(i, "</" + h.getTag() + ">");
						} else if (h.getBegin() == i) {
							b.insert(i, "<" + h.getTag() + h.getAttributes() + ">");
						}
					}
				}
			}
		}

		if (JCasUtil.exists(jcas, XMLParsingDescription.class)) {
			XMLParsingDescription xpd = JCasUtil.selectSingle(jcas, XMLParsingDescription.class);
			for (int i = xpd.getXmlDeclarations().size() - 1; i >= 0; i--) {
				b.insert(0, xpd.getXmlDeclarations(i));
			}
		} else {
			b.insert(0, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		}
		try {
			OutputStreamWriter fos = new OutputStreamWriter(getOutputStream(jcas, ".xml"));
			fos.write(b.toString());
			fos.flush();
			fos.close();
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	protected OutputStream getOutputStream(JCas jcas, String suffix) throws FileNotFoundException {
		String id = DocumentMetaData.get(jcas).getDocumentId();

		File outputDir = new File(outputDirectory);
		if (!outputDir.exists())
			outputDir.mkdirs();

		return new FileOutputStream(new File(outputDir, id + suffix));
	}

}
