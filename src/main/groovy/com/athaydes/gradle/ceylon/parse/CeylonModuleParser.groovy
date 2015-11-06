package com.athaydes.gradle.ceylon.parse

import groovy.transform.CompileStatic
import groovy.transform.TailRecursive

/**
 * Parser of Ceylon module files.
 */
@CompileStatic
class CeylonModuleParser {

    private boolean inBlockComment = false
    private boolean inDocComment = false
    private boolean inModule = false
    private boolean parsingImport = false
    private boolean parsingName = false
    private boolean parsingVersion = false
    private boolean parsingStartModule = false
    private boolean parsingSemiColon = false
    private int currentLine = 0
    private String fileName

    Map parse( String name, String text ) {
        fileName = name
        final result = [ : ]
        final lines = text.readLines()
        println "Starting parser ..."

        lineLoop:
        for ( line in lines ) {
            line = line.trim()
            currentLine++
            println "Line [$currentLine]: $line"
            def nonComments = nonCommentsFrom( line, [ ] ) as LinkedList<String>
            println "Non-comments: $nonComments"

            while ( nonComments ) {
                def words = nonComments.removeFirst().split( ' ' ).findAll { !it.empty } as LinkedList<String>
                println "Looking at words: $words"
                if ( !inModule && !parsingName ) {
                    String first = words.removeFirst()
                    if ( first != 'module' ) {
                        throw new RuntimeException( error( "expected 'module', found '$first'" ) )
                    } else {
                        parsingName = true
                    }
                }
                for ( String word in words ) {
                    println "Word: $word"
                    if ( parsingName ) {
                        println "Parsing name"
                        parseName word, result
                    } else if ( parsingVersion ) {
                        println "Parsing version"
                        parseVersion word, result
                    } else if ( inModule && parsingImport ) {
                        if ( word == '}' ) break lineLoop
                        println "Parsing import"
                        if ( word == 'shared' ) {
                            def imports = getImports( result )
                            imports << [ shared: true ]
                        } else {
                            parseImport word
                        }
                    } else if ( !inModule && parsingStartModule ) {
                        println "Parsing startModule"
                        parseStartModule word
                    } else if ( inModule && parsingSemiColon ) {
                        println "Parsing semi-colon"
                        if ( word == '}' ) break lineLoop
                        parseSemiColon word
                    } else {
                        throw new RuntimeException( error( "Unexpected '$word'" ) )
                    }
                }
            }
        }
        return result
    }

    private static List<Map> getImports( Map result ) {
        result.get( 'imports', [ ] ) as List<Map>
    }

    private void parseName( String word, Map result ) {
        String name = getName( word )
        if ( inModule ) {
            def imports = getImports( result )
            if ( !imports.empty && !imports.last().name ) { // newest entry has no name yet
                assert imports.last().containsKey( 'shared' )
                imports.last().name = name
            } else {
                imports << [ name: name ]
            }
        } else {
            result.moduleName = name
        }
        parsingName = false
        parsingVersion = true
    }

    private void parseVersion( String word, Map result ) {
        def version = getUnquoted( word )
        if ( version ) {
            if ( inModule ) {
                def imports = getImports( result )
                imports.last().version = version
                if ( word.endsWith( ';' ) ) parsingImport = true
                else parsingSemiColon = true
            } else {
                result.version = version
                parsingStartModule = true
            }
            parsingVersion = false
        } else {
            throw new RuntimeException( error( "Expected version String, found '$word'" ) )
        }
    }

    private void parseImport( String word ) {
        if ( word == 'import' ) {
            parsingName = true
            parsingImport = false
        } else {
            throw new RuntimeException( error( "Expected 'import', found '$word'" ) )
        }
    }

    private void parseStartModule( String word ) {
        if ( word == '{' ) {
            parsingImport = true
            parsingStartModule = false
            inModule = true
        } else {
            throw new RuntimeException( error( "Expected '{', found '$word'" ) )
        }
    }

    private void parseSemiColon( String word ) {
        if ( word == ';' ) {
            parsingImport = true
            parsingSemiColon = false
        } else {
            throw new RuntimeException( error( "Expected ';', found '$word'" ) )
        }
    }

    private String getName( String word ) {
        getUnquoted( word ) ?: word
    }

    private String getUnquoted( String word ) {
        if ( word.startsWith( '"' ) ) {
            def endOfWord = word.indexOf( '"', 1 )
            if ( endOfWord > 0 ) {
                return word.substring( 1, endOfWord )

            } else {
                throw new RuntimeException( error( "String literal not terminated: ${word}" ) )
            }
        } else {
            return null
        }
    }

    @TailRecursive
    List<String> nonCommentsFrom( String line, List<String> previous ) {
        if ( inBlockComment ) {
            line = afterBlockComment( line )
        }
        if ( !inBlockComment ) {
            def indexOfCommentStart = line.indexOf( '//' )
            if ( indexOfCommentStart >= 0 ) {
                line = line.substring( 0, indexOfCommentStart ).trim()
            }
            def openBlockCommentIndex = line.indexOf( '/*' )
            if ( openBlockCommentIndex >= 0 ) {
                def currentLine = line.substring( 0, openBlockCommentIndex ).trim()
                if ( currentLine ) previous << currentLine
                line = line.substring( openBlockCommentIndex + 2 ).trim()
                inBlockComment = true
            } else {
                line = line.trim()
                if ( line ) previous << line
                line = ''
            }
        }

        if ( line.trim().empty ) previous
        else nonCommentsFrom( line, previous )
    }

    private String afterBlockComment( String line ) {
        def endCommentIndex = line.indexOf( '*/' )
        if ( endCommentIndex >= 0 ) {
            line = line.substring( endCommentIndex + 2 ).trim()
            inBlockComment = false
        } else {
            line = ''
        }
        line
    }

    private String error( String message ) {
        "Cannot parse module [$fileName]. Error on line $currentLine: $message"
    }

}
