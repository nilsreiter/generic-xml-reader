package de.unistuttgart.ims.uima.io.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_DET;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_NOUN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS_VERB;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.unistuttgart.ims.uima.io.xml.type.XMLElement;
import de.unistuttgart.ims.uima.io.xml.type.XMLParsingDescription;

public class TestGenericXmlReader {

	JCas jcas;
	GenericXmlReader<DocumentMetaData> gxr;

	@Before
	public void setUp() throws UIMAException {
		jcas = JCasFactory.createJCas();
		gxr = new GenericXmlReader<DocumentMetaData>(DocumentMetaData.class);
	}

	@Test
	public void test1() throws UIMAException, IOException {
		String xmlString = "<s><det>the</det> <noun>dog</noun> <verb>barks</verb></s>";
		gxr.addRule("det", POS_DET.class);
		gxr.addRule("s", Sentence.class);
		gxr.addRule("noun", POS_NOUN.class);
		gxr.addRule("verb", POS_VERB.class);

		jcas = gxr.read(IOUtils.toInputStream(xmlString, "UTF-8"));

		assertNotNull(jcas);
		assertEquals("the dog barks", jcas.getDocumentText());

		assertTrue(JCasUtil.exists(jcas, Sentence.class));
		assertTrue(JCasUtil.exists(jcas, POS_DET.class));
		assertTrue(JCasUtil.exists(jcas, POS_NOUN.class));
		assertTrue(JCasUtil.exists(jcas, POS_VERB.class));
	}

	@Test
	public void test2() throws UIMAException, IOException {

		String xmlString = "<text><s><pos pos=\"det\">the</pos> <pos pos=\"nn\">dog</pos> <pos pos=\"v\">barks</pos></s> <s><pos>The</pos> <pos>cat</pos> <pos>too</pos></s></text>";
		gxr.addRule("s", Sentence.class);
		gxr.addRule("pos", POS.class, (anno, xmlElement) -> {
			if (xmlElement.hasAttr("pos"))
				anno.setPosValue(xmlElement.attr("pos"));
		});

		jcas = gxr.read(IOUtils.toInputStream(xmlString, "UTF-8"));

		assertNotNull(jcas);
		assertEquals("the dog barks The cat too", jcas.getDocumentText());

		assertTrue(JCasUtil.exists(jcas, Sentence.class));
		assertEquals(2, JCasUtil.select(jcas, Sentence.class).size());

		assertTrue(JCasUtil.exists(jcas, POS.class));
		assertEquals(6, JCasUtil.select(jcas, POS.class).size());
		assertEquals("det", JCasUtil.selectByIndex(jcas, POS.class, 0).getPosValue());
		assertEquals("nn", JCasUtil.selectByIndex(jcas, POS.class, 1).getPosValue());

	}

	@Test
	public void test3() throws UIMAException, IOException {

		String xmlString = "<text><head><title>The Dog Story</title><title>bla</title></head><body><s><pos pos=\"det\">the</pos> <pos pos=\"nn\">dog</pos> <pos pos=\"v\">barks</pos></s> <s><pos>The</pos> <pos>cat</pos> <pos>too</pos></s></body></text>";
		gxr.setTextRootSelector("text > body");
		gxr.addGlobalRule("text > head > title:first-child", (d, e) -> {
			d.setDocumentTitle(e.text());
		});
		gxr.addRule("s", Sentence.class);
		gxr.addRule("pos", POS.class, (anno, xmlElement) -> {
			if (xmlElement.hasAttr("pos"))
				anno.setPosValue(xmlElement.attr("pos"));
		});

		jcas = gxr.read(IOUtils.toInputStream(xmlString, "UTF-8"));

		assertNotNull(jcas);
		assertEquals("the dog barks The cat too\n", jcas.getDocumentText());

		assertTrue(JCasUtil.exists(jcas, Sentence.class));
		assertEquals(2, JCasUtil.select(jcas, Sentence.class).size());

		assertTrue(JCasUtil.exists(jcas, POS.class));
		assertEquals(6, JCasUtil.select(jcas, POS.class).size());
		assertEquals("det", JCasUtil.selectByIndex(jcas, POS.class, 0).getPosValue());
		assertEquals("nn", JCasUtil.selectByIndex(jcas, POS.class, 1).getPosValue());

		assertEquals("The Dog Story", DocumentMetaData.get(jcas).getDocumentTitle());
	}

