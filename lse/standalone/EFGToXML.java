/* author: K. Bletzer */
/* Last updated August 31, 2011 */

/* More about the .efg file format can be found at:
 * http://www.gambit-project.org/doc/formats.html#file-formats
 */
package lse.standalone;

import java.io.BufferedReader;
import java.io.FileReader;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.EmptyStackException;

/* Takes a file in the Gambit .efg format and converts the data to 
 * the version 0.1 gte XML format.
 * 
 * Limited error handling as .efg file is assumed to be a well formatted 
 * file.
 */
public class EFGToXML 
{
	private Document xmlDoc;
	private String filename;
	private String[] fileLines;
	private ArrayList<String> playerNames;
	private Stack<Element> prevNodeStack;
	private Stack<nodeProp> nodePropStack;
	private HashMap<String, String> isetMap;
	private HashMap<String, String> moveMap;
	private int lastIsetNum;
	private int lastMoveNum;
	private ConversionUtilities util;
	private Element root;
	
	private boolean testMode = false; 
	private String dtd; 
	private String version = "0.1";
	private String fileSuffix = ".xml";
	
	/* constructor */
	public EFGToXML(String fn)
	{
		this.filename = fn;
		this.util = new ConversionUtilities();
	}
	
	/*
	 * @param tm: true or false.  If test mode is on the dtd will be refernced
	 * in the output XML and the XML document factory will have setValidating = true
	 */
	public void setTestMode(boolean tm)
	{
		this.testMode = tm;
	}
	
	/* set the DTD name - only used if class is in test mode */
	public void setDTD(String d)
	{
		this.dtd = d;
	}
	
	/* Change the suffix appended to the file if desired.
	 * For example, instead of the default .tex, can append _test.tex 
	 * to the file name if required to differentiate files. 
	 */
	public void setFileSuffix(String suffix)
	{
		this.fileSuffix = suffix;
	}
	
	//create DOM document for XML
	private void createXMLDocument(String rootName) throws ParserConfigurationException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        if(this.testMode)
        { 
        	factory.setValidating(true); 
        	factory.setNamespaceAware(true);
        } 
        this.xmlDoc  = builder.newDocument();
        
        this.root = this.xmlDoc.createElement(rootName);
        this.xmlDoc.appendChild(this.root);
        this.root.setAttribute("version", this.version);
        
		//add game Description Element - it will be updated with value later
		Element descr = this.xmlDoc.createElement("gameDescription");
    	this.root.appendChild(descr);
    	
		//add players Element - it will be updated with player names later
		Element players = this.xmlDoc.createElement("players");
    	this.root.appendChild(players);
    	   
		//add extensiveForm node
        Element extForm = this.xmlDoc.createElement("extensiveForm");
        this.root.appendChild(extForm);
        
        //this stack keeps track of the nodes in order to place the internal
        //nodes of the tree source from the .efg file under the correct parent
        //
        this.prevNodeStack = new Stack<Element>();
        this.prevNodeStack.push(extForm);
        
        this.nodePropStack = new Stack<nodeProp>();
        
    	isetMap = new HashMap<String, String>();
    	this.lastIsetNum = 1;
    	
