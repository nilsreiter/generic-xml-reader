package de.unistuttgart.ims.uima.io.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.unistuttgart.ims.uima.io.xml.type.XMLElement;
import de.unistuttgart.ims.uima.io.xml.type.XmlDeclarationAnnotation;
import de.unistuttgart.ims.uima.io.xml.type.XmlNodeAnnotation;

public class GenericXmlWriter {
	public void write(JCas jcas, OutputStream os) {
		StringBuilder b = new StringBuilder(jcas.getDocumentText());

		Collection<XmlNodeAnnotation> htmls = JCasUtil.select(jcas, XmlNodeAnnotation.class);
		Map<Integer, List<XmlNodeAnnotation>> positions = new HashMap<Integer, List<XmlNodeAnnotation>>();

		for (XmlNodeAnnotation h : htmls) {
			if (!positions.containsKey(h.getBegin())) {
				positions.put(h.getBegin(), new LinkedList<XmlNodeAnnotation>());
			}
			positions.get(h.getBegin()).add(h);
			if (h.getBegin() != h.getEnd()) {

				if (!positions.containsKey(h.getEnd())) {
					positions.put(h.getEnd(), new LinkedList<XmlNodeAnnotation>());
				}
				positions.get(h.getEnd()).add(h);
			}

		}

		for (int i = b.length() + 10; i >= 0; i--) {
			final int currentPos = i;
			if (positions.containsKey(i)) {
				TreeSet<XmlNodeAnnotation> ts = new TreeSet<XmlNodeAnnotation>(new AnnotationChooser(currentPos));
				ts.addAll(positions.get(i));
				for (XmlNodeAnnotation nodeAnno : ts) {
					if (nodeAnno instanceof XMLElement) {
						XMLElement h = (XMLElement) nodeAnno;
						if (h.getTag() == "#root")
							continue;
						if (h.getEnd() == h.getBegin()) {
							b.insert(i, "<" + h.getTag() + h.getAttributes() + "/>");
						} else {
							if (h.getEnd() == i) {
								b.insert(i, "</" + h.getTag() + ">");
							} else if (h.getBegin() == i) {
								b.insert(i, "<" + h.getTag() + h.getAttributes() + ">");
							}
						}
					} else if (nodeAnno instanceof XmlDeclarationAnnotation) {
						XmlDeclarationAnnotation h = (XmlDeclarationAnnotation) nodeAnno;
						b.insert(i, h.getOuterHtml());
					}
				}
			}
		}

		OutputStreamWriter fos = new OutputStreamWriter(os);
		try {
			fos.write(b.toString());
			fos.flush();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(fos);
		}

	}
}
