package com.athaydes.gradle.ceylon.parse

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.transform.ToString

import java.util.regex.Matcher

@ToString( includeNames = true, includePackage = false )
class AnnotationState {
    boolean parsingName = false
    boolean parsingOpenBracket = false
    boolean afterOpenBracket = false // could be close bracket or argument
    boolean parsingArgument = false
    boolean parsingCloseBracket = false
}

@Immutable
class ParsingDocs {
    boolean on
    boolean isTripleQuote
}

abstract class BaseState {
    ParsingDocs parsingDocs = new ParsingDocs( false, false )
    AnnotationState parsingAnnotationState = null
    boolean parsingName = false
    boolean parsingVersion = false
}

@ToString( includeFields = true, includePackage = false, includeSuperProperties = true )
class ModuleDeclarationState extends BaseState {
    boolean parsingStartModule = false
}

@ToString( includeFields = true, includePackage = false, includeSuperProperties = true )
class ModuleImportsState extends BaseState {
    boolean parsingSemiColon = false
}

@ToString( includeFields = true, includePackage = false )
class DoneState {}

/**
 * Parser of Ceylon module files.
 */
@CompileStatic
class CeylonModuleParser {

    static final moduleIdentifierRegex = /^[a-z][a-zA-Z_0-9\.]*[a-zA-Z_0-9]/
    static final mavenModuleIdentifierRegex = /^"[a-z][a-zA-Z_0-9\.:\-]*[a-zA-Z_0-9]"/
    static final annotationNameRegex = /^[a-z_][a-zA-Z_0-9]*/
    static final versionRegex = /^\"[a-zA-Z_0-9][a-zA-Z_0-9\.\-\+]*\"/
    static final String nonEscapedTripleQuoteRegex = /.*(?<!\\)"""/
    static final String nonEscapedQuoteRegex = /.*(?<!\\)"/
    static final String endBlockCommentRegex = /.*(?<!\\)\*\//

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

        lineLoop:
        while ( lines ) {
            words = lines.removeFirst().split( /\s/ )
                    .findAll { !it.empty } as LinkedList<String>

            currentLine++
            lineComment = false

            while ( words && !lineComment ) {
                def word = words.removeFirst()

                final state = this.state
                if ( state instanceof ModuleDeclarationState ) {
                    parseModuleDeclaration( word, state, result )
                } else if ( state instanceof ModuleImportsState ) {
                    parseModuleImports( word, state as ModuleImportsState, result )
                } else if ( state instanceof DoneState ) {
                    break lineLoop
                } else {
                    throw error( "internal state not recognized: $state" )
                }
            }
        }

        return result
    }

    private static List<Map> getImports( Map result ) {
        result.get( 'imports', [ ] ) as List<Map>
    }

    private void parseModuleDeclaration( String word,
                                         ModuleDeclarationState state,
                                         Map result ) {
        if ( blockComment ) {
            def endBlockMatcher = ( word =~ endBlockCommentRegex )
            if ( endBlockMatcher.find() ) {
                blockComment = false
                def lastIndex = endBlockMatcher.end()
                consumeChars lastIndex, word, result
            }
        } else if ( word.startsWith( '/*' ) && !state.parsingDocs.on ) {
            blockComment = true
            consumeChars 2, word, result
        } else if ( word.startsWith( '//' ) ) {
            lineComment = true
        } else if ( state.parsingAnnotationState ) {
            AnnotationState aState = state.parsingAnnotationState
            parseAnnotation word, state, aState, result
        } else if ( state.parsingName ) {
            def moduleNameMatcher = ( word =~ moduleIdentifierRegex )
            if ( moduleNameMatcher.find() ) {
                def lastIndex = moduleNameMatcher.end()
                result.moduleName = word[ 0..<lastIndex ]
                this.state = new ModuleDeclarationState( parsingVersion: true )
                consumeChars lastIndex, word, result
            } else {
                throw error( "expected module name, found '$word'" )
            }
        } else if ( state.parsingVersion ) {
            def versionMatcher = ( word =~ versionRegex )
            if ( versionMatcher.find() ) {
                def lastIndex = versionMatcher.end()
                result.version = word[ 1..( lastIndex - 2 ) ]
                this.state = new ModuleDeclarationState( parsingStartModule: true )
                consumeChars lastIndex, word, result
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
        } else if ( state.parsingDocs.on ) {
            Matcher unescapedDelimiterMatcher = matcherFor( state.parsingDocs, word )
            if ( unescapedDelimiterMatcher.find() ) {
                def lastIndex = unescapedDelimiterMatcher.end()
                result.hasDocs = true
                this.state = new ModuleDeclarationState()
                consumeChars lastIndex, word, result
            }
        } else { // begin
            if ( word == 'module' ) {
                this.state = new ModuleDeclarationState( parsingName: true )
            } else if ( word.startsWith( '"' ) ) {
                if ( result.hasDocs ) {
                    throw error( 'more than one doc String is not allowed' )
                }
                def isTripleQuote = word.startsWith( '"""' )
                this.state = new ModuleDeclarationState( parsingDocs: new ParsingDocs( true, isTripleQuote ) )
                consumeChars 1, word, result
            } else {
                this.state = new ModuleDeclarationState(
                        parsingAnnotationState: new AnnotationState( parsingName: true ) )
                words.addFirst( word )
            }
        }
    }

    private void parseModuleImports( String word, ModuleImportsState state, Map result ) {
        if ( blockComment ) {
            def endBlockMatcher = ( word =~ endBlockCommentRegex )
            if ( endBlockMatcher.find() ) {
                blockComment = false
                def lastIndex = endBlockMatcher.end()
                consumeChars lastIndex, word, result
            }
        } else if ( state.parsingDocs.on ) {
            Matcher unescapedDelimiterMatcher = matcherFor( state.parsingDocs, word )
            if ( unescapedDelimiterMatcher.find() ) {
                def lastIndex = unescapedDelimiterMatcher.end()
                this.state = new ModuleImportsState()
                consumeChars lastIndex, word, result
            }
        } else if ( word.startsWith( '/*' ) ) {
            blockComment = true
            consumeChars 2, word, result
        } else if ( word.startsWith( '//' ) ) {
            lineComment = true
        } else if ( state.parsingAnnotationState ) {
            parseAnnotation( word, state, state.parsingAnnotationState, result )
        } else if ( state.parsingName ) {
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
                    imports.last().name = name
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
                consumeChars lastIndex, word, result
            } else {
                throw error( "expected module version, found '$word'" )
            }
        } else if ( state.parsingSemiColon ) {
            if ( word.startsWith( ';' ) ) {
                this.state = new ModuleImportsState()
                consumeChars 1, word, result
            } else {
                throw error( "expected semi-colon, found '$word'" )
            }
        } else { // begin or end
            if ( word == 'import' ) {
                this.state = new ModuleImportsState( parsingName: true )
            } else if ( word.startsWith( '"' ) ) {
                def isTripleQuote = word.startsWith( '"""' )
                this.state = new ModuleImportsState( parsingDocs: new ParsingDocs( true, isTripleQuote ) )
                consumeChars 1, word, result
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
        if ( blockComment ) {
            def endBlockMatcher = ( word =~ endBlockCommentRegex )
            if ( endBlockMatcher.find() ) {
                blockComment = false
                def lastIndex = endBlockMatcher.end()
                consumeChars lastIndex, word, result
            }
        } else if ( aState.parsingArgument ) {
            Matcher unescapedDelimiterMatcher = ( word =~ nonEscapedQuoteRegex )
            if ( unescapedDelimiterMatcher.find() ) {
                state.parsingAnnotationState = new AnnotationState( parsingCloseBracket: true )
                def lastIndex = unescapedDelimiterMatcher.end()
                consumeChars lastIndex, word, result
            }
        } else if ( word.startsWith( '/*' ) ) {
            blockComment = true
            consumeChars 2, word, result
        } else if ( word.startsWith( '//' ) ) {
            lineComment = true
        } else if ( aState.parsingName ) {
            if ( state instanceof ModuleImportsState && word == 'import' ) {
                this.state = new ModuleImportsState( parsingName: true )
            } else if ( state instanceof ModuleDeclarationState && word == 'module' ) {
                this.state = new ModuleDeclarationState( parsingName: true )
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
                    state.parsingAnnotationState = new AnnotationState( parsingOpenBracket: true )
                    consumeChars lastIndex, word, result
                } else {
                    throw error( "expected annotation or module name, found '$word'" )
                }
            }
        } else if ( aState.parsingOpenBracket ) {
            if ( word.startsWith( '(' ) ) {
                state.parsingAnnotationState = new AnnotationState( afterOpenBracket: true )
                consumeChars 1, word, result
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
                state.parsingAnnotationState = new AnnotationState( parsingArgument: true )
                consumeChars 1, word, result
            } else {
                state.parsingAnnotationState = new AnnotationState( parsingCloseBracket: true )
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
                        parseModuleDeclaration( word, state, result )
                    } else if ( state instanceof ModuleImportsState ) {
                        parseModuleImports( word, state, result )
                    }
                }
            } else {
                throw error( "expected annotation ')', found '$word'" )
            }
        } else { // start over
            state.parsingAnnotationState = new AnnotationState( parsingName: true )
        }
    }

    private consumeChars( int count, String word, Map result ) {
        if ( count < word.size() ) {
            word = word[ count..-1 ]
            final state = this.state
            if ( state instanceof ModuleImportsState ) {
                parseModuleImports( word, state, result )
            } else if ( state instanceof ModuleDeclarationState ) {
                parseModuleDeclaration( word, state, result )
            }
        }
    }

    private RuntimeException error( String message ) {
        new RuntimeException( "Cannot parse module [$fileName]. " +
                "Error on line $currentLine: $message" )
    }

    private static Matcher matcherFor( ParsingDocs state, String word ) {
        state.isTripleQuote ?
                word =~ nonEscapedTripleQuoteRegex :
                word =~ nonEscapedQuoteRegex
    }

}
