package com.squarepeace.nnppss.service;

import com.squarepeace.nnppss.model.Console;

/**
 * Abstraction over pkg2zip so that the download coordinator can be tested
 * without running external processes.
 */
public interface PackageExtractor {

    void extractPackage(String pkgName, String zRifKey, Console console,
                        PackageService.ExtractionListener listener);
}
