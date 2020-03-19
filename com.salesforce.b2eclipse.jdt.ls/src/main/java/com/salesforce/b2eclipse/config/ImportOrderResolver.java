package com.salesforce.b2eclipse.config;

import java.util.List;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import com.salesforce.b2eclipse.model.AspectPackageInfos;
import com.salesforce.b2eclipse.model.BazelPackageInfo;

final class ImportOrderResolver {

	/**
	 * Builds the dependency graph of modules based on the aspects info.
	 * Current solution is a subject for optimization - to many iteration inside may slow down overall import performance.
     *
     * @return ordered list of modules - leaves nodes goes first, those which dependent on them next 
     * 	and so on up to the root module
     */
	static Iterable<BazelPackageInfo> resolveModulesImportOrder(BazelPackageInfo rootModule, List<BazelPackageInfo> childModules, AspectPackageInfos aspects) {

        MutableGraph<BazelPackageInfo> graph = GraphBuilder.undirected().build();
        
        graph.addNode(rootModule);
        for (BazelPackageInfo childPackageInfo : childModules) {
        	graph.addNode(childPackageInfo);
        	graph.putEdge(rootModule, childPackageInfo);
        }

        for (BazelPackageInfo childPackageInfo : childModules) {
        	for (String dep : aspects.lookByPackageName(childPackageInfo.getBazelPackageName()).getDeps()) {
                for (BazelPackageInfo candidateNode : childModules) {
                	if (dep.startsWith(candidateNode.getBazelPackageName()) && childPackageInfo != candidateNode) {
                		graph.putEdge(childPackageInfo, candidateNode);
                	}
                }
        	}
        }
        
        Iterable<BazelPackageInfo> postOrderedModules = Traverser.forGraph(graph).depthFirstPostOrder(rootModule);
        
        return postOrderedModules;
        
	}

}
