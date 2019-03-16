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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Locale.ENGLISH;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.EQUALS;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.STARTSWITH;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.WORDRANGE;

public class PathMatcherTree implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(PathMatcherTree.class);

    private PathMatcherTree parent = null;

    private void setParent(PathMatcherTree nParent) {
        this.fragmentName = ".(" + index + ")" + fragment.name().toLowerCase(ENGLISH);
        this.parent = nParent;
    }

    // MY position with my parent.
    // Root element = agent (1)
    private final AgentPathFragment fragment;
    private String            fragmentName;
    private final int               index;

    // TODO: A tree structure with all words separately
    // IFF fragment == WORDRANGE
    private int firstWord = -1;
    private int lastWord = -1;

    // IFF fragment == EQUALS or STARTSWITH
    private String matchString = "";

    private EnumMap<AgentPathFragment, List<PathMatcherTree>> children = new EnumMap<>(AgentPathFragment.class);

    public PathMatcherTree(AgentPathFragment fragment, int index) {
        this.fragment = fragment;
        this.fragmentName = fragment.name().toLowerCase(ENGLISH);
        this.index = index;
    }

    public void makeItWordRange(int nFirstWord, int nLastWord) {
        if (this.fragment != WORDRANGE) {
            throw new IllegalArgumentException("When you specify a first/last word it MUST be WORDRANGE");
        }
        this.firstWord = nFirstWord;
        this.lastWord = nLastWord;
        this.fragmentName = "[" + firstWord + "-" + lastWord + "]";
    }

    public void makeItEquals(String nMatchString) {
        if (fragment != EQUALS) {
            throw new IllegalArgumentException("When you specify a matchString it MUST be either EQUALS or STARTSWITH 1"); // FIXME
        }
        this.matchString= nMatchString;
        fragmentName = "=\"" + matchString + "\"";
//        LOG.warn("Setting equals to path {}", this);
    }

    public void makeItStartsWith(String nMatchString) {
        if (fragment != STARTSWITH) {
            throw new IllegalArgumentException("When you specify a matchString it MUST be either EQUALS or STARTSWITH 2 "); // FIXME
        }
        this.matchString= nMatchString;
        fragmentName = "{\"" + matchString + "\"";
//        LOG.warn("Setting startWith to path {}", this);
    }

    private Set<MatcherAction> actions = new HashSet<>();

    public Set<MatcherAction> getActions() {
        return actions;
    }

    public void addMatcherAction(MatcherAction action) {
        actions.add(action);
//        LOG.warn("Adding to path \"{}\" action:    {}", this, action.getMatchExpression());
    }

//    public void addMatcherAction(Set<MatcherAction> newActions) {
//        actions.addAll(newActions);
//    }

    public PathMatcherTree getOrCreateChild(AgentPathFragment newChild, int newChildIndex) {
//        LOG.info("[getOrCreateChild]>: {} --- {} --- {}", this, newChild, newChildIndex);
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
//        LOG.info("[getOrCreateChild]<: {}", child);
        return child;
    }

    public PathMatcherTree getChild(AgentPathFragment child, int childIndex) {
        List<PathMatcherTree> childList = children.get(child);
        if (childList == null) {
            return null;
        }
        return childList.get(childIndex);
    }

    @Override
    public String toString() {
        if (parent == null) {
            return fragmentName;
        }
        return parent.toString() + fragmentName;
    }

    public List<String> getChildrenStrings() {
        List<String> results = new ArrayList<>();

        if (children.isEmpty()) {
            results.add(fragmentName);
        } else {
            for (Map.Entry<AgentPathFragment, List<PathMatcherTree>> childrenPerType : children.entrySet()) {
                for (PathMatcherTree child : childrenPerType.getValue()) {
                    if (child != null) {
                        final String fn = fragmentName;
                        child.getChildrenStrings().forEach(cs -> results.add(fn + cs));
                    }
                }
            }
        }
        return results;
    }

    public boolean verifyTree() {
        if (parent != null) {
            List<PathMatcherTree> siblings = parent.children.get(fragment);
            if (siblings.size() <= index) {
                LOG.error("Verify FAIL: Parent does not have enough siblings: {}", this);
                return false;
            }
            PathMatcherTree thisShouldBeThis = siblings.get(index);
            if (thisShouldBeThis == null || thisShouldBeThis != this) {
                LOG.error("Verify FAIL: Parent ({}) has the wrong {} child at {} for {}", parent, fragment, index, this);
                return false;
            }
        }

        for (Map.Entry<AgentPathFragment, List<PathMatcherTree>> childrenPerType : children.entrySet()) {
            List<PathMatcherTree> childrenPerTypeValue = childrenPerType.getValue();
            for (PathMatcherTree child : childrenPerTypeValue) {
                if (child != null) {
                    if (!child.verifyTree()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
