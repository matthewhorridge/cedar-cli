package org.metadatacenter.csvpipeline;

import org.metadatacenter.csvpipeline.ont.*;
import org.metadatacenter.csvpipeline.redcap.DataDictionaryRow;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-06-16
 */
@Configuration
public class OntologyGeneratorConfiguration {

    @Value("${iri-prefix}")
    private String iriPrefix;

    @Value("${lang:en}")
    private String lang;

    @Value("${label-property:http://www.w3.org/2004/02/skos/core#prefLabel}")
    private String choiceLabelPropertyIri;

    @Value("${database-value-property:http://www.w3.org/2004/02/skos/core#notation}")
    private String databaseValuePropertyIri;

    @Value("${choice-iri-type:DB_VALUE}")
    private ChoiceIriType choiceIriType;

    @Value("${output-vocabulary:NONE}")
    private VocabularyType vocabularyType;

    @Value("${vocabulary-iri-type:VARIABLE_NAME}")
    private OntologyIriType ontologyIriType;

    @Bean
    OWLDataFactory dataFactory() {
        return new OWLDataFactoryImpl();
    }

    @Bean
    @Scope("prototype")
    KnowledgeArtifactGenerator generateOntology(OntologyIriStrategy ontologyIriStrategy,
                                                OntologyAnnotationStrategy ontologyAnnotationStrategy,
                                                ChoiceIriStrategy choiceIriStrategy,
                                                ChoiceAxiomsStrategy choiceAxiomsStrategy) {
        return new KnowledgeArtifactGenerator(ontologyIriStrategy,
                                              ontologyAnnotationStrategy,
                                              choiceIriStrategy,
                                              choiceAxiomsStrategy);
    }

    @Bean
    @Scope("prototype")
    OntologyIriStrategy ontologyIriStrategy() {
        if (ontologyIriType == OntologyIriType.VARIABLE_NAME) {
            return new VariableNameOntologyIriStrategy(iriPrefix);
        }
        else {
            return new UuidOntologyIriStrategy(iriPrefix);
        }
    }

    @Bean
    OntologyLabelStrategy ontologyLabelStrategy() {
        return new OntologyLabelStrategy() {
            @Override
            public String getOntologyLabel(DataDictionaryRow row) {
                return "http://purl.org/ontology/" + row.variableName();
            }
        };
    }

    @Bean
    OntologyAcronymStrategy ontologyAcronymStrategy() {
        return new OntologyAcronymStrategy() {
            @Override
            public String getOntologyAcronym(DataDictionaryRow row) {
                return row.variableName().toUpperCase();
            }
        };
    }

    @Bean
    OntologyAnnotationStrategy ontologyAnnotationStrategy(OWLDataFactory dataFactory) {
        return new BasicOntologyAnnotationStrategy(dataFactory);
    }

    @Bean
    @Scope("prototype")
    ChoiceIriStrategy choiceIriStrategy() {
        if(choiceIriType == ChoiceIriType.UUID) {
            return new UuidChoiceIriStrategy(iriPrefix);
        }
        else {
            return new VariableNameCodeChoiceIriStrategy(iriPrefix);
        }
    }

    @Bean
    @Scope("prototype")
    ChoiceAxiomsStrategy choiceAxiomsStrategy(OWLDataFactory dataFactory) {
        if(vocabularyType == VocabularyType.OWL) {
            return new OntologyChoiceAxiomsStrategy(lang, dataFactory, IRI.create(choiceLabelPropertyIri), IRI.create(databaseValuePropertyIri));
        }
        else if(vocabularyType == VocabularyType.SKOS) {
            return new SkosChoiceAxiomsStrategy(lang, dataFactory, IRI.create(choiceLabelPropertyIri), IRI.create(databaseValuePropertyIri));
        }
        else {
            return new NoOpAxiomStrategy();
        }
    }

    @Bean
    @Scope("prototype")
    KnowledgeArtifactGenerator knowledgeArtifactGenerator(OntologyIriStrategy ontologyIriStrategy,
                                                          OntologyAnnotationStrategy ontologyAnnotationStrategy,
                                                          ChoiceIriStrategy iriGenerationStrategy,
                                                          ChoiceAxiomsStrategy choiceAxiomsStrategy) {
        return new KnowledgeArtifactGenerator(ontologyIriStrategy,
                                              ontologyAnnotationStrategy,
                                              iriGenerationStrategy,
                                              choiceAxiomsStrategy);
    }
}
