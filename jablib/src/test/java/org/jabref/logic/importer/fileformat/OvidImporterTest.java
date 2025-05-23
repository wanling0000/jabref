package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.BibEntryAssert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OvidImporterTest {

    private static final String FILE_ENDING = ".txt";
    private OvidImporter importer = new OvidImporter();

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("OvidImporterTest")
                && !name.contains("Invalid")
                && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private static Stream<String> invalidFileNames() throws IOException {
        Predicate<String> fileName = name -> !name.contains("OvidImporterTest");
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void isRecognizedFormatAccept(String fileName) throws IOException {
        ImporterTestEngine.testIsRecognizedFormat(importer, fileName);
    }

    @ParameterizedTest
    @MethodSource("invalidFileNames")
    void isRecognizedFormatRejected(String fileName) throws IOException {
        ImporterTestEngine.testIsNotRecognizedFormat(importer, fileName);
    }

    @Test
    void importEmpty() throws IOException, URISyntaxException {
        Path file = Path.of(OvidImporter.class.getResource("Empty.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(List.of(), entries);
    }

    @Test
    void importEntries1() throws IOException, URISyntaxException {
        Path file = Path.of(OvidImporter.class.getResource("OvidImporterTest1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(5, entries.size());

        BibEntry entry = entries.getFirst();
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertEquals(Optional.of("Mustermann and Musterfrau"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Short abstract"), entry.getField(StandardField.ABSTRACT));
        assertEquals(Optional.of("Musterbuch"), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("Einleitung"), entry.getField(new UnknownField("chaptertitle")));

        entry = entries.get(1);
        assertEquals(StandardEntryType.InProceedings, entry.getType());
        assertEquals(Optional.of("Max"), entry.getField(StandardField.EDITOR));
        assertEquals(Optional.of("Max the Editor"), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("Very Long Title"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("28"), entry.getField(StandardField.VOLUME));
        assertEquals(Optional.of("2"), entry.getField(StandardField.ISSUE));
        assertEquals(Optional.of("2015"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("103--106"), entry.getField(StandardField.PAGES));

        entry = entries.get(2);
        assertEquals(StandardEntryType.InCollection, entry.getType());
        assertEquals(Optional.of("Max"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("Test"), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("Very Long Title"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("28"), entry.getField(StandardField.VOLUME));
        assertEquals(Optional.of("2"), entry.getField(StandardField.ISSUE));
        assertEquals(Optional.of("April"), entry.getField(StandardField.MONTH));
        assertEquals(Optional.of("2015"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("103--106"), entry.getField(StandardField.PAGES));

        entry = entries.get(3);
        assertEquals(StandardEntryType.Book, entry.getType());
        assertEquals(Optional.of("Max"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("2015"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("Editor"), entry.getField(StandardField.EDITOR));
        assertEquals(Optional.of("Very Long Title"), entry.getField(StandardField.BOOKTITLE));
        assertEquals(Optional.of("103--106"), entry.getField(StandardField.PAGES));
        assertEquals(Optional.of("Address"), entry.getField(StandardField.ADDRESS));
        assertEquals(Optional.of("Publisher"), entry.getField(StandardField.PUBLISHER));

        entry = entries.get(4);
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("2014"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("58"), entry.getField(StandardField.PAGES));
        assertEquals(Optional.of("Test"), entry.getField(StandardField.ADDRESS));
        assertEquals(Optional.empty(), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("TestPublisher"), entry.getField(StandardField.PUBLISHER));
    }

    @Test
    void importEntries2() throws IOException, URISyntaxException {
        Path file = Path.of(OvidImporter.class.getResource("OvidImporterTest2Invalid.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(List.of(), entries);
    }

    @Test
    void importSingleEntries() throws IOException, URISyntaxException {

        for (int n = 3; n <= 7; n++) {
            Path file = Path.of(OvidImporter.class.getResource("OvidImporterTest" + n + ".txt").toURI());
            try (InputStream nis = OvidImporter.class.getResourceAsStream("OvidImporterTestBib" + n + ".bib")) {
                List<BibEntry> entries = importer.importDatabase(file).getDatabase()
                                                 .getEntries();
                assertNotNull(entries);
                assertEquals(1, entries.size());
                BibEntryAssert.assertEquals(nis, entries.getFirst());
            }
        }
    }
}
