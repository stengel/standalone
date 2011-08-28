/* author: K. Bletzer */
/* last updated August 28, 2011 */
package lse.standalone;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

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

/* A strategic form flat file is a file with one or two matrices.
 * Each row of a matrix is separated from the next by a newline.
 * Two matrices are separated by a blank line.
 * 
 * This class takes such a file as an input and transforms the 
 * matrices into an XML structure suitable for consumption by the 
 * gte tool.
 */
public class StrategicFileToXML 
{
	private String a1 = "";
	private String a2 = "";
	private String version = "0.1";
	
	private int numRows;    //number of rows in payoff matrices
	private int numCols;	//number of columns in payoff matrices
	private String filename;
	private ArrayList<String> playerNames;
	private boolean bSinglePayoff = false; //will be updated based on input matrices
	
	private Element root;
	private Element stratForm;
	private Document xmlDoc;
	private ConversionUtilities util;
	private ArrayList<String> fileLines;
	
	private boolean testMode = false;
	private String dtd;
	private String fileSuffix = ".xml";
	
	/* Constructor
	 * @param fn: the filename for the file that contains the matrices to be used in constructing
	 * the xml.
	 */
	public StrategicFileToXML(String fn)
	{
		filename = fn;
		this.util = new ConversionUtilities();
		this.playerNames =  new ArrayList<String>();
		this.fileLines = new ArrayList<String>();
	}
	
	
	/*
	 * @param fn: filename to change the file that is being converted.
	 
	public void setFilename(String fn)
	{
		filename = fn;
	}*/
	
	/*
	 * @param tm: true or false.  If test mode is on the dtd will be refernced
	 * in the output XML and the XML document factory will have setValidating = true
	 */
	public void setTestMode(boolean tm)
	{
		this.testMode = tm;
	}
	
	/*
	 * @param d: the dtd file name; used only in test mode
	 */
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
	
	
	/* create DOM document for building XML; initialize relevant XML root "handles"
	 * to be used in the creation of child elements.
	 * If the class is set to testMode = true then the document will contain a
	 * reference to the DTD for validation purposes.  TestMode should be false
	 * for conversion in non-test circumstances.
	 * 
	 * @param rootName - the XML root element
	 * @throws ParserConfigurationException
	 */
	private void createXMLDocument(String rootName) throws ParserConfigurationException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        if(this.testMode) 
        { 
        	factory.setValidating(true); 
        	factory.setNamespaceAware(true);
        } 
        Document doc = builder.newDocument();
        
        this.root = doc.createElement(rootName);
        doc.appendChild(this.root);
        this.root.setAttribute("version", this.version);
        
        if (this.bSinglePayoff) 
        { 
            Element display = doc.createElement("display");
            this.root.appendChild(display);
            Element single = doc.createElement("singlePayoff");
        	single.setTextContent("true");
            display.appendChild(single);
        }
        
		Element descr = doc.createElement("gameDescription");
    	this.root.appendChild(descr);  
    	
    	this.processPlayerNames();
    	Element players = util.createPlayersNode(this.playerNames, doc);
    	this.root.appendChild(players);
    	   
        this.stratForm = doc.createElement("strategicForm");
        this.root.appendChild(this.stratForm);
        
