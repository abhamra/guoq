package qoptimizer.circuit;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import qoptimizer.circuit.Node;
import qoptimizer.ast.BinOp;
import qoptimizer.ast.Expr;
import qoptimizer.ast.Real;
import qoptimizer.ast.Symbol;
import qoptimizer.ast.UnOp;
import qoptimizer.ast.Var;

public class Match {
    public final Node startNode;
    public final Map<Node, Node> patternToCircMap;
    public final int startDepth;
    public final int endDepth;
    public final Map<String, Expr> angleMap;

    public Match(Node startNode, Map<Node, Node> patternToCircMap, int startDepth, int endDepth, Map<String, Expr> angleMap) {
        this.startNode = startNode;
        this.patternToCircMap = patternToCircMap;
        this.startDepth = startDepth;
        this.endDepth = endDepth;
        this.angleMap = angleMap;
    }
}
