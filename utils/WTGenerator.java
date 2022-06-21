package ext.gemalto.pdm.generic.xml.utils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import com.ptc.core.lwc.common.ScreenDefinitionName;
import ext.gemalto.pdm.generic.iba.services.GemaltoIbaHelper;
import ext.gemalto.pdm.generic.softtype.services.SoftTypeHelper;
import ext.gemalto.pdm.generic.vc.services.VersionHelper;
import ext.gemalto.pdm.generic.vc.struct.PdmStructHelper;
import ext.gemalto.pdm.generic.xml.models.XMLProperty;
import ext.gemalto.pdm.generic.xml.models.WTConstantsIF;
import ext.gemalto.pdm.generic.xml.models.XMLAssociatedObject;
import ext.gemalto.pdm.generic.xml.models.XMLAttribute;
import ext.gemalto.pdm.generic.xml.models.XMLLink;
import ext.gemalto.pdm.generic.xml.models.XMLRoot;
import ext.gemalto.pdm.sitetransfer.service.StandardTransferService;
import ext.gemalto.pdm.ws.checker.CheckerHelper;
import wt.configurablelink.ConfigurableReferenceLink;
import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.fc.Persistable;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.iba.value.IBAHolder;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartUsageLink;
import wt.part._WTPartUsageLink;
import wt.util.WTException;
import wt.vc.Mastered;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class will generate the logical Part of exporting object structure from PDM to xml file 
 *  
 * @author hmoro
 *
 */

public class WTGenerator {

	private Map<WTObject, Map<WTObject, WTObject>> usesLink;
	private static final String CLASSNAME = WTGenerator.class.getName();
	private static final Logger LOGGER = Logger.getLogger(CLASSNAME);
	private List<XMLRoot> xmlObjectChildrensList = new ArrayList<XMLRoot>();
	private List<XMLProperty> objectProperties = new ArrayList<XMLProperty>();


	private WTPart partRoot;
	private String context;
	private XMLRoot xmlRoot = new XMLRoot();
	private WTRender xmlRender = new WTRender();

	
	/**
	 *  Construct with the first parent object 
	 *  
	 * @param wtPartObject
	 * @throws WTException
	 * @throws IOException
	 */
	public WTGenerator(WTPart wtPartObject) throws WTException, IOException {

		// Set the context
		this.context = wtPartObject.getContainerName();

		// Set the root object
		this.partRoot = wtPartObject;

		// request helper initializer
		WTRequestHelper wtObjectRequesttHelper = new WTRequestHelper();

		// Get all uses part master of part root object
		wtObjectRequesttHelper.getAllUsesWTPartMasters(partRoot,
				WTPartHelper.service.getUsesWTPartMasters(wtPartObject));

		// Set Part uses link
		this.usesLink = WTRequestHelper.getMultipleXmlValuesMap();

		// Removing Parent Object from Map list
		this.usesLink.remove(this.partRoot);

		// Set Root "Xml element Attributes" via an invoking function
		// Args : invokeObjectAttributeSetter(WTPart ,xmlRootObject)
		this.xmlRoot = ((XMLRoot) appendAttributeToXmlElement(this.partRoot, this.xmlRoot));

		// Set Root Xml Attributes Names with values
		this.xmlRoot.setAttributes(buildXmlObjectAttributeElement(this.partRoot));

		// Add root property section
		objectProperties.add(buildObjectProperty(this.partRoot));
	
	}

