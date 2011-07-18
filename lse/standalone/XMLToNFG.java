/* Last updated July 17, 2011 */
package lse.standalone;

import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLToNFG 
{
	private String nfgString;
	private String gameName = "\"My untitled game - temporary!\"";
	private ConversionUtilities util;
	private ArrayList<String> playerNames;
	private HashMap<String, ArrayList<String>> playerStrategies;
	private HashMap<String, String> playerPayoffs;
	private String filename;

	
	public XMLToNFG(String fn)
	{
		this.util = new ConversionUtilities();
		this.playerNames = new ArrayList<String>();
		this.playerStrategies = new HashMap<String, ArrayList<String>>();
		this.playerPayoffs = new HashMap<String, String>();
		this.filename = fn;
	}
	
	public void createNFGFile(String xmlFileName)
	{
		ArrayList<String> players = this.getPlayerNames();
		
		this.nfgString = "NFG 1 R " + this.gameName + " \n";
		this.nfgString += this.formatPlayerData(players);
		this.nfgString += this.formatStrategyData();
		this.nfgString += this.formatPayoffData();
		
		String outFile = xmlFileName.substring(0, xmlFileName.length() - 4) + ".nfg";
		
		util.createFile(outFile, this.nfgString);
	}
	
	private String formatPlayerData(ArrayList<String> players)
	{
		String result = "{ ";
		
		for (int i = 0; i < players.size(); i++)
		{
			result += "\""+players.get(i)+"\" ";
		}
		
		return result + "}";
	}
	
	public void readXML(String filename)
	{
		Document xml = util.fileToXML(filename);
		
		Element root = (Element)xml.getFirstChild();
		if ("strategicForm".equals(xml.getFirstChild().getNodeName())) 
		{			
			for (Node child = root.getFirstChild(); child != null; child =  child.getNextSibling()) 
			{
				if ("strategy".equals(child.getNodeName())) 
				{
					processStrategy((Element)child);
				} 
				else if ("payoff".equals(child.getNodeName())) 
				{
					processPayoff((Element)child);
				} 
				else 
				{
					//unknown element - update handling
				}
			}
		}
		else 
		{
			//error handling - first element not recognized
		}
	}
	
	private void processPayoff(Node node)
	{
		NodeList nl = node.getChildNodes();
		String value = nl.item(0).getNodeValue();
		String playerName = util.getAttribute(node, "player");
		
		this.addPlayerName(playerName);
		
		this.playerPayoffs.put(playerName, value.trim()); /**/
	}
	
	private void processStrategy(Node node)
	{
		NodeList nl = node.getChildNodes();
		String value = nl.item(0).getNodeValue();
		String playerName = util.getAttribute(node, "player");
		
		this.addPlayerName(playerName);

		//parse the value into the strategy arraylist
		value = value.replace("}", "");
		value = value.replace("{", "");
		
		value = value.trim();
		ArrayList<String> strategies = util.extractTokens(value);
		
		this.playerStrategies.put(playerName, strategies);
	}
	
 	private String formatStrategyData()
	{
 		ArrayList<String> players = this.getPlayerNames();
 		String result = " { ";
 		
		//move this to a format method instead of here to match format player & format payoff
		for (int i = 0; i < players.size(); i++)
		{
			result += "{ ";
			ArrayList<String> strat = this.getPlayerStrategiesByName(players.get(i));
			for (int j = 0; j < strat.size(); j++)
			{
				result = result + "\"" + strat.get(j)+"\" ";
			}
			result += " }";
		}
		result += " }" ;
		
		return result;
	} 
	
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
		
		int numPayoffs = allPayoffs.get(0).length;
		
		for (int j = 0; j < numPayoffs; j++ )
		{
			for (int i = 0; i < players.size(); i++)
			{
				result = result + allPayoffs.get(i)[j] +" ";
			}//split the line at a convenient place to avoid a super long line of text
		} 
		
		return result;
	} 
	
	private ArrayList<String> getPlayerNames()
	{
		return this.playerNames;
	}
	
	private ArrayList<String> getPlayerStrategiesByName(String playerName)
	{
		return this.playerStrategies.get(playerName);
	}
	
	private String getPlayerPayoffsByName(String playerName)
	{
		return this.playerPayoffs.get(playerName);
	} 
	
	private void addPlayerName(String name)
	{
		if (!this.playerNames.contains(name))
		{
			this.playerNames.add(name);
		}
	}
	
	public void convertXMLToNFG()
	{
		try 
		{
			this.readXML(this.filename);
			this.createNFGFile(this.filename);
		}
		catch(Exception e)
		{
			System.out.println("exception is " + e.toString());
		}
	}
	
	public static void main (String [] args)
	{	
		String fn = args[0];
		XMLToNFG xtn = new XMLToNFG(fn);

		xtn.convertXMLToNFG();
	}
}
