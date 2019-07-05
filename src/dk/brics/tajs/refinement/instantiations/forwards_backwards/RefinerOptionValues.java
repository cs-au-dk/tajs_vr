package dk.brics.tajs.refinement.instantiations.forwards_backwards;

import dk.brics.tajs.options.ExperimentalOptions;

import java.io.Serializable;

public class RefinerOptionValues implements ExperimentalOptions.ExperimentalOption, Serializable {

    /**
     * If set, enable refinement at dynamic property write operations.
     */
    private boolean writePropertyRefineEnabled = false;

    // Algorithm options
    private boolean contextInsensitiveRefinements;
    private boolean disallowRefineToBottom = false;

    /**
     * If set, we do not refine concrete values or single objects
     */
    private boolean doNotRefinePreciseValues = false;

    /**
     * If set, info about each refinement is printed
     */
    private boolean displayQueryDebuggingInformationEnabled;

    /**
     * If set, functions in v, at o[p] = v that contains an imprecise closure variable are
     * redefined with a heapContext that specializes to a partitioned part of the closure variable.
     */
    private boolean specializeImpreciseClosureVariablesWithOnlyOneWrite;

    /**
     * If set, return sound default, when trying to refine the property name and the target value is undefined
     */
    private boolean doNotRefinePropertyNameForUndefined;

    /**
     * If set, use sound default for queries where queries from the program location has failed more than 5 times before
     */
    private boolean useSoundDefaultForFailedQueriesLocations;

    // Refinement options

    public boolean isWritePropertyRefineEnabled() {
        return writePropertyRefineEnabled;
    }

    public void setWritePropertyRefineEnabled(boolean enable) {
        this.writePropertyRefineEnabled = enable;
    }

    public boolean isDisableRefineToBottom() {
        return disallowRefineToBottom;
    }

    public void setDisallowRefineToBottom(boolean disallowRefineToBottom) {
        this.disallowRefineToBottom = disallowRefineToBottom;
    }

    public boolean isDoNotRefinePreciseValues() {
        return doNotRefinePreciseValues;
    }

    public void setDoNotRefinePreciseValues(boolean doNotRefinePreciseValues) {
        this.doNotRefinePreciseValues = doNotRefinePreciseValues;
    }

    public void reset() { // TODO: never invoked + check that all options are included here
        writePropertyRefineEnabled = false;
    }

    private String boolToString(boolean b){
        return b ? "1" : "0";
    }

    public String toString() { // TODO: check that all options are included here
        return boolToString(writePropertyRefineEnabled);
    }

    public boolean isContextInsensitiveRefinementsEnabled() {
        return contextInsensitiveRefinements;
    }

    public void setContextInsensitiveRefinements(boolean contextInsensitiveRefinements) {
        this.contextInsensitiveRefinements = contextInsensitiveRefinements;
    }

    public boolean isDisplayQueryDebuggingInformationEnabled() {
        return displayQueryDebuggingInformationEnabled;
    }

    public void setDisplayQueryDebuggingInformationEnabled(boolean displayQueryDebuggingInformationEnabled) {
        this.displayQueryDebuggingInformationEnabled = displayQueryDebuggingInformationEnabled;
    }

    public boolean isSpecializeImpreciseClosureVariablesWithOnlyOneWrite() {
        return specializeImpreciseClosureVariablesWithOnlyOneWrite;
    }

    public void setSpecializeImpreciseClosureVariablesWithOnlyOneWrite(boolean specializeImpreciseClosureVariablesWithOnlyOneWrite) {
        this.specializeImpreciseClosureVariablesWithOnlyOneWrite = specializeImpreciseClosureVariablesWithOnlyOneWrite;
    }

    public boolean isDoNotRefinePropertyNameForUndefined() {
        return doNotRefinePropertyNameForUndefined;
    }

    public void setDoNotRefinePropertyNameForUndefined(boolean doNotRefinePropertyNameForUndefined) {
        this.doNotRefinePropertyNameForUndefined = doNotRefinePropertyNameForUndefined;
    }

    public boolean isUseSoundDefaultForFailedQueriesLocations() {
        return useSoundDefaultForFailedQueriesLocations;
    }

    public void setUseSoundDefaultForFailedQueriesLocations(boolean useSoundDefaultForFailedQueriesLocations) {
        this.useSoundDefaultForFailedQueriesLocations = useSoundDefaultForFailedQueriesLocations;
    }
}
