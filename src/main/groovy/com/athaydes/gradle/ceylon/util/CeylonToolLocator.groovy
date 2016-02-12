package com.athaydes.gradle.ceylon.util

import groovy.transform.Memoized
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException

import java.util.concurrent.Callable

class CeylonToolLocator {


    @Memoized
    static String findCeylon( configLocation ) {
        if ( configLocation ) {
            return provided( configLocation )
        }
        List<String> options = ceylonHomeOptions() + sdkManOptions() + osOptions()
        def ext = Os.isFamily( Os.FAMILY_WINDOWS ) ? ".bat" : ""
        def ceylon = options.collect { new File( it + ext) }.find { it.file }

        if ( ceylon ) {
            return ceylon
        } else {
            return 'ceylon'
        }
    }

    private static String provided( configLocation ) {
        switch ( configLocation ) {
            case String: return configLocation
            case File: return configLocation.absolutePath
            case Callable: return provided( configLocation.call() )
            default: throw new GradleException( 'ceylonLocation must be a String | File | Callable' )
        }
    }

    private static List<String> ceylonHomeOptions() {
        def envVar = System.getenv( 'CEYLON_HOME' )
        if ( envVar ) {
            [ "$envVar/bin/ceylon", "$envVar/ceylon" ]
        } else {
            [ ]
        }
    }

    private static List<String> osOptions() {
        if ( Os.isFamily( Os.FAMILY_UNIX ) ) {
            [ '/usr/bin/ceylon', '/usr/local/bin/ceylon' ]
        } else if ( Os.isFamily( Os.FAMILY_MAC ) ) {
            [ '/usr/bin/ceylon', '/usr/local/bin/ceylon' ]
        } else if ( Os.isFamily( Os.FAMILY_WINDOWS ) ) {
            [ /C:\Program Files\Ceylon\bin\ceylon/,
              /C:\Program Files (x86)\Ceylon\bin\ceylon/ ]
        } else {
            [ ]
        }
    }

    private static List<String> sdkManOptions() {
        List<String> result = [ ]
        def sdkmanHome = System.getenv( 'SDKMAN_DIR' )
        if ( sdkmanHome ) {
            result << "$sdkmanHome/candidates/ceylon/current/bin/ceylon"
        }
        def userHome = System.getProperty( 'user.home' )
        if ( userHome ) {
            result << "$userHome/.sdkman/candidates/ceylon/current/bin/ceylon"
        }
        return result
    }

    static String error() {
        def markers = '=' * 50
        """
        |$markers
        |
        |The Ceylon-Gradle Plugin could not find Ceylon.
        |
        |Please try one of these suggestions to fix the problem:
        |
        |* Explicitly declare the Ceylon location using your build file:
        |
        |ceylon {
        |    ceylonLocation = "/usr/bin/ceylon" // path to Ceylon
        |}
        |
        |* Set the CEYLON_HOME environment variable to the Ceylon home directory
        |  (will look for ceylon at \$CEYLON_HOME/bin/ceylon).
        |
        |* Install Ceylon using SDKMAN! (http://sdkman.io/) by typing the following
        |  in the Terminal:
        |
        |  curl -s get.sdkman.io | bash
        |  source "\$HOME/.sdkman/bin/sdkman-init.sh"
        |  sdk install ceylon
        |
        |$markers
        |""".stripMargin()
    }

}
