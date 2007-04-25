// file: JADEFIPAAlignmentServerOntologyOntology.java generated by ontology bean generator.  DO NOT EDIT, UNLESS YOU ARE REALLY SURE WHAT YOU ARE DOING!
package fr.inrialpes.exmo.align.service.jade.messageontology;

import jade.content.onto.*;
import jade.content.schema.*;
import jade.util.leap.HashMap;
import jade.content.lang.Codec;
import jade.core.CaseInsensitiveString;

/** file: JADEFIPAAlignmentServerOntologyOntology.java
 * @author ontology bean generator
 * @version 2007/03/19, 17:12:29
 */
public class JADEFIPAAlignmentServerOntology extends jade.content.onto.Ontology  {
  //NAME
  public static final String ONTOLOGY_NAME = "JADEFIPAAlignmentServerOntology";
  // The singleton instance of this ontology
  private static ReflectiveIntrospector introspect = new ReflectiveIntrospector();
  private static Ontology theInstance = new JADEFIPAAlignmentServerOntology();
  public static Ontology getInstance() {
     return theInstance;
  }


   // VOCABULARY
    public static final String METADATA="METADATA";
    public static final String FIND="FIND";
    public static final String TRANSLATE="TRANSLATE";
    public static final String ACTION_HASPARAMETER="hasParameter";
    public static final String ACTION="Action";
    public static final String CUT="CUT";
    public static final String LOAD="LOAD";
    public static final String RETRIEVE="RETRIEVE";
    public static final String ALIGN="ALIGN";
    public static final String STORE="STORE";
    public static final String ACLIENT="AClient";
    public static final String ASERVER="AServer";
    public static final String _NAME="name";
    public static final String _VALUE="value";
    public static final String parameter="parameter";

  /**
   * Constructor
  */
  private JADEFIPAAlignmentServerOntology(){ 
    super(ONTOLOGY_NAME, BasicOntology.getInstance());
    try { 

    // adding Concept(s)
    ConceptSchema Schema = new ConceptSchema(parameter);
    add(Schema, fr.inrialpes.exmo.align.service.jade.messageontology.Parameter.class);

    // adding AgentAction(s)

    // adding AID(s)
    ConceptSchema aServerSchema = new ConceptSchema(ASERVER);
    add(aServerSchema, fr.inrialpes.exmo.align.service.jade.messageontology.AServer.class);
    ConceptSchema aClientSchema = new ConceptSchema(ACLIENT);
    add(aClientSchema, fr.inrialpes.exmo.align.service.jade.messageontology.AClient.class);

    // adding Predicate(s)
    PredicateSchema storeSchema = new PredicateSchema(STORE);
    add(storeSchema, fr.inrialpes.exmo.align.service.jade.messageontology.STORE.class);
    PredicateSchema alignSchema = new PredicateSchema(ALIGN);
    add(alignSchema, fr.inrialpes.exmo.align.service.jade.messageontology.ALIGN.class);
    PredicateSchema retrieveSchema = new PredicateSchema(RETRIEVE);
    add(retrieveSchema, fr.inrialpes.exmo.align.service.jade.messageontology.RETRIEVE.class);
    PredicateSchema loadSchema = new PredicateSchema(LOAD);
    add(loadSchema, fr.inrialpes.exmo.align.service.jade.messageontology.LOAD.class);
    PredicateSchema cutSchema = new PredicateSchema(CUT);
    add(cutSchema, fr.inrialpes.exmo.align.service.jade.messageontology.CUT.class);
    PredicateSchema actionSchema = new PredicateSchema(ACTION);
    add(actionSchema, fr.inrialpes.exmo.align.service.jade.messageontology.Action.class);
    PredicateSchema translateSchema = new PredicateSchema(TRANSLATE);
    add(translateSchema, fr.inrialpes.exmo.align.service.jade.messageontology.TRANSLATE.class);
    PredicateSchema findSchema = new PredicateSchema(FIND);
    add(findSchema, fr.inrialpes.exmo.align.service.jade.messageontology.FIND.class);
    PredicateSchema metadataSchema = new PredicateSchema(METADATA);
    add(metadataSchema, fr.inrialpes.exmo.align.service.jade.messageontology.METADATA.class);


    // adding fields
    Schema.add(_VALUE, (TermSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
    Schema.add(_NAME, (TermSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
    actionSchema.add(ACTION_HASPARAMETER, Schema, 0, ObjectSchema.UNLIMITED);

    // adding name mappings

    // adding inheritance
    
    storeSchema.addSuperSchema(actionSchema);
    cutSchema.addSuperSchema(actionSchema);
    findSchema.addSuperSchema(actionSchema);
    loadSchema.addSuperSchema(actionSchema);
    alignSchema.addSuperSchema(actionSchema);
    retrieveSchema.addSuperSchema(actionSchema);
    metadataSchema.addSuperSchema(actionSchema);
    translateSchema.addSuperSchema(actionSchema);
    


   }catch (java.lang.Exception e) {e.printStackTrace();}
  }
  }