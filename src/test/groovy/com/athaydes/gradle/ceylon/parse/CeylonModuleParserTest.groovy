package com.athaydes.gradle.ceylon.parse

import org.junit.Test

class CeylonModuleParserTest {

    def parser = new CeylonModuleParser()

    @Test
    void "Can parse module with multi-line block comment"() {
        def result = parser.parse( 'example.ceylon', """
        /* a comment
           spanning several lines
           Some [[Example]] markup.
           And even some code:
           <code>shared function() {
               print("Hello world!");
           }
           </code>
        */
        module my.test.module "2.0" {
           // this is an import
           import other.module "1.0";
        }
        """ )

        assert result
        assert result.moduleName == 'my.test.module'
        assert result.version == '2.0'
        assert result.imports == [ [ name: 'other.module', version: '1.0' ] ]
    }

    @Test
    void "Can parse module with multi-line doc comment with block comment inside it"() {
        def result = parser.parse( 'example.ceylon', """
        "A comment
         spanning several lines
         Some [[Example]] markup.
         And even some code:
             shared function() {
                 print(\\"Hello world!\\");
             }

          /* block comments
             are acceptable inside doc comments.
           */
         "
        module my.test.module "0" {
           "This import is required.
            The \\"1.0\\" is the version!
            "
           import other.module "1.0";
        }
        """ )

        assert result
        assert result.moduleName == 'my.test.module'
        assert result.version == '0'
        assert result.imports == [ [ name: 'other.module', version: '1.0' ] ]
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
    void "Can parse a simple module file without any imports"() {
        def result = parser.parse 'myfile.ceylon', 'module com.hello.world "1.0" {}'

        assert result
        assert result.moduleName == 'com.hello.world'
        assert result.version == '1.0'
        assert result.imports == null
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

    @Test
    void "Can parse a module file with Maven imports"() {
        def result = parser.parse 'myfile.ceylon', """
                module com.hello.world "1.0" {
                    import "org.apache.logging.log4j:log4j-core"  "2.4.1";
                    import "org.apache.logging.log4j:log4j-api"  "2.4.1";
                    import "org.junit:junit"  "4.12";
                    import ceylon.collection "1.2";
                }
                """

        assert result
        assert result.moduleName == 'com.hello.world'
        assert result.version == '1.0'
        assert result.imports == [
                [ name: 'org.apache.logging.log4j:log4j-core', version: '2.4.1' ],
                [ name: 'org.apache.logging.log4j:log4j-api', version: '2.4.1' ],
                [ name: 'org.junit:junit', version: '4.12' ],
                [ name: 'ceylon.collection', version: '1.2' ]
        ]
    }

    @Test
    void "Can parse a module file with annotated imports and module"() {
        def result = parser.parse 'myfile.ceylon', """
                "An example module
                 with \\"lots\\" of annotations"
                shared("jvm")
                module com.hello.world "1.0.0" {
                    shared import java.lang.base  "7";
                    shared("js") import org.jquery "2.5";
                    // normal import
                    import ceylon.promise "1.2";
                }
                """

        assert result
        assert result.moduleName == 'com.hello.world'
        assert result.version == '1.0.0'
        assert result.shared == true
        assert result.imports == [
                [ name: 'java.lang.base', version: '7', shared: true ],
                [ name: 'org.jquery', version: '2.5', shared: true ],
                [ name: 'ceylon.promise', version: '1.2' ],
        ]
    }

    @Test
    void "Can parse module file used in Ceylon Specification"() {
        def result = parser.parse 'myfile.ceylon', """
                |"The best-ever ORM solution!"
                |license ("http://www.gnu.org/licenses/lgpl.html")
                |module org.hibernate "3.0.0.beta" {
                |    shared import ceylon.language "1.0.1";
                |    import javax.sql "4.0";
                |}
                |""".stripMargin()

        assert result
        assert result.moduleName == 'org.hibernate'
        assert result.version == '3.0.0.beta'
        assert result.imports == [
                [ name: 'ceylon.language', version: '1.0.1', shared: true ],
                [ name: 'javax.sql', version: '4.0' ]
        ]
    }
}
