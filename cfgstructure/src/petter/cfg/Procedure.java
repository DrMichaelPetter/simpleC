package petter.cfg;

import java.util.*;
import petter.cfg.edges.MethodCall;
import petter.cfg.edges.Transition;

class ForwardReachability extends AbstractPropagatingVisitor<Boolean> {

	/* least upper bound */
	static private Boolean lub(Boolean b1,Boolean b2){
		if (b1==null) return b2;
		if (b2==null) return b1;
		return b1||b2;
	}
	/* less or equal */
	static boolean lessoreq(Boolean b1,Boolean b2){
		if (b1==null) return true;
		if (b2==null) return false;
		return ((!b1)||b2);
	}
	
	
	
	
	
	public ForwardReachability() {
		super(true);
	}
	@Override
	public Boolean visit(State s, Boolean newFlow) {
		Boolean oldFlow = dataflowOf(s);
		if (!lessoreq(newFlow, oldFlow)){
			Boolean newval = lub(oldFlow, newFlow);
			dataflowOf(s,newval);
			return newval;
		}
		return null;
	}
	
	@Override
	public Boolean visit(MethodCall ae, Boolean d) {
		return d;
	}
	
	public static Set<State> unreachableStates(Procedure p){
		ForwardReachability fr = new ForwardReachability();
        fr.enter(p, true);
        fr.fullAnalysis();
        Set<State> toRemove = new HashSet<>();
        for (State s:p.getStates())
        	if (!fr.dataflowOf(s))
        		toRemove.add(s);
        return toRemove;
	}

}

public class Procedure implements java.io.Serializable,Analyzable{
    protected State begin;
    protected State end;
    protected String name;
    private List<Integer> literals;
    private List<Integer> params;
    private CompilationUnit myclass;

    /**
     * Arbitrary annotations identified by key.
     */
    private Map<Object, Object> annotations;
    public Iterable<State> getStates(){
        return states;
    }
    @Override
    public Object getAnnotation(Object key) {
        if (annotations == null) return null;
        return annotations.get(key);
    }
    @Override
    public Object putAnnotation(Object key, Object value) {
        if (annotations == null)
            annotations = new HashMap<>();
        return annotations.put(key, value);
    }
    @Override
    public <T> T getAnnotation(Class<T> key) throws ClassCastException {
        if (annotations == null) return null;
        return key.cast(annotations.get(key));
    }
    @Override
    public <T> T putAnnotation(Class<T> key, T value) throws ClassCastException {
        if (annotations == null)
            annotations = new HashMap<>();
        return key.cast(annotations.put(key, value));
    }
    @Override
    public Map<Object, Object> getAnnotations() {
        return annotations;
    }
    @Override
    public void putAnnotations(Map<?, ?> a) {
        annotations.putAll(a);
    }
  
    private Set<State> states;//added
    private Map<Long,State> stateHash;
    /**
     * create a new CFG for a method.
     * @param name method name
     * @param begin start state for the CFG
     * @param end end state for the CFG
     * @param localvariables all locals used in this method
     * @param params all parameters used in this method
     */
    public Procedure(String name,State begin, State end,List<Integer> localvariables, List<Integer> params){
	this.name=name;
	//this.begin=begin;
        setBegin(begin);
	//this.end=end;
        setEnd(end);
	this.literals=localvariables;
	this.params=params;
	states = new HashSet<>();
	collectStates(states,begin);
	stateHash = fillHash(states);
        
        Set<State> unreachable = ForwardReachability.unreachableStates(this);
        for (State s: unreachable){
            if (s.isBegin()||s.isEnd()) continue;
            states.remove(s);
            for (Transition t:s.getOut()){
                if (unreachable.contains(t.getDest())) continue;
                System.out.println("Removing: "+t);
                t.getDest().deleteInEdge(t);
                transen.remove(t);
            }
        }
        stateHash = fillHash(states);
    }
    public void refreshStates(){
        states=new HashSet<>();
        collectStates(states, begin);
        stateHash = fillHash(states);
    }
    /**
     * the methods name
     * @return guess what?
     */
    public String getName(){
	return name.toString();
    }
    private void collectStates(Set<State> states, State newone){
	if (states.contains(newone)) return;
	states.add(newone);
	newone.setProcedure(this);
        for (Transition t : newone.getOut()){
            collectStates(states, t.getDest());
        }
    }
    private Set<Transition> transen;
    public Set<Transition> getTransitions(){
        if (transen!=null) return transen;
        transen=new HashSet<>();
        for (State s : states){
            for (Transition transe : s.getOut())
                transen.add(transe);
        }
        return transen;
    }
    private Map<Long,State> fillHash(Set<State> stateSet){
	Map<Long,State> retval = new HashMap<>();
        for(State candidate : stateSet){
       	    retval.put(candidate.getId(),candidate);
	}
	return retval;
    }
    /**
     * the start state
     * @return guess what?
     */
    public State getBegin() {
	return begin;
    }
    /*
     *
     */
    public final void setBegin(State begin){
        if (this.begin!=null) this.begin.setBegin(false);
        this.begin=begin;
        begin.setBegin(true);
       	states = new HashSet<>();
	collectStates(states,begin);
	stateHash = fillHash(states);
    }
    /**
     * the end state
     * @return guess what?
     */
    public State getEnd() {
	return end;
    }
    public final void setEnd(State end){
        if (this.end!=null) this.end.setEnd(false);
        this.end=end;
        end.setEnd(true);
       	states = new HashSet<>();
	collectStates(states,begin);
	stateHash = fillHash(states);
    }

    /**
     * gives a state corresponding to the delivered id
     * @return guess what?
     * @param id ID(long) of the state
     */
    public State getState(long id){
	return stateHash.get(id);
    }
    /**
     * very basic textual representation of a method.
     * @return outputs a string containing method name and the contained states
     */
    @Override
    public String toString(){
	String retval= "Methode "+name+":\n";
	Iterator<State> it = stateHash.values().iterator();
	while (it.hasNext()) retval+=it.next().toString();
	return retval;
    }
    /**
     * obtain all local variables declared in this method
     * @return guess what?
     */
    public List<Integer> getLocalVariables(){
        return literals;
    }
    public boolean contains(State state){
        return states.contains(state);
    }
     /**
     * obtain all formal parameters the method has
     * @return guess what?
     */ 
    public List<Integer> getFormalParameters(){
        return params;
    }
     /**
     * obtain all fields in the class
     * @return guess what?
     */ 
    public List<Integer> getFields(){
        if (myclass!=null){
            return myclass.getFields();
        }
        else {
            return new ArrayList<>();
        }
    }
    public void setCompilationUnit(CompilationUnit cls) {
        myclass=cls;
    }
    public CompilationUnit getCompilationUnit(){
        return myclass;
    }
  
    // interface Analyzable:
    @Override
    public void forwardAccept(Visitor v){
	if (!v.visit(this)) return;
	v.enter(getBegin());
    }
    @Override
    public void backwardAccept(Visitor v){
	if (!v.visit(this)) return;
	v.enter(getEnd());

    }
    @Override
    public <T>void forwardAccept(PropagatingVisitor<T> v, T d){
	if ((d = v.visit(this,d)) == null) return;
	v.enter(getBegin(),d );

    }
    @Override
    public <T>void backwardAccept(PropagatingVisitor<T> v, T d){
	if ((d = v.visit(this,d)) == null) return;
	v.enter(getEnd(),d);
    }

}
