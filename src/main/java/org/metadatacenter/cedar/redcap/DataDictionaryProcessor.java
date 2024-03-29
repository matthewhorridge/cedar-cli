package org.metadatacenter.cedar.redcap;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-06-16
 */
public class DataDictionaryProcessor {

    private final Header header;

    private final DataDictionaryParser dataDictionaryParser;

    private final DataDictionaryRowProcessor rowProcessor;


    public DataDictionaryProcessor(Header header,
                                   DataDictionaryParser dataDictionaryParser,
                                   DataDictionaryRowProcessor rowProcessor) {
        this.header = header;
        this.dataDictionaryParser = dataDictionaryParser;
        this.rowProcessor = rowProcessor;
    }

    public void processDataDictionary(InputStream inputStream) throws IOException {
        var processorHandler = new ProcessorHandler();
        dataDictionaryParser.parse(inputStream, header, processorHandler);
    }

    public class ProcessorHandler implements DataDictionaryHandler, DataDictionaryChoicesHandler {

        private final List<DataDictionaryChoice> choices = new ArrayList<>();

        @Override
        public void handleBeginChoices() {
            choices.clear();
        }

        @Override
        public void handleChoice(DataDictionaryChoice choice) {
            choices.add(choice);
        }

        @Override
        public void handleEndChoices() {
        }

        @Override
        public void handleBeginDataDictionary() {
        }

        @Override
        public void handleDataDictionaryRow(DataDictionaryRow row) {
                try {
                    System.out.println("Processing row: " + row.variableName());
                    rowProcessor.processRow(row, new ArrayList<>(choices));
                    choices.clear();
                } catch (OWLOntologyCreationException | OWLOntologyStorageException | IOException e) {
                    e.printStackTrace();
                }
        }

        @Override
        public void handleEndDataDictionary() {

        }

        @Override
        public DataDictionaryChoicesHandler getChoicesHandler() {
            return this;
        }
    }


}
