/*
 * Copyright 2009-2019 Aarhus University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.brics.tajs.analysis;

import dk.brics.tajs.analysis.dom.DOMObjects;
import dk.brics.tajs.analysis.js.UserFunctionCalls;
import dk.brics.tajs.flowgraph.AbstractNode;
import dk.brics.tajs.flowgraph.BasicBlock;
import dk.brics.tajs.refinement.instantiations.forwards_backwards.RefinerOptions;
import dk.brics.tajs.lattice.Bool;
import dk.brics.tajs.lattice.ExecutionContext;
import dk.brics.tajs.lattice.FreeVariablePartitioning;
import dk.brics.tajs.lattice.Obj;
import dk.brics.tajs.lattice.ObjectLabel;
import dk.brics.tajs.lattice.ObjectProperty;
import dk.brics.tajs.lattice.PKey;
import dk.brics.tajs.lattice.PKey.StringPKey;
import dk.brics.tajs.lattice.PKey.SymbolPKey;
import dk.brics.tajs.lattice.PKeys;
import dk.brics.tajs.lattice.Property;
import dk.brics.tajs.lattice.PropertyReadSpecialization;
import dk.brics.tajs.lattice.ScopeChain;
import dk.brics.tajs.lattice.State;
import dk.brics.tajs.lattice.Str;
import dk.brics.tajs.lattice.UnknownValueResolver;
import dk.brics.tajs.lattice.Value;
import dk.brics.tajs.options.Options;
import dk.brics.tajs.solver.Message;
import dk.brics.tajs.util.AnalysisException;
import dk.brics.tajs.util.Pair;
import dk.brics.tajs.util.Strings;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static dk.brics.tajs.analysis.InitialStateBuilder.GLOBAL;
import static dk.brics.tajs.lattice.Property.Kind.DEFAULT_NUMERIC;
import static dk.brics.tajs.lattice.Property.Kind.ORDINARY;
import static dk.brics.tajs.util.Collections.newList;
import static dk.brics.tajs.util.Collections.newSet;
import static dk.brics.tajs.util.Collections.singleton;

/**
 * Operations for accessing object properties and variables in abstract states.
 */
public class PropVarOperations {

    private static Logger log = Logger.getLogger(PropVarOperations.class);

    private final Unsoundness unsoundness;

    private Solver.SolverInterface c;

    /**
     * Constructs a new PropVarOperations object.
     */
    public PropVarOperations(Unsoundness unsoundness) {
        this.unsoundness = unsoundness;
    }

    /**
     * Initializes the connection to the solver.
     */
    public void setSolverInterface(Solver.SolverInterface c) {
        this.c = c;
    }

    /**
     * 8.6.2.1 [[Get]]
     * Returns the value of the given property in the given objects.
     * The internal prototype chains are used.
     * Absent is converted to undefined. Attributes are set to bottom.
     */
    public Value readPropertyValue(Collection<ObjectLabel> objlabels, String propertyname) {
        return readPropertyValue(objlabels, propertyname, null);
    }

    /**
     * 8.6.2.1 [[Get]]
     * Returns the value of the given property in the given objects.
     * The internal prototype chains are used.
     * Absent is converted to undefined. Attributes are set to bottom.
     *
     * @param collect if non-null, collect the objects where the property may be found
     */
    public Value readPropertyValue(Collection<ObjectLabel> objlabels, String propertyname, Set<ObjectLabel> collect) {
        return readPropertyValue(objlabels, Value.makeTemporaryStr(propertyname), collect);
    }

    /**
     * 8.6.2.1 [[Get]]
     * Returns the value of the given property in the given objects.
     * The internal prototype chains are used.
     * Absent is converted to undefined. Attributes are set to bottom.
     */
    public Value readPropertyValue(Collection<ObjectLabel> objlabels, PKeys propertystr) {
        return readPropertyValue(objlabels, propertystr, null);
    }

    /**
     * 8.6.2.1 [[Get]]
     * Returns the value of the given property in the given objects.
     * The internal prototype chains are used.
     * Absent is converted to undefined. Attributes are set to bottom.
     *
     * @param collect if non-null, collect the objects where the property may be found
     */
    public Value readPropertyValue(Collection<ObjectLabel> objlabels, PKeys propertystr, Set<ObjectLabel> collect) {
        Value v = readPropertyRaw(objlabels, propertystr, false, false, collect);
        if (v.isMaybeAbsent())
            v = v.restrictToNotAbsent().joinUndef();
        v = v.setBottomPropertyData();
        if (log.isDebugEnabled())
            log.debug("readPropertyValue(" + objlabels + "," + propertystr + ") = " + v);
        if (RefinerOptions.get().isSpecializeImpreciseClosureVariablesWithOnlyOneWrite()) {
            v = specializeValueAtRead(v, Value.canonicalize((Value) propertystr)); //TODO: casting to Value
        }
        return v;
    }

    private Value specializeValueAtRead(Value v, Value propName) {
        v = UnknownValueResolver.getRealValue(v, c.getState());
        Value res = v.restrictToNotObject();
        for (ObjectLabel objectLabel : v.getObjectLabels()) {
            PropertyReadSpecialization propertyReadSpecialization = objectLabel.getPropertyReadSpecialization();
            if (objectLabel.getKind() == ObjectLabel.Kind.FUNCTION
                    && propertyReadSpecialization != null
                    && !propertyReadSpecialization.getPropName().isPresent()) {
                PropertyReadSpecialization newPropertyReadSpecialization =
                        new PropertyReadSpecialization(propertyReadSpecialization.getProgramLocation(), propertyReadSpecialization.getMemoryLocation(), Optional.of(propName));
                ObjectLabel specializedObjectLabel = objectLabel.makeSpecialization(newPropertyReadSpecialization);
                c.getState().propagateObj(specializedObjectLabel, c.getState(), objectLabel, true, false);
                res = res.joinObject(specializedObjectLabel);
                c.getState().addSpecialization(c.getState().getGeneralization(objectLabel), specializedObjectLabel);
            } else {
                res = res.joinObject(objectLabel);
            }
        }
        return res;
    }


    /**
     * Returns the value of the given property in the given objects.
     * The internal prototype chains are used.
     * Getters are not called.
     */
    public Value readPropertyWithAttributes(Collection<ObjectLabel> objlabels, String propertyname) {
        return readPropertyWithAttributes(objlabels, Value.makeTemporaryStr(propertyname));
    }

    /**
     * Returns the value of the given property in the given objects, together with the actual objects where the property is read.
     * The internal prototype chains are used.
     * Getters are not called.
     */
    public Pair<Set<ObjectLabel>,Value> readPropertyWithAttributesAndObjs(Collection<ObjectLabel> objlabels, PKey propertyname) {
        Set<ObjectLabel> objs = newSet();
        Value v = readPropertyRaw(objlabels, propertyname.toValue(), false, true, objs);
        if (log.isDebugEnabled())
            log.debug("readPropertyWithAttributesAndObjs(" + objlabels + "," + propertyname + ") = " + v);
        return Pair.make(objs, v);
    }

    /**
     * Returns the value of the given property in the given objects.
     * The internal prototype chains are used.
     * Getters are not called.
     */
    public Value readPropertyWithAttributes(Collection<ObjectLabel> objlabels, PKeys propertystr) {
        Value v = readPropertyRaw(objlabels, propertystr, false, true, null);
        if (log.isDebugEnabled())
            log.debug("readPropertyWithAttributes(" + objlabels + "," + propertystr + ") = " + v);
        return v;
    }

