package lse.standalone;
/* last updated August 13, 2011 */
import java.io.File;

public class GeneralTest 
{
	private String nfgFilePath = "<< fill in path to nfgToXML_test_files >>";
	private String efgFilePath = "<< fill in path to efgToXML_test_files >>";
	private String latexFilePath = "<< fill in path to strategicToXML_test_files >>";
	
	public void allNFGToXML()
	{
		File folder = new File(this.nfgFilePath);
	    File[] listOfFiles = folder.listFiles();

	    for (int i = 0; i < listOfFiles.length ; i++) 
	    {
	      if (listOfFiles[i].isFile()) 
	      {
	    	  String filename = listOfFiles[i].toString();
	    	  if (filename.endsWith(".nfg"))
	    	  {
	    		  System.out.println(listOfFiles[i].toString());
	    		  NFGToXML ntx = new NFGToXML(filename);
	    		 // ntx.setTestMode(true);
	    		 // ntx.setDTD("gte.dtd");
	    		  ntx.convertNFGToXML(); 
	    	  }
	       }
	    }
	}
	
	public void allXMLToNFG()
	{
		File folder = new File(this.nfgFilePath);
	    File[] listOfFiles = folder.listFiles();

	    for (int i = 0; i < listOfFiles.length ; i++) 
	    {
	      if (listOfFiles[i].isFile()) 
	      {
	    	  String filename = listOfFiles[i].toString();
	    	  if (filename.endsWith(".xml"))
	    	  {
	    		  System.out.println(listOfFiles[i].toString());
	    		  XMLToNFG xtn = new XMLToNFG(filename);
	    		  
	    		  xtn.setFileSuffix("_out.nfg");
	    		  xtn.setNFGFormat("outcome");
	    		  xtn.convertXMLToNFG();
	    		  
	    		  xtn.setFileSuffix("_pay.nfg");
	    		  xtn.setNFGFormat("payoff");
	    		  xtn.convertXMLToNFG();
	    	  }
	      }
	    }
	}
	
	public void allXMLToEFG()
	{
		File folder = new File(this.efgFilePath);
	    File[] listOfFiles = folder.listFiles();

	    for (int i = 0; i < listOfFiles.length ; i++) 
	    {
	      if (listOfFiles[i].isFile()) 
	      {
	    	  String filename = listOfFiles[i].toString();
	    	  if (filename.endsWith(".xml"))
	    	  {
	    		  System.out.println(listOfFiles[i].toString());
	    		  XMLToEFG xte = new XMLToEFG(filename);
	    		  xte.setFileSuffix("_efg.efg");
	    		  xte.convertXMLToEFG(); 
	    	  }
	      }
	    } 
	}
	
	public void allEFGToXML()
	{
		File folder = new File(this.efgFilePath);
	    File[] listOfFiles = folder.listFiles();

	    for (int i = 0; i < listOfFiles.length ; i++) 
	    {
	      if (listOfFiles[i].isFile()) 
	      {
	    	  String filename = listOfFiles[i].toString();
	    	  if (filename.endsWith(".efg"))
	    	  {
	    		  System.out.println(listOfFiles[i].toString());
	    		  EFGToXML etx = new EFGToXML(filename);
	    		  etx.setTestMode(true);
	    		  etx.setDTD("gte.dtd");
	    		  etx.convertEFGtoXML(); 
	    	  }
	      }
	    }
	}
	
	public void allStrategicFilesToXML()
	{
		File folder = new File(this.latexFilePath);
	    File[] listOfFiles = folder.listFiles();

	    for (int i = 0; i < listOfFiles.length ; i++) 
	    {
	      if (listOfFiles[i].isFile()) 
	      {
	    	  String filename = listOfFiles[i].toString();
	    	  if (filename.endsWith(".txt"))
	    	  {
	    		  System.out.println(listOfFiles[i].toString());
	    		  StrategicFileToXML x = new StrategicFileToXML(filename);
	    		  //etx.setTestMode(true);
	    		  //etx.setDTD("gte.dtd");
	    		  x.convertFlatFileToXML(); 
	    	  }
	      }
	    }
	}
	
	public void allXMLToLaTeX()
	{
		File folder = new File(this.latexFilePath);
	    File[] listOfFiles = folder.listFiles();

	    //for (int i = 0; i < listOfFiles.length ; i++) 
	    for (int i = 0; i < listOfFiles.length ; i++) 
	    {
	      if (listOfFiles[i].isFile()) 
	      {
	    	  String filename = listOfFiles[i].toString();
	    	  if (filename.endsWith(".xml"))
	    	  {
	    		  System.out.println(listOfFiles[i].toString());
	    		  String[] s = {};
	    		  XMLToLaTeX x = new XMLToLaTeX(filename, s);
	    		  x.convertXMLToLaTeX(); 
	    	  }
	      }
	    }
	}
	
	public void roundTripNFG()
	{
		this.allNFGToXML();
	    this.allXMLToNFG();
	}
	
	public void roundTripEFG()
	{
		this.allEFGToXML();
		this.allXMLToEFG();
	}
	
	public void flatToLaTeX()
	{
		this.allStrategicFilesToXML();
		this.allXMLToLaTeX();
	}
	
	public static void main (String [] args)
	{
		GeneralTest test = new GeneralTest();
		test.roundTripNFG();
		test.flatToLaTeX();
		test.roundTripEFG();
	}
	
}
