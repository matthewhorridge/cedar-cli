package org.metadatacenter.cedar.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.metadatacenter.artifacts.model.core.fields.FieldInputType;
import org.metadatacenter.cedar.api.*;
import org.metadatacenter.cedar.codegen.CodeGenerationNode;
import org.metadatacenter.cedar.codegen.CodeGenerationNodeRecord;
import org.metadatacenter.cedar.csv.*;
import org.metadatacenter.cedar.docs.DocsGenerator;
import org.metadatacenter.cedar.io.PostedArtifactResponse;
import org.metadatacenter.cedar.io.CedarArtifactPoster;
import org.metadatacenter.cedar.codegen.JavaGenerator;
import org.metadatacenter.cedar.ts.TypeScriptGenerator;
import org.metadatacenter.cedar.util.StripInstance;
import org.metadatacenter.cedar.util.StrippingOperations;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-07-29
 */
@Component
@Command(name = "csv2artifacts",
        description = "Generate CEDAR artifacts from a Comma Separated Values (CSV) file.  Artifacts are generated as CEDAR JSON-LD and are output as a set of JSON files.  Artifacts can also pushed directly into CEDAR.")
public class Csv2ArtifactsCommand implements CedarCliCommand {

    @Option(names = "--in", required = true, description = "A path to a CSV file that conforms to the CEDAR CSV format.")
    String input;

    @Option(names = "--out", required = true, description = "A path to a local directory where JSON-LD CEDAR representations of CEDAR artifacts will be written to.")
    Path outputDirectory;

    @Option(names = "--overwrite", defaultValue = "false",
            description = "Force generated artifacts to be locally overwritten if the local output directory is not empty")
    boolean overwrite;


    @Option(names = "--template-identifier")
    String templateIdentifier;

    @Option(names = "--template-name", required = true, description = "The name of the generated template")
    String templateName;

    @Option(names = "--default-template-id", description = "An id that can be used to specify the default id for the template")
    String defaultTemplateId = null;

    @Option(names = "--json-schema-description", description = "A string that will be inserted into the JSON-Schema 'description' property of all generated CEDAR artifact objects.", defaultValue = "Generated by CSV2CEDAR.")
    String jsonSchemaDescription;