    /**
     * Returns the value of the given property in the given objects.
     * The internal prototype chains are used.
     *
     * @param only_attributes if set, only attributes (incl. pseudo-attributes) are considered
     * @param no_call_getters if set, do not call getters
     * @param collect if non-null, collect the objects where the property may be found
     */
    private Value readPropertyRaw(Collection<ObjectLabel> objlabels, PKeys propertystr, boolean only_attributes, boolean no_call_getters, Set<ObjectLabel> collect) {
        Collection<Value> values = newList();
        Set<ObjectLabel> visited = newSet();
        BasicBlock implicitAfterCall = null;
        for (ObjectLabel base : objlabels) {
            Collection<ObjectLabel> ol = singleton(base);
            while (!ol.isEmpty()) {
                Set<ObjectLabel> ol2 = newSet();
                for (ObjectLabel l : ol)
                    if (!visited.contains(l)) {
                        visited.add(l);
                        Value v;
                        if (l.getKind() == ObjectLabel.Kind.STRING) {
                            // String objects have their own [[GetOwnProperty]], see 15.5.5.2 in the ECMAScript 5 standard.
                            v = readStringPropertyDirect(l, propertystr);
                        } else {
                            v = readPropertyDirect(l, propertystr);
                        }
                        if (collect != null && v.isMaybePresent())
                            collect.add(l);
                        if (!no_call_getters) {
                            if (v.isMaybePresentAccessor())
                                v = UnknownValueResolver.getRealValue(v, c.getState());
                            implicitAfterCall = callGetters(values, singleton(base), v, implicitAfterCall);
                        }
                        // add data values
                        Value v2 = v.restrictToNotAbsent();
                        if (v2.isMaybePresent()) {
                            if (only_attributes)
                                v2 = v2.restrictToAttributes();
                            if (!no_call_getters)
                                v2 = v2.restrictToNotGetterSetter();
                            if (v2.isMaybePresent()) {
                                values.add(v2);
                            }
                        }
                        // if maybe absent, proceed along prototype chain
                        if (v.isMaybeAbsent()) {
                            boolean add_absent = false;
                            if (unsoundness.maySkipPrototypesForPropertyRead(c.getNode(), propertystr, v)) // unsoundly skipping the prototype
                                add_absent = true;
                            else {
                                Value proto = UnknownValueResolver.getInternalPrototype(l, c.getState(), false);
                                ol2.addAll(proto.getObjectLabels());
                                if (proto.isMaybeAbsent() || proto.isMaybeNull()) // reached end of prototype chain
                                    add_absent = true;
                            }
                            if (add_absent)
                                values.add(Value.makeAbsent());
                        }
                    }
                ol = ol2;
            }
        }
        return UserFunctionCalls.implicitUserFunctionReturn(values, !values.isEmpty() || no_call_getters, implicitAfterCall, c);
    }

    /**
     * Calls the getters in v.
     * @param values if the absent accessor function is among the getters, then undefined is added to 'values'
     * @param base the base objects holding the getters
     * @param v the value containing the getters
     * @param implicitAfterCall existing after-call node, or null if none
     * @return after-call node, or null if none
     */
    public BasicBlock callGetters(Collection<Value> values, Set<ObjectLabel> base, Value v, BasicBlock implicitAfterCall) {
        for (ObjectLabel obj : v.getGetters()) {
            if (obj == ObjectLabel.absent_accessor_function) {
                values.add(Value.makeUndef()); // TODO: warn about call to dummy getter?
            } else {
                implicitAfterCall = UserFunctionCalls.implicitUserFunctionCall(obj, new FunctionCalls.CallInfo() {
                    @Override
                    public AbstractNode getSourceNode() {
                        return c.getNode();
                    }

                    @Override
                    public AbstractNode getJSSourceNode() {
                        return c.getNode();
                    }

                    @Override
                    public boolean isConstructorCall() {
                        return false;
                    }

                    @Override
                    public Value getFunctionValue() {
                        throw new AnalysisException("Unexpected usage of getFunctionValue");
                    }

                    @Override
                    public Value getThis() {
                        return Value.makeObject(base);
                    }

                    @Override
                    public int getResultRegister() {
                        return AbstractNode.NO_VALUE;
                    }

                    @Override
                    public ExecutionContext getExecutionContext() {
                        return c.getState().getExecutionContext();
                    }

                    @Override
                    public Value getArg(int i) {
                        return Value.makeAbsent();
                    }

                    @Override
                    public int getNumberOfArgs() {
                        return 0;
                    }

                    @Override
                    public Value getUnknownArg() {
                        throw new AnalysisException("Should not be called. Arguments are not unknown.");
                    }

                    @Override
                    public boolean isUnknownNumberOfArgs() {
                        return false;
                    }

                    @Override
                    public boolean assumeFunction() {
                        return false;
                    }

                    @Override
                    public FreeVariablePartitioning getFreeVariablePartitioning() {
                        return null;
                    }
                }, c);
            }
        }
        return implicitAfterCall;
    }

    /**
     * Variant of {@link #readPropertyDirect(ObjectLabel, PKeys)} for string objects.
     * String objects have their own [[GetOwnProperty]], see 15.5.5.2 in the ECMAScript 5 standard.
     */
    private Value readStringPropertyDirect(ObjectLabel str, PKeys propertystr) {
        Value internal_value = UnknownValueResolver.getInternalValue(str, c.getState(), false);
        if (internal_value.isNone()) {
            return Value.makeNone();
        }
        Value character = readStringCharacter(internal_value, propertystr);
        Value length = readStringLength(internal_value, propertystr);
        Value result = Value.makeNone();
        if (character.isMaybePresent()) {
            result = result.join(character);
        }
        if (length.isMaybePresent()) {
            result = result.join(length);
        }
        if (character.isNotPresent() && result.isNotPresent()) {
            result = Value.makeAbsent();
        }
        if (result.isMaybeAbsent()) { //  not "length" or character index
            Value v = readPropertyDirect(str, propertystr);
            if (result.isNotPresent())
                result = v;
            else
                result = UnknownValueResolver.join(result, v, c.getState());
        }
        return result;
    }

    /**
     * Reads the string length, provided that propertystr matches "length" and otherwise returns 'absent'.
     */
    private Value readStringLength(Str str, Str propertystr) {
        Value result = Value.makeNone();
        if (propertystr.isMaybeFuzzyStr()) {
            result = result.joinAbsent();
        }
        if (propertystr.isMaybeStr("length")) {
            if (str.isMaybeSingleStr()) {
                result = result.joinNum(str.getStr().length());
            } else {
                result = result.joinAnyNumUInt();
            }
            result = result.setAttributes(true, true, false);
        }
        return result.isNone() ? Value.makeAbsent() : result;
    }

    /**
     * Reads a char from the string, provided that propertystr is a valid index and otherwise returns 'absent'.
     */
    private Value readStringCharacter(Str str, Str propertystr) {
        boolean isUnknownUIntIndex = propertystr.isMaybeStrUInt();
        boolean isKnownUIntIndex = propertystr.isMaybeSingleStr() && Strings.isArrayIndex(propertystr.getStr());
        if (!isUnknownUIntIndex && !isKnownUIntIndex) {
            // not reading a character
            return Value.makeAbsent();
        }
        if (str.isMaybeFuzzyStr()) {
            // unknown string
            return Value.makeAnyStr().joinAbsent(); // TODO: improve precision: e.g. identifierparts are preserved
        }
        String string = str.getStr();
        if (isKnownUIntIndex) {
            int index = Integer.parseInt(propertystr.getStr());
            if (index >= string.length()) {
                // index out of bounds
                return Value.makeAbsent();
            }
            // known index, known string
            return Value.makeStr(Character.toString(string.charAt(index)));
        }
        Set<Value> chars = newSet();
        for (int i = 0; i < string.length(); i++) {
            chars.add(Value.makeStr(Character.toString(string.charAt(i))));
        }
        // unknown index, known string
        return Value.join(chars).joinAbsent();
    }

