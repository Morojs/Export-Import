package ext.gemalto.pdm.generic.xml.models;

import java.util.Arrays;
import java.util.List;

import ext.gemalto.pdm.generic.iba.GemaltoIbaConstants;
import ext.gemalto.pdm.generic.softtype.SoftTypePropConstants;
import ext.gemalto.pdm.ws.utils.WSConstantsIF;
import wt.part.WTPart;

public interface WTConstantsIF extends WSConstantsIF {

	public final String PREFIX = GemaltoIbaConstants.ATTRIBUTE_DEFINITION_PREFIX;
	public final static String CONFIG_REFERENCE_LINK = "wt.configurablelink.ConfigurableReferenceLink";
	public final static String BASELINE = "wt.vc.baseline.BaselineMember";
	public final static String CONTENT_LINK = SoftTypePropConstants.CONTENT_LINK_IDENITIFIER_WT;
	public final static WTPart rootObject = null;

	/**
	 * Constants used during export/import object structure implementation.
	 *
	 **/

	public static final String OBJECT_TYPE = "type";
	public static final String DISPLAY_NAME = "displayType";
	public static final String IDENTITY = "identity";
	public static final String CONTEXT = "context";

	/** Start of Usage Link attributes */

	// Standard attributes
	public static final String QUANTITY = "quantity.amount";
	public static final String UNIT = "quantity.unit";
	public static final String TRACE_CODE = "tracecode";
	public static final String LINE_NUMBER = "linenumber";
	public static final String FIND_NUMBER = "findnumber";
	public static final String NUMBER = "number";
	public static final String NAME = "name";

	// IBAs
	public static final String POSITION = PREFIX + "Position";
	public static final String LAYER = PREFIX + "Layer";
	public static final String BASIS = PREFIX + "Basis";
	public static final String SUPPLY_TYPE = PREFIX + "SupplyType";
	public static final String INCLUDE_ON_SHIP_DOCUMENTS = PREFIX + "IncludeShipDocs";
	public static final String OPTIONS = "Options";
	public static final String REFERENCE_DESIGNATOR = PREFIX + "GtoReferenceDesignator";

	// Collect Standards and IBAs in one side
	public static final String[] usageLinkAttr = { NAME, NUMBER, QUANTITY, UNIT, TRACE_CODE, LINE_NUMBER, FIND_NUMBER,
			POSITION, LAYER, BASIS, SUPPLY_TYPE, INCLUDE_ON_SHIP_DOCUMENTS, OPTIONS, REFERENCE_DESIGNATOR };

	// UsageLink type for importing task
	public static final List<String> usagesLinkType = Arrays.asList(
			(String) WSConstantsIF.DESCRIBE_LINK,
			(String) WSConstantsIF.REFERENCE_LINK, 
			(String) WSConstantsIF.IMPLEMENTED_BY_LINK,
			(String) WTConstantsIF.CONFIG_REFERENCE_LINK,
			(String) WTConstantsIF.BASED_ON_LINK,
			(String) WTConstantsIF.SHIP_WITH_LINK
			);

}
