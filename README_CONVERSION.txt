(1) Download the contents of the git repository kbletzer/standalone to a folder, following the clone github procedures.  Make a note of the path to the standalone directory.  The standalone directory is created in the location where the following git clone command is run.  This folder should contain folders named test and lse, along with other files.
> git clone git@github.com:kbletzer/standalone.git

(2) Run the script buildConversion.sh in the standalone folder.    The buildConversion.sh script will build all the java classes that are part of the conversion work.  Note that the script does not assume any edits to the classpath variable - the path to the files is explicitly included in the build commands.

> sh buildConversion.sh

(3) Each of the six conversion classes takes a file as an input when run in "standalone" mode.  The classes are
* EFGToXML - accepts a .efg format file and converts it to an extensive game format XML file
* NFGToXML - accepts a .nfg format file and converts it to an strategic game format XML file
* StrategicFileToXML - accepts a flat file with one or two matrices converts it to a strategic game format XML file
* XMLToEFG - accepts an extensive game form XML file and converts it to an .efg file
* XMLToNFG - accepts a strategic game XML file and converts it to an .nfg file
* XMLToLaTeX - accepts a strategic game XML file and converts it to a LaTeX file following the required format for the bimatrixgame.sty macro


From a directory containing the input files run the conversion program(s) as follows:

> java -cp <path to standalone folder> lse.standalone.EFGToXML e01.efg

where EFGToXML above can be replaced by any of the other conversion programs, and the file e01.efg can be any file with the file format expected by the conversion program, as noted above.  The <path to standalone folder> is the path noted in step 1.  If the path to the standalone folder is added to the java classpath, the -cp <path to standalone folder> option can be omitted.

The default output is a file with the same name as the input file and the extension updated to correspond with the output type.   For the example above the output file will be e01.xml.  Note that there is no error checking and each input file is assumed to meet the specifications for its file type.

(4) Further documentation can be found here: <URL to project documentation, to be updated>.

  