    @Option(names = "--artifact-status",
            description = "Specifies the status of the artifacts that are generated.  Valid values are ${COMPLETION-CANDIDATES}",
            defaultValue = "DRAFT",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    ArtifactStatus artifactStatus;

    @Option(names = "--artifact-version",
            required = true,
            description = "A string in the format major.minor.patch that specifies the version number for generated artifacts",
            defaultValue = "0.0.1",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    String version;

    @Option(names = "--generate-fields", defaultValue = "false",
            description = "Specifies that CEDAR template fields should be individually generated.")
    boolean generateFields;

    @Option(names = "--generate-elements", defaultValue = "false",
            description = "Specifies that individual CEDAR template elements should be individually generated.")
    boolean generateElements;

    @Option(names = "--generate-docs", defaultValue = "false", description = "Specifies that markdown documentation for elements and fields should be generated.")
    boolean generateDocs;

    @Option(names = "--docs-file-name", description = "The output file name for the markdown file that is generated if the --generate-docs option is set to true.  By default this will be output to a file called docs.md in a docs directory in the output path.  This option may be used to override this file path/name.")
    String docsOutputFileName;

    @Option(names = "--generate-umbrella-element", description = "Specifies that an umbrella element that contains all other elements in the template should be generated.")
    boolean generateUmbrellaElement;

    @Option(names = "--umbrella-element-name", description = "The name for the umbrella element.  This only has an effect if the --generate-umbrella-element flag is specified.")
    String umbrellaElementName;


    @Option(names = "--umbrella-element-description", description = "The description for the umbrella element.  This only has an effect if the --generate-umbrella-element flag is specified.")
    String umbrellaElementDescription;


    @Mixin
    BioPortalApiKeyMixin bioportalApiKey;

    @Option(names = "--artifact-previous-version", defaultValue = "", hidden = true)
    public String previousVersion;

    @Option(names = "--generate-example-template-instance", description = "Generate an example template instance using the example information contained within the CSV file.")
    boolean generateExampleTemplateInstance;

    @Option(names = "--example-template-instance-file-name", description = "The output file name for the example template instance JSON-LD file that is generated if the --generate-example-template-instance option is set to true.  By default this will be output to a file called example-template-instance.json in a examples directory in the output path.  This option may be used to override this file path/name.")
    String exampleTemplateInstanceOutputFileName;

    @Option(names = "--generate-type-script", description = "Generates a TypeScript source file that contains TypeScript interfaces that describe fields, elements and templates.", defaultValue = "false")
    private boolean generateTypeScript;

    @Option(names = "--generate-java", description = "Generates a Java source file that contains Java records that describe fields, elements and templates.", defaultValue = "false")
    private boolean generateJava;

    @Option(names = "--java-package-name", description = "If the generate-java option is specified then this option allows the package name for the generated Java code to be set", defaultValue = "")
    private String javaPackageName;

    @Option(names = "--root-class-name", description = "The name of the Java class that other generated classes that represent CEDAR artifacts will be inner classes of", defaultValue = "Cedar", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private String rootClassName;

    @ArgGroup(exclusive = false)
    public PostToCedarOptions pushToCedar;

    private final CedarArtifactPoster importer;

    private final CedarCsvParserFactory cedarCsvParserFactory;

    private final CliCedarArtifactWriter writer;

    private final Map<CedarId, CedarId> artifact2GeneratedIdMap = new HashMap<>();

    private final DocsGenerator docsGenerator;

    private final TemplateInstanceGenerator templateInstanceGenerator;

    private final StripInstance stripInstance;

    private final ObjectMapper objectMapper;

    public Csv2ArtifactsCommand(CedarArtifactPoster importer,
                                CedarCsvParserFactory cedarCsvParserFactory,
                                CliCedarArtifactWriter writer,
                                DocsGenerator docsGenerator,
                                TemplateInstanceGenerator templateInstanceGenerator,
                                StripInstance stripInstance, ObjectMapper objectMapper) {
        this.importer = importer;
        this.cedarCsvParserFactory = cedarCsvParserFactory;
        this.writer = writer;
        this.docsGenerator = docsGenerator;
        this.templateInstanceGenerator = templateInstanceGenerator;
        this.stripInstance = stripInstance;
        this.objectMapper = objectMapper;
    }

    @Override
    public Integer call() throws Exception {
        if(input == null) {
            System.err.println("Input (file or URL) not specified");
            return 1;
        }
        final URI inputUri;
        final String inputShortName;
        if(input.startsWith("http")) {
           inputUri = new URI(input);
           inputShortName = input;
        }
        else {
            var inputPath = Path.of(input);
            if(!Files.exists(inputPath)) {
                System.err.println("Input file " + inputPath + " does not exist");
                System.exit(1);
            }
            inputUri = inputPath.toUri();
            inputShortName = inputPath.getFileName().toString();
        }

        if(jsonSchemaDescription == null) {
            jsonSchemaDescription = "Generated from " + inputShortName + " by CEDAR-CSV on " + Instant.now();
        }

        if(version == null) {
            version = VersionInfo.initialDraft().pavVersion();
        }

        if(!Files.exists(outputDirectory)) {
            Files.createDirectories(outputDirectory);
        }
        else {
            var nonEmpty = Files.list(outputDirectory).findFirst().isPresent();
            if(nonEmpty && !overwrite) {
                System.err.println("Output directory is not empty.  To overwrite existing files use the --overwrite option.");
                return 1;
            }
        }

        System.err.println("Loading template sheet from " + inputUri);
        var inputStream = inputUri.toURL().openStream();
        var cedarCsvParser = cedarCsvParserFactory.createParser(artifactStatus,
                                                                version, previousVersion);
        try {
            var rootNode = cedarCsvParser.parseNodes(inputStream);

            if (generateTypeScript) {
                new TypeScriptGenerator().generateTypeScript(Path.of("."), rootNode);
            }

            if(generateJava) {
                var codeGenerationNode = toCodeGenerationNode(rootNode);
                var codeOutputDirectory = outputDirectory.resolve("code");
                JavaGenerator.get(javaPackageName, rootClassName, true)
                                        .writeJavaFile(codeGenerationNode, codeOutputDirectory);
            }

            if(templateIdentifier == null) {
                templateIdentifier = templateName.trim().toLowerCase().replace(" ", "-");
            }
            var template = cedarCsvParser.translateToTemplate(rootNode, templateIdentifier, templateName);

            // Write artifacts in a depth first manner

            if (generateFields) {
                var fields = template.getAllFields()
                                     .stream().filter(f -> !f.ui().inputType().equals(InputType.ATTRIBUTE_VALUE))
                                     .toList();
                writeArtifacts(fields);
            }

            if (generateElements) {
                var elements = template.getAllElements();
                writeArtifacts(elements);
            }

            writeArtifacts(List.of(template));

            if (generateUmbrellaElement) {
                var umbrellaElement = template.asElement(umbrellaElementName,
                                                         umbrellaElementDescription,
                                                         version,
                                                         artifactStatus,
                                                         previousVersion);
                writeArtifacts(List.of(umbrellaElement));


            }

            if(generateExampleTemplateInstance) {
                var exampleInstancePath = getExampleTemplateInstanceFileName();
                if(!Files.exists(exampleInstancePath.getParent())) {
                    Files.createDirectories(exampleInstancePath.getParent());
                }
                var templateId = getTemplateId(template);
                System.err.println("Generating example template instance in " + exampleInstancePath);
                var exampleInstance = templateInstanceGenerator.generateCedarInstance(rootNode,
                                                                                      TemplateInstanceGenerationMode.WITH_EXAMPLES_AND_DEFAULTS,
                                                                                      "Example Metadata Instance",
                                                                                      "Generated by CEDARCSV",
                                                                                      templateId);
                writeArtifacts(List.of(exampleInstance));



                var blankInstance = templateInstanceGenerator.generateCedarInstance(rootNode,
                                                                                          TemplateInstanceGenerationMode.WITH_DEFAULTS,
                                                                                          "Example Blank Metadata Instance",
                                                                                          "Generated by CEDARCSV",
                                                                                          templateId);
                var examplesDirectory = outputDirectory.resolve("examples");
                var blankJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(blankInstance);
                Files.writeString(examplesDirectory.resolve("blank.json"), blankJson);

                var jsonNode = objectMapper.convertValue(blankInstance, JsonNode.class);

                var strippedJsonNode = stripInstance.stripInstance(jsonNode,
                                            Set.of(StrippingOperations.STRIP_CONTEXT,
                                                   StrippingOperations.STRIP_TYPES
                                            ));
                var strippedJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(strippedJsonNode);
                Files.writeString(examplesDirectory.resolve("blank-stripped.json"), strippedJson);

                var strippedCollapsedJsonNode = stripInstance.stripInstance(jsonNode,
                                                                   Set.of(StrippingOperations.STRIP_CONTEXT,
                                                                          StrippingOperations.STRIP_TYPES,
                                                                          StrippingOperations.COLLAPSE_ENTITIES,
                                                                          StrippingOperations.COLLAPSE_VALUES,
                                                                          StrippingOperations.STRIP_IDS
                                                                   ));
                var strippedCollapsedJson = objectMapper.writerWithDefaultPrettyPrinter()
                                               .writeValueAsString(strippedCollapsedJsonNode);
                Files.writeString(examplesDirectory.resolve("blank-stripped-collapsed.json"), strippedCollapsedJson);


                new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                                                  .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
                        .writeValue(examplesDirectory.resolve("blank-stripped-collapsed.yml").toFile(), strippedCollapsedJsonNode);


                var ctx = new JsonLdContextGenerator().generateContext(rootNode);
                var ctxJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(ctx);
                Files.writeString(examplesDirectory.resolve("context.json"), ctxJson);


            }

            if(generateDocs) {
                var docsPath = getDocumentationFileName();
                if (!Files.exists(docsPath.getParent())) {
                    Files.createDirectories(docsPath.getParent());
                }
                System.err.println("Generating documentation in " + docsPath);
                docsGenerator.writeDocs(template,  docsPath, bioportalApiKey.getApiKey());
            }
        } catch (CedarCsvParseException e) {
            System.err.println("\033[31;1mERROR: " + e.getMessage() + "\033[0m");
            System.err.println("   \033[31;1mAt: " + e.getNode().getPath().stream()
                                                          .map(CedarCsvParser.Node::getName)
                                                      .collect(Collectors.joining(" > "))+ "\033[0m");
        }
        return 0;
    }

    public static CodeGenerationNode toCodeGenerationNode(CedarCsvParser.Node node) {
        var row = node.getRow();
        FieldInputType inputType;
        if(row == null) {
            inputType = null;
        }
        else {
            inputType = row.getInputType().map(CedarCsvInputType::getCedarInputType).map(InputType::getFieldInputType).orElse(null);
        }
        return new CodeGenerationNodeRecord(
                null,
                node.isRoot(),
                node.getName(),
                node.getChildNodes().stream().map(Csv2ArtifactsCommand::toCodeGenerationNode).toList(),
                getArtifactType(node), node.getDescription(),
                node.getXsdDatatype().orElse(null),
                node.isRequired() ? CodeGenerationNode.Required.REQUIRED : CodeGenerationNode.Required.OPTIONAL,
                node.getCardinality().equals(Cardinality.SINGLE) ? CodeGenerationNode.Cardinality.getZeroOrOne() : CodeGenerationNode.Cardinality.getZeroOrMore(),
                node.getPropertyIri().map(Object::toString).orElse(null),
                inputType);
    }

    private static CodeGenerationNode.ArtifactType getArtifactType(CedarCsvParser.Node node) {
        if(node.isField()) {
            if(node.isLiteralValueType()) {
                return CodeGenerationNode.ArtifactType.LITERAL_FIELD;
            }
            else {
                return CodeGenerationNode.ArtifactType.IRI_FIELD;
            }
        }
        else if(node.isElement()) {
            return CodeGenerationNode.ArtifactType.ELEMENT;
        }
        else {
            return CodeGenerationNode.ArtifactType.TEMPLATE;
        }
    }

    private CedarId getTemplateId(CedarTemplate template) {
        var tid = artifact2GeneratedIdMap.get(template.id());
        if(tid != null) {
            return tid;
        }
        if(this.defaultTemplateId != null) {
            return CedarId.valueOf(this.defaultTemplateId);
        }
        return template.id();
    }

    private Path getDocumentationFileName() {
        if(docsOutputFileName == null) {
            var docsDirectory = outputDirectory.resolve("docs");
            return docsDirectory.resolve("docs.md");
        }
        var outputFile = Path.of(docsOutputFileName);
        if(outputFile.isAbsolute()) {
            return outputFile;
        }
        else {
            return outputDirectory.resolve(outputFile);
        }
    }

    private Path getExampleTemplateInstanceFileName() {
        if(exampleTemplateInstanceOutputFileName == null) {
            var docsDirectory = outputDirectory.resolve("examples");
            return docsDirectory.resolve("example-template-instance.json");
        }
        var outputFile = Path.of(exampleTemplateInstanceOutputFileName);
        if(outputFile.isAbsolute()) {
            return outputFile;
        }
        else {
            return outputDirectory.resolve(outputFile);
        }
    }

    private void writeArtifacts(List<? extends CedarArtifact> artifacts) {
        var counter = new AtomicInteger();
        if(shouldPushToCedar()) {
            artifacts.forEach(artifact -> {
                try {
                    var initialId = artifact.id();
                    var artifactWithReplacedIds = artifact.replaceIds(artifact2GeneratedIdMap);
                    var artifactWithNullId = artifactWithReplacedIds.withId(null);
                    var posted = postArtifactToCedar(artifactWithNullId);
                    counter.incrementAndGet();
                    posted.ifPresent(r -> {
                        System.err.printf("\033[32;1mPosted\033[30;0m %s %d of %d to CEDAR\n", artifact.getSimpleTypeName().getName(), counter.get(), artifacts.size());
                        System.err.printf("    %s (id=%s)\n", r.schemaName(), r.cedarId().value());
                        if (initialId != null) {
                            artifact2GeneratedIdMap.put(initialId, r.cedarId());
                            var postedArtifact = artifactWithReplacedIds.withId(r.cedarId());
                            writeCedarArtifact(postedArtifact);
                        }
                    });
                } catch (IOException | InterruptedException e) {
                    System.err.println(e.getMessage());
                }
            });
        }
        else {
            artifacts.forEach(this::writeCedarArtifact);
        }
    }

    private Optional<PostedArtifactResponse> postArtifactToCedar(CedarArtifact artifact) throws IOException, InterruptedException {
        var cedarFolderId = getFolderId();
        // The ID must be null.  This is because CEDAR mints it
        return importer.postToCedar(artifact, cedarFolderId,
                                    pushToCedar.getCedarApiKey(),
                                    artifact.toCompactString(), jsonSchemaDescription);
    }

    private CedarId getFolderId() {
        return pushToCedar.getCedarFolderId();
    }

    private boolean shouldPushToCedar() {
        return pushToCedar != null && pushToCedar.postToCedar;
    }

    private void writeCedarArtifact(CedarArtifact f) {
        try {
            writer.writeCedarArtifact(f, outputDirectory, jsonSchemaDescription);
        } catch (IOException e) {
            System.err.println("Could not write " + f.getSimpleTypeName().getName() + ": " + e.getMessage());
        }
    }
}
