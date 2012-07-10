package net.bioclipse.cdk.ui.sdfeditor.business;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.formats.SDFFormat;
import org.openscience.cdk.io.iterator.IteratingMDLReader;

public class PropertiesImportFileHandler {

    private IFile sdFile;
    private IFile dataFile;
    private ArrayList<String> propertiesID, choosenPropID;
    private ArrayList<String> sdFilePropertiesID;
    private ArrayList<ArrayList<String>> topValues;
    private boolean topRowContainsPropName, propLinkedBy;
    private String dataFileLink, sdFileLink, newSDFilePath;
    // The number of rows read in to topValues at initiation 
    private final static int ROWS_IN_TOPVALUES = 5;
    
    /**
     * A constructor to use if non, or only one, of the files are known. 
     */
    public PropertiesImportFileHandler() { 
        propertiesID = new ArrayList<String>();
        choosenPropID = new ArrayList<String>();
        sdFilePropertiesID = new ArrayList<String>();
        topValues = new ArrayList<ArrayList<String>>();
        topRowContainsPropName = true;
        sdFileLink = "";
        dataFileLink = "";
        propLinkedBy = false;
    }
    
    /**
     * A constructor to use if both the files are known.
     * 
     * @param sdFile The IFile containing the sd-file
     * @param dataFile The IFile containing the data file
     * @throws FileNotFoundException Thrown if some, or both, of the files are 
     *      not found
     */
    public PropertiesImportFileHandler(IFile sdFile, IFile dataFile) throws FileNotFoundException {
        this();
        setSDFile( sdFile );
        setDataFile( dataFile );
    }
    
    /**
     * This method load the sd-file.
     * 
     * @param dataFile The IFile containing the sd-file
     * @throws FileNotFoundException Thrown if there's no file found
     */
    public void setSDFile(IFile sdFile) throws FileNotFoundException {
        this.sdFile = sdFile;
        sdFilePropertiesID.clear();
        extractSDFProerties();
    }
    
    /**
     * This method read the data in the sd-file.
     * 
     * @throws FileNotFoundException If the sd-file isn't found
     */
    private void extractSDFProerties() throws FileNotFoundException {
        /* FIXME I would love to use MoleculesFromSDF, but there's three obstacles:
         * - The method for adding properties isn't implemented get
         * - The method for saving SDFiles isn't implemented get
         * - The method for getting properties just returns an empty collection,
         *      I tried to implement the function, but I didn't succeeded... */
//        if (!sdFile.exists() || sdFile == null)
//            throw new FileNotFoundException ("Can't find the sd-file.");
//        MoleculesFromSDF molFrSDF = new MoleculesFromSDF(sdFile);
//        sdFilePropertiesID = new ArrayList<String>();
//        Collection<Object> properties = molFrSDF.getAvailableProperties();       
//        Iterator<Object> propItr = properties.iterator();
//        while ( propItr.hasNext() ) 
//            sdFilePropertiesID.add( propItr.next().toString() );
        
        /* This use the IteratingMDLReader in CDK and works*/       
        IteratingMDLReader sdfItr = new IteratingMDLReader( getSDFileContents(), DefaultChemObjectBuilder.getInstance() );
        Map<Object, Object> propertiesMap = sdfItr.next().getProperties();
        Set<Object> propSet = propertiesMap.keySet();
        Iterator<Object> propSetItr = propSet.iterator();
        sdFilePropertiesID.clear();
        while (propSetItr.hasNext())
            sdFilePropertiesID.add( propSetItr.next().toString() );
    }
    
    /**
     * This method opens a stream to the sd-file.
     * 
     * @return A stream to the sd-file
     */
    public InputStream getSDFileContents() {
        String path = sdFile.getFullPath().toOSString(); 
        try {
            return new FileInputStream(new File(path));
        } catch (FileNotFoundException e) {
            path = sdFile.getLocation().toOSString();
            try {
                return new FileInputStream(new File(path));
            } catch ( FileNotFoundException e1 ) {
                return null;
            }
        }
    }
    
    /**
     * A method to get the path to the sd-file. If the sd-file isn't loaded it
     * returns an empty string.
     * 
     * @return The path to the sd-file
     */
    public String getSDFilePath() {
        if (sdFileExists())
            return sdFile.getProjectRelativePath().toOSString();
        else
            return "";
    }
    
    /**
     * To check if the sd-file has been loaded.
     * 
     * @return True if the file has been loaded.
     */
    public boolean sdFileExists() {
        return (sdFile != null);
    }
    
    /**
     * Returns the properties of the first molecule in the sd-file.
     * 
     * @return The properties in the sd-file
     */
    public ArrayList<String> getPropertiesFromSDFile() {
        return sdFilePropertiesID;
    }
    
