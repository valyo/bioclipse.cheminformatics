/*******************************************************************************
 *Copyright (c) 2008 The Bioclipse Team and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package net.bioclipse.cml.managers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import net.bioclipse.cml.contenttypes.CmlFileDescriber;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.managers.business.IBioclipseManager;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLElement;

public class ValidateCMLManager implements IBioclipseManager {

    public String validate( String filename ) throws IOException,
                                             BioclipseException {

        IFile file =
                ResourcesPlugin.getWorkspace().getRoot()
                        .getFile( new Path( filename ) );
        if ( !file.exists() )
            throw new BioclipseException( "File " + filename
                                          + " does not exist" );
        return validateCMLFile( file );
    }

    public void validate( IFile input ) throws IOException {

        final String result = validateCMLFile( input );
        Display.getDefault().syncExec( new Runnable() {

            public void run() {

                MessageBox mb = new MessageBox( new Shell(), SWT.OK );
                mb.setText( "CML checked" );
                mb.setMessage( result );
                mb.open();
            }
        } );
    }

    public String getManagerName() {

        return "cml";
    }

    private String validateCMLFile( IFile input ) {

        boolean succeeded = true;
        StringBuffer returnString = new StringBuffer();
        InputStream is = null;
        try {
            is = input.getContents();
            Element element =
                    (Element) new Builder().build( is ).getRootElement();
            if ( !element.getNamespaceURI().equals(CmlFileDescriber.NS_CML)) {
                returnString.append(
                	"Namespace is not " + CmlFileDescriber.NS_CML + "; "
                );
                succeeded = false;
            }
            is.close();
            is = input.getContents();
            
        } catch ( ParsingException e ) {
            returnString.append( e );
            succeeded = false;
        } catch ( Exception e ) {
            returnString.append( e );
            succeeded = false;
        } finally {
            try {
                is.close();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        if ( succeeded ) {
            // here attribute validation could follow
            return ("Input is valid CML. " + returnString.toString());
        } else {
            return ("Input is not valid CML: " + returnString.toString());
        }
    }

    public CMLElement fromString(String cmlString) throws BioclipseException {
        CMLBuilder builder = new CMLBuilder();
        try {
            Document doc =  builder.buildEnsureCML(new StringReader(cmlString));
            return (CMLElement)doc.getRootElement();
        } catch (IOException e) {
            throw new BioclipseException("Could not read the cmlString.", e);
        } catch (ParsingException e) {
            throw new BioclipseException(
                "Could not parse the cmlString; " + e.getMessage(),
                e
            );
        }
    }

    public CMLElement parseFile(IFile input) throws BioclipseException {
        CMLBuilder builder = new CMLBuilder();
        try {
            Document doc =  builder.buildEnsureCML(new InputStreamReader(input.getContents()));
            return (CMLElement)doc.getRootElement();
        } catch (CoreException e) {
            throw new BioclipseException("Could not read the cmlString.", e);
        } catch (IOException e) {
            throw new BioclipseException("Could not read the cmlString.", e);
        } catch (ParsingException e) {
            throw new BioclipseException(
                "Could not parse the cmlString; " + e.getMessage(),
                e
            );
        }
    }
}
