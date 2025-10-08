package qoptimizer.circuit;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Getter;
import lombok.Setter;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import qoptimizer.config.OptObj;
import qoptimizer.config.Params;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static qoptimizer.parser.NodeVisitor.allAnglesZero;


public class CircuitDAG {

    @Getter
    private DirectedMultigraph<Node, Edge> dag;
    @Getter
    @Setter
    private String qasmHeader;
    @Getter
    private Set<String> qubits;
    @Getter
    private BiMap<String, String> qubitRenameMap; // partition name : original name
    private Map<String, Node> qubitToLeaf;
    private LinkedList<Node> gatesToAddStack;

    public CircuitDAG() {
        this.dag = new DirectedMultigraph<>(Edge.class);
        this.qasmHeader = "";
        this.qubits = new HashSet<>();
        this.qubitRenameMap = HashBiMap.create();
        this.qubitToLeaf = new HashMap<>();
        this.gatesToAddStack = new LinkedList<>();
    }

    public CircuitDAG(CircuitDAG circuit) {
        this.dag = (DirectedMultigraph<Node, Edge>) circuit.getDag().clone();
        this.qasmHeader = circuit.getQasmHeader();
        this.qubits = new HashSet<>(circuit.getQubits());
        this.qubitRenameMap = HashBiMap.create(circuit.getQubitRenameMap());
    }

    public String toQASM() {
        TopologicalOrderIterator<Node, Edge> dagIter = new TopologicalOrderIterator<>(dag);

        StringBuilder qasm = new StringBuilder();
        qasm.append(qasmHeader);
        dagIter.forEachRemaining((x) -> {
            if (x.isGate() && (x.getAngles() == null || !allAnglesZero(x.getAngles())))
                qasm.append(x.toString(qubitRenameMap).concat(";\n"));
        });

        return qasm.toString();
    }

    public int getDepth() {
        Map<Node, Integer> depthMap = new HashMap<>();
        TopologicalOrderIterator<Node, Edge> iter = new TopologicalOrderIterator<>(dag);

        while (iter.hasNext()) {
            Node node = iter.next();

            // Ignore source qubits
            if (node.isSourceQubit()) {
                depthMap.put(node, 0);
                continue;
            }

            int maxPredDepth = 0;
            for (Node pred : Graphs.predecessorListOf(dag, node)) {
                int predDepth = depthMap.getOrDefault(pred, 0);
                if (pred.isSourceQubit()) {
                    predDepth = 0; // don't count sources
                } else if (pred.isSinkQubit()) {
                    continue; // skip sinks in path
                }
                maxPredDepth = Math.max(maxPredDepth, predDepth);
            }

            depthMap.put(node, maxPredDepth + 1); // add this gate
        }

        // max depth over all sink's predecessors
        int maxDepth = 0;
        for (Node n : dag.vertexSet()) {
            if (n.isSinkQubit()) {
                for (Node pred : Graphs.predecessorListOf(dag, n)) {
                    maxDepth = Math.max(maxDepth, depthMap.getOrDefault(pred, 0));
                }
            }
        }

        return maxDepth;
    }

    public boolean contains(Node gate) {
        return gatesToAddStack.contains(gate);
    }

    // True if adding gate doesn't exceed max allowed qubits
    public boolean addGateOk(CircuitDAG circuit, Node gate, int maxQubits, boolean addChild) {
        if (!gate.isGate()) {
            return false;
        }
        if (gatesToAddStack.contains(gate)) {
            return false;
        }

        for (String qubit : gate.getQubits()) {
            if (qubit == null) {
                continue;
            }
            if (!qubits.contains(qubit) && qubits.size() == maxQubits) {
                return false;
            }
        }

        if (addChild) {
            // check when adding child if parents are in the partition
            List<Node> parents = Graphs.predecessorListOf(circuit.getDag(), gate);
            for (Node parent : parents) {
                if (!gatesToAddStack.contains(parent) && !parent.isSourceQubit()) {
                    return false;
                }
            }
        } else {
            List<Node> children = Graphs.successorListOf(circuit.getDag(), gate);
            for (Node child : children) {
                if (!gatesToAddStack.contains(child) && !child.isSinkQubit()) {
                    return false;
                }
            }
        }

        return true;
    }

