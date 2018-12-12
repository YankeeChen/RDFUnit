package org.aksw.rdfunit.model.shacl;

import org.aksw.rdfunit.RDFUnit;
import org.aksw.rdfunit.io.reader.RdfReaderException;
import org.aksw.rdfunit.io.reader.RdfReaderFactory;
import org.aksw.rdfunit.model.interfaces.GenericTestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dimitris Kontokostas
 * @since 14/2/2016 5:32 μμ
 */
public class ShaclModelTest {
    private static final String shapeResource = "/org/aksw/rdfunit/shacl/sampleShape.ttl" ;

    @Before
    public void setUp() {
        // Needed to resolve the patterns
        RDFUnit
            .createWithAllGenerators()
            .init();
    }

    @Test
    public void testRead() throws RdfReaderException {

        ShaclModel shaclModel = new ShaclModel(RdfReaderFactory.createResourceReader(shapeResource).read());
        assertThat(shaclModel.getShapes())
                .isNotEmpty();

        Set<GenericTestCase> tests = shaclModel.generateTestCases();
        assertThat(tests)
                .isNotEmpty();
    }
}
