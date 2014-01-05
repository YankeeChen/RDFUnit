package org.aksw.databugger.tests;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.core.Var;
import org.aksw.databugger.Utils.DatabuggerUtils;
import org.aksw.databugger.Utils.TestUtils;
import org.aksw.databugger.enums.TestGenerationType;
import org.aksw.databugger.patterns.Pattern;
import org.aksw.databugger.patterns.PatternParameter;
import org.aksw.databugger.sources.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Dimitris Kontokostas
 * Takes a sparqlPattern and a SPARQL query and based on the data
 * returned from that query we generate test cases
 * Created: 9/20/13 2:48 PM
 */
public class TestAutoGenerator {
    private static final Logger log = LoggerFactory.getLogger(TestAutoGenerator.class);

    private final String URI;
    private final String description;
    private final String query;
    private final Pattern pattern;

    public TestAutoGenerator(String uri, String description, String query, Pattern pattern) {
        this.URI = uri;
        this.description = description;
        this.query = query;
        this.pattern = pattern;
    }

    /**
     * Checks if the the generator is valid (provides correct parameters)
     */
    public boolean isValid() {
        Query q;
        if (pattern == null) {
            log.error(getURI() + " : Pattern " + getPattern() + " does not exist");
            return false;
        }
        try {
            q = QueryFactory.create(DatabuggerUtils.getAllPrefixes() + getQuery());
        } catch (Exception e) {
            log.error(getURI() + " Cannot parse query");
            e.printStackTrace();
            return false;
        }

        List<Var> sv = q.getProjectVars();
        if (sv.size() != pattern.getParameters().size() + 2) {   // +2 is about ?MESSAGE & LOGLEVEL
            log.error(getURI() + " Select variables are different than Pattern parameters");
            return false;
        }


        return true;
    }

    public List<TestCase> generate(Source source) {
        List<TestCase> tests = new ArrayList<TestCase>();

        Query q = QueryFactory.create(DatabuggerUtils.getAllPrefixes() + getQuery());
        QueryExecution qe = source.getExecutionFactory().createQueryExecution(q);
        ResultSet rs = qe.execSelect();

        while (rs.hasNext()) {
            QuerySolution row = rs.next();

            List<Binding> bindings = new ArrayList<Binding>();
            List<String> references = new ArrayList<String>();

            for (PatternParameter p : pattern.getParameters()) {
                if (row.contains(p.getId())) {
                    RDFNode n = row.get(p.getId());
                    bindings.add(new Binding(p, n));
                    if (n.isResource()) {
                        references.add(n.toString().trim().replace(" ", ""));
                    }
                } else {
                    //TODO exception
                    break;
                }
            }
            String message = "", logLevel = "";
            if (row.contains("MESSAGE"))
                message = row.get("MESSAGE").toString();
            if (row.contains("LOGLEVEL"))
                logLevel = row.get("LOGLEVEL").toString();

            try {
                tests.add(new PatternBasedTestCase(
                        TestUtils.generateTestURI(source.getPrefix(), getPattern(), bindings, URI),
                        new TestCaseAnnotation(
                                TestGenerationType.AutoGenerated,
                                this.getURI(),
                                source.getSourceType(),
                                source.getUri(),
                                references),
                        pattern,
                        bindings, message, logLevel));
            } catch (Exception e) {

            }

        }
        return tests;
    }

    public String getURI() {
        return URI;
    }

    public String getDescription() {
        return description;
    }

    public String getQuery() {
        return query;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