    private Edge getEdge(Node source, Node target, String qubit) {
        Edge.Label sourceLabel = Edge.Label.NONE;
        Edge.Label targetLabel = Edge.Label.NONE;
        if (source.isCX()) {
            if (qubit.equals(source.getQubits().get(0))) {
                sourceLabel = Edge.Label.CONTROL;
            } else {
                sourceLabel = Edge.Label.TARGET;
            }
        } else if (source.isCCZ()) {
            if (qubit.equals(source.getQubits().get(0))) {
                sourceLabel = Edge.Label.CONTROL;
            } else if (qubit.equals(source.getQubits().get(1))) {
                sourceLabel = Edge.Label.CONTROL2;
            } else {
                sourceLabel = Edge.Label.TARGET;
            }
        }
        if (target.isCX()) {
            if (qubit.equals(target.getQubits().get(0))) {
                targetLabel = Edge.Label.CONTROL;
            } else {
                targetLabel = Edge.Label.TARGET;
            }
        } else if (target.isCCZ()) {
            if (qubit.equals(target.getQubits().get(0))) {
                targetLabel = Edge.Label.CONTROL;
            } else if (qubit.equals(target.getQubits().get(1))) {
                targetLabel = Edge.Label.CONTROL2;
            } else {
                targetLabel = Edge.Label.TARGET;
            }
        }

        return new Edge(sourceLabel, targetLabel, qubit);
    }

    private void addEdges(Node gateNode) {
        for (String qubit : gateNode.getQubits()) {
            if (qubitToLeaf.containsKey(qubit)) {
                Node anc = qubitToLeaf.get(qubit);
                dag.addEdge(anc, gateNode, getEdge(anc, gateNode, qubit));
            } else {
                Node qubitNode = new Node(qubit, Node.Type.QUBIT_SOURCE, null, null);
                dag.addVertex(qubitNode);
                dag.addEdge(qubitNode, gateNode, getEdge(qubitNode, gateNode, qubit));
            }

            qubitToLeaf.put(qubit, gateNode);
        }
    }

    private void addSinkNodes() {
        for (String qubit : qubitToLeaf.keySet()) {
            Node qubitSink = new Node(qubit, Node.Type.QUBIT_SINK, null, null);
            dag.addVertex(qubitSink);
            dag.addEdge(qubitToLeaf.get(qubit), qubitSink, getEdge(qubitToLeaf.get(qubit), qubitSink, qubit));
        }
    }

    public String wouldAddNewQubit(Node gate, int maxQubits) {
        for (String qubit : gate.getQubits()) {
            if (qubit != null && !qubits.contains(qubit) && qubits.size() < maxQubits) {
                return qubit;
            }
        }
        return null;
    }

    public void addGate(Node gate, Set<Node> roots, boolean child) {
        if (gatesToAddStack.contains(gate)) {
            return;
        }
        for (int i = 0; i < gate.getQubits().size(); i++) {
            String qubit = gate.getQubits().get(i);
            if (!qubits.contains(qubit)) {
                qubitRenameMap.put(String.format("q[%s]", qubitRenameMap.size()), qubit);
                qubits.add(qubit);
                roots.add(gate);
            }
        }

        if (child) {
            gatesToAddStack.add(gate);
        } else {
            gatesToAddStack.push(gate);
        }
    }

    public void addGate(Node gate) {
        for (String qubit : gate.getQubits()) {
            qubits.add(qubit);
        }

        gatesToAddStack.add(gate);
    }

    public void addAllPartition() {
        for (Node gate : gatesToAddStack) {
            dag.addVertex(gate);
            addEdges(gate);
        }
        addSinkNodes();
    }

    /*********************************** FUNCTIONS TO COMPUTE SIZE ***********************************/
    // TODO: gate count dictionary function
    public int totalGateCount() {
        return dag.vertexSet().size() - (2 * qubits.size()); // vertices that are not qubit source or sink nodes
    }

    public int twoQGateCount() {
        int size = 0;
        for (Node n : dag.vertexSet()) {
            if (n.is2QGate()) {
                size++;
            }
        }
        return size;
    }

    public int tGateCount() {
        int size = 0;
        for (Node n : dag.vertexSet()) {
            if (n.isTGate()) {
                size++;
            }
        }
        return size;
    }

