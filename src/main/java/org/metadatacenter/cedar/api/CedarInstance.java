package org.metadatacenter.cedar.api;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nonnull;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-10-14
 */
public record CedarInstance(@JsonView(FragmentView.class) @JsonProperty("@context") CedarInstanceContext context,
                            @JsonView(FragmentView.class) @JsonProperty("@id") CedarId id,
                            @JsonView(FragmentView.class) @JsonAnyGetter Map<String, CedarInstanceNode> children,
                            @JsonProperty("schema:name") String schemaName,
                            @JsonProperty("schema:description") String schemaDescription,
                            @JsonProperty("schema:isBasedOn") CedarId schemaIsBasedOn,
                            @JsonUnwrapped ModificationInfo modificationInfo) implements CedarInstanceNode, CedarArtifact {

    @Nonnull
    @Override
    public CedarArtifact withId(CedarId id) {
        var childrenWithoutIds = new LinkedHashMap<String, CedarInstanceNode>();
        children.forEach((name, node) -> {
            childrenWithoutIds.put(name, node.withoutId());
        });
        return new CedarInstance(context, id, childrenWithoutIds, schemaName, schemaDescription, schemaIsBasedOn, modificationInfo);
    }

    public CedarInstance prune(String retain) {
        var pruned = new HashMap<String, CedarInstanceNode>();
        pruned.put(retain, children.get(retain));
        return new CedarInstance(context.prune(retain), id, pruned, schemaName, schemaDescription, schemaIsBasedOn, modificationInfo);
    }

    @Override
    public CedarInstanceNode withoutId() {
        return new CedarInstance(context, new CedarId(""), children, schemaName, schemaDescription, schemaIsBasedOn, modificationInfo);
    }

    @Nonnull
    @Override
    public CedarArtifact replaceIds(Map<CedarId, CedarId> idReplacementMap) {
        return this;
    }

    @Override
    public CedarId getReplacementId(Map<CedarId, CedarId> idReplacementMap) {
        return idReplacementMap.get(id);
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ArtifactInfo artifactInfo() {
        return new ArtifactInfo(schemaName, schemaName, schemaDescription, null, schemaName, List.of());
    }

    @Override
    public String toCompactString() {
        return String.format("Instance(id=%s)", id);
    }

    @Nonnull
    @Override
    public ArtifactSimpleTypeName getSimpleTypeName() {
        return ArtifactSimpleTypeName.INSTANCE;
    }

    public CedarInstance getJsonLdBoilerPlate() {
        var childrenBoilerPlate =  new LinkedHashMap<String, CedarInstanceNode>();
        children.forEach((fieldName, fieldValue) -> {
            if(!(fieldValue instanceof CedarInstanceFieldValueNode)) {
                var fieldBoilerPlate = fieldValue.getJsonLdBoilerPlate();
                if (fieldBoilerPlate instanceof CedarInstanceListNode listNode) {
                    if(!listNode.isEmpty()) {
                        childrenBoilerPlate.put(fieldName, fieldBoilerPlate);
                    }
                }
                else {
                    childrenBoilerPlate.put(fieldName, fieldBoilerPlate);
                }
            }
        });
        return new CedarInstance(context, id, childrenBoilerPlate, schemaName, null, null, null);
    }

    @Override
    public CedarInstance getEmptyCopy() {
        var emptyChildren = new LinkedHashMap<String, CedarInstanceNode>();
        children.forEach((fieldName, fieldValue) -> {
            emptyChildren.put(fieldName, fieldValue.getEmptyCopy());
        });
        return new CedarInstance(context, id, emptyChildren, schemaName, schemaDescription, schemaIsBasedOn, modificationInfo);
    }
}
