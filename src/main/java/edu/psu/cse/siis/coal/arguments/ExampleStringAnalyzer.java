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
import java.util.Set;

import soot.G;
import soot.Local;
import soot.PackManager;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;

class ExampleStringAnalyzer extends SceneTransformer {

  private static final boolean USE_SHIMPLE = false;

  /*
   * Find the calls to this 'System.out.println' function, and figure out the language for its
   * argument
   */
  public static void main(String[] args) {
    G.v().out.println("in ExampleStringAnalyzer.main");
    Options.v().setPhaseOption("cg.spark", "on");
    Options.v().setPhaseOption("jb.ulp", "off");
    Options.v().setPhaseOption("jb.uce", "remove-unreachable-traps:true");
    if (USE_SHIMPLE) {
      Options.v().set_via_shimple(true);
      Options.v().set_whole_shimple(true);
    }
    String pack = USE_SHIMPLE ? "wstp" : "wjtp";
    PackManager.v().getPack(pack)
        .add(new Transform(pack + ".ExampleStringAnalyzer", new ExampleStringAnalyzer()));

    CCExprVisitor.verbose_level = 20;
    CCVisitor.verbose_level = 20;
    DBG.verbose_level = 20;

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
          // return !class_name.contains("dummy");
        }
      });
    }

    for (SootClass sootClass : Scene.v().getApplicationClasses()) {
      for (SootMethod method : sootClass.getMethods()) {
        G.v().out.println("DBG:SEEN:" + method);
      }
    }
    // Now use the collector to perform the analysis
    for (SootClass sootClass : Scene.v().getApplicationClasses()) {
      for (SootMethod method : sootClass.getMethods()) {
        // A ConstraintCollector collects constraints for the languages of string
        // variables
        ConstraintCollector cc =
            new ConstraintCollector(new ExceptionalUnitGraph(method.retrieveActiveBody()));

        Iterator<Unit> unitIt = method.getActiveBody().getUnits().snapshotIterator();
        G.v().out.println("DO internalTransform for " + method);
        G.v().out.println("BODY " + method.getActiveBody());
        while (unitIt.hasNext()) {
          Unit s = unitIt.next();
          handleStatement(s, cc, method);
        }
        // p2dbg(method);
      }
    }
    // ///////////////
    /*
     * SootMethod sm1 = Scene.v().getMethod("<dummy: java.lang.String testExceptionHelper()>");
     * G.v().out.println("DBG:METHOD:sm1" + sm1); SootMethod sm2 =
     * Scene.v().getMethod("<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>"
     * ); G.v().out.println("DBG:METHOD:sm2" + sm2); G.v().out.println("DBG:METHOD:sm2:body:" +
     * sm2.getActiveBody());
     */
  }

  static boolean dbg_hack_force = false;
  static boolean dbg_hack = false;

  // Look for calls to 'println' and report the set of strings for the parameter.
  static void handleStatement(Unit u, ConstraintCollector cc, SootMethod method) {
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
          G.v().out.println("DAG-VISITOR:RES:\t\t=======>\t\t" + dagvlc.result);
          if (dbg_hack_force && dbg_hack && dagvlc.result.isEmpty()) {
            // int dbg=10;
            // while(dbg==10) {
            G.v().out.println("EMPTY");
            dagvlc.solve(lcb);
            String str_lcb = lcb.toString();
            G.v().out.println("DAG-VISITOR:lcb:\t\t=======>\t\t" + str_lcb);
            // }
          }
        }
        String str_lcb = lcb.toString();
        G.v().out.println("DAG-VISITOR:lcb:\t\t=======>\t\t" + str_lcb);
      }
    }
  }

  static void p2dbg(SootMethod method) {
    Iterator localsIt = method.getActiveBody().getLocals().iterator();
    G.v().out.println(">>>>>> p2 locals of" + method);
    while (localsIt.hasNext()) {
      Local l = (Local) localsIt.next();
      p2dbg(l, method);
    }
    G.v().out.println("<<<<<< p2 locals of" + method);
  }

  static void p2dbg(Value v, SootMethod current_sm) {
    if (v instanceof Local) {
      Local l = (Local) v;
      // PointsToSet set1 = Scene.v().getPointsToAnalysis().reachingObjects(l1);
      // PointsToSet set2 = Scene.v().getPointsToAnalysis().reachingObjects(l2);
      // set1.hasNonEmptyIntersection(set2);
      // LocalMustAliasAnalysis
      PointsToAnalysis pta = Scene.v().getPointsToAnalysis();
      PointsToSet set = pta.reachingObjects(l);
      Set<String> strConstants = set.possibleStringConstants();
      G.v().out.println(strConstants);
      G.v().out.println("PossibleTypes:" + set.possibleTypes());

      Iterator localsIt = current_sm.getActiveBody().getLocals().iterator();
      while (localsIt.hasNext()) {
        Local l2 = (Local) localsIt.next();
        PointsToSet set2 = Scene.v().getPointsToAnalysis().reachingObjects(l2);
        // G.v().out.println("PossibleTypes2:"+set2.possibleTypes());
        if (set.hasNonEmptyIntersection(set2)) {
          G.v().out.println(l + "<-->" + l2);
          Set<String> strConstants2 = set2.possibleStringConstants();
          G.v().out.println(strConstants2);
        }
      }
    }
  }
}
