package de.unistuttgart.ims.uima.io.xml;

import java.util.Collection;
import java.util.Iterator;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;

/**
 * Utility classes
 * 
 * @author reiterns
 *
 */
public class ArrayUtil {

	/**
	 * Creates a UIMA StringArray based on a collection of strings
	 * 
	 * @param jcas
	 *            The JCas to store the array
	 * @param coll
	 *            the collection of strings
	 * @return A StringArray that is already added to indexes
	 */
	public static StringArray toStringArray(JCas jcas, Collection<String> coll) {
		StringArray arr = new StringArray(jcas, coll.size());
		Iterator<String> collIter = coll.iterator();
		int i = 0;
		while (collIter.hasNext()) {
			arr.set(i++, collIter.next());
		}
		arr.addToIndexes();
		return arr;
	}

	public static StringArray toStringArray(JCas jcas, String... coll) {
		StringArray arr = new StringArray(jcas, coll.length);
		for (int i = 0; i < coll.length; i++) {
			arr.set(i, coll[i]);
		}
		arr.addToIndexes();
		return arr;
	}

	public static FSArray toFSArray(JCas jcas, FeatureStructure... fs) {
		FSArray arr = new FSArray(jcas, fs.length);
		for (int i = 0; i < fs.length; i++) {
			arr.set(i, fs[i]);
		}
		arr.addToIndexes();
		return arr;
	}

}