	@Test
	public void test4() throws UIMAException, IOException {

		String xmlString = "<?xml encoding=\"UTF-8\"?>\n<?xml-stylesheet type=\"text/css\" href=\"../schema/tei.css\"?>\n<text>\n<head>\n<title>The Dog Story</title><title>bla</title></head><body><s><pos pos=\"det\">the</pos> <pos pos=\"nn\">dog</pos> <pos pos=\"v\">barks</pos></s> <s><pos>The</pos> <pos>cat</pos> <pos>too</pos></s></body></text>";
		gxr.setPreserveWhitespace(true);

		gxr.addRule("title", Sentence.class);
		gxr.addRule("s", Sentence.class);
		gxr.addRule("pos", POS.class, (anno, xmlElement) -> {
			if (xmlElement.hasAttr("pos"))
				anno.setPosValue(xmlElement.attr("pos"));
		});

		jcas = gxr.read(IOUtils.toInputStream(xmlString, "UTF-8"));

		assertNotNull(jcas);

		assertTrue(JCasUtil.exists(jcas, XMLParsingDescription.class));
		assertTrue(JCasUtil.exists(jcas, Sentence.class));
		assertEquals(4, JCasUtil.select(jcas, Sentence.class).size());

		assertEquals('\n', jcas.getDocumentText().charAt(0));
		assertEquals('\n', jcas.getDocumentText().charAt(1));
		assertEquals('\n', jcas.getDocumentText().charAt(2));
		assertEquals('\n', jcas.getDocumentText().charAt(3));
		assertEquals('T', jcas.getDocumentText().charAt(4));
		assertEquals(4, JCasUtil.selectByIndex(jcas, Sentence.class, 0).getBegin());

		assertTrue(JCasUtil.exists(jcas, POS.class));
		assertEquals(6, JCasUtil.select(jcas, POS.class).size());
		assertEquals("det", JCasUtil.selectByIndex(jcas, POS.class, 0).getPosValue());
		assertEquals("nn", JCasUtil.selectByIndex(jcas, POS.class, 1).getPosValue());

	}

	@Test
	public void test5() throws UIMAException, IOException {
		String xmlString = "<s><det><c>t</c><c>h</c><c>e</c></det><c> </c><noun><c>d</c><c>o</c><c>g</c></noun> <verb>barks</verb></s>";
		gxr.addRule("det", POS_DET.class);
		gxr.addRule("s", Sentence.class);
		gxr.addRule("noun", POS_NOUN.class);
		gxr.addRule("verb", POS_VERB.class);

		gxr.setIgnoreFunction(e -> (e.tagName().equalsIgnoreCase("c")));

		jcas = gxr.read(IOUtils.toInputStream(xmlString, "UTF-8"));

		assertNotNull(jcas);
		assertEquals("the dog barks", jcas.getDocumentText());

		assertTrue(JCasUtil.exists(jcas, Sentence.class));
		assertTrue(JCasUtil.exists(jcas, POS_DET.class));
		assertTrue(JCasUtil.exists(jcas, POS_NOUN.class));
		assertTrue(JCasUtil.exists(jcas, POS_VERB.class));

		for (XMLElement e : JCasUtil.select(jcas, XMLElement.class)) {
			assertNotEquals(e.getTag(), "c");
		}
	}

	@Test
	public void testHeader() throws UIMAException, IOException {
		String xmlString = "<TEI><teiHeader></teiHeader><body><s><det><c>t</c><c>h</c><c>e</c></det><c> </c><noun><c>d</c><c>o</c><c>g</c></noun> <verb>barks</verb></s></body></TEI>";
		gxr.addRule("det", POS_DET.class);
		gxr.addRule("s", Sentence.class);
		gxr.addRule("noun", POS_NOUN.class);
		gxr.addRule("verb", POS_VERB.class);

		gxr.setPreserveWhitespace(true);

		gxr.setIgnoreFunction(e -> (e.tagName().equalsIgnoreCase("c")));

		jcas = gxr.read(IOUtils.toInputStream(xmlString, "UTF-8"));

		assertNotNull(jcas);
		assertEquals("the dog barks", jcas.getDocumentText());

		assertTrue(JCasUtil.exists(jcas, Sentence.class));
		assertTrue(JCasUtil.exists(jcas, POS_DET.class));
		assertTrue(JCasUtil.exists(jcas, POS_NOUN.class));
		assertTrue(JCasUtil.exists(jcas, POS_VERB.class));

		for (XMLElement e : JCasUtil.select(jcas, XMLElement.class)) {
			assertNotEquals(e.getTag(), "c");
		}
	}
}
