package it.unisa.ocelot.c.cdg;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CFGBuilder;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.util.Utils;
import it.unisa.ocelot.conf.ConfigManager;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/*
public class CDGTest {

    private CFG buildCFGFromCode(String code, String funcName) throws IOException, CoreException {
        // ensure the test configuration is available for MacroDefinerVisitor
        URL cfgRes = getClass().getClassLoader().getResource("tests/test.properties");
        if (cfgRes != null) {
            ConfigManager.setFilename(cfgRes.getFile());
        }

        // create temporary directory under target
        Path tmpDir = Path.of("target", "test-temp");
        Files.createDirectories(tmpDir);
        Path src = tmpDir.resolve(funcName + ".c");
        Files.writeString(src, code);

        CFG cfg = CFGBuilder.build(src.toFile().getCanonicalPath(), funcName);
        assertNotNull(cfg.getStart());
        assertNotNull(cfg.getEnd());
        return cfg;
    }

    @Test
    public void testIfAndIfElse() throws Exception {
        String code = "int foo(int a, int b) { if (a > b) { a = 1; } else { b = 2; } return a; }";
        CFG cfg = buildCFGFromCode(code, "foo");

        CDG.resetNodeIds();
        CDG cdg = new CDG(cfg);
        assertNotNull(cdg.getEntryNode());

        // at least one CDG edge expected from the if condition to inner statements
        boolean found = cdg.edgeSet().stream().anyMatch(e -> {
            String lbl = e.getBranchLabel() == null ? "" : e.getBranchLabel().toString();
            return lbl.contains("T") || lbl.contains("F") || lbl.equals("TRUE") || lbl.equals("FALSE");
        });
        assertTrue("Expected boolean-labeled CD edges for if/else", found);
    }

    @Test
    public void testNestedIfs() throws Exception {
        String code = "int foo(int a, int b) { if (a>0) { if (b>0) { a=2; } } return a; }";
        CFG cfg = buildCFGFromCode(code, "foo");
        CDG.resetNodeIds();
        CDG cdg = new CDG(cfg);

        // There must be more than one CDG node and some edges
        assertTrue(cdg.vertexSet().size() > 1);
        assertTrue(cdg.edgeSet().size() >= 1);
    }

    @Test
    public void testLoopsWithBreakContinue() throws Exception {
        String code = "int foo(int n) { int i=0; for (i=0;i<n;i++) { if (i==2) continue; if (i==3) break; } return i; }";
        CFG cfg = buildCFGFromCode(code, "foo");
        CDG.resetNodeIds();
        CDG cdg = new CDG(cfg);

        // look for CONTINUE/BREAK labels or boolean labels present
        boolean hasSpecial = cdg.edgeSet().stream().anyMatch(e -> {
            Object l = e.getBranchLabel();
            if (l == null) return false;
            String s = l.toString();
            return s.contains("CONTINUE") || s.contains("BREAK") || s.contains("T") || s.contains("F");
        });
        assertTrue("Expected loop-related CD edges", hasSpecial);
    }

    @Test
    public void testSwitchCase() throws Exception {
        String code = "int foo(int x) { switch(x) { case 1: x=10; break; case 2: x=20; break; default: x=0; } return x; }";
        CFG cfg = buildCFGFromCode(code, "foo");
        CDG.resetNodeIds();
        CDG cdg = new CDG(cfg);

        // look for case labels in CDG edges; use ControlDependenceEdge.toString() which normalizes labels
        boolean hasCaseLabel = cdg.edgeSet().stream().anyMatch(e -> {
            String s = e.toString().toLowerCase();
            return s.contains("case") || s.contains("default") || s.contains("1") || s.contains("2") || s.contains("10") || s.contains("20");
        });

        boolean cfgHasCaseEdge = cfg.edgeSet().stream().anyMatch(e -> e.getClass().getSimpleName().equals("CaseEdge"));

        assertTrue("Expected switch/case CD edges or CFG to contain case edges", hasCaseLabel || cfgHasCaseEdge);
    }

    @Test
    public void testNestedLoopSwitchCombination() throws Exception {
        String code = "int foo(int n, int x) { for (int i=0;i<n;i++) { switch(x) { case 0: if (i%2==0) x++; break; default: x--; } } return x; }";
        CFG cfg = buildCFGFromCode(code, "foo");
        CDG.resetNodeIds();
        CDG cdg = new CDG(cfg);

        // just ensure CDG built and contains at least one edge
        assertTrue(cdg.vertexSet().size() > 0);
        assertTrue(cdg.edgeSet().size() > 0);
    }

    @Test
    public void testCFGContinuationAfterIf() throws Exception {
        String code = "int foo(int a) { if (a>0) { a = 1; } int x = 2; return a; }";
        CFG cfg = buildCFGFromCode(code, "foo");

        // find node containing assignment 'a = 1' and node containing declaration 'int x = 2'
        CFGNode assignNode = null;
        CFGNode nextNode = null;
        for (CFGNode n : cfg.vertexSet()) {
            String txt = n.toString();
            if (txt.contains("a = 1") || txt.contains("a=1")) assignNode = n;
            if (txt.contains("int x = 2") || txt.contains("int x=2") || txt.contains("int x= 2")) nextNode = n;
        }

        assertNotNull("assignment node should be found", assignNode);
        assertNotNull("next sequential node should be found", nextNode);

        // There must be a direct edge from assignNode to nextNode (Flow edge)
        boolean hasDirect = cfg.getEdge(assignNode, nextNode) != null;
        assertTrue("Expected CFG to route from if-body end to next sequential statement", hasDirect);
    }

    @Test
    public void testLoopConditionHasTrueAndFalseCDGEdges() throws Exception {
        String code = "int foo(int n) { int i=0; while (i < n) { i++; } return i; }";
        CFG cfg = buildCFGFromCode(code, "foo");

        CDG.resetNodeIds();
        CDG cdg = new CDG(cfg);

        // find CDG node corresponding to the loop condition
        CDGNode loopCond = null;
        for (CFGNode cn : cfg.vertexSet()) {
            if (cn.getNodes() != null && !cn.getNodes().isEmpty()) {
                String s = cn.getNodes().get(0).getRawSignature();
                if (s != null && s.contains("i < n")) {
                    loopCond = cdg.getCDGNode(cn);
                    break;
                }
            }
        }

        assertNotNull("Loop condition CDG node should be found", loopCond);

        boolean hasTrue = false;
        boolean hasFalse = false;
        for (ControlDependenceEdge e : cdg.outgoingEdgesOf(loopCond)) {
            String lbl = e == null ? null : e.toString();
            if (lbl == null) continue;
            if (lbl.equalsIgnoreCase("TRUE") || lbl.equalsIgnoreCase("T") || lbl.contains("true")) hasTrue = true;
            if (lbl.equalsIgnoreCase("FALSE") || lbl.equalsIgnoreCase("F") || lbl.contains("false")) hasFalse = true;
        }

        assertTrue("Expected TRUE outgoing CDG edge from loop condition", hasTrue);
        assertTrue("Expected FALSE outgoing CDG edge from loop condition", hasFalse);
    }

    @Test
    public void testForLoopHasTrueAndFalseCDGEdges() throws Exception {
        String code = "int foo(int n) { int i=0; for (i=0;i<n;i++) { i++; } return i; }";
        CFG cfg = buildCFGFromCode(code, "foo");

        CDG.resetNodeIds();
        CDG cdg = new CDG(cfg);

        CDGNode condNode = null;
        for (CFGNode cn : cfg.vertexSet()) {
            if (cn.getNodes() != null && !cn.getNodes().isEmpty()) {
                String raw = cn.getNodes().get(0).getRawSignature();
                if (raw != null && raw.contains("i<n")) {
                    condNode = cdg.getCDGNode(cn);
                    break;
                }
            }
        }

        assertNotNull("For-loop condition CDG node should be found", condNode);

        boolean hasT = false, hasF = false;
        for (ControlDependenceEdge e : cdg.outgoingEdgesOf(condNode)) {
            String lbl = e == null ? null : e.toString();
            if (lbl == null) continue;
            if (lbl.equalsIgnoreCase("TRUE") || lbl.equalsIgnoreCase("T") || lbl.contains("true")) hasT = true;
            if (lbl.equalsIgnoreCase("FALSE") || lbl.equalsIgnoreCase("F") || lbl.contains("false")) hasF = true;
        }
        assertTrue("Expected TRUE outgoing CDG edge from for-loop condition", hasT);
        assertTrue("Expected FALSE outgoing CDG edge from for-loop condition", hasF);
    }

    @Test
    public void testDoWhileLoopHasTrueAndFalseCDGEdges() throws Exception {
        String code = "int foo(int n) { int i=0; do { i++; } while (i < n); return i; }";
        CFG cfg = buildCFGFromCode(code, "foo");

        CDG.resetNodeIds();
        CDG cdg = new CDG(cfg);

        CDGNode condNode = null;
        for (CFGNode cn : cfg.vertexSet()) {
            if (cn.getNodes() != null && !cn.getNodes().isEmpty()) {
                String raw = cn.getNodes().get(0).getRawSignature();
                if (raw != null && raw.contains("i < n")) {
                    condNode = cdg.getCDGNode(cn);
                    break;
                }
            }
        }

        assertNotNull("Do-while condition CDG node should be found", condNode);

        boolean hasT = false, hasF = false;
        for (ControlDependenceEdge e : cdg.outgoingEdgesOf(condNode)) {
            String lbl = e == null ? null : e.toString();
            if (lbl == null) continue;
            if (lbl.equalsIgnoreCase("TRUE") || lbl.equalsIgnoreCase("T") || lbl.contains("true")) hasT = true;
            if (lbl.equalsIgnoreCase("FALSE") || lbl.equalsIgnoreCase("F") || lbl.contains("false")) hasF = true;
        }
        assertTrue("Expected TRUE outgoing CDG edge from do-while condition", hasT);
        assertTrue("Expected FALSE outgoing CDG edge from do-while condition", hasF);
    }

    @Test
    public void testNestedLoopsProduceTrueFalseEdges() throws Exception {
        String code = "int foo(int n) { int i=0,j=0; while (i<n) { for (j=0;j<n;j++) { if (j==2) break; } i++; } return i+j; }";
        CFG cfg = buildCFGFromCode(code, "foo");

        CDG.resetNodeIds();
        CDG cdg = new CDG(cfg);

        // Check that we find at least two loop condition nodes and each has T and F
        int conditionsFound = 0;
        for (CFGNode cn : cfg.vertexSet()) {
            if (cn.getNodes() == null || cn.getNodes().isEmpty()) continue;
            String raw = cn.getNodes().get(0).getRawSignature();
            if (raw == null) continue;
            if (raw.contains("i<n") || raw.contains("j<n")) {
                CDGNode cdgn = cdg.getCDGNode(cn);
                assertNotNull(cdgn);
                boolean hasT = false, hasF = false;
                for (ControlDependenceEdge e : cdg.outgoingEdgesOf(cdgn)) {
                    String lbl = e == null ? null : e.toString();
                    if (lbl == null) continue;
                    if (lbl.equalsIgnoreCase("TRUE") || lbl.equalsIgnoreCase("T") || lbl.contains("true")) hasT = true;
                    if (lbl.equalsIgnoreCase("FALSE") || lbl.equalsIgnoreCase("F") || lbl.contains("false")) hasF = true;
                }
                assertTrue("Expected both T and F for nested loop condition", hasT && hasF);
                conditionsFound++;
            }
        }
        assertTrue("Expected at least two loop condition nodes", conditionsFound >= 2);
    }

    @Test
    public void testLoopFalseEdgeTargetsNextSequentialStatement() throws Exception {
        String code = "int foo(int n) { int i=0; while (i<n) { i++; } int y = 5; return i; }";
        CFG cfg = buildCFGFromCode(code, "foo");

        CDG.resetNodeIds();
        CDG cdg = new CDG(cfg);

        CDGNode loopCond = null;
        CFGNode nextCfg = null;
        for (CFGNode cn : cfg.vertexSet()) {
            if (cn.getNodes() != null && !cn.getNodes().isEmpty()) {
                String raw = cn.getNodes().get(0).getRawSignature();
                if (raw != null && raw.contains("i<n")) {
                    loopCond = cdg.getCDGNode(cn);
                }
                if (raw != null && raw.contains("int y")) {
                    nextCfg = cn;
                }
            }
        }

        assertNotNull("Loop condition should be found", loopCond);
        assertNotNull("Next sequential CFG node (int y) should be found", nextCfg);

        boolean foundFalseToNext = false;
        for (ControlDependenceEdge e : cdg.outgoingEdgesOf(loopCond)) {
            String lbl = e == null ? null : e.toString();
            if (lbl == null) continue;
            if (lbl.equalsIgnoreCase("FALSE") || lbl.equalsIgnoreCase("F") || lbl.contains("false")) {
                CDGNode tgt = cdg.getEdgeTarget(e);
                if (tgt != null && tgt.getOriginalCFGNode() != null && tgt.getOriginalCFGNode().equals(nextCfg)) {
                    foundFalseToNext = true;
                    break;
                }
            }
        }

        assertTrue("Expected loop FALSE CDG edge to target the next sequential statement (int y)", foundFalseToNext);
    }
}
*/