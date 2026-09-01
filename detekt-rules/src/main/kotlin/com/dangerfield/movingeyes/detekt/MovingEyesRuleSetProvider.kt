package com.dangerfield.movingeyes.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * Registers the project's custom rule set. Discovered by detekt via the
 * `META-INF/services/dev.detekt.api.RuleSetProvider` entry. The rule set id
 * (`movingeyes`) namespaces the rules in `detekt.yml`.
 */
class MovingEyesRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("movingeyes")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::VerifyStrings,
        ),
    )
}
