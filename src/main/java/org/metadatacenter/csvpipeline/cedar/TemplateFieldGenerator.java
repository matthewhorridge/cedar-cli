package org.metadatacenter.csvpipeline.cedar;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.CharMatcher;
import org.metadatacenter.csvpipeline.redcap.DataDictionaryChoice;
import org.metadatacenter.csvpipeline.redcap.DataDictionaryRow;
import org.metadatacenter.csvpipeline.redcap.FieldType;
import org.metadatacenter.csvpipeline.redcap.RedcapValidationType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-06-17
 */
public class TemplateFieldGenerator {

    private static final String TEMPLATE_FILE = "/cedar-template-field.json";

    private final CedarValuesStrategy valuesStrategy;

    private final String templateFieldDescription;

    private Pattern dateTimeValidationPattern = Pattern.compile("^date(time)?.+");

    public TemplateFieldGenerator(CedarValuesStrategy valuesStrategy, String templateFieldDescription) {
        this.valuesStrategy = valuesStrategy;
        this.templateFieldDescription = templateFieldDescription;
    }

    public String generateTemplateField(DataDictionaryRow row,
                                        List<DataDictionaryChoice> choices) {
        try {
            var multipleChoice = getMultipleChoice(row);
            var required = row.isRequired();
            var inputType = getInputType(row);

            if(inputType.isEmpty()) {
                return "";
            }

            var objectMapper = new ObjectMapper();
            objectMapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());
            var srcUrl = TemplateFieldGenerator.class.getResource(TEMPLATE_FILE);
            var parser = objectMapper.createParser(srcUrl);
            var tree = (ObjectNode) parser.readValueAsTree();

            var description = row.fieldNotes() != null ? row.fieldNotes() : templateFieldDescription;

            var ui = (ObjectNode) tree.get("_ui");
            ui.set("inputType", text(inputType.get().getName()));
            tree.set("schema:name", text(row.fieldLabel()));
            tree.set("schema:identifier", text(row.variableName()));
            tree.set("schema:description", text(description));
            tree.set("skos:prefLabel", text(row.variableName()));

            var valueConstraintsNode = (ObjectNode) tree.path("_valueConstraints");
            valueConstraintsNode.set("requiredValue", JsonNodeFactory.instance.booleanNode(required));
            valueConstraintsNode.set("multipleChoice", JsonNodeFactory.instance.booleanNode(multipleChoice));

            if(row.textValidationMin() != null && !row.textValidationMin().isBlank()) {
                valueConstraintsNode.set("minValue", getMinValueNode(row.textValidationMin()));
            }

            if(row.textValidationMax() != null && !row.textValidationMax().isBlank()) {
                valueConstraintsNode.set("maxValue", getMinValueNode(row.textValidationMax()));
            }

            if("integer".equals(row.textValidationOrShowSliderNumber())) {
                valueConstraintsNode.set("numberType", text("xsd:long"));
            }

            var validation = row.textValidationOrShowSliderNumber().trim().toLowerCase();
            var redCapValidation = RedcapValidationType.get(validation);

            redCapValidation.flatMap(RedcapValidationType::getTemporalGranularity)
                    .ifPresent(tg -> ui.set("temporalGranularity", text(tg.getName())));
            redCapValidation.flatMap(RedcapValidationType::getTemporalType)
                    .ifPresent(tt -> valueConstraintsNode.set("temporalType", text(tt.getName())));

            redCapValidation.flatMap(RedcapValidationType::getDecimalPlaces)
                    .ifPresent(dp -> valueConstraintsNode.set("decimalPlace", JsonNodeFactory.instance.numberNode(dp.getDecimalPlace())));

            if(!choices.isEmpty()) {
                // Specify the values for the field only if the list of choices is not empty
                valuesStrategy.installValuesNode(JsonNodeFactory.instance,
                                                 row,
                                                 choices,
                                                 valueConstraintsNode);
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private TextNode getMinValueNode(String s) {
        return JsonNodeFactory.instance.textNode(s);
    }

    private boolean getMultipleChoice(DataDictionaryRow row) {
        return row.fieldType() == FieldType.CHECKBOX;
    }

    private Optional<CedarInputType> getInputType(DataDictionaryRow row) {
        switch (row.fieldType()) {
            case CHECKBOX -> {
                return  Optional.of(CedarInputType.CHECKBOX);
            }
            case RADIO -> {
                return Optional.of(CedarInputType.RADIO);
            }
            case DROP_DOWN -> {
                return Optional.of(CedarInputType.LIST);
            }
            case TEXT -> {
                return Optional.of(row.textValidationOrShowSliderNumber())
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .flatMap(RedcapValidationType::get)
                        .map(RedcapValidationType::getCedarInputType)
                        .or(() -> Optional.of(CedarInputType.TEXTFIELD));
            }
            case SECTION_HEADER ->  {
                return Optional.of(CedarInputType.SECTION_BREAK);
            }
            case NOTES -> {
                return Optional.of(CedarInputType.TEXTAREA);
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    private static TextNode text(String value) {
        return JsonNodeFactory.instance.textNode(value);
    }

}
