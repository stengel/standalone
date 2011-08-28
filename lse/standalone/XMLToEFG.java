/* author: K. Bletzer */
/* last updated August 18, 2011 */
package lse.standalone;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/* takes an XML file that conforms to the gte v0.1 XML specification
 * and convert the data to Gambit .efg format.
 */
public class XMLToEFG 
{
	private ArrayList<String> playerNames;
	private String filename;
	private StringBuffer efgFile;
	private ConversionUtilities util;
	private String gameDescription;
	private int MAX_PLAYERS = 15;
	private String fileSuffix = ".efg";
	
	/* constructor */
	public XMLToEFG(String fn)
	{
		this.playerNames = new ArrayList<String>();
		this.efgFile = new StringBuffer();
		this.util = new ConversionUtilities();
		this.gameDescription = "";
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

	/* process node or outcome XML nodes */
	private void processChildren(Element parent)
	{
		for (Node child = parent.getFirstChild(); child != null; child =  child.getNextSibling()) 
		{
			if ("node".equals(child.getNodeName())) 
			{
				processDecisionNode((Element)child);
			} 
			else if ("outcome".equals(child.getNodeName())) 
			{
				processOutcome((Element)child);
			} 
		}
	}
	
	/* process XML header information such as game description and players */
	private void processHeader(Element root)
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
			else if ("extensiveForm".equals(child.getNodeName())) 
			{
				this.processChildren((Element)child);
			} 
		}
	}
	
	/* initial processing of the XML file */
	private void readXML(String filename)
	{	
		Document xml = util.fileToXML(filename);
		
		Element root = xml.getDocumentElement();
		
		if ("gte".equals(root.getNodeName())) 
		{
			this.processHeader(root);
		}
		
		String playerList = " { ";
		for(int i=0; i < this.playerNames.size(); i++)
		{
			playerList+= "\"" + this.playerNames.get(i) +"\" ";
		}
		playerList += "}";
		
		this.efgFile.insert(0, "EFG 2 R " + gameDescription+  playerList + "\n\"\"\n\n");
	}
	
	
	//processing a decision node, which may be similar to the following:
	/*<extensiveForm>
 		<node player="1">
    		<outcome move="0:0">
      			<payoff player="1">16</payoff>
      			<payoff player="2">4</payoff>
    		</outcome>
  		</node>
	</extensiveForm> */
	private void processDecisionNode(Node node)
	{
		String result="";
		
		ArrayList<String> moves = this.processNodeActions(node);
		ArrayList<String> probs = this.processNodeProbs(node);
		
		if (probs.size() == 0)  //no probabilities => player node
		{
			result = this.processPlayerNode(node, moves);
		}
		else  //chance node
		{
			result = this.processChanceNode(node, moves, probs);
		}
		
		this.efgFile.append(result+"\n");
		this.processChildren((Element)node);
	}
	
	//convenience method for formatting individual data elements for efg file
	private String addNodeData(String d, String dataType)
	{
		String result = "";
		
		if (dataType.equals("STRING"))
		{
			if (d == null) { result =  "\"\"" + " "; }
			else { result = "\"" + d + "\" " ; }
		}
		else if (dataType.equals("NUMBER"))
		{
			if (d == null) { result = "0 "; }
			else { result = d + " "; }
		}
		
		return result;
	}
	
	/* process the node actions for a decision node by looping through the node's children */
	private ArrayList<String> processNodeActions(Node node)
	{
		ArrayList<String> moveList = new ArrayList<String>();
		//retrieve actions (this is the list of "move" attributes from all of the children)
		NodeList children = node.getChildNodes();
		
		if (children.getLength() > 0)
		{
			for (int i = 0; i < children.getLength(); i++)
			{
				Node child = children.item(i);
				String childName = child.getNodeName();
				if (childName.equals("node") || childName.equals("outcome")) 
				{
					moveList.add( "\"" + util.getAttribute(child, "move") + "\"");
				}
			}
		}
		
		return moveList;
	}
	
	/* process the node probabilities by looping through the children of the current node */
	private ArrayList<String> processNodeProbs(Node node)
	{
		ArrayList<String> probList = new ArrayList<String>();
		//retrieve actions (this is the list of "move" attributes from all of the children)
		NodeList children = node.getChildNodes();
		
		if (children.getLength() > 0)
		{
			for (int i = 0; i < children.getLength(); i++)
			{
				Node child = children.item(i);
				String childName = child.getNodeName();
				if (childName.equals("node") || childName.equals("outcome")) 
				{
					String prob = util.getAttribute(child, "prob");
					if (prob != null)
					{
						probList.add(prob); 
					}
				}
			}
		}
		
		return probList;
	}
	
	/* extract payoff information from an internal node and format for efg file */
	private String processInternalPayoff(Node node, String outcomeName)
	{
		String result = "";
		//process payoffs if they exist
		NodeList children = node.getChildNodes();
		
		if (children.getLength() > 0)  //the node could have multiple children no payoff nodes
		{
			String[] orderedPlayers = new String[this.MAX_PLAYERS];
			boolean payoffAvailable = false;
			
			for (int i = 0; i < children.getLength(); i++) 
			{
				if ("payoff".equals(children.item(i).getNodeName())) 
				{
					payoffAvailable = true;
					String player = util.getAttribute(children.item(i), "player");
					this.addPlayerName(player);
					int playerNum = Integer.parseInt(getPlayerNumber(player));
					String payoffValue = children.item(i).getTextContent();

					orderedPlayers[playerNum] = payoffValue;
				} 
			}
			
			if (payoffAvailable)
			{
				result += " " + this.addNodeData(outcomeName, "STRING");
				result += "{ ";
				result += this.processEFGPayoff(orderedPlayers);
				int lastCommaStart = result.lastIndexOf(", ");
				result = result.substring(0, lastCommaStart);
				result += " }";
			}
		} 
		return result;
	}
	
	/* extract relevant decision node information from node and children (moves)
	 *  and format for efg file */
	private String processPlayerNode(Node node, ArrayList<String> moves)
	{
		String playerName = util.getAttribute(node, "player");
		this.addPlayerName(playerName);
		String playerNumber = this.getPlayerNumber(playerName);
		
		String nodename = util.getAttribute(node, "nodeName");
		String iset = util.getAttribute(node, "iset");
		String isetName = util.getAttribute(node, "isetName");
		String outcomeNum = util.getAttribute(node, "outcomeId");
		String outcomeName = util.getAttribute(node, "outcomeName");

		String result = "p ";
		result += this.addNodeData(nodename, "STRING");
		result += this.addNodeData(playerNumber, "NUMBER");
		result += this.addNodeData(iset, "NUMBER");
		result += this.addNodeData(isetName, "STRING");
		result += util.arrayListToBracketList(moves) +" "; //movelist
		result += this.addNodeData(outcomeNum, "NUMBER");
		
		result = result.trim();
		result += this.processInternalPayoff(node, outcomeName);
		
		return result;
	}
	
	/* extract relevant chance node and children (prob and move) information
	 *  and format for efg file */
	private String processChanceNode(Node node, ArrayList<String> moves, ArrayList<String> probs)
	{
		String nodename = util.getAttribute(node, "nodeName");
		String iset = util.getAttribute(node, "iset");
		String isetName = util.getAttribute(node, "isetName");
		String outcomeNum = util.getAttribute(node, "outcomeId");
		String outcomeName = util.getAttribute(node, "outcomeName");

		String result="c ";
		result += this.addNodeData(nodename, "STRING");
		result += this.addNodeData(iset, "NUMBER");
		result += this.addNodeData(isetName, "STRING");
		
		String moveList = "{ ";
		for (int i = 0; i < moves.size(); i++)
		{
			moveList += moves.get(i) + " " + probs.get(i) +" ";
		}
		
		moveList += "} ";
		result += moveList;
		
		result += this.addNodeData(outcomeNum, "NUMBER");
		result = result.trim();
		result += this.processInternalPayoff(node, outcomeName);
		
		return result.trim();
	}
	
	/* process outcome node information and format for efg file */
	private void processOutcome(Node node)
	{
		String outcomeName = this.addNodeData(util.getAttribute(node, "outcomeName"), "STRING");
		String nodeName = this.addNodeData(util.getAttribute(node, "nodeName"), "STRING");
		String outcomeId = this.addNodeData(util.getAttribute(node, "outcomeId"), "NUMBER");
		
		String simpleTerminal = "t " + nodeName + outcomeId;
		
		this.efgFile.append(simpleTerminal.trim());
		
		//process payoffs if they exist
		NodeList children = node.getChildNodes();
		
		if (children.getLength() > 0)
		{
			this.efgFile.append(" " + outcomeName + "{ ");
			String[] orderedPlayers = new String[this.MAX_PLAYERS];
			
			for (int i = 0; i < children.getLength(); i++) 
			{
				if ("payoff".equals(children.item(i).getNodeName())) 
				{
					String player = util.getAttribute(children.item(i), "player");
					this.addPlayerName(player); 
					int playerNum = Integer.parseInt(getPlayerNumber(player));
					String payoffValue = children.item(i).getTextContent();

					orderedPlayers[playerNum] = payoffValue;
				} 
			}
			
			this.efgFile.append(processEFGPayoff(orderedPlayers));

			int lastCommaStart = this.efgFile.lastIndexOf(", ");
			this.efgFile.delete(lastCommaStart, lastCommaStart+2);
			this.efgFile.append(" }");

		}
		this.efgFile.append("\n");
	}
	
	/* format a string of payoffs to a comma separated list for efg file */
	private String processEFGPayoff(String[] al)
	{
		String result = "";
		
		for (int i = 0; i < al.length; i++)
		{
			String payoffValue = al[i];
			if (payoffValue != null)
			{
				result += payoffValue + ", ";
			}
		}
		
		return result;
	}

	/* adds player name taking into account duplicates */
	private void addPlayerName(String name)
	{
		if (!this.playerNames.contains(name))
		{
			this.playerNames.add(name);
		}
	}
	
	/* gets a player's id given their name */
	private String getPlayerNumber(String name)
	{
		if (this.playerNames.contains(name))
		{
			return "" + (1 + this.playerNames.indexOf(name));
		}
		
		return null;
	}
	
	/* create the efg file leveraging the utility class */
	private void createEFGFile(String xmlFileName)
	{
		String outFile = xmlFileName.substring(0, xmlFileName.length() - 4) + this.fileSuffix;
		
		util.createFile(outFile, this.efgFile.toString());
	}
	
	/* main method that organizes conversion tasks */
	public void convertXMLToEFG()
	{
		try 
		{
			this.readXML(this.filename);
			this.createEFGFile(this.filename);
		}
		catch(Exception e)
		{
			System.out.println("exception is " + e.toString());
		}
	}
	
	public static void main (String [] args)
	{	
		String fn = args[0];
		XMLToEFG xte = new XMLToEFG(fn);
		
		if (args.length > 1)
		{
			String ext = args[1];
			xte.setFileSuffix(ext);
		}

		xte.convertXMLToEFG();	
	}

}
