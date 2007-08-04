/*
 * $Id$
 *
 * Copyright (C) INRIA Rh�ne-Alpes, 2007.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package fr.inrialpes.exmo.align.service;

import fr.inrialpes.exmo.align.impl.BasicParameters;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Parameters;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarFile;

import java.lang.NullPointerException;

// For message parsing
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * WSAServProfile: a SOAP over HTTP provile for the Alignment server
 * It uses the HTTP server of HTTPAServProfile
 * 
 * Improvements to come:
 * - provide WSDL from that channel as well
 * - implement request_id management (fully missing here)
 * - use XML/Xpath parsers [Make it namespace aware please]
 * - clean up
 */

public class WSAServProfile implements AlignmentServiceProfile {

    private int tcpPort;
    private String tcpHost;
    private int debug = 0;
    private AServProtocolManager manager;
    private static String wsdlSpec = "";

    private String myId;
    private String serverId;
    private int localId = 0;

    private static DocumentBuilder BUILDER = null;


    // ==================================================
    // Socket & server code
    // ==================================================

    public void init( Parameters params, AServProtocolManager manager ) throws AServException {
	this.manager = manager;
	// This may register the WSDL file to some directory
	myId = "SOAPoverHTTPInterface";
	serverId = "dummy";
	localId = 0;	

	// New XML parsing stuff
	final DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
	fac.setValidating(false);
	fac.setNamespaceAware(false); // Change this!
	try { BUILDER = fac.newDocumentBuilder(); }
	catch (ParserConfigurationException e) {
	    throw new AServException( "Cannot initialize SOAP message parsing", e );
	}
	// Read the WSDL specification
	try {
	    // JE: This is to be modified so that we look for this file in the
	    // classpath
	    JarFile archive = new JarFile ( "/Java/alignapi/lib/alignsvc.jar" );
	    InputStream is = archive.getInputStream( archive.getJarEntry("fr/inrialpes/exmo/align/service/aserv.wsdl") );
	    BufferedReader in = new BufferedReader(new InputStreamReader(is));
	    String line;
	    while ((line = in.readLine()) != null) {
		wsdlSpec += line + "\n";
	    }
	    if (in != null) in.close();
	} catch (IOException ioex) {
	    ioex.printStackTrace();
	}
    }

    public void close(){
	// This may unregister the WSDL file to some directory
    }
    
    // ==================================================
    // API parts
    // ==================================================

