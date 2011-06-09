import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.IOException;

//invoke by java matrixFileToLaTeX inputfile.txt
public class matrixFileToLaTeX 
{
	public matrixFileToLaTeX(String fn)
	{
		filename = fn;
		splitMatrices(readFile());
	} 
	public ArrayList<ArrayList<String>> a1;
	public ArrayList<ArrayList<String>> a2;
	private String filename;
	
	//reads the input file 
	//outputs an ArrayList containing the list of lines (strings) that make up the file
	public ArrayList<String> readFile() 
	{
		ArrayList<String> lines = new ArrayList();
		String line;
		
		try
        {
			BufferedReader reader = new BufferedReader(new FileReader(filename));	
			
			//read all the lines from the file
			while ((line = reader.readLine()) != null) 
			{
				lines.add(line);
		    }
		    reader.close();
        }
		catch (Exception e)
		{
			//raise error regarding unsuccessfully reading the file
		}
		
		return lines;
	}
	
	//there may be either one or two matrices in the input text
	//if one, duplicate to make 2 symmetric matrices
	//otherwise output two as expected
	public void splitMatrices(ArrayList<String> lines)
	{
		a1 = new ArrayList();
		a2 = new ArrayList();
		boolean bSwitch = false;
		
		//loop through all the lines
		//putting them either into lists a1 or a2
		for (Iterator<String> i = lines.iterator(); i.hasNext();) 
		{
			String line = (String)i.next();
			
			if (bSwitch) 
			{
				a2.add(this.lineToArray(line));
			}
			else if (line.trim().length() == 0)  //check for blank dividing line
			{
				bSwitch = true;
			}
			else
			{
				a1.add(this.lineToArray(line));
			}
		}
		
		if (a2.size() < 1)
		{
			//copy array 1 to array 2 since there was no array 2 in the file
			//clone is a shallow copy; don't need deep copy here
			a2 = (ArrayList<ArrayList<String>>)a1.clone();
		
		}
	}
	
	//take a line of text, tokenize, and return as ArrayList
	public ArrayList<String> lineToArray(String line)
	{
		StringTokenizer st = new StringTokenizer(line);
		ArrayList<String> row = new ArrayList<String>();
		
  	    while (st.hasMoreTokens() ) 
  	    {	
  	    	row.add(st.nextToken());
  	    } 
  	    return row;
	}
	
	//format a1 into a LaTeX friendly string
	public String formatMatrix()
	{
		//determine number of columns
		ArrayList<String> a1row;
		ArrayList<String> a2row;
		
		int numCols = a1.get(0).size();
		String cols = "|";
		
		for(int i = 0;i < numCols; i++) 
		{
			cols = cols+"r|";
		}
		
		String s = "\\documentclass{article} \n\\begin{document} \n";
		
		
		s = s+ "\\begin{tabular}{" + cols + "} \n\\hline \n";
		
		
		for(int i=0; i < a1.size(); i++)  //iterate rows
		{	
			a1row = a1.get(i);
			a2row = a2.get(i);
			for(int j=0; j < a1row.size(); j++)  //iterate rows
			{
				if (j == (a1row.size() - 1))
				{
					s = s + a1row.get(j) + "," + a2row.get(j) + " \\\\ \\hline \n" ;
				}
				else
				{
					s = s + a1row.get(j) + "," + a2row.get(j) +  " & " ;
				}
			}
		}
		
		
		s = s+ "\\end{tabular} \n\\end{document} \n";
		return s;
	}
	
	public String printMatrix() 
	{
		return this.formatMatrix();
	}
	
	public void writeMatrixToFile()
	{
		try 
		{
		    BufferedWriter out = new BufferedWriter(new FileWriter("mat3.tex"));
		    out.write(""+ this.printMatrix());
		    out.close();
		} 
		catch (IOException e) 
		{
		}
	}

	public static void main (String [] args)
	{	
		String filename = args[0];
		matrixFileToLaTeX ml = new matrixFileToLaTeX(filename);
		//System.out.println(ml.a1.toString());
		System.out.println(ml.printMatrix());
		ml.writeMatrixToFile();
	}
	
}