	/**
	 * 
	 * Build usages link based on Part Structure
	 * 
	 * @throws WTException
	 */
	public void buildWTPartUsagesLinks() throws WTException {
		// CREATE ROOT ELEMENT

		// Remove null objects from the list
		xmlRoot.setXMLLinkModel(getObjectLinks(partRoot));
		// GET ALL CHILDRENS KEY
		Set<WTObject> keys = usesLink.keySet();
		WTPart wtPart = null;
		for (WTObject key : keys) {
			// FOREACH CHILD CREATE ONE XML ELEMENT
			XMLRoot partChildXmlModel = new XMLRoot();
			try {
				wtPart = (WTPart) key;

				// ADD ATTRIBUTES TO CHILD XML ELEMENT
				partChildXmlModel = ((XMLRoot) appendAttributeToXmlElement(wtPart, partChildXmlModel));

				partChildXmlModel.setAttributes(buildXmlObjectAttributeElement(wtPart));
				// SET PROPERTY XML ELEMENT
				objectProperties.add(buildObjectProperty(wtPart));
				try {
					partChildXmlModel.setXMLLinkModel(getObjectLinks(wtPart));
				} catch (WTException e) {
				}
				xmlObjectChildrensList.add(partChildXmlModel);
			} catch (WTException e) {
				LOGGER.log(Level.WARNING, e.getMessage());
			}
		}
		xmlRoot.setSubWTObjectList(xmlObjectChildrensList);
		xmlRender.setPartRootXmlModel(xmlRoot);
		xmlRoot.setProperties(getFixedPropertiesList());
		// SAVE THE XML ROOT OBJECT STRUCTURE
		xmlRender.save();

	}

	/**
	 * Java Reflection used for setting the following xml attributes - TYPE -
	 * CONTEXT - DISPLAY TYPE - IDENTITY
	 * 
	 * @param wtObject
	 * @param xmlObject
	 * @return
	 * @throws WTException
	 */
	private Object appendAttributeToXmlElement(WTObject wtObject, Object xmlObject) throws WTException {
		try {

			getSetter(WTConstantsIF.OBJECT_TYPE, xmlObject).invoke(xmlObject,
					SoftTypeHelper.service.getExactSoftTypeOfObject(wtObject));
			getSetter(WTConstantsIF.DISPLAY_NAME, xmlObject).invoke(xmlObject,
					GemaltoIbaHelper.service.getIBAValueToString((IBAHolder) wtObject, WTConstantsIF.OBJECT_TYPE));
			getSetter(WTConstantsIF.IDENTITY, xmlObject).invoke(xmlObject, GemaltoIbaHelper.service
					.getAllIBAValuesToString((IBAHolder) wtObject, WTConstantsIF.NUMBER).get(0));
			getSetter(WTConstantsIF.CONTEXT, xmlObject).invoke(xmlObject, this.context);

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOGGER.log(Level.WARNING, e.getMessage());
		}
		return xmlObject;
	}

	/**
	 * Get the setter method by variable Name and selected Object
	 * 
	 * @param variableName
	 * @param wtObjectModel
	 * @return
	 * @throws WTException
	 */
	private Method getSetter(String variableName, Object wtObjectModel) throws WTException {
		Method setter = null;
		try {
			PropertyDescriptor pd = new PropertyDescriptor(variableName, wtObjectModel.getClass());
			setter = pd.getWriteMethod();
		} catch (IntrospectionException | IllegalArgumentException e) {
			LOGGER.log(Level.WARNING, e.getMessage());
		}

		return setter;
	}

