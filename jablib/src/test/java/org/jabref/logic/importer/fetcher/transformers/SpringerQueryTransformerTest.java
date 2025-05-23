package org.jabref.logic.importer.fetcher.transformers;

import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringerQueryTransformerTest extends InfixTransformerTest<SpringerQueryTransformer> {

    @Override
    public String getAuthorPrefix() {
        return "name:";
    }

    @Override
    public SpringerQueryTransformer getTransformer() {
        return new SpringerQueryTransformer();
    }

    @Override
    public String getUnFieldedPrefix() {
        return "";
    }

    @Override
    public String getJournalPrefix() {
        return "journal:";
    }

    @Override
    public String getTitlePrefix() {
        return "title:";
    }

    @Override
    @Test
    public void convertYearField() throws QueryNodeParseException {
        String queryString = "year:2015";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);

        Optional<String> expected = Optional.of("date:2015*");
        assertEquals(expected, searchQuery);
    }

    @Override
    @Test
    public void convertYearRangeField() throws QueryNodeParseException {
        String queryString = "year-range:2012-2015";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(queryString, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        Optional<String> searchQuery = getTransformer().transformLuceneQuery(luceneQuery);

        Optional<String> expected = Optional.of("date:2012* OR date:2013* OR date:2014* OR date:2015*");
        assertEquals(expected, searchQuery);
    }
}
