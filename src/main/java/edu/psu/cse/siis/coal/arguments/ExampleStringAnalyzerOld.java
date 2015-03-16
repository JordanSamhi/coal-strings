/*
 * Copyright (C) 2015 The University of Wisconsin and the Pennsylvania State University
 *
 * Author: Daniel Luchaup
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.psu.cse.siis.coal.arguments;

import java.util.Iterator;
import java.util.Map;

import soot.G;
import soot.Local;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class ExampleStringAnalyzerOld extends SceneTransformer {

  private static final boolean USE_SHIMPLE = false;

  /*
   * Find the calls to this 'System.out.println' function, and figure out the language for its
   * argument
   */
  public static void main(String[] args) {
    G.v().out.println("in ExampleStringAnalyzer.main");
    Options.v().setPhaseOption("cg.spark", "on");
    Options.v().setPhaseOption("jb.ulp", "off");
    if (USE_SHIMPLE) {
      Options.v().set_via_shimple(true);
      Options.v().set_whole_shimple(true);
    }
    String pack = USE_SHIMPLE ? "wstp" : "wjtp";
    PackManager.v().getPack(pack)
        .add(new Transform(pack + ".ExampleStringAnalyzer", new ExampleStringAnalyzerOld()));
    soot.Main.main(args);
  }

  @Override
  protected void internalTransform(String phase, Map options) {
    boolean want_global_analysis = true;
    if (want_global_analysis) {
      /*
       * Call this for global/whole program analysis IMPORTANT: (1) The argument must implement the
       * ConstraintCollector.ModelInterface to decide what classes are excluded from the global
       * analysis. (2) ConstraintCollector.globalCollection must only be called once! Subsequent
       * calls will silently be ignored, and the cached values from the first call (i.e. using the
       * first model) will be used.
       */

      ConstraintCollector.globalCollection(new ConstraintCollector.CCModelInterface() {
        @Override
        public boolean isExcludedClass(String class_name) {
          return class_name.startsWith("sun.") || class_name.startsWith("java.")
              || class_name.startsWith("com.") || class_name.startsWith("org.");
        }
      });
    }

    for (SootClass sootClass : Scene.v().getApplicationClasses()) {
      for (SootMethod method : sootClass.getMethods()) {
        G.v().out.println("DBG:SEEN:" + method);
      }
    }

    CallGraph cg = Scene.v().getCallGraph();

    Iterator it = cg.listener();
    while (it.hasNext()) {
      soot.jimple.toolkits.callgraph.Edge e = (soot.jimple.toolkits.callgraph.Edge) it.next();
      System.out.println("" + e.src() + e.srcStmt() + " =" + e.kind() + "=> " + e.tgt());
    }

    System.out.println(Scene.v().getMethod("<StringProblems: void main(java.lang.String[])>")
        .getActiveBody());

    // Now use the collector to perform the analysis
    for (SootClass sootClass : Scene.v().getApplicationClasses()) {
      for (SootMethod method : sootClass.getMethods()) {
        // A ConstraintCollector collects constraints for the languages of string
        // variables
        ConstraintCollector cc =
            new ConstraintCollector(new ExceptionalUnitGraph(method.retrieveActiveBody()));

        Iterator<Unit> unitIt = method.getActiveBody().getUnits().snapshotIterator();
        G.v().out.println("in ConstraintCollector for " + method);
        while (unitIt.hasNext()) {
          Unit s = unitIt.next();
          handleStatement(s, cc);
        }
      }
    }
  }

  // Look for calls to 'println' and report the set of strings for the parameter.
  static void handleStatement(Unit u, ConstraintCollector cc) {
    Stmt s = (Stmt) u;
    if (!(s.containsInvokeExpr()))
      return;
    InvokeExpr iexpr = s.getInvokeExpr();
    SootMethod sm = iexpr.getMethod();
    String m_signature = sm.getSignature();
    if (m_signature.equals("<java.io.PrintStream: void println(java.lang.String)>")) {
      // OK, so this is a call to copy1
      Value v = iexpr.getArg(0);
      if (v instanceof Local) {
        Local l = (Local) v;
        // get the constraints for 'l'
        LanguageConstraints.Box lcb = cc.getConstraintOfAt(l, s);

        /*
         * Now, in theory we can use any one of the available solvers ...
         * 
         * In particular, DAGSolverVisitorLC is a simple solver (and the only one available for now)
         * that only works if there are no recursive constraints (ex. in a loop) and the set is
         * finite.
         */
        // DAGSolverVisitorLC dagvlc = new DAGSolverVisitorLC();
        RecursiveDAGSolverVisitorLC dagvlc = new RecursiveDAGSolverVisitorLC(5);
        if (dagvlc.solve(lcb)) {
          /*
           * dagvlc.result is a set of strings which can contain .* for unknown substrings.
           */
          G.v().out.println("DAG-VISITOR:RES:\t\t=======>\t\t" + dagvlc.result.size()
              + " expressions: " + dagvlc.result);
        }
        G.v().out.println("DAG-VISITOR:lcb:\t\t=======>\t\t" + lcb);
      }
    }
  }
}
