package ext.gemalto.pdm.generic.xml.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import ext.gemalto.pdm.generic.xml.models.XMLProperty;
import ext.gemalto.pdm.generic.xml.models.WTConstantsIF;
import ext.gemalto.pdm.generic.xml.models.XMLAssociatedObject;
import ext.gemalto.pdm.generic.xml.models.XMLAttribute;
import ext.gemalto.pdm.generic.xml.models.XMLRoot;
import ext.gemalto.pdm.ws.utils.WSConstantsIF;
import java.util.List;
import wt.doc.WTDocument;
import wt.fc.WTObject;
import wt.fc.collections.WTArrayList;
import wt.fc.collections.WTCollection;
import wt.part.WTPart;
import wt.part.WTPartUsageLink;
import wt.pom.PersistenceException;
import wt.util.WTException;
import wt.util.WTPropertyVetoException;
import wt.vc.Iterated;
import wt.vc.wip.NonLatestCheckoutException;
import wt.vc.wip.WorkInProgressException;


/**
 * 
 * This Builder is used for importing object structure based on xml form called
 * by import form processor
 * 
 * @author hmoro
 *
 */

public class WTBuilder {

	private static final Logger LOGGER = Logger.getLogger(WTBuilder.class.getName());
	public static WTPart generatedObject;
	private XMLRoot root;
	private boolean isSuccess = true;
	private int cmptA = 0, cmptB = 0, ctp = -1, cmptA2 = 0;

	/**
	 * Construct with default collections and root object initialization
	 * 
	 * @throws WTPropertyVetoException
	 * @throws PersistenceException
	 */
	public WTBuilder() {
		try {
			WTCreator.wtCollection = new HashMap<>();
			WTCreator.objectCollection = new WTArrayList();
			// Get the unmarshed object form Xml Render
			this.root = new WTRender().getUnmarsheledWTObject();

		} catch (WTException e) {
			LOGGER.log(Level.WARNING,
					" Error occured while importing unmarshled xml object [ " + e.getMessage() + " ]");
		}
	}

	/**
	 * 
	 * Import object structure form unmarshed xml file , this is the main function
	 * that has the signature of import structure function the function will return
	 * true if all objects are created successfully that's by comparing the counter
	 * of the unmarshed objects and the new created objects must be equals
	 * 
	 * @return boolean
	 * @throws WTException 
	 * @throws PersistenceException 
	 * @throws WorkInProgressException 
	 * @throws ClassNotFoundException 
	 * @throws WTPropertyVetoException 
	 * @throws IllegalAccessException 
	 * @throws NumberFormatException 
	 */
	public boolean importXmlStructure() throws WorkInProgressException, PersistenceException, WTException, ClassNotFoundException {
		try {
			fullStructureImport(root);
			// Initial the generated object by the new one which is the root to have the
			// ability for fast access on the feedBack message as url in the import form
			// processor
			generatedObject = (WTPart) WTCreator.wtCollection.get(root.getIdentity().toString());
			// Do all check in
			WTCreator.doAllCheckin();

		} catch (NumberFormatException | IllegalAccessException | WTPropertyVetoException e) {
			// TODO Auto-generated catch block
			LOGGER.log(Level.WARNING, " Error occured during import process [ " + e.getMessage() + " ]");
		}
		return true;
	}

	/**
	 * The core of getting attributes values section by names on XmlAttributes Map
	 * 
	 * @param wtPartAttributeXmlModels
	 * @param properList
	 * 
	 * @return Map
	 */
	private Map<String, String> getAllAttributesValues(List<XMLAttribute> wtPartAttributeXmlModels,
			List<String> properList) {
		Map<String, String> mapAttributes = new HashMap<>();
		for (int i = 0; i < properList.size(); i++) {
			String key = properList.get(i).trim();
			for (int j = 0; j < wtPartAttributeXmlModels.size(); j++) {
				if (wtPartAttributeXmlModels.get(j).getName().equalsIgnoreCase(key)) {
					String name = wtPartAttributeXmlModels.get(j).getName();
					String value = wtPartAttributeXmlModels.get(j).getInternalValue();
					mapAttributes.put(name, value);
				}
			}
		}
		return mapAttributes;
	}

