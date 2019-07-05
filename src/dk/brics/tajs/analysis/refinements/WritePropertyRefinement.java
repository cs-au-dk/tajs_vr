package dk.brics.tajs.analysis.refinements;

import dk.brics.tajs.flowgraph.jsnodes.WritePropertyNode;
import dk.brics.tajs.lattice.ObjectLabel;
import dk.brics.tajs.lattice.State;
import dk.brics.tajs.lattice.UnknownValueResolver;
import dk.brics.tajs.lattice.Value;
import dk.brics.tajs.refinement.instantiations.forwards_backwards.RefinerOptions;
import dk.brics.tajs.solver.refinement.QueryManager;
import dk.brics.tajs.solver.refinement.RefineQuery;
import dk.brics.tajs.util.AnalysisException;
import forwards_backwards_api.Formula;
import forwards_backwards_api.InstructionComponent;
import forwards_backwards_api.ProgramLocation;
import forwards_backwards_api.memory.RegisterWithRefinementGoal;

import java.util.Collection;

import static dk.brics.tajs.util.Collections.newSet;
import static dk.brics.tajs.util.Collections.singleton;

public class WritePropertyRefinement {

    /**
     * Attempts to refine the property name for the given write operation, relative to the given base or value (if not null).
     * Should only be invoked if WritePropertyNode refinement is enabled.
     */
    public static Collection<Value> getPropertyName(Value defaultres, Value base, Value val, WritePropertyNode n, State state) {
        if (base != null && val != null) {
            throw new AnalysisException("expected base != null && val != null");
        }
        if (RefinerOptions.get().isDoNotRefinePropertyNameForUndefined() && val != null && val.restrictToNotUndef().isNone()) {
            return singleton(defaultres);
        }
        QueryManager.MultiQueryManager<Formula> qm = QueryManager.MultiQueryManager.getInstance();
        Formula constraint;
        if (base != null) {
            constraint = qm.getRefiner().mkEqualityConstraint(new RegisterWithRefinementGoal(n.getBaseRegister(), InstructionComponent.BASE), base);
        } else if (val != null) {
            constraint = qm.getRefiner().mkEqualityConstraint(new RegisterWithRefinementGoal(n.getValueRegister(), InstructionComponent.TARGET), val);
        } else {
            constraint = qm.getRefiner().mkTrueConstraint();
        }
        RefineQuery<Formula> query = new RefineQuery<>(new RegisterWithRefinementGoal(n.getPropertyRegister(), InstructionComponent.PROPERTY), constraint, UnknownValueResolver.getRealValue(defaultres, state));

        Collection<Value> valueRefineResult = qm.getRefinerManager().getQueryResult(new ProgramLocation(n, state.getContext()), query);
        return partitionValues(valueRefineResult, state);
    }

    /**
     * Partitions the given values.
     */
    public static Collection<Value> partitionValues(Collection<Value> vals, State state) {
        if (vals == null)
            return vals;
        Collection<Value> res = newSet();
        for (Value val : vals) {
            val = UnknownValueResolver.getRealValue(val, state);
            for (ObjectLabel objlabel : val.getObjectLabels())
                res.add(Value.makeObject(objlabel));
            if (val.isMaybePrimitive()) {
                Value primitiveValue = val.restrictToNotGetterSetter().restrictToNotObject();
                if (!primitiveValue.restrictToStr().isNone()) {
                    res.add(primitiveValue.restrictToStr());
                }
                if (!primitiveValue.restrictToNum().isNone()) {
                    res.add(primitiveValue.restrictToNum());
                }
                if (!primitiveValue.restrictToBool().isNone()) {
                    res.add(primitiveValue.restrictToBool());
                }
                if (primitiveValue.isMaybeNull()) {
                    res.add(Value.makeNull());
                }
                if (primitiveValue.isMaybeUndef()) {
                    res.add(Value.makeUndef());
                }
            }
            if (val.isMaybeGetterOrSetter()) {
                res.add(val.restrictToGetterSetter());
            }
        }
        return res;
    }
}
