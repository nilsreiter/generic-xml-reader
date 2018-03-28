package de.unistuttgart.ims.uima.io.xml;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class TestRoundTrip {
	JCas jcas;
	GenericXmlReader<DocumentMetaData> gxr;
	GenericXmlWriter gxw;
	String xmlString;

	@Before
	public void setUp() throws UIMAException, IOException {
		gxr = new GenericXmlReader<DocumentMetaData>(DocumentMetaData.class);
		gxr.setPreserveWhitespace(true);
		jcas = JCasFactory.createJCas();
		gxw = new GenericXmlWriter();
	}

	@Test
	public void test1() throws IOException {
		xmlString = "<text><s><pos pos=\"det\">the</pos> <pos pos=\"nn\">dog</pos> <pos pos=\"v\">barks</pos></s> <s><pos>The</pos> <pos>cat</pos> <pos>too</pos></s></text>";
		jcas = gxr.read(jcas, IOUtils.toInputStream(xmlString, "UTF-8"));
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		gxw.write(jcas, boas);
		String s = boas.toString("UTF-8");
		assertEquals(xmlString, s);
	}

	@Test
	public void test2() throws IOException {
		xmlString = "<?xml version=\"1.0\"?>\n<text><s><pos pos=\"det\">the</pos> <pos pos=\"nn\">dog</pos> <pos pos=\"v\">barks</pos></s> <s><pos>The</pos> <pos>cat</pos> <pos>too</pos></s></text>";
		jcas = gxr.read(jcas, IOUtils.toInputStream(xmlString, "UTF-8"));
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		gxw.write(jcas, boas);
		String s = boas.toString("UTF-8");
		assertEquals(xmlString, s);
	}

	@Test
	public void test3() throws IOException {
		xmlString = "<?xml version=\"1.0\"?>\n<?xml-stylesheet type=\"text/css\" href=\"../schema/tei.css\"?>\n<text><s><pos pos=\"det\">the</pos> <pos pos=\"nn\">dog</pos> <pos pos=\"v\">barks</pos></s> <s><pos>The</pos> <pos>cat</pos> <pos>too</pos></s></text>";
		jcas = gxr.read(jcas, IOUtils.toInputStream(xmlString, "UTF-8"));
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		gxw.write(jcas, boas);
		String s = boas.toString("UTF-8");
		assertEquals(xmlString, s);
	}
}
