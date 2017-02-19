package com.athaydes.gradle.ceylon.util

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * A simple command option.
 *
 * {@link CommandOption#toString} returns the option without quoting the argument (if there is an argument).
 *
 * To get the argument quoted, use {@link CommandOption#withQuotedArgument()}.
 */
@Immutable( knownImmutableClasses = [ Optional ] )
@CompileStatic
class CommandOption {
    String option
    Optional<String> argument

    @Override
    String toString() {
        option + argument.map { '=' + it }.orElse( '' )
    }

    String withQuotedArgument() {
        option + argument.map { /="$it"/.toString() }.orElse( '' )
    }

    static CommandOption of( String option, String argument = null ) {
        new CommandOption( option, Optional.ofNullable( argument ) )
    }
}