	/**
	 * 
	 * Convert Xml Attributes Element to Map<String,String> as a pre-process Part
	 * step Creation
	 * 
	 * @param wtPartAttributeXmlModels
	 * @param property
	 * @param objectType
	 * @param context
	 * @param nameValue
	 * @return attributes map
	 * @throws WTException
	 */
	private Map<String, String> parseXmlAttributesToMap(List<XMLAttribute> wtPartAttributeXmlModels,
			XMLProperty property, String objectType, String context, String nameValue) throws WTException {
		/*
		 * Converting process
		 */

		Map<String, String> mapAttributes = new HashMap<>();
		// converting comma separate String to array of string
		String[] attributesNames = property.getDefaultProperty().split(",");
		List<String> fixedLenghtList = Arrays.asList(attributesNames);

		// Get All attributes values
		mapAttributes = getAllAttributesValues(wtPartAttributeXmlModels, fixedLenghtList);

		// Add missed attributes to list
		mapAttributes.put(WSConstantsIF.TYPEDEF, objectType);
		mapAttributes.put(WSConstantsIF.CONTAINER, context);
		mapAttributes.put("name", nameValue);

		return mapAttributes;
	}

	/**
	 * This method navigates all WTPart objects from unmarshed xml object and save
	 * them on the database
	 * 
	 * 
	 * Note : the function save all the WTObject structure in a collection to
	 * preparing them for real save
	 * 
	 * @param partRootXmlModel
	 * @throws WTPropertyVetoException
	 * @throws IllegalAccessException
	 * @throws NumberFormatException
	 * @throws WTException 
	 * @throws ClassNotFoundException 
	 */

	@SuppressWarnings({ "unused", "deprecation" })
	public void fullStructureImport(XMLRoot partRootXmlModel)
			throws NumberFormatException, IllegalAccessException, WTPropertyVetoException, WTException, ClassNotFoundException {

		LOGGER.log(Level.INFO, " Reading [ " + partRootXmlModel.getIdentity() + " ] object");
		ctp++;
		// Get the object attributes list
		List<XMLAttribute> attributeXmlModel = partRootXmlModel.getAttributes();
		String softType = partRootXmlModel.getType();
		String context = partRootXmlModel.getContext();
		// Object name
		String nameValue = partRootXmlModel.getObjectName();
		// Get the list of the properties to be added
		XMLProperty objectProperty = getObjectPropertiesBySoftType(softType);
		// Initialize the attributes map list for storing the old object identifier as
		// key and the new as value (key-value)
		Map<String, String> attributes = new HashMap<>();
	

			// Parsing attributes list with the right object properties names
			attributes = parseXmlAttributesToMap(attributeXmlModel, objectProperty, softType, context, nameValue);

			// Create WTPart Object based on the attributes list
			WTObject wtPart = (WTObject) new WTCreator(attributes).create();

			LOGGER.log(Level.INFO, " Creating - identity = [ " + wtPart.getIdentity() + " ] object");
			// Save the new created object with its old number
			WTCreator.wtCollection.put(partRootXmlModel.getIdentity().toString(), wtPart);

			if (ctp < this.root.getSubWTObjectList().size()) {
				fullStructureImport(this.root.getSubWTObjectList().get(ctp));
			} else {

				ctp = -1;
				// Create unlisted Object from the structure
				WTObject workable = createUnlistedObject(this.root);

				if (!isSuccess) {
					LOGGER.log(Level.WARNING, "Check logs for more error details - Creating objects failed");
					WTCreator.objectCollection.clear();
				} else {
					// Persist Objects
					WTCollection clk = WTCreator.buildWTCollection();
					// PART- set usages link by workable object
					workable = createWTObjectLink(this.root);
				}

			}
	}

