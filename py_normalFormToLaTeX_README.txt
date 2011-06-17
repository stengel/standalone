The python script normalFormToLaTeX.py will create a formatted LaTeX document from a file containing a matrix or pair of matrices representing game payoffs. 

The script expects a filename containing the matrix information.  The information should be formatted as follows:

1 2 3
2 2 1

4 2 1
3 3 2  

The size of the matrices will be auto-detected. Use of one matrix will format the output as single payoff mode.

call the script on a matrix file:

>> python normalFormToLaTeX.py -n matrix22.txt

The following optional parameters may be supplied:
-h		print the help text
-f		turn OFF the fraction formatting, it is ON by default
-b		turn OFF the best response formatting, it is ON by default

When the script is run a .tex file will be produced with the name <filename>.tex, for the above example the file would be named matrix22.txt.tex.