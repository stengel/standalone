The class normalFormToLaTeX will create a formatted LaTeX document from a matrix or pair of matrices
representing game payoffs. 

There are three possible ways to provide information to the class.  The first is to use the class as a 
standalone executable and pass a filename containing the matrix information.  The information should be
 formatted as follows:

1 2 3
2 2 1

4 2 1
3 3 2   

The size of the matrices will be auto-detected. Use of one matrix will format the output as single payoff mode.

The second way of invoking the class is via another java class, where the filename is passed in to the class 
constructor.  See standaloneClassTester.java for an example.

The third way of invoking the class is via another java class, where the information is passed as arrays of 
Strings.  See standaloneClassTester.java for an example.

***Setup 

compile normalFormToLaTeX.java as follows:

>>  javac normalFormToLaTeX.java


***Usage examples for first method of using class
call the class on a matrix file:

>> java normalFormToLaTeX matrix22.txt

a .tex file will be produced with the name matrix22.txt.tex

OR

please see the contents of standaloneClassTester.java for more details on how to format payoff matrices passed 
in via parameter.  To run that test class compile via 

>> javac standaloneClassTester.java

and run

>> java standaloneClassTester.java

(on Unix) run shell script to generate & view dvi files if required:

>> sh tex2dvi.sh

