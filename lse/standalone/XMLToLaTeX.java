/* author: K. Bletzer */
/* last updated August 18, 2011 */
package lse.standalone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/* Only two players supported, since LaTeX macro only supports two players */
/* Note: game description will not be used as not supported by LaTeX macro */
public class XMLToLaTeX 
{
	//private String gameDescription="\"\"";
	private ConversionUtilities util;
	private ArrayList<String> playerNames;
	private HashMap<String, ArrayList<String>> playerStrategies;
	private ArrayList<String> numPlayerStrategies;
	private HashMap<String, String> playerPayoffs;
	private String filename;
	
	private ArrayList<ArrayList<String>> a1;
	private ArrayList<ArrayList<String>> a2;
	
	private boolean[][] br1;  //best response corresponding to a1
	private boolean[][] br2;  //best response corresponding to a2
	
	private int numRows;    //number of rows in payoff matrices
	private int numCols;	//number of columns in payoff matrices
	
	private boolean bSinglePayoff = false; //will be updated based on input matrices
	private boolean bPrettyFraction = true;  //apply additional formatting to fractions - yes or no
	private boolean bShowBestResponse = true;  //show a box around the best response
	
	//default settings for bimatrix LaTeX macro
	private String rowColor = "Red";
	private String colColor = "Blue";
	private String cellSize = "4mm";
	private String diagSize = "0";
	private String pairFont = "\\small";
	private String singleFont = "\\normalsize";
	private String fileSuffix = ".tex";

	
	/* constructor */
	public XMLToLaTeX(String fn)
	{
		this.util = new ConversionUtilities();
		this.playerNames = new ArrayList<String>();
		this.playerStrategies = new HashMap<String, ArrayList<String>>();
		this.numPlayerStrategies = new ArrayList<String>();
		this.playerPayoffs = new HashMap<String, String>();
		this.filename = fn;
	}
	
	/* Change the suffix appended to the file if desired.
	 * For example, instead of the default .tex, can append _test.tex 
	 * to the file name if required to differentiate files. 
	 */
	public void setFileSuffix(String suffix)
	{
		this.fileSuffix = suffix;
	}
	
	/* Main method to kick off conversion from XML to LaTeX */
	public void convertXMLToLaTeX()
	{
		try 
		{
			this.readXML(this.filename);
			this.createLaTeXFile(this.filename);
		}
		catch(Exception e)
		{
			System.out.println("XMLToLaTeX exception: " + e.toString());
		}
	}
	
	/* Read the main XML elements and call appropriate private functions to handle
	 * reading child elements. 
	 */
	private void readXML(String filename)
	{
		Document xml = util.fileToXML(filename);
		
		Node root = xml.getDocumentElement();
		if ("gte".equals(root.getNodeName())) 
		{
			for (Node child = root.getFirstChild(); child != null; child =  child.getNextSibling()) 
			{
				if ("gameDescription".equals(child.getNodeName()))
				{
					//this.gameDescription = "\"" + child.getTextContent() + "\"";
				}
				if ("players".equals(child.getNodeName()))
				{
					this.playerNames = util.readPlayersXML(child);
				}
				if ("strategicForm".equals(child.getNodeName())) 
				{	
					this.readStrategicForm(child);
					this.numRows = Integer.parseInt(this.numPlayerStrategies.get(0));
					this.numCols = Integer.parseInt(this.numPlayerStrategies.get(1));
				}
				if ("display".equals(child.getNodeName()))
				{
					this.readDisplayXML(child);
				}
			}
		}
		else 
		{
			System.out.println("XMLToLaTeX error: first XML element not recognized.");
		}
	}
	
	/* read and process the XML data from the strategicForm XML node */
	private void readStrategicForm(Node stratForm)
	{
		String gameSize = util.getAttribute(stratForm, "size");
		gameSize = gameSize.replace("}", "");
		gameSize = gameSize.replace("{", "").trim();
		
		String[] strategies = gameSize.split("\\s+");
		
		for(int i = 0; i < strategies.length; i++)
		{
			this.numPlayerStrategies.add(strategies[i].trim());
		}
		
		for (Node child = stratForm.getFirstChild(); child != null; child =  child.getNextSibling()) 
		{
			if ("strategy".equals(child.getNodeName())) 
			{
				processStrategy((Element)child);
			} 
			else if ("payoffs".equals(child.getNodeName())) 
			{
				processPayoff((Element)child);
			} 
		} 
	}
	
