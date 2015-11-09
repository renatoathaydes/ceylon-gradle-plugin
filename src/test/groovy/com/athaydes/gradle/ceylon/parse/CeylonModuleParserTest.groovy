package com.athaydes.gradle.ceylon.parse

import org.junit.Test

class CeylonModuleParserTest {

    def parser = new CeylonModuleParser()

    @Test
    void "Can remove comments from each line"() {
        assert parser.nonCommentsFrom( '//hello', [ ] ) == [ ]
        assert parser.nonCommentsFrom( ' // hello', [ ] ) == [ ]
        assert parser.nonCommentsFrom( 'hello', [ ] ) == [ 'hello' ]
        assert parser.nonCommentsFrom( 'hello // bye', [ ] ) == [ 'hello' ]
        assert parser.nonCommentsFrom( 'hello /* this is  a comment */ hi', [ ] ) == [ 'hello', 'hi' ]
        assert parser.nonCommentsFrom( 'hello /*this*/is/*comment*/bye', [ ] ) == [ 'hello', 'is', 'bye' ]
    }

    @Test
    void "Can remove multi-line block comment"() {
        def result = [ ]
        def go = parser.&nonCommentsFrom.rcurry( result )
        go ''
        go '        /* a comment'
        go '           spanning several lines'
        go '           Some [[Example]] markup.'
        go '           And even some code:'
        go '           <code>shared function() {'
        go '               print("Hello world!");'
        go '           }'
        go '           </code>'
        go '        */'
        go '        module my.test.module {'
        go '           // this is an import'
        go '           import other.module "1.0";'
        go '        }'

        assert result == [ 'module my.test.module {', 'import other.module "1.0";', '}' ]
    }

    @Test
    void "Can remove multi-line doc comment"() {
        def result = [ ]
        def go = parser.&nonCommentsFrom.rcurry( result )
        go ''
        go '        "A comment'
        go '         spanning several lines'
        go '         Some [[Example]] markup.'
        go '         And even some code:'
        go '             shared function() {'
        go '                 print(\\"Hello world!\\");'
        go '             }'
        go '          '
        go '          /* block comments'
        go '             are acceptable inside doc comments.'
        go '           */'
        go '         "'
        go '        module my.test.module {'
        go '           "This import is required.'
        go '            The \\"1.0\\" is the version""'
        go '            "'
        go '           import other.module "1.0";'
        go '        }'

        assert result == [ 'module my.test.module {', 'import other.module "1.0";', '}' ]
    }

    @Test
    void "Can parse a simple module file"() {
        def result = parser.parse 'myfile.ceylon', """
                // some module
                module com.hello.world "1.0.0" {
                    import java.lang.base  "7";
                }
                """

        assert result
        assert result.moduleName == 'com.hello.world'
        assert result.version == '1.0.0'
        assert result.imports == [
                [ name: 'java.lang.base', version: '7' ]
        ]
    }

    @Test
    void "Can parse a module file with many imports"() {
        def result = parser.parse 'myfile.ceylon', """
                // some module
                module com.hello.world2 "2.3" {
                    import java.lang.base  "7";
                    import ceylon.collection "1.2";
                    // comment
                    import "com.maven:module" "2.3"; // another comment
                    import one.more.ceylon.module "4.3";
                    /* that's it!
                    */
                }
                """

        assert result
        assert result.moduleName == 'com.hello.world2'
        assert result.version == '2.3'
        assert result.imports == [
                [ name: 'java.lang.base', version: '7' ],
                [ name: 'ceylon.collection', version: '1.2' ],
                [ name: 'com.maven:module', version: '2.3' ],
                [ name: 'one.more.ceylon.module', version: '4.3' ],
        ]
    }

    @Test
    void "Can parse a module file with doc Strings"() {
        def result = parser.parse 'myfile.ceylon', """
                "This module is very \\"cool\\".
                 It lets you do lots of things with [[MyType]].

                 Example:

                     value something = MyType(\\"Hello there!\\");

                 Enjoy!!!"
                module com.hello.world "1.0.0" {
                    import java.lang.base  "7";
                }
                """

        assert result
        assert result.moduleName == 'com.hello.world'
        assert result.version == '1.0.0'
        assert result.imports == [
                [ name: 'java.lang.base', version: '7' ]
        ]
    }

}