    /**
     * Returns the value of the given property in the objects.
     * The internal prototype chains and scope chains are <em>not</em> used.
     */
    public Value readPropertyDirect(Collection<ObjectLabel> objlabels, PKey propertyname) {
        State state = c.getState();
        Collection<Value> values = newList();
        for (ObjectLabel obj : objlabels)
            values.add(UnknownValueResolver.getProperty(obj, propertyname, state, true));
        Value v = UnknownValueResolver.join(values, state);
        if (log.isDebugEnabled())
            log.debug("readPropertyDirect(" + objlabels + "," + propertyname + ") = " + v);
        return v;
    }

    /**
     * Returns the join of the values of the given properties of an object.
     * The internal prototype chains and scope chains are <em>not</em> used.
     */
    public Value readPropertyDirect(ObjectLabel objlabel, PKeys propertystr) {
        State state = c.getState();
        Collection<Value> values = newList();
        // read string property keys
        if (propertystr.isMaybeSingleStr()) {
            values.add(UnknownValueResolver.getProperty(objlabel, StringPKey.make(propertystr.getStr()), state, true));
        } else if (propertystr.getIncludedStrings() != null) {
            propertystr.getIncludedStrings().forEach(n ->
                    values.add(UnknownValueResolver.getProperty(objlabel, StringPKey.make(n), state, true))
            );
        } else if (propertystr.isMaybeFuzzyStr()) {
            if (propertystr.isMaybeStrSomeNumeric())
                values.add(UnknownValueResolver.getDefaultNumericProperty(objlabel, state));
            if (propertystr.isMaybeStrSomeNonNumeric())
                values.add(UnknownValueResolver.getDefaultOtherProperty(objlabel, state));
            // the calls to UnknownValueResolver above have materialized all relevant properties
            for (PKey propertyname : state.getObject(objlabel, false).getPropertyNames())
                if (propertyname instanceof StringPKey && propertyname.isMaybeValue(propertystr)) {
                    if (unsoundness.maySkipSpecificDynamicPropertyRead(c.getNode(), propertyname)) {
                        continue;
                    }
                    values.add(UnknownValueResolver.getProperty(objlabel, propertyname, state, false));
                }
        }
        // read symbol property keys
        propertystr.getSymbols().forEach(s ->
                values.add(UnknownValueResolver.getProperty(objlabel, SymbolPKey.make(s), state, true))
        );
        return UnknownValueResolver.join(values, state);
    }

    /**
     * [[HasProperty]].
     * The internal prototype chains are used.
     */
    private Bool hasPropertyRaw(Collection<ObjectLabel> objlabels, PKeys propertyname) {
        Value v = readPropertyRaw(objlabels, propertyname, true, true, null);
        boolean maybe_present = v.isMaybePresent();
        boolean maybe_absent = v.isMaybeAbsent();
        return maybe_present ? (maybe_absent ? Value.makeAnyBool() : Value.makeBool(true)) : (maybe_absent ? Value.makeBool(false) : Value.makeNone());
    }

    /**
     * Checks whether the given property is present in the given objects.
     * The internal prototype chains are used.
     */
    public Bool hasProperty(Collection<ObjectLabel> objlabels, PKeys propertyname) {
        Bool b = hasPropertyRaw(objlabels, propertyname);
        if (log.isDebugEnabled())
            log.debug("hasProperty(" + objlabels + "," + propertyname + ") = " + b);
        return b;
    }

    /**
     * 8.6.2.3 [[CanPut]] Checks whether the given property can be assigned in the given object.
     * The internal prototype chains are used.
     */
    private Bool canPut(ObjectProperty objprop) {
        /*
        ReadOnly properties:
        - the function name in scope object of a function expression
        - 'length' of built-in function objects and string objects
        - 'prototype' of Object, Function, Array, String, etc.
        - Number.MAX_VALUE and related constants in Number
        - 'source', 'global', etc. of regexp objects
        (note: CanPut considers the internal prototype chain...)
        */
        if (Options.get().isAlwaysCanPut())
            return Value.makeBool(true);
        Value v = readPropertyRaw(Collections.singleton(objprop.getObjectLabel()), objprop.getProperty().toValue(), true, true, null); // TODO: possible to optimize modeling of [[CanPut]]? i.e. in some cases omit this call to readPropertyRaw?
        Bool b;
        if (v.isNotPresent() || v.isNotReadOnly())
            b = Value.makeBool(true);
        else if (v.isReadOnly() && !v.isMaybeAbsent())
            b = Value.makeBool(false);
        else
            b = Value.makeAnyBool();
        if (log.isDebugEnabled())
            log.debug("canPut(" + objprop + ") = " + b);
        return b;
    }

    /**
     * Same as {@link #writeProperty(Collection, PKeys, Value, boolean)},
     * with force_weak set to false.
     */
    public void writeProperty(Collection<ObjectLabel> objlabels, PKeys propertystr, Value value) {
        writeProperty(objlabels, propertystr, value, false);
    }

    /**
     * Same as {@link #writeProperty(Collection, PKeys, Value, boolean, boolean)},
     * with not_invoke_setters set to false.
     */
    public void writeProperty(Collection<ObjectLabel> objlabels, PKeys propertystr, Value value, boolean force_weak) {
        writeProperty(objlabels, propertystr, value, force_weak, false);
    }

    /**
     * Same as {@link #writeProperty(Collection, PKeys, Value, boolean, boolean, boolean, boolean)},
     * with process_attributes set to true and value_has_attributes set to false.
     */
    public void writeProperty(Collection<ObjectLabel> objlabels, PKeys propertystr, Value value, boolean force_weak, boolean not_invoke_setters) {
        writeProperty(objlabels, propertystr, value, true, false, force_weak, not_invoke_setters);
    }

