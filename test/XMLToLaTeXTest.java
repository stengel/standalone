package lse.standalone;
/* last updated August 13, 2011 */
//TODO improve flexibility/robustness of testing by using XMLUnit or other XML testing extension

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class XMLToLaTeXTest 
{
	@Rule  
	public TemporaryFolder folder = new TemporaryFolder(); 
	
	@Test
	public void testPayoffPairXML() throws IOException
	{
		String line1 = "1 3/4\n";
		String line2 = "1/4 -8/-9\n";
		String line3 = "5 2\n";
		
		String matrix22 = line1 + line2+"\n"+line2+line3+"\n";
		
		File file = this.createTempFile("matrix22.txt", matrix22);
		//convert to xml
		StrategicFileToXML t = new StrategicFileToXML(file.getPath());
		//t.setFileSuffix(".xml");
		System.out.println(file.getPath());
		t.convertFlatFileToXML(); 
		
		//read new xml file
		String fileLine;
		String xmlString="";
		String path = file.getPath().replace(file.getName(), "");
		String xmlFileName = path + "matrix22.xml";

		BufferedReader reader = new BufferedReader(new FileReader(xmlFileName));	
		
		//read all the lines from the file
		while ((fileLine = reader.readLine()) != null) 
		{
			xmlString+=fileLine;
	    }
	    reader.close();
	    
	    String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	    expectedResult += "<gte version=\"0.1\">\n";
	    expectedResult += "<gameDescription/>\n";
	    expectedResult += "<players>\n";
	    expectedResult += "<player playerId=\"1\">I</player>\n";
	    expectedResult += "<player playerId=\"2\">II</player>\n";
	    expectedResult += "</players>\n";
	    expectedResult += "<strategicForm size=\"{ 2 2 }\">\n";
	    expectedResult += "<strategy player=\"I\">{ \"T\" \"B\" }</strategy>\n";
	    expectedResult += "<strategy player=\"II\">{ \"l\" \"r\" }</strategy>\n";
	    expectedResult += "<payoffs player=\"I\">1 3/4\n";
	    expectedResult += "1/4 -8/-9\n";
	    expectedResult += "</payoffs>\n";
	    expectedResult += "<payoffs player=\"II\">1/4 -8/-9\n";
	    expectedResult += "5 2\n";
	    expectedResult += "</payoffs>\n";
	    expectedResult += "</strategicForm>\n";
	    expectedResult += "</gte>\n";
	    
	    assertEquals("xml does not match", xmlString.replace("\n", ""), expectedResult.replace("\n", ""));
		
	}
	
	@Test
	public void testSinglePayoffXML() throws IOException
	{
		String line1 = "1/4 1 -8/-9\n";
		String line2 = "5 2 1\n";
		
		String matrix23 = line1 + line2+"\n";
		
		File file = this.createTempFile("matrix23single.txt", matrix23);
		//convert to xml
		StrategicFileToXML t = new StrategicFileToXML(file.getPath());
		System.out.println(file.getPath());
		t.convertFlatFileToXML(); 
		
		//read new xml file
		String fileLine;
		String xmlString="";
		String path = file.getPath().replace(file.getName(), "");
		String xmlFileName = path + "matrix23single.xml";

		BufferedReader reader = new BufferedReader(new FileReader(xmlFileName));	
		
		//read all the lines from the file
		while ((fileLine = reader.readLine()) != null) 
		{
			xmlString+=fileLine;
	    }
	    reader.close();
	    
	    String expectedResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	    expectedResult += "<gte version=\"0.1\">\n";
	    expectedResult += "<display>\n";
		expectedResult += "<singlePayoff>true</singlePayoff>\n";
	    expectedResult += "</display>\n";
	    expectedResult += "<gameDescription/>\n";
		expectedResult += "<players>\n";
		expectedResult += "<player playerId=\"1\">I</player>\n";
		expectedResult += "<player playerId=\"2\">II</player>\n";
		expectedResult += "</players>\n";
		expectedResult += "<strategicForm size=\"{ 3 2 }\">\n";
		expectedResult += "<strategy player=\"I\">{ \"T\" \"M\" \"B\" }</strategy>\n";
		expectedResult += "<strategy player=\"II\">{ \"l\" \"r\" }</strategy>\n";
		expectedResult += "<payoffs player=\"I\">1/4 1 -8/-9\n";
		expectedResult += "5 2 1\n";
		expectedResult += "</payoffs>\n";
		expectedResult += "<payoffs player=\"II\">1/4 1 -8/-9\n";
		expectedResult += "5 2 1\n";
		expectedResult += "</payoffs>\n";
		expectedResult += "</strategicForm>\n";
		expectedResult += "</gte>\n";

	    
	    assertEquals("xml does not match", xmlString.replace("\n", ""), expectedResult.replace("\n", ""));
		
	}
	
	private File createTempFile(String filename, String content) throws IOException
	{
		//create efg file in temporary folder
		File file = folder.newFile(filename); 
		
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
	    out.write(content);
	    out.close();
	    
	    return file;
	}
}