        this.xmlDoc = doc;
	}
	
	/* main public method that coordinates the conversion of flat file matrices
	 * to gte strategic form XML.
	 */
	public void convertFlatFileToXML()
	{
		try 
		{
			this.readFlatFile();
			this.splitMatrices(this.fileLines);
			
			this.createXMLDocument("gte");
			
			this.parseFlatFile();
			
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
	
	/* create the required strategy and payoff structures based on defaults */
	private void parseFlatFile()
	{
		//create strategy names
		this.processStrategies();

		//process payoffs based on matrices in input file
		this.processPayoffs();
		
		//attach size of game attribute
		String gameSize = "{ " + this.numRows + " " + this.numCols + " }";
		this.stratForm.setAttribute("size", gameSize); 
	}
	
	/* create the required strategy structures based on defaults */
	private void processStrategies()
	{
		Element child = createXMLStrategyNode(this.playerNames.get(0), this.generateRowLabels());
		this.stratForm.appendChild(child);
		
		child = createXMLStrategyNode(this.playerNames.get(1), this.generateColumnLabels());
		this.stratForm.appendChild(child);
	}
	
	/* create the required player Names based on defaults */
	private void processPlayerNames()
	{
		this.playerNames.add(0, "I");
		this.playerNames.add(1, "II");
	}
	
	/* create the required payoff structures based on defaults */
	private void processPayoffs()
	{
		String payoff1 = this.a1;
		Element child = createXMLPayoffNode(this.playerNames.get(0), payoff1);
		this.stratForm.appendChild(child);

		String payoff2 = this.a2;
		child = createXMLPayoffNode(this.playerNames.get(1), payoff2);
		this.stratForm.appendChild(child);
	}
	
	/* Convenience method - create an XML strategy node */
	private Element createXMLStrategyNode(String player, String strategy)
	{
		Element child = this.xmlDoc.createElement("strategy");
        child.setAttribute("player", player);
		child.appendChild(this.xmlDoc.createTextNode(strategy));
		
        return child;
	}
	
	/* Convenience method - create an XML payoff node */
	private Element createXMLPayoffNode(String player, String payoff)
	{
		Element child = this.xmlDoc.createElement("payoffs");
        child.setAttribute("player", player);
		child.appendChild(this.xmlDoc.createTextNode(payoff));
		
        return child;
	}
	
	//reads the lines from the input text file
	private void readFlatFile() 
	{
		String fileLine;

		try
        {
			BufferedReader reader = new BufferedReader(new FileReader(this.filename));	
			
			//read all the lines from the file and append to string buffer
			while ((fileLine = reader.readLine()) != null) 
			{
				this.fileLines.add(fileLine+"\n");
		    }
		    reader.close();
        }
		catch (Exception e)
		{
			System.out.println("StrategicFileToXML Exception: " + e);
		}
	}
	
	//there may be either one or two matrices in the input text
	//if one, duplicate to make 2 symmetric matrices
	//otherwise output two as expected
	 //@SuppressWarnings({"unchecked"})
	private void splitMatrices(ArrayList<String> fileLines)
	{
		boolean bSwitch = false;
		int cols = 0;
		int rows = this.textToArray(fileLines.get(0)).size();
		
		//loop through all the lines
		//putting them either into payoff strings a1 or a2
		for (Iterator<String> i = fileLines.iterator(); i.hasNext();) 
		{
			String fileLine = (String)i.next();
			
			if (bSwitch) 
			{
				a2+=fileLine;
			}
			else if (fileLine.trim().length() == 0)  //check for blank dividing line
			{
				bSwitch = true;
			}
			else
			{
				a1+=fileLine;
				cols++;
			}
		}
		
		if (a2.length() < 1)
		{
			a2 = a1;
			this.bSinglePayoff = true;
		}
		
		this.numCols = cols;
		this.numRows = rows;
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
	
	//generate default labels in XML format, based on number of strategies
	private String generateRowLabels() 
	{
		 String label;
		 char chr;
		 switch (this.numRows)
		 {
	         case 1:  label = "{ \"M\" }";		break;
	         case 2:  label = "{ \"T\" \"B\" }";	break;
	         case 3:  label = "{ \"T\" \"M\" \"B\" }";	break;
	         default: 
	         {
	        	 label = "{";
	        	 chr = 97; //97 = ascii 'a' - label cols a, b, c, d, e...etc.
	        	 for (int i = 97; i < this.numRows+97; i++)
	        	 {
	        		 chr = (char) i;
	        		 label = label + "\"" + chr + "\" ";
	        	 }
	        	 label = label + "}";
	         }
		 }
	     return label;
	}
	
	//generate default labels in XML format, based on number of strategies
	private String generateColumnLabels() 
	{
		String label;
		 char chr;
		 switch (this.numCols)
		 {
	         case 1:  label = "{ \"c\" }";		break;
	         case 2:  label = "{ \"l\" \"r\" }";	break;
	         case 3:  label = "{ \"l\" \"c\" \"r\" }";	break;
	         default: 
	         {
	        	 label = "{";
	        	 chr = 97; 			//97 = ascii 'a'
	        	 for (int i = 97; i < numCols+97; i++)
	        	 {
	        		 chr = (char) i;
	        		 label = label + "\"" + chr + "\" ";
	        	 }
	        	 label = label + "}";
	         }
		 }
	     return label;
	}

	public static void main (String [] args)
	{	
		String filename = args[0];
		StrategicFileToXML stx = new StrategicFileToXML(filename);
		
		if (args.length > 1)
		{
			String ext = args[1];
			stx.setFileSuffix(ext);
		}
		stx.convertFlatFileToXML(); 
		
	}
}
