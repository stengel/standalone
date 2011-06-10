import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Arrays;

//to-do
//ability to enter parameters 1, 4, 5 (size of cell, name of row player, name of col player) (?)
//move the bimatrix macro format to file instead of hard-coding (?)
//some error handling for mal-formed inputs?
//support for fractions (rational numbers) including 
//		change payoff type from string to Number? Rational (similar to gte)?
//conversion to .eps format
//computation of best responses for row and column player and surrounding the payoffs with \fbox
public class normalFormToLaTeX 
{
	private ArrayList<ArrayList<String>> a1;
	private ArrayList<ArrayList<String>> a2;
	private String filename;
	private Boolean bSinglePayoff = false;
	
	/* constructor - by file which defines matrices */
	public normalFormToLaTeX(String fn)
	{
		filename = fn;
		splitMatrices(fileToArrayList());
	}
	
	/* constructor - payoff matrices passed in as parameters */
	public normalFormToLaTeX(String[][] p1, String[][] p2 )
	{
		filename = "bm_latex";
		//convert input strings to arraylist since that's what we're working with in this class
		a1 = arrayToArrayList2D(p1);
		
		if (p2 !=null ) { a2 = arrayToArrayList2D(p2); }
		else
		{
			bSinglePayoff = true;
			a2 = a1;
		}
	}
	
	public void setFilename(String fn)
	{
		filename = fn;
	}
	
	//reads the input file 
	//outputs an ArrayList containing the list of lines (strings) that make up the file
	public ArrayList<String> fileToArrayList() 
	{
		ArrayList<String> fileLines = new ArrayList();
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
	public void splitMatrices(ArrayList<String> fileLines)
	{
		a1 = new ArrayList();
		a2 = new ArrayList();
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
		}
	}
	
	//take a line of text, tokenize, and return as ArrayList
	public ArrayList<String> textToArray(String t)
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
	public String mapTokensToBMTemplate()
	{
		//determine number of columns
		ArrayList<String> a1row;
		ArrayList<String> a2row;
		
		int numCols = a1.get(0).size();
		int numRows = a1.size();
		
		/* \bimatrixgame{4mm}{2}{3}{I}{II}%
	    {{T}{B}}%
	    {{l}{c}{r}}
	    {
	    \payoffpairs{1}{012}{421}
	    \payoffpairs{2}{301}{132}
	    } */
		
		String s ="\\bimatrixgame{4mm}";
		s = s + "{" + numRows + "}{" +numCols + "}{I}{II}%\n";
		
		//row labels & column labels
		s = s + this.generateRowLabels(numRows);
		s = s + this.generateColumnLabels(numCols);
		
		//payoff pairs
		s = s + this.generatePayoffPairs();

		return s;
	}
	
	public String getLaTeXString()
	{
		String s = "\\documentclass{article}\n\\input{gamesty}%\n\\begin{document} \n";
		s = s + this.mapTokensToBMTemplate();
		s =s + "\\end{document} \n";
		return s;
	}
	 
	public void createLaTeXFile()
	{
		try 
		{
		    BufferedWriter out = new BufferedWriter(new FileWriter(filename+".tex"));
		    out.write(""+ this.getLaTeXString());
		    out.close();
		} 
		catch (IOException e) 
		{
			//throw exception if can't print file
		}
	}
	
	public String generateRowLabels(int numRows) 
	{
		 String label;
		 char chr;
		 switch (numRows)
		 {
	         case 1:  label = "{{M}}%\n";		break;
	         case 2:  label = "{{T}{B}}%\n";	break;
	         case 3:  label = "{{T}{M}{B}}%\n";	break;
	         default: 
	         {
	        	 label = "{";
	        	 chr = 97; //97 = ascii 'a'
	        	 for (int i = 97; i < numRows+97; i++)
	        	 {
	        		 chr = (char) i;
	        		 label = label + "{" + chr + "}";
	        	 }
	        	 label = label + "}%\n";
	         }
		 }
	     return label;
	}
	
	public String generateColumnLabels(int numCols) 
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
	
	public String generatePayoffPairs() 
	{
		//a1 and a2 are the ArrayLists containing the payoff matrices
		/* {
	    \payoffpairs{1}{012}{421}
	    \payoffpairs{2}{301}{132}
	    } */
		String s="{\n";
		String r="";
		String strPayoff = "\\payoffpairs{";
		int numRows = a1.size();
		int numCols = a1.get(0).size(); 
		
		if (bSinglePayoff) {strPayoff = "\\singlepayoffs{"; }
		
		for(int i = 0; i < numRows; i++ )
		{
			r = strPayoff + (i+1) + "}{";
			
			for(int j = 0; j < numCols; j++) //player I payoffs
			{
				r = r+ "{$" + a1.get(i).get(j) + "$}";
			}
			r = r + "}" ;
			
			if (!bSinglePayoff)
			{
				r = r + "{";
				
				for(int j = 0; j < numCols; j++) //player II payoffs
				{
					r = r+ "{$" + a2.get(i).get(j) + "$}";
				}
				r = r + "}";
			}
			s = s + r + "\n";
		}
		
		s = s + "}\n";
		return s;
	}

	public ArrayList<ArrayList<String>> arrayToArrayList2D(String[][] s)
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
