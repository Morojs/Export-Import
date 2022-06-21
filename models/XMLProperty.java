package ext.gemalto.pdm.generic.xml.models;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Property")
public class XMLProperty {

	private String name;
	private String defaultProperty;
	
	@XmlAttribute(name = "name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@XmlAttribute(name = "default")
	public String getDefaultProperty() {
		return defaultProperty;
	}
	
	public void setDefaultProperty(String defaultProperty) {
		this.defaultProperty = defaultProperty;
	}
	

	
}
