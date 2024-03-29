package org.metadatacenter.cedar.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-07-26
 */
public record CedarTemplate(@JsonProperty("@id") CedarId id,
                            @JsonUnwrapped ArtifactInfo artifactInfo,
                            @JsonUnwrapped VersionInfo versionInfo,
                            @JsonUnwrapped ModificationInfo modificationInfo,
                            List<EmbeddedCedarArtifact> nodes) implements CedarSchemaArtifact, CedarArtifactContainer {

    @Nonnull
    @Override
    public CedarTemplate withId(CedarId id) {
        return new CedarTemplate(id, artifactInfo, versionInfo, modificationInfo, nodes);
    }

    @Override
    public String toCompactString() {
        return "Template(" + artifactInfo.schemaName() + ")";
    }

    @Override
    public @Nonnull ArtifactSimpleTypeName getSimpleTypeName() {
        return ArtifactSimpleTypeName.TEMPLATE;
    }

    @Override
    public <R, E extends Exception> R accept(CedarSchemaArtifactVisitor<R, E> visitor) throws E {
        return visitor.visit(this);
    }

    @Nonnull
    @Override
    public CedarTemplate replaceIds(Map<CedarId, CedarId> idReplacementMap) {
        var replacedChildNodes = nodes.stream()
                                      .map(n -> n.replaceIds(idReplacementMap))
                                      .toList();
        var replacementId = getReplacementId(idReplacementMap);
        return new CedarTemplate(replacementId, artifactInfo, versionInfo, modificationInfo, replacedChildNodes);
    }

    /**
     * Gets all nested elements and this element in a depth first traversal order.
     */
    @JsonIgnore
    public List<CedarTemplateElement> getAllElements() {
        var elements = new ArrayList<CedarTemplateElement>();
        collectElements(nodes, elements);
        return elements;
    }

    private void collectElements(List<EmbeddedCedarArtifact> nodes, List<CedarTemplateElement> elements) {
        nodes.forEach(n -> collectElements(n, elements));
    }

    private void collectElements(EmbeddedCedarArtifact node, List<CedarTemplateElement> elements) {
        node.artifact().ifTemplateElement(element -> {
            collectElements(element.nodes(), elements);
            elements.add(element);
        });
    }

    @JsonIgnore
    public List<CedarTemplateField> getAllFields() {
        var fields = new ArrayList<CedarTemplateField>();
        collectFields(nodes, fields);
        return fields;
    }

    @JsonIgnore
    public List<List<CedarArtifact>> getAllFieldsWithPaths() {
        var path = new ArrayList<CedarArtifact>();
        var fields = new ArrayList<List<CedarArtifact>>();
        collectFieldsWithPaths(nodes, path, fields);
        return fields;
    }


    private void collectFieldsWithPaths(Collection<EmbeddedCedarArtifact> nodes, List<CedarArtifact> currentPath, List<List<CedarArtifact>> fields) {
        nodes.forEach(n -> {
            var pathForNode = new ArrayList<>(currentPath);
            pathForNode.add(n.artifact());
            n.artifact().ifTemplateField(f -> {
                fields.add(pathForNode);
            });
            n.artifact().ifTemplateElement(e -> {
                collectFieldsWithPaths(e.nodes(), pathForNode, fields);
            });
        });
    }

    private void collectFields(Collection<EmbeddedCedarArtifact> nodes, List<CedarTemplateField> fields) {
        nodes.forEach(n -> collectFields(n, fields));
    }

    private void collectFields(EmbeddedCedarArtifact node, List<CedarTemplateField> fields) {
        node.artifact().ifTemplateElement(element -> {
            collectFields(element.nodes(), fields);
        });
        node.artifact().ifTemplateField(fields::add);
    }

    /**
     * Create an element that essentially contains all elements from this template
     * @return The element
     */
    public CedarTemplateElement asElement(String schemaName,
                                          String schemaDescription,
                                          String version,
                                          ArtifactStatus status,
                                          String previousVersion) {
        return new CedarTemplateElement(
                CedarId.generateUrn(),
                null,
                new ArtifactInfo(schemaName.trim().toLowerCase().replace(" ", "_"),
                                 schemaName,
                                 schemaDescription,
                                 null,
                                 schemaName,
                                 List.of()),
                new VersionInfo(version, status, previousVersion),
                ModificationInfo.empty(),
                nodes,
                SupplementaryInfo.empty()
        );
    }
}
