package org.metadatacenter.cedar.api;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-10-17
 */
public record CedarInstancesStringNode(String stringValue) implements CedarInstanceNode, CedarInstanceFieldValueNode {

    @JsonValue
    public String stringValue() {
        return stringValue;
    }
}