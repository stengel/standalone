/* Last updated July 17, 2011 */
package lse.standalone;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ConversionUtilities 
{
	public ArrayList<String> extractTokens(String s)
	{
		//either the first token in the string is a quoted string
		// or it is a value offset by spaces
		ArrayList<String> tokenList = new ArrayList<String>();
		String token; 
		s = s.trim();
		
		int beginTkn, endTkn;
		beginTkn = 0;
		boolean stringToken = false;
		
		while (s.length() > 0)
		{
			stringToken = false;
			if (s.charAt(beginTkn) == '\"')
			{
				endTkn = s.indexOf("\"", beginTkn + 1) + 1;
				stringToken = true;
			}
			else
			{
				endTkn = s.indexOf(" ", beginTkn + 1);
			}
			
			if (endTkn < 0) { endTkn = s.length();  }
			
			token = this.removeQuoteMarks(s.substring(beginTkn, endTkn));
			//token = s.substring(beginTkn, endTkn);
			
			if (stringToken || token.trim().length() > 0)
			{
				tokenList.add(token);  //add any token if enclosed in "" marks, even if blank
			}

			s = s.substring(endTkn).trim();
		}
		
		return tokenList;
	}
	
	private String removeQuoteMarks(String q)
	{
		Pattern pat = Pattern.compile("\"");
		Matcher m = pat.matcher(q);

        return m.replaceAll("");
	}
	
	public void printStringArray(String[] s)
	{
		for(int i =0; i < s.length; i++)
		{
			System.out.println(i+" - |" + s[i] +"| ");
		}
	} 
	
	public String arrayListToBracketList(ArrayList<String> al)
	{
		String s = al.toString();
		s = s.replace("]", "");
		s = s.replace("[", "");
		s = s.replace(",", "");
		return "{ " + s + " }";
	}
	
	public void createFile(String name, String contents)
	{
		try 
		{
		    BufferedWriter out = new BufferedWriter(new FileWriter(name));
		    out.write(contents);
		    out.close();
		} 
		catch (IOException e) 
		{
			//throw exception if can't create file
		}
	}
	
	/* note - from function of same name in ExtensiveFormXMLReader */
	public String getAttribute(Node elem, String key)
	{
		String value = null;
		if (elem != null && elem.getAttributes().getNamedItem(key) != null) 
		{
			value = elem.getAttributes().getNamedItem(key).getNodeValue();
		}
		return value;
	}
	
	public Document fileToXML(String filename)
	{
		Document doc = null;
		try 
		{
			File file = new File(filename);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

			DocumentBuilder db;
			db = dbf.newDocumentBuilder();
			doc = db.parse(file);
		} 
		catch (Exception e) 
		{
			System.out.println("fileToXML: " + e.toString());
		}
		return doc;
		
	}
}
