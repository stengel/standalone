#last modified June 17, 2011

import sys
import getopt

class normalFormToLaTeX:
	""" Convert a matrix file to a LaTeX file."""
	def __init__(self,fname, formatFraction = True, showBestResponse = True): 
		""" Instantiate the class, parse the file, and determine best responses in preparation for formatting."""

		self.a1 = []
		self.a2 = []	
		
		self.numRows = 0		# number of rows in payoff matrices, will be updated
		self.numCols = 0		# number of columns in payoff matrices, will be updated
	
		self.bSinglePayoff = False 				# will be updated based on input matrices
		self.bPrettyFraction = formatFraction		# apply formatting to fractions - yes or no
		self.bShowBestResponse = showBestResponse		# show a box around the best response

		self.filename = fname
		self.f = open(self.filename)
		self.lines = self.f.readlines() 
		self.splitMatrices()
	
		# best response corresponding to a1
		self.br1 = [[None for i in range(self.numCols)] for j in range(self.numRows)]	
		# best response corresponding to a2		
		self.br2 = [[None for i in range(self.numCols)] for j in range(self.numRows)]	

		if self.bShowBestResponse and not self.bSinglePayoff:
			 self.calculateBestResponse()

	def splitMatrices(self):
		""" Parse input matrix, determine rows & cols, and place into two nested lists, a1, a2."""
		bSwitch = False
		for li in self.lines:
			if bSwitch: 
				self.a2.append(li.split())
			elif not li.strip():  #checking for blank lines as delimiter between 1st & 2nd matrix
				bSwitch = True
			else:
				self.a1.append(li.split())
		
		if not self.a2:
			self.a2 = self.a1
			self.bSinglePayoff = True
			self.bShowBestResponse = False

		self.numCols = len(self.a1[0]);
		self.numRows = len(self.a1)

	def getLaTeXString(self):
		""" Generate the contents of the LaTeX file in string form."""
		s = "\\documentclass{article}\n\\input{gamesty}%\n\\begin{document} \n"
		s = s + self.mapTokensToBMTemplate()
		return s + "\\end{document} \n"

	def mapTokensToBMTemplate(self):
		""" Create the LaTeX bimatrix macro inputs and wrap in appropriate macro format."""
		s ="\\bimatrixgame{4mm}"
		s = s + "{" + `self.numRows` + "}{" + `self.numCols` + "}{I}{II}%\n"
		
		#row labels & column labels
		s = s + self.generateRowLabels()
		s = s + self.generateColumnLabels()
		
		#payoff pairs
		s = s + self.generatePayoffPairs()

		return s

	def generateRowLabels(self): 
		""" Generate the labels for the row player strategy according to the defaults."""
		if self.numRows == 1:
			label = "{{M}}%\n"
		elif self.numRows == 2:		
			label = "{{T}{B}}%\n"
		elif self.numRows == 3:
			label = "{{T}{M}{B}}%\n"
		else:
			label = self.generateDefaultLabels(self.numRows)
	
		return label

	def generateColumnLabels(self): 
		""" Generate the labels for the column player strategy according to the defaults."""
		if self.numCols == 1:
			label = "{{c}}%\n"
		elif self.numCols == 2:		
			label = "{{l}{r}}%\n"
		elif self.numCols == 3:
			label = "{{l}{c}{r}}%\n"
		else:
			label = self.generateDefaultLabels(self.numCols)
	
		return label

	def generateDefaultLabels(self, numLabels):
		""" Generate the default row or column labels for matrices with > 3 rows/cols."""
		#97 = ascii 'a' - label cols a, b, c, d, e...etc.	
		label = "{"	
		for i in range(97, 97 + numLabels):
			label = label + "{" + chr(i) + "}"
		return label + "}%\n"
	
	def generatePayoffPairs(self): 	
		""" Generate the formatted payoff pairs values for all payoff pairs."""
		bBox = False
		s="{\n"
		r=""
		strPayoff = "\\payoffpairs{"
		
		if (self.bSinglePayoff):
			strPayoff = "\\singlepayoffs{"
		
		for i in range(0, self.numRows):
			r = strPayoff + `i+1` + "}{"
			
			for j in range(0, self.numCols):   	# player I payoffs
				if (self.bShowBestResponse):
					bBox = self.br1[i][j]
				r = r + self.wrapPayoff(self.a1[i][j], bBox)

			r = r + "}"
			
			if not self.bSinglePayoff:
				r = r + "{"
				
				for j in range(0, self.numCols): # player II payoffs
					if (self.bShowBestResponse):
						bBox = self.br2[i][j]
					r = r + self.wrapPayoff(self.a2[i][j], bBox)

				r = r + "}"

			s = s + r + "\n"
		
		return s + "}\n"

	def wrapPayoff(self, p, bBox):
		""" Generate the formatted payoff pair text for one set of payoff pairs."""
		# format at the individual payoff level
		if bBox: 
			lBox, rBox = "{\\fbox", "}"
		else:
			lBox, rBox = "", ""

		
		if not self.bPrettyFraction:		# pretty fractions OFF
			p = "{$" + p + "$}"
		elif p.count("/") > 0:		# reformat payoff for nicer fractions
			if p.count("-") == 1: 	# negative number
				negSign = "-"
			else:
				negSign = ""
			
			# split into numerator and denominator
			pFrac = p.split("/")
			pFrac[0] = pFrac[0].lstrip("-")
			pFrac[1] = pFrac[1].lstrip("-")
			
			p = "{$" + negSign + "\\frac{" + pFrac[0] + "}{" + pFrac[1] + "}$}"
	        else: 				# not a fraction
			p = "{$" + p + "$}"
		
		return lBox + p + rBox;


	def stringToDouble(self, s):
		""" Convert a string representing an integer or fraction to floating point representation."""
		#convert string to double
		#assumes either integer or fractional entries
		#does not handle entries with decimal point

		#int num;
		#int den;
		
		if s.count("/") == 0:
			n = float(s)
		else:
			sFrac = s.split("/")
	        	num = float(sFrac[0])
	        	den = float(sFrac[1])
	        	n = num/den

		return n	

	def calculateBestResponse(self):
		""" Calculate the payoffs that represent the best response for the row and column players."""
		n1 = [[0.0 for i in range(self.numCols)] for j in range(self.numRows)]
		n2 = [[0.0 for i in range(self.numCols)] for j in range(self.numRows)]

		
		rmax = [0.0 for i in range(self.numRows)]  		#row max values
		# double rtmp;
		
		cmax = [0.0 for i in range(self.numCols)] 		#col max values
		# double ctmp;
		
		#calculate best responses for first player
		for i in range(0, self.numRows):
			rmax[i] = self.stringToDouble(self.a2[i][0])

			for j in range(0, self.numCols):
				rtmp = self.stringToDouble(self.a2[i][j])
				n2[i][j] = rtmp

				if rmax[i] < rtmp:
					rmax[i] = rtmp

		# calculate best responses for second player
		for j in range(0, self.numCols):
			cmax[j] = self.stringToDouble(self.a1[0][j])

			for i in range(0, self.numRows):
				ctmp = self.stringToDouble(self.a1[i][j])
				n1[i][j] = ctmp
				
				if cmax[j] < ctmp:
					 cmax[j] = ctmp

		# store best response (T or F) in array for use in formatting table entry later
		for j in range(0, self.numCols):
			for i in range(0, self.numRows):
				if n2[i][j] < rmax[i]:
					self.br2[i][j] = False
				else:
					self.br2[i][j] = True
				
				if n1[i][j] < cmax[j]:
					self.br1[i][j] = False
				else:
					self.br1[i][j] = True
	
	def createLaTeXFile(self):
		f = open(self.filename + ".tex", 'r+')
		f.write(self.getLaTeXString())

def main(args): 

	options, rest = getopt.getopt(args, 'fbhn:', ['FractionFormatOff', 'BestResponseOff', 'Filename', 'help'])

	bFormatFraction = True
	bShowBestResponse = True

	helpString = "usage: \npython normalFormToLaTeX.py\n"
	helpString += " -n FILENAME [(req.) file contains 1 or 2 matrices, separated by a blank line]\n -f [(opt.) turn fraction format OFF]\n"
	helpString += " -b [(opt.) turn best response format OFF]"

	helpPrinted = False

	for opt, arg in options:
    		if opt in ('-f', '--FractionFormatOff'):
       			bFormatFraction = False
   		elif opt in ('-b', '--BestResponseOff'):
        		bShowBestResponse = False
		elif opt in ('-n', '--Filename'):
			print arg
			filename = arg
		elif opt in ('-h', '--help'):
        		print helpString
			helpPrinted = True
	
	try:
		a = normalFormToLaTeX(filename, bFormatFraction, bShowBestResponse)
		a.createLaTeXFile() 
		print "Created file " + filename + ".tex"
	except:
		if not helpPrinted:
			print helpString

if __name__ == '__main__': 
	main(sys.argv[1:]) 