	/* Read and process the XML data from the strategicForm XML node. 
	 * This will be used for the LaTeX macro formatting options available.
	 */
	private void readDisplayXML(Node display)
	{	
		for (Node child = display.getFirstChild(); child != null; child =  child.getNextSibling()) 
		{
			if ("rowColor".equals(child.getNodeName())) 
			{
				this.rowColor = child.getTextContent();
			} 
			else if ("colColor".equals(child.getNodeName())) 
			{
				this.colColor = child.getTextContent();
			} 
			else if ("cellSize".equals(child.getNodeName())) 
			{
				this.cellSize = child.getTextContent();
			}
			else if ("diagSize".equals(child.getNodeName())) 
			{
				this.diagSize = child.getTextContent();
			} 
			else if ("pairFont".equals(child.getNodeName())) 
			{
				this.pairFont = child.getTextContent();
			} 
			else if ("singleFont".equals(child.getNodeName())) 
			{
				this.singleFont = child.getTextContent();
			}
			else if ("prettyFraction".equals(child.getNodeName())) 
			{
				if("true".equals(child.getTextContent())) { this.bPrettyFraction = true; }
				else { this.bPrettyFraction = false; }
			} 
			else if ("bestResponse".equals(child.getNodeName())) 
			{
				if("true".equals(child.getTextContent())) { this.bShowBestResponse = true; }
				else { this.bShowBestResponse = false; }
			} 
			else if ("singlePayoff".equals(child.getNodeName())) 
			{
				if("true".equals(child.getTextContent())) { this.bSinglePayoff = true; }
				else { this.bSinglePayoff = false; }
			}
		} 
	}
	
	/* Process the data from a payoff node */
	private void processPayoff(Node node)
	{
		NodeList nl = node.getChildNodes();
		String value = nl.item(0).getNodeValue();
		String playerName = util.getAttribute(node, "player");
		
		//this call is superfluous if <players> present, but keeping to ensure players populated
		this.addPlayerName(playerName);  
		
		this.playerPayoffs.put(playerName, value.trim()); 
	}
	
	/* Parse the data from a strategy node */
	private void processStrategy(Node node)
	{
		NodeList nl = node.getChildNodes();
		String value = nl.item(0).getNodeValue();
		String playerName = util.getAttribute(node, "player");
		
		this.addPlayerName(playerName);

		value = value.replace("}", "");
		value = value.replace("{", "");
		
		value = value.trim();
		ArrayList<String> strategies = util.extractTokens(value);
		
		this.playerStrategies.put(playerName, strategies);
	}
	
	/* returns an ArrayList containing the player names in order */
	private ArrayList<String> getPlayerNames()
	{
		return this.playerNames;
	}
	
	/* returns a set of payoffs for a particular player, given the playerName */
	private String getPlayerPayoffsByName(String playerName)
	{
		return this.playerPayoffs.get(playerName);
	} 
	
	/* Add a player to the list, checking for duplicates before adding */
	private void addPlayerName(String name)
	{
		if (!this.playerNames.contains(name))
		{
			this.playerNames.add(name);
		}
	}
	
	/* returns a set of payoffs for a particular player, given the playerNumber */
	private ArrayList<String> getPlayerStrategiesByNumber(int playerNum)
	{
		return this.playerStrategies.get(this.playerNames.get(playerNum));
	}
	
	/* create the output file leveraging the utilities class */
	private void createLaTeXFile(String filename)
	{
        String outFile = this.filename.substring(0, filename.length() - 4) + this.fileSuffix;
        util.createFile(outFile, this.getLaTeXString()); 
	}
	
	/* transform the strategy list (String) to a nested ArrayList for further processing */
	private ArrayList<ArrayList<String>> payoffStringToNestedList(String p)
	{
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		
		String[] pp = p.split("\\s+");
		
		int col = Integer.parseInt(this.numPlayerStrategies.get(1));  //player 2 determines number of columns
		int row = Integer.parseInt(this.numPlayerStrategies.get(0));
		int totalPayoffs = col*row;

		for (int r = 0; r < totalPayoffs; r+=col)
		{
			ArrayList<String> rowList = new ArrayList<String>();
			for (int c = 0; c < col; c++)
			{
				rowList.add("" + pp[r+c]);
			}
			result.add(rowList);
		}
		
		return result;
	}
	
