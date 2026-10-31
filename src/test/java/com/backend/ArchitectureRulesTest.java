package com.backend;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The dependency rule, enforced on every build: source code only ever points inward. */
class ArchitectureRulesTest {

    private static final Path ROOT = Path.of("src/main/java/com/backend");

    /** Java, Lombok and its own inner layers are all an inner layer may import; domain also gets HttpStatus for BusinessError, application Spring's common annotations and Bean Validation. */
    private static final Pattern DOMAIN_ALLOWED =
            Pattern.compile("java\\..*|lombok\\..*|org\\.springframework\\.http\\.HttpStatus|com\\.backend\\.domain\\..*");
    private static final Pattern APPLICATION_ALLOWED =
            Pattern.compile("java\\..*|lombok\\..*|org\\.springframework\\.(stereotype|transaction\\.annotation)\\..*|jakarta\\.validation\\.constraints\\..*"
                    + "|com\\.backend\\.(domain|application)\\..*");

    @Test
    void domainDependsOnNothingOutsideItself() throws IOException {
        assertEquals(List.of(), importsNotMatching("domain", DOMAIN_ALLOWED));
    }

    @Test
    void applicationDependsOnlyOnDomain() throws IOException {
        assertEquals(List.of(), importsNotMatching("application", APPLICATION_ALLOWED));
    }

    @Test
    void infrastructureDoesNotKnowPresentation() throws IOException {
        assertEquals(List.of(), importsMatching("infrastructure", Pattern.compile("com\\.backend\\.presentation\\..*")));
    }

    @Test
    void presentationKnowsNeitherInfrastructureNorDomainModels() throws IOException {
        assertEquals(List.of(), importsMatching("presentation",
                Pattern.compile("com\\.backend\\.(infrastructure|domain\\.model)\\..*")));
    }

    @Test
    void responsesExposeNoDomainModel() throws IOException {
        assertEquals(List.of(), importsMatching("application/dto", Pattern.compile("com\\.backend\\.domain\\.model\\..*")));
    }

    @Test
    void onlyInfrastructureKnowsThePaymentGateway() throws IOException {
        for (String layer : List.of("domain", "application", "presentation")) {
            try (Stream<Path> files = Files.walk(ROOT.resolve(layer))) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    assertFalse(Files.readString(file).toLowerCase().contains("sepay"), file.toString());
                }
            }
        }
    }

    @Test
    void eachLayerActuallyHasCodeToCheck() throws IOException {
        for (String layer : List.of("domain", "application", "infrastructure", "presentation")) {
            assertTrue(importsOf(layer).findAny().isPresent(), layer);
        }
    }

    private static List<String> importsNotMatching(String layer, Pattern allowed) throws IOException {
        return importsOf(layer).filter(line -> !allowed.matcher(importedName(line)).matches()).toList();
    }

    private static List<String> importsMatching(String layer, Pattern forbidden) throws IOException {
        return importsOf(layer).filter(line -> forbidden.matcher(importedName(line)).matches()).toList();
    }

    // "<file>: import static a.b.C.d;" -> "a.b.C.d"
    private static String importedName(String line) {
        return line.substring(line.indexOf("import ") + "import ".length()).replace("static ", "").replace(";", "").trim();
    }

    private static Stream<String> importsOf(String layer) throws IOException {
        try (Stream<Path> files = Files.walk(ROOT.resolve(layer))) {
            return files.filter(file -> file.toString().endsWith(".java")).toList().stream()
                    .flatMap(ArchitectureRulesTest::importLines);
        }
    }

    private static Stream<String> importLines(Path file) {
        try {
            return Files.readAllLines(file).stream()
                    .filter(line -> line.startsWith("import "))
                    .map(line -> ROOT.relativize(file) + ": " + line);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
