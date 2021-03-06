/*******************************************************************************
 * Copyright (c) 2008-2009  Egon Willighagen <egonw@users.sf.net>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contact: http://www.bioclipse.net/
 ******************************************************************************/
package net.bioclipse.cdk.ui.wizards;

import static org.openscience.cdk.graph.ConnectivityChecker.isConnected;
import static org.openscience.cdk.graph.ConnectivityChecker.partitionIntoMolecules;
import static org.openscience.cdk.tools.manipulator.AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import net.bioclipse.cdk.business.ICDKManager;
import net.bioclipse.cdk.domain.CDKMolecule;
import net.bioclipse.cdk.domain.ICDKMolecule;
import net.bioclipse.cdk.ui.Activator;
import net.bioclipse.core.util.LogUtils;
import net.bioclipse.ui.business.IUIManager;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.tools.AtomTypeAwareSaturationChecker;

public class NewFromSMILESWizard extends BasicNewResourceWizard {

    private static final Logger logger =
        Logger.getLogger(NewFromSMILESWizard.class);

    public static final String WIZARD_ID =
        "net.bioclipse.cdk.ui.wizards.NewFromSMILESWizard"; //$NON-NLS-1$
    
    private SMILESInputWizardPage mainPage;
    
    private String smiles = null;
    
    public void setSMILES(String smiles) {
        this.smiles = smiles;
    }

    public String getSMILES() {
        return smiles;
    }

    public boolean canFinish() {
        return getSMILES() != null;
    }
    
    public void addPages() {
        super.addPages();
        mainPage = new SMILESInputWizardPage("newFilePage0", this);//$NON-NLS-1$
        mainPage.setTitle("Open SMILES");
        mainPage.setDescription("Create a new resource from a SMILES"); 
        addPage(mainPage);
    }

    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        setWindowTitle("New Molecule From SMILES");
        setNeedsProgressMonitor(true);
    }

    private void doFinish(SubMonitor progress) throws Exception{
        ICDKManager cdk = net.bioclipse.cdk.business.Activator.getDefault()
                        .getJavaCDKManager();
        IUIManager ui = net.bioclipse.ui.business.Activator.getDefault()
                        .getUIManager();
        ICDKMolecule mol = cdk.fromSMILES( getSMILES() );
        List<ICDKMolecule> molecules;
        if ( !isConnected( mol.getAtomContainer() ) ) {
            IMoleculeSet containers = partitionIntoMolecules( mol
                            .getAtomContainer() );
            final AtomTypeAwareSaturationChecker ataSatChecker = new AtomTypeAwareSaturationChecker();
            molecules = new ArrayList<ICDKMolecule>( containers.getAtomContainerCount() );
            SubMonitor child = progress.newChild( containers.getAtomContainerCount() );
            for ( IAtomContainer container : containers.molecules() ) {
                final IMolecule betterMol = asCDKMolecule( container );
                percieveAtomTypesAndConfigureAtoms( betterMol );
                ataSatChecker.decideBondOrder( betterMol );
                molecules.add( new CDKMolecule( betterMol ) );
                child.worked( 1 );
            }
        } else {
            molecules = new ArrayList<ICDKMolecule>( 1 );
            molecules.add( mol );
        }

        progress.setWorkRemaining( molecules.size() );
        for ( ICDKMolecule molecule : molecules ) {
            // cdk.generate2dCoordinates(molecule);
            progress.worked( 1 );
            ui.open( molecule, "net.bioclipse.cdk.ui.editors.jchempaint.cml" );
        }
    }
    public boolean performFinish() {

        // Open editor with content (String) as content
        IRunnableWithProgress job = new IRunnableWithProgress() {

            public void run( IProgressMonitor monitor ) {

                final SubMonitor progress = SubMonitor.convert( monitor );
                progress.beginTask( "Creating molecule from SMILES", 200 );
                Executor executor = Executors.newSingleThreadExecutor();

                try {
                    progress.subTask( "Generating molecule" );
                    FutureTask<Void> future = new FutureTask<Void>(
                      new Callable<Void>() {

                          @Override
                          public Void call() throws Exception {
                              doFinish( progress );
                              return null;
                          }
                      } );
                    executor.execute( future );
                    waitForFuture( future, progress );
                    try {
                        future.get();
                    } catch (ExecutionException e) {
                        LogUtils.handleException( e, logger ,Activator.PLUGIN_ID);
                    }
                } catch ( InterruptedException e ) {
                    LogUtils.handleException( e, logger, Activator.PLUGIN_ID );
                } finally {
                }
            }
        };
        try {
            getContainer().run( true, true, job );
        } catch ( InvocationTargetException e ) {
            LogUtils.handleException( e, logger, Activator.PLUGIN_ID );
        } catch ( InterruptedException e ) {
            LogUtils.handleException( e, logger, Activator.PLUGIN_ID );
        }
        return true;
    }

    private void waitForFuture( Future<Void> future, IProgressMonitor progress )
                                                  throws InterruptedException {
        while ( !future.isDone() ) {
            progress.worked( 1 );
            if ( progress.isCanceled() ) {
                future.cancel( true );
                throw new OperationCanceledException();
            }
            Thread.sleep( 1000 );
        }
    }
    /**
     * Converts (if needed) a CDK {@link IAtomContainer} into a CDK
     * {@link IMolecule}.
     */
	private org.openscience.cdk.interfaces.IMolecule
	    asCDKMolecule(IAtomContainer container) {
		if (container instanceof org.openscience.cdk.interfaces.IMolecule)
			return (org.openscience.cdk.interfaces.IMolecule)container;

		return container.getBuilder().newInstance(
			org.openscience.cdk.interfaces.IMolecule.class, container
		);
	}
    
}
