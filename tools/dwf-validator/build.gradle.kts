plugins { kotlin("multiplatform") }

kotlin {
    jvm { testRuns["test"].executionTask.configure { useJUnit() } }
    js {
        nodejs { testTask { useKarma { useChromeHeadless() } } }
        binaries.executable()
    }

    sourceSets {
        val commonTest by getting { dependencies { implementation(kotlin("test")) } }
        val jsTest by getting { dependencies { implementation(kotlin("test")) } }
    }
}

tasks.register("test") {
    // dependsOn(":dwf-validator:jsTest")
    dependsOn(":dwf-validator:jvmTest")
}
