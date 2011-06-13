package lse.standalone;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

//to-do
//ability to enter parameters 1, 4, 5 (size of cell, name of row player, name of col player) (?)
//move the bimatrix macro format to file instead of hard-coding (?)
//error handling for mal-formed inputs (?)

//conversion to .eps format
public class normalFormToLaTeX 
{
	private ArrayList<ArrayList<String>> a1;
	private ArrayList<ArrayList<String>> a2;
	
	private boolean[][] br1;  //best response corresponding to a1
	private boolean[][] br2;  //best response corresponding to a2
	
	private int numRows;    //number of rows in payoff matrices
	private int numCols;	//number of columsn in payoff matrices
	
	private String filename;
	
	private boolean bSinglePayoff = false; //will be updated based on input matrices
	private boolean bPrettyFraction = true;  //apply additional formatting to fractions - yes or no
	private boolean bShowBestResponse = true;  //show a box around the best response
	
	/* constructor - by file which defines matrices */
	public normalFormToLaTeX(String fn)
	{
		filename = fn;
		splitMatrices(fileToArrayList());
		if (bShowBestResponse & !bSinglePayoff) { this.calculateBestResponse(); }
	}
	
	public normalFormToLaTeX(String fn, boolean bFraction, boolean bBestResponse)
	{
		this(fn);
		bPrettyFraction = bFraction;
		bShowBestResponse = bBestResponse;
	}
	
	/* constructor - payoff matrices passed in as parameters */
	public normalFormToLaTeX(String[][] p1, String[][] p2 )
	{
		filename = "default";
		//convert input strings to arraylist
		a1 = arrayToArrayList2D(p1);
		
		if (p2 !=null ) { a2 = arrayToArrayList2D(p2); }
		else
		{
			bSinglePayoff = true;
			a2 = a1;
		}
		
		this.numCols = a1.get(0).size();
		this.numRows = a1.size();
		
		if (this.bShowBestResponse & !bSinglePayoff) { this.calculateBestResponse(); }
		else { this.bShowBestResponse = false; }
	}
	
	public normalFormToLaTeX(String[][] p1, String[][] p2, boolean bFraction, boolean bBestResponse)
	{
		this(p1, p2);
		bPrettyFraction = bFraction;
		bShowBestResponse = bBestResponse & !bSinglePayoff;
	}
	
	public void setFilename(String fn)
	{
		filename = fn;
	}
	
	public void setShowBestResponse(boolean b)
	{
		bShowBestResponse = b;
	}
	
	public void setPrettyFraction(boolean b)
	{
		bPrettyFraction = b;
	}
	
	//reads the input file 
	//outputs an ArrayList containing the list of lines (strings) that make up the file
	private ArrayList<String> fileToArrayList() 
	{
		ArrayList<String> fileLines = new ArrayList<String>();
		String fileLine;
		
		try
        {
			BufferedReader reader = new BufferedReader(new FileReader(filename));	
			
			//read all the lines from the file
			while ((fileLine = reader.readLine()) != null) 
			{
				fileLines.add(fileLine);
		    }
		    reader.close();
        }
		catch (Exception e)
		{
			//raise error regarding unsuccessfully reading the file
		}
		
		return fileLines;
	}
	
	//there may be either one or two matrices in the input text
	//if one, duplicate to make 2 symmetric matrices
	//otherwise output two as expected
	@SuppressWarnings({"unchecked"})
	private void splitMatrices(ArrayList<String> fileLines)
	{
		a1 = new ArrayList<ArrayList<String>>();
		a2 = new ArrayList<ArrayList<String>>();
		boolean bSwitch = false;
		
		//loop through all the lines
		//putting them either into lists a1 or a2
		for (Iterator<String> i = fileLines.iterator(); i.hasNext();) 
		{
			String fileLine = (String)i.next();
			
			if (bSwitch) 
			{
				a2.add(this.textToArray(fileLine));
			}
			else if (fileLine.trim().length() == 0)  //check for blank dividing line
			{
				bSwitch = true;
			}
			else
			{
				a1.add(this.textToArray(fileLine));
			}
		}
		
		if (a2.size() < 1)
		{
			//copy array 1 to array 2 since there was no array 2 in the file
			//clone is a shallow copy; don't need deep copy here
			a2 = (ArrayList<ArrayList<String>>)a1.clone();
			bSinglePayoff = true;
			bShowBestResponse = false;
		}
		
		this.numCols = a1.get(0).size();
		this.numRows = a1.size();
	}
	
