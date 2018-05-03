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

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

public class GenericInlineWriter<S extends Annotation> {

	Class<S> annotationClass;
	InlineTagFactory<S> tagFactory;

	public GenericInlineWriter(Class<S> clz) {
		annotationClass = clz;
	}

	public void write(JCas jcas, OutputStream os) {
		StringBuilder b = new StringBuilder(jcas.getDocumentText());

		Collection<? extends S> htmls = JCasUtil.select(jcas, annotationClass);
		Map<Integer, List<S>> positions = new HashMap<Integer, List<S>>();

		for (S h : htmls) {
			if (!positions.containsKey(h.getBegin())) {
				positions.put(h.getBegin(), new LinkedList<S>());
			}
			positions.get(h.getBegin()).add(h);
			if (h.getBegin() != h.getEnd()) {

				if (!positions.containsKey(h.getEnd())) {
					positions.put(h.getEnd(), new LinkedList<S>());
				}
				positions.get(h.getEnd()).add(h);
			}

		}

		for (int i = b.length() + 10; i >= 0; i--) {
			final int currentPos = i;
			if (positions.containsKey(i)) {
				TreeSet<S> ts = new TreeSet<S>(new AnnotationChooser(currentPos));
				ts.addAll(positions.get(i));
				for (S nodeAnno : ts) {
					if (nodeAnno.getEnd() == nodeAnno.getBegin() && nodeAnno.getBegin() == i)
						b.insert(i, tagFactory.getEmptyTag(nodeAnno));
					else if (nodeAnno.getEnd() == i)
						b.insert(i, tagFactory.getEndTag(nodeAnno));
					else if (nodeAnno.getBegin() == i)
						b.insert(i, tagFactory.getBeginTag(nodeAnno));
				}
			}
		}

		try (OutputStreamWriter fos = new OutputStreamWriter(os)) {
			fos.write(b.toString());
			fos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public Class<S> getAnnotationClass() {
		return annotationClass;
	}

	public void setAnnotationClass(Class<S> annotationClass) {
		this.annotationClass = annotationClass;
	}

	public InlineTagFactory<S> getTagFactory() {
		return tagFactory;
	}

	public void setTagFactory(InlineTagFactory<S> tagFactory) {
		this.tagFactory = tagFactory;
	}
}
