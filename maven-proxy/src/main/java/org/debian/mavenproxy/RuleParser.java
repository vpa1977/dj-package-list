package org.debian.mavenproxy;

import org.debian.maven.repo.DependencyRule;
import org.debian.maven.repo.DependencyRuleSet;

import java.util.List;


public class RuleParser {
    public static DependencyRuleSet parseRules(List<String> rules, String description) {
        DependencyRuleSet ruleSet = new DependencyRuleSet(description);
        if (rules == null) {
            return ruleSet;
        }
        for (var rule : rules) {
            ruleSet.add(new DependencyRule(rule));
        }
        return ruleSet;
    }
}