    	moveMap = new HashMap<String, String>();
    	this.lastMoveNum = 1;
	}
	
	/* read the contents of the input file and place each line in an ArrayList */
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
			System.out.println("EFGToXML Exception: " + e);
		}
	}
	
	/* walk through each line in the file and call a function to begin the processing */
	private void parseEFGFile()
	{
		for (int i = 0; i < fileLines.length; i++) 
		{
			this.parseEFGLine(fileLines[i]);
		}
		
		//cleanup expectedChildren node attributes, as they are not part of final xml
		while (!prevNodeStack.empty())
		{
			prevNodeStack.pop().removeAttribute("expectedChildren");
		}
	}
	
	/* take a given line from the file and process it according to the line type */
	private void parseEFGLine(String line)
	{
		//determine node type and call appropriate parsing logic
		String[] tokens = line.split("\\s+");
		
		if (tokens[0].equals("c")) { parseEFGChanceNode(line); }
		else if (tokens[0].equals("p")) { parseEFGPlayerNode(line); }
		else if (tokens[0].equals("t")) { parseEFGTerminalNode(line); }
		else if (tokens[0].equals("EFG")) { parseEFGHeader(line); }
		else { /* not header, not c p or t, ignore */ }
	}
	
	//parse the efg file header line; 
	//Sample header line:
	//EFG 2 R "General Bayes game, one stage" { "Player 1" "Player 2" }
	private void parseEFGHeader(String line)
	{
		String gameDescr;
		ArrayList<String> playerNames;
		
		String[] tokens = line.split("\"");
	
		//game Description
		gameDescr = tokens[1]; 
		if (gameDescr.trim().length() > 0)
		{
			NodeList gd = this.root.getElementsByTagName("gameDescription");
			gd.item(0).setTextContent(gameDescr);
		}
		
		playerNames = new ArrayList<String>();
		int playerPlaceholder = 1;
		
		for (int k = 3; k < tokens.length; k = k+2)
		{
			if (tokens[k].trim().length() == 0)
			{ //player names are required for gte, prefix with "_" to decrease chance of name collision
				playerNames.add("_"+playerPlaceholder++); 
			}
			else 
			{ 
				if (!tokens[k].equals("}")) { playerNames.add(tokens[k]); }
			}
		}
		
		this.playerNames = playerNames;
		
		Node players = this.root.getElementsByTagName("players").item(0);
    	util.updatePlayersNode(this.playerNames, this.xmlDoc, players);
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
		String nodeName, isetName, iset, outcome, outcomeName;
		ArrayList<nodeProp> actionList;
		String[] payoffs;
		
		String[] tokens1 = line.split("\\{|\\}");  //split the action list from the rest of the line
		String[] tokens = tokens1[0].split("\""); //split the rest of the line by quotation mark
		String[] tokens0 = tokens1[2].split("\"");

		//parse node name - string
		nodeName = util.removeQuoteMarks(tokens[1]);
		
		//parse info set number - integer
		String isetKey = "chance"+tokens[2].trim();
		iset = this.getIsetNumber(isetKey);
		
		//parse info set name - string - optional
		isetName = tokens[3].trim();
		
		//outcome
		outcome = tokens0[0].trim();
		
		//parse action/probability list
		actionList = this.parseActionList(tokens1[1], "chance", isetKey);
		
		//parse outcome name - need to add this here
		outcomeName = null; 
		
		//payoffs
		ArrayList<String> payoffsList = null;
		if (tokens1.length > 3) //payoffs are included for the node
		{
			outcomeName = tokens0[1]; 
			if (outcomeName.length() == 0) { outcomeName = null; }
			
			payoffs = tokens1[3].split(",|\\s+");  //payoffs can be comma or space delimited
			payoffsList = this.parsePayoffList(payoffs);
		}
		
		this.attachNonTerminalXMLNode(nodeName, null, iset, isetName, actionList, outcome, outcomeName, payoffsList);
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
	Examples:
		p "nodename" 2 4 "isetName" { "Action1" "Action2" } 0
		p "" 1 2 "(1,3)" { "H" "L" } 1 "Outcome 1" { 1/2, 1/2 }
	 */
	private void parseEFGPlayerNode(String line)
	{
		String nodeName, player, isetName, iset, isetParsed, outcome;
		String outcomeName = null;
		ArrayList<nodeProp> actionList;
		String[] payoffs;
		
		String[] tokens1 = line.split("\\{|\\}");  //split the action list from the rest of the line
		String[] tokens = tokens1[0].split("\""); //split the rest of the line by quotation mark
		String[] tokens0 = tokens1[2].split("\"");
		
		//parse node name - string
		nodeName = util.removeQuoteMarks(tokens[1]);
		
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
		String isetKey = player + isetParsed;
		iset = this.getIsetNumber(isetKey);
		
		//parse info set name - string
		isetName = tokens[3].trim();
		if (isetName.length() == 0) { isetName = null; }
		
		//parse outcome number - integer
		outcome = tokens0[0].trim(); 
		
		//parse action list information
		actionList = this.parseActionList(tokens1[1], "player", isetKey);

		ArrayList<String> payoffsList = null;

		if (tokens1.length > 3) //payoffs are included for the node
		{
			//parse outcome name - need to add this here
			outcomeName = tokens0[1]; 
			if (outcomeName.length() == 0) { outcomeName = null; }
			
			payoffs = tokens1[3].split(",|\\s+");  //payoffs can be comma or space delimited
			payoffsList = this.parsePayoffList(payoffs);
		}

		this.attachNonTerminalXMLNode(nodeName, player, iset, isetName, actionList, outcome, outcomeName, payoffsList);
	}
	
	/* Format of terminal nodes. Entries for terminal nodes begin with the character t . 

	-- a text string, giving the name of the node
	-- a nonnegative integer specifying the outcome
	-- a string specifying the name of the outcome
	-- the payoffs to each player for the outcome (optional); can have comma or space separation
	
	Examples:
		t "s4" 4 "?" { 10, 1 }
		t "" 0
	*/
	private void parseEFGTerminalNode(String line)
	{
       String payoff, nodename, outcomeValue, outcomeName;
       String[] payoffs;
       int playerNum = 0;
       
       String[] tokens1 = line.split("\\{|\\}");  //split the payoff list from the rest of the line
		
       String[] tokens = tokens1[0].split("\""); //split the rest of the line by quotation mark
		
       //remove t & space
       line = line.substring(2);
		
       //handle node name
       nodename = tokens[1].trim();

       //outcome integer
       outcomeValue = tokens[2].trim();
		
       //outcome name (optional)
       outcomeName = null;
	
       String move, prob;
       nodeProp m;
       
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
       
       Element prevNode = this.calculatePreviousNode();
       int openBracketLoc = line.indexOf("{");
		
       if (openBracketLoc >= 0) //a payoff exists
       {
    	   outcomeName = tokens[3];
    	   if (outcomeName.length() == 0) { outcomeName = null; }
    	   
    	   Element child = this.createXMLOutcomeNode(move, prob, nodename, outcomeValue, outcomeName);
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
    	   String player = prevNode.getAttribute("player");
    	   if (player.trim().length() == 0) { player = null; }
    	   
    	   Element child = this.createXMLOutcomeNode(move, prob, nodename, outcomeValue, null);
    	   prevNode.appendChild(child);
		} 
	}
	
	/* Create an XML payoff node which may be attached to a node anywhere */
	private Element createXMLPayoffNode(String player, String value)
	{
		//if the parent of this node is not an outcome node, add an outcome node
		Element child = this.xmlDoc.createElement("payoff");
        child.setAttribute("player", player);
        child.setTextContent(value);
        return child;
	}
	
	/* Create an XML Outcome node */
	private Element createXMLOutcomeNode(String move, String prob, String nodename, String outcome, String outcomeName)
	{
		Element child = this.xmlDoc.createElement("outcome");
		if (move != null) { child.setAttribute("move", move); }
        if (prob != null) { child.setAttribute("prob", prob); }
		if (nodename != null && nodename.trim().length() > 0) 
		{ 
			child.setAttribute("nodeName", nodename); 
		}
        if (outcome != null) 
        { 
        	if (!("0".equals(outcome))) { child.setAttribute("outcomeId", outcome); }
        }
        if (outcomeName != null) { child.setAttribute("outcomeName", outcomeName); }
        
        return child;
	}
	
	/* Create an xml "node" representing an internal node in the game tree */
	private Element createXMLNodeNode(String nodename, String player, String move, String prob, String iset, 
				String isetName, String outcome, String outcomeName, String expectedChildren)
	{
		Element child = this.xmlDoc.createElement("node");
		if (nodename != null && nodename.trim().length() > 0) 
		{ 
			child.setAttribute("nodeName", nodename); 
		}
		if (player != null) { child.setAttribute("player", player); }
		if (move != null) { child.setAttribute("move", move); }
        if (prob != null) { child.setAttribute("prob", prob); }
        if (iset != null) { child.setAttribute("iset", iset); }
        if (isetName != null) { child.setAttribute("isetName", isetName); }
        if (outcome != null) 
        { 
        	if (!("0".equals(outcome))) { child.setAttribute("outcomeId", outcome); }
        }
        if (outcomeName != null) { child.setAttribute("outcomeName", outcomeName); }
        if (expectedChildren != null) {child.setAttribute("expectedChildren", expectedChildren) ; }
        
        return child;
	}
	
	/* link a non-terminal node to the document, and handle the parent node stack
	 * in order to track the list of parent nodes that still need children to be 
	 * created and attached.
	 */
	private void attachNonTerminalXMLNode(String nodename, String player, String iset, String isetname, ArrayList<nodeProp> actionList, 
			String outcome, String outcomeName, ArrayList<String> payoffList)
	{
		nodeProp m;
		String move, prob; 
	       
		try
		{  //remember move and probability belong to parent node, whereas other attr belongs to self
			m = this.nodePropStack.pop();
			move = m.move;
			prob = m.prob;
		}
		catch(EmptyStackException e)
		{
			move = null;
			prob = null;
		}

		Element child = this.createXMLNodeNode(nodename, player, move, prob, iset, isetname, outcome, outcomeName, ""+ actionList.size());

		Element prevNode = this.calculatePreviousNode();
	       
		prevNode.appendChild(child);
		prevNodeStack.push(child);
		
		if (payoffList != null)
		{ 
		   int playerNum = 0;	 
		   for (int i = 0; i < payoffList.size(); i++)
	  	   {
	  		   String payoff = payoffList.get(i).trim();
	
	  		   if (!(payoff.length() == 0))
	  		   {
	  			   Element payoffNode = this.createXMLPayoffNode(this.playerNames.get(playerNum), payoff);
	  			   child.appendChild(payoffNode);
	  			   playerNum++;
	  		   }
	  	   }
		}

		this.pushReversedList(actionList, this.nodePropStack);
	}
	
	/* Based on current node calculate previous parent node in order to use for further
	 * decisions and/or processing in other methods.
	 */
	private Element calculatePreviousNode()
	{
		Element prevNode = (Element) this.prevNodeStack.peek();
		
		while (true)
        {
			//true root/wrapper element of structure
        	if (prevNode.getTagName().equals("extensiveForm") ) { break; }
        	
	        int expectedChildren = Integer.parseInt(prevNode.getAttribute("expectedChildren"));
	        
	        //find out the count of child Nodes that are not "payoff"
	        NodeList children = prevNode.getChildNodes();
	        int currChildren = 0;
	        for (int i = 0; i < children.getLength(); i++)
	        {
	        	if (!("payoff".equals(children.item(i).getNodeName())))
	        	{
	        		currChildren++;
	        	}
	        }
	        
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
	
	/* main class method that initiates conversion activities */
	public void convertEFGtoXML()
	{
		try 
		{
			//create DOM document for XML
			this.createXMLDocument("gte");

			//Assemble XML by reading file and parsing into appropriate elements
			this.readEFGFile();
			this.parseEFGFile();
			
			//Transform XML
			TransformerFactory factory = TransformerFactory.newInstance();
			factory.setAttribute("indent-number", new Integer(2));
            Transformer trans = factory.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            if(testMode)
            {    
            	trans.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, this.dtd);
            }
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(this.xmlDoc);
            trans.transform(source, result);
            String xmlString = result.getWriter().toString();
            xmlString = util.orderXMLAttributes(xmlString);

            String outFile = this.filename.substring(0, filename.length() - 4) + this.fileSuffix;
            util.createFile(outFile, xmlString);
		}
		catch(Exception e)
		{
			System.out.println("exception is " + e.toString());
		}
	}
	
	/* Transform payoffs of the form String array to ArrayList */
	private ArrayList<String> parsePayoffList(String[] payoffs)
	{
		String payoff;
		ArrayList<String> payoffList = new ArrayList<String>();
		
		for (int i = 0; i < payoffs.length; i++)
		{
			payoff = payoffs[i];

			if (!(payoff.trim().length() == 0)) //if not empty string
			{
				payoffList.add(payoff);
			}
		}
		return payoffList;
	}
	
	//parse action/probability list
	//tokenString is the yet to be parsed string
	//gte does not accept blank move names...need to make sure move attr is populated
	private ArrayList<nodeProp> parseActionList(String tokenString, String nodeType, String isetKey)
	{
		ArrayList<nodeProp> actionList = new ArrayList<nodeProp>();
		ArrayList<String> tokenList = new ArrayList<String>();
		String[] tokens;
		String prob = null;
		
		tokenString = tokenString.trim();
		tokenList = util.extractTokens(tokenString);
		
		String[] a = {"A"};
		tokens =  tokenList.toArray(a);
		
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
			if (tokens[k].trim().length() == 0) //need to create a move identifier
			{   //add prefix to decrease chance of collision with existing move id
				tokens[k] = "_" + this.getMoveNumber(""+k+isetKey); 
			}
			if (nodeType.equals("chance")) { prob =  tokens[k+1].trim(); }

			nodeProp node = new nodeProp(nodeType, util.removeQuoteMarks(tokens[k]), prob);
			actionList.add(node);

			if (nodeType.equals("chance")) { k++; }
		}
		
		return actionList;
	}
	
	/* when adding moves to the list of nodes to be processed, need to add in reverse order */
	private void pushReversedList(ArrayList<nodeProp> al, Stack<nodeProp> s)
	{
		int listSize = al.size();
		
		for ( int i = listSize - 1; i >= 0; i--)
		{
			s.push(al.get(i));
		}
	}
	
	/* generate a unique iset number if required, otherwise find number used
	 * by an existing node in the same iset
	 */
	private String getIsetNumber(String isetKey)
	{
		String iset = isetMap.get(isetKey);
		if (iset == null)
		{
			iset = ""+this.lastIsetNum++;
			isetMap.put(isetKey, iset);
		}
		
		return iset;
	}
	
	/* generate a unique move number if move number from file is blank
	 */
	private String getMoveNumber(String isetKey)
	{
		String move = moveMap.get(isetKey);
		
		if (move == null)
		{
			move = ""+this.lastMoveNum++;
			moveMap.put(isetKey, move);
		}
		
		return move;
	}

	public static void main (String [] args)
	{	
		String fn = args[0];
		EFGToXML etx = new EFGToXML(fn);
		
		if (args.length > 1)
		{
			String ext = args[1];
			etx.setFileSuffix(ext);
		}

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
	}
}
