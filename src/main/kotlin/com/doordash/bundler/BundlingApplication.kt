package com.doordash.bundler

import com.google.ortools.Loader
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
class BundlingApplication

fun main(args: Array<String>) {
    Loader.loadNativeLibraries()
    runApplication<BundlingApplication>(*args)
}
