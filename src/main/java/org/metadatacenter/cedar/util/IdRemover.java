package org.metadatacenter.cedar.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-10-12
 */
@Component
public class IdRemover implements StrippingOperation {

    private final FieldRemover fieldRemover;

    public IdRemover(FieldRemover fieldRemover) {
        this.fieldRemover = fieldRemover;
    }

    public JsonNode process(JsonNode node) {
        return fieldRemover.removeField("@id", node);
    }
}
