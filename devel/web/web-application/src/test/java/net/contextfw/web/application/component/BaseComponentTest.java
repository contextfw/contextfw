package net.contextfw.web.application.component;

import java.io.StringWriter;

import net.contextfw.web.application.WebApplicationException;
import net.contextfw.web.application.configuration.Configuration;
import net.contextfw.web.application.internal.ToStringSerializer;
import net.contextfw.web.application.internal.component.ComponentBuilder;
import net.contextfw.web.application.internal.component.ComponentBuilderImpl;
import net.contextfw.web.application.internal.component.ComponentRegister;
import net.contextfw.web.application.internal.component.WebApplicationComponent;
import net.contextfw.web.application.serialize.AttributeSerializer;

import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public abstract class BaseComponentTest {
    
    protected Logger log = LoggerFactory.getLogger(BaseComponentTest.class); 
    
    protected ComponentRegister componentRegister;
    
    protected ComponentBuilder componentBuilder;
    
    protected ScriptContext scriptContext;
    
    protected WebApplicationComponent webApplicationComponent;
    
    protected DOMBuilder domBuilder;
    
    protected AttributeSerializer<Object> serializer = new ToStringSerializer();
    
    protected DomAssert assertDom(String xpath) {
        Node node = domBuilder.toDocument().getRootElement().selectSingleNode(xpath);
        return new DomAssert(xpath, node);
    }
    
    @Before
    public void before() {
        componentRegister = new ComponentRegister();
        Configuration configuration = Configuration.getDefaults();
        Gson gson = new Gson();
        componentBuilder = new ComponentBuilderImpl(null, gson, configuration);
        scriptContext = (ScriptContext) componentBuilder;
        domBuilder = new DOMBuilder("WebApplication", serializer, componentBuilder);
        webApplicationComponent = new WebApplicationComponent(componentRegister);
    }
    
    public void logXML(DOMBuilder b) {
        try {
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer;

            StringWriter xml = new StringWriter();
            writer = new XMLWriter(xml, format);
            writer.write(b.toDocument());
            log.info("Logged xml:\n"+xml.toString());
            
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }
}
