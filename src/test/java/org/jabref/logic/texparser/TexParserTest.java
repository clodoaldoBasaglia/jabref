package org.jabref.logic.texparser;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.texparser.TexBibEntriesResolverResult;
import org.jabref.model.texparser.TexParserResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TexParserTest {
    private final static String DARWIN = "Darwin1888";
    private final static String EINSTEIN = "Einstein1920";
    private final static String NEWTON = "Newton1999";
    private final static String EINSTEIN_A = "Einstein1920a";
    private final static String EINSTEIN_B = "Einstein1920b";
    private final static String EINSTEIN_C = "Einstein1920c";
    private final static String EINSTEIN_21 = "Einstein1921";
    private final static String UNRESOLVED = "UnresolvedKey";
    private final static String UNKNOWN = "UnknownKey";

    private static BibDatabase database;
    private static BibDatabase database2;

    @BeforeEach
    private void setUp() {
        database = new BibDatabase();
        database2 = new BibDatabase();

        BibEntry darwin = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", DARWIN)
                .withField("title", "The descent of man, and selection in relation to sex")
                .withField("publisher", "J. Murray")
                .withField("year", "1888")
                .withField("author", "Darwin, Charles");
        database.insertEntry(darwin);

        BibEntry einstein = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", EINSTEIN)
                .withField("title", "Relativity: The special and general theory")
                .withField("publisher", "Penguin")
                .withField("year", "1920")
                .withField("author", "Einstein, Albert");
        database.insertEntry(einstein);
        database2.insertEntry(einstein);

        BibEntry newton = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", NEWTON)
                .withField("title", "The Principia: mathematical principles of natural philosophy")
                .withField("publisher", "Univ of California Press")
                .withField("year", "1999")
                .withField("author", "Newton, Isaac");
        database.insertEntry(newton);

        BibEntry einsteinA = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", EINSTEIN_A)
                .withField("crossref", "Einstein1920")
                .withField("pages", "22--23");
        database.insertEntry(einsteinA);

        BibEntry einsteinB = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", EINSTEIN_B)
                .withField("crossref", "Einstein1921")
                .withField("pages", "22--23");
        database.insertEntry(einsteinB);

        BibEntry einsteinC = new BibEntry(BibtexEntryTypes.BOOK)
                .withField("bibtexkey", EINSTEIN_C)
                .withField("crossref", "Einstein1920")
                .withField("pages", "25--33");
        database.insertEntry(einsteinC);
    }

    @Test
    public void testSameFileDifferentDatabases() throws URISyntaxException {
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser().parse(texFile);
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().add(texFile);
        expectedParserResult.addKey(EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}");
        expectedParserResult.addKey(DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.");
        expectedParserResult.addKey(EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        expectedParserResult.addKey(DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");

        TexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database).resolveKeys(parserResult);
        TexBibEntriesResolverResult expectedCrossingResult = new TexBibEntriesResolverResult(expectedParserResult);

        expectedCrossingResult.insertEntry(database, DARWIN);
        expectedCrossingResult.insertEntry(database, EINSTEIN);

        assertEquals(expectedCrossingResult, crossingResult);

        TexBibEntriesResolverResult crossingResult2 = new TexBibEntriesResolver(database2).resolveKeys(parserResult);
        TexBibEntriesResolverResult expectedCrossingResult2 = new TexBibEntriesResolverResult(expectedParserResult);

        expectedCrossingResult2.insertEntry(database2, EINSTEIN);
        expectedCrossingResult2.addUnresolvedKey(DARWIN);

        assertEquals(expectedCrossingResult2, crossingResult2);
    }

    @Test
    public void testTwoFilesDifferentDatabases() throws URISyntaxException {
        Path texFile = Paths.get(TexParserTest.class.getResource("paper.tex").toURI());
        Path texFile2 = Paths.get(TexParserTest.class.getResource("paper2.tex").toURI());

        TexParserResult parserResult = new DefaultTexParser().parse(Arrays.asList(texFile, texFile2));
        TexParserResult expectedParserResult = new TexParserResult();

        expectedParserResult.getFileList().addAll(Arrays.asList(texFile, texFile2));
        expectedParserResult.addKey(EINSTEIN, texFile, 4, 0, 19, "\\cite{Einstein1920}");
        expectedParserResult.addKey(DARWIN, texFile, 5, 0, 17, "\\cite{Darwin1888}.");
        expectedParserResult.addKey(EINSTEIN, texFile, 6, 14, 33, "Einstein said \\cite{Einstein1920} that lorem impsum, consectetur adipiscing elit.");
        expectedParserResult.addKey(DARWIN, texFile, 7, 67, 84, "Nunc ultricies leo nec libero rhoncus, eu vehicula enim efficitur. \\cite{Darwin1888}");
        expectedParserResult.addKey(DARWIN, texFile2, 4, 48, 65, "This is some content trying to cite a bib file: \\cite{Darwin1888}");
        expectedParserResult.addKey(EINSTEIN, texFile2, 5, 48, 67, "This is some content trying to cite a bib file: \\cite{Einstein1920}");
        expectedParserResult.addKey(NEWTON, texFile2, 6, 48, 65, "This is some content trying to cite a bib file: \\cite{Newton1999}");

        TexBibEntriesResolverResult crossingResult = new TexBibEntriesResolver(database).resolveKeys(parserResult);
        TexBibEntriesResolverResult expectedCrossingResult = new TexBibEntriesResolverResult(expectedParserResult);

        expectedCrossingResult.insertEntry(database, DARWIN);
        expectedCrossingResult.insertEntry(database, EINSTEIN);
        expectedCrossingResult.insertEntry(database, NEWTON);

        assertEquals(expectedCrossingResult, crossingResult);

        TexBibEntriesResolverResult crossingResult2 = new TexBibEntriesResolver(database2).resolveKeys(parserResult);
        TexBibEntriesResolverResult expectedCrossingResult2 = new TexBibEntriesResolverResult(expectedParserResult);

        expectedCrossingResult2.insertEntry(database2, EINSTEIN);
        expectedCrossingResult2.addUnresolvedKey(DARWIN);
        expectedCrossingResult2.addUnresolvedKey(NEWTON);

        assertEquals(expectedCrossingResult2, crossingResult2);
    }
}