	/**
	 * Building links structure by type for listed object
	 * 
	 * @param linkType
	 * @param listLink
	 * @return
	 * @throws WTException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private XMLLink getXmlLinkElement(String linkType, QueryResult listLink) throws WTException {

		List convertedList = new ArrayList(listLink.getObjectVectorIfc().getVector());
		XMLLink linkXmlModel = null;

		// if the list has no records do nothing
		if (convertedList.size() != 0) {

			linkXmlModel = new XMLLink();
			linkXmlModel.setType(linkType);
			// AssociatedObjectLink List Initialization
			List<XMLAssociatedObject> associatedWTObjectList = new ArrayList<XMLAssociatedObject>();

			for (int i = 0; i < convertedList.size(); i++) {
				// GET THE OBJECT BY CONVERTED TYPE
				WTObject object = getWTObjectByType(convertedList.get(i));

				try {

					// SET AN ASSOCIATED OBJECT FOR EACH RECORD
					XMLAssociatedObject associatedObjectXmlModel = new XMLAssociatedObject();
					// SET THE ASSOCIATED OBJECT ATTRIBUTES
					associatedObjectXmlModel = ((XMLAssociatedObject) appendAttributeToXmlElement(object,
							associatedObjectXmlModel));

					// BUILD ASSOCIATED OBJECT ATTRIBUTE ELEMENT

					// if instance of WTPartUsageLink get the specific usage link attributes
					if (convertedList.get(i) instanceof WTPartUsageLink)
						associatedObjectXmlModel.setAttributes(
								getUsageLinkAttributesValues((WTPartUsageLink) convertedList.get(i), (WTPart) object));
					else
						// if instance of one of the casted object , put the casted one
						associatedObjectXmlModel.setAttributes(buildXmlObjectAttributeElement(object));

					// ADD TO ASSOCIATED OBJECT LIST
					associatedWTObjectList.add(associatedObjectXmlModel);
					//// SET PROPERTY XML ELEMENT
					objectProperties.add(buildObjectProperty(object));

				} catch (WTException e) {
					LOGGER.log(Level.WARNING, e.getMessage());
				}
			}
			// ADD ASSOCIATED OBJECT TO THE LINK ROOT
			linkXmlModel.setAssociatedObjectChildXmls(associatedWTObjectList);
		}
		return linkXmlModel;
	}

	/**
	 * The object property section allows us to have fast access for attributes values on import process
	 * @param wtObject
	 * @return
	 * @throws WTException
	 */
	private XMLProperty buildObjectProperty(WTObject wtObject) throws WTException {
		XMLProperty objectProperty = null;
		try {

			objectProperty = new XMLProperty();
			String strType = SoftTypeHelper.service.getExactSoftTypeOfObject(wtObject);
			getSetter("name", objectProperty).invoke(objectProperty, strType);
			List<String> attrList = CheckerHelper.getRequiredAttributeList(strType, ScreenDefinitionName.CREATE);
			String result = StringUtils.join(attrList, ", ");
			getSetter("defaultProperty", objectProperty).invoke(objectProperty, result);

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | WTException e) {
			LOGGER.log(Level.WARNING, e.getMessage());
		}

		return objectProperty;
	}

	/**
	 * Building object attributes element by object type passed as arg
	 * 
	 * @param attrObject
	 * @return
	 * @throws WTException
	 */
	private List<XMLAttribute> buildXmlObjectAttributeElement(WTObject attrObject) throws WTException {

		List<XMLAttribute> attributes = new ArrayList<XMLAttribute>();
		String strType = SoftTypeHelper.service.getExactSoftTypeOfObject(attrObject);
		List<String> attrList = CheckerHelper.getLayoutAttributeList(strType, ScreenDefinitionName.CREATE);
		attrList.addAll(Arrays.asList("number","name"));
		
		int cpt = 0;
		for (int i = 0; i < attrList.size(); i++) {

			String internalKey = GemaltoIbaHelper.service.getStringIBAValue((IBAHolder) attrObject, attrList.get(i));
			attributes.add(new XMLAttribute());
			attributes.get(cpt).setName(attrList.get(i));
			attributes.get(cpt).setInternalValue(internalKey);
			cpt++;

		}
		return attributes;

	}
	/**
	 * Retrieve Object by type / using cast to avoid ambiguity between objects type 
	 * 
	 * @param object
	 * @return
	 * @throws WTException
	 */
	private WTObject getWTObjectByType(Object object) throws WTException {

		// WTPartUsageLink Object
		if (object instanceof WTPartUsageLink) {
			try {
				object = (WTPartUsageLink) object;
				Mastered master = (Mastered) ((_WTPartUsageLink) object).getUses();
				StandardTransferService transferService = new StandardTransferService();
				WTPart child = (WTPart) transferService.getExpectedLatestIteration(master);
				object = child;
				// ADD WTOBJECT PROPERTY SECTION
				objectProperties.add(buildObjectProperty((WTObject) object));
			} catch (WTException e) {
				LOGGER.log(Level.WARNING, e.getMessage());
			}
		}
		// Configurable Refrence Link
		if (object instanceof ConfigurableReferenceLink) {
			object = (ConfigurableReferenceLink) object;
			Persistable rolebObject = ((ConfigurableReferenceLink) object).getRoleBObject();
			if (rolebObject instanceof WTPartMaster) {
				try {
					WTPartMaster currentApplicabilityMaster = (WTPartMaster) rolebObject;
					object = (WTPart) VersionHelper.service.getLatestVersion(currentApplicabilityMaster);
				} catch (WTException e) {
					LOGGER.log(Level.WARNING, e.getMessage());
				}
			}
		}
		// Document used by Document Master
		if (object instanceof WTDocument)
			object = (WTDocument) object;
		// Document Master uses Document
		if (object instanceof WTDocumentMaster) {
			try {
				object = WTRequestHelper.getDocumentByNumber(((WTDocumentMaster) object).getNumber());
			} catch (WTException e) {
				LOGGER.log(Level.WARNING, e.getMessage());
			}
		}

		return (WTObject) object;
	}

