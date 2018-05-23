package de.unistuttgart.ims.uima.io.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class TestRealFile {
	@Test
	public void testTitle() throws UIMAException, IOException {
		JCas jcas = JCasFactory.createJCas();
		GenericXmlReader<DocumentMetaData> gxr = new GenericXmlReader<DocumentMetaData>(DocumentMetaData.class);

		gxr.addGlobalRule("titleStmt > title", (d, e) -> d.setDocumentTitle(e.text()));

		jcas = gxr.read(jcas, getClass().getResourceAsStream("/0.xml"));
		assertTrue(JCasUtil.exists(jcas, DocumentMetaData.class));
		DocumentMetaData dmd = JCasUtil.selectSingle(jcas, DocumentMetaData.class);
		assertEquals("[Widmung]", dmd.getDocumentTitle());
	}
}
