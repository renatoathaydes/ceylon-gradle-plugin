package com.athaydes.gradle.ceylon.util

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import org.gradle.api.Nullable

/**
 * A simple command option.
 *
 * {@link CommandOption#toString} returns the option without quoting the argument (if there is an argument).
 *
 * To get the argument quoted, use {@link CommandOption#withQuotedArgument()}.
 */
@Immutable
@CompileStatic
class CommandOption {
    String option
    @Nullable
    String argument

    @Override
    String toString() {
        option + ( argument ? '=' + argument : '' )
    }

    String withQuotedArgument() {
        option + ( argument ? '="' + argument + '"' : '' )
    }

    static CommandOption of( String option, String argument = null ) {
        new CommandOption( option, argument )
    }
}
