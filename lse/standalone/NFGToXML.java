/* Last updated July 17, 2011 */
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

public class NFGToXML 
{
	private Document xmlDoc;
	private StringBuffer buffer;
	private Element root;
	private String filename;
	private int linePosition;
	private String gameName;
	private ArrayList<String> playerNames;
	private ArrayList<String> numPlayerStrategies;
	private ArrayList<ArrayList<String>> playerStrategies;
	private ArrayList<ArrayList<String>> playerPayoffs;
	private ConversionUtilities util;
	
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
	
	//create DOM document for XML
	private void createXMLDocument(String rootName) throws ParserConfigurationException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();
        
        this.root = doc.createElement(rootName);
        doc.appendChild(root);
        
        this.xmlDoc = doc;
	}
	
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
			//better handling?
			System.out.println("Exception: " + e);
		}
	}
	
	private void processGameName()
	{
		int beginIndex, endIndex;
		
		beginIndex = this.buffer.indexOf("\"");
		
		if (beginIndex > 0) 
		{ 
			endIndex = this.buffer.indexOf("\"", beginIndex+1);
			
			this.linePosition = endIndex;
			this.gameName = this.buffer.substring(beginIndex+1, endIndex);
		}
	}
	
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
			//error!
		}
	}
	
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
			//error!
		}
		
		int numPlayers = this.playerStrategies.size();
		
		for (int i =0; i < numPlayers; i++)
		{
			String strat = util.arrayListToBracketList(this.playerStrategies.get(i));
			Element child = createXMLStrategyNode(this.playerNames.get(i), strat);
			this.root.appendChild(child);
		}
	}
	
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
			//error
		}
		
		//strategy list is substring between beginIndex and endIndex
		strategyLine = this.buffer.substring(beginIndex, endIndex).trim();
		
		String[] strategies = strategyLine.split("\\}\\s*\\{");
		
		for(int i = 0; i < strategies.length; i++)
		{
			this.parseStrategyNames(strategies[i]);
		}
	}
	
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
			//error
		}
		
		//strategy list is substring between beginIndex and endIndex
		strategyLine = this.buffer.substring(beginIndex, endIndex).trim();
		
		String[] strategies = strategyLine.split("\\s+");
		
		for(int i = 0; i < strategies.length; i++)
		{
			this.numPlayerStrategies.add(strategies[i]);
		}
	}
	
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
			//error
		}
		
		String outcomesString = this.buffer.substring(beginIndex+1, endIndex-1).trim();
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
				//error
			}
			
			payoffString = outcomes[i].substring(endIndex);
			String[] payoffs = payoffString.split(",");
			int numPlayers = this.playerNames.size();
			
			//move this to somewhere else for increased efficiency...but create here for now
			//also don't like the way the arraylist needs to be filled, should be a better way
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
				int t = Integer.parseInt(mapping[i]) - 1;
				String p = payoffs[j];
				this.playerPayoffs.get(j).set(t, p);
			}
		}
		this.processPayoffLine();
	}
	
	private void processPayoffPairs(int beginIndex, int endIndex)
	{
		String payoffString = this.buffer.substring(this.linePosition, this.buffer.length());
		this.parsePayoffLine(payoffString.trim());
		this.processPayoffLine();
	}
	
	private void parseNFGFile()
	{
		//extract game name
		this.processGameName();
		
		//extract player names
		this.processPlayerNames();
		
		//extract strategies - either named OR # strategies per player
		this.processStrategies();

		//extract outcomes & outcome list OR payoff pairs (depending on file)
		this.processPayoffs();
		
		//attach size of game attribute
		String gameSize = util.arrayListToBracketList(this.numPlayerStrategies);
		this.root.setAttribute("size", gameSize); 
	}
	
	private void parseStrategyNames(String line)
	{
		//may need to revisit this split to account for empty names
		String[] tokens = line.split("\\{|\\}|\"\\s+\"|\"");
		
		ArrayList<String> stratNames = new ArrayList<String>();
		
		for(int i = 0; i < tokens.length; i++)
		{
			if (! (tokens[i].trim().length() == 0))
			{
				stratNames.add("\""+tokens[i]+"\"");
			}
		}
		
		this.numPlayerStrategies.add(""+stratNames.size());
		this.playerStrategies.add(stratNames);
	}
	
	private void parsePayoffLine(String line)
	{
		//need to know the number of strategies per player
		//then pick up each pair of payoffs and place in the right matrix position
		String[] tokens = line.split("\\s+");
		
		int numPlayers = this.playerNames.size();
		
		for (int j = 0; j < numPlayers; j++ )
		{
			//move this to somewhere else for increased efficiency...but create here for now
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
	
	private void processPayoffLine()
	{
		for (int j = 0; j < this.playerNames.size(); j++ )
		{
			Element child = createXMLPayoffNode(this.playerNames.get(j), createPayoffMatrix(this.playerPayoffs.get(j)));
			this.root.appendChild(child);
		}
	}
	
	private String createPayoffMatrix(ArrayList<String> payoffs)
	{
		int numPlayers = this.playerNames.size();
		
		int totalResponses = payoffs.size();
		boolean processed = false; //keep track of adding newlines, add only when adding content
		int i = 0;
		String temp = "";
		
		//TODO: here we are going through the loop extra times at the end, should fix for efficiency
		while ( i < totalResponses)
		{
			for(int j = 0; j < numPlayers - 1; j++)
			{
				int numStrat = Integer.parseInt(this.numPlayerStrategies.get(j));

				for (int k = 0; (i+k < totalResponses)  && (i < numStrat); k = k+numStrat)
				{
					temp = temp + " " + payoffs.get(i+k);
					processed = true;
				}
				i++;
				
				if (processed) 
				{ 
					temp = temp + (char)10; 
					processed = false;
				}
			}
		}
		return temp;

	}
	
	private Element createXMLPayoffNode(String player, String payoff)
	{
		Element child = this.xmlDoc.createElement("payoff");
        child.setAttribute("player", player);
		child.appendChild(this.xmlDoc.createTextNode(payoff));
		
        return child;
	}
	
	private Element createXMLStrategyNode(String player, String strategy)
	{
		Element child = this.xmlDoc.createElement("strategy");
        child.setAttribute("player", player);
		child.appendChild(this.xmlDoc.createTextNode(strategy));
		
        return child;
	}
	
	public void convertNFGToXML()
	{
		try 
		{
			//create DOM document for XML
			this.createXMLDocument("strategicForm");

			//Assemble XML by reading file and parsing into appropriate elements
			this.readNFGFile();
			this.parseNFGFile();
			
			//Transform XML
			TransformerFactory factory = TransformerFactory.newInstance();
            Transformer trans = factory.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(this.xmlDoc);
            trans.transform(source, result);
            String xmlString = result.getWriter().toString();

            String outFile = this.filename.substring(0, filename.length() - 4) + ".xml";
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

		ntx.convertNFGToXML();
	}

}
