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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Locale.ENGLISH;

public class PathMatcherTree {

    private PathMatcherTree parent = null;

    private void setParent(PathMatcherTree nParent) {
        this.parent = nParent;
    }

    // MY position with my parent.
    // Root element = agent (1)
    private final AgentPathFragment fragment;
    private final String            fragmentName;
    private final int               index;

    private EnumMap<AgentPathFragment, List<PathMatcherTree>> children = new EnumMap<>(AgentPathFragment.class);

    public PathMatcherTree(AgentPathFragment fragment, int index) {
        this.fragment = fragment;
        this.fragmentName = fragment.name().toLowerCase(ENGLISH);
        this.index = index;
    }

    private Set<MatcherAction> actions = new HashSet<>();

    public Set<MatcherAction> getActions() {
        return actions;
    }

    public void addMatcherAction(MatcherAction action) {
        actions.add(action);
    }

    public void addMatcherAction(Set<MatcherAction> newActions) {
        actions.addAll(newActions);
    }

    public PathMatcherTree getOrCreateChild(AgentPathFragment newChild, int newChildIndex) {
        List<PathMatcherTree> childrenn = children.computeIfAbsent(newChild, k -> new ArrayList<>(10));

        PathMatcherTree child = null;

        if (childrenn.size() > newChildIndex) {
            child = childrenn.get(newChildIndex);
        }
        if (child == null) {
            child = new PathMatcherTree(newChild, newChildIndex);

            // WTF: If you have an ArrayList with a capacity of N elements, you CANNOT set an element at
            // an index within this range which is higher than the number of element already in there.
            while (childrenn.size() <= newChildIndex) {
                childrenn.add(null);
                childrenn.add(null);
                childrenn.add(null);
                childrenn.add(null);
                childrenn.add(null);
            }

            childrenn.set(newChildIndex, child);
            child.setParent(this);
        }
        return child;
    }

    @Override
    public String toString() {
        if (parent == null) {
            return fragmentName;
        }
        return parent.toString() + ".(" + index + ")" + fragmentName;
    }

    public List<String> getChildrenStrings() {
        List<String> results = new ArrayList<>();

        if (children.isEmpty()) {
            results.add(fragmentName);
        } else {
            for (Map.Entry<AgentPathFragment, List<PathMatcherTree>> childrenPerType : children.entrySet()) {
                List<PathMatcherTree> xx = childrenPerType.getValue();
                for (int childIndex = 1; childIndex < xx.size(); childIndex++) {
                    PathMatcherTree child = xx.get(childIndex);
                    if (child != null) {
                        List<String> childStrings = child.getChildrenStrings();
                        for (String childString : childStrings) {
                            results.add(fragmentName + ".(" + childIndex + ")" + childString);
                        }
                    }
                }
            }
        }

        return results;
    }

}
