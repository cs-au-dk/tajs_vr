package dk.brics.tajs.solver.refinement;

import forwards_backwards_api.Formula;
import forwards_backwards_api.Query;
import forwards_backwards_api.RefutationResult;

import static dk.brics.tajs.util.Collections.newMap;

public class RefuteQuery<FormulaType extends Formula> implements Query<RefutationResult> {

    private FormulaType formula;

    public RefuteQuery(FormulaType formula) {
        this.formula = formula;
    }

    @Override
    public RefutationResult getSoundDefault() {
        return new RefutationResult(false, newMap());
    }

    public FormulaType getFormula() {
        return formula;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefuteQuery<?> that = (RefuteQuery<?>) o;

        return formula != null ? formula.equals(that.formula) : that.formula == null;

    }

    @Override
    public int hashCode() {
        return formula != null ? formula.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "RefuteQuery{" +
                "formula=" + formula +
                '}';
    }
}
