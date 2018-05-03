package de.unistuttgart.ims.uima.io.xml;

import org.apache.uima.jcas.tcas.Annotation;

public interface InlineTagFactory<S extends Annotation> {

	String getBeginTag(S anno);

	String getEndTag(S anno);

	String getEmptyTag(S anno);
}
