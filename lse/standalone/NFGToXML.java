/* author: K. Bletzer */
/* Last updated August 13, 2011 */
package lse.standalone;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/* Takes a file in the Gambit .nfg format and converts the data to 
 * the version 0.1 gte XML format.
 * 
 * Limited error handling as .nfg file is assumed to be a well formatted 
 * file.
 */
public class NFGToXML 
{
	private Document xmlDoc;
	private StringBuffer buffer;
	private Element root;
	private Element stratForm;
	private String filename;
	private int linePosition;
	private String gameDescription;
	private ArrayList<String> playerNames;
	private ArrayList<String> numPlayerStrategies;
	private ArrayList<ArrayList<String>> playerStrategies;
	private ArrayList<ArrayList<String>> playerPayoffs;
	private ConversionUtilities util;
	private String fileSuffix = ".xml";
	
	private boolean testMode = false;
	private String dtd;
	private String version = "0.1";
	
	/* constructor */
	public NFGToXML(String fn)
	{
		this.filename = fn;
		this.numPlayerStrategies = new ArrayList<String>();
		this.playerNames =  new ArrayList<String>();
		this.playerStrategies =  new ArrayList<ArrayList<String>>();
		this.playerPayoffs =  new ArrayList<ArrayList<String>>();
		this.buffer = new StringBuffer();
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
	 * For example, instead of the default .nfg, can append _test.nfg 
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
		factory.setValidating(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        if(this.testMode) 
        { 
        	factory.setValidating(true); 
        	factory.setNamespaceAware(true);
        } 
        this.xmlDoc = builder.newDocument();
        
        this.root = this.xmlDoc.createElement(rootName);
        this.xmlDoc.appendChild(this.root);
        this.root.setAttribute("version", this.version);
        
		Element descr = this.xmlDoc.createElement("gameDescription");
    	this.root.appendChild(descr);  //value will be added later
    	
		//add players Element - it will be updated with player names later
		Element players = this.xmlDoc.createElement("players");
    	this.root.appendChild(players);
    	   
        this.stratForm = this.xmlDoc.createElement("strategicForm");
        this.root.appendChild(this.stratForm);
	}
	
	/* read file line by line and add lines to the file buffer */
	private void readNFGFile()
	{
		String fileLine;

		try
        {
			BufferedReader reader = new BufferedReader(new FileReader(this.filename));	
			
			//read all the lines from the file and append to string buffer
			while ((fileLine = reader.readLine()) != null) 
			{
				this.buffer.append(fileLine+"\n");
		    }
		    reader.close();
        }
		catch (Exception e)
		{
			System.out.println("NFGToXML Exception: " + e);
		}
	}
	
	/* read game description and update internal member with value */
	private void processGameDescription()
	{
		int beginIndex, endIndex;
		
		beginIndex = this.buffer.indexOf("\"");
		
		if (beginIndex > 0) 
		{ 
			endIndex = this.buffer.indexOf("\"", beginIndex+1);
			
			this.linePosition = endIndex;
			this.gameDescription = this.buffer.substring(beginIndex+1, endIndex);
			
			NodeList gd = this.root.getElementsByTagName("gameDescription");
			gd.item(0).setTextContent(this.gameDescription);
		}
	}
	
	/* read player names from NFG header and add to the players list */
	private void processPlayerNames()
	{
		int beginIndex, endIndex;
		ConversionUtilities util = new ConversionUtilities();
		
		beginIndex = this.buffer.indexOf("{", this.linePosition);
		if (beginIndex > 0) 
		{ 
			endIndex = this.buffer.indexOf("}", beginIndex+1);
				
			this.playerNames = util.extractTokens(this.buffer.substring(beginIndex+1, endIndex).trim());
			this.linePosition = endIndex;
		}
		else
		{
			System.out.println("NFGToXML Error: problem in processing player names.");
		}
		
		//handle blank names; blank names not allowed in gte
		for (int i = 0; i < this.playerNames.size(); i++)
		{
			if (this.playerNames.get(i).trim().length() < 1)
			{
				this.playerNames.set(i, "_Player " + (i+1));
			}
		}
		
		Node players = this.root.getElementsByTagName("players").item(0);
    	util.updatePlayersNode(this.playerNames, this.xmlDoc, players);
	}
	
	/* read strategies from file and call for XML processing */
	private void processStrategies()
	{
		//check to see if there is a second { in the line
		//if not move one and find the next {
		//if there is a number after the {, then we have # of strategies
		//else if there is another { after (no number preceding) we have named strategies
		int beginIndex;
		
		beginIndex = this.buffer.indexOf("{", this.linePosition) + 1;
		
		if (beginIndex > 0) 
		{ 
			int nextStart = this.buffer.indexOf("{", beginIndex);
			int nextEnd = this.buffer.indexOf("}", beginIndex);
			
			if ((nextStart < nextEnd) && (nextStart >= 0))  
			{
				this.processStrategyList(beginIndex);
			}
			else  
			{
				this.processStrategyCounts(beginIndex);
			}
		}
		else
		{
			System.out.println("NFGToXML Error: problem in processing strategies.");
		}
		
		int numPlayers = this.playerStrategies.size();
		
		for (int i =0; i < numPlayers; i++)
		{
			String strat = util.arrayListToBracketList(this.playerStrategies.get(i));
			Element child = createXMLStrategyNode(this.playerNames.get(i), strat);
			this.stratForm.appendChild(child);
		}
	}
	
	/* process list of strategies if they exist */
	private void processStrategyList(int beginIndex)
	{
		//find location of }}
		int endIndex = 0;
		String strategyLine;
		
		Pattern pat = Pattern.compile("}\\s*}");
		Matcher m = pat.matcher(this.buffer.toString());
		
		if (m.find(beginIndex))
		{
			endIndex = m.end() -1;
			this.linePosition = endIndex +1;
		}
		else
		{
			System.out.println("NFGToXML Error: problem in processing strategy List.");
		}
		
		//strategy list is substring between beginIndex and endIndex
		strategyLine = this.buffer.substring(beginIndex, endIndex).trim();
		
		String[] strategies = strategyLine.split("\\}\\s*\\{");
		
		for(int i = 0; i < strategies.length; i++)
		{
			this.parseStrategyNames(strategies[i]);
		}
	}
	
	/* process information regarding strategy counts */
	private void processStrategyCounts(int beginIndex)
	{
		int endIndex = 0;
		String strategyLine;
		Pattern pat = Pattern.compile("}");
		Matcher m = pat.matcher(this.buffer.toString());
		
		if (m.find(beginIndex))
		{
			endIndex = m.end() -1;
			this.linePosition = endIndex +1;
		}
		else
		{
			System.out.println("NFGToXML Error: problem in processing strategy counts.");
		}
		
		//strategy list is substring between beginIndex and endIndex
		strategyLine = this.buffer.substring(beginIndex, endIndex).trim();
		
		String[] strategies = strategyLine.split("\\s+");
		
		for(int i = 0; i < strategies.length; i++)
		{
			this.numPlayerStrategies.add(strategies[i]);
		}
	}
	
	/* process payoff information - payoffs are either formatted as "outcomes"
	 * or "payoff" pairs based on nfg file formats.
	 */
	private void processPayoffs()
	{
		int endIndex=0;
		int beginIndex = this.buffer.indexOf("{", this.linePosition) + 1;
		
		if (beginIndex > 0) //outcomes structure 
		{ 
			this.processOutcomePayoffs(beginIndex, endIndex);
		}
		else //payoff pairs only
		{
			this.processPayoffPairs(beginIndex, endIndex);
		}
	}
	
	/* process payoffs of type outcome */
	private void processOutcomePayoffs(int beginIndex, int endIndex)
	{
		String payoffString;
		Pattern pat = Pattern.compile("}\\s*}");
		Matcher m = pat.matcher(this.buffer.toString());
		
		if (m.find(beginIndex))
		{
			this.linePosition = m.end();
			endIndex = m.start();
		}
		else
		{
			System.out.println("NFGToXML Error: problem in outcome payoffs.");
		}
		
		String outcomesString = this.buffer.substring(beginIndex+1, endIndex).trim();
		String[] outcomes = outcomesString.split("\\}\\s*\\{");
		
		//get outcomes mapping
		String outcomeMapping = this.buffer.substring(this.linePosition).trim();
		String[] mapping = outcomeMapping.split("\\s+");
		
		for(int i = 0; i < outcomes.length; i++)
		{
			Pattern pat2 = Pattern.compile("\"(.|\\s)*\"");
			Matcher m2 = pat2.matcher(outcomes[i]);
			
			if (m2.find())
			{
				endIndex = m2.end();
			}
			else
			{
				System.out.println("NFGToXML Error: problem in processing outcome mapping.");
			}
			
			int t = Integer.parseInt(mapping[i]) - 1;  	
			payoffString = outcomes[t].substring(endIndex);
			payoffString = payoffString.replace("\"", "");
			payoffString = payoffString.replace(",", "").trim();
			
			String[] payoffs = payoffString.split("\\s+");
			int numPlayers = this.playerNames.size();
			
			//TODO improve this section of code for efficiency
			if (this.playerPayoffs.size() < numPlayers)
			{
				for (int k = 0; k < numPlayers; k++)
				{
					this.playerPayoffs.add(new ArrayList<String>());
					
					for (int v = 0; v < outcomes.length; v++)
					{
						this.playerPayoffs.get(k).add("!");
					}
				}
			}

			for (int j = 0; j < payoffs.length; j++)
			{
				this.playerPayoffs.get(j).set(i, payoffs[j].trim());
			}
		}
		this.processPayoffLine();
	}
	
	/* process payoffs of type payoff pairs */
	private void processPayoffPairs(int beginIndex, int endIndex)
	{
		String payoffString = this.buffer.substring(this.linePosition, this.buffer.length());
		this.parsePayoffLine(payoffString.trim());
		this.processPayoffLine();
	}
	
	/* step through NFG file picking up and parsing inforamtion in order */
	private void parseNFGFile()
	{
		//extract game name
		this.processGameDescription();
		
		//extract player names
		this.processPlayerNames();
		
		//extract strategies - either named OR # strategies per player
		this.processStrategies();

		//extract outcomes & outcome list OR payoff pairs (depending on file)
		this.processPayoffs();
		
		//attach size of game attribute
		String gameSize = util.arrayListToBracketList(this.numPlayerStrategies);
		this.stratForm.setAttribute("size", gameSize); 
	}
	
	/* tokenize and process strategy names */
	private void parseStrategyNames(String line)
	{
		line = line.replace("{", "");
		line = line.replace("}", "");
		line = line.trim();

		String[] tokens = line.split("\"\\s+\"");
		
		ArrayList<String> stratNames = new ArrayList<String>();
		
		int placeholder = 1;
		
		for(int i = 0; i < tokens.length; i++)
		{
			tokens[i] = tokens[i].replace("\"", "");
			if (! (tokens[i].trim().length() == 0))
			{
				stratNames.add("\""+tokens[i]+"\"");
			}
			else
			{   //for consistency of behavior, fill in blank strategy with placeholder
				stratNames.add("\"_" + placeholder +"\"");
				placeholder++;
			}
		}
		
		this.numPlayerStrategies.add(""+stratNames.size());
		this.playerStrategies.add(stratNames);
	}
	
	/* tokenize and process payoff values */
	private void parsePayoffLine(String line)
	{
		//need to know the number of strategies per player
		//then pick up each pair of payoffs and place in the right matrix position
		String[] tokens = line.split("\\s+");
		
		int numPlayers = this.playerNames.size();
		
		for (int j = 0; j < numPlayers; j++ )
		{
			//TODO improve efficiency of this code - place loop in different location?
			if (this.playerPayoffs.size() < numPlayers)
			{
				this.playerPayoffs.add(new ArrayList<String>());
			}
			
			for (int i = j; i < tokens.length; i = i + numPlayers)
			{
				this.playerPayoffs.get(j).add(tokens[i]);
			}
		}
	}
	
	/* take payoff information and match with players in order to create the payoff matrices
	 * and XML elements */
	private void processPayoffLine()
	{
		for (int j = 0; j < this.playerNames.size(); j++ )
		{
			Element child = createXMLPayoffNode(this.playerNames.get(j), createPayoffMatrix(this.playerPayoffs.get(j)));
			this.stratForm.appendChild(child);
		}
	}
	
	/* take a single list of payoffs and format to a matrix format (2D) format */
	private String createPayoffMatrix(ArrayList<String> payoffs)
	{
		int totalResponses = payoffs.size();
		String temp = "";
		
		int col = Integer.parseInt(this.numPlayerStrategies.get(1)); //player 2 determines number of columns
		int row = Integer.parseInt(this.numPlayerStrategies.get(0));
		int matSize = row * col;  //size of matrix 
		
		for (int t = 0; t < totalResponses; t += matSize)
		{
			for (int r = 0; r < row; r++)
			{
				for (int c = 0; c < matSize; c += row)
				{
					temp = temp + " " + payoffs.get(t + r + c);
				}
				temp += "\n";
			}
		}

		return temp;
	}
	
	/* create XML node for payoff info */
	private Element createXMLPayoffNode(String player, String payoff)
	{
		Element child = this.xmlDoc.createElement("payoffs");
        child.setAttribute("player", player);
		child.appendChild(this.xmlDoc.createTextNode(payoff));
		
        return child;
	}
	
	/* create XML node for strategy info */
	private Element createXMLStrategyNode(String player, String strategy)
	{
		Element child = this.xmlDoc.createElement("strategy");
        child.setAttribute("player", player);
		child.appendChild(this.xmlDoc.createTextNode(strategy));
		
        return child;
	}
	
	/* main class method to drive conversion from nfg to xml */
	public void convertNFGToXML()
	{
		try 
		{
			//create DOM document for XML
			this.createXMLDocument("gte");

			//Assemble XML by reading file and parsing into appropriate elements
			this.readNFGFile();
			this.parseNFGFile();
			
			//Transform XML
			TransformerFactory factory = TransformerFactory.newInstance();
            Transformer trans = factory.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            if(this.testMode)
            {
            	trans.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, this.dtd);
            }
            
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(this.xmlDoc);
            trans.transform(source, result);
            String xmlString = result.getWriter().toString();

            String outFile = this.filename.substring(0, filename.length() - 4) + this.fileSuffix;
            util.createFile(outFile, xmlString); 
		}
		catch(Exception e)
		{
			System.out.println("exception is " + e.toString());
		}
	}
	
	public static void main (String [] args)
	{	
	    String fn = args[0];
		NFGToXML ntx = new NFGToXML(fn);
		
		if (args.length > 1)
		{
			String ext = args[1];
			ntx.setFileSuffix(ext);
		}

		ntx.convertNFGToXML();
	}

}
