package ext.gemalto.pdm.generic.xml.models;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;



@XmlRootElement(name = "Link")
public class XMLLink {
	
	
	private String identity;
	private String type;
	private List<XMLAssociatedObject> associatedObjectChildXmls;

	public XMLLink() {
		super();
		this.associatedObjectChildXmls = new ArrayList<XMLAssociatedObject>();
	}

	
	@XmlElement(name = "AssociatedObject")
	public List<XMLAssociatedObject> getAssociatedObjectChildXmls() {
		return associatedObjectChildXmls;
	}

	public void setAssociatedObjectChildXmls(List<XMLAssociatedObject> associatedObjectChildXmls) {
		this.associatedObjectChildXmls = associatedObjectChildXmls;
	}

	
	@XmlAttribute(name = "Identity")
	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}


	@XmlAttribute(name = "Type")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	
}