	//convert string to double
	//assumes either integer or fractional entries
	//does not handle entries with decimal point
	private double stringToDouble(String s)
	{
		double n = 999.999;
		String[] sFraction;
		int num;
		int den;
		
		try //process integer
		{
			n = (double)Integer.parseInt(s);
		}
		catch (NumberFormatException nfe)  //process fraction
		{
			Pattern pat = Pattern.compile("/");
			Matcher m = pat.matcher(s);
			
	        if (m.find()) //fractional input
	        { 
	        	sFraction = pat.split(s);
	        	num = Integer.parseInt(sFraction[0]);
	        	den = Integer.parseInt(sFraction[1]);
	        	n = ((double)num)/((double)den);
	        }
		}

		return n;
	}
	
	private void calculateBestResponse()
	{
		double[][] n1 = new double[this.numRows][this.numCols];
		double[][] n2 = new double[this.numRows][this.numCols];
		
		this.br1 = new boolean[this.numRows][this.numCols]; 
		this.br2 = new boolean[this.numRows][this.numCols];
		
		double[] rmax = new double[this.numRows];  //row max values
		double rtmp;
		
		double[] cmax = new double[this.numCols];  //column max values
		double ctmp;
		
		//calculate best responses for first player
		for(int i=0; i < this.numRows; i++)
		{
			rmax[i] = stringToDouble( a2.get(i).get(0) );
			for(int j = 0; j < this.numCols; j++)
			{
				rtmp = stringToDouble( a2.get(i).get(j) );
				n2[i][j] = rtmp;
				if (rmax[i] < rtmp ) { rmax[i] = rtmp; }
			}
		}
		
		//calculate best responses for second player
		for(int j=0; j < this.numCols; j++)
		{
			cmax[j] = stringToDouble( a1.get(0).get(j) );
			for(int i = 0; i < this.numRows; i++)
			{
				ctmp = stringToDouble( a1.get(i).get(j) );
				n1[i][j] = ctmp;
				if (cmax[j] < ctmp ) { cmax[j] = ctmp; }
			}
		}
		
		//store best response (T or F) in array for use in formatting table entry
		for(int j=0; j < this.numCols; j++)
		{
			for(int i = 0; i < this.numRows; i++)
			{
				if (n2[i][j] < rmax[i]) { br2[i][j] = false; }
				else { br2[i][j] = true; }
				
				if (n1[i][j] < cmax[j]) { br1[i][j] = false;  }
				else { br1[i][j] = true; }
			}
		}
	}
	
	//take a line of text, tokenize, and return as ArrayList
	private ArrayList<String> textToArray(String t)
	{
		StringTokenizer st = new StringTokenizer(t);
		ArrayList<String> row = new ArrayList<String>();
		
  	    while (st.hasMoreTokens() ) 
  	    {	
  	    	row.add(st.nextToken());
  	    } 
  	    return row;
	}
	
	//map tokens found in the input file to the bimatrix macro template
	//improvement: read from file instead of hard-coding 
	// \bimatrixgame{4mm}{2}{3}{I}{II}%
	//    {{T}{B}}%
	//    {{l}{c}{r}}
	//    {
	//    \payoffpairs{1}{012}{421}
	//    \payoffpairs{2}{301}{132}
	//    } 
	private String mapTokensToBMTemplate()
	{
		String s ="\\bimatrixgame{4mm}";
		s = s + "{" + numRows + "}{" +numCols + "}{I}{II}%\n";
		
		//row labels & column labels
		s = s + this.generateRowLabels();
		s = s + this.generateColumnLabels();
		
		//payoff pairs
		s = s + this.generatePayoffPairs();

		return s;
	}
	
	//create String representing LateX document
	public String getLaTeXString()
	{
		String s = "\\documentclass{article}\n\\input{gamesty}%\n\\begin{document} \n";
		s = s + this.mapTokensToBMTemplate();
		s =s + "\\end{document} \n";
		return s;
	}
	 
	//use String representing LateX document to create file on OS
	public void createLaTeXFile()
	{
		createLaTeXFile(this.filename + ".tex");
	}
	
	public void createLaTeXFile(String name)
	{
		try 
		{
		    BufferedWriter out = new BufferedWriter(new FileWriter(name));
		    out.write(""+ this.getLaTeXString());
		    out.close();
		} 
		catch (IOException e) 
		{
			//throw exception if can't print file
		}
	}
	
