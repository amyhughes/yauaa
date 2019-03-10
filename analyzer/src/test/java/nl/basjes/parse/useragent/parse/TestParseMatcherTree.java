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


import static nl.basjes.parse.useragent.parse.AgentPathFragment.AGENT;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.PRODUCT;

public class TestParseMatcherTree {

    @Test
    public void developFakeTest() throws UselessMatcherException {

        UserAgentAnalyzerDirect userAgentAnalyzerDirect = UserAgentAnalyzerDirect.newBuilder().build();
        Matcher         matcher = new Matcher(userAgentAnalyzerDirect);
        MatcherAction   action  = new MatcherRequireAction("agent.product.name", matcher);

        PathMatcherTree root    = new PathMatcherTree(AGENT, 1);

        root.addMatcherAction(action);
        PathMatcherTree child1 = root.addChild(PRODUCT);
        child1.addMatcherAction(action);
        PathMatcherTree child2 = root.addChild(PRODUCT);
        child2.addMatcherAction(action);
    }

}
