package ext.gemalto.pdm.generic.xml.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "ParentObject")
@XmlType(propOrder = { "attributes", "XMLLinkModel", "subWTObjectList", "properties" })
public class XMLRoot {

	private String context;
	private String identity;
	private String type;
	private String displayType;

	private List<XMLProperty> properties;
	private List<XMLAttribute> attributes;
	private List<XMLLink> xmlLinkModel;
	private List<XMLRoot> subWTObjectList;

	public XMLRoot() {
		super();
		properties = new ArrayList<XMLProperty>();
		attributes = new ArrayList<XMLAttribute>();
		xmlLinkModel = new ArrayList<XMLLink>();
		subWTObjectList = new ArrayList<XMLRoot>();
	}

	/****** Helper Section *******/

	public XMLAssociatedObject getCurrentLinkCollectionByType(String softType, int cmptA, int cmptB) {

		if (this.xmlLinkModel.get(cmptA).getType().equalsIgnoreCase(softType)) {
			if (cmptB < this.xmlLinkModel.get(cmptA).getAssociatedObjectChildXmls().size()) {
				return this.xmlLinkModel.get(cmptA).getAssociatedObjectChildXmls().get(cmptB);
			}
		}

		return null;
	}
	
	public String getAppendedObjectLinkType(int cmptA) {
		return cmptA < this.xmlLinkModel.size() ? this.xmlLinkModel.get(cmptA).getType() : null;
	}
	
	// get the associated object name
	public String getObjectName() {
		String nameValue = "";
		for (int i = 0; i < this.getAttributes().size(); i++) {
			XMLAttribute attrElement = this.getAttributes().get(i);
			if (attrElement.getName().equalsIgnoreCase("Name")) {
				nameValue = attrElement.getInternalValue();
				break;
			}
		}
		return nameValue;
	}
	
	/****** Helper Section *******/

	@XmlAttribute(name = "Type", required = false)
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@XmlAttribute(name = "Context", required = false)
	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	@XmlAttribute(name = "DisplayedType", required = false)
	public String getDisplayType() {
		return displayType;
	}

	public void setDisplayType(String displayedType) {
		this.displayType = displayedType;
	}

	@XmlElement(name = "Property")
	public List<XMLProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<XMLProperty> properties) {
		this.properties = properties;
	}

	@XmlElementWrapper(name = "ObjectAttributes", required = true)
	@XmlElement(name = "ObjectAttribute")
	public List<XMLAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<XMLAttribute> attributes) {
		this.attributes = attributes;
	}

	@XmlElementWrapper(name = "Links", required = false)
	@XmlElement(name = "Link")
	public List<XMLLink> getXMLLinkModel() {
		return xmlLinkModel;
	}

	public void setXMLLinkModel(List<XMLLink> linkXmlModels) {
		
		// Pre-Process null values
		List<XMLLink> xmlLinkList=new ArrayList<>(linkXmlModels);
		xmlLinkList.removeAll(Collections.singleton(null));
		
		this.xmlLinkModel = xmlLinkList;
	}

	@XmlAttribute(name = "Identity")
	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	/****** CHILD OBJECT - RECURSIVE ******/

	@XmlElement(name = "ChildObject", required = false)
	public List<XMLRoot> getSubWTObjectList() {
		return subWTObjectList;
	}

	public void setSubWTObjectList(List<XMLRoot> listChildObject) {
		this.subWTObjectList = listChildObject;
	}

}