	/**
	 * 
	 * The link creation function will create every relational parts by link type .
	 * The step will appeared after ensuring all part objects creation.
	 * 
	 * @param workable
	 * @return
	 * @throws NumberFormatException
	 * @throws IllegalAccessException
	 * @throws WTPropertyVetoException
	 * @throws NonLatestCheckoutException
	 * @throws WorkInProgressException
	 * @throws PersistenceException
	 * @throws WTException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings({ "unused" })
	private WTObject createWTObjectLink(XMLRoot workable)
			throws NumberFormatException, IllegalAccessException, WTPropertyVetoException, NonLatestCheckoutException,
			WorkInProgressException, PersistenceException, WTException, ClassNotFoundException {

		// Retrieve the workable object
		WTPart workableObject = (WTPart) WTCreator.wtCollection.get(workable.getIdentity());

		// initial usagelink
		WTPartUsageLink usageLink = null;

		// count links
		for (int i = 0; i < workable.getXMLLinkModel().size(); i++) {
			cmptB = 0;
			// get the soft type
			String linkType = workable.getXMLLinkModel().get(i).getType();

			// Get the associated link from current object link collection
			XMLAssociatedObject associatedObject = workable.getCurrentLinkCollectionByType(linkType, i, cmptB);

			while (associatedObject != null) {

				try {

					// Get the persistable link to be appended through the workable object
					WTObject persistable = (WTObject) WTCreator.wtCollection
							.get(associatedObject.getIdentity().toString());

					// Create WTPartDescribeLink objects step
					if (linkType.equalsIgnoreCase(WSConstantsIF.DESCRIBE_LINK)) {
						WTCreator.createDocument(workableObject, (WTDocument) persistable, WTConstantsIF.DESCRIBE_LINK);
	
					}

					// Create WTPartReferenceLink objects step
					if (linkType.equalsIgnoreCase(WSConstantsIF.REFERENCE_LINK)) {
						WTCreator.createDocument(workableObject, (WTDocument) persistable, WTConstantsIF.REFERENCE_LINK);
					}

					
					// Create Base on link
					if (linkType.equalsIgnoreCase(WSConstantsIF.BASED_ON_LINK)) {
						WTCreator.createBasedOnLink((WTPart) workableObject,((WTPart) persistable).getNumber().toString());
					}

					// Create Implemented by objects step / Based on
					if (linkType.equalsIgnoreCase(WSConstantsIF.IMPLEMENTED_BY_LINK)) {
						WTCreator.createImplementedLink((WTPart) persistable,(WTPart) workableObject);
					}
					
					
					// Create Content link
					if (linkType.equalsIgnoreCase(WTConstantsIF.CONTENT_LINK)) {
						WTCreator.createContents(workableObject, (Iterated) persistable);
					}

					
					// Create Configurable reference link / Sites 
					if (linkType.equalsIgnoreCase(WTConstantsIF.CONFIG_REFERENCE_LINK)) {		
						WTCreator.createConfigurableLink((Iterated) workableObject,(WTPart) persistable);
					}

				
					// Create Part Usages link
					if (linkType.equalsIgnoreCase(WSConstantsIF.PARTUSAGELINK)) {
						WTCreator.createUsagesLink((WTPart) persistable, (WTPart) workableObject, associatedObject);
					}

				} catch (WTException e) {
					LOGGER.log(Level.WARNING, " Error occured while creating links [ " + e.getMessage() + " ]");
				} finally {
					// take the next associated object
					associatedObject = workable.getCurrentLinkCollectionByType(linkType, i, ++cmptB);

				}
			}

		}

		return cmptA < this.root.getSubWTObjectList().size()
				? createWTObjectLink(this.root.getSubWTObjectList().get(cmptA++))
				: workableObject;

	}

	/**
	 * Helped Function to involve the unlisted objects as childObject on the xml
	 * file structure There are some of them which not allowed to be a child , so we
	 * use this recursive function to create each object that has appeared on link
	 * list type by looping the ones of the object's core link
	 * 
	 * @param workable
	 * @return
	 */
	@SuppressWarnings("unused")
	public WTObject createUnlistedObject(XMLRoot workable) {
		WTObject wtObject = null;
		// Retrieve the workable object
		WTPart workableObject = (WTPart) WTCreator.wtCollection.get(workable.getIdentity());
		for (int i = 0; i < workable.getXMLLinkModel().size(); i++) {
			cmptB = 0;
			// get the soft type
			String linkType = workable.getXMLLinkModel().get(i).getType();
			// Get the associated link from current object link collection
			XMLAssociatedObject associatedObject = workable.getCurrentLinkCollectionByType(linkType, i, cmptB);
			LOGGER.log(Level.INFO, "Preparing " + associatedObject.getType() + " - Link type");

			while (associatedObject != null) {
				try {
					if (WTConstantsIF.usagesLinkType.contains(linkType)) {
						wtObject = (WTObject) createWTObject(associatedObject);
						LOGGER.log(Level.INFO, "Creating associated [" + associatedObject.getIdentity() + "] Object");
					}
				} catch (WTException e) {
					LOGGER.log(Level.WARNING, " Error occured while creating objects [ " + e.getMessage() + " ]");
					isSuccess = false;
				}
				associatedObject = workable.getCurrentLinkCollectionByType(linkType, i, ++cmptB);
			}
		}

		return cmptA2 < this.root.getSubWTObjectList().size()
				? createUnlistedObject(this.root.getSubWTObjectList().get(cmptA2++))
				: workableObject;
	}

