package org.metadatacenter.cedar.cli;

import org.metadatacenter.cedar.api.*;
import org.metadatacenter.cedar.csv.*;
import org.metadatacenter.cedar.docs.DocsGenerator;
import org.metadatacenter.cedar.io.PostedArtifactResponse;
import org.metadatacenter.cedar.io.CedarArtifactPoster;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
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
    Path inputCsvFile;

    @Option(names = "--out", required = true, description = "A path to a local directory where JSON-LD CEDAR representations of CEDAR artifacts will be written to.")
    Path outputDirectory;

    @Option(names = "--overwrite", defaultValue = "false",
            description = "Force generated artifacts to be locally overwritten if the local output directory is not empty")
    boolean overwrite;

    @Option(names = "--json-schema-description", description = "A string that will be inserted into the JSON-Schema 'description' property of all generated CEDAR artifact objects.", defaultValue = "Generated by CSV2CEDAR.")
    String jsonSchemaDescription;

    @Option(names = "--artifact-status",
            description = "Specifies the status of the artifacts that are generated.  Valid values are ${COMPLETION-CANDIDATES}",
            defaultValue = "DRAFT",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    ArtifactStatus artifactStatus;

    @Option(names = "--artifact-version",
            required = true,
            description = "A string in the format major.minor.patch that specifies the version number for generatated artifacts")
    String version;

    @Option(names = "--generate-fields", defaultValue = "false",
            description = "Specifies that CEDAR template fields should be individually generated.")
    boolean generateFields;

    @Option(names = "--generate-elements", defaultValue = "false",
            description = "Specifies that individual CEDAR template elements should be individually generated.")
    boolean generateElements;

    @Option(names = "--generate-docs", defaultValue = "false", description = "Specifies that markdown documentation for elements and fields should be generated.")
    boolean generateDocs;

    @Option(names = "--docs-file-name", description = "The output file name for the markdown file that is generated if the --generate-docs option is set to true.  By default this will be output to a file called docs.md in the output path.  This option may be used to override this file path/name.")
    String docsOutputFileName;


    @Mixin
    BioPortalApiKeyMixin bioportalApiKey;

    @Option(names = "--artifact-previous-version", defaultValue = "", hidden = true)
    public String previousVersion;

    @ArgGroup(exclusive = false)
    public PostToCedarOptions pushToCedar;

    private final CedarArtifactPoster importer;

    private final CedarCsvParserFactory cedarCsvParserFactory;

    private final CliCedarArtifactWriter writer;

    private final Map<CedarId, CedarId> artifact2GeneratedIdMap = new HashMap<>();

    private final DocsGenerator docsGenerator;

    public Csv2ArtifactsCommand(CedarArtifactPoster importer,
                                CedarCsvParserFactory cedarCsvParserFactory,
                                CliCedarArtifactWriter writer,
                                DocsGenerator docsGenerator) {
        this.importer = importer;
        this.cedarCsvParserFactory = cedarCsvParserFactory;
        this.writer = writer;
        this.docsGenerator = docsGenerator;
    }

    @Override
    public Integer call() throws Exception {
        if(jsonSchemaDescription == null) {
            jsonSchemaDescription = "Generated from " + inputCsvFile.getFileName().toString() + " by CEDAR-CSV on " + Instant.now();
        }
        if(inputCsvFile == null) {
            System.err.println("Input file not specified");
        }
        if(!Files.exists(inputCsvFile)) {
            System.err.println("Input file " + inputCsvFile + " does not exist");
            System.exit(1);
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
        var inputStream = Files.newInputStream(inputCsvFile);
        var cedarCsvParser = cedarCsvParserFactory.createParser(artifactStatus,
                                                                version, previousVersion);
        try {
            var template = cedarCsvParser.parse(inputStream);

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

            if(generateDocs) {
                var docsPath = getDocumentationFileName();
                if (!Files.exists(docsPath.getParent())) {
                    Files.createDirectories(docsPath.getParent());
                }
                docsGenerator.writeDocs(template, docsPath, bioportalApiKey.getApiKey());
            }


        } catch (CedarCsvParseException e) {
            System.err.println("\033[31;1mERROR: " + e.getMessage() + "\033[0m");
            System.err.println("   \033[31;1mAt: " + e.getNode().getPath().stream()
                                                          .map(CedarCsvParser.Node::getName)
                                                      .collect(Collectors.joining(" > "))+ "\033[0m");
        }

        return 0;
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

    private void writeArtifacts(List<? extends CedarArtifact> artifacts) {
        artifacts.forEach(this::writeCedarArtifact);
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
                        }
                    });
                } catch (IOException | InterruptedException e) {
                    System.err.println(e.getMessage());
                }
            });
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