    /**
     * This method load the data file.
     * 
     * @param dataFile The IFile containing the txt-file
     * @throws FileNotFoundException Thrown if there's no file found
     */
    public void setDataFile(IFile dataFile) throws FileNotFoundException {
        this.dataFile = dataFile;
        propertiesID.clear();
        topValues.clear();
        readProperiesFile( 0, ROWS_IN_TOPVALUES );
    }
    
    /**
     * To check if the txt-file with data has been loaded.
     * 
     * @return True if the file has been loaded.
     */
    public boolean dataFileExists() {
        return ( dataFile != null );
    }
    
    public InputStream getDataFileContents() {
        String path = dataFile.getFullPath().toOSString(); 
        try {
            return new FileInputStream(new File(path));
        } catch (FileNotFoundException e) {
            path = dataFile.getLocation().toOSString();
            try {
                return new FileInputStream(new File(path));
            } catch ( FileNotFoundException e1 ) {
                return null;
            }
        }
    }
    
    public String getDataFilePath() {
        return dataFile.getProjectRelativePath().toOSString();
    }
    
    /**
     * This method returns the properties ID:s of the data file. I.e. the 
     * top row.
     * @return The top row of the data file.
     */
    public ArrayList<String> getPropertiesIDFromDataFile() {
        return propertiesID;
    }
    
    /**
     * This method returns the first number of rows from the data file.<br>
     * This method returns matrix made of two {@link ArrayList}s, where the 
     * outer <code>ArrayList</code> corresponds to the columns of the data
     * file and the inner to the rows, i.e. to get the i:th element of the j:te 
     * column write:<br> <code>[Name of the matrix].get(j).get(i)</code>; <br>
     * The top row of the data file are considered to be the ID:s of the 
     * properties and therefore NOT returned.
     *  
     * @param numberOfRows
     * @return A matrix with the properties in
     * @throws FileNotFoundException 
     */
    public ArrayList<ArrayList<String>> getTopValuesFromDataFile(int numberOfRows) throws FileNotFoundException {
        if (topValues.isEmpty() || topValues.get( 0 ).isEmpty())
            return new ArrayList<ArrayList<String>>();
        int rows;
        if (topRowContainsPropName)
            rows = topValues.get( 0 ).size() + 1;
        else
            rows = topValues.get( 0 ).size();
        if (numberOfRows == rows)
            return topValues;
        ArrayList<ArrayList<String>> temp = new ArrayList<ArrayList<String>>();
        ArrayList<String> temp2;
        if (numberOfRows < rows) {
            for (int j = 0; j < topValues.size(); j++) {
                temp2 = new ArrayList<String>();                
                for (int i = 0; i < topValues.get( j ).size(); i++) {
                    temp2.add( topValues.get( j ).get( i ) );
                }
                temp.add( temp2 );
            }
            return temp;
        } else {
            readProperiesFile( rows, numberOfRows );
            return topValues;
        }
    }
    
    /**
     * This method reads the data file. It assumes that the top row contatins
     * the names of the properties, If not: After loading the file use the 
     * method "propertiesNameInDataFile(boolean)".
     *  
     * @param startRow The row to start read from
     * @param endRow The last row to read
     * @throws FileNotFoundException Thrown if the file can't be found
     */
    private void readProperiesFile(int startRow, int endRow) throws FileNotFoundException {   
        /*
         * Read the first element and add that to an (local) array list and add 
         * that to as the first array list in propertiesData, take the next add 
         * that as the top element in a new (local) array list and add that 
         * array list as the second element of propertiesData etc...
         * 
         * then read the next line and add the first element of that as the 
         * Second element in the first array list of propertiesData, i.e.
         * propertiesData.get(0).add(data);
         * 
         * It might use to much memory if the hole file is read, maybe it should 
         * only read the first e.g. five elements of the file...?  
         */
        if (endRow <= startRow)
            return;
        if (endRow < ROWS_IN_TOPVALUES)
            endRow = ROWS_IN_TOPVALUES;
        propertiesID.clear();
        topValues.clear();
        
        Scanner fileScanner = new Scanner(getDataFileContents());
        Scanner lineScanner;
        ArrayList<String> columns;
        int column, row = 0;
        String line, element;
        while (fileScanner.hasNextLine() && row <= endRow) {
            while (row < startRow) {
                fileScanner.next();
                row++;
            }

            column = 0;
            line = fileScanner.nextLine();
            lineScanner = new Scanner(line);
            lineScanner.useDelimiter("\t"); // Separated by a tab...
            while (lineScanner.hasNext()) {
                element = lineScanner.next();
                if (topRowContainsPropName) {
                    if (row == 0) {
                        propertiesID.add( element );
                    } else {   
                        /* If we are reading the first row with data then we create a new ArrayList
                         *  for each column*/
                        if (row == 1) {
                            columns = new ArrayList<String>();
                            topValues.add( columns );
                        }
                        topValues.get( column ).add( element );
                    }
                } else {
                    /* If we are reading the first row with data then we create a new ArrayList
                     *  for each column*/
                    if (row == 0) {
                        columns = new ArrayList<String>();
                        topValues.add( columns );
                    }
                    topValues.get( column ).add( element );
                }
                column++;
            }
            row++;
        }
    }

