package informationRetrieval;

import java.io.*;

public class MyFileWriter {
	File currentFile;
	PrintWriter out;
	
	public MyFileWriter(String filePath) throws IOException {
		currentFile = new File(filePath);
		FileWriter fw = new FileWriter(filePath, true);
	    BufferedWriter bw = new BufferedWriter(fw);
	    out = new PrintWriter(bw);
	}
	
	public void println(String text) {
		out.println(text);
	}
	
	public void print(String text) {
		out.print(text);
	}
	
	public void close() {
		out.close();	
	}
}