    /**
     * 8.6.2.2 [[Put]]
     * Assigns the given value to the given property of the given objects.
     * Modified is set on all values being written (irrespective of process_attributes and value_has_attributes).
     *
     * @param process_attributes if set, attempts to overwrite ReadOnly properties are ignored, and attributes are cleared or copied from the old value according to 8.6.2.2 (false for function declarations)
     * @param value_has_attributes if set, value has attributes that should be used instead of default (cleared)
     * @param force_weak if set, force weak update disregarding objlabels
     * @param not_invoke_setters if set, do not invoke setter (e.g. for property declarations in literals)
     */
    public void writeProperty(Collection<ObjectLabel> objlabels, PKeys propertystr, Value value, boolean process_attributes, boolean value_has_attributes, boolean force_weak, boolean not_invoke_setters) {
        if (Options.get().isDebugOrTestEnabled() && propertystr.isMaybeOtherThanStrOrSymbol()) {
            throw new AnalysisException("Uncoerced property name: " + propertystr);
        }
        value.assertNonEmpty();
        final boolean weak = force_weak || objlabels.size() > 1 || propertystr.isMaybeFuzzyStrOrSymbol();
        ParallelTransfer pt = new ParallelTransfer(c);
        for (ObjectLabel objlabel : objlabels) {
            if (propertystr.isMaybeSingleStr()) {
                pt.add(() -> writeProperty(ObjectProperty.makeOrdinary(objlabel, PKey.make(propertystr)), value, process_attributes, value_has_attributes, true, true, weak, not_invoke_setters));
            } else if (propertystr.getIncludedStrings() != null) {
                propertystr.getIncludedStrings().forEach(specialString ->
                        pt.add(() -> writeProperty(ObjectProperty.makeOrdinary(objlabel, StringPKey.make(specialString)), value, process_attributes, value_has_attributes, true, true, force_weak || objlabels.size() != 1 || propertystr.getIncludedStrings().size() > 1, not_invoke_setters))
                );
            } else if (propertystr.isMaybeFuzzyStr()) {
                State state = c.getState();
                if (propertystr.getExcludedStrings() != null)
                    state.getObject(objlabel, true).materialize(propertystr.getExcludedStrings());
                if (propertystr.isMaybeStrSomeNumeric()) {
                    UnknownValueResolver.getDefaultNumericProperty(objlabel, state);
                    pt.add(() -> writeProperty(ObjectProperty.makeDefaultNumeric(objlabel), value, process_attributes, value_has_attributes, true, true, true, not_invoke_setters));
                }
                if (propertystr.isMaybeStrSomeNonNumeric()) {
                    UnknownValueResolver.getDefaultOtherProperty(objlabel, state);
                    pt.add(() -> writeProperty(ObjectProperty.makeDefaultOther(objlabel), value, process_attributes, value_has_attributes, true, true, true, not_invoke_setters));
                }
                for (PKey propertyname : newSet(state.getObject(objlabel, false).getPropertyNames())) { // calls to UnknownValueResolver and materialize above have materialized all relevant properties
                    if (propertyname instanceof StringPKey && propertyname.isMaybeValue(propertystr)) {
                        pt.add(() -> writeProperty(ObjectProperty.makeOrdinary(objlabel, propertyname), value, process_attributes, value_has_attributes, true, true, true, not_invoke_setters));
                    }
                }
            }
            propertystr.getSymbols().forEach(s ->
                    pt.add(() -> writeProperty(ObjectProperty.makeOrdinary(objlabel, SymbolPKey.make(s)), value, process_attributes, value_has_attributes,true, true, weak, not_invoke_setters))
            );
        }
        pt.complete();
        if (log.isDebugEnabled())
            log.debug("writeProperty(" + objlabels + "," + propertystr + "," + value + ")");
    }

    /**
     * Assigns the given value to the given object property.
     *
     * @param process_attributes if set, attempts to overwrite ReadOnly properties are ignored, and attributes are cleared or copied from the old value according to 8.6.2.2 (false for function declarations)
     * @param value_has_attributes if set, value has attributes that should be used instead of default (cleared)
     * @param set_modified    if set, the modified flag is set on written values
     * @param allow_overwrite if set, allow overwriting of existing properties (if not set, do nothing if the property already exists), false for variable declarations
     * @param force_weak      if set, force weak update
     * @param not_invoke_setters if set, do not invoke setter (e.g. for property declarations in literals)
     */
    private void writeProperty(ObjectProperty objprop, Value value,
                               boolean process_attributes, boolean value_has_attributes, boolean set_modified, boolean allow_overwrite, boolean force_weak, boolean not_invoke_setters) {
        actualWriteProperty(objprop, value, process_attributes, value_has_attributes, set_modified, allow_overwrite, force_weak, not_invoke_setters);
        if (c.getState().isBottom())
            return;
        ObjectLabel generalization = c.getState().getGeneralization(objprop.getObjectLabel());
        if (generalization != null)
            actualWriteWithBottomCheck(objprop.makeRenamed(generalization), value, process_attributes, value_has_attributes, set_modified, allow_overwrite, true, not_invoke_setters);

        for (ObjectLabel objectLabel : c.getState().getSpecializations(objprop.getObjectLabel())) {
            actualWriteWithBottomCheck(objprop.makeRenamed(objectLabel), value, process_attributes, value_has_attributes, set_modified, allow_overwrite, true, not_invoke_setters);
        }
    }

    private void actualWriteWithBottomCheck(ObjectProperty objprop, Value value,
                                            boolean process_attributes, boolean value_has_attributes, boolean set_modified, boolean allow_overwrite, boolean force_weak, boolean not_invoke_setters) {
        State oldState = c.getState().clone();
        actualWriteProperty(objprop, value, process_attributes, value_has_attributes, set_modified, allow_overwrite, force_weak, not_invoke_setters);
        if (c.getState().isBottom()) {
            c.setState(oldState);
        }
    }

