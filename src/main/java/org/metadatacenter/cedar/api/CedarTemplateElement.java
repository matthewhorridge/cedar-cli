package org.metadatacenter.cedar.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-07-26
 * <p>
 * A CEDAR Template Element is a container for Template Fields and other Template Elements (that is, template nodes)
 *
 * @param id    A {@link CedarId} that identifies this object.  Cedar Ids can be null.
 * @param nodes A list of contained Template Element and Template Field objects
 */
public record CedarTemplateElement(@Nullable @JsonProperty("@id") CedarId id,
                                   @Nonnull @JsonUnwrapped ArtifactInfo artifactInfo,
                                   @Nonnull @JsonUnwrapped CedarVersionInfo versionInfo,
                                   @Nonnull @JsonUnwrapped ArtifactModificationInfo modificationInfo,
                                   @Nonnull @JsonIgnore List<CedarTemplateNode> nodes) implements CedarTemplateNode, CedarSchemaArtifact, CedarArtifactContainer {

    @Override
    public <R, E extends Exception> R accept(CedarSchemaArtifactVisitor<R, E> visitor) throws E {
        return visitor.visit(this);
    }

    @Override
    public String getSchemaName() {
        return artifactInfo.schemaName();
    }

    @Override
    public String getSchemaDescription() {
        return artifactInfo.schemaDescription();
    }

    @Override
    public void ifTemplateElement(Consumer<CedarTemplateElement> elementConsumer) {
        elementConsumer.accept(this);
    }

    @Override
    public void ifTemplateField(Consumer<CedarTemplateField> fieldConsumer) {

    }

    @Override
    public String toCompactString() {
        return "Element(" + artifactInfo.schemaName() + ")";
    }

    @Override
    public @Nonnull
    ArtifactSimpleTypeName getSimpleTypeName() {
        return ArtifactSimpleTypeName.ELEMENT;
    }
}
