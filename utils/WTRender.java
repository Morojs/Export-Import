package ext.gemalto.pdm.generic.xml.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import ext.gemalto.pdm.generic.xml.models.XMLRoot;
import wt.util.WTException;
import wt.util.WTProperties;

/**
 * the class provides a mechanism to write Java objects into XML and read XML as
 * objects. Simply put, you can say it is used to convert Java objects into XML
 * and vice-versa.
 * 
 * @author hmoro
 *
 */
public class WTRender {

	private static final String WTHOME;
	private static JAXBContext context;
	private static Marshaller marshaller;
	private static String WTPART_XML_DESTINATION;
	private static XMLRoot partRootXmlModel;
	private static Transformer transformer;
	private static TransformerFactory transformerFactory;
	private static DocumentBuilder dBuilder = null;
	private static final Logger LOGGER = Logger.getLogger(WTRender.class.getName());

	/**
	 * FILE DESTINATION PROPERTIES
	 * 
	 */
	static {
		try {
			WTProperties wtProps = WTProperties.getServerProperties();
			WTHOME = wtProps.getProperty("wt.home", "/windchill");
			WTPART_XML_DESTINATION = WTHOME + "/loadFiles/ext/wtpartUsagesLinks.xml";
		} catch (Exception exp) {
			throw new ExceptionInInitializerError(exp);
		}
	}

	/**
	 * DOCUMENT BUILDER
	 */
	static {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		transformerFactory = TransformerFactory.newInstance();
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			LOGGER.log(Level.ALL, e.getMessage(), e.getLocalizedMessage());
		}
	}

	/**
	 * Construct , initial context 
	 */
	public WTRender() {
		super();
		try {
			context = JAXBContext.newInstance(XMLRoot.class);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * write Java objects into XML 
	 * @throws WTException
	 */
	public void save() throws WTException {

		try {

			marshaller = context.createMarshaller();
			marshaller.setProperty("jaxb.encoding", "UTF-8");
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			Document xmlOutput = dBuilder.newDocument();
			marshaller.marshal(partRootXmlModel, xmlOutput);
			String xmlString = "";

			try {

				transformer = transformerFactory.newTransformer();

				DOMSource source = new DOMSource(xmlOutput);

				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "InternalValue");
				StreamResult result = new StreamResult(new StringWriter());

				transformer.transform(source, result);
				xmlString = result.getWriter().toString();

				Writer writer = null;

				writer = new BufferedWriter(
						new OutputStreamWriter(new FileOutputStream(WTPART_XML_DESTINATION), "utf-8"));

				writer.write(xmlString);
				writer.close();

				LOGGER.log(Level.FINE, " THE XML FILE SUCCESSFULLY GENERATED ON  " + WTPART_XML_DESTINATION);

			} catch (TransformerException | IOException e) {
				LOGGER.log(Level.WARNING, e.getMessage(), e.getLocalizedMessage());
				throw new WTException("Error Occured when parsing outputs to XML Format.");
			}

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			LOGGER.log(Level.WARNING, e.getMessage(), e.getLocalizedMessage());
			throw new WTException("Error Occured when parsing outputs to XML Format.");
		}
	}


	public XMLRoot getUnmarsheledWTObject() throws WTException {
		try {
			Unmarshaller jaxbUnmarshaller = context.createUnmarshaller();
			partRootXmlModel = (XMLRoot) jaxbUnmarshaller.unmarshal(new File(WTPART_XML_DESTINATION));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			LOGGER.log(Level.WARNING, e.getMessage(), e.getLocalizedMessage());
			throw new WTException("Error Occured when unmrashled outputs to java object .");
		}

		return partRootXmlModel;
	}

	
	public void setPartRootXmlModel(XMLRoot partRootXmlModel) {
		WTRender.partRootXmlModel = partRootXmlModel;
	}

}
