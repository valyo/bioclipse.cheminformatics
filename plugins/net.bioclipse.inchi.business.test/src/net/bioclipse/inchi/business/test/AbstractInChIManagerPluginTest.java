/*******************************************************************************
 * Copyright (c) 2008  Egon Willighagen <egonw@users.sf.net>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contact: http://www.bioclipse.net/
 ******************************************************************************/
package net.bioclipse.inchi.business.test;

import org.junit.Assert;
import org.junit.Ignore;

import net.bioclipse.cdk.business.ICDKManager;
import net.bioclipse.core.domain.IMolecule;
import net.bioclipse.inchi.InChI;
import net.bioclipse.inchi.business.IInChIManager;

import org.junit.Test;

public abstract class AbstractInChIManagerPluginTest {

    protected static IInChIManager inchi;
    protected static ICDKManager cdk;

    @Test
    public void testInitialization() {
    	Assert.assertNotNull(inchi);
    }
    
    @Test
    public void testGenerate() throws Exception {
        IMolecule mol = cdk.fromSMILES("C");
        Assert.assertNotNull("Input structure is unexpectedly null", mol);
        InChI inchiObj = inchi.generate(mol);
        Assert.assertNotNull(inchiObj);
        Assert.assertEquals("InChI=1S/CH4/h1H4", inchiObj.getValue());
    }

    @Test
    public void testGenerateWithOptions() throws Exception {
        IMolecule mol = cdk.fromSMILES("OC=O");
        Assert.assertNotNull("Input structure is unexpectedly null", mol);
        InChI inchiObj = inchi.generate(mol, "FixedH");
        Assert.assertNotNull(inchiObj);
        Assert.assertEquals("InChI=1/CH2O2/c2-1-3/h1H,(H,2,3)/f/h2H", inchiObj.getValue());
    }

    @Test
    public void testGenerateNoStereo() throws Exception {
        IMolecule mol = cdk.fromSMILES("ClC(Br)(F)(O)");
        Assert.assertNotNull("Input structure is unexpectedly null", mol);
        InChI inchiStr = inchi.generate(mol);
        Assert.assertNotNull(inchiStr);
        Assert.assertEquals(
            "InChI=1S/CHBrClFO/c2-1(3,4)5/h5H",
            inchiStr.getValue()
        );
    }

    @Test
    public void testGenerateKey() throws Exception {
        IMolecule mol = cdk.fromSMILES("C");
        Assert.assertNotNull("Input structure is unexpectedly null", mol);
        InChI key = inchi.generate(mol);
        Assert.assertNotNull(key);
        Assert.assertEquals(
            "VNWKTOKETHGBQD-UHFFFAOYSA-N",
            key.getKey()
        );
    }
}
