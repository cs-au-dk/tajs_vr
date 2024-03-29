package dk.brics.tajs.test;
//import static org.junit.Assert.fail;

import dk.brics.tajs.Main;
import dk.brics.tajs.monitoring.soundness.SoundnessTesterMonitor;
import dk.brics.tajs.options.Options;
import dk.brics.tajs.util.AnalysisLimitationException;
import dk.brics.tajs.util.ParseError;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
// TODO: assertEquals, assertTrue, assertFalse, assertThrows

@SuppressWarnings("static-method")
public class TestV8 { // TODO: check expected output for TestV8

    public static void main(String[] args) {
        org.junit.runner.JUnitCore.main("dk.brics.tajs.test.TestV8");
    }

    @Before
    public void init() {
        Main.reset();
        Options.get().enableTest();
        Options.get().enableContextSensitiveHeap();
        Options.get().enableParameterSensitivity();
        Options.get().enableUnevalizer();
        Options.get().enablePolyfillMDN();
        // Options.get().enableNoLazy();
        Options.get().setAnalysisTimeLimit(1 * 60);
    }

    @Test
    public void testV8_api_call_after_bypassed_exception() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/api-call-after-bypassed-exception.js");
        Misc.checkSystemOutput();
    }

    @Ignore // FIXME GitHub #206
    @Test
    public void testV8_apply() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/apply.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_arguments_call_apply() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/arguments-call-apply.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_arguments_enum() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/arguments-enum.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_arguments_indirect() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/arguments-indirect.js");
        Misc.checkSystemOutput();
    }

    // TODO: github #183
    @Test
    public void testV8_arguments() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/arguments.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_array_concat() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/array-concat.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_array_functions_prototype() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/array-functions-prototype.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_array_indexing() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/array-indexing.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_array_iteration() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/array-iteration.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_array_join() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/array-join.js");
        Misc.checkSystemOutput();
    }

    // TODO: syntax error (!)
    @Test(expected = ParseError.class)
    public void testV8_array_sort() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/array-sort.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_array_splice_webkit() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/array-splice-webkit.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_array_splice() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/array-splice.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_array_length() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/array_length.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_ascii_regexp_subject() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/ascii-regexp-subject.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_binary_operation_overwrite() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/binary-operation-overwrite.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_body_not_visible() throws Exception {
        Options.get().enableUnevalizer();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/body-not-visible.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_call_non_function_call() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/call-non-function-call.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_call_non_function() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/call-non-function.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_call() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/call.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_char_escape() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/char-escape.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_class_of_builtins() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/class-of-builtins.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_closure() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/closure.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_compare_nan() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/compare-nan.js");
        Misc.checkSystemOutput();
    }

    // TODO: `const` unsupported, github #182
    @Test(expected = AnalysisLimitationException.AnalysisPrecisionLimitationException.class)
    public void testV8_const_redecl() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/const-redecl.js");
        Misc.checkSystemOutput();
    }

    // TODO: `const` unsupported, github #182
    @Test
    public void testV8_cons_test() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/const.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_context_variable_assignments() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/context-variable-assignments.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_cyclic_array_to_string() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/cyclic-array-to-string.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_date_parse() throws Exception {
        Options.get().enablePolyfillMDN();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/date-parse.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_date() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/date.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_backtrace_text() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-backtrace-text.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_backtrace() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-backtrace.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_breakpoints() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-breakpoints.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_changebreakpoint() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-changebreakpoint.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_clearbreakpoint() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-clearbreakpoint.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_conditional_breakpoints() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-conditional-breakpoints.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_constructed_by() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-constructed-by.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_constructor() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-constructor.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_continue() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-continue.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_enable_disable_breakpoints() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-enable-disable-breakpoints.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_evaluate_arguments() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-evaluate-arguments.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_evaluate_locals() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-evaluate-locals.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_evaluate_recursive() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-evaluate-recursive.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_evaluate_with() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-evaluate-with.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_evaluate() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-evaluate.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_event_listener() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-event-listener.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_ignore_breakpoints() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-ignore-breakpoints.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_multiple_breakpoints() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-multiple-breakpoints.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_referenced_by() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-referenced-by.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_script_breakpoints() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-script-breakpoints.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_script() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-script.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_scripts_request() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-scripts-request.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_setbreakpoint() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-setbreakpoint.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_sourceinfo() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-sourceinfo.js");
        Misc.checkSystemOutput();
    }

    @Test(expected = AnalysisLimitationException.class) // TODO investigate (GitHub #417)
    public void testV8_debug_sourceslice() throws Exception {
        Options.get().enableUnevalizer();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-sourceslice.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_step_stub_callfunction() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-step-stub-callfunction.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_step() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-step.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_debug_stepin_constructor() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/debug-stepin-constructor.js");
        Misc.checkSystemOutput();
    }

    @Test(expected = AnalysisLimitationException.AnalysisPrecisionLimitationException.class)
    public void testV8_declare_locally() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/declare-locally.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_deep_recursion() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/deep-recursion.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_delay_syntax_error() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/delay-syntax-error.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_delete_global_properties() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/delete-global-properties.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_delete_in_eval() throws Exception {
        Options.get().enableUnevalizer();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/delete-in-eval.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_delete_in_with() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/delete-in-with.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_delete_vars_from_eval() throws Exception {
        Options.get().enableUnevalizer();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/delete-vars-from-eval.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_delete() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/delete.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_do_not_strip_fc() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/do-not-strip-fc.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_dont_enum_array_holes() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/dont-enum-array-holes.js");
        Misc.checkSystemOutput();
    }

    @Test(expected = AnalysisLimitationException.AnalysisPrecisionLimitationException.class)
    public void testV8_dont_reinit_global_var() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/dont-reinit-global-var.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_double_equals() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/double-equals.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_dtoa() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/dtoa.js");
        Misc.checkSystemOutput();
    }

    @Test(expected = AnalysisLimitationException.AnalysisPrecisionLimitationException.class)
    public void testV8_enumeration_order() throws Exception {
        Options.get().enableUnevalizer();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/enumeration_order.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_Error_captureStackTrace() {
        Misc.runSource("var obj = {};", "Error.captureStackTrace(obj);");
        Misc.checkSystemOutput();
    }

    // TODO: GitHub #550
    @Test(expected = SoundnessTesterMonitor.SoundnessException.class)
    public void testV8_Error_prepareStackTrace() {
        Misc.runSource("var obj = {};", "Error.prepareStackTrace = function(){};", "Error.captureStackTrace(obj);", "obj.stack");
    }

    @Test
    public void testV8_escape() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/escape.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_eval_typeof_non_existing() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/eval-typeof-non-existing.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_execScript_case_insensitive() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/execScript-case-insensitive.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_extra_arguments() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/extra-arguments.js");
        Misc.checkSystemOutput();
    }

    // TODO: github #184
    @Test
    public void testV8_extra_commas() throws Exception {
        Options.get().enableUnevalizer();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/extra-commas.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_for_in_null_or_undefined() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/for-in-null-or-undefined.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_for_in_special_cases() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/for-in-special-cases.js");
        Misc.checkSystemOutput();
    }

    // TODO: parse error
    @Test
    public void testV8_for_in() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/for-in.js");
        Misc.checkSystemOutput();
    }

    // TODO: __proto__
    @Test
    public void testV8_fun_as_prototype() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/fun-as-prototype.js");
        Misc.checkSystemOutput();
    }

    // TODO: github #184
    @Test
    public void testV8_fun_name() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/fun_name.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_function_arguments_null() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/function-arguments-null.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_function_caller() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/function-caller.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_function_names() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/function-names.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_function_property() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/function-property.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_function_prototype() throws Exception {
        Options.get().enablePolyfillMDN();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/function-prototype.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_function_source() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/function-source.js");
        Misc.checkSystemOutput();
    }

    // TODO: GitHub #147
    @Test(expected = AnalysisLimitationException.AnalysisPrecisionLimitationException.class)
    // implicit toString calls on line 77 share the return-node, merging their results!
    public void testV8_function() throws Exception {
        Options.get().enableUnevalizer();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/function.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_fuzz_accessors() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/fuzz-accessors.js");
        Misc.checkSystemOutput();
    }

    @Test(expected = ParseError.class)
    public void testV8_fuzz_natives() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/fuzz-natives.js");
        Misc.checkSystemOutput();
    }

    // TODO: github #3
    @Test
    public void testV8_getter_in_value_prototype() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/getter-in-value-prototype.js");
        Misc.checkSystemOutput();
    }

    // TODO: `const` unsupported, github #182
    @Test
    public void testV8_global_const_var_conflicts() throws Exception {
        Options.get().enableUnevalizer();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/global-const-var-conflicts.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_global_vars_eval() throws Exception {
        Options.get().enableUnevalizer();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/global-vars-eval.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_global_vars_with() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/global-vars-with.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_greedy() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/greedy.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_has_own_property() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/has-own-property.js");
        Misc.checkSystemOutput();
    }

    // TODO: HTML comments not handled
    @Test(expected = ParseError.class)
    public void testV8_html_comments() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/html-comments.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_html_string_funcs() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/html-string-funcs.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_if_in_undefined() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/if-in-undefined.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_in() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/in.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_instance_of() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/instanceof.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_integer_to_string() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/integer-to-string.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_invalid_lhs() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/invalid-lhs.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_keyed_ic() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/keyed-ic.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_keyed_storage_extend() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/keyed-storage-extend.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_large_object_allocation() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/large-object-allocation.js");
        Misc.checkSystemOutput();
    }

    @Test(expected = AnalysisLimitationException.AnalysisPrecisionLimitationException.class)
    public void testV8_large_object_literal() throws Exception {
        Options.get().enableUnevalizer();
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/large-object-literal.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_lazy_load() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/lazy-load.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_leakcheck() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/leakcheck.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_length() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/length.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_math_min_max() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/math-min-max.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_megamorphic_callbacks() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/megamorphic-callbacks.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_array() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-array.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_boolean() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-boolean.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_date() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-date.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_error() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-error.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_function() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-function.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_null() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-null.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_number() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-number.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_object() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-object.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_regexp() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-regexp.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_string() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-string.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_undefined() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-undefined.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mirror_unresolved_function() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mirror-unresolved-function.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_mul_exhaustive() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/mul-exhaustive.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_negate_zero() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/negate-zero.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_negate() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/negate.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_nested_repetition_count_overflow() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/nested-repetition-count-overflow.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_new_test() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/new.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_newline_in_string() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/newline-in-string.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_no_branch_elimination() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/no-branch-elimination.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_no_octal_constants_above_256() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/no-octal-constants-above-256.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_no_semicolon() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/no-semicolon.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_non_ascii_replace() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/non-ascii-replace.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_nul_characters() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/nul-characters.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_number_limits() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/number-limits.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_number_string_index_call() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/number-string-index-call.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_number_tostring_small() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/number-tostring-small.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_number_tostring() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/number-tostring.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_obj_construct() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/obj-construct.js");
        Misc.checkSystemOutput();
    }

    @Test(expected = AnalysisLimitationException.AnalysisPrecisionLimitationException.class)
    public void testV8_parse_int_float() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/parse-int-float.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_property_object_key() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/property-object-key.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_proto() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/proto.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_prototype() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/prototype.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_regexp_indexof() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/regexp-indexof.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_regexp_multiline_stack_trace() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/regexp-multiline-stack-trace.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_regexp_multiline() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/regexp-multiline.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_regexp_standalones() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/regexp-standalones.js");
        Misc.checkSystemOutput();
    }

    // TODO: github #183
    @Test
    public void testV8_regexp_static() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/regexp-static.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_regexp() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/regexp.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_scanner() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/scanner.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_smi_negative_zero() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/smi-negative-zero.js");
        Misc.checkSystemOutput();
    }

    // TODO: `const` unsupported, github #182
    @Test
    public void testV8_smi_ops() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/smi-ops.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_sparse_array_reverse() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/sparse-array-reverse.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_sparse_array() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/sparse-array.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_str_to_num() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/str-to-num.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_stress_array_push() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/stress-array-push.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_strict_equals() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/strict-equals.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_string_case() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/string-case.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_string_charat() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/string-charat.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_string_charcodeat() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/string-charcodeat.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_string_compare_alignment() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/string-compare-alignment.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_string_flatten() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/string-flatten.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_string_index() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/string-index.js");
        Misc.checkSystemOutput();
    }

    // TODO: syntax error (!)
    @Test
    public void testV8_string_indexof() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/string-indexof.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_string_lastindexof() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/string-lastindexof.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_string_localecompare() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/string-localecompare.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_string_search() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/string-search.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_string_split() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/string-split.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_substr() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/substr.js");
        Misc.checkSystemOutput();
    }

    @Test(expected = AnalysisLimitationException.SyntacticSupportNotImplemented.class /* switch in non-last position */)
    public void testV8_switch_test() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/switch.js");
    }

    // TODO: github #183
    @Test
    public void testV8_this_in_callbacks() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/this-in-callbacks.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_this_test() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/this.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_throw_exception_for_null_access() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/throw-exception-for-null-access.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_to_precision() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/to-precision.js");
        Misc.checkSystemOutput();
        // TODO: Find out why there are "Converting primitive number to object" warnings when running this test,
        // but not when the test is cut and pasted verbatim into a separate file outside the test framework.
    }

    @Test
    public void testV8_tobool() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/tobool.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_toint32() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/toint32.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_touint32() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/touint32.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_try_finally_nested() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/try-finally-nested.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_try_test() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/try.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_try_catch_scopes() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/try_catch_scopes.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_unicode_string_to_number() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/unicode-string-to-number.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_unicode_test() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/unicode-test.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_unusual_constructor() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/unusual-constructor.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_uri() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/uri.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_value_callic_prototype_change() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/value-callic-prototype-change.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_var() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/var.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_with_function_expression() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/with-function-expression.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_with_leave() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/with-leave.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_with_parameter_access() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/with-parameter-access.js");
        Misc.checkSystemOutput();
    }

    @Test
    public void testV8_with_value() throws Exception {
        Misc.run("test-resources/src/v8tests/prologue.js", "test-resources/src/v8tests/with-value.js");
        Misc.checkSystemOutput();
    }
}
