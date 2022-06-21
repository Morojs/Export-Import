package ext.gemalto.pdm.generic.xml.models;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AssociatedObject")
public class XMLAssociatedObject {

	private String identity;
	private String type;
	private String displayType;
	private String context;

	private List<XMLAttribute> attributes;

	public XMLAssociatedObject() {
		super();
		attributes = new ArrayList<XMLAttribute>();
	}
	
	
	// get the name of the current object 
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

	public String getAttributeValueByName(String attrName) {
		String attrValue=null;
		for (int i = 0; i < attributes.size(); i++) {
			if (attributes.get(i).getName().equalsIgnoreCase(attrName)) {
				attrValue=attributes.get(i).getInternalValue();
				break;
			}	
		}	
		return attrValue;
	}
	
	@XmlElementWrapper(name = "ObjectAttributes")
	@XmlElement(name = "ObjectAttribute",required = true)
	public List<XMLAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<XMLAttribute> attributes) {
		this.attributes = attributes;
	}

	@XmlAttribute(name = "Identity")
	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	@XmlAttribute(name = "Type", required = false)
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@XmlAttribute(name = "DisplayedType", required = false)
	public String getDisplayType() {
		return displayType;
	}

	public void setDisplayType(String displayedType) {
		this.displayType = displayedType;
	}

	@XmlAttribute(name = "Context")
	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}


}