    private void actualWriteProperty(ObjectProperty objprop, Value value,
                                     boolean process_attributes, boolean value_has_attributes, boolean set_modified, boolean allow_overwrite, boolean force_weak, boolean not_invoke_setters) {
        State state = c.getState();
        final Value origValue = value; // keep the given value for setters
        Value oldvalue = UnknownValueResolver.getValue(objprop, state, true);
        if (!allow_overwrite && oldvalue.isNotAbsent()) // not allowed to overwrite and definitely present already, so just return
            return; // TODO: warn that the operation definitely has no effect? (double declaration of variable)
        Value prototypevalue = Value.makeNone();
        if (!not_invoke_setters && oldvalue.isMaybeAbsent()) { // if old value (maybe) absent, we also need the setters from the prototype chain
            Value proto = UnknownValueResolver.getInternalPrototype(objprop.getObjectLabel(), state, false);
            prototypevalue = readPropertyRaw(proto.getObjectLabels(), objprop.getProperty().toValue(), false, true, null);
        }
        boolean maybeDefiningGetter = !value.isPolymorphic() && value.isMaybeGetter();
        boolean maybeDefiningSetter = !value.isPolymorphic() && value.isMaybeSetter();
        boolean maybeOrdinaryWrite = not_invoke_setters || oldvalue.isMaybePresentData() || (oldvalue.isMaybeAbsent() && (prototypevalue.isMaybeAbsent() || prototypevalue.isMaybePresentData() || !prototypevalue.isMaybePresentAccessor()));
        boolean maybeSetterCall = !not_invoke_setters && (oldvalue.isMaybePresentAccessor() || prototypevalue.isMaybePresentAccessor());
        if (process_attributes) {
            if (oldvalue.isNotPresent()) { // definitely not present already
                if (!value_has_attributes)
                    value = value.removeAttributes(); // 8.6.2.2 item 6
            } else if (oldvalue.isNotAbsent()) // definitely present already
                value = value.setAttributes(oldvalue); // 8.6.2.2 item 4
            else { // maybe present already
                if (value_has_attributes)
                    value = value.setAttributes(oldvalue).join(value);
                else
                    value = value.setAttributes(oldvalue).join(value.removeAttributes());
            }
        }
        Bool writeable = (process_attributes && maybeOrdinaryWrite) ? canPut(objprop) : Value.makeBool(true); // TODO: generate warning if [[CanPut]] gives false?
        if (maybeSetterCall)
            writeable = writeable.joinBool(true);
        if (writeable.isMaybeTrue()) {
            if (!Options.get().isNoStringSets()
                    && (objprop.getProperty().getKind() == DEFAULT_NUMERIC || (objprop.getProperty().getKind() == ORDINARY && objprop.getPropertyName() instanceof PKey.StringPKey && Strings.isArrayIndex(((PKey.StringPKey) objprop.getPropertyName()).getStr())))
                    && !value.isPolymorphicOrUnknown() && value.isMaybeSingleStr()) {
                // about to write string to an array-like-property. We assume it is a string-collection, and "upgrade" it to a special string
                value = value.join(Value.makeStrings(singleton(value.getStr())));
            }
            if (force_weak || writeable.isMaybeFalse() || !objprop.getObjectLabel().isSingleton() || objprop.getProperty().isFuzzy() || (!allow_overwrite && oldvalue.isMaybePresent())) { // weak update
                // TODO: if !allow_overwrite && oldvalue.isMaybePresent(): warn that the operation maybe has no effect? (double declaration of variable)
                value = UnknownValueResolver.join(value, oldvalue, state);
                if (!value.isPolymorphicOrUnknown()) {
                    if (maybeDefiningGetter && !maybeDefiningSetter) {
                        value = value.join(Value.makeObject(ObjectLabel.absent_accessor_function).makeSetter()); // add dummy setter
                    } else if (maybeDefiningSetter && !maybeDefiningGetter) {
                        value = value.join(Value.makeObject(ObjectLabel.absent_accessor_function).makeGetter()); // add dummy getter
                    }
                }
            } else { // strong update
                if (!value.isPolymorphicOrUnknown()) {
                    if (maybeDefiningGetter || maybeDefiningSetter)
                        oldvalue = UnknownValueResolver.getRealValue(oldvalue, state);
                    if (maybeDefiningGetter && !maybeDefiningSetter) { // strong update of getter, but keep setter if present and else add dummy setter
                        Value oldsetter = oldvalue.restrictToSetter();
                        if (oldsetter.isNone())
                            oldsetter = Value.makeObject(ObjectLabel.absent_accessor_function).makeSetter();
                        value = UnknownValueResolver.join(value, oldsetter, state);
                    } else if (maybeDefiningSetter && !maybeDefiningGetter) { // strong update of setter, but keep getter if present and else add dummy getter
                        Value oldgetter = oldvalue.restrictToGetter();
                        if (oldgetter.isNone())
                            oldgetter = Value.makeObject(ObjectLabel.absent_accessor_function).makeGetter();
                        value = UnknownValueResolver.join(value, oldgetter, state);
                    }
                }
            }
            if (set_modified || oldvalue.isMaybeModified())
                value = value.joinModified();
            checkProperty(value);
            ParallelTransfer pt = new ParallelTransfer(c);
            boolean hasDummySetter = false;
            if (maybeSetterCall) {
                c.getMonitoring().visitPropertyRead(c.getNode(), Collections.singleton(objprop.getObjectLabel()), objprop.getProperty().toValue(), state, false);
                BasicBlock implicitAfterCall = null;
                Set<ObjectLabel> setters = newSet();
                setters.addAll(UnknownValueResolver.getRealValue(oldvalue, state).getSetters());
                setters.addAll(UnknownValueResolver.getRealValue(prototypevalue, state).getSetters());
                for (ObjectLabel obj : setters) {
                    if (obj == ObjectLabel.absent_accessor_function) {
                        hasDummySetter = true; // TODO: warn about call to dummy setter?
                    } else {
                        implicitAfterCall = UserFunctionCalls.implicitUserFunctionCall(obj, new FunctionCalls.CallInfo() {
                            @Override
                            public AbstractNode getSourceNode() {
                                return c.getNode();
                            }

                            @Override
                            public AbstractNode getJSSourceNode() {
                                return c.getNode();
                            }

                            @Override
                            public boolean isConstructorCall() {
                                return false;
                            }

                            @Override
                            public Value getFunctionValue() {
                                throw new AnalysisException("Unexpected usage of getFunctionValue");
                            }

                            @Override
                            public Value getThis() {
                                return Value.makeObject(objprop.getObjectLabel());
                            }

                            @Override
                            public int getResultRegister() {
                                return AbstractNode.NO_VALUE;
                            }

                            @Override
                            public ExecutionContext getExecutionContext() {
                                return c.getState().getExecutionContext();
                            }

                            @Override
                            public Value getArg(int i) {
                                if (i == 0) {
                                    return origValue.removeAttributes();
                                } else {
                                    return Value.makeAbsent();
                                }
                            }

                            @Override
                            public int getNumberOfArgs() {
                                return 1;
                            }

                            @Override
                            public Value getUnknownArg() {
                                throw new AnalysisException("Should not be called. Arguments are not unknown.");
                            }

                            @Override
                            public boolean isUnknownNumberOfArgs() {
                                return false;
                            }

                            @Override
                            public boolean assumeFunction() {
                                return false;
                            }

                            @Override
                            public FreeVariablePartitioning getFreeVariablePartitioning() {
                                return null;
                            }
                        }, c);
                    }
                }
                BasicBlock finalImplicitAfterCall = implicitAfterCall;
                boolean finalHasDummySetter = hasDummySetter;
                pt.add(() -> UserFunctionCalls.implicitUserFunctionReturn(null, finalHasDummySetter, finalImplicitAfterCall, c));
            }
            if (maybeOrdinaryWrite) {
                if (!value.equals(oldvalue)) { // don't request writable obj if the value doesn't change anyway
                    Value valueNonAccessor = value.restrictToNotGetterSetter();
                    Value finalValue = value;
                    pt.add(() -> {
                        if (!not_invoke_setters) {
                            boolean wroteToArrayLength = updateArrayLength(objprop, valueNonAccessor, c);
                            if (wroteToArrayLength) {
                                return;
                            }
                        }
                        boolean writeInternalPrototype_fixed = objprop.getProperty().getKind() == Property.Kind.ORDINARY && StringPKey.__PROTO__.equals(objprop.getPropertyName());
                        boolean writeInternalPrototype_dynamic = objprop.getProperty().getKind() == Property.Kind.DEFAULT_OTHER;
                        boolean writeInternalPrototype = writeInternalPrototype_fixed || writeInternalPrototype_dynamic;
                        boolean skipInternalPrototype = writeInternalPrototype && unsoundness.maySkipInternalProtoPropertyWrite(c.getNode());
                        if (writeInternalPrototype && !skipInternalPrototype) {
                            // TODO: make use of c.getState().writeInternalPrototype(objprop.getObjectLabel(), finalValue) instead? (GitHub #356) + currently ignoring old value of the internal prototype!
                            c.getState().getObject(objprop.getObjectLabel(), true).setInternalPrototype(valueNonAccessor);
                        }
                        if (!unsoundness.maySkipPropertyWrite(c.getNode(), objprop)) {
                            if (!writeInternalPrototype_fixed || !skipInternalPrototype) {
                                Value v = finalValue;
                                if (writeInternalPrototype_fixed)
                                    v = v.setAttributes(true, true, false);
                                // TODO: should also set attributes (weakly) if writeInternalPrototype_dynamic and !maySkipInternalProtoPropertyWrite - see also #356
                                c.getState().getObject(objprop.getObjectLabel(), true).setValue(objprop, v);
                                c.getState().getMustEquals().setToBottom(objprop); // TODO: no need to reset must-equals if called from Filtering
                            }
                        }
                    });
                } else {
                    pt.add(() -> {}); // nop transfer
                }
            }
            if (!not_invoke_setters && objprop.getObjectLabel().isHostObject()) {
                pt.add(() -> evaluateHostObjectSetter(objprop.getObjectLabel(), objprop.getProperty(), origValue.removeAttributes()));
            }
            pt.complete();
        }
    }

