package at.jku.ssw.java.bytecode.generator;

import at.jku.ssw.java.bytecode.generator.cli.ControlValueParser;
import at.jku.ssw.java.bytecode.generator.cli.GenerationController;
import at.jku.ssw.java.bytecode.generator.generators.RandomCodeGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public interface GeneratorTest extends CLIArgumentsProvider {

    int TIMEOUT = 10;

    Logger logger = LogManager.getLogger();

    String TEMP_DIR = ".tmp";

    /**
     * The default output directory for generated files
     *
     * @return a path denoting the output directory for generated files
     * of this test class
     */
    default Path outputDirectory() {
        return Paths.get(TEMP_DIR).resolve(getClass().getSimpleName());
    }

    default void fail(GeneratedClass clazz, Throwable throwable) {
        Assertions.fail(
                "Test failed\n" +
                        "Command line arguments: " + clazz.args + "\n" +
                        "Seed: " + clazz.seed + "\n",
                throwable
        );
    }

    default GeneratedClass generateClass(String path, String name, String... options) {
        // join passed options and defaults
        String[] allOpts = Stream.concat(
                Stream.of(
                        "-filename", name               // use file name
                ),
                Stream.of(options)
        ).toArray(String[]::new);

        ControlValueParser parser = new ControlValueParser(allOpts);
        GenerationController controller = parser.parse();

        logger.info("Generating class {}", name);
        logger.info("Seed: {}", controller.getSeedValue());
        logger.info("Parameters: {}", String.join(" ", allOpts));

        final String className = controller.getFileName();

        RandomCodeGenerator randomCodeGenerator;
        try {
            randomCodeGenerator = new RandomCodeGenerator(className, controller);
            randomCodeGenerator.generate();
            Path p = outputDirectory().resolve(path);
            Files.createDirectories(p);
            randomCodeGenerator.writeFile(p.toString());
        } catch (Throwable t) {
            logger.error("Generation failed");
            logger.error("Command line arguments: {}", String.join(" ", allOpts));
            logger.error("Seed: {}", controller.getSeedValue());
            Assertions.fail(t);
            return null;
        }

        return new GeneratedClass(path, className, randomCodeGenerator.getSeed(), String.join(" ", allOpts));
    }

    default GeneratedClass generateClass(String path, String name, List<String> options) {
        return generateClass(path, name, options.toArray(new String[0]));
    }

    default void compareResults(Result expected, Result actual) {
        assertEquals(expected.out, actual.out.replaceAll(actual.className, expected.className));
        assertEquals(expected.err, actual.err.replaceAll(actual.className, expected.className));
    }

    default GeneratedClass generateClass(String name, List<String> options) {
        return generateClass("", name, options);
    }

    default Result run(GeneratedClass clazz)
            throws IOException, InterruptedException {

        Path path = outputDirectory().resolve(clazz.path);

        Process p = Runtime.getRuntime().exec("java " + clazz.name, null, path.toFile());

        try (BufferedReader outStr = new BufferedReader(new InputStreamReader(p.getInputStream()));
             BufferedReader errStr = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {

            if (!p.waitFor(TIMEOUT, TimeUnit.MINUTES)) {
                p.destroyForcibly();
                fail(clazz, new RuntimeException("Sample class exceeded maximum runtime"));
            }

            String out = outStr.lines().collect(Collectors.joining());
            String err = errStr.lines().collect(Collectors.joining());

            return new Result(clazz.name, out, err);
        }
    }

    default boolean validateExceptions(Result result, Class... allowedExceptions) {
        List<String> diff = Stream.of(result.err.split("\n"))
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .filter(l -> Arrays.stream(allowedExceptions)
                        .map(Class::getCanonicalName)
                        .noneMatch(l::contains))
                .collect(Collectors.toList());

        diff.forEach(l -> {
            logger.error(result.err);
            logger.error("Execution of " + result.className + " failed - difference in line: " + l);
        });

        return diff.isEmpty();
    }
}
