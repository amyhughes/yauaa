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

package nl.basjes.parse.useragent.analyze;

import nl.basjes.parse.useragent.analyze.WordRangeVisitor.Range;
import nl.basjes.parse.useragent.analyze.treewalker.TreeExpressionEvaluator;
import nl.basjes.parse.useragent.analyze.treewalker.steps.WalkList.WalkResult;
import nl.basjes.parse.useragent.parse.AgentPathFragment;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerBaseVisitor;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerLexer;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherBaseContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherConcatContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherConcatPostfixContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherConcatPrefixContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherExtractContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherNormalizeBrandContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherPathIsInLookupPrefixContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherPathLookupPrefixContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherVariableContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherWordRangeContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.PathVariableContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepContainsValueContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownAgentContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownBase64Context;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownCommentsContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownEmailContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownEntryContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownKeyContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownKeyvalueContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownNameContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownProductContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownTextContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownUrlContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownUuidContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownValueContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepDownVersionContext;
import nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepWordRangeContext;
import nl.basjes.parse.useragent.utils.DefaultANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nl.basjes.parse.useragent.analyze.NumberRangeVisitor.NUMBER_RANGE_VISITOR;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.AGENT;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.BASE64;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.COMMENTS;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.EMAIL;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.ENTRY;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.KEY;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.KEYVALUE;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.NAME;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.PRODUCT;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.TEXT;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.URL;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.UUID;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.VALUE;
import static nl.basjes.parse.useragent.parse.AgentPathFragment.VERSION;
import static nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherCleanVersionContext;
import static nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherPathContext;
import static nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherPathIsNullContext;
import static nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.MatcherPathLookupContext;
import static nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.PathFixedValueContext;
import static nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.PathWalkContext;
import static nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepEndsWithValueContext;
import static nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepEqualsValueContext;
import static nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepNotEqualsValueContext;
import static nl.basjes.parse.useragent.parser.UserAgentTreeWalkerParser.StepStartsWithValueContext;

public abstract class MatcherAction implements Serializable {

    private   String                  matchExpression;
    protected TreeExpressionEvaluator evaluator;

    TreeExpressionEvaluator getEvaluatorForUnitTesting() {
        return evaluator;
    }

    private static final Logger LOG = LoggerFactory.getLogger(MatcherAction.class);


    private Matcher     matcher;
    private MatchesList matches;
    private boolean     mustHaveMatches = false;

    boolean mustHaveMatches() {
        return mustHaveMatches;
    }

    boolean verbose = false;
    private boolean verbosePermanent = false;
    private boolean verboseTemporary = false;

    private void setVerbose(boolean newVerbose) {
        setVerbose(newVerbose, false);
    }

    public void setVerbose(boolean newVerbose, boolean temporary) {
        this.verbose = newVerbose;
        if (!temporary) {
            this.verbosePermanent = newVerbose;
        }
        this.verboseTemporary = temporary;
    }

    public String getMatchExpression() {
        return matchExpression;
    }

    class InitErrorListener implements DefaultANTLRErrorListener {
        @Override
        public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e) {
            LOG.error("Syntax error");
            LOG.error("Source : {}", matchExpression);
            LOG.error("Message: {}", msg);
            throw new InvalidParserConfigurationException("Syntax error \"" + msg + "\" caused by \"" + matchExpression + "\".");
        }