    /**
     * Evaluates a setter for the given host object property, if any.
     */
    private void evaluateHostObjectSetter(ObjectLabel objlabel, Property prop, Value value) {
        if (Options.get().isDOMEnabled() &&
                (objlabel.getHostObject().getAPI() == HostAPIs.DOCUMENT_OBJECT_MODEL || objlabel == GLOBAL)) {
            DOMObjects.evaluateDOMSetter(objlabel, prop.toValue(), value, c); // TODO: refactor to use Property instead of Str
        }
        else if (objlabel.getHostObject().getAPI() == HostAPIs.SPEC) {
            log.error("Unsupported setter " + prop + " on " + objlabel);
        } else { // not applicable for any other family of host objects
            c.getState().setToBottom();
        }
    }

    /**
     * Updates the length property of arrays in accordance with 15.4.5.1. Also models truncation of the array if the 'length' property is being set.
     * @return true iff the write is to the length property of an array object (in which case all required writes have been performed by this method)
     */
    private boolean updateArrayLength(ObjectProperty objprop, Value value, Solver.SolverInterface c) {
        if (objprop.getObjectLabel().getKind() != ObjectLabel.Kind.ARRAY)
            return false;
        Str propertystr = objprop.getProperty().toValue();
        boolean maybe_length = propertystr.isMaybeStr("length");
        boolean maybe_index = propertystr.isMaybeStrSomeUInt();
        if (maybe_length || maybe_index) {
            Double old_length = UnknownValueResolver.getRealValue(readPropertyValue(Collections.singleton(objprop.getObjectLabel()), "length"), c.getState()).getNum();
            // step 12-15 assignment to 'length', need to check for RangeError exceptions and array truncation
            if (maybe_length) {
                value = UnknownValueResolver.getRealValue(value, c.getState());
                Value numvalue = Conversion.toNumber(value, c);
                if (!numvalue.isNone()) {
                    // throw RangeError exception if illegal value
                    boolean maybeInvalid = false;
                    boolean maybeValid = false;
                    Double n = numvalue.getNum();
                    if (numvalue.isMaybeSingleNum() && n != null) {
                        long uintvalue = Conversion.toUInt32(n);
                        if (uintvalue != n) {
                            maybeInvalid = true;
                        } else {
                            maybeValid = true;
                        }
                        numvalue = Value.makeNum(uintvalue);
                    } else {
                        if (numvalue.isMaybeOtherThanNumUInt()) {
                            maybeInvalid = true;
                        }
                        if (numvalue.isMaybeNumUInt()) {
                            maybeValid = true;
                        }
                        numvalue = numvalue.restrictToNotNumOther().restrictToNotNaN().restrictToNotInf();
                    }
                    boolean definitely_length = propertystr.isMaybeSingleStr() && propertystr.getStr().equals("length");
                    if (maybeInvalid) {
                        Exceptions.throwRangeError(c, maybeValid);
                        c.getMonitoring().addMessage(c.getNode(), Message.Severity.HIGH, "RangeError, assigning invalid value to array 'length' property");
                        if (definitely_length && !maybeValid) {
                            c.getState().setToBottom();
                        }
                    }
                    if (maybeValid) {
                        // truncate
                        Double num = numvalue.getNum();
                        if (definitely_length && num != null && old_length != null && old_length - num < Options.Constants.ARRAY_TRUNCATION_BOUND) { // note: bound to avoid too many iterations
                            for (int i = num.intValue(); i < old_length.intValue(); i++) {
                                deleteProperty(Collections.singleton(objprop.getObjectLabel()), Value.makeStr(Integer.toString(i)), false);
                            }
                        } else {
                            deleteProperty(Collections.singleton(objprop.getObjectLabel()), Value.makeAnyStrUInt(), false);
                        }
                        // write 'length' property
                        numvalue = numvalue.setAttributes(true, true, false);
                        c.getState().getObject(objprop.getObjectLabel(), true).setProperty(StringPKey.LENGTH, numvalue.joinModified());
                        c.getState().getMustEquals().setToBottom(objprop.getObjectLabel(), StringPKey.LENGTH);
                    }
                }
            }
            // step 9-10 assignment to array index, need to magically update 'length'
            if (maybe_index) {
                Value v;
                boolean definitely_index = propertystr.isMaybeSingleStr() && Strings.isArrayIndex(propertystr.getStr());
                if ((definitely_index && old_length != null))
                    v = Value.makeNum(Math.max(old_length, Double.valueOf(propertystr.getStr()) + 1));
                else
                    v = Value.makeAnyNumUInt();
                c.getState().getObject(objprop.getObjectLabel(), true).setProperty(StringPKey.LENGTH, v.setAttributes(true, true, false).joinModified());
                c.getState().getMustEquals().setToBottom(objprop.getObjectLabel(), StringPKey.LENGTH);
            }
        }
        return objprop.getKind() == ORDINARY && StringPKey.LENGTH.equals(objprop.getPropertyName());
    }

    /**
     * Assigns the given value to the given property of the given objects.
     *
     * @param process_attributes if set, attempts to overwrite ReadOnly properties are ignored, and attributes are cleared or copied from the old value according to 8.6.2.2 (false for function declarations)
     * @param value_has_attributes if set, value has attributes that should be used instead of default (cleared)
     * @param set_modified    if set, the modified flag is set on written values
     * @param allow_overwrite if set, allow overwriting of existing properties (if not set, do nothing if the property already exists), false for variable declarations
     * @param force_weak      if set, force weak update disregarding objlabels
     * @param not_invoke_setter if set, do not invoke setter (e.g. for property declarations in literals)
     */
    private void writeProperty(Collection<ObjectLabel> objlabels, Property property, Value value,
                               boolean process_attributes, boolean value_has_attributes, boolean set_modified, boolean allow_overwrite, boolean force_weak, boolean not_invoke_setter) {
        ParallelTransfer.process(objlabels, (objlabel) -> writeProperty(new ObjectProperty(objlabel, property), value, process_attributes, value_has_attributes, set_modified, allow_overwrite, force_weak || objlabels.size() != 1, not_invoke_setter), c);
    }

    /**
     * Assigns the given value to the given property of the given object.
     * Attributes are set to (false,false,false). Setters are not invoked.
     * Modified is set on all values being written.
     *
     */
    public void writeProperty(ObjectLabel objlabel, PKey propertyname, Value value) {
        writePropertyWithAttributes(objlabel, propertyname, value.setAttributes(false, false, false));
    }

    /**
     * @see #writeProperty(ObjectLabel, PKey, Value)
     */
    public void writeProperty(ObjectLabel objlabel, String propertyname, Value value) {
        writeProperty(objlabel, StringPKey.make(propertyname), value);
    }
    /**
     * Assigns the given value to the given property of the given objects, with attributes.
     * Attributes are taken from the given value.
     *
     * @param set_modified if set, the modified flag is set on written values
     * @param decl if set, do not invoke setter (e.g. for property declarations in literals)
     */
    public void writePropertyWithAttributes(Collection<ObjectLabel> objlabels, PKey propertyname, Value value, boolean set_modified, boolean decl) {
        writePropertyWithAttributes(objlabels, propertyname, value, set_modified, false, decl);
    }

