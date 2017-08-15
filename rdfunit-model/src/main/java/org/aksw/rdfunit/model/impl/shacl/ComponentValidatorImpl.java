package org.aksw.rdfunit.model.impl.shacl;

import com.google.common.collect.ImmutableSet;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.aksw.rdfunit.enums.ComponentValidatorType;
import org.aksw.rdfunit.enums.ShapeType;
import org.aksw.rdfunit.model.interfaces.shacl.ComponentParameter;
import org.aksw.rdfunit.model.interfaces.shacl.ComponentValidator;
import org.aksw.rdfunit.model.interfaces.shacl.PrefixDeclaration;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.aksw.rdfunit.model.helper.NodeFormatter.formatNode;

@ToString
@EqualsAndHashCode(exclude={"element"})
@Builder
@Slf4j
public class ComponentValidatorImpl implements ComponentValidator {
    @Getter @NonNull private final Resource element;
    @Getter private final Literal message;
    @Getter @NonNull private final String sparqlQuery;
    @Getter @NonNull private final ComponentValidatorType type;
    @Getter @NonNull @Singular private final ImmutableSet<PrefixDeclaration> prefixDeclarations;
    @Getter private final String filter;

    private static final List<ComponentValidatorType> propertyValidators = Arrays.asList(ComponentValidatorType.ASK_VALIDATOR, ComponentValidatorType.PROPERTY_VALIDATOR);
    private static final List<ComponentValidatorType> nodeValidators = Arrays.asList(ComponentValidatorType.ASK_VALIDATOR, ComponentValidatorType.NODE_VALIDATOR);

    @Override
    public Optional<Literal> getDefaultMessage() {
        return Optional.ofNullable(message);
    }

    @Override
    public boolean filterAppliesForBindings(ShapeType shapeType, Map<ComponentParameter, RDFNode> bindings) {
        if (shapeType.equals(ShapeType.PROPERTY_SHAPE)  && !propertyValidators.contains(type)) {
            return false;
        }
        if (shapeType.equals(ShapeType.NODE_SHAPE)  && !nodeValidators.contains(type)) {
            return false;
        }
        if (filter == null || filter.isEmpty()) {
            return true;
        }

        return canBind(filter, bindings);
    }


    private boolean canBind(String filter, Map<ComponentParameter, RDFNode> bindings) {
        String filterQ = filter;
        for ( Map.Entry<ComponentParameter, RDFNode> e: bindings.entrySet()) {
            filterQ = filterQ.replace("$" + e.getKey().getParameterName(), formatNode(e.getValue()));
        }
        boolean canBind = checkFilter(filterQ);
        return canBind;
    }

    private boolean checkFilter(String askQuery) {
        Model m = ModelFactory.createDefaultModel();
        try (QueryExecution qex = org.apache.jena.query.QueryExecutionFactory.create(QueryFactory.create(askQuery), m)) {
            return qex.execAsk();
        } catch (Exception e) {
            log.error("Error evaluating filter query: {}", askQuery, e);
            return false;
        }
    }



}
