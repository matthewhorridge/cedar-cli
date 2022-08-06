package org.metadatacenter.cedar.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-08-04
 */
public record EmbeddedCedarArtifact(@JsonUnwrapped EmbeddableCedarArtifact artifact,
                                    @JsonUnwrapped Multiplicity multiplicity,
                                    @JsonIgnore Visibility visibility) {

    @JsonIgnore
    public String getSchemaName() {
        return artifact.getSchemaName();
    }

    @JsonIgnore
    public String getSchemaDescription() {
        return artifact.getSchemaDescription();
    }
}