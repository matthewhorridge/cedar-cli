package org.metadatacenter.cedar.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-07-26
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record VersionInfo(@JsonProperty("pav:version") String pavVersion,
                          @JsonProperty("bibo:status") ArtifactStatus biboStatus,
                          @JsonProperty("pav:previousVersion") String pavPreviousVersion) {

    @JsonCreator
    public VersionInfo(@JsonProperty("pav:version") String pavVersion,
                       @JsonProperty("bibo:status") ArtifactStatus biboStatus,
                       @JsonProperty("pav:previousVersion") String pavPreviousVersion) {
        this.pavVersion = Objects.requireNonNullElse(pavVersion, "");
        this.biboStatus = Objects.requireNonNullElse(biboStatus, ArtifactStatus.DRAFT);
        this.pavPreviousVersion = Objects.requireNonNullElse(pavPreviousVersion, "");
    }

    /**
     * Gets the version info for an initial draft.  This has a version number of 0.0.1
     * a status of draft and no previous version specified.
     */
    public static VersionInfo initialDraft() {
        return new VersionInfo("0.0.1", ArtifactStatus.DRAFT, "");
    }
}