	//create String representing LateX document
	private String getLaTeXString()
	{	
		ArrayList<String> players = this.getPlayerNames();
		
		String pay0 = this.getPlayerPayoffsByName(players.get(0));
		String pay1 = this.getPlayerPayoffsByName(players.get(1));
		
		a1 = this.payoffStringToNestedList(pay0);
		a2 = this.payoffStringToNestedList(pay1);
		
		if (this.bShowBestResponse & !bSinglePayoff) { this.calculateBestResponse(); }
		else { this.bShowBestResponse = false; }
		
		String s = "\\documentclass{article}\n\\usepackage{bimatrixgame}\n\\usepackage[usenames]{color}\n\\begin{document} \n";
		s = s + this.mapTokensToBMTemplate();
		s =s + "\\end{document} \n";
		return s;
	}

	//map tokens found in the input file to the bimatrix macro template
	//Excpected output format is similar to:
	// \bimatrixgame{4mm}{2}{3}{I}{II}%
	//    {{T}{B}}%
	//    {{l}{c}{r}}
	//    {
	//    \payoffpairs{1}{012}{421}
	//    \payoffpairs{2}{301}{132}
	//    } 
	private String mapTokensToBMTemplate()
	{
		String s = "\n";
		//optional parameters here
		s += "\\renewcommand{\\bimatrixrowcolor}{" + this.rowColor + "}%\n";
		s += "\\renewcommand{\\bimatrixcolumncolor}{" + this.colColor +"}%\n";
		s += "\\renewcommand{\\bimatrixdiag}{" + this.diagSize +"}%\n";
		
		if (this.bSinglePayoff) { s += "\\renewcommand{\\bimatrixsinglefont}{" + this.singleFont +"}%\n";  }
		else { s += "\\renewcommand{\\bimatrixpairfont}{" + this.pairFont +"}%\n"; }
		
		s +="\n\\bimatrixgame{" + this.cellSize + "}";
		s += "{" + this.numRows + "}{" + this.numCols + "}{I}{II}%\n";
		
		//row labels & column labels
		if (this.playerStrategies.isEmpty())
		{
			s += this.generateRowLabels();
			s += this.generateColumnLabels();
		}
		else  //strategies are listed in the xml
		{
			s += this.formatStrategyLabels(this.getPlayerStrategiesByNumber(0));
			s += this.formatStrategyLabels(this.getPlayerStrategiesByNumber(1));
		}
		
		//payoff pairs
		s = s + this.generatePayoffPairs();

		return s;
	}
	
	/* format strategy labels if they are included in input XML (and not
	 * generated by default).
	 */
	private String formatStrategyLabels(ArrayList<String> list)
	{
		String label = "{";
		for (int i = 0; i <list.size(); i++)
		{
			 label = label + "{" + list.get(i) + "}";
		}
		return label + "}%\n";
	}
	
	/* Generate default row labels */
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
	
	/* Generate default column labels */
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
	
	/* Calculate the best response for the game and store true/false
	 * in an array to indicate if a particular payoff is (true) or is not
	 * (false) a best response to the other player's strategy. */
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
		
		//store best response (T or F) in array for use in formatting table entry later
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
	
	/* Generate and format the payoff pairs for the final file */
	private String generatePayoffPairs() 
	{
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
	
	/* Wrap an individual payoff with a box if it is a calculated best response 
	 * (and the "best response" option is true) and/or format the fraction */
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
		else  //reformat for nicer fractions
		{  
			Pattern pat = Pattern.compile("/");
			Pattern neg = Pattern.compile("-");
			Matcher m = pat.matcher(p);
			Matcher mneg = neg.matcher(p);
			
	        if (m.find()) //fractional input
	        { 
	        	p = mneg.replaceAll("");  //eliminate negative signs, will format at front 
	        		
	        	String[] result = pat.split(p);
	      
	        	String negSign = "";
	        	
	        	//determine if fraction negative or positive: 
	        	//		if find one neg sign, and no others, then negative
	        	if (mneg.find() & !mneg.find()) { negSign = "-"; }
	        	
	        	p = "{$" + negSign + "\\frac{"+result[0]+"}{"+result[1]+"}$}";
	        }
	        else {p = 	"{$" + p + "$}"; }  //not a fraction
		}
		return lBox + p + rBox;
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
	
	
	public static void main (String [] args)
	{	
		String fn = args[0];
		XMLToLaTeX xtl = new XMLToLaTeX(fn);

		xtl.convertXMLToLaTeX();
	}
}
