import org.junit.jupiter.api.Test;
import qoptimizer.Optimizer;
import qoptimizer.parser.CircuitParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OptimizerTest {

    private Optimizer applier = new Optimizer(new Random(), 5);
    private Random rand = new Random();

    @Test
    public void testRule1() {
        try {
            // FIXME: Make this test work!!
            String circuit = "h q1; h q2; h q2; x q2;";
            String find = "h q0; h q0;";
            String replace = "";
            var circuitDag = CircuitParser.qasmToDag(circuit);
            // circuitDag = applier.applyRule(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
            // circuitDag = applier.applyRuleParallel(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
            circuitDag = applier.applyRuleParallelNew(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
            assertEquals("h q1;\nx q2;\n", CircuitParser.dagToQasm(circuitDag));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testRule2() {
        String circuit = "x q0; x q1; cx q0,q1; cx q2,q0; cx q2,q1;";
        String find = "cx q0,q1; cx q2,q0; cx q2,q1;";
        String replace = "cx q2,q0; cx q0,q1;";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        // circuitDag = applier.applyRule(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        // FIXME: Fix this later!
        circuitDag = applier.applyRuleParallelNew(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        assertEquals("x q0;\nx q1;\ncx q2,q0;\ncx q0,q1;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testRule3() {
        String circuit = "t q2; cx q2,q1; cx q2,q0; cx q3,q1;";
        String find = "cx q2,q1; cx q2,q0;";
        String replace = "cx q2,q0; cx q2,q1;";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applyRuleParallelNew(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        assertEquals("t q2;\ncx q2,q0;\ncx q2,q1;\ncx q3,q1;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testRule4() {
        // testing not matched, not real rule
        String circuit = "x q0; x q1; cx q0,q1; cx q2,q0; cx q2,q1;";
        String find = "x q3; x q1; cx q3,q1; cx q2,q4; cx q2,q1;";
        String replace = "";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applyRuleParallelNew(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        assertEquals("x q0;\nx q1;\ncx q0,q1;\ncx q2,q0;\ncx q2,q1;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testRule5() {
        // testing not matched
        String circuit = "t q4; cx q2,q4; cx q2,q6; tdg q4; cx q3,q5; cx q3,q4;";
        String find = "t q1; cx q0,q1; tdg q1; cx q0,q1;";
        String replace = "cx q0,q1; tdg q1; cx q0,q1; t q1;";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applyRuleParallelNew(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        assertEquals("t q4;\ncx q3,q5;\ncx q2,q4;\ncx q2,q6;\ntdg q4;\ncx q3,q4;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testRule6() {
        // testing not matched, not real rule
        String circuit = "s q2; cx q1,q2; cx q2,q3; tdg q3; cx q1,q3;";
        String find = "s q0; cx q2,q0; cx q2,q1;";
        String replace = "";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applyRuleParallelNew(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        assertEquals("s q2;\ncx q1,q2;\ncx q2,q3;\ntdg q3;\ncx q1,q3;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testRule7() {
        // testing matched, not real rule
        String circuit = "x q1; x q0; cx q0,q1; cx q1,q0; x q1; x q0;";
        String find = "cx q0,q1; cx q1,q0;";
        String replace = "";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        // FIXME: Remove parallel later
        circuitDag = applier.applyRuleParallelNew(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        assertEquals("x q1;\nx q0;\nx q1;\nx q0;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testRule8() {
        // testing not matched, not real rule
        String circuit = "x q1; x q0; cx q0,q1; h q1; cx q1,q0; x q1; x q0;";
        String find = "cx q0,q1; cx q1,q0;";
        String replace = "";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applyRule(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        assertEquals("x q1;\nx q0;\ncx q0,q1;\nh q1;\ncx q1,q0;\nx q1;\nx q0;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testRule9() {
        String circuit = "h q0; cx q2,q0; h q0; cx q2,q3; cx q3,q1; cx q0,q1;";
        String find = "h q0; cx q2,q0; h q0; cx q0,q1;";
        String replace = "cx q0,q1; h q0; cx q2,q0; h q0;";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applyRuleParallelNew(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        assertEquals("h q0;\ncx q2,q0;\nh q0;\ncx q2,q3;\ncx q3,q1;\ncx q0,q1;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testRule10() {
        String circuit = "t q13; cx q13,q16; tdg q16; cx q14,q16; t q16; h q16; ccz q9,q16,q15; t q14; h q9; h q16;";
        String find = "t q13; cx q13,q16; tdg q16; cx q14,q16; t q16; h q16; ccz q9,q16,q15; t q14; h q9; h q16;";
        String replace = "";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applyRule(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        assertEquals("", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testRule11() {
        // test because second match shouldn't be found (would be not valid after first match applied)
        String circuit = "cx q0,q3; cx q3,q4; t q4; tdg q3; cx q0,q4; cx q2,q3; cx q0,q3; cx q2,q4; cx q2,q1; tdg q4;";
        String find = "cx q2,q0; cx q1,q0";
        String replace = "cx q1,q0; cx q2,q0;";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applyRuleParallelNew(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        assertEquals("cx q0,q3;\ncx q3,q4;\nt q4;\ntdg q3;\ncx q2,q3;\ncx q2,q4;\ncx q0,q4;\ncx q2,q1;\ntdg q4;\ncx q0,q3;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testRule12() {
        // test because second match shouldn't be found (would be not valid after first match applied)
        String circuit = "rz(pi/2) q0; rz(-pi/2) q0;";
        String find = "rz(theta1) q0; rz(theta2) q0;";
        String replace = "rz(theta1+theta2) q0;";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applyRuleParallelNew(circuitDag, replace, CircuitParser.qasmToDag(find), false, rand);
        assertEquals("", CircuitParser.dagToQasm(circuitDag));
    }

    // Helper method to load circuit from file
    private String loadCircuitFromFile(String filepath) {
        try {
            return Files.readString(Path.of(filepath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load circuit file: " + filepath, e);
        }
    }

    @Test
    public void testParallelPerfBench() {
        // Test that is more of a benchmark, for parallel vs not
        String circuit = loadCircuitFromFile("latest_sol__qft_N100_basis_rz_rx_ry_cx.qasm");
        String find = "rz(theta1) q0;";
        String replace = "";
        int benchmarkIterations = 10;
        
        System.out.println("\nBenchmarking applyRule (sequential)...");
        long sequentialTotal = 0;
        for (int i = 0; i < benchmarkIterations; i++) {
            var circuitDag = CircuitParser.qasmToDag(circuit);
            var findDag = CircuitParser.qasmToDag(find);
            
            long startTime = System.nanoTime();
            circuitDag = applier.applyRule(circuitDag, replace, findDag, false, rand);
            long endTime = System.nanoTime();
            
            long duration = endTime - startTime;
            sequentialTotal += duration;
            System.out.printf("  Iteration %d: %.3f ms%n", i + 1, duration / 1_000_000.0);
        }
        
        System.out.println("\nBenchmarking applyRuleParallelNew (parallel)...");
        long parallelTotal = 0;
        for (int i = 0; i < benchmarkIterations; i++) {
            var circuitDag = CircuitParser.qasmToDag(circuit);
            var findDag = CircuitParser.qasmToDag(find);
            
            long startTime = System.nanoTime();
            // circuitDag = applier.applyRuleParallelNewTiming(circuitDag, replace, findDag, false, rand);
            circuitDag = applier.applyRuleParallelNewTimingFull(circuitDag, replace, findDag, false, rand);
            long endTime = System.nanoTime();
            
            long duration = endTime - startTime;
            parallelTotal += duration;
            System.out.printf("  Iteration %d: %.3f ms%n", i + 1, duration / 1_000_000.0);
        }
        
        double sequentialAvg = sequentialTotal / (double) benchmarkIterations / 1_000_000.0;
        double parallelAvg = parallelTotal / (double) benchmarkIterations / 1_000_000.0;
        double speedup = sequentialAvg / parallelAvg;
        
        System.out.println("\n    BENCHMARK RESULTS");
        System.out.printf("Sequential (applyRule) average: %.3f ms%n", sequentialAvg);
        System.out.printf("Parallel (applyRuleParallelNew) average: %.3f ms%n", parallelAvg);
        System.out.printf("Speedup: %.2fx%n", speedup);

        assertTrue(parallelAvg < sequentialAvg); // Parallel should be faster
    }

    @Test
    public void testSymbRule0() {
        String circuit = "rz(-pi/4) q0; cx q1,q0; cx q0,q1; rz(-pi/4) q1;";
        String constraints = "[{[false, false]=[true, false], [true, false]=[false, true], [false, true]=[false, false], [true, true]=[true, true]}, {[false, false]=[true, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[false, true]}, {[false, false]=[false, false], [true, false]=[true, true], [false, true]=[true, false], [true, true]=[false, true]}, {[false, false]=[false, false], [true, false]=[false, true], [false, true]=[true, false], [true, true]=[true, true]}]";

        String findBefore = "rz(theta1) q0;";
        String findAfter = "rz(theta2) q1;";
        String replaceBefore = "rz(theta1+theta2) q0;";
        String replaceAfter = "";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applySymbRule(circuitDag, findBefore, findAfter, replaceBefore, replaceAfter, applier.parseConstraints(constraints), 7, 10, false, rand);
        assertEquals("rz(-1.5707963267948966) q0;\ncx q1,q0;\ncx q0,q1;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testSymbRule1() {
        String circuit = "t q0; cx q1,q0; cx q0,q1; t q1;";
        String constraints = "[{[false, false]=[true, false], [true, false]=[false, true], [false, true]=[false, false], [true, true]=[true, true]}, {[false, false]=[true, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[false, true]}, {[false, false]=[false, false], [true, false]=[true, true], [false, true]=[true, false], [true, true]=[false, true]}, {[false, false]=[false, false], [true, false]=[false, true], [false, true]=[true, false], [true, true]=[true, true]}]";

        String findBefore = "t q0;";
        String findAfter = "t q1;";
        String replaceBefore = "s q0;";
        String replaceAfter = "";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applySymbRule(circuitDag, findBefore, findAfter, replaceBefore, replaceAfter, applier.parseConstraints(constraints), 7, 10, false, rand);
        assertEquals("s q0;\ncx q1,q0;\ncx q0,q1;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testSymbRule2() {
        String circuit = "t q2; cx q2,q1; cx q2,q3; t q1; cx q1,q2; h q3; cx q2,q3; cx q1,q2; t q2;";
        String constraints = "[{[false, false]=[false, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[true, true]}, {[false, false]=[false, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[true, true]}, {[false, false]=[false, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[true, true]}, {[false, false]=[false, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[true, true]}]";

        String findBefore = "t q0;";
        String findAfter = "t q0;";
        String replaceBefore = "s q0;";
        String replaceAfter = "";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applySymbRule(circuitDag, findBefore, findAfter, replaceBefore, replaceAfter, applier.parseConstraints(constraints), 7, 10, false, rand);
        assertEquals("s q2;\ncx q2,q1;\nt q1;\ncx q2,q3;\ncx q1,q2;\nh q3;\ncx q2,q3;\ncx q1,q2;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testSymbRule3() {
        String circuit = "t q4; h q4; cx q2,q4; t q2; h q4; cx q1,q4; t q1;";
        String constraints = "[{[false, false]=[true, false], [true, false]=[false, true], [false, true]=[false, false], [true, true]=[true, true]}, {[false, false]=[true, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[false, true]}, {[false, false]=[false, false], [true, false]=[true, true], [false, true]=[true, false], [true, true]=[false, true]}, {[false, false]=[false, false], [true, false]=[false, true], [false, true]=[true, false], [true, true]=[true, true]}]";

        String findBefore = "t q0;";
        String findAfter = "t q1;";
        String replaceBefore = "s q0;";
        String replaceAfter = "";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applySymbRule(circuitDag, findBefore, findAfter, replaceBefore, replaceAfter, applier.parseConstraints(constraints), 7, 10, false, rand);
        assertEquals("t q4;\nh q4;\ncx q2,q4;\nt q2;\nh q4;\ncx q1,q4;\nt q1;\n", CircuitParser.dagToQasm(circuitDag));
    }

    @Test
    public void testSymbRule4() {
        String circuit = "t q2; s q2; cx q2,q1; cx q2,q3; t q1; cx q1,q2; h q3; cx q2,q3; cx q1,q2; t q2; s q2";
        String constraints = "[{[false, false]=[false, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[true, true]}, {[false, false]=[false, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[true, true]}, {[false, false]=[false, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[true, true]}, {[false, false]=[false, false], [true, false]=[true, true], [false, true]=[false, false], [true, true]=[true, true]}]";

        String findBefore = "t q0; s q0;";
        String findAfter = "t q0; s q0;";
        String replaceBefore = "s q0;";
        String replaceAfter = "";
        var circuitDag = CircuitParser.qasmToDag(circuit);
        circuitDag = applier.applySymbRule(circuitDag, findBefore, findAfter, replaceBefore, replaceAfter, applier.parseConstraints(constraints), 7, 10, false, rand);
        assertEquals("s q2;\ncx q2,q1;\nt q1;\ncx q2,q3;\ncx q1,q2;\nh q3;\ncx q2,q3;\ncx q1,q2;\n", CircuitParser.dagToQasm(circuitDag));
    }
}
