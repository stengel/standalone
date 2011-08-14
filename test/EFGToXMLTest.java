package lse.standalone;
/* last updated Augut 13, 2011 */
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


public class EFGToXMLTest 
{
	@Rule  
	public TemporaryFolder folder = new TemporaryFolder(); 
	
	@Test
	public void testValidateEFGToXML() throws IOException
	{
		String efgString = "EFG 2 R \"Test file - efg to xml\" { \"Player 1\" \"Henry\" }\n";
		efgString += "\"\"\n";

		efgString += "c \"node 1\" 1 \"(1,1)\" { \"TAKE\" 1/2 \"PASS\" 1/2 } 0\n";
		efgString += "p \"\" 2 1 \"\" { \"PLAY 3A\" \"PLAY 3B\" } 1 \"\" { 0.80, 0.20 }\n";
		efgString += "t \"\" 8 \"\" { 1.20, 2.10 }\n";
		efgString += "t \"\" 9 \"\" { 1.30, 2.10 }\n";
		efgString += "p \"node 3\" 2 2 \"(2,1)\" { \"TAKE\" \"PASS\" } 2 \"result 6\" { 0.50, 0.70 }\n";
		efgString += "t \"\" 3 \"Outcome 2\" { 0.40, 1.60 }\n";
		efgString += "p \"\" 1 1 \"(1,2)\" { \"\" \"\" } 4 \"result 1\" { 0.70, 0.50 }\n";
		efgString += "t \"\" 5 \"\" { 3.20, 0.80 }\n";
		efgString += "p \"\" 2 3 \"(2,2)\" { \"TAKE\" \"PASS\" } 0\n";
		efgString += "t \"\" 6 \"Outcome 4\" { 1.60, 6.40 }\n";
		efgString += "t \"\" 7 \"Outcome 5\" { 12.80, 3.20 }\n";

		
		//create file in temporary folder
		File file = folder.newFile("junit1.efg"); 
		
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
	    out.write(efgString);
	    out.close();
		
		//convert to xml
		EFGToXML etx = new EFGToXML(file.getPath());
		System.out.println(file.getPath());
		etx.convertEFGtoXML(); 
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
	    expectedResult += "<gameDescription>Test file - efg to xml</gameDescription>\n";
	    expectedResult += "<extensiveForm>\n";
	    expectedResult += "<node iset=\"1\" isetName=\"(1,1)\" nodeName=\"node 1\">\n";
	    expectedResult += "<node iset=\"2\" move=\"TAKE\" outcomeId=\"1\" player=\"Henry\" prob=\"1/2\">\n";
	    expectedResult += "<payoff player=\"Player 1\">0.80</payoff>\n";
	    expectedResult += "<payoff player=\"Henry\">0.20</payoff>\n";
	    expectedResult += "<outcome move=\"PLAY 3A\" outcomeId=\"8\">\n";
	    expectedResult += "<payoff player=\"Player 1\">1.20</payoff>\n";
	    expectedResult += "<payoff player=\"Henry\">2.10</payoff>\n";
	    expectedResult += "</outcome>\n";
	    expectedResult += "<outcome move=\"PLAY 3B\" outcomeId=\"9\">\n";
	    expectedResult += "<payoff player=\"Player 1\">1.30</payoff>\n";
	    expectedResult += "<payoff player=\"Henry\">2.10</payoff>\n";
	    expectedResult += "</outcome>\n";
	    expectedResult += "</node>\n";
	    expectedResult += "<node iset=\"3\" isetName=\"(2,1)\" move=\"PASS\" nodeName=\"node 3\" outcomeId=\"2\" outcomeName=\"result 6\" player=\"Henry\" prob=\"1/2\">\n";
	    expectedResult += "<payoff player=\"Player 1\">0.50</payoff>\n";
	    expectedResult += "<payoff player=\"Henry\">0.70</payoff>\n";
	    expectedResult += "<outcome move=\"TAKE\" outcomeId=\"3\" outcomeName=\"Outcome 2\">\n";
	    expectedResult += "<payoff player=\"Player 1\">0.40</payoff>\n";
	    expectedResult += "<payoff player=\"Henry\">1.60</payoff>\n";
	    expectedResult += "</outcome>\n";
	    expectedResult += "<node iset=\"4\" isetName=\"(1,2)\" move=\"PASS\" outcomeId=\"4\" outcomeName=\"result 1\" player=\"Player 1\">\n";
	    expectedResult += "<payoff player=\"Player 1\">0.70</payoff>\n";
	    expectedResult += "<payoff player=\"Henry\">0.50</payoff>\n";
	    expectedResult += "<outcome move=\"_1\" outcomeId=\"5\">\n";
	    expectedResult += "<payoff player=\"Player 1\">3.20</payoff>\n";
	    expectedResult += "<payoff player=\"Henry\">0.80</payoff>\n";
	    expectedResult += "</outcome>\n";
	    expectedResult += "<node iset=\"5\" isetName=\"(2,2)\" move=\"_2\" player=\"Henry\">\n";
	    expectedResult += "<outcome move=\"TAKE\" outcomeId=\"6\" outcomeName=\"Outcome 4\">\n";
	    expectedResult += "<payoff player=\"Player 1\">1.60</payoff>\n";
	    expectedResult += "<payoff player=\"Henry\">6.40</payoff>\n";
	    expectedResult += "</outcome>\n";
	    expectedResult += "<outcome move=\"PASS\" outcomeId=\"7\" outcomeName=\"Outcome 5\">\n";
	    expectedResult += "<payoff player=\"Player 1\">12.80</payoff>\n";
	    expectedResult += "<payoff player=\"Henry\">3.20</payoff>\n";
	    expectedResult += "</outcome>\n";
	    expectedResult += "</node>\n";
	    expectedResult += "</node>\n";
	    expectedResult += "</node>\n";
	    expectedResult += "</node>\n";
	    expectedResult += "</extensiveForm>\n";
	    expectedResult += "</gte>\n";
		
		assertEquals("report didn't match xml", xmlString.replace("\n", ""), expectedResult.replace("\n", ""));
	}
	
    /* @Test*/
	public void testValidateEFGDTD() throws ParserConfigurationException, SAXException, IOException
	{
		File folder = new File("/Users/kbletzer/Documents/Gambit-Code/efgToXML_test_files");
	    File[] listOfFiles = folder.listFiles();

	    //for (int i = 0; i < listOfFiles.length ; i++) 
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
					builder.setErrorHandler(new EFGErrorHandler());
				
					builder.parse(new InputSource(filename));
	    	  }
	      }
	    }
	} 
	
	
}

class EFGErrorHandler implements ErrorHandler
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