    /**
     * Assigns the given value to the given property of the given objects, with attributes.
     * Attributes are taken from the given value.
     *
     * @param set_modified if set, the modified flag is set on written values
     * @param decl if set, do not invoke setter (e.g. for property declarations in literals)
     */
    public void writePropertyWithAttributes(Collection<ObjectLabel> objlabels, PKey propertyname, Value value, boolean set_modified, boolean force_weak, boolean decl) {
        writeProperty(objlabels, Property.makeOrdinaryProperty(propertyname), value, false, true, set_modified, true, force_weak, decl);
        if (log.isDebugEnabled())
            log.debug("writePropertyWithAttributes(" + objlabels + "," + propertyname + "," + value + "," + value.printAttributes() + ")");
    }

    /**
     * Assigns the given value to the given property of the given objects, with attributes.
     * Attributes are taken from the given value.
     * Modified is set on all values being written.
     */
    public void writePropertyWithAttributes(Collection<ObjectLabel> objlabels, PKey propertyname, Value value) {
        writePropertyWithAttributes(objlabels, propertyname, value, true, true);
    }

    /**
     * Assigns the given value to the given property of the given object, with attributes.
     * Attributes are taken from the given value. Setters are not invoked.
     * Modified is set on all values being written.
     */
    public void writePropertyWithAttributes(ObjectLabel objlabel, PKey propertyname, Value value) {
        writePropertyWithAttributes(Collections.singleton(objlabel), propertyname, value, true, true);
    }

    /**
     * @see #writePropertyWithAttributes(ObjectLabel, PKey, Value)
     */
    public void writePropertyWithAttributes(ObjectLabel objlabel, String propertyname, Value value) {
        writePropertyWithAttributes(Collections.singleton(objlabel), StringPKey.make(propertyname), value, true, true);
    }
    /**
     * Checks for the given value that attributes are non-bottom if the value is nonempty.
     */
    private static void checkProperty(Value v) {
        if (v.isMaybePresent() && (!v.hasDontDelete() || !v.hasDontEnum() || !v.hasReadOnly()))
            throw new AnalysisException("Missing attribute information at property value " + v);
    }

    /**
     * @see #writeVariable(String, Value, boolean, boolean)
     */
    public Pair<Set<ObjectLabel>, Boolean> writeVariable(String varname, Value value, boolean set_modified) {
        return writeVariable(varname, value, set_modified, false);
    }

    /**
     * Assigns the given value to the given variable.
     *
     * @param varname the variable name
     * @param value   the new value
     * @param set_modified if set, the modified flag is set on written values (false for 'assume' operations)
     * @param not_invoke_setters if set, do not invoke setter (e.g. for assume-node variable updates)
     * @return the set of objects where the variable may be stored (i.e. the base objects),
     *         and a boolean indicating whether the write was definitely performed
     */
    public Pair<Set<ObjectLabel>, Boolean> writeVariable(String varname, Value value, boolean set_modified, boolean not_invoke_setters) {
        State state = c.getState();
        value.assertNonEmpty();
        // 10.1.4 Identifier Resolution
        // 1. Get the next object in the scope chain. If there isn't one, go to step 5.
        // 2. Call the [[HasProperty]] method of Result(1), passing the Identifier as the property.
        // 3. If Result(2) is true, return a value of type Reference whose base object is Result(1) and whose property name is the Identifier.
        // 4. Go to step 1.
        // 5. Return a value of type Reference whose base object is null and whose property name is the Identifier.
        ParallelTransfer pf = new ParallelTransfer(c);
        Set<ObjectLabel> objlabels = newSet();
        boolean definitely_found = false;
        for (Iterator<Set<ObjectLabel>> it = ScopeChain.iterable(state.getExecutionContext().getScopeChain()).iterator(); it.hasNext(); ) {
            Set<ObjectLabel> sc = it.next();
            definitely_found = true;
            for (ObjectLabel objlabel : sc) {
                Bool h = hasPropertyRaw(Collections.singleton(objlabel), Value.makeTemporaryStr(varname));
                if (h.isMaybeTrue() || !it.hasNext()) {
                    pf.add(() -> {
                        // 8.6.2.2 [[Put]]
                        // 1. Call the [[CanPut]] method of O with name P.
                        // 2. If Result(1) is false, return.
                        // 3. If O doesn't have a property with name P, go to step 6.
                        // 4. Set the value of the property to V. The attributes of the property are not changed.
                        // 5. Return.
                        // 6. Create a property with name P, set its value to V and give it empty attributes.
                        if (h.isMaybeFalseButNotTrue() && !it.hasNext() && unsoundness.maySkipDeclaringGlobalVariablesImplicitly(c.getNode(), varname)) {
                            return;
                        }
                        writeProperty(ObjectProperty.makeOrdinary(objlabel, StringPKey.make(varname)), value, true, false, set_modified, true, false, not_invoke_setters);
                        objlabels.add(objlabel);
                    });
                }
                if (h.isMaybeFalse())
                    definitely_found = false;
            }
            if (definitely_found)
                break;
        }
        pf.complete();
        if (log.isDebugEnabled())
            log.debug("writeVariable(" + varname + "," + value + ")");
        return Pair.make(objlabels, definitely_found);
    }

    /**
     * Declares the given variable (or function) and assigns the given value to it.
     * @param allow_overwrite if set, allow overwriting of existing properties (if not set, do nothing if the property already exists), false for variable declarations
     */
    public void declareAndWriteVariable(String varname, Value value, boolean allow_overwrite) {
        State state = c.getState();
        // For function declarations (p.46): If the variable object already has a property with this name,
        // replace its value and attributes.
        // For variable declarations: If there is already a property of the variable object with
        // the name of a declared variable, the value of the property and its attributes are not changed.
        // For global code (10.2.1): Variable instantiation is performed using the global object as the variable
        // object and using property attributes { DontDelete }.
        // For eval code (10.2.2): Variable instantiation is performed using the calling context's variable
        // object and using empty property attributes.
        // For function code (10.2.3): Variable instantiation is performed using the activation object as the
        // variable object and using property attributes { DontDelete }.
        // TODO: variable instantiation (excluding the activation object arguments property) in eval code should use empty attributes, i.e. without DontDelete (10.2.2)
        value.assertNonEmpty();
        writeProperty(state.getExecutionContext().getVariableObject(), Property.makeOrdinaryProperty(StringPKey.make(varname)), value.restrictToNotAbsent().setAttributes(false, true, false), false, false, true, allow_overwrite, false, false);
        if (log.isDebugEnabled())
            log.debug("declareAndWriteVariable(" + varname + "," + value + ")");
    }

    /**
     * @see #readVariable(String, Collection, boolean, boolean)
     */
    public Value readVariable(String varname, Collection<ObjectLabel> base_objs) {
        return readVariable(varname, base_objs, false, false);
    }

    /**
     * Returns the value of the given variable.
     * @param varname   the variable name
     * @param base_objs collection where base objects are added (ignored if null)
     * @param not_invoke_getters if set, do not invoke getter (e.g. for assume-node variable updates)
     * @param preserve_attributes if set, do not reset property data
     */
    public Value readVariable(String varname, Collection<ObjectLabel> base_objs, boolean not_invoke_getters, boolean preserve_attributes) {
        Collection<Value> values = newList();
        boolean definitely_found_at_some_level = false;
        for (Set<ObjectLabel> sc : ScopeChain.iterable(c.getState().getExecutionContext().getScopeChain())) {
            boolean definitely_found_at_current_level = true;
            for (ObjectLabel objlabel : sc) {
                Value v = readPropertyRaw(Collections.singleton(objlabel), Value.makeTemporaryStr(varname), false, not_invoke_getters, null);
                if (v.isMaybePresent()) { // found one (maybe)
                    values.add(preserve_attributes ? v : v.setBottomPropertyData());
                    if (base_objs != null)
                        base_objs.add(objlabel); // collecting the object from the scope chain (although the property may be in its prototype chain)
                }
                if (v.isMaybeAbsent()) {
                    definitely_found_at_current_level = false;
                }
            }
            if (definitely_found_at_current_level) {
                definitely_found_at_some_level = true;
                break;
            }
        }
        if (!definitely_found_at_some_level) {
            values.add(Value.makeAbsent()); // end of scope chain, so add absent
        }
        Value res = UnknownValueResolver.join(values, c.getState());
        if (log.isDebugEnabled())
            log.debug("readVariable(" + varname + ") = " + res + (base_objs != null ? " at " + base_objs : ""));
        return res;
    }

