/* author: K. Bletzer */
/* Last updated August 28, 2011 */
package lse.standalone;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/* takes an XML file that conforms to the gte v0.1 XML specification
 * and convert the data to Gambit .nfg format.  The format may either be
 * "outcome" format (default) or "payoff" format based on chosen setting.  
 */
public class XMLToNFG 
{
	private String nfgString;
	private String gameDescription="\"\"";
	private ConversionUtilities util;
	private ArrayList<String> playerNames;
	private HashMap<String, ArrayList<String>> playerStrategies;
	private ArrayList<String> numPlayerStrategies;
	private HashMap<String, String> playerPayoffs;
	private String filename;
	private boolean outcomeFormat = true; //outcome or payoff format for the output
	private String fileSuffix = ".nfg";

	/* constructor */
	public XMLToNFG(String fn)
	{
		this.util = new ConversionUtilities();
		this.playerNames = new ArrayList<String>();
		this.playerStrategies = new HashMap<String, ArrayList<String>>();
		this.numPlayerStrategies = new ArrayList<String>();
		this.playerPayoffs = new HashMap<String, String>();
		this.filename = fn;
	}
	
	/* set format for nfg output */
	public void setNFGFormat(String format)
	{
		if (format.equals("outcome")) { this.outcomeFormat = true; }
		else if (format.equals("payoff")) { this.outcomeFormat = false; }
	}
	
	/* Change the suffix appended to the file if desired.
	 * For example, instead of the default .tex, can append _test.tex 
	 * to the file name if required to differentiate files. 
	 */
	public void setFileSuffix(String suffix)
	{
		this.fileSuffix = suffix;
	}
	
	/* begins the process of creating the nfg file by creating the header and 
	 * calling other methods to complete the work 
	 */
	private void createNFGFile(String xmlFileName)
	{
		ArrayList<String> players = this.getPlayerNames();
		
		this.nfgString = "NFG 1 R " + this.gameDescription + " \n";
		this.nfgString += this.formatPlayerData(players);
		this.nfgString += this.formatStrategyData();
		
		if (!outcomeFormat) {  this.nfgString += this.formatPayoffData();  }
		else { this.nfgString += this.formatOutcomeData();  } 
		
		String outFile = xmlFileName.substring(0, xmlFileName.length() - 4) + this.fileSuffix;
		
		util.createFile(outFile, this.nfgString);
	}
	
	/* Change player data from ArrayList to String format */
	private String formatPlayerData(ArrayList<String> players)
	{
		String result = "{ ";
		
		for (int i = 0; i < players.size(); i++)
		{
			result += "\""+players.get(i)+"\" ";
		}
		
		return result + "}";
	}
	
	/* reads data from strategic form XML and process */
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
	
	/* reads XML document at the "header" level and requests other methods
	 * to continue reading child information.
	 */
	private void readXML(String filename)
	{
		Document xml = util.fileToXML(filename);
		Element root = xml.getDocumentElement();

		//if ("gte".equals(root.getNodeName())) 
		if (root!=null) 
		{
			for (Node child = root.getFirstChild(); child != null; child =  child.getNextSibling()) 
			{
				if ("gameDescription".equals(child.getNodeName()))
				{
					this.gameDescription = "\"" + child.getTextContent() + "\"";
				}
				if ("players".equals(child.getNodeName()))
				{
					this.playerNames = util.readPlayersXML(child);
				}
				if ("strategicForm".equals(child.getNodeName())) 
				{	
					this.readStrategicForm(child);
				}
			}
		}
		else 
		{
			System.out.println("XMLToNFG Error: first XML element not recognized.");
		}
	}
	
	/* reads payoff information from the XML and places in internal class storage */
	private void processPayoff(Node node)
	{
		NodeList nl = node.getChildNodes();
		String value = nl.item(0).getNodeValue();
		String playerName = util.getAttribute(node, "player");
		
		this.addPlayerName(playerName);
		
		this.playerPayoffs.put(playerName, value.trim()); 
	}
	
	/* reads strategy information from the XML and places in internal class storage */
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
	
