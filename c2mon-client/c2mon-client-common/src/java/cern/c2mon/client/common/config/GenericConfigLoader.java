package cern.c2mon.client.common.config;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class creates a set of configuration properties from an
 * XML configuration file 
 */
public class GenericConfigLoader {
  private Map<String, ConfigurationProperty> propertyTable = new Hashtable<String, ConfigurationProperty>();
  protected Element xmlRoot = null;
  
  /** Log4j log instance */
  private static final Logger LOG = Logger.getLogger(GenericConfigLoader.class);

  /**
   * Default Constructor
   */
  public GenericConfigLoader() {
    // Do nothing;
  }

  /* This constructor creates the properties defined
   * in the <code>InputStream</code> XML file
   */
  public GenericConfigLoader(InputStream in) throws org.xml.sax.SAXParseException
  {
    try {
      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = builderFactory.newDocumentBuilder();
      Document document = builder.parse(in);
      document.getDocumentElement().normalize();
      xmlRoot = document.getDocumentElement();
      propertyTable.putAll(loadBasicProperties(xmlRoot));
    } catch (org.xml.sax.SAXParseException saxpe) {
      throw saxpe;
    } catch(Exception e) {
      String errorMessage =
        "Error making root node: " + e;
      LOG.error(errorMessage, e);
    }    
  }
  
  /**
   * Creates a table containing the <code>ConfigurationProperty</code> 
   * corresponding to the properties written on the first level of
   * the document starting from the root element
   * @param root the XML root <code>Element</code> from which the properties are taken
   * @return the <code>Hashtable</code> containing the properties
   */  
  protected static Map<String, ConfigurationProperty> loadBasicProperties(final Element root)
  {
    Map<String, ConfigurationProperty> propTable = new Hashtable<String, ConfigurationProperty>();
    NamedNodeMap elementAttributes = null;
    String pName = null;
    String pType = null;
    String pValue = null;
    
    NodeList childElements = root.getChildNodes();
    for (int i = 0; i < childElements.getLength(); i++) {
      Node childElement = childElements.item(i);
      if ((childElement instanceof Element) 
          && ((childElement.getNodeName()).equals("property"))) {
        elementAttributes = childElement.getAttributes();
        pName = elementAttributes.getNamedItem("name").getNodeValue();
        pType = elementAttributes.getNamedItem("type").getNodeValue();
        pValue = elementAttributes.getNamedItem("value").getNodeValue();
        Object value = null;
        try {
          value = Class.forName(pType).getConstructor(new Class[]{String.class}).newInstance(new Object[]{pValue});
        }
        catch (Exception e) {
          LOG.error("Error loading property " + pName, e);
        }
        propTable.put(pName, new ConfigurationProperty(pName, value)); 
      }
    } 
    return propTable;
  }

  /**
   * Retrieves a property from its name
   * @param the name of the property
   * @return the property
   */    
  public Object getPropertyByName(final String name) {
    ConfigurationProperty property = (ConfigurationProperty) propertyTable.get(name);
    if (property != null)
      return property.getValue();
    else
      return null;
  }  
  
  /**
   * Add a property to the configuration
   * @param newProperty the <code>ConfigurationProperty</code>
   */  
  public void addBasicProperty(final ConfigurationProperty newProperty) {
    String newPropName = newProperty.getName();
    if (!propertyTable.containsKey(newPropName)) {
      propertyTable.put(newPropName, newProperty); 
    }
  }
  
  /**
   * Adds all properties defined by the Map. This can also overwrite
   * already defined properties.
   * @param properties Contains properties that shall be add.
   */
  public synchronized void addBasicProperties(final Map<String, ConfigurationProperty> properties) {
    propertyTable.putAll(properties);
  }
}