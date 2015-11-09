package com.athaydes.gradle.ceylon.parse

import groovy.transform.CompileStatic

import java.util.regex.Matcher

class AnnotationState {
    boolean parsingName = false
    boolean parsingOpenBracket = false
    boolean afterOpenBracket = false // could be close bracket or argument
    boolean parsingArgument = false
    boolean parsingCloseBracket = false
}

abstract class BaseState {
    boolean parsingDocs = false
    AnnotationState parsingAnnotationState = null
    boolean parsingName = false
    boolean parsingVersion = false
}

class ModuleDeclarationState extends BaseState {
    boolean parsingStartModule = false
}

class ModuleImportsState extends BaseState {
    boolean parsingSemiColon = false
}

class DoneState {}

/**
 * Parser of Ceylon module files.
 */
@CompileStatic
class CeylonModuleParser {

    static final moduleIdentifierRegex = /^[a-z][a-zA-Z_0-9\.]*[a-zA-Z_0-9]/
    static final mavenModuleIdentifierRegex = /^"[a-z][a-zA-Z_0-9\.:\-]*[a-zA-Z_0-9]"/
    static final annotationNameRegex = /^[a-z_][a-zA-Z_0-9]*/
    static final versionRegex = /^\"[a-zA-Z_0-9][a-zA-Z_0-9\.]*\"/

    private state = new ModuleDeclarationState()
    private int currentLine = 0
    private String fileName
    private LinkedList<String> words = [ ]
    private boolean lineComment = false
    private boolean blockComment = false

    Map parse( String name, String text ) {
        fileName = name
        final result = [ : ]
        final lines = text.readLines() as LinkedList<String>
        println "Starting parser ..."

        lineLoop:
        while ( lines ) {
            words = lines.removeFirst().split( ' ' )
                    .findAll { !it.empty } as LinkedList<String>

            currentLine++
            lineComment = false

            println "Looking at words: $words"
            while ( words && !lineComment ) {
                def word = words.removeFirst()
                switch ( state ) {
                    case ModuleDeclarationState:
                        parseModuleDeclaration( word, result )
                        break
                    case ModuleImportsState:
                        parseModuleImports( word, result )
                        break
                    case DoneState:
                        break lineLoop
                    default:
                        throw error( "internal state not recognized: $state" )
                }
            }
        }

        if ( words || lines ) {
            throw error( "expected end of module declaration" )
        }

        return result
    }

    private static List<Map> getImports( Map result ) {
        result.get( 'imports', [ ] ) as List<Map>
    }