	/* formats strategy data for output to nfg file */
 	private String formatStrategyData()
	{
 		ArrayList<String> players = this.getPlayerNames();
 		String result = "\n\n{ ";
 		boolean stratAvailable = false;
 		
		for (int i = 0; i < players.size(); i++)
		{
			ArrayList<String> strat = this.getPlayerStrategiesByName(players.get(i));
			if (strat != null)
			{
				stratAvailable = true;
				result += "{ ";
				for (int j = 0; j < strat.size(); j++)
				{
					result = result + "\"" + strat.get(j)+"\" ";
				}
				result += "}\n";
			}
		}
		result += "}" ;
		
		if (!stratAvailable)
		{	
			result = "\n{ ";
			
			for (int i = 0; i<players.size(); i++)
			{
				result += this.numPlayerStrategies.get(i) +" ";
			}
			
			result += "}";
		}
		
		return result;
	} 
	
 	/* takes payoff information and formats for nfg file, in payoff format */
	private String formatPayoffData()
	{
		ArrayList<String> players = this.getPlayerNames();
		ArrayList<String[]> allPayoffs = new ArrayList<String[]>();
		
		String result = "\n\n";
		
		for (int i = 0; i<players.size(); i++)
		{
			String p = this.getPlayerPayoffsByName(players.get(i));
			String[] pp = p.split("\\s+");
			allPayoffs.add(pp);
		}
		
		int totalResponses = allPayoffs.get(0).length;
		int col = Integer.parseInt(this.numPlayerStrategies.get(1));  //player 2 determines number of columns
		int row = Integer.parseInt(this.numPlayerStrategies.get(0));
		int matSize = row * col;  //size of payoff matrix 

		for (int t = 0; t < totalResponses; t += matSize)
		{
			for (int r = 0; r < col; r++)
			{
				for (int c = 0; c < matSize; c += col)
				{
					for (int i = 0; i < players.size(); i++)
					{
						result = result + allPayoffs.get(i)[t+r+c] + " ";
					}
				}
				result += "\n";
			}
		}
		
		return result;
	} 
	
	
 	/* takes payoff information and formats for nfg file, in outcmoe format */
	private String formatOutcomeData()
	{
		ArrayList<String> players = this.getPlayerNames();
		ArrayList<String[]> allPayoffs = new ArrayList<String[]>();
		
		for (int i = 0; i<players.size(); i++)
		{
			String p = this.getPlayerPayoffsByName(players.get(i));
			String[] pp = p.split("\\s+");
			allPayoffs.add(pp);
		}
		
		String result = "\n\n{\n";
		
		int totalResponses = allPayoffs.get(0).length;
		int col = Integer.parseInt(this.numPlayerStrategies.get(1));  //player 2 determines number of columns
		int row = Integer.parseInt(this.numPlayerStrategies.get(0));
		int matSize = row * col;  //size of payoff matrix 
		int outcomeOrder = 1;
		String orderString = "";

		for (int t = 0; t < totalResponses; t += matSize)
		{
			for (int r = 0; r < col; r++)
			{
				for (int c = 0; c < matSize; c += col)
				{
					result += "{ \"\" ";
					for (int i = 0; i < players.size(); i++)
					{
						result += allPayoffs.get(i)[t+r+c] + ", ";
					}
					result = result.substring(0, result.length()-2);
					result += " }\n";
					orderString += outcomeOrder++ + " ";
				}
			}
		}
		result += "}\n" + orderString;
		
		
		return result ;
	} 
	
	/* retrieves player names in order */
	private ArrayList<String> getPlayerNames()
	{
		return this.playerNames;
	}
	
	/* retrieves player's strategies given a player's name */
	private ArrayList<String> getPlayerStrategiesByName(String playerName)
	{
		return this.playerStrategies.get(playerName);
	}
	
	/* retrieves player's payoffs given a player's name */
	private String getPlayerPayoffsByName(String playerName)
	{
		return this.playerPayoffs.get(playerName);
	} 
	
	/* add player name to list, checking for duplicates first */
	private void addPlayerName(String name)
	{
		if (!this.playerNames.contains(name))
		{
			this.playerNames.add(name);
		}
	}
	
	/* main method that initiations conversion from XML format to NFG format */
	public void convertXMLToNFG()
	{
		try 
		{
			this.readXML(this.filename);
			this.createNFGFile(this.filename);
		}
		catch(Exception e)
		{
			System.out.println("XMLToNFG Exception: " + e.toString());
		}
	}
	
	public static void main (String [] args)
	{	
		String fn = args[0];
		XMLToNFG xtn = new XMLToNFG(fn);
		
		if (args.length > 1)
		{
			String ext = args[1];
			xtn.setFileSuffix(ext);
		}

		xtn.convertXMLToNFG();
	}
}
