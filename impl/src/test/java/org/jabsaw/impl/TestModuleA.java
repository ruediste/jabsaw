package org.jabsaw.impl;

import org.jabsaw.Module;

@Module(exported = TestModuleB.class, includePackage = false, include = {
		TestModuleA.class, TestClassA.class })
public class TestModuleA {

}
