package ext.gemalto.pdm.generic.xml.utils;

import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import com.ptc.core.command.server.delegate.ServerCommandDelegateUtility;
import com.ptc.core.lwc.common.ScreenDefinitionName;
import com.ptc.core.meta.common.TypeIdentifier;
import com.ptc.core.meta.type.common.TypeInstance;
import ext.gemalto.pdm.generic.iba.services.GemaltoIbaHelper;
import ext.gemalto.pdm.generic.query.PDMQueryHelper;
import ext.gemalto.pdm.generic.softtype.SoftTypePropConstants;
import ext.gemalto.pdm.generic.vc.struct.PdmStructHelper;
import ext.gemalto.pdm.generic.xml.models.WTConstantsIF;
import ext.gemalto.pdm.generic.xml.models.XMLAssociatedObject;
import ext.gemalto.pdm.ws.checker.CheckerHelper;
import ext.gemalto.pdm.ws.checker.WSAttributesChecker;
import ext.gemalto.pdm.ws.checker.WSGenericChecker;
import ext.gemalto.pdm.ws.utils.WSConstantsIF;
import ext.gemalto.pdm.ws.utils.WSResource;
import ext.gemalto.pdm.ws.write.ActionsEnum;
import wt.access.NotAuthorizedException;
import wt.configurablelink.ConfigurableDescribeLink;
import wt.configurablelink.ConfigurableReferenceLink;
import wt.configuration.TraceCode;
import wt.doc.WTDocument;
import wt.doc.WTDocumentMaster;
import wt.fc.PersistenceHelper;
import wt.fc.PersistenceServerHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.fc.collections.WTCollection;
import wt.folder.Folder;
import wt.iba.value.litevalue.AbstractValueViewMap;
import wt.iba.value.service.MultiObjIBAValueDBService;
import wt.inf.container.WTContainerRef;
import wt.method.MethodContext;
import wt.org.WTPrincipal;
import wt.part.LineNumber;
import wt.part.Quantity;
import wt.part.QuantityUnit;
import wt.part.WTPart;
import wt.part.WTPartDescribeLink;
import wt.part.WTPartMaster;
import wt.part.WTPartReferenceLink;
import wt.part.WTPartUsageLink;
import wt.pom.PersistenceException;
import wt.session.SessionHelper;
import wt.type.TypeDefinitionReference;
import wt.type.TypedUtility;
import wt.util.WTException;
import wt.util.WTMessage;
import wt.util.WTPropertyVetoException;
import wt.vc.Iterated;
import wt.vc.Versioned;
import wt.vc.wip.CheckoutLink;
import wt.vc.wip.WorkInProgressException;
import wt.vc.wip.WorkInProgressHelper;
import wt.vc.wip.WorkInProgressService;
import wt.vc.wip.Workable;

public class WTCreator {

	private static final Logger LOGGER = Logger.getLogger(WTBuilder.class.getName());
	private static final String RESOURCE = WSResource.class.getName();
	private Map<String, String> attributes;
	public static WTCollection objectCollection;
	public static Map<String, WTObject> wtCollection;

