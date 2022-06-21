package org.metadatacenter.csvpipeline.ont;

import org.metadatacenter.csvpipeline.redcap.DataDictionaryChoice;
import org.metadatacenter.csvpipeline.redcap.DataDictionaryRow;
import org.semanticweb.owlapi.model.IRI;

import java.util.UUID;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-06-16
 */
public class UuidChoiceIriStrategy implements ChoiceIriStrategy {

    private final String iriPrefix;

    public UuidChoiceIriStrategy(String iriPrefix) {
        this.iriPrefix = iriPrefix;
    }

    @Override
    public IRI generateIriForChoice(DataDictionaryRow row, DataDictionaryChoice choice) {
        return IRI.create(iriPrefix + UUID.randomUUID());
    }
}
