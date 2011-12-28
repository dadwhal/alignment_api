/*
 * $Id$
 *
 * Copyright (C) 2011, INRIA
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

/* 
 * Generates a single test by taking as input either an ontology
 * or an ontology and an alignment and generating a modified ontology
 * and an alignment between this one and either the initial ontology
 * or the first ontology of the alignments.
 *
 * Alterations are specified in a list of parameters.
 */

package fr.inrialpes.exmo.align.gen;

// Alignment API implementation classes
import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentVisitor;

//Java classes
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Properties;

//Jena API classes
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.util.FileManager;

import fr.inrialpes.exmo.align.gen.alt.EmptyModification;

import java.util.Properties;

public class TestGenerator {
    private String urlprefix = "http://example.com/"; // Prefix (before testnumber) of test URL
    private String dirprefix = "";                    // Prefix (idem) of directory
    private String ontoname = "onto.rdf";             // name of ontology to generate
    private String alignname = "refalign.rdf";        // name of alignment to generate
    private String initOntoURI = null;                // URI of initial ontology
    private OntModel modifiedOntology;                                             //modified ontology
    private Alignment resultAlignment;                                                //the reference alignment
    private Alterator modifier = null;                                   //the modifier
    private boolean debug = false;

    public TestGenerator() {}

    // ******************************************************* SETTERS
    public void setURLPrefix( String u ) { urlprefix = u; }

    public void setDirPrefix( String d ) { dirprefix = d; }

    public void setOntoFilename( String o ) { ontoname = o; }

    public void setAlignFilename( String a ) { alignname = a; }

    public void setDebug( boolean d ) { debug = d; }

    //returns the modified ontology
    public OntModel getModifiedOntology() { return modifiedOntology; }

    //returns the modified ontology
    public void setInitOntoURI( String uri ) { initOntoURI = uri; }

    // ******************************************************* GB STUFF

    //gets the URI
    public String getURI( String testNumber ) {
        return urlprefix + "/" + testNumber + "/" + ontoname + "#"; // Do not like this #...
        //return urlprefix + getPrefix(ontoname) + "/" + testNumber + "/" + ontoname + "#";
    }

    public static String directoryName( String dir, String suffix ) {
	if ( suffix == null ) return dir;
	else return dir+"-"+suffix;
    }

    // ******************************************************* FACILITIES

    public OntModel loadOntology ( String file ) {
        InputStream in = FileManager.get().open( file );
        OntModel model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM );
        model.read( in, null );
	return model;
    }

    public static void writeOntology( OntModel model, String destFile, String ns ) {
	//System.err.println( " ==> Writing: "+ns );
        try {
            File f = new File( destFile );
            FileOutputStream fout = new FileOutputStream( f );
            Charset defaultCharset = Charset.forName("UTF8");
            RDFWriter writer = model.getWriter("RDF/XML-ABBREV");
            writer.setProperty( "showXmlDeclaration","true" );
	    // Instructions below are Jena specific
	    // They ensure that the default prefix (that of the ontology) is ns
	    // This also warrants that the owl:Ontology element is generated
	    // Without these, the generator does not work.
            model.setNsPrefix( "", ns );
            writer.setProperty( "xmlbase", ns );
            model.createOntology( ns );
            writer.write( model.getBaseModel(), new OutputStreamWriter(fout, defaultCharset), "");
            fout.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void outputTestDirectory( OntModel onto, Alignment align, String testnumber ) {
	// build the directory to save the file
	boolean create = new File( dirprefix+"/"+testnumber ).mkdir();
	// write the ontology into the directory
	writeOntology( onto, dirprefix+"/"+testnumber+"/"+ontoname, getURI( testnumber ));
	try {
	    //write the alignment into the directory
	    OutputStream stream = new FileOutputStream( dirprefix+"/"+testnumber+"/"+alignname );
	    // Outputing
	    PrintWriter  writer = new PrintWriter (
						   new BufferedWriter(
								      new OutputStreamWriter( stream, "UTF-8" )), true);
	    AlignmentVisitor renderer = new RDFRendererVisitor( writer );
	    align.render( renderer );
	    writer.flush();
	    writer.close();
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }

    /**
     * Generate a test by altering an existing test
     */
    public Properties incrementModifyOntology( String pKey, String pVal, String suffix, String prevTest, Properties al, String testNb ) {
	Properties p = new Properties();
	p.setProperty( pKey, pVal );
	String prevDirName = directoryName( prevTest, suffix );
	String crtDirName = directoryName( testNb, suffix );
	return modifyOntology( dirprefix+"/"+prevDirName+"/"+ontoname, al, crtDirName, p );
    }

    /**
     * Generate a test by altering an existing test
     */
    public Properties incrementModifyOntology( String prevTestDir, Properties al, String testDir, Properties params ) {
	return modifyOntology( dirprefix+"/"+prevTestDir+"/"+ontoname, al, testDir, params );
    }

    //modifies an ontology
    /**
     * Generate a test from an ontology
     */
    public Properties modifyOntology( String file, Properties al, String testNumber, Properties params) {
	if ( debug ) System.err.println( "Source: "+file+" Target "+testNumber );
	//set the TestGenerator ontology
	OntModel onto = loadOntology( file );
	Alterator modifier = generate( onto, params, al );
	// Prepare to generate
	modifier.relocateTest( getURI( testNumber ) );
	Alignment align = modifier.getAlignment(); // process, not just get
	modifiedOntology = modifier.getModifiedOntology();
	//saves the alignment into the file "refalign.rdf", null->System.out
                        //at the end, compute the reference alignment
	outputTestDirectory( modifiedOntology, align, testNumber );
	return modifier.getProtoAlignment();
    }


    // ******************************************************* GENERATOR
    //generate the alingnment
    public Alterator generate( OntModel onto, Properties params, Properties initalign ) {
        if ( debug ) {
	    System.err.println( "[-------------------------------------------------]" );
	    System.err.println( urlprefix+" / "+dirprefix+" / "+ontoname+" / "+alignname );
	}
	// Load the ontology
	Alterator modifier = new EmptyModification( onto );
	((EmptyModification)modifier).setDebug( debug );
	// Initialize the reference alignment
        if ( initalign != null ) ((EmptyModification)modifier).initializeAlignment( initalign );
	modifier.modify( params );
	// Apply all modifications
	modifier = AlteratorFactory.cascadeAlterators( modifier, params );
	return modifier;
    }

}

