package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jabref.logic.importer.ParseException;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.support.BibEntryAssert;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MarcXmlParserTest {

    private static final String FILE_ENDING = ".xml";

    private static Stream<String> fileNames() throws IOException {
        Predicate<String> fileName = name -> name.startsWith("MarcXMLParserTest") && name.endsWith(FILE_ENDING);
        return ImporterTestEngine.getTestFiles(fileName).stream();
    }

    private void doTest(String xmlName, String bibName) throws IOException, ParseException {
        try (InputStream is = MarcXmlParserTest.class.getResourceAsStream(xmlName)) {
            MarcXmlParser parser = new MarcXmlParser();
            List<BibEntry> entries = parser.parseEntries(is);
            assertNotNull(entries);
            BibEntryAssert.assertEquals(MarcXmlParserTest.class, bibName, entries.getFirst());
        }
    }

    @ParameterizedTest
    @MethodSource("fileNames")
    void importEntries(String fileName) throws IOException, ParseException {
        String bibName = FileUtil.getBaseName(fileName) + ".bib";
        doTest(fileName, bibName);
    }
}
