package de.unistuttgart.ims.uima.io.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class TestGenericXmlWriter {

	JCas jcas;
	GenericXmlReader<DocumentMetaData> gxr;
	GenericXmlWriter gxw;
	String xmlString;

	@Before
	public void setUp() throws UIMAException, IOException {
		xmlString = "<text><s><pos pos=\"det\">the</pos> <pos pos=\"nn\">dog</pos> <pos pos=\"v\">barks</pos></s> <s><pos>The</pos> <pos>cat</pos> <pos>too</pos></s></text>";
		gxr = new GenericXmlReader<DocumentMetaData>(DocumentMetaData.class);
		gxr.setPreserveWhitespace(true);
		jcas = JCasFactory.createJCas();
		jcas = gxr.read(IOUtils.toInputStream(xmlString, "UTF-8"));
		gxw = new GenericXmlWriter();
	}

	@Test
	public void testWriter() throws UnsupportedEncodingException {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		gxw.write(jcas, boas);
		String s = boas.toString("UTF-8");
		assertEquals(xmlString, s);
	}

	@Test
	public void testSegmentedWriter() throws UnsupportedEncodingException {
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		gxw.write(jcas, boas, 3, 8);
		String s = boas.toString("UTF-8");
		assertEquals(" <pos pos=\"nn\">dog</pos> ", s);
	}

	@Test
	public void testPostWriting() throws UnsupportedEncodingException {
		StringWriter boas = new StringWriter();
		gxw.write(jcas, boas, 3, 8);
		String s = boas.toString();
		assertEquals(" <pos pos=\"nn\">dog</pos> ", s);
		boas.write("bla");
		assertTrue(true);
	}
}