    public int totalGateCountIgnoreRz() {
        int size = 0;
        for (Node n : dag.vertexSet()) {
            if (n.isGate()) {
                if (!n.getId().equals("rz") && !n.getId().equals("u1")) {
                    size++;
                }
            }
        }
        return size;
    }

    public int fidelity() {
        int size = 0;
        for (Node n : dag.vertexSet()) {
            if (n.is2QGate()) {
                size += Params.FIDELITY_BREAKEVEN;
                continue;
            }
            if (n.isGate() && !n.getId().equals("rz") && !n.getId().equals("u1")) {
                size++;
            }
        }
        return size;
    }

    public int cost(OptObj optObj) {
        switch (optObj) {
            case TOTAL: {
                return totalGateCount();
            }
            case T: {
                return tGateCount();
            }
            case TWO_Q: {
                return twoQGateCount();
            }
            case TOTAL_IGNORE_RZ: {
                return totalGateCountIgnoreRz();
            }
            case FIDELITY: {
                return fidelity();
            }
            case FT: {
                return Params.FIDELITY_BREAKEVEN * tGateCount() + twoQGateCount();
            }
            default:
                throw new RuntimeException("Unsupported optObj: " + optObj);
        }
    }

    public int combinedCost(OptObj optObj) {
        switch (optObj) {
            case TOTAL: {
                return totalGateCount();
            }
            case T: {
                return tGateCount() + totalGateCount();
            }
            case TWO_Q: {
                return twoQGateCount() + totalGateCount();
            }
            case TOTAL_IGNORE_RZ: {
                return totalGateCountIgnoreRz();
            }
            default:
                throw new RuntimeException("Unsupported optObj: " + optObj);
        }
    }

    /*********************************** FUNCTIONS TO GET VERTICES/ROOTS/LEAVES ***********************************/

    public Set<Node> nodes() {
        return dag.vertexSet();
    }

    public List<Node> roots() {
        List<Node> roots = new ArrayList<>();
        for (Node n : dag.vertexSet()) {
            if (n.isSourceQubit()) {
                roots.addAll(Graphs.successorListOf(dag, n));
            }
        }
        return roots;
    }

    public Map<String, Node> rootsMap() {
        Map<String, Node> roots = new HashMap<>();
        for (Node n : dag.vertexSet()) {
            if (n.isSourceQubit()) {
                var succs = Graphs.successorListOf(dag, n);
                if (succs.size() > 1) {
                    throw new RuntimeException("source node has more than one successor");
                }
                roots.put(n.getId(), succs.get(0));
            }
        }
        return roots;
    }

    public Map<String, Node> leavesMap() {
        Map<String, Node> leaves = new HashMap<>();
        for (Node n : dag.vertexSet()) {
            if (n.isSinkQubit()) {
                var preds = Graphs.predecessorListOf(dag, n);
                if (preds.size() > 1) {
                    throw new RuntimeException("sink node has more than one predecessor");
                }
                leaves.put(n.getId(), preds.get(0));
            }
        }
        return leaves;
    }

    public Set<String> getConnectivity() {
        Set<String> pairs = new HashSet<>();
        for (Node n : dag.vertexSet()) {
            if (n.isCX()) {
                String qubit1 = n.getQubits().get(0);
                String qubit2 = n.getQubits().get(1);
                int comp = qubit1.compareTo(qubit2);
                if (comp > 0) {
                    pairs.add(qubit2 + qubit1);
                } else if (comp < 0) {
                    pairs.add(qubit1 + qubit2);
                } else {
                    throw new RuntimeException("two qubit gate with same qubit");
                }
            }
        }
        return pairs;
    }

    public int getDagHash() {
        int hash = 31;
        boolean isDirected = dag.getType().isDirected();

        int part;
        for (Iterator<Edge> var3 = dag.edgeSet().iterator(); var3.hasNext(); hash += part) {
            Edge e = var3.next();
            part = e.hash();
            int source = dag.getEdgeSource(e).hash();
            int target = dag.getEdgeTarget(e).hash();
            int pairing = source + target;
            if (isDirected) {
                pairing = pairing * (pairing + 1) / 2 + target;
            }

            part = 31 * part + pairing;
            part = 31 * part + Double.hashCode(dag.getEdgeWeight(e));
        }

        return hash;
    }

}
