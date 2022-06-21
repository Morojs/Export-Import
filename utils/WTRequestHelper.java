package ext.gemalto.pdm.generic.xml.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import wt.doc.WTDocument;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.log4j.LogR;
import wt.part.WTPart;
import wt.part.WTPartHelper;
import wt.part.WTPartMaster;
import wt.part.WTPartUsageLink;
import wt.pds.StatementSpec;
import wt.pom.PersistenceException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.util.WTException;
import wt.vc.VersionControlHelper;

public class WTRequestHelper {

	private static final String CLASSNAME = WTRequestHelper.class.getName();
	private static final Logger LOGGER = LogR.getLogger(CLASSNAME);
	private static Map<WTObject, Map<WTObject, WTObject>> multipleXmlValuesMap = new HashMap<WTObject, Map<WTObject, WTObject>>();

	/**
	 * 
	 * 
	 * Navigates the WTPartUsageLink along the uses role, returning a Map list of
	 * WTPartUsageLinks for root object and sub objects.
	 * 
	 * @param wtObject
	 * @param queryResult
	 * @throws PersistenceException
	 * @throws WTException
	 * @throws IOException
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void getAllUsesWTPartMasters(WTObject wtObject, QueryResult queryResult)
			throws PersistenceException, WTException, IOException {
		Map<WTObject, WTObject> inner = null;
		// PREPARE THE INNER MAP FOR EACH WTPART CHILD
		inner = new HashMap<WTObject, WTObject>();
		WTObject wtParent = wtObject;
		// CONVERT QUERY RESULT TO AN ARRAYLIST COLLECTION
		List alreadyLinkedObjectsInPS = new ArrayList(queryResult.getObjectVectorIfc().getVector());
		for (int i = 0; i < alreadyLinkedObjectsInPS.size(); i++) {
			if (alreadyLinkedObjectsInPS.get(i) instanceof WTPartUsageLink) {
				// WTPART USAGES LINK
				WTPartUsageLink link = (WTPartUsageLink) alreadyLinkedObjectsInPS.get(i);
				// GET THE WTPART MASTER OBJECT LINKED TO THE WTPART USAGESLINK OBJECT ROLE
				WTPartMaster childPartMaster = (WTPartMaster) link.getRoleBObjectRef().getObject();
				// FIND ALL ITTERATION OF SELECTED WTPARTMASTER
				QueryResult childPartResult = VersionControlHelper.service.allIterationsOf(childPartMaster);
				// CHECK IF THERE ANY CHILD OBJECT FROM THE ITTERATION
				if (childPartResult.size() != 0) {
					WTPart childPart = (WTPart) childPartResult.nextElement();
					// mapHelper.put(childPart, new WTObject[] { childPart, wtParent });
					inner.put(wtParent, childPart);
					// GET WTPARTMASTER SUB-CHILDREN
					QueryResult qr = WTPartHelper.service.getUsesWTPartMasters((WTPart) childPart);
					getAllUsesWTPartMasters(childPart, qr);
				}
			}
		}
		multipleXmlValuesMap.put(wtParent, inner);
	}

	/**
	 * Get the object by number
	 * 
	 * @param number
	 * @return
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	public static WTPart getWTPartByNumber(String number) throws WTException {

		QueryResult qr = new QueryResult();
		WTPart part = null;
		QuerySpec qs;

		try {
			qs = new QuerySpec(WTPart.class);
			SearchCondition sc = new SearchCondition(WTPart.class, WTPart.NUMBER, SearchCondition.EQUAL, number, false);
			qs.appendSearchCondition(sc);
			qr = PersistenceHelper.manager.find((StatementSpec) qs);
			// take the next one
			while (qr.hasMoreElements()) {
				part = (WTPart) qr.nextElement();
			}
			

		} catch (WTException e) {
			LOGGER.error("An Exception occured during search for document " + number + " >", e);
		}

		return part;
	}

	/**
	 * Searching for document by number
	 * 
	 * @param number
	 * @return
	 * @throws WTException
	 */
	@SuppressWarnings("deprecation")
	public static WTDocument getDocumentByNumber(String number) throws WTException {
		QueryResult qr = new QueryResult();
		WTDocument doc = null;

		QuerySpec qs;
		try {
			qs = new QuerySpec(WTDocument.class);
			SearchCondition sc = new SearchCondition(WTDocument.class, WTDocument.NUMBER, SearchCondition.EQUAL, number,
					false);
			qs.appendSearchCondition(sc);
			qr = PersistenceHelper.manager.find((StatementSpec) qs);
			while (qr.hasMoreElements()) {
				doc = (WTDocument) qr.nextElement();
			}
		} catch (WTException e) {
			// TODO Auto-generated catch block
			LOGGER.error("An Exception occured during search for document " + number + " >", e);
			throw new WTException(e);
		}
		return doc;
	}

	public static Map<WTObject, Map<WTObject, WTObject>> getMultipleXmlValuesMap() {
		return multipleXmlValuesMap;
	}

}
