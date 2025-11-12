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

import lombok.Getter;
import lombok.Setter;

@Getter
public class Match {
    public final Node startNode;
    public final Map<Node, Node> patternToCircMap;
    public final Map<String, Expr> angleMap;

    public int startDepth;
    public int endDepth;


    public Match(Node startNode, Map<Node, Node> patternToCircMap, int startDepth, int endDepth, Map<String, Expr> angleMap) {
        this.startNode = startNode;
        this.patternToCircMap = patternToCircMap;
        this.startDepth = startDepth;
        this.endDepth = endDepth;
        this.angleMap = angleMap;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Match(");
        sb.append("startNode=").append(startNode);
        sb.append(", startDepth=").append(startDepth);
        sb.append(", endDepth=").append(endDepth);

        sb.append(", patternToCircMap={");
        boolean first = true;
        for (Map.Entry<Node, Node> e : patternToCircMap.entrySet()) {
            if (!first) sb.append(", ");
            Node patternNode = e.getKey();
            Node circuitNode = e.getValue();
            sb.append(patternNode)
              .append(" -> ")
              .append(circuitNode)
              .append(" (depth=")
              .append(circuitNode.getDepth())
              .append(")");
            first = false;
        }
        sb.append("}");

        sb.append(", angleMap={");
        first = true;
        for (Map.Entry<String, Expr> e : angleMap.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(e.getKey()).append("=").append(e.getValue());
            first = false;
        }
        sb.append("}");

        sb.append(")");
        return sb.toString();
    }
}

