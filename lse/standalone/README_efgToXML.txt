The class efgToXML will create a xml document starting from an input document of .efg format (Gambit extensive form format).

This version as uploaded on/near June 30 is the preliminary "DRAFT" of the work, which may be incorporated into the 
gte tool at some point in the future.

***Setup 

compile efgToXML.java as follows (using -cp option if directory is not in classpath):

>>  javac efgToXML.java


***Usage examples for first method of using class
call the class on a matrix file:

>> java lse.standalone.efgToXML test1.efg

a .xml file will be produced in the same directory as the program is called from; for example, for the above input, the 
output file name would be test1.xml


