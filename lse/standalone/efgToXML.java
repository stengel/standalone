/* Last updated June 30, 2011 */

/* Comment information regarding .efg file format sourced from 
 * http://www.gambit-project.org/doc/formats.html#file-formats
 */
package lse.standalone;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.EmptyStackException;

public class efgToXML 
{
	private Document xmlDoc;
	private String filename;
	private String[] fileLines;
	private ArrayList<String> playerNames;
	private Stack<Element> prevNodeStack;
	private Stack<nodeProp> nodePropStack;
	private HashMap<String, String> isetMap;
	private int lastIsetNum;
	
	public efgToXML(String fn)
	{
		this.filename = fn;
	}
	
	//create DOM document for XML
	private void createXMLDocument(String rootName) throws ParserConfigurationException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        
        Element root = doc.createElement(rootName);
        doc.appendChild(root);
        
        this.xmlDoc = doc;
        
        this.prevNodeStack = new Stack<Element>();
        this.prevNodeStack.push(root);
        
        this.nodePropStack = new Stack<nodeProp>();
        
    	isetMap = new HashMap<String, String>();
    	this.lastIsetNum = 0;
	}
	
	private void readEFGFile()
	{
		String fileLine;
		ArrayList<String> fl = new ArrayList<String>();
		
		try
        {
			BufferedReader reader = new BufferedReader(new FileReader(this.filename));	
			
			//read all the lines from the file
			while ((fileLine = reader.readLine()) != null) 
			{
				fl.add(fileLine);
		    }
		    reader.close();
		    
			String[] a = {"A"}; 
		    fileLines = fl.toArray(a);
        }
		catch (Exception e)
		{
			//better handling?
			System.out.println("Exception: " + e);
		}
	}
	
	private void parseEFGFile()
	{
		for (int i = 0; i < fileLines.length; i++) 
		{
			this.parseEFGLine(fileLines[i], i);
		}
		
		//cleanup expectedChildren node attributes, as they are not part of final xml
		while (!prevNodeStack.empty())
		{
			prevNodeStack.pop().removeAttribute("expectedChildren");
		}
	}
	
	private void parseEFGLine(String line, int lineNumber)
	{
		//Parse Header
		
		//determine node type and call appropriate parsing logic
		String[] tokens = line.split("\\s+");
		
		if (tokens[0].equals("c")) { parseEFGChanceNode(line); }
		else if (tokens[0].equals("p")) { parseEFGPlayerNode(line); }
		else if (tokens[0].equals("t")) { parseEFGTerminalNode(line); }
		else if (tokens[0].equals("EFG")) { this.playerNames = parseEFGHeader(line); }
		else { /* not header, not c p or t, ignore */ }
		
	}
	
	//Sample header:
	//EFG 2 R "General Bayes game, one stage" { "Player 1" "Player 2" }
	private ArrayList<String> parseEFGHeader(String line)
	{
		String gameName;
		ArrayList<String> playerNames;
		
		String[] tokens = line.split("\"");
	
		//game name - where to use this in gte, or xml file?
		gameName = tokens[1]; 
		
		playerNames = new ArrayList<String>();
		
		for (int k = 3; k < tokens.length; k = k+2)
		{
			if (tokens[k].trim().length() == 0) //skip 'empty' tokens
			{ 
				playerNames.add("" + (k-3)); 
			}
			else 
			{ 
				if (!tokens[k].equals("}")) { playerNames.add(tokens[k]); }
			}
		}
		
		return playerNames;
	}
	
	/* Entries for chance nodes begin with the character c. Following this is:
	-- a text string, giving the name of the node
	-- a positive integer specifying the information set number
	-- (optional) the name of the information set
	-- (optional) a list of actions at the information set with their corresponding probabilities
	-- a nonnegative integer specifying the outcome
	-- (optional)the payoffs to each player for the outcome
	
	Example possibilities: 
		c "nodename" 1 "" { "A" 1/2 "D" 1/2 } 0
		c "nodename" 1 "" { "A" 1/2 "D" 1/2 } 0 {10,0}
	*/
	private void parseEFGChanceNode(String line)
	{
		String nodeName, isetName, iset, payoff, outcome;
		ArrayList<nodeProp> actionList;
		String[] payoffs;
		
		String[] tokens1 = line.split("\\{|\\}");  //split the action list from the rest of the line
		String[] tokens = tokens1[0].split("\""); //split the rest of the line by quotation mark

		//parse node name - string
		nodeName = this.removeQuoteMarks(tokens[1]);
		
		//parse info set number - integer
		iset = this.getIsetNumber("chance"+tokens[2].trim());
		
		//parse info set name - string - optional
		isetName = tokens[3].trim();
		
		//outcome
		outcome = tokens1[2].trim();
		
		//parse action/probability list
		actionList = this.parseActionList(tokens1[1], "chance");
		
		//payoffs
		if (tokens1.length > 3) //payoffs are included for the node
		{
			payoffs = tokens1[3].split(",|\\s+");  //payoffs can be comma or space delimited
	
			for (int i = 0; i < payoffs.length; i++)
			{
				payoff = payoffs[i];
	
				if (!(payoff.trim().length() == 0)) //if not empty string
				{
					//do payoff work here as appropriate
				}
			}
		}
		
		this.attachNonTerminalXMLNode(nodeName, null, null, actionList);
	}

	/* Format of personal (player) nodes. Entries for personal player decision 
	 * nodes begin with the character p . Following this, in order, are:
	-- a text string, giving the name of the node
	-- a positive integer specifying the player who owns the node
	-- a positive integer specifying the information set
	-- (optional) the name of the information set
	-- (optional) a list of action names for the information set
	-- a nonnegative integer specifying the outcome
	-- (optional) the payoffs to each player for the outcome
	Example:
		p "nodename" 2 4 "isetName" { "Action1" "Action2" } 0
				player infoset							  outcome
				
		p "" 1 2 "(1,3)" { "H" "L" } 1 "Outcome 1" { 1/2, 1/2 }
	 */
	//For now, assume all elements are there, even if blank (excepting payoffs which are considered optional)
	private void parseEFGPlayerNode(String line)
	{
		String nodeName, player, isetName, iset, isetParsed, payoff, outcome;
		ArrayList<nodeProp> actionList;
		String[] payoffs;
		
		String[] tokens1 = line.split("\\{|\\}");  //split the action list from the rest of the line
		String[] tokens = tokens1[0].split("\""); //split the rest of the line by quotation mark

		//parse node name - string
		nodeName = this.removeQuoteMarks(tokens[1]);
		
		//parse player/owner - integer
		tokens[2] = tokens[2].trim();
		String[] playerAndInfoset = tokens[2].split("\\s+");
		player = playerAndInfoset[0].trim();
		isetParsed = playerAndInfoset[1].trim();
		
		try 
		{
			int index = Integer.parseInt(player) - 1;
			player = this.playerNames.get(index);
		}
		catch (Exception e)
		{
			player = playerAndInfoset[0].trim();
		}
		
		//parse info set number - integer
		iset = this.getIsetNumber(player + isetParsed);
		
		//parse info set name - string
		isetName = tokens[3];
		
		//parse outcome number - integer
		outcome = tokens1[2].trim();
		
		//parse action list information
		actionList = this.parseActionList(tokens1[1], "player");
		

		//parse payoffs and have outline of logic ready for when gte can accept payoffs at interior nodes
		//and in the meantime provide an error saying that the efg file cannot be 
		//entirely represented in gte?
		if (tokens1.length > 3) //payoffs are included for the node
		{
			payoffs = tokens1[3].split(",|\\s+");  //payoffs can be comma or space delimited
	
			for (int i = 0; i < payoffs.length; i++)
			{
				payoff = payoffs[i];
	
				if (!(payoff.trim().length() == 0)) //if not empty string
				{
					//do payoff work here as appropriate
				}
			}
		}
       
		this.attachNonTerminalXMLNode(nodeName, player, iset, actionList);
	}
	
	/* Format of terminal nodes. Entries for terminal nodes begin with the character t . 

	-- a text string, giving the name of the node
	-- a nonnegative integer specifying the outcome
	-- a string specifying the name of the outcome
	-- the payoffs to each player for the outcome (optional); can have comma or space separation
	
	Example:
		t "s4" 4 "?" { 10, 1 }
		t "" 0
	*/
	private void parseEFGTerminalNode(String line)
	{
       String payoff, nodename, outcomeValue;
       String[] payoffs;
       int playerNum = 0;
       
       String[] tokens1 = line.split("\\{|\\}");  //split the action list from the rest of the line
		
       String[] tokens = tokens1[0].split("\""); //split the rest of the line by quotation mark
		
       //remove t & space
       line = line.substring(2);
		
       //handle node name
       nodename = tokens[1].trim();
		
       //outcome integer
       outcomeValue = tokens[2].trim();
		
       //outcome name (optional)
       //--skip for now 
	
       String move;
       nodeProp m;
       
       try
       {
    	   m = this.nodePropStack.pop();
    	   move = m.move;
       }
       catch(EmptyStackException e)
       {
    	   move = null;
       }
       
       Element prevNode = this.calculatePreviousNode();
       int openBracketLoc = line.indexOf("{");
		
       if (openBracketLoc >= 0) //a payoff exists
       {
    	   Element child = this.createXMLOutcomeNode(move, null, null);
    	   prevNode.appendChild(child);
			
    	   Element outcome = child;
    	   int closeBracketLoc = line.indexOf("}");
    	   String p = line.substring(openBracketLoc+1, closeBracketLoc - 1).trim();

    	   payoffs = p.split(",|\\s+");  //payoffs can be comma or space delimited

    	   for (int i = 0; i < payoffs.length; i++)
    	   {
    		   payoff = payoffs[i].trim();

    		   if (!(payoff.length() == 0))
    		   {
    			   child = this.createXMLPayoffNode(this.playerNames.get(playerNum), payoff);
    			   outcome.appendChild(child);
    			   playerNum++;
    		   }
    	   }
       }
       else //no payoff, append a standard node
       {
    	   Element child = this.createXMLNodeNode(nodename, prevNode.getAttribute("player"), move, null, null, null);
    	   prevNode.appendChild(child);
		}
	}
	
	private Element createXMLPayoffNode(String player, String value)
	{
		//if the parent of this node is not an outcome node, add an outcome node
		Element child = this.xmlDoc.createElement("payoff");
        child.setAttribute("player", player);
        child.setAttribute("value", value);
        
        return child;
	}
	
	private Element createXMLOutcomeNode(String move, String prob, String iset)
	{
		Element child = this.xmlDoc.createElement("outcome");
		if (move != null) { child.setAttribute("move", move); }
        if (prob != null) { child.setAttribute("prob", prob); }
        if (iset != null) { child.setAttribute("iset", iset); }
        
        return child;
	}
	
	private Element createXMLNodeNode(String nodename, String player, String move, String prob, String iset, String expectedChildren)
	{
		Element child = this.xmlDoc.createElement("node");
		if (nodename != null) { child.setAttribute("nodename", nodename); }
		if (player != null) { child.setAttribute("player", player); }
		if (move != null) { child.setAttribute("move", move); }
        if (prob != null) { child.setAttribute("prob", prob); }
        if (iset != null) { child.setAttribute("iset", iset); }
        if (expectedChildren != null) {child.setAttribute("expectedChildren", expectedChildren) ; }
        
        return child;
	}
	
	private void attachNonTerminalXMLNode(String nodename, String player, String iset, ArrayList<nodeProp> actionList)
	{
		nodeProp m;
		String move, prob; 
	       
		try
		{
			m = this.nodePropStack.pop();
			move = m.move;
			prob = m.prob;
		}
		catch(EmptyStackException e)
		{
			move = null;
			prob = null;
		}

		Element child = this.createXMLNodeNode(nodename, player, move, prob, iset, ""+ actionList.size());

		Element prevNode = this.calculatePreviousNode();
	       
		prevNode.appendChild(child);
		prevNodeStack.push(child);

		this.pushReversedList(actionList, this.nodePropStack);
	}
	
	private Element calculatePreviousNode()
	{
		Element prevNode = (Element) this.prevNodeStack.peek();
		
		while (true)
        {
			//true root/wrapper element of structure
        	if (prevNode.getTagName().equals("extensiveForm") ) { break; }
        	
	        int expectedChildren = Integer.parseInt(prevNode.getAttribute("expectedChildren"));
	        int currChildren = prevNode.getChildNodes().getLength();
	        
	        if (currChildren == expectedChildren)
	        {
	        	prevNode.removeAttribute("expectedChildren");
				prevNodeStack.pop(); 

		    	prevNode = prevNodeStack.peek();    
	        }
	        else
	        {
	        	break;
	        }
        }
		
		return prevNode;
	}
	
	public void convertEFGtoXML()
	{
		try 
		{
			//create DOM document for XML
			this.createXMLDocument("extensiveForm");

			//Assemble XML by reading file and parsing into appropriate elements
			this.readEFGFile();
			this.parseEFGFile();
			
			//Transform XML
			TransformerFactory factory = TransformerFactory.newInstance();
            Transformer trans = factory.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(this.xmlDoc);
            trans.transform(source, result);
            String xmlString = result.getWriter().toString();

            //print xml, each element on a separate line
            //System.out.println(xmlString); 
            String outFile = this.filename.substring(0, filename.length() - 4) + ".xml";
            this.createXMLFile(outFile, xmlString);
		}
		catch(Exception e)
		{
			System.out.println("exception is " + e.toString());
		}
	}
	
	//parse action/probability list
	private ArrayList<nodeProp> parseActionList(String tokenString, String nodeType)
	{
		ArrayList<nodeProp> actionList = new ArrayList<nodeProp>();
		String[] tokens;
		String prob = null;
		
		tokenString = tokenString.trim();
		tokens =  tokenString.split("\"");

		int maxSize = tokens.length;
		
		if (nodeType.equals("chance"))
		{
			maxSize = maxSize - 1;
		}
		else if (!nodeType.equals("player"))
		{
			System.out.println("error in parseActionList method call");
		}
		
		for (int k = 0; k < maxSize; k++)
		{
			if (!(tokens[k].trim().length() == 0))
			{
				if (nodeType.equals("chance")) { prob =  tokens[k+1].trim(); }

				nodeProp node = new nodeProp(nodeType, this.removeQuoteMarks(tokens[k]), prob);
				actionList.add(node);
				
				if (nodeType.equals("chance")) { k++; }
			}
		}
		
		return actionList;
	}
	
	private void pushReversedList(ArrayList<nodeProp> al, Stack<nodeProp> s)
	{
		int listSize = al.size();
		
		for ( int i = listSize - 1; i >= 0; i--)
		{
			s.push(al.get(i));
		}
	}
	
	public void createXMLFile(String name, String contents)
	{
		try 
		{
		    BufferedWriter out = new BufferedWriter(new FileWriter(name));
		    out.write(contents);
		    out.close();
		} 
		catch (IOException e) 
		{
			//throw exception if can't create file
		}
	}
	
	private String removeQuoteMarks(String q)
	{
		Pattern pat = Pattern.compile("\"");
		Matcher m = pat.matcher(q);

        return m.replaceAll("");
	}
	
	private String getIsetNumber(String isetKey)
	{
		//String isetKey = "chance"+tokens[2].trim();
		String iset = isetMap.get(isetKey);
		if (iset == null)
		{
			iset = ""+this.lastIsetNum++;
			isetMap.put(isetKey, iset);
		}
		
		return iset;
	}

	public static void main (String [] args)
	{	
		String fn = args[0];
		efgToXML etx = new efgToXML(fn);

		etx.convertEFGtoXML();
	}
}

class nodeProp
{
	public String type;
	public String move;
	public String prob;
	public String iset;
	
	public nodeProp(String t, String m, String p)
	{
		this.type = t;
		this.move = m;
		this.prob = p;
		//this.iset = i;
	}
}
