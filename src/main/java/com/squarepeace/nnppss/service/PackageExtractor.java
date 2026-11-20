package com.squarepeace.nnppss.service;

import com.squarepeace.nnppss.model.Console;

import java.nio.file.Path;

/**
 * Abstraction over pkg2zip so that the download coordinator can be tested
 * without running external processes.
 */
public interface PackageExtractor {

    void extractPackage(Path pkgPath, Path outputDirectory, String zRifKey,
                        Console console, PackageService.ExtractionListener listener);
}