	private String generateRowLabels() 
	{
		 String label;
		 char chr;
		 switch (this.numRows)
		 {
	         case 1:  label = "{{M}}%\n";		break;
	         case 2:  label = "{{T}{B}}%\n";	break;
	         case 3:  label = "{{T}{M}{B}}%\n";	break;
	         default: 
	         {
	        	 label = "{";
	        	 chr = 97; //97 = ascii 'a' - label cols a, b, c, d, e...etc.
	        	 for (int i = 97; i < this.numRows+97; i++)
	        	 {
	        		 chr = (char) i;
	        		 label = label + "{" + chr + "}";
	        	 }
	        	 label = label + "}%\n";
	         }
		 }
	     return label;
	}
	
	private String generateColumnLabels() 
	{
		String label;
		 char chr;
		 switch (numCols)
		 {
	         case 1:  label = "{{c}}%\n";		break;
	         case 2:  label = "{{l}{r}}%\n";	break;
	         case 3:  label = "{{l}{c}{r}}%\n";	break;
	         default: 
	         {
	        	 label = "{";
	        	 chr = 97; 			//97 = ascii 'a'
	        	 for (int i = 97; i < numCols+97; i++)
	        	 {
	        		 chr = (char) i;
	        		 label = label + "{" + chr + "}";
	        	 }
	        	 label = label + "}\n";
	         }
		 }
	     return label;
	}
	
	private String generatePayoffPairs() 
	{
		//a1 and a2 are the ArrayLists containing the payoff matrices
		/* {
	    \payoffpairs{1}{012}{421}
	    \payoffpairs{2}{301}{132}
	    } */
		boolean bBox = false;
		String s="{\n";
		String r="";
		String strPayoff = "\\payoffpairs{";
		
		if (bSinglePayoff) {strPayoff = "\\singlepayoffs{"; }
		
		for(int i = 0; i < this.numRows; i++ )
		{
			r = strPayoff + (i+1) + "}{";
			
			for(int j = 0; j < this.numCols; j++) //player I payoffs
			{
				if (this.bShowBestResponse) { bBox = this.br1[i][j]; }
				r = r + wrapPayoff(a1.get(i).get(j), bBox);
			}
			r = r + "}" ;
			
			if (!bSinglePayoff)
			{
				r = r + "{";
				
				for(int j = 0; j < this.numCols; j++) //player II payoffs
				{
					if (this.bShowBestResponse) { bBox = this.br2[i][j]; }
					r = r + wrapPayoff(a2.get(i).get(j), bBox);
				}
				r = r + "}";
			}
			s = s + r + "\n";
		}
		
		s = s + "}\n";
		return s;
	}
	
	//at the individual payoff level
	private String wrapPayoff(String p, boolean bBox)
	{
		String lBox = "";
		String rBox = "";
		
		if (bBox) 
		{
			lBox = "{\\fbox ";
			rBox = "}";
		}
		
		if (!bPrettyFraction) {p = "{$" + p + "$}";}
		else
		{  
			boolean bNegFraction = false;
			
			Pattern pat = Pattern.compile("/");
			Pattern neg = Pattern.compile("-");
			Matcher m = pat.matcher(p);
			Matcher mneg = neg.matcher(p);
			
	        if (m.find()) //fractional input
	        { 
	        	//determine if fraction negative or positive: 
	        	//		if find one neg sign, and no others, then negative
	        	//-if (mneg.find() & !mneg.find()) { bNegFraction = true; }
	        	p = mneg.replaceAll("");  //eliminate negative signs, will format at front 
	        		
	        	String[] result = pat.split(p);
	      
	        	String negSign = "";
	        	if (mneg.find() & !mneg.find()) { negSign = "-"; }
	        	//-if (bNegFraction) { negSign = "-"; } 
	        	
	        	p = "{$" + negSign + "\\frac{"+result[0]+"}{"+result[1]+"}$}";
	        }
	        else {p = 	"{$" + p + "$}"; }  //not a fraction
		}
		return lBox + p + rBox;
	}

	private ArrayList<ArrayList<String>> arrayToArrayList2D(String[][] s)
	{
		ArrayList<ArrayList<String>> al = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < s.length; i ++)
		{
			ArrayList<String> tmp = new ArrayList<String>(Arrays.asList(s[i]));
			al.add(tmp);
		}
		
		return al;
	}
	
	public static void main (String [] args)
	{	
		String filename = args[0];
		normalFormToLaTeX ml = new normalFormToLaTeX(filename);
		ml.createLaTeXFile(); 
	}
}
