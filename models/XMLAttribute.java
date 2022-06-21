package ext.gemalto.pdm.generic.xml.models;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.sun.xml.txw2.annotation.XmlCDATA;



@XmlRootElement(name = "ObjectAttribute")
public class XMLAttribute {

	// The fields to be presented in the ObjectAttribute value type ,
	// the Pascal Case should be respected

	private String Name;
	private String internalValue;

	@XmlAttribute(required = true)
	public String getName() {
		return Name;
	}

	public void setName(String name) {
		this.Name = name;
	}

	@XmlCDATA
	@XmlElement(name = "InternalValue", required = true)
	public String getInternalValue() {
		return internalValue;
	}
	@XmlCDATA
	public void setInternalValue(String internalValue) {
		this.internalValue = internalValue;
	}

}