    /**
     * HTTP protocol implementation
     * each call of the protocol is a direct URL
     * and the answer is through the resulting page (RDF? SOAP? HTTP?)
     * Not implemented yet
     * but reserved if appears useful
     */
    public String protocolAnswer( String uri, String perf, Properties header, Parameters param ) {
	String method = header.getProperty("SOAPAction");
	String message = ((String)param.getParameter("content")).trim();

	// Create the DOM tree for the message
	Document domMessage = null;
	try {
	    domMessage = BUILDER.parse( new ByteArrayInputStream( message.getBytes()) );
	    // DECIDE WHAT TO DO WITH THESE ERRORS
	    // CERTAINLY RETURN A "CANNOT PARSE REQUEST"
	} catch  ( IOException ioex ) {
	    ioex.printStackTrace();
	} catch  ( SAXException saxex ) {
	    saxex.printStackTrace();
	}

	String msg = "<SOAP-ENV:Envelope   xmlns:SOAP-ENV='http://schemas.xmlsoap.org/soap/envelope/'   xmlns:xsi='http://www.w3.org/1999/XMLSchema-instance'   xmlns:xsd='http://www.w3.org/1999/XMLSchema'>  <SOAP-ENV:Body>";
	if ( perf.equals("WSDL") || method.equals("wsdl") ) {
	    msg += wsdlAnswer();
	} else if ( method.equals("listalignments") ) {
	    for( Enumeration e = manager.alignments(); e.hasMoreElements(); ){
		String id = ((Alignment)e.nextElement()).getExtension("id");
		msg += "<uri>"+id+"</uri>";
	    }
	    // -> List of URI
	} else if ( method.equals("listmethods") ) { // -> List of String
	    for( Iterator it = manager.listmethods().iterator(); it.hasNext(); ) {
		msg += "<method>"+it.next()+"</method>";
	    }
	} else if ( method.equals("listrenderers") ) { // -> List of String
	    for( Iterator it = manager.listrenderers().iterator(); it.hasNext(); ) {
		msg += "<renderer>"+it.next()+"</renderer>";
	    }
	} else if ( method.equals("listservices") ) { // -> List of String
	    for( Iterator it = manager.listservices().iterator(); it.hasNext(); ) {
		msg += "<service>"+it.next()+"</service>";
	    }
	} else if ( method.equals("store") ) { // URI -> URI
	    Parameters params = new BasicParameters();
	    Message answer = null;

	    getParameter( domMessage, message, params, "id", "id" );
	    if ( params.getParameter( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);
	    }

	    if ( answer == null )
		answer = manager.store( new Message(newId(),(Message)null,myId,serverId,(String)params.getParameter( "id" ), params) );
	    if ( answer instanceof ErrorMsg ) {
		msg += displayError( answer );
	    } else {
		msg += displayAnswer( answer );
	    }
	} else if ( method.equals("invert") ) { // URI -> URI
	    Parameters params = new BasicParameters();
	    Message answer = null;

	    getParameter( domMessage, message, params, "id", "id" );
	    if ( params.getParameter( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);
	    }

	    if ( answer == null )
		answer = manager.inverse( new Message(newId(),(Message)null,myId,serverId, (String)params.getParameter( "id" ), params) );
	    if ( answer instanceof ErrorMsg ) {
		msg += displayError( answer );
	    } else {
		msg += displayAnswer( answer );
	    }
	} else if ( method.equals("cut") ) { // URI * string * float -> URI
	    Parameters params = new BasicParameters();
	    Message answer = null;

	    getParameter( domMessage, message, params, "id", "id" );
	    if ( params.getParameter( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);
	    }

	    getParameter( domMessage, message, params, "method", "method" );
	    if ( params.getParameter( "method" ) == null ) {
		params.setParameter( "method", "hard" );
	    }

	    getParameter( domMessage, message, params, "threshold", "threshold" );
	    if ( params.getParameter( "threshold" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);
	    }

	    if ( answer == null )
		answer = manager.cut( new Message(newId(),(Message)null,myId,serverId,(String)params.getParameter( "id" ), params) );
	    if ( answer instanceof ErrorMsg ) {
		msg += displayError( answer );
	    } else {
		msg += displayAnswer( answer );
	    }
	} else if ( method.equals("align") ) { // URL * URL * URI * String * boolean * (params) -> URI
	    Parameters params = new BasicParameters();
	    Message answer = null;

	    getParameter( domMessage, message, params, "url1", "onto1" );
	    if ( params.getParameter( "onto1" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);
	    }

	    getParameter( domMessage, message, params, "url2", "onto2" );
	    if ( params.getParameter( "onto2" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);
	    }

	    getParameter( domMessage, message, params, "method", "method" );
	    getParameter( domMessage, message, params, "force", "force" );

	    if ( answer == null )
		answer = manager.align( new Message(newId(),(Message)null,myId,serverId,"", params) );
	    if ( answer instanceof ErrorMsg ) {
		msg += displayError( answer );
	    } else {
		msg += displayAnswer( answer );
	    }
	} else if ( method.equals("find") ) { // URI * URI -> List of URI
	    Parameters params = new BasicParameters();
	    Message answer = null;

	    getParameter( domMessage, message, params, "uri1", "onto1" );
	    if ( params.getParameter( "onto1" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);
	    }

	    getParameter( domMessage, message, params, "uri2", "onto2" );
	    if ( params.getParameter( "onto2" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);
	    }

	    if ( answer == null )
		answer = manager.existingAlignments( new Message(newId(),(Message)null,myId,serverId,"", params) );
	    if ( answer instanceof ErrorMsg ) {
		msg += displayError( answer );
	    } else {
		msg += displayAnswer( answer );
	    }
	} else if ( method.equals("retrieve") ) { // URI * method -> XML
	    Parameters params = new BasicParameters();
	    Message answer = null;

	    getParameter( domMessage, message, params, "id", "id" );
	    if ( params.getParameter( "id" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);
	    }

	    getParameter( domMessage, message, params, "method", "method" );
	    if ( params.getParameter( "method" ) == null )
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);

	    if ( answer == null )
		answer = manager.render( new Message(newId(),(Message)null,myId,serverId, "", params) );
	    if ( answer instanceof ErrorMsg ) {
		msg += displayError( answer );
	    } else {
		// JE: Depending on the type we should change the MIME type
		// This should be returned in answer.getParameters()
		// JE: This should also suppress the <?xml... statement
		msg += "<result>" + answer.getContent() + "</result>";
	    }
	} else if ( method.equals("metadata") ) { // URI -> XML
	    // Not done yet
	} else if ( method.equals("load") ) { // URL -> URI
	    Parameters params = new BasicParameters();
	    Message answer = null;

	    getParameter( domMessage, message, params, "url", "url" );
	    if ( params.getParameter( "url" ) == null ) {
		getParameter( domMessage, message, params, "content", "content" );
		if ( params.getParameter( "content" ) == null ) {
		    answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);
		} else {
		    // Save the content as a temporary file (gensym)
		    // Set the URI as the file:// uri for that file
		    // Set it in the "url" parameter
		    // Call load as below
		    // Take care somehow to discard the temporary file
		}
	    }
	    if ( answer == null )
		answer = manager.load( new Message(newId(),(Message)null,myId,serverId,"", params) );
	    if ( answer instanceof ErrorMsg ) {
		msg += displayError( answer );
	    } else {
		msg += displayAnswer( answer );
	    }
	} else if ( method.equals("loadfile") ) { // XML -> URI
	    Parameters params = new BasicParameters();
	    Message answer = null;

	    getParameter( domMessage, message, params, "url", "url" );
	    if ( params.getParameter( "url" ) == null ) {
		answer = new NonConformParameters(0,(Message)null,myId,"",message,(Parameters)null);
	    }
	    if ( answer == null )
		answer = manager.load( new Message(newId(),(Message)null,myId,serverId,"", params) );
	    if ( answer instanceof ErrorMsg ) {
		msg += displayError( answer );
	    } else {
		msg += displayAnswer( answer );
	    }
	} else if ( method.equals("translate") ) { // XML * URI -> XML
	    // Not done yet
	} else {
	    msg += "<UnRecognizedAction />";
	}
	msg += "  </SOAP-ENV:Body></SOAP-ENV:Envelope>";
	return msg;
    }

    public static String wsdlAnswer() { return wsdlSpec; }

    private void getParameter( Document dom, String message, Parameters p, String tag, String key ){
	XPath XPATH = XPathFactory.newInstance().newXPath();
	String result = null;
	try {
	    // The two first elements are prefixed by: "SOAP-ENV:"
	    result = ((Node)(XPATH.evaluate("/Envelope/Body/" + tag, dom, XPathConstants.NODE))).getTextContent().trim();
	    // Whatever error is NOTHING FOUND
	} catch (XPathExpressionException e) {
	} catch (NullPointerException e) {
	}
	if ( result != null && !result.equals("") ){
	    p.setParameter( key, result);
	}
    }

    private String displayAnswer ( Message answer ) {
	return answer.SOAPString();
    }

    private String displayError( Message answer ) {
	return answer.SOAPString();
    }

    private int newId() { return localId++; }
}
