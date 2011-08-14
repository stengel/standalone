package lse.standalone;
/* last updated August 13, 2011 */
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;

public class NFGToXMLTest 
{
	@Rule  
	public TemporaryFolder folder = new TemporaryFolder(); 
	
	@Test
	public void testValidateNFGToXML() throws IOException
	{
		String nfgString = "NFG 1 R \"2x2x2 Example from McKelvey-McLennan...\"";
		nfgString += "{ \"Player 1\" \"Player 2\" \"Player 3\" }\n";
		nfgString += "{ { \"1\" \"2\" }\n";
		nfgString += "{ \"1\" \"2\" }\n";
		nfgString += "{ \"1\" \"2\" }\n";
		nfgString += "}\n";
		nfgString += "\"\"\n\n";
		nfgString += "{\n";
		nfgString += "{ \"\" 9, 8, 12 }\n";
		nfgString += "{ \"\" 0, 0, 0 }\n";
		nfgString += "{ \"\" 0, 0, 0 }\n";
		nfgString += "{ \"\" 9, 8, 2 }\n";
		nfgString += "{ \"\" 0, 0, 0 }\n";
		nfgString += "{ \"\" 3, 4, 6 }\n";
		nfgString += "{ \"\" 3, 4, 6 }\n";
		nfgString += "{ \"\" 0, 0, 0 }\n";
		nfgString += "}\n";
		nfgString += "1 2 3 4 5 6 7 8 \n";

		
		//create nfg file in temporary folder
		File file = folder.newFile("junit1.nfg"); 
		
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
	    out.write(nfgString);
	    out.close();
		
		//convert to xml
		NFGToXML ntx = new NFGToXML(file.getPath());
		System.out.println(file.getPath());
		ntx.convertNFGToXML(); 
		//String xmlFileName = file.getName().substring(0, file.getName().length() - 4) + ".xml";
		String path = file.getPath().replace(file.getName(), "");
		String xmlFileName = path + "junit1.xml";
		
		//read new xml file
		String fileLine;
		String xmlString="";

		BufferedReader reader = new BufferedReader(new FileReader(xmlFileName));	
		
		//read all the lines from the file
		while ((fileLine = reader.readLine()) != null) 
		{
			xmlString+=fileLine;
	    }
	    reader.close();

		
		//compare to "sample" xml file (String built here) and confirm equivalent
		//need to retrieve newly created xml file and compare to 
	    String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		expectedResult += "<gte version=\"0.1\">\n";
		expectedResult += "<gameDescription>2x2x2 Example from McKelvey-McLennan...</gameDescription>\n";
		expectedResult += "<players>\n";
		expectedResult += "<player playerId=\"1\">Player 1</player>\n";
		expectedResult += "<player playerId=\"2\">Player 2</player>\n";
		expectedResult += "<player playerId=\"3\">Player 3</player>\n";
		expectedResult += "</players>\n";
		expectedResult += "<strategicForm size=\"{ 2 2 2 }\">\n";
		expectedResult += "<strategy player=\"Player 1\">{ \"1\" \"2\" }</strategy>\n";
		expectedResult += "<strategy player=\"Player 2\">{ \"1\" \"2\" }</strategy>\n";
		expectedResult += "<strategy player=\"Player 3\">{ \"1\" \"2\" }</strategy>\n";
		expectedResult += "<payoffs player=\"Player 1\"> 9 0\n";
		expectedResult += " 0 9\n";
		expectedResult += " 0 3\n";
		expectedResult += " 3 0\n";
		expectedResult += "</payoffs>\n";
		expectedResult += "<payoffs player=\"Player 2\"> 8 0\n";
		expectedResult += " 0 8\n";
		expectedResult += " 0 4\n";
		expectedResult += " 4 0\n";
		expectedResult += "</payoffs>\n";
		expectedResult += "<payoffs player=\"Player 3\"> 12 0\n";
		expectedResult += " 0 2\n";
		expectedResult += " 0 6\n";
		expectedResult += " 6 0\n";
		expectedResult += "</payoffs>\n";
		expectedResult += "</strategicForm>\n";
		expectedResult += "</gte>\n";
		
		assertEquals("report didn't match xml", xmlString.replace("\n", ""), expectedResult.replace("\n", ""));
	}
	
    /* @Test*/
	public void testValidateNFGDTD() throws ParserConfigurationException, SAXException, IOException
	{
		File folder = new File("/Users/kbletzer/Documents/Gambit-Code/efgToXML_test_files");
	    File[] listOfFiles = folder.listFiles();

	    for (int i = 0; i <  listOfFiles.length ; i++) 
	    {
	      if (listOfFiles[i].isFile()) 
	      {
	    	  String filename = listOfFiles[i].toString();
	    	  if (filename.endsWith(".xml"))
	    	  {
					System.out.println(listOfFiles[i].toString());
					//efgToXML etx = new efgToXML(filename);
					//etx.convertEFGtoXML(); 
	  
	    		  	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					factory.setValidating(true);
					factory.setNamespaceAware(true);
					
					DocumentBuilder builder;
					builder = factory.newDocumentBuilder();
					builder.setErrorHandler(new NFGErrorHandler());
				
					builder.parse(new InputSource(filename));
	    	  }
	      }
	    }
	} 
	
	
}

class NFGErrorHandler implements ErrorHandler
{

	public void error(SAXParseException e) throws SAXException 
	{
		System.out.println("Error at "  + "line " +e.getLineNumber());
		System.out.println(e.getMessage());
		throw new SAXException();
		
	}
	public void fatalError(SAXParseException e) throws SAXException
	{
		System.out.println("fatalError at " + " line " +e.getLineNumber() );
		System.out.println(e.getMessage());
		throw new SAXException();
		
	}
	public void warning(SAXParseException e) throws SAXException 
	{
		System.out.println("Warning at "  + "line" +e.getLineNumber());
		System.out.println(e.getMessage());
		
	}
} 