	public WTCreator(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	/**
	 * check and verify the requirements when creating object
	 * 
	 * @return
	 * @throws WTException
	 */
	public WTObject create() throws WTException {

		// pre processing of the map
		Map<String, String> mapAttributes = getAttributes();
		// get object type definition from the map attributes
		String objTypeDef = mapAttributes.get(WSConstantsIF.TYPEDEF);

		if (!CheckerHelper.checkTypeIdentifier(attributes.get(WSConstantsIF.TYPEDEF))) {
			throw new WTException(WTMessage.getLocalizedMessage(RESOURCE, WSResource.VALUE_NOT_VALID,
					new Object[] { ActionsEnum.create, attributes.get(WSConstantsIF.TYPEDEF), WSConstantsIF.TYPEDEF }));
		}
		// get the container from the map attributes
		WTContainerRef wTContainerRef = CheckerHelper.getContainer(mapAttributes.get(WSConstantsIF.CONTAINER));
		if (wTContainerRef == null) {
			throw new WTException(WTMessage.getLocalizedMessage(RESOURCE, WSResource.VALUE_NOT_VALID, new Object[] {
					ActionsEnum.create, attributes.get(WSConstantsIF.CONTAINER), WSConstantsIF.CONTAINER }));
		}
		// validate that the required attributes are available in the provided map
		CheckerHelper.validateRequiredAttribute(objTypeDef, mapAttributes, ScreenDefinitionName.CREATE);
		WTObject wtObj = null;

		try {

			TypeInstance typeInstance = CheckerHelper.getTypeInstance(mapAttributes, wTContainerRef);
			boolean already = MethodContext.getContext().containsKey(MultiObjIBAValueDBService.METHOD_CONTEXT_KEY);
			if (!already) {
				MethodContext.getContext().put(MultiObjIBAValueDBService.METHOD_CONTEXT_KEY,
						new AbstractValueViewMap());
			}

			// convert the type instance to a persistable object
			wtObj = (WTObject) ServerCommandDelegateUtility.translate(typeInstance, wtObj);

			// get object type definition from the map attributes
			TypeIdentifier typeIdentifier = CheckerHelper.getTypeIdentifier(objTypeDef);
			// apply values on hard attributes
			wtObj = CheckerHelper.applyHardAttributes(wtObj, typeIdentifier, wTContainerRef, mapAttributes);

			// Save the new created object / WTCollection Pre-process
			objectCollection.add(wtObj);

			WSGenericChecker checker = new WSAttributesChecker(wtObj, mapAttributes, ActionsEnum.create);
			checker.performCheck();

		} catch (NotAuthorizedException noe) {
			WTPrincipal principal = SessionHelper.manager.getPrincipal();

			LOGGER.log(Level.WARNING, "Error occured during the " + ActionsEnum.create + " action + the user "
					+ principal.getName() + "has no permissions on object " + noe);
			throw new WTException(WTMessage.getLocalizedMessage(RESOURCE, WSResource.RIGHTS_PERMISSION_ERROR,
					new Object[] { ActionsEnum.create }));
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error occured during create object object " + e.getMessage());
			throw new WTException(e.getLocalizedMessage());
		}
		return wtObj;
	}

	
	/**
	 * Set Check in for each object presented on the collection 
	 * @throws WorkInProgressException
	 * @throws WTPropertyVetoException
	 * @throws PersistenceException
	 * @throws WTException
	 */
	public static void doAllCheckin()
			throws WorkInProgressException, WTPropertyVetoException, PersistenceException, WTException {

		if (wtCollection.size() != 0) {

			LOGGER.info("Do check in all ... ");

			// Check in objects collections
			for (Entry<String, WTObject> obj : wtCollection.entrySet()) {
				WTObject selectedObject = (WTObject) obj.getValue();

				if (selectedObject instanceof WTPart)
					selectedObject = WTRequestHelper.getWTPartByNumber(((WTPart) obj.getValue()).getNumber());

				if (selectedObject instanceof WTDocument)
					selectedObject = WTRequestHelper.getDocumentByNumber(((WTDocument) obj.getValue()).getNumber());

				// do check in
				if (WorkInProgressHelper.isCheckedOut((Workable) selectedObject)
						&& WorkInProgressHelper.isWorkingCopy((Workable) selectedObject)) {
					selectedObject = (WTPart) WorkInProgressHelper.service.checkin((Workable) selectedObject, "");
				}
			}
		} else
			LOGGER.info("No Objects checking in - check logs for more infos ...");
	}

	/**
	 * Checkout and return the working copy of the part. If the part is checkouted
	 * returns only the working copy.
	 * 
	 * @param selectedPart
	 * @return
	 * @throws WTException
	 */
	public static Workable doCheckout(WTPart selectedPart) throws WTException {

		Workable workable;
		if (WorkInProgressHelper.isCheckedOut(selectedPart, SessionHelper.getPrincipal())) {
			if (!WorkInProgressHelper.isWorkingCopy(selectedPart)) {
				workable = WorkInProgressHelper.service.workingCopyOf(selectedPart);
			} else {
				workable = selectedPart;
			}
		} else {

			try {
				Folder folder = WorkInProgressHelper.service.getCheckoutFolder();
				workable = WorkInProgressHelper.service.checkout(selectedPart, folder, "").getWorkingCopy();
			} catch (WTPropertyVetoException e) {
				LOGGER.log(Level.WARNING, "Error occured during check out object " + e.getMessage());
				throw new WTException(e);
			}
		}

		return workable;
	}

	/**
	 * 
	 * This method will create the Implemeneted by link between the parent item and
	 * the child object Passed as args
	 * 
	 * @param parent
	 * @param child
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */
	public static void createImplementedLink(Versioned parent, Versioned child)
			throws WTException, WTPropertyVetoException {

		QueryResult qrResImplBy = PdmStructHelper.service.getImplementedBy((WTObject) child, true);
		if (!qrResImplBy.hasMoreElements()) {

			String strLink = WTConstantsIF.IMPLEMENTED_BY_LINK;

			WorkInProgressService wipService = WorkInProgressHelper.service;

			if (!WorkInProgressHelper.isCheckedOut((Workable) child)) {
				CheckoutLink col = wipService.checkout((Workable) child, wipService.getCheckoutFolder(),
						"CheckedOut by WTCreator for Link creation.");

				child = (WTPart) col.getWorkingCopy();
			}
			TypeDefinitionReference tdRef = TypedUtility.getTypeDefinitionReference(strLink);
			ConfigurableReferenceLink clink = ConfigurableReferenceLink.newConfigurableReferenceLink(child,
					parent.getMaster(), tdRef);

			PersistenceHelper.manager.save(clink);

			child = (WTPart) WorkInProgressHelper.service.checkin((Workable) child,
					"Automatically checked in by WTCreator after link creation.");
		}
	}

	/**
	 * Create Described and referenced link
	 * 
	 * @return
	 * @throws WTException
	 */

	public static void createDocument(WTPart parent, WTDocument document, String docType) {

		WTObject doc = null;
		try {

			if (docType.equalsIgnoreCase(WSConstantsIF.DESCRIBE_LINK))
				doc = WTPartDescribeLink.newWTPartDescribeLink((WTPart) parent, (WTDocument) document);

			if (docType.equalsIgnoreCase(WSConstantsIF.REFERENCE_LINK))
				doc = WTPartReferenceLink.newWTPartReferenceLink((WTPart) parent,
						(WTDocumentMaster) ((WTDocument) document).getMaster());

			// save as link
			doc = (WTObject) PersistenceHelper.manager.save(doc);
			
			if (doc != null)
				LOGGER.log(Level.INFO,docType + " was created successfully ...");
			
			
		} catch (WTException e) {
			LOGGER.log(Level.WARNING, "Error occured during document object creation " + e.getMessage());

		}
	

	}

	/**
	 * Create usages link / with Standard and IBAs Attributes
	 * 
	 * @return
	 * @throws WTException
	 */

	public static void createUsagesLink(WTPart persistable, WTPart workableObject,
			XMLAssociatedObject associatedObject) {
		// initial usagelink
		  WTPartUsageLink usageLink = null;
		try {

			WTPartMaster softMaster = (WTPartMaster) ((WTPart) persistable).getMaster();
			usageLink = WTPartUsageLink.newWTPartUsageLink((WTPart) workableObject, softMaster);
			PersistenceServerHelper.manager.insert(usageLink);
			usageLink = setStandardAndIBAsAttributes(usageLink, associatedObject);
			

		} catch (WTException | NumberFormatException | WTPropertyVetoException e) {
			LOGGER.log(Level.WARNING, "Error occured during usage link creation " + e.getMessage());
		}

	}

	/**
	 * Set Standards/IBAs Attributes
	 * 
	 * @return
	 * @throws NumberFormatException
	 * @throws WTException
	 * @throws WTPropertyVetoException
	 */

	public static WTPartUsageLink setStandardAndIBAsAttributes(WTPartUsageLink usageLink,
			XMLAssociatedObject associatedObject) throws NumberFormatException, WTException, WTPropertyVetoException {

		// Standard attributes updates
		if (StringUtils.isNotEmpty(associatedObject.getAttributeValueByName(WTConstantsIF.TRACE_CODE))) {
			TraceCode tracecode = TraceCode
					.toTraceCode(associatedObject.getAttributeValueByName(WTConstantsIF.TRACE_CODE));
			usageLink.setTraceCode(tracecode);
		}

		if (StringUtils.isNotEmpty(associatedObject.getAttributeValueByName(WTConstantsIF.LINE_NUMBER))) {
			LineNumber lineNumber = LineNumber
					.newLineNumber(Long.parseLong(associatedObject.getAttributeValueByName(WTConstantsIF.LINE_NUMBER)));
			usageLink.setLineNumber(lineNumber);
		}

		if (StringUtils.isNotEmpty(associatedObject.getAttributeValueByName(WTConstantsIF.FIND_NUMBER))) {
			String fnumber = associatedObject.getAttributeValueByName(WTConstantsIF.FIND_NUMBER);
			usageLink.setFindNumber(fnumber);
		}

		if (StringUtils.isNotEmpty(associatedObject.getAttributeValueByName(WTConstantsIF.POSITION))) {
			usageLink = (WTPartUsageLink) GemaltoIbaHelper.service.setIba(usageLink, WTConstantsIF.POSITION,
					associatedObject.getAttributeValueByName(WTConstantsIF.POSITION));
		}

		if (StringUtils.isNotEmpty(associatedObject.getAttributeValueByName(WTConstantsIF.LAYER))) {
			usageLink = (WTPartUsageLink) GemaltoIbaHelper.service.setIba(usageLink, WTConstantsIF.LAYER,
					associatedObject.getAttributeValueByName(WTConstantsIF.LAYER));
		}

		if (StringUtils.isNotEmpty(associatedObject.getAttributeValueByName(WTConstantsIF.BASIS))) {
			usageLink = (WTPartUsageLink) GemaltoIbaHelper.service.setIba(usageLink, WTConstantsIF.BASIS,
					associatedObject.getAttributeValueByName(WTConstantsIF.BASIS));
		}

		if (StringUtils.isNotEmpty(associatedObject.getAttributeValueByName(WTConstantsIF.SUPPLY_TYPE))) {
			usageLink = (WTPartUsageLink) GemaltoIbaHelper.service.setIba(usageLink, WTConstantsIF.SUPPLY_TYPE,
					associatedObject.getAttributeValueByName(WTConstantsIF.SUPPLY_TYPE));
		}

		if (StringUtils.isNotEmpty(associatedObject.getAttributeValueByName(WTConstantsIF.INCLUDE_ON_SHIP_DOCUMENTS))) {
			usageLink = (WTPartUsageLink) GemaltoIbaHelper.service.setIba(usageLink,
					WTConstantsIF.INCLUDE_ON_SHIP_DOCUMENTS,
					associatedObject.getAttributeValueByName(WTConstantsIF.INCLUDE_ON_SHIP_DOCUMENTS));
		}

		if (StringUtils.isNotEmpty(associatedObject.getAttributeValueByName(WTConstantsIF.OPTIONS))) {
			usageLink = (WTPartUsageLink) GemaltoIbaHelper.service.setIba(usageLink, WTConstantsIF.OPTIONS,
					associatedObject.getAttributeValueByName(WTConstantsIF.OPTIONS));
		}

		if (StringUtils.isNotEmpty(associatedObject.getAttributeValueByName(WTConstantsIF.REFERENCE_DESIGNATOR))) {
			usageLink = (WTPartUsageLink) GemaltoIbaHelper.service.setIba(usageLink, WTConstantsIF.REFERENCE_DESIGNATOR,
					associatedObject.getAttributeValueByName(WTConstantsIF.REFERENCE_DESIGNATOR));
		}
		
	
		if (StringUtils.isNotEmpty(associatedObject.getAttributeValueByName(WTConstantsIF.QUANTITY))) {
			usageLink.setQuantity(Quantity.newQuantity(Integer.parseInt(associatedObject.getAttributeValueByName(WTConstantsIF.QUANTITY)), QuantityUnit.toQuantityUnit(associatedObject.getAttributeValueByName(WTConstantsIF.UNIT))));
		}
		
		
		
		
		
		// Refresh and save as link
		PersistenceServerHelper.manager.update(usageLink);

		return usageLink;

	}

	/**
	 * Create Base on link 
	 * @param customerItem
	 * @param markettingProductCode
	 * @throws WTException
	 */

	public static void createBasedOnLink(WTPart customerItem,
			String markettingProductCode) throws WTException {

		try {

			// Getting the object
			@SuppressWarnings("rawtypes")
			Vector mpObjects = PDMQueryHelper.service.findRevisionsFromNumber(
					markettingProductCode, true, "wt.part.WTPart",
					SoftTypePropConstants.MARKETING_PRODUCT_TYPE_WT);

			// creating the link
			TypeDefinitionReference tdRef = TypedUtility
					.getTypeDefinitionReference(SoftTypePropConstants.BASED_ON_LINK_TYPE_WT);
			ConfigurableReferenceLink link = ConfigurableReferenceLink
					.newConfigurableReferenceLink(customerItem,
							((WTPart) mpObjects.get(0)).getMaster(), tdRef);
			PersistenceHelper.manager.save(link);

		} catch (ClassNotFoundException e) {
			LOGGER.log(Level.WARNING, "\"Exception at StandardPdmStructService.createBasedOnLink >> \" " + e.getMessage());
			throw new WTException(e);
		}
	}
	/**
	 * Create Configurable links / Sites
	 * 
	 * @return
	 * @throws WTException
	 */

	public static void createConfigurableLink(Iterated workableObject, WTPart persistable) {

		try {
			// Get the type definition
			TypeDefinitionReference tdRef = TypedUtility
					.getTypeDefinitionReference(SoftTypePropConstants.APPLICABILITY_LINK_TYPE_WT);

			ConfigurableReferenceLink configurableReferenceLink = ConfigurableReferenceLink
					.newConfigurableReferenceLink(workableObject,
							((WTPart) persistable).getMaster(), tdRef); 
			// save as link
			configurableReferenceLink = (ConfigurableReferenceLink) PersistenceHelper.manager
					.save(configurableReferenceLink);

			if (configurableReferenceLink != null)
				LOGGER.log(Level.INFO, "Configurable reference link was created successfully");
			
		} catch (WTException e) {
			LOGGER.log(Level.WARNING, "Error occured during configurable link object " + e.getMessage());

		}

	}
	
	/**
	 * Create Contents
	 * @return
	 * @throws WTException
	 */
	
	public static void createContents(WTPart workableObject,Iterated persistable) {

		try {
			// Create Link
			TypeDefinitionReference tdRef = TypedUtility.getTypeDefinitionReference(WTConstantsIF.CONTENT_LINK);
			ConfigurableDescribeLink contentLink;
			contentLink = ConfigurableDescribeLink.newConfigurableDescribeLink(workableObject,persistable, tdRef);
			// Save Link
			contentLink = (ConfigurableDescribeLink) PersistenceHelper.manager.save(contentLink);
		} catch (WTException e) {
			LOGGER.log(Level.WARNING, "Error occured during content link object creation " + e.getMessage());
		}
		
	}
	

	public static WTCollection buildWTCollection() throws WTException {
		return PersistenceHelper.manager.save(objectCollection);
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

}