        // Ignore all other types of problems
    }

    void init(String newMatchExpression, Matcher newMatcher) {
        this.matcher = newMatcher;
        this.matchExpression = newMatchExpression;
        setVerbose(newMatcher.getVerbose());
    }


    public void initialize() {
        InitErrorListener errorListener = new InitErrorListener();

        CodePointCharStream      input = CharStreams.fromString(this.matchExpression);
        UserAgentTreeWalkerLexer lexer = new UserAgentTreeWalkerLexer(input);

        lexer.addErrorListener(errorListener);

        CommonTokenStream         tokens = new CommonTokenStream(lexer);
        UserAgentTreeWalkerParser parser = new UserAgentTreeWalkerParser(tokens);

        parser.addErrorListener(errorListener);

        ParserRuleContext requiredPattern = parseWalkerExpression(parser);

        // We couldn't ditch the double quotes around the fixed values in the parsing phase.
        // So we ditch them here. We simply walk the tree and modify some of the tokens.
        new UnQuoteValues().visit(requiredPattern);

        // Now we create an evaluator instance
        evaluator = new TreeExpressionEvaluator(requiredPattern, matcher, verbose);

        // Is a fixed value (i.e. no events will ever be fired)?
        String fixedValue = evaluator.getFixedValue();
        if (fixedValue != null) {
            setFixedValue(fixedValue);
            mustHaveMatches = false;
            matches = new MatchesList(0);
            return; // Not interested in any patterns
        }

        mustHaveMatches = !evaluator.usesIsNull();

        // TESTING CODE:
        List<String> testInformPath = new ArrayList<>();

        testInformPath.add("N:"+AGENT);
        int informs = calculateInformPath(this, testInformPath, requiredPattern);

        // If this is based on a variable we do not need any matches from the hashmap.
        if (mustHaveMatches && informs == 0) {
            mustHaveMatches = false;
        }

        int listSize = 0;
        if (informs > 0) {
            listSize = 1;
        }
        this.matches = new MatchesList(listSize);
    }

    protected abstract ParserRuleContext parseWalkerExpression(UserAgentTreeWalkerParser parser);

    private static class UnQuoteValues extends UserAgentTreeWalkerBaseVisitor<Void> {
        private void unQuoteToken(Token token) {
            if (token instanceof CommonToken) {
                CommonToken commonToken = (CommonToken) token;
                commonToken.setStartIndex(commonToken.getStartIndex() + 1);
                commonToken.setStopIndex(commonToken.getStopIndex() - 1);
            }
        }

        @Override
        public Void visitMatcherPathLookup(MatcherPathLookupContext ctx) {
            unQuoteToken(ctx.defaultValue);
            return super.visitMatcherPathLookup(ctx);
        }

        @Override
        public Void visitMatcherPathLookupPrefix(MatcherPathLookupPrefixContext ctx) {
            unQuoteToken(ctx.defaultValue);
            return super.visitMatcherPathLookupPrefix(ctx);
        }

        @Override
        public Void visitPathFixedValue(PathFixedValueContext ctx) {
            unQuoteToken(ctx.value);
            return super.visitPathFixedValue(ctx);
        }

        @Override
        public Void visitMatcherConcat(MatcherConcatContext ctx) {
            unQuoteToken(ctx.prefix);
            unQuoteToken(ctx.postfix);
            return super.visitMatcherConcat(ctx);
        }

        @Override
        public Void visitMatcherConcatPrefix(MatcherConcatPrefixContext ctx) {
            unQuoteToken(ctx.prefix);
            return super.visitMatcherConcatPrefix(ctx);
        }

        @Override
        public Void visitMatcherConcatPostfix(MatcherConcatPostfixContext ctx) {
            unQuoteToken(ctx.postfix);
            return super.visitMatcherConcatPostfix(ctx);
        }

        @Override
        public Void visitStepEqualsValue(StepEqualsValueContext ctx) {
            unQuoteToken(ctx.value);
            return super.visitStepEqualsValue(ctx);
        }

        @Override
        public Void visitStepNotEqualsValue(StepNotEqualsValueContext ctx) {
            unQuoteToken(ctx.value);
            return super.visitStepNotEqualsValue(ctx);
        }

        @Override
        public Void visitStepStartsWithValue(StepStartsWithValueContext ctx) {
            unQuoteToken(ctx.value);
            return super.visitStepStartsWithValue(ctx);
        }

        @Override
        public Void visitStepEndsWithValue(StepEndsWithValueContext ctx) {
            unQuoteToken(ctx.value);
            return super.visitStepEndsWithValue(ctx);
        }

        @Override
        public Void visitStepContainsValue(StepContainsValueContext ctx) {
            unQuoteToken(ctx.value);
            return super.visitStepContainsValue(ctx);
        }
    }

    protected abstract void setFixedValue(String newFixedValue);

    /**
     * For each key that this action wants to be notified for this method is called.
     * Note that on a single parse event the same name CAN be called multiple times!!
     *
     * @param key    The key of the node
     * @param value  The value that was found
     * @param result The node in the parser tree where the match occurred
     */
    public void inform(String key, String value, ParseTree result) {
        matcher.receivedInput();

        // Only if this needs input we tell the matcher on the first one.
        if (mustHaveMatches && matches.isEmpty()) {
            matcher.gotMyFirstStartingPoint();
        }
        matches.add(key, value, result);
    }

    protected abstract void inform(String key, WalkResult foundValue);

    /**
     * @return If it is impossible that this can be valid it returns true, else false.
     */
    boolean cannotBeValid() {
        return mustHaveMatches == matches.isEmpty();
    }

    /**
     * Called after all nodes have been notified.
     *
     * @return true if the obtainResult result was valid. False will fail the entire matcher this belongs to.
     */
    public abstract boolean obtainResult();

    boolean isValidIsNull() {
        return matches.isEmpty() && evaluator.usesIsNull();
    }

    /**
     * Optimization: Only if there is a possibility that all actions for this matcher CAN be valid do we
     * actually perform the analysis and do the (expensive) tree walking and matching.
     */
    void processInformedMatches() {
        for (MatchesList.Match match : matches) {
            WalkResult matchedValue = evaluator.evaluate(match.getResult(), match.getKey(), match.getValue());
            if (matchedValue != null) {
                inform(match.getKey(), matchedValue);
                break; // We always stick to the first match
            }
        }
    }

    // ============================================================================================================

    @FunctionalInterface
    public interface CalculateInformPathFunction {
        /**
         * Applies this function to the given arguments.
         *
         * @param treePath The name of the current tree.
         * @param tree     The actual location in the parseTree
         * @return the number of informs done
         */
        int calculateInformPath(MatcherAction action, List<String> treePath, ParserRuleContext tree);
    }

    private static final Map<Class, CalculateInformPathFunction> CALCULATE_INFORM_PATH = new HashMap<>();

    static {
        // -------------
        CALCULATE_INFORM_PATH.put(MatcherBaseContext.class,             (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherBaseContext) tree).matcher()));
        CALCULATE_INFORM_PATH.put(MatcherPathIsNullContext.class,       (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherPathIsNullContext) tree).matcher()));

        // -------------
        CALCULATE_INFORM_PATH.put(MatcherExtractContext.class,          (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherExtractContext) tree).expression));

        // -------------
        CALCULATE_INFORM_PATH.put(MatcherVariableContext.class,         (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherVariableContext) tree).expression));

        // -------------
        CALCULATE_INFORM_PATH.put(MatcherPathContext.class,             (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherPathContext) tree).basePath()));
        CALCULATE_INFORM_PATH.put(MatcherConcatContext.class,           (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherConcatContext) tree).matcher()));
        CALCULATE_INFORM_PATH.put(MatcherConcatPrefixContext.class,     (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherConcatPrefixContext) tree).matcher()));
        CALCULATE_INFORM_PATH.put(MatcherConcatPostfixContext.class,    (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherConcatPostfixContext) tree).matcher()));
        CALCULATE_INFORM_PATH.put(MatcherNormalizeBrandContext.class,   (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherNormalizeBrandContext) tree).matcher()));
        CALCULATE_INFORM_PATH.put(MatcherCleanVersionContext.class,     (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherCleanVersionContext) tree).matcher()));
        CALCULATE_INFORM_PATH.put(MatcherPathLookupContext.class,       (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherPathLookupContext) tree).matcher()));
        CALCULATE_INFORM_PATH.put(MatcherPathLookupPrefixContext.class,       (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherPathLookupPrefixContext) tree).matcher()));
        CALCULATE_INFORM_PATH.put(MatcherPathIsInLookupPrefixContext.class,       (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherPathIsInLookupPrefixContext) tree).matcher()));
        CALCULATE_INFORM_PATH.put(MatcherWordRangeContext.class,        (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((MatcherWordRangeContext) tree).matcher()));

        // -------------
        CALCULATE_INFORM_PATH.put(PathVariableContext.class, (action, treePath, tree) -> {
            LOG.info("Need variable {}", ((PathVariableContext) tree).variable.getText());
//            action.matcher.informMeAboutVariable(action, ((PathVariableContext) tree).variable.getText();
            return 0;
        });

        CALCULATE_INFORM_PATH.put(PathWalkContext.class, (action, treePath, tree) ->
            calculateInformPath(action, treePath, ((PathWalkContext) tree).nextStep));

        // -------------
        CALCULATE_INFORM_PATH.put(StepDownAgentContext.class, (action, treePath, tree) -> {
            StepDownAgentContext thisTree = ((StepDownAgentContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(AGENT.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });

        CALCULATE_INFORM_PATH.put(StepDownProductContext.class, (action, treePath, tree) -> {
            StepDownProductContext thisTree = ((StepDownProductContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(PRODUCT.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });

        CALCULATE_INFORM_PATH.put(StepDownNameContext.class, (action, treePath, tree) -> {
            StepDownNameContext thisTree = ((StepDownNameContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(NAME.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });

        CALCULATE_INFORM_PATH.put(StepDownVersionContext.class, (action, treePath, tree) -> {
            StepDownVersionContext thisTree = ((StepDownVersionContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(VERSION.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });

        CALCULATE_INFORM_PATH.put(StepDownCommentsContext.class, (action, treePath, tree) -> {
            StepDownCommentsContext thisTree = ((StepDownCommentsContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(COMMENTS.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });


        CALCULATE_INFORM_PATH.put(StepDownEntryContext.class, (action, treePath, tree) -> {
            StepDownEntryContext thisTree = ((StepDownEntryContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(ENTRY.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });

        CALCULATE_INFORM_PATH.put(StepDownTextContext.class, (action, treePath, tree) -> {
            StepDownTextContext thisTree = ((StepDownTextContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(TEXT.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });


        CALCULATE_INFORM_PATH.put(StepDownUrlContext.class, (action, treePath, tree) -> {
            StepDownUrlContext thisTree = ((StepDownUrlContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(URL.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });


        CALCULATE_INFORM_PATH.put(StepDownEmailContext.class, (action, treePath, tree) -> {
            StepDownEmailContext thisTree = ((StepDownEmailContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(EMAIL.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });

        CALCULATE_INFORM_PATH.put(StepDownBase64Context.class, (action, treePath, tree) -> {
            StepDownBase64Context thisTree = ((StepDownBase64Context) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(BASE64.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });

        CALCULATE_INFORM_PATH.put(StepDownUuidContext.class, (action, treePath, tree) -> {
            StepDownUuidContext thisTree = ((StepDownUuidContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(UUID.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });

        CALCULATE_INFORM_PATH.put(StepDownKeyvalueContext.class, (action, treePath, tree) -> {
            StepDownKeyvalueContext thisTree = ((StepDownKeyvalueContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(KEYVALUE.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });


        CALCULATE_INFORM_PATH.put(StepDownKeyContext.class, (action, treePath, tree) -> {
            StepDownKeyContext thisTree = ((StepDownKeyContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(KEY.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });


        CALCULATE_INFORM_PATH.put(StepDownValueContext.class, (action, treePath, tree) -> {
            StepDownValueContext thisTree = ((StepDownValueContext) tree);
            treePath.add(NUMBER_RANGE_VISITOR.visit(thisTree.numberRange()).toString());
            treePath.add(VALUE.toString());
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });

        CALCULATE_INFORM_PATH.put(StepEqualsValueContext.class,         (action, treePath, tree) -> {
            StepEqualsValueContext thisTree = ((StepEqualsValueContext)tree);
            LOG.info("Want {}={}", treePath, thisTree.value.getText());
            //            action.matcher.informMeAbout(action, treePath + "=\"" + thisTree.value.getText() + "\"");
            return 1;
        });

        CALCULATE_INFORM_PATH.put(StepStartsWithValueContext.class,     (action, treePath, tree) -> {
            StepStartsWithValueContext thisTree = ((StepStartsWithValueContext)tree);
            LOG.info("Want {} startsWith {}", treePath, thisTree.value.getText());
//            action.matcher.informMeAboutPrefix(action, treePath, thisTree.value.getText());
            return 1;
        });

        CALCULATE_INFORM_PATH.put(StepWordRangeContext.class,           (action, treePath, tree) -> {
            StepWordRangeContext thisTree = ((StepWordRangeContext)tree);
            Range range = WordRangeVisitor.getRange(thisTree.wordRange());
            LOG.info("Want {} startsWith {}", treePath, range);
//            action.matcher.lookingForRange(treePath, range);
            treePath.add("Range: "+ range);
            return calculateInformPath(action, treePath, thisTree.nextStep);
        });
    }

    private static int calculateInformPath(MatcherAction action, List<String> treePath, ParserRuleContext tree) {
        if (tree == null) {
            LOG.info("Have: {}", treePath);
//            action.matcher.informMeAbout(action, treePath);
            return 1;
        }

        CalculateInformPathFunction function = CALCULATE_INFORM_PATH.get(tree.getClass());
        if (function != null) {
            return function.calculateInformPath(action, treePath, tree);
        }

        LOG.info("Want : {}", treePath);
//        action.matcher.informMeAbout(action, treePath);
        return 1;
    }

    // ============================================================================================================

    public void reset() {
        matches.clear();
        if (verboseTemporary) {
            verbose = verbosePermanent;
        }
    }

    public MatchesList getMatches() {
        return matches;
    }
}
