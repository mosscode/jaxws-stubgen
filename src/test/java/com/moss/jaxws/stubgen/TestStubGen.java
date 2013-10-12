/**
 * Copyright (C) 2013, Moss Computing Inc.
 *
 * This file is part of jaxws-stubgen.
 *
 * jaxws-stubgen is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * jaxws-stubgen is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jaxws-stubgen; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.moss.jaxws.stubgen;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;

import com.moss.jaxws.stubgen.StubGen;

public class TestStubGen {

	@Test
	public void basic() throws Exception {
		
		File outputDir = new File("target/stubgen-output");
		
		if (outputDir.exists()) {
			if (!deleteDir(outputDir)) {
				throw new RuntimeException("Cannot delete existing output directory: " + outputDir);
			}
		}
		
		StubGen.generate(VehicleFactory.class, outputDir);
		
		/*
		 * Verify that @Oneway is processed correctly.
		 */
		
		File demandChangedResponseFile = new File(outputDir, "DemandChangedResponse.java"); 
		Assert.assertFalse(demandChangedResponseFile.exists());
	}
	
	private boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}
}
