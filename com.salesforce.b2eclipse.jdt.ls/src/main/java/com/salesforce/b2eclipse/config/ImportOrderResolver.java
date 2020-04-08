/**
 * Copyright (c) 2020, Salesforce.com, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.salesforce.b2eclipse.config;

import java.util.List;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import com.salesforce.b2eclipse.model.AspectPackageInfo;
import com.salesforce.b2eclipse.model.AspectPackageInfos;
import com.salesforce.b2eclipse.model.BazelPackageInfo;

final class ImportOrderResolver {

    private ImportOrderResolver() {

    }

    /**
     * Builds the dependency graph of modules based on the aspects info. Current solution is a subject for optimization
     * - to many iteration inside may slow down overall import performance.
     *
     * @return ordered list of modules - leaves nodes goes first, those which dependent on them next and so on up to the
     *         root module
     */
    static Iterable<BazelPackageInfo> resolveModulesImportOrder(BazelPackageInfo rootModule,
            List<BazelPackageInfo> childModules, AspectPackageInfos aspects) {

        MutableGraph<BazelPackageInfo> graph = GraphBuilder.undirected().build();

        graph.addNode(rootModule);
        for (BazelPackageInfo childPackageInfo : childModules) {
            graph.addNode(childPackageInfo);
            graph.putEdge(rootModule, childPackageInfo);
        }

        for (BazelPackageInfo childPackageInfo : childModules) {
        	AspectPackageInfo packageAspect = aspects.lookByPackageName(childPackageInfo.getBazelPackageName());
        	
        	if (packageAspect == null) {
        		throw new IllegalStateException(
        				"Package dependencies couldn't be resolved: " + childPackageInfo.getBazelPackageName());
        	}
        	 
            for (String dep : packageAspect.getDeps()) {
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
