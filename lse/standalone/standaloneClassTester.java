/* Created 13 June 2011 */
package lse.standalone;

public class standaloneClassTester 
{
	public static void main (String [] args)
	{	
		// create formatted output via file
		String[] filename = { "matrix22.txt", "matrix23.txt", "matrix34.txt", "matrix33.txt", "matrix33single.txt" };
		
		for (int i = 0; i < filename.length; i++)
		{
			normalFormToLaTeX ml = new normalFormToLaTeX(filename[i]);
			ml.createLaTeXFile(); 
		} 
		
		//create formatted output via parameter passing
		String[][] p1_22 = {{"2", "3"}, {"1", "1/2"}};
		String[][] p1_23 = {{"2", "3", "0"}, {"1", "1/2", "-2/3"}};
		String[][] p1_33 = {{"2", "3", "1"}, {"9/3", "-3", "5"},{"1", "1/2", "2"}};
		String[][] p1_34 = {{"2", "3", "1", "4"}, {"9/3", "-3", "5", "2"},{"1", "1/2", "2", "-3/4"}};
		String[][] p1_44 = {{"2", "3", "1", "4"}, {"9/3", "-3", "5", "2"},{"1", "1/2", "2", "-3/4"},{"1", "3", "2", "4"}};
		
		String[][] p2_22 = {{"1", "3"}, {"1", "1/2"}};
		String[][] p2_23 = {{"1", "3", "-11"}, {"-1", "1/2", "2/3"}};
		String[][] p2_33 = {{"1", "3", "1"}, {"9/3", "-3", "4"},{"-1", "1/2", "2"}};
		String[][] p2_34 = {{"1", "3", "5", "4"}, {"9/3", "-3", "-5", "2"},{"1", "-1/2", "2", "-3/4"}};
		String[][] p2_44 = {{"1", "3", "5", "4"}, {"9/3", "-3", "-5", "2"},{"1", "-1/2", "2", "-3/4"},{"4", "1/2", "2", "3"}};
		
		//no fraction formatting & display best response
		normalFormToLaTeX ml22 = new normalFormToLaTeX(p1_22, p2_22, false, true); 
		//nice fraction formatting & display best response
		normalFormToLaTeX ml23 = new normalFormToLaTeX(p1_23, p2_23, true, true);
		//nice fraction formatting, don't  display best response
		normalFormToLaTeX ml33 = new normalFormToLaTeX(p1_33, p2_33, true, false);
		normalFormToLaTeX ml34 = new normalFormToLaTeX(p1_34, p2_34); 
		normalFormToLaTeX ml44 = new normalFormToLaTeX(p1_44, p2_44, false, false); 
		
		normalFormToLaTeX ml22single = new normalFormToLaTeX(p1_22, null, false, true); 
		//nice fraction formatting & display best response
		normalFormToLaTeX ml23single = new normalFormToLaTeX(p1_23, null, true, true);
		//nice fraction formatting, don't  display best response
		normalFormToLaTeX ml33single = new normalFormToLaTeX(p1_33, null, true, false);
		normalFormToLaTeX ml34single = new normalFormToLaTeX(p1_34, null); 
		normalFormToLaTeX ml44single = new normalFormToLaTeX(p1_44, null, false, false); 
		
		ml22.createLaTeXFile("s22.tex");
		ml23.createLaTeXFile("s23.tex");
		ml33.createLaTeXFile("s33.tex");
		ml34.createLaTeXFile("s34.tex");
		ml44.createLaTeXFile("s44.tex");
		
		ml22single.createLaTeXFile("s22single.tex");
		ml23single.createLaTeXFile("s23single.tex");
		ml33single.createLaTeXFile("s33single.tex");
		ml34single.createLaTeXFile("s34single.tex");
		ml44single.createLaTeXFile("s44single.tex");
	}
}
