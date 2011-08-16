package lse.standalone;
/* author: K. Bletzer */
/* last updated August 15, 2011 */
import java.io.File;

/*
 * This class covers general testing of the six conversion classes.  
 * It assumes that each set of "starter" test files (for nfg, efg, and strategic
 * flat file conversion) are in a defined directory.  Any number of test files can 
 * be used.
 * 
 * The methods included in this class will allow the tester to exercise a simple
 * conversion, or a "round trip" conversion (e.g., efg to xml and back to efg).
 * 
 * The validation of the results of the conversion are done manually outside of this
 * class using other tools.
 */
public class GeneralTest 
{
	private String nfgFilePath = "< path to nfg files here >";
	private String efgFilePath = "< path to efg files here >";
	private String latexFilePath = "< path to strategic flat files here >";
	
	/* convert all of the .nfg files in the nfg folder from nfg format to xml */
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
	
	/* convert all of the .xml files in the nfg folder from xml to nfg.
	 * Since there are two types of nfg format this method will convert to 
	 * both differentiating the output based on the file name. */
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
	    		   
	    		  xtn.setFileSuffix("_out.nfg");  //format 1: outcome
	    		  xtn.setNFGFormat("outcome");
	    		  xtn.convertXMLToNFG();
	    		  
	    		  xtn.setFileSuffix("_pay.nfg");  //format 2: payoff
	    		  xtn.setNFGFormat("payoff");
	    		  xtn.convertXMLToNFG();
	    	  }
	      }
	    }
	}
	
	/* convert all of the .xml files in the efg folder from xml to efg */
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
	
	/* convert all of the .efg files in the efg folder from efg to xml */
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
	
	/* convert all of the strategic flat files in the flat file folder to xml */
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
	
	/* convert all xml files in the strategic flat file folder to LaTeX format */
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
	    		  XMLToLaTeX x = new XMLToLaTeX(filename);
	    		  x.convertXMLToLaTeX(); 
	    	  }
	      }
	    }
	}
	
	/* nfg to xml and back to _nfg.nfg */
	public void roundTripNFG()
	{
		this.allNFGToXML();
	    this.allXMLToNFG();
	}
	
	/* efg to xml and back to _efg.efg */
	public void roundTripEFG()
	{
		this.allEFGToXML();
		this.allXMLToEFG();
	}
	
	/* flat file to xml to .tex */
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
