package org.metadatacenter.cedar.redcap;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-06-15
 */
public interface DataDictionaryChoicesHandler {

    default void handleBeginChoices() {};

    void handleChoice(DataDictionaryChoice choice);

    default void handleEndChoices() {};
}