    public void meargeFiles(ArrayList<String> propertiesName, 
                            boolean propNameInDataFile) throws FileNotFoundException {
        /* In this case we don't want to remove any properties, so lets send in 
         * an empty ArrayList */
        meargeFiles( new ArrayList<String>(), propertiesName, propNameInDataFile );
    }
    
    public void meargeFiles(ArrayList<String> exludedProerties, 
                            ArrayList<String> propertiesName,
                            boolean propNameInDataFile) throws FileNotFoundException {
//        if (!sdFile.exists() || !dataFile.exists())
//                throw new FileNotFoundException ("Can't find one or both files...");
//        System.out.println(exludedProerties.toString());
        ArrayList<ArrayList<String>> properties = readAllData( exludedProerties, propertiesName, propNameInDataFile );
        IteratingMDLReader sdfItr = new IteratingMDLReader( getSDFileContents(), DefaultChemObjectBuilder.getInstance() );
        int index = 1, propIndex = 0, index2;
        
        /* We can't write to a file we are reading from, so we create an new 
         * sd-file where we save the data. If the path to where to save the new 
         * file isn't set we save it where the other sd-file is */
        if ( newSDFilePath == null || newSDFilePath.isEmpty() )
            setPathToNewSDFile( sdFile.getLocation().toOSString() );
        
        String newFile = sdFile.getName();
        index2 = newFile.indexOf( "\u002E" ); // the Unicode for for '.'
        newFile = newSDFilePath + newFile.substring( 0, index2 ) + "\u005Fnew\u002E" + sdFile.getFileExtension();
        
        FileOutputStream out = new FileOutputStream(newFile);
        SDFWriter writer = new SDFWriter( out );
      
        if (propLinkedBy)
            propIndex = properties.get( 0 ).indexOf( sdFileLink );
        while ( sdfItr.hasNext() ||  index < properties.size() ) {
            IChemObject mol = sdfItr.next();
            if (propLinkedBy) {
                /* This option only add the properties to the molecules where 
                 * the linked properties has the same value.*/ 
                String molProp = mol.getProperty( sdFileLink ).toString();
                if ( molProp != null && !molProp.isEmpty() ) {
                    if ( properties.get( index ).get( propIndex ).equals( molProp ) )
                        addPropToMol( mol, properties.get( 0 ), properties.get( index ), exludedProerties ); 
                }
            } else {
                addPropToMol( mol, properties.get( 0 ), properties.get( index ), exludedProerties );
            }
            try {
                writer.write( mol );
            } catch ( CDKException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            index++;
        }
        try {
            writer.close();
            out.close();
        } catch ( IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//        System.out.println(out.toString()); 
         
//        System.out.println(properties.size() + "x"+ properties.get( 0 ).size());
//        Iterator<ArrayList<String>> colItr = properties.iterator();
//        while ( colItr.hasNext() ) {
//            Iterator<String> eItr = colItr.next().iterator();
//            while ( eItr.hasNext() ) {
//                System.out.print( "<"+ eItr.next() + ">\t" );
//            }
//            System.out.println();
//        }
        
       //TODO Write it... But for this it would be nice if I could use MoleculesFromSDF here...
        // A hint of how to write to the file, see also notes...
//        ICDKManager cdk = Activator.getDefault().getJavaCDKManager();
//        try {
//            ICDKMolecule mol = cdk.fromSMILES( "cccc" );
//            mol.getAtomContainer().setProperty( "wee", "how" );
//            ByteArrayOutputStream out= new ByteArrayOutputStream();
//            SDFWriter writer = new SDFWriter( out );
//            writer.write( mol.getAtomContainer() );
//            writer.close();
//            System.out.println(out.toString());
//            
//            
//        } catch ( BioclipseException e ) {
//            e.printStackTrace();
//        } catch ( CDKException e ) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch ( IOException e ) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        throw new UnsupportedOperationException(this.getClass().getName()+
//                " does not support this operation yet");

    }
    
    private void addPropToMol( IChemObject mol, ArrayList<String> propNames,
                               ArrayList<String> propValues, ArrayList<String> exludedProp) {
         Iterator<String> namesItr = propNames.iterator();
         Iterator<String> valueItr = propValues.iterator();         
         String name = "";

         while ( namesItr.hasNext() ) {
             name = namesItr.next();
             if (!exludedProp.contains( name ) )
                 mol.setProperty( name, valueItr.next() );
             else
                 valueItr.next();
         }   
//         System.out.println(mol.getProperties().toString());
        
    }

    public void setPathToNewSDFile(String path) {
        String separator = System.getProperty( "file.separator" );
        int index = path.lastIndexOf( separator );
        newSDFilePath = path.substring( 0, index + 1 );
    }
//    private void linkPropToMol(IChemObject mol, ArrayList<String> propNames,
//                               ArrayList<String> propValues) {
//        Iterator<String> namesItr = propNames.iterator();
//        Iterator<String> valueItr = propValues.iterator();
//        while ( namesItr.hasNext() )
//            if ()
//            mol.setProperty( namesItr.next(), valueItr.next() );
//        
//    }
    
    private ArrayList<ArrayList<String>> readAllData(ArrayList<String> exludedProerties, 
                                                    ArrayList<String> propertiesName, 
                                                    boolean propNameInDataFile) {
        ArrayList<ArrayList<String>> properties = new ArrayList<ArrayList<String>>();
        Scanner fileScanner = new Scanner( getDataFileContents() );
        Scanner lineScanner;
        ArrayList<String> columns;
        
//        if ( !propNameInDataFile ) { 
//            properties.add( propertiesName );
//        }
        // TODO Don't read the excluded properties
        while ( fileScanner.hasNextLine() ) {
            columns = new ArrayList<String>();
            lineScanner = new Scanner( fileScanner.nextLine() );
            lineScanner.useDelimiter( "\t" ); // Separated by a tab...
            while ( lineScanner.hasNext() ) {
                columns.add( lineScanner.next() );
            }
            properties.add( columns );
        }
        
        if (propLinkedBy) {
            int index = properties.get( 0 ).indexOf( dataFileLink );
            properties.get( 0 ).set( index, sdFileLink );
        }
        
        return properties;
    }
    
    /**
     * Use this method to tell the file-handler if the top row of the data file 
     * contains the names of the properties.
     * 
     * @param exists True if the top row contains the properties names
     */
    public void propertiesNameInDataFile(boolean exists) {
        topRowContainsPropName = exists;
        updatePropertiesLists();
    }
    
    /**
     * Use this to check whether the top row of the data file is said to 
     * contain the name of the properties.
     * 
     * @return True if the top row is the name of the properties
     */
    public boolean isPropertiesNameInDataFile() {
        return topRowContainsPropName;
    }

    /**
     * This method add the top row of topValues to propertiesID (if 
     * topRowContainsPropName is true) or add the propertiesID to the top row of
     * the topValues (if topRowContainsPropName is false).
     */
    private void updatePropertiesLists() {
        if(topRowContainsPropName) {
            // Add the top row of topValues to propertiesID
            propertiesID.clear();
            for (int i = 0; i < topValues.size(); i++) {
                propertiesID.add( topValues.get( i ).remove( 0 ) );
            }
        } else {
            // Add the propertiesID to the top row of the topValues
            int elements = topValues.size();
            for (int i = 0; i < elements; i++) {
                // Zero 'cos we remove the element... 
                topValues.get( i ).add( 0, propertiesID.remove( 0 ) );
                if (choosenPropID.size() == i)
                    choosenPropID.add( i, "" );
            }
        }
    }
    
    public void setLinkProperties(boolean linkedBy, String dataFileProp, String sdFileProp) {
        if (linkedBy) {
            propLinkedBy = linkedBy;
            dataFileLink = dataFileProp;
            sdFileLink = sdFileProp;
        } else {
            propLinkedBy = linkedBy;
            dataFileLink = "";
            sdFileLink = "";
        }
//        System.out.println("propLinkedBy: "+propLinkedBy);
//        System.out.println("dataFileLink: "+dataFileLink);
//        System.out.println("sdFileLink: "+sdFileLink+"\n");
    }
    
    public void setdataFileLink(String dataFileProp) {
        dataFileLink = dataFileProp;
//        System.out.println("dataFileLink: "+dataFileLink+"\n");
    }
    
    public void setsdFileLink(String sdFileProp) {
        sdFileLink = sdFileProp;
//        System.out.println("sdFileLink: "+sdFileLink+"\n");
    }
    
    public void addChoosenPropID(int index, String propName) {
        choosenPropID.add( index, propName );
//        for (int i = 0; i < choosenPropID.size(); i++)
//            System.out.print( choosenPropID.get( i ) + ", " );
//        System.out.println();
    }
}