    private void parseModuleDeclaration( String word,
                                         Map result ) {
        def state = this.state as ModuleDeclarationState

        def consumeChars = { int count ->
            if ( count < word.size() ) {
                word = word[ count..-1 ]
                println "Looking a shortened word '$word'"
                parseModuleDeclaration( word, result )
            }
        }

        if ( state.parsingAnnotationState ) {
            AnnotationState aState = state.parsingAnnotationState
            parseAnnotation word, state, aState, result
        } else if ( state.parsingName ) {
            def moduleNameMatcher = ( word =~ moduleIdentifierRegex )
            if ( moduleNameMatcher.find() ) {
                def lastIndex = moduleNameMatcher.end()
                result.moduleName = word[ 0..<lastIndex ]
                this.state = new ModuleDeclarationState( parsingVersion: true )
                consumeChars lastIndex
            } else {
                throw error( "expected module name, found '$word'" )
            }
        } else if ( state.parsingVersion ) {
            def versionMatcher = ( word =~ versionRegex )
            if ( versionMatcher.find() ) {
                def lastIndex = versionMatcher.end()
                result.version = word[ 1..( lastIndex - 2 ) ]
                this.state = new ModuleDeclarationState( parsingStartModule: true )
                consumeChars lastIndex
            } else {
                throw error( "expected module version, found $word" )
            }
        } else if ( state.parsingStartModule ) {
            if ( word.startsWith( '{' ) ) {
                this.state = new ModuleImportsState()
                if ( word.length() > 1 ) {
                    words.addFirst( word[ 1..-1 ] )
                }
            }
        } else if ( state.parsingDocs ) {
            Matcher unescapedDelimiterMatcher = ( word =~ /.*(?<!\\)"/ )
            if ( unescapedDelimiterMatcher.find() ) {
                def lastIndex = unescapedDelimiterMatcher.end()
                result.hasDocs = true
                this.state = new ModuleDeclarationState()
                consumeChars lastIndex
            }
        } else if ( word.startsWith( '//' ) ) {
            lineComment = true
        } else { // begin
            if ( word == 'module' ) {
                this.state = new ModuleDeclarationState( parsingName: true )
            } else if ( word.startsWith( '"' ) ) {
                if ( result.hasDocs ) {
                    throw error( 'more than one doc String is not allowed' )
                }
                this.state = new ModuleDeclarationState( parsingDocs: true )
                consumeChars 1
            } else {
                def aState = new AnnotationState( parsingName: true )
                this.state = new ModuleDeclarationState(
                        parsingAnnotationState: aState )
                parseAnnotation( word, state, aState, result )
            }
        }
    }

    private void parseModuleImports( String word, Map result ) {
        def state = this.state as ModuleImportsState

        def consumeChars = { int count ->
            if ( count < word.size() ) {
                word = word[ count..-1 ]
                println "Looking a shortened word '$word'"
                parseModuleImports( word, result )
            }
        }

        if ( state.parsingDocs ) {
            Matcher unescapedDelimiterMatcher = ( word =~ /.*(?<!\\)"/ )
            if ( unescapedDelimiterMatcher.find() ) {
                def lastIndex = unescapedDelimiterMatcher.end()
                this.state = new ModuleImportsState()
                consumeChars lastIndex
            }
        } else if ( state.parsingAnnotationState ) {
            parseAnnotation( word, state, state.parsingAnnotationState, result )
        } else if ( state.parsingName ) {
            println "Trying to parse import module name: $word"
            def nameMatcher = ( word =~ moduleIdentifierRegex )
            def mavenNameMatcher = ( word =~ mavenModuleIdentifierRegex )
            def matcher = nameMatcher.find() ? nameMatcher :
                    mavenNameMatcher.find() ? mavenNameMatcher : null
            if ( matcher != null ) {
                def lastIndex = matcher.end()
                def name = mavenNameMatcher.is( matcher ) ?
                        word[ 1..( lastIndex - 2 ) ] :
                        word[ 0..<lastIndex ]
                def imports = getImports( result )
                if ( !imports.empty && !imports.last().name ) { // newest entry has no name yet
                    assert imports.last().containsKey( 'shared' )
                    imports.last().name = word
                } else {
                    imports << [ name: name ]
                }
                this.state = new ModuleImportsState( parsingVersion: true )
            } else {
                throw error( "expected imported module name, found '$word'" )
            }
        } else if ( state.parsingVersion ) {
            def versionMatcher = ( word =~ versionRegex )
            if ( versionMatcher.find() ) {
                def imports = getImports( result )
                def lastIndex = versionMatcher.end()
                imports.last().version = word[ 1..( lastIndex - 2 ) ]
                this.state = new ModuleImportsState( parsingSemiColon: true )
                consumeChars lastIndex
            } else {
                throw error( "expected module version, found '$word'" )
            }
        } else if ( state.parsingSemiColon ) {
            if ( word.startsWith( ';' ) ) {
                this.state = new ModuleImportsState()
                consumeChars 1
            } else {
                throw error( "expected semi-colon, found '$word'" )
            }
        } else if ( word.startsWith( '//' ) ) {
            lineComment = true
        } else { // begin or end
            println "Started parsing module imports, looking at $word"
            if ( word == 'import' ) {
                this.state = new ModuleImportsState( parsingName: true )
            } else if ( word.startsWith( '"' ) ) {
                this.state = new ModuleImportsState( parsingDocs: true )
                consumeChars 1
            } else if ( word == '}' ) {
                this.state = new DoneState()
            } else {
                this.state = new ModuleImportsState(
                        parsingAnnotationState: new AnnotationState( parsingName: true ) )
                // put word back in the queue as we don't know what it is
                words.addFirst( word )
            }
        }
    }

    private void parseAnnotation( String word,
                                  BaseState state,
                                  AnnotationState aState,
                                  Map result ) {
        println "Parsing annotation: $word"
        def consumeChars = { int count ->
            if ( count < word.size() ) {
                word = word[ count..-1 ]
                println "Looking a shortened word '$word'"
                parseAnnotation( word, state, aState, result )
            }
        }

        if ( aState.parsingName ) {
            if ( state instanceof ModuleImportsState && word == 'import' ) {
                println "Import is no annotation, moving to parseModuleImports"
                this.state = new ModuleImportsState( parsingName: true )
            } else {
                Matcher nameMatcher = ( word =~ annotationNameRegex )
                if ( nameMatcher.find() ) {
                    int lastIndex = nameMatcher.end()
                    def annotation = word[ 0..<lastIndex ]
                    if ( annotation == 'shared' ) {
                        if ( state instanceof ModuleDeclarationState ) {
                            result.shared = true
                        } else {
                            def imports = getImports( result )
                            imports << [ shared: true ]
                        }
                    }
                    aState = new AnnotationState( parsingOpenBracket: true )
                    state.parsingAnnotationState = aState
                    consumeChars lastIndex
                } else {
                    throw error( "expected annotation or module name, found '$word'" )
                }
            }
        } else if ( aState.parsingArgument ) {
            Matcher unescapedDelimiterMatcher = ( word =~ /.*(?<!\\)"/ )
            if ( unescapedDelimiterMatcher.find() ) {
                aState = new AnnotationState( parsingCloseBracket: true )
                state.parsingAnnotationState = aState
                def lastIndex = unescapedDelimiterMatcher.end()
                consumeChars lastIndex
            }
        } else if ( aState.parsingOpenBracket ) {
            if ( word.startsWith( '(' ) ) {
                aState = new AnnotationState( afterOpenBracket: true )
                state.parsingAnnotationState = aState
                consumeChars 1
            } else {
                // get out of annotation parsing as annotations don't need args
                words.addFirst( word )
                if ( state instanceof ModuleDeclarationState ) {
                    this.state = new ModuleDeclarationState()
                } else {
                    this.state = new ModuleImportsState()
                }
            }
        } else if ( aState.afterOpenBracket ) {
            if ( word.startsWith( '"' ) ) {
                aState = new AnnotationState( parsingArgument: true )
                state.parsingAnnotationState = aState
                consumeChars 1
            } else {
                aState = new AnnotationState( parsingCloseBracket: true )
                state.parsingAnnotationState = aState
            }
        } else if ( aState.parsingCloseBracket ) {
            if ( word.startsWith( ')' ) ) {
                if ( state instanceof ModuleDeclarationState ) {
                    this.state = new ModuleDeclarationState()
                } else {
                    this.state = new ModuleImportsState()
                }
                if ( word.size() > 1 ) {
                    word = word[ 1..-1 ]
                    if ( state instanceof ModuleDeclarationState ) {
                        parseModuleDeclaration( word, result )
                    } else {
                        parseModuleImports( word, result )
                    }
                }
            } else {
                throw error( "expected annotation ')', found '$word'" )
            }
        } else if ( word.startsWith( '//' ) ) {
            lineComment = true
        } else {
            parseAnnotation( word, state,
                    new AnnotationState( parsingName: true ), result )
        }
    }

    private RuntimeException error( String message ) {
        new RuntimeException( "Cannot parse module [$fileName]. " +
                "Error on line $currentLine: $message" )
    }

}