	/**
	 * The core function for setting up the link type , we prefer to use WTConstantsIF for link type.
	 * For Each link has some requirements before implemented here .
	 * 			1-  we should have the link type processing on the import step logic (See importing link section) 
	 * 			2-  The link must be appeared on the PDM , that's means on the type and attributes management (must exists)
	 * 	
	 * Note : the document link structure logic doesn't implemented yet !! 
	 * 
	 * @param wtObject
	 * @return
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	private List<XMLLink> getObjectLinks(WTPart wtObject) throws WTException {
		// SEARCH FOR WTPART USAGES LINKS
		return Arrays.asList(
				getXmlLinkElement(WTConstantsIF.PARTUSAGELINK, WTPartHelper.service.getUsesWTPartMasters(wtObject)),
				getXmlLinkElement(WTConstantsIF.DESCRIBE_LINK, WTPartHelper.service.getDescribedByDocuments(wtObject)),
				getXmlLinkElement(WTConstantsIF.REFERENCE_LINK,WTPartHelper.service.getReferencesWTDocumentMasters(wtObject)),
				getXmlLinkElement(WTConstantsIF.CONFIG_REFERENCE_LINK,PdmStructHelper.service.getApplicabilityLink(wtObject)),
				getXmlLinkElement(WTConstantsIF.CONTENT_LINK, PdmStructHelper.service.getContents(wtObject, true)),
				getXmlLinkElement(WTConstantsIF.IMPLEMENTED_BY_LINK,PdmStructHelper.service.getImplementedBy(wtObject, false)),
				getXmlLinkElement(WTConstantsIF.SHIP_WITH_LINK,PdmStructHelper.service.getShipWith(wtObject, false)),
				getXmlLinkElement(WTConstantsIF.BASED_ON_LINK, PdmStructHelper.service.getBasedOn(wtObject, false)));

	}
	
	/**
	 * 
	 * Remove duplicated properties from the list
	 * 
	 * @return
	 */
	public List<XMLProperty> getFixedPropertiesList() {
		for (int i = 0; i < this.objectProperties.size() - 1; i++) {
			String selectedName = this.objectProperties.get(i).getName().trim();
			for (int j = i + 1; j < this.objectProperties.size(); j++) {
				String comparedName = this.objectProperties.get(j).getName().trim();
				if (selectedName.equalsIgnoreCase(comparedName)) {
					this.objectProperties.remove(this.objectProperties.get(i));
					i = 0;
					break;
				}
			}
		}
		return this.objectProperties;
	}

	/**
	 * Get usage Link attributes values
	 * 
	 * @throws WTException
	 */

	public List<XMLAttribute> getUsageLinkAttributesValues(WTPartUsageLink link, WTPart wtPart) throws WTException {
		String internalValue = null;
		List<XMLAttribute> attributes = new ArrayList<XMLAttribute>();
		int cpt = 0;

		for (int i = 0; i < WTConstantsIF.usageLinkAttr.length; i++) {

			if (WTConstantsIF.usageLinkAttr[i].equalsIgnoreCase(WTConstantsIF.NUMBER)
					|| WTConstantsIF.usageLinkAttr[i].equalsIgnoreCase(WTConstantsIF.NAME))
				internalValue = GemaltoIbaHelper.service.getIBAValueToString((IBAHolder) wtPart,
						WTConstantsIF.usageLinkAttr[i]);
			else
				internalValue = GemaltoIbaHelper.service.getIBAValueToString((IBAHolder) link,
						WTConstantsIF.usageLinkAttr[i]);

			if (!internalValue.isEmpty()) {
				attributes.add(new XMLAttribute());
				attributes.get(cpt).setName(WTConstantsIF.usageLinkAttr[i].replace(WTConstantsIF.PREFIX, ""));
				attributes.get(cpt).setInternalValue(internalValue);
				cpt++;
			}
		}

		return attributes;
	}

}
