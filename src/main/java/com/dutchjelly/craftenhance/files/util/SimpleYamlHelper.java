package com.dutchjelly.craftenhance.files.util;

import org.broken.arrow.yaml.library.YamlFileManager;

import static com.dutchjelly.craftenhance.CraftEnhance.self;


/**
 * Helper clas to load and save data. You get data from one or several files and can save it also
 * have a serialize method you can use .
 */

public abstract class SimpleYamlHelper extends YamlFileManager {


	public SimpleYamlHelper(final String name, final boolean shallGenerateFiles) {
		this(name,  false, shallGenerateFiles);
	}

	public SimpleYamlHelper(final String path, final boolean singleFile, final boolean shallGenerateFiles) {
		super(self() ,path,singleFile,shallGenerateFiles);
	}

}