    /**
     * Deletes the given variable.
     *
     * @see #deleteProperty(Collection, PKeys, boolean)
     */
    public Value deleteVariable(String varname) {
        State state = c.getState();
        // 10.1.4 Identifier Resolution
        // 1. Get the next object in the scope chain. If there isn't one, go to step 5.
        // 2. Call the [[HasProperty]] method of Result(1), passing the Identifier as the property.
        // 3. If Result(2) is true, return a value of type Reference whose base object is Result(1) and whose property name is the Identifier.
        // 4. Go to step 1.
        // 5. Return a value of type Reference whose base object is null and whose property name is the Identifier.
        Set<ObjectLabel> objlabels = newSet();
        boolean definitely_found = false;
        for (Set<ObjectLabel> sc : ScopeChain.iterable(state.getExecutionContext().getScopeChain())) {
            definitely_found = true;
            for (ObjectLabel objlabel : sc) {
                Bool h = hasPropertyRaw(Collections.singleton(objlabel), Value.makeTemporaryStr(varname));
                if (h.isMaybeTrue())
                    objlabels.add(objlabel);
                if (h.isMaybeFalse())
                    definitely_found = false;
            }
            if (definitely_found)
                break;
        }
        // 11.4.1 The delete Operator
        // 1. Evaluate UnaryExpression.
        // 2. If Type(Result(1)) is not Reference, return true.
        // 3. Call GetBase(Result(1)).
        // 4. Call GetPropertyName(Result(1)).
        // 5. Call the [[Delete]] method on Result(3), providing Result(4) as the property name to delete.
        // 6. Return Result(5).
        Value res = deleteProperty(objlabels, Value.makeTemporaryStr(varname), false);
        if (!definitely_found)
            res = res.joinBool(true);
        if (log.isDebugEnabled())
            log.debug("deleteVariable(" + varname + ") = " + res);
        return res;
    }

    /**
     * Deletes the given property.
     * Ignored if the property has attribute DontDelete.
     * Returns false if attempting to delete a property with attribute DontDelete, true otherwise.
     */
    public Value deleteProperty(Collection<ObjectLabel> objlabels, PKeys propertystr, boolean force_weak) {
        Value res = Value.makeNone();
        final boolean weak = force_weak || objlabels.size() > 1 || propertystr.isMaybeFuzzyStrOrSymbol();
        for (ObjectLabel objlabel : objlabels) {
            if (weak) {
                res = res.joinBool(weakDeleteProperty(objlabel, propertystr));
            } else if (propertystr.isMaybeSingleStr() || propertystr.isMaybeSymbol()) {
                res = strongDeleteProperty(objlabel, PKey.make(propertystr));
            }
        }
        if (log.isDebugEnabled())
            log.debug("deleteProperty(" + objlabels + "," + propertystr + ") = " + res);
        return res;
    }

    /**
     * Deletes the given property (strong update).
     */
    private Value strongDeleteProperty(ObjectLabel objlabel, PKey propertyname) {
        // 8.6.2.5 [[Delete]] (P)
        // When the [[Delete]] method of O is called with property name P, the following steps are taken:
        // 1. If O doesn't have a property with name P, return true.
        // 2. If the property has the DontDelete attribute, return false.
        // 3. Remove the property with name P from O.
        // 4. Return true.
        Value res;
        Value v = UnknownValueResolver.getProperty(objlabel, propertyname, c.getState(), true);
        if (!v.isMaybePresent()) // property is definitely absent already
            res = Value.makeBool(true);
        else if (!v.isMaybeAbsent() && v.isDontDelete()) // definitely present already, definitely can't delete
            res = Value.makeBool(false);
        else {
            Value newval;
            if (v.isNotDontDelete()) { // maybe present already, definitely can delete
                newval = Value.makeAbsentModified(); // FIXME: deleting magic properties? (also other places...) (GitHub #405)
                res = Value.makeBool(true);
            } else { // don't know, maybe delete
                newval = v.joinAbsentModified();
                res = Value.makeAnyBool();
            }
            Obj obj = c.getState().getObject(objlabel, true);
            obj.setProperty(propertyname, newval);
            c.getState().getMustEquals().setToBottom(objlabel, propertyname);
        }
        return res;
    }

    /**
     * Deletes the given property (weak update).
     */
    private Value weakDeleteProperty(ObjectLabel objlabel, PKeys propertystr) {
        // 8.6.2.5 [[Delete]] (P)
        // When the [[Delete]] method of O is called with property name P, the following steps are taken:
        // 1. If O doesn't have a property with name P, return true.
        // 2. If the property has the DontDelete attribute, return false.
        // 3. Remove the property with name P from O.
        // 4. Return true.
        Value res = Value.makeNone();
        if (propertystr.isMaybeSingleStr())
            res = res.joinBool(weakDeleteProperty(ObjectProperty.makeOrdinary(objlabel, PKey.make(propertystr))));
        else if (propertystr.isMaybeFuzzyStr()) {
            if (propertystr.isMaybeStrSomeNumeric())
                res = res.joinBool(weakDeleteProperty(ObjectProperty.makeDefaultNumeric(objlabel)));
            if (propertystr.isMaybeStrSomeNonNumeric())
                res = res.joinBool(weakDeleteProperty(ObjectProperty.makeDefaultOther(objlabel)));
            // the calls to readProperty above via weakDeleteProperty have materialized all relevant properties
            for (PKey propertyname : newSet(c.getState().getObject(objlabel, false).getPropertyNames()))
              if (propertyname instanceof StringPKey && propertyname.isMaybeValue(propertystr))
                  res = res.joinBool(weakDeleteProperty(ObjectProperty.makeOrdinary(objlabel, propertyname)));
        }
        propertystr.getSymbols().forEach(s ->
                weakDeleteProperty(ObjectProperty.makeOrdinary(objlabel, SymbolPKey.make(s)))
        );
        return res;
    }


    private Value weakDeleteProperty(ObjectProperty p) {
        Value res;
        Value v = c.getState().readProperty(p, true);
        if (!v.isMaybePresent()) // property is definitely absent already
            res = Value.makeBool(true);
        else if (!v.isMaybeAbsent() && v.isDontDelete()) // definitely present already, definitely can't delete
            res = Value.makeBool(false);
        else {
            if (v.isNotDontDelete()) // maybe present already, definitely can delete (but still only do weak update)
                res = Value.makeBool(true);
            else // don't know, maybe delete
                res = Value.makeAnyBool();
            c.getState().getObject(p.getObjectLabel(), true).setValue(p, v.joinAbsentModified());
            c.getState().getMustEquals().setToBottom(p);
        }
        return res;
    }
}
