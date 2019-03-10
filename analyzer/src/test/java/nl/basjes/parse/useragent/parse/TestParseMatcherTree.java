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

import nl.basjes.parse.useragent.UserAgentAnalyzerDirect;
import nl.basjes.parse.useragent.analyze.Matcher;
import nl.basjes.parse.useragent.analyze.MatcherAction;
import nl.basjes.parse.useragent.analyze.MatcherRequireAction;
import nl.basjes.parse.useragent.analyze.UselessMatcherException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.basjes.parse.useragent.parse.AgentPathFragment.AGENT;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.COMMENTS;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.ENTRY;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.PRODUCT;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.VERSION;

public class TestParseMatcherTree {

    private static final Logger LOG = LoggerFactory.getLogger(TestParseMatcherTree.class);

    @Test
    public void developFakeTest() throws UselessMatcherException {

        UserAgentAnalyzerDirect userAgentAnalyzerDirect = UserAgentAnalyzerDirect.newBuilder().build();
        Matcher         matcher = new Matcher(userAgentAnalyzerDirect);
        MatcherAction   action  = new MatcherRequireAction("agent.product.name", matcher);

        PathMatcherTree root    = new PathMatcherTree(AGENT, 1);

        PathMatcherTree child1 = root
            .getOrCreateChild(PRODUCT, 1)
            .getOrCreateChild(COMMENTS, 2)
            .getOrCreateChild(ENTRY, 3)
            .getOrCreateChild(PRODUCT, 4)
            .getOrCreateChild(VERSION, 5);

        PathMatcherTree child2 = root
            .getOrCreateChild(PRODUCT, 1)
            .getOrCreateChild(COMMENTS, 2)
            .getOrCreateChild(ENTRY, 2);

        child2
            .getOrCreateChild(PRODUCT, 1)
            .getOrCreateChild(VERSION, 1);

        child2
            .getOrCreateChild(PRODUCT, 3)
            .getOrCreateChild(VERSION, 2);

//        LOG.info("1: {}", child1.toString());
//        LOG.info("2: {}", child2.toString());

        root.getChildrenStrings().forEach(s -> {
            LOG.info("==> {}", s);
        });
    }

}
