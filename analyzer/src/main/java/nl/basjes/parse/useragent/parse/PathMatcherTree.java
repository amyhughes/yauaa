/*
 * Yet Another UserAgent Analyzer
 * Copyright (C) 2013-2019 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.parse.useragent.parse;

import nl.basjes.parse.useragent.analyze.MatcherAction;

import java.util.Set;
import java.util.TreeSet;

public class PathMatcherTree {

    // - AgentPathFragment - index -->

    private PathMatcherTree parent = null;

    // MY position with my parent.
    // Root element = agent (1)
    private final AgentPathFragment fragment;
    private final int index;

    private PathMatcherTree[][] subtree;

    public PathMatcherTree(AgentPathFragment fragment, int index) {
        this.fragment = fragment;
        this.index = index;
    }

    private Set<MatcherAction> actions = new TreeSet<>();

}
