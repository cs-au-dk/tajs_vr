package dk.brics.tajs.refinement.instantiations.forwards_backwards;

import dk.brics.tajs.flowgraph.jsnodes.BinaryOperatorNode;
import dk.brics.tajs.flowgraph.jsnodes.UnaryOperatorNode;
import dk.brics.tajs.lattice.HostObject;
import dk.brics.tajs.lattice.ObjectLabel;
import dk.brics.tajs.lattice.Value;
import dk.brics.tajs.util.Pair;
import forwards_backwards_api.Forwards;
import forwards_backwards_api.ProgramLocation;
import forwards_backwards_api.Properties;
import forwards_backwards_api.memory.MemoryLocation;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static dk.brics.tajs.util.Collections.newMap;

public class ForwardsCache implements Forwards {

    private final Forwards forwards;
    private Map<Pair<ProgramLocation, Pair<UnaryOperatorNode.Op, Value>>, Value> unOpCache;
    private Map<Pair<ProgramLocation, Pair<Value, Pair<BinaryOperatorNode.Op, Value>>>, Value> binOpCache;
    private Map<ProgramLocation, Set<ProgramLocation>> predecessorCache;
    private Map<ProgramLocation, Set<ProgramLocation>> callSitesCache;
    private Map<ProgramLocation, Pair<Set<ProgramLocation>, Set<HostObject>>> calleeExitsCache;
    private Map<Pair<ProgramLocation, MemoryLocation>, Value> getValueCache;
    private Map<Pair<ProgramLocation, ObjectLabel>, Properties> forInPropertiesCache;
    private Map<Pair<ProgramLocation, Pair<ObjectLabel, Value>>, Set<ObjectLabel>> prototypeObjectsCache;
    private Map<Pair<ProgramLocation, Pair<Value, Pair<Value, Boolean>>>, Optional<Set<Value>>> inferPropertyNamesCache;
    private Map<Pair<ProgramLocation, Pair<Value, Pair<Value, Boolean>>>, Optional<Set<Value>>> inferPropertyValueCache;

    public ForwardsCache(Forwards forwards) {
        this.forwards = forwards;
    }

    public void resetCaches() {
        unOpCache = newMap();
        binOpCache = newMap();
        predecessorCache = newMap();
        callSitesCache = newMap();
        calleeExitsCache = newMap();
        getValueCache = newMap();
        forInPropertiesCache = newMap();
        prototypeObjectsCache = newMap();
        inferPropertyNamesCache = newMap();
        inferPropertyValueCache = newMap();
    }

    public Forwards getForwards() {
        return forwards;
    }

    @Override
    public Set<ProgramLocation> getCallSites(ProgramLocation functionEntry) {
        if (callSitesCache.containsKey(functionEntry)) {
            return callSitesCache.get(functionEntry);
        }
        Set<ProgramLocation> callSites = forwards.getCallSites(functionEntry);
        callSitesCache.put(functionEntry, callSites);

        return callSites;
    }

    @Override
    public Pair<Set<ProgramLocation>, Set<HostObject>> getCalleeExits(ProgramLocation caller) {
        if (calleeExitsCache.containsKey(caller)) {
            return calleeExitsCache.get(caller);
        }

        Pair<Set<ProgramLocation>, Set<HostObject>> calleeExits = forwards.getCalleeExits(caller);
        calleeExitsCache.put(caller, calleeExits);

        return calleeExits;
    }

    @Override
    public Value get(ProgramLocation location, MemoryLocation memoryLocation) {
        Pair<ProgramLocation, MemoryLocation> key = Pair.make(location, memoryLocation);
        if (getValueCache.containsKey(key)) {
            return getValueCache.get(key);
        }

        Value value = forwards.get(location, memoryLocation);
        getValueCache.put(key, value);

        return value;
    }

    @Override
    public Value binop(ProgramLocation location, Value v1, BinaryOperatorNode.Op op, Value v2) {
        Pair<ProgramLocation, Pair<Value, Pair<BinaryOperatorNode.Op, Value>>> keyInCache = Pair.make(location, Pair.make(v1, Pair.make(op, v2)));
        if(binOpCache.containsKey(keyInCache)) {
            return binOpCache.get(keyInCache);
        }
        Value value = forwards.binop(location, v1, op, v2);
        binOpCache.put(keyInCache, value);
        return value;
    }


    @Override
    public Value unop(ProgramLocation location, UnaryOperatorNode.Op op, Value v) {
        Pair<ProgramLocation, Pair<UnaryOperatorNode.Op, Value>> keyInCache = Pair.make(location, Pair.make(op, v));
        if(unOpCache.containsKey(keyInCache)) {
            return unOpCache.get(keyInCache);
        }

        Value value = forwards.unop(location, op, v);
        unOpCache.put(keyInCache, value);

        return value;
    }

    @Override
    public Set<ProgramLocation> getPredecessors(ProgramLocation location, boolean loopUnrollingInsensitive) {
        if (predecessorCache.containsKey(location)) {
            return predecessorCache.get(location);
        }

        Set<ProgramLocation> predecessors = forwards.getPredecessors(location, loopUnrollingInsensitive);
        predecessorCache.put(location, predecessors);

        return predecessors;
    }

    @Override
    public Properties getForInProperties(ProgramLocation location, ObjectLabel object) {
        Pair<ProgramLocation, ObjectLabel> key = Pair.make(location, object);
        if (forInPropertiesCache.containsKey(key)) {
            return forInPropertiesCache.get(key);
        }

        Properties forInProperties = forwards.getForInProperties(location, object);
        forInPropertiesCache.put(key, forInProperties);

        return forInProperties;
    }

    @Override
    public Set<ObjectLabel> getPrototypeObjects(ProgramLocation location, ObjectLabel object, Value propertyName) {
        Pair<ProgramLocation, Pair<ObjectLabel, Value>> key = Pair.make(location, Pair.make(object, propertyName));
        if (prototypeObjectsCache.containsKey(key)) {
            return prototypeObjectsCache.get(key);
        }

        Set<ObjectLabel> prototypeObjects = forwards.getPrototypeObjects(location, object, propertyName);
        prototypeObjectsCache.put(key, prototypeObjects);

        return prototypeObjects;
    }

    @Override
    public Optional<Set<Value>> inferPropertyNames(ProgramLocation location, Value base, Value targetValue, boolean usePrototypes) {
        Pair<ProgramLocation, Pair<Value, Pair<Value, Boolean>>> key = Pair.make(location, Pair.make(base, Pair.make(targetValue, usePrototypes)));
        if (inferPropertyNamesCache.containsKey(key)) {
            return inferPropertyNamesCache.get(key);
        }
        Optional<Set<Value>> res = forwards.inferPropertyNames(location, base, targetValue, usePrototypes);
        inferPropertyNamesCache.put(key, res);
        
        return res;
    }

    @Override
    public Optional<Set<Value>> inferPropertyValue(ProgramLocation location, Value base, Value propertyName, boolean usePrototypes) {
        Pair<ProgramLocation, Pair<Value, Pair<Value, Boolean>>> key = Pair.make(location, Pair.make(base, Pair.make(propertyName, usePrototypes)));
        if (inferPropertyValueCache.containsKey(key)) {
            return inferPropertyValueCache.get(key);
        }
        Optional<Set<Value>> res = forwards.inferPropertyValue(location, base, propertyName, usePrototypes);
        inferPropertyValueCache.put(key, res);

        return res;
    }
}