	/**
	 * 
	 * the simple function to create the unlisted objects by associated link ,
	 * called by unlisted creation function
	 * 
	 * @param associatedObjects
	 * @return
	 * @throws WTException
	 */
	public WTObject createWTObject(XMLAssociatedObject associatedObject) throws WTException {

		// Get the object attributes list
		List<XMLAttribute> attributeXmlModel = associatedObject.getAttributes();
		// Object Soft type
		String softType = associatedObject.getType();
		// Get the list of the properties to be added
		XMLProperty objectProperty = getObjectPropertiesBySoftType(softType);
		String context = associatedObject.getContext();
		// Ininialize WTDocument object
		WTObject wtObject = null;
		// Get the document name
		String nameValue = associatedObject.getObjectName();

		// GET ATTRIBUTES NAMES,VALUES AS KEY , VALUE
		Map<String, String> attributes = parseXmlAttributesToMap(attributeXmlModel, objectProperty, softType, context,
				nameValue);

		// Create WTObject
		wtObject = (WTObject) new WTCreator(attributes).create();

		// Save the new created object with its old number
		WTCreator.wtCollection.put(associatedObject.getIdentity().toString(), wtObject);

		return wtObject;

	}

	/**
	 * 
	 * Get all property object by soft type
	 * 
	 * @param softType
	 * @return WTObjectProperty
	 * 
	 */
	public XMLProperty getObjectPropertiesBySoftType(String softType) {

		XMLProperty selectedProperty = null;
		for (int i = 0; i < this.root.getProperties().size(); i++) {
			XMLProperty objectProperty = this.root.getProperties().get(i);
			if (objectProperty.getName().equalsIgnoreCase(softType)) {
				selectedProperty = objectProperty;
				break;
			}
		}
		return selectedProperty;
	}

	/**
	 * Get the value of the attribute name from WTPartRootXmlModel object
	 * 
	 * @param rootXmlModel
	 * @return Name value
	 */

	public String getName(XMLRoot rootXmlModel) {

		String nameValue = "";

		for (int i = 0; i < rootXmlModel.getAttributes().size(); i++) {

			XMLAttribute attrElement = rootXmlModel.getAttributes().get(i);
			if (attrElement.getName().equalsIgnoreCase("Name")) {
				nameValue = attrElement.getInternalValue();
				break;
			}

		}
		return nameValue;
	}

}
