package com.dangerfield.movingeyes.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * Fails on user-facing copy hardcoded as a string literal instead of coming
 * from `stringResource(...)` (backed by `:libraries:resources`).
 *
 * A literal is copy when it reaches the screen one of two ways:
 * - passed to a DS text composable, which renders everything it is given —
 *   `Text` and its wrappers, see [TEXT_CALLEES];
 * - passed under a name that means copy whatever the callee is, see
 *   [COPY_ARGUMENT_NAMES]. This is what catches `ToolbarButton(label = "Undo")`,
 *   where the literal is one hop from the `Text` that draws it.
 *
 * Deliberately allows:
 * - `stringResource(...)` / any non-literal expression (a variable, a
 *   remote-config string) — only a bare literal is a violation;
 * - glyph-only literals (emoji / symbols with no letters), e.g. `Text("📧")`;
 * - literals inside preview scaffolding, see [isInsidePreview];
 * - developer-facing callees, see [isDeveloperFacing].
 *
 * Scope (v1): pure string literals only. An *interpolated* literal
 * (`Text("Hi $name")`) is not flagged — those should use a `%1$s` placeholder
 * in the catalogue, but catching them needs more than a PSI shape match.
 *
 * Adding a rule is a new [Rule] subclass plus its constructor reference in
 * [MovingEyesRuleSetProvider]; widening coverage is a new entry in
 * [TEXT_CALLEES] or [COPY_ARGUMENT_NAMES].
 */
class VerifyStrings(config: Config) : Rule(
    config,
    "User-facing copy must come from string resources (stringResource), not inline string literals.",
) {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.isInsidePreview()) return

        val callee = expression.calleeExpression?.text ?: return
        val isTextCallee = callee in TEXT_CALLEES
        if (!isTextCallee && callee.isDeveloperFacing()) return

        for (argument in expression.valueArguments) {
            val argumentName = (argument as? KtValueArgument)?.getArgumentName()?.text
            // Either the callee renders every string it's given, or this
            // particular argument is named like copy whatever the callee is.
            if (!isTextCallee && argumentName !in COPY_ARGUMENT_NAMES) continue

            val template = argument.getArgumentExpression() as? KtStringTemplateExpression ?: continue
            if (template.entries.any { it is KtStringTemplateEntryWithExpression }) continue
            val literal = template.entries.joinToString(separator = "") { it.text }
            if (literal.none { it.isLetter() }) continue

            val where = if (isTextCallee) "$callee()" else "$callee($argumentName = …)"
            report(
                Finding(
                    Entity.from(template),
                    "Inline user-facing string \"$literal\" passed to $where — " +
                        "use stringResource(...) from :libraries:resources instead.",
                ),
            )
        }
    }

    /**
     * True for callees whose `label` (or `message`) argument is read by a
     * developer, never by a user.
     *
     * Every `androidx.compose.animation` entry point takes a `label` for the
     * Animation Inspector — `animateFloatAsState(label = "panelTravel")` is a
     * tooling identifier, and translating it would be nonsense. The prefixes
     * below are the naming convention that whole API follows, so new animation
     * functions are covered as they appear. `Debug` catches our own debug-only
     * surfaces, e.g. `showDebugSnackBar`.
     */
    private fun String.isDeveloperFacing(): Boolean =
        DEVELOPER_CALLEE_PREFIXES.any { startsWith(it) } || contains("Debug")

    /**
     * True inside an `@Preview` composable, or inside a private `…Sample`
     * helper — the convention for scaffolding two previews share
     * (`PanelSample`, `StyleSample`, `TextSample`). A preview needs literal copy to show
     * anything at all, and a `…Sample` that stops being preview-only should
     * stop being private, which takes it out of this exemption.
     */
    private fun KtCallExpression.isInsidePreview(): Boolean {
        val function = getParentOfType<KtNamedFunction>(strict = true) ?: return false
        if (function.annotationEntries.any { it.shortName?.asString() == "Preview" }) return true
        return function.hasModifier(KtTokens.PRIVATE_KEYWORD) &&
            function.name?.endsWith("Sample") == true
    }

    private companion object {
        // The DS text composables that render a user-facing String argument. Their
        // wrappers (e.g. OutlinedText) pass the string on to Text internally, but
        // the literal lives at the wrapper call site, so each is checked directly.
        // AsteriskText is absent on purpose — it takes a composable lambda, whose
        // inner Text is already covered.
        val TEXT_CALLEES = setOf("Text", "OutlinedText", "ClickableText", "BoldPrefixedText")

        /** See [isDeveloperFacing]. */
        val DEVELOPER_CALLEE_PREFIXES = listOf(
            "animate",
            "Animated",
            "updateTransition",
            "rememberInfiniteTransition",
            "rememberTransition",
        )

        /**
         * Named arguments that carry copy to the screen, whatever the callee.
         *
         * Deliberately excludes `tag` (log tags), `name` and `path` (config
         * value identifiers), and `id` — all developer-facing and none of them
         * ever rendered.
         */
        val COPY_ARGUMENT_NAMES = setOf(
            "text",
            "label",
            "title",
            "subtitle",
            "headline",
            "subtext",
            "message",
            "body",
            "supporting",
            "placeholder",
            "hint",
            "caption",
            "actionTitle",
            "reportActionTitle",
            "contentDescription",
            "unitLabel",
            "holdLabel",
            "skipLabel",
            "nextLabel",
            "doneLabel",
            "eyebrow",
        )
    }
}
