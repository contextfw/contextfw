package net.contextfw.web.application.internal;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import net.contextfw.web.application.ModuleConfiguration;
import net.contextfw.web.application.WebApplicationException;
import net.contextfw.web.application.WebApplicationServletModule;
import net.contextfw.web.application.internal.util.PackageUtils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.HTMLWriter;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class WebResponder {

    private Logger logger = LoggerFactory.getLogger(WebResponder.class);
    
    public enum Mode {
        
        INIT("text/html;charset=UTF-8"), 
        UPDATE("text/xml;charset=UTF-8"),
        XML("text/xml;charset=UTF-8");
        
        private final String contentType;
        
        private Mode(String contentType) {
            this.contentType = contentType;
        }

        public String getContentType() {
            return contentType;
        }
    }
    
    private final ModuleConfiguration configuration;
    
    @Inject
    public WebResponder(ModuleConfiguration configuration) {
        this.configuration = configuration;
    }
    
    private final static TransformerFactory factory = TransformerFactory
            .newInstance();

    /*
     * Transformer is not thread safe
     */
    private static ThreadLocal<Transformer> _transformer = null;

    public void logXML(Document d) {
        try {
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer;

            StringWriter xml = new StringWriter();
            writer = new XMLWriter(xml, format);
            writer.write(d);
            
            logger.debug("Logged xml-response:\n{}", xml.toString());
            
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    private Transformer getTransformer() {
        if (_transformer == null || configuration.isDebugMode()) {
            _transformer = new ThreadLocal<Transformer>() {
                protected Transformer initialValue() {
                    try {
                        return factory.newTransformer(new StreamSource(getXSLDocument()));
                    } catch (TransformerConfigurationException e) {
                        throw new WebApplicationException("Could not get transformer", e);
                    }
                }
            };
        }
        return _transformer.get();
    }

    protected String getXSLDocumentContent() {
        List<File> resources = new ArrayList<File>();
        try {
            for (String pckg : configuration.getResourceRootPackages()) {
                resources.addAll(PackageUtils.getResources(pckg,
                        Thread.currentThread().getContextClassLoader()));
            }
        } catch (IOException e1) {
            throw new WebApplicationException(e1);
        }

        File root = null;

        Iterator<File> iter = resources.iterator();

        while (iter.hasNext()) {
            File next = iter.next();
            if (next.getName().endsWith("root.xsl")) {
                iter.remove();
                root = next;
                break;
            }
        }

        SAXReader reader = new SAXReader();
        try {
            Document document = reader.read(root);

            Element stylesheet = (Element) document
                    .selectSingleNode("//stylesheet");

            // Adding other stylesheets

            for (File file : resources) {
                if (file.getName().endsWith(".xsl")) {
                    reader = new SAXReader();
                    Document child = reader.read(file);

                    for (Object el : child.getRootElement().elements()) {
                        if (el instanceof Node) {
                            stylesheet.add(((Node) el).detach());
                        }
                    }
                }
            }
            StringWriter content = new StringWriter();
            OutputFormat format = OutputFormat.createCompactFormat();
            format.setXHTML(true);
            format.setTrimText(false);
            format.setPadText(true);
            format.setNewlines(false);
            XMLWriter writer = new XMLWriter( content, format );
            writer.write( document );
            return content.toString();
        } catch (DocumentException e) {
            throw new WebApplicationException(e);
        } catch (UnsupportedEncodingException e) {
            throw new WebApplicationException(e);
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
    }
    
    public Reader getXSLDocument() {
        return new StringReader(getXSLDocumentContent());
    }
    
    public void sendResponse(Document document, HttpServletResponse resp, Mode mode) throws ServletException, IOException {
        if (configuration.isLogXML()) {
            logXML(document);
        }
        if (mode != Mode.XML) {
            sendHTMLResponse(document, resp, mode);
        } else {
            sendXMLResponse(document, resp);
        }
    }
    
    private void sendXMLResponse(Document document, HttpServletResponse resp) throws IOException {
        resp.setContentType(Mode.XML.getContentType());
        resp.setHeader("Expires", "-1");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-cache, no-store");
        OutputFormat format = OutputFormat.createPrettyPrint();
        new XMLWriter(resp.getWriter(), format).write(document);
    }

    public void sendHTMLResponse(Document document, HttpServletResponse resp, Mode mode) throws ServletException, IOException {

        resp.setContentType(mode.getContentType());
        resp.setHeader("Expires", "-1");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-cache, no-store");
        
        DocumentSource source = new DocumentSource(document);
        DocumentResult result = new DocumentResult();

        try {
            Transformer tr = getTransformer();
            synchronized(tr) {
                String lang = document.getRootElement().attributeValue("xml:lang");
                if (lang != null) {
                    tr.setParameter("xml:lang", lang);
                }
                tr.transform(source, result);
            }
            Document rDocument = result.getDocument();

            OutputFormat format = OutputFormat.createCompactFormat();
            format.setXHTML(true);
            format.setTrimText(false);
            format.setPadText(true);
            format.setNewlines(false);
            format.setExpandEmptyElements(true);
            
            if (mode == Mode.INIT) {
                rDocument.addDocType("html", "-//W3C//DTD XHTML 1.0 Transitional//EN",
                    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd");
                new HTMLWriter(resp.getWriter(), format).write(rDocument);    
            } else {
                new XMLWriter(resp.getWriter(), format).write(rDocument);
            }
        }
        catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}
