package org.aksw.databugger.tests;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import org.aksw.databugger.Utils;
import org.aksw.databugger.sources.Source;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Dimitris Kontokostas
 * Various utility test functions for tests
 * Created: 9/24/13 10:59 AM
 */
public class TestUtil {
    private static final Logger log = LoggerFactory.getLogger(TestUtil.class);

    public static List<TestAutoGenerator> instantiateTestGeneratorsFromModel(QueryExecutionFactory queryFactory) {
        List<TestAutoGenerator> autoGenerators = new ArrayList<TestAutoGenerator>();

        String sparqlSelect =  Utils.getAllPrefixes() +
                        " SELECT ?generator ?desc ?query ?patternID WHERE { " +
                        " ?generator a tddo:TestGenerator ; " +
                        "  dcterms:description ?desc ; " +
                        "  tddo:generatorSPARQL ?query ; " +
                        "  tddo:basedOnPattern ?sparqlPattern . " +
                        " ?sparqlPattern dcterms:identifier ?patternID ." +
                        "} ";

        QueryExecution qe = queryFactory.createQueryExecution(sparqlSelect);
        ResultSet results = qe.execSelect();

        while (results.hasNext()) {
            QuerySolution qs = results.next();

            String generator = qs.get("generator").toString();
            String description = qs.get("desc").toString();
            String query = qs.get("query").toString();
            String patternID = qs.get("patternID").toString();

            TestAutoGenerator tag = new TestAutoGenerator(generator, description, query,patternID);
            if (tag.isValid())
                autoGenerators.add(tag);
            else {
                log.error("AutoGenerator not valid: " + tag.getURI());
                System.exit(-1);
            }
        }
        qe.close();

        return autoGenerators;

    }

    public static List<UnitTest> isntantiateTestsFromAG(List<TestAutoGenerator> autoGenerators, Source source) {
        List<UnitTest> tests = new ArrayList<UnitTest>();

        for (TestAutoGenerator tag: autoGenerators ) {
            tests.addAll( tag.generate(source));
        }

        return tests;

    }

    public static void writeTestsToFile(List<UnitTest> tests, PrefixMapping prefixes, String filename) {
        Model model = ModelFactory.createDefaultModel();
        for (UnitTest t: tests)
            t.saveTestToModel(model);
        try {
            File f = new File(filename);
            f.getParentFile().mkdirs();

            model.setNsPrefixes(prefixes);
            model.write(new FileOutputStream(filename),"TURTLE");
        } catch (Exception e) {
            log.error("Cannot write tests to file: " + filename);
        }
    }
}
