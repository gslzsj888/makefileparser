
package myparse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import test.Test;

public class MakefileStandarded {
	public int Standardlevel = 0;
	// 0精确匹配，1模糊匹配
	static String inFile = "in.txt";
	static String inFile1 ="part.txt";
	static String inFile2 ="linux-makefile.txt";
	public static final String ENTERWindows = "\r\n";
	public static final String ENTERLinux = "\n";
	private Vector<Word> lexical;
	private Vector<Word> fuzzy0;
	private Vector<Word> fuzzy1;
	private Vector<block> blocks;
	private boolean scan0 = true;
	private boolean scan1 = true;
	private boolean debuglex = true;
	private boolean debug = false;
	public MakefileStandarded() {
		lexical = new Vector<Word>();
		fuzzy0 = new Vector<Word>();
		fuzzy1= new Vector<Word>();
		blocks = new Vector<block>();
	}

	public static void main(String[] args) throws IOException {
		
		MakefileStandarded m = new MakefileStandarded();
		m.scan(inFile);
	}

	public void scan(String filename) throws IOException {
		FileInputStream is = new FileInputStream(filename);
		InputStreamReader in = new InputStreamReader(is, "iso-8859-1");
		//in = new InputStreamReader(Test.class.getResourceAsStream("/resource/Makefile.in"));
		BufferedReader rl = new BufferedReader(in);
		lexical = new Vector<Word>();
		scann(rl);
		if(debuglex){
			BufferedWriter writer = null;
			String outFile = filename + "_Vector";
			FileWriter fw = null;
			try {
				fw = new FileWriter(outFile);
				writer = new BufferedWriter(fw);
				for (int i = 0; i < lexical.size(); i++) {
					writer.write("(wordName:"+lexical.elementAt(i).getWordName()
							+" type:"+lexical.elementAt(i).getType()
							+" line:"+lexical.elementAt(i).getLine()
							+" col:"+lexical.elementAt(i).getColumn()+")");
					writer.newLine();
				}
				writer.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					writer.close();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if(scan0){// 去注释，空行
			BufferedWriter writer = null;
			String outFile = filename + "_Standard_0";
			FileWriter fw = null;
			try {
				fw = new FileWriter(outFile);
				writer = new BufferedWriter(fw);
				for (int i = 0; i < lexical.size(); i++) {
					if ("Comment".equals(lexical.elementAt(i).getType())) {
						i++;
						continue;
					} else if(";".equals(lexical.elementAt(i).getWordName())) {
						int j=i+1;
						int line = lexical.elementAt(i).getLine();
						boolean flag=false;
						while(j<lexical.size() && lexical.elementAt(j).getLine()==line){
							if(!ENTERLinux.equals(lexical.elementAt(j).getWordName())
							&& !" ".equals(lexical.elementAt(j).getWordName())
							&& !"\t".equals(lexical.elementAt(j).getWordName())){
								flag=true;
								break;
							}
							j++;
						}
						if(flag){
							fuzzy0.addElement(lexical.elementAt(i));
							Word newWord = new Word("Separator", ENTERLinux, lexical.elementAt(i).getLine(), lexical.elementAt(i).getColumn());
							fuzzy0.addElement(newWord);
						}
						else
							fuzzy0.addElement(lexical.elementAt(i));
					}
					else{
						fuzzy0.addElement(lexical.elementAt(i));
					}
				}
				for (int i = 0; i < fuzzy0.size(); i++) {
					if(ENTERLinux.equals(fuzzy0.elementAt(i).getWordName())){
						if(i==0 || i==fuzzy0.size()-1 || ENTERLinux.equals(fuzzy0.elementAt(i-1).getWordName())){
							continue;
						}
					}
					writer.write(fuzzy0.elementAt(i).getWordName());
				}
				writer.flush();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					writer.close();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		if(scan1){
			rules();
			BufferedWriter writer = null;
			String outFile = filename + "_Standard_1";
			FileWriter fw = null;
			try {
				fw = new FileWriter(outFile);
				writer = new BufferedWriter(fw);
				for (int i = 0; i < fuzzy1.size(); i++) {
					if(ENTERLinux.equals(fuzzy1.elementAt(i).getWordName())){
						if(i==0 || i==fuzzy1.size()-1 || ENTERLinux.equals(fuzzy1.elementAt(i-1).getWordName())){
							continue;
						}
					}
					writer.write(fuzzy1.elementAt(i).getWordName());
				}
				writer.flush();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					writer.close();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		in.close(); // 关闭文件读入类
	}
	
	public void chunkBlocks(){
		/*按结构分块
		 * target
		 * ifdef
		 * ifeq
		 * export
		 * define
		 */
		final int TargetState=1;
		final int IfeqState=3;
		final int IfdefState=2;
		final int ExportState=4;
		final int DefineState=5;
		final int OtherState=0;
		int blockLen=blocks.size();
		int line = 0;
		int column = 0;
		int state=OtherState;
		for (int i = 0; i < fuzzy1.size(); i++) {
			line=fuzzy1.elementAt(i).getLine();
			column=fuzzy1.elementAt(i).getColumn();
			if("target".equals(fuzzy1.elementAt(i).getWordName())){
				state=TargetState;
				int line2=line;
				for(int j=i+1; j<fuzzy1.size(); j++){
					if("\t".equals(fuzzy1.elementAt(i).getWordName())){
						;
					}
					else{
						state=OtherState;
					}
				}
			}else if ("ifeq".equals(fuzzy1.elementAt(i).getWordName())
					|| "ifneq".equals(fuzzy1.elementAt(i).getWordName())) {
				state =IfeqState;
				//block(int start, int end, Vector<Word> from, int type)
				fuzzy1.addElement(fuzzy0.elementAt(i));
			}else if ("ifdef".equals(fuzzy1.elementAt(i).getWordName())
					|| "ifndef".equals(fuzzy1.elementAt(i).getWordName())) {
				state =IfdefState;
				
				fuzzy1.addElement(fuzzy0.elementAt(i));
			}else if ("define".equals(fuzzy1.elementAt(i).getWordName())
					|| "undefine".equals(fuzzy1.elementAt(i).getWordName())) {
				state =DefineState; 
				
				fuzzy1.addElement(fuzzy0.elementAt(i));
			}else if ("export".equals(fuzzy1.elementAt(i).getWordName())
					|| "unexport".equals(fuzzy1.elementAt(i).getWordName())) {
				state =ExportState;
				
			}else{
				state=OtherState;
			}
		}
		
	}
	
	
	public void rules(){//按规则模糊
		/*模糊的规则有(优先级从小到大)
		 * 1.string模糊--string(只支持")
		 * 2.target模糊--target
		 * 3.自定义函数模糊--function
		 * 4.makefile变量模糊--var shellvar
		 * 5.参数 -- pere
		 */
		final int STRING=1;
		final int OTHER=0;
		int state=0;
		int line=0;
		int column=0;
		Word firstOfLine = null;
		for (int i=0; i<fuzzy0.size();i++){
			if(fuzzy0.elementAt(i).getColumn()==0)
				firstOfLine=fuzzy0.elementAt(i);
			if ("Assignment".equals(fuzzy0.elementAt(i).getType())) {
				for(int i1=i-1; i1>=0 && !"Separator".equals(fuzzy0.elementAt(i1).getType()); i1--){
					if("id".equals(fuzzy0.elementAt(i1).getType())){
						fuzzy0.elementAt(i1).setWordName("var");;
						break;
					}
				}
			} else if ("$(".equals(fuzzy0.elementAt(i).getWordName())
					|| "${".equals(fuzzy0.elementAt(i).getWordName())
					|| "$$".equals(fuzzy0.elementAt(i).getWordName())
					|| "$".equals(fuzzy0.elementAt(i).getWordName())) {
				for(int i1=i+1; i1<fuzzy0.size() && !"Separator".equals(fuzzy0.elementAt(i1).getType()); i1++){
					if("keyword".equals(fuzzy0.elementAt(i1).getType())){
						if(!"call".equals(fuzzy0.elementAt(i1).getWordName())){
									break;
						}
					}
					if("id".equals(fuzzy0.elementAt(i1).getType())){
						if (")".equals(fuzzy0.elementAt(i1 + 1).getWordName())
						|| "}".equals(fuzzy0.elementAt(i1 + 1).getWordName())
						|| ":".equals(fuzzy0.elementAt(i1 + 1).getWordName())){
							if("$$".equals(fuzzy0.elementAt(i).getWordName())){
								fuzzy0.elementAt(i1).setWordName("shellvar");	
							}
							else{
								fuzzy0.elementAt(i1).setWordName("var");
							}
						}else{
							fuzzy0.elementAt(i1).setWordName("function");
						}
						break;
					}
				}
			} else if("define".equals(fuzzy0.elementAt(i).getWordName())
					|| "ifdef".equals(fuzzy0.elementAt(i).getWordName())
					|| "ifndef".equals(fuzzy0.elementAt(i).getWordName())){
				for(int i1=i+1; i1<fuzzy0.size() && !"Separator".equals(fuzzy0.elementAt(i1).getType()); i1++){
					if("id".equals(fuzzy0.elementAt(i1).getType())){
						fuzzy0.elementAt(i1).setWordName("var");
						break;
					}
				}
			}else if("id".equals(fuzzy0.elementAt(i).getType())){
				if('-'==fuzzy0.elementAt(i).getWordName().charAt(0)){
					if(firstOfLine!=null && "Tabspace".equals(fuzzy0.elementAt(i).getType()))
						fuzzy0.elementAt(i).setWordName("pere");
				}
			}
		}
		for (int i = 0; i < fuzzy0.size(); i++) {
			line=fuzzy0.elementAt(i).getLine();
			column=fuzzy0.elementAt(i).getColumn();
			if("\"".equals(fuzzy0.elementAt(i).getWordName())){//string
				if(state!=STRING)
					state=STRING;
				else{
					state=OTHER;
					Word newWord = new Word("String", "String", line, column);
					fuzzy1.addElement(newWord);
				}
			}
			else if (":".equals(fuzzy0.elementAt(i).getWordName())
					|| "::".equals(fuzzy0.elementAt(i).getWordName())) {//target
				if(state==STRING)
					continue;
				int j=i-1;
				String tmp="";
				while (j>=0 && fuzzy0.elementAt(j).getLine()==line && !"Separator".equals(fuzzy0.elementAt(j).getType())) {
					tmp+=fuzzy0.elementAt(j).getType();
					j--;
				}
				String[] tmp1=tmp.split("[ \t]+");
				int fuzzy1len=fuzzy1.size();
				int lay=0;
				for(int i1=fuzzy1len-1; i1>=0 && !"Separator".equals(fuzzy1.elementAt(i1).getType()); i1--){
					if(fuzzy1.elementAt(i1).getLine()==line){
						if(")".equals(fuzzy1.elementAt(i1).getWordName())
						||"}".equals(fuzzy1.elementAt(i1).getWordName())){
							lay++;
						}
						else if("$(".equals(fuzzy1.elementAt(i1).getWordName())
						||"${".equals(fuzzy1.elementAt(i1).getWordName())
						||"(".equals(fuzzy1.elementAt(i1).getWordName())
						||"{".equals(fuzzy1.elementAt(i1).getWordName())){
							lay--;
							if(lay<0)
								break;
						}	
						fuzzy1.remove(i1);
					}
				}
				int len1=tmp1.length;
				for(int i2=0; i2<len1; i2++){
					if(tmp1[i2].length()>0 && !"\t".equals(tmp1[i2]) && !" ".equals(tmp1[i2])){
						Word newWord = new Word("target", "target", line, column);
						fuzzy1.addElement(newWord);
					}
				}
				fuzzy1.addElement(fuzzy0.elementAt(i));
			}  else{
				if(state==STRING)
					continue;
				fuzzy1.addElement(fuzzy0.elementAt(i));
			}
		}
	}

	public void scann(BufferedReader rl) throws IOException {
		int line = 0;
		String S;
		char ch;
		MyMap map = new MyMap();
		MyMap.init();
		boolean commentContinue = false;
		int commentLines = 0;
		StringBuffer temp1 = new StringBuffer("");
		while ((S = rl.readLine()) != null) {
			line++;
			int sLong = S.length();
			if (debug)
				System.out.println(line + " length=" + sLong);
			int i = 0;
			for (i = 0; i < sLong; i++) {
				ch = S.charAt(i);
				if (commentContinue) {
					commentLines++;
					while (i < sLong) {
						ch = S.charAt(i);
						if (ch != '\n' && ch != '\r' && ch != '\0' && ch != '\\') {
							temp1.append(ch);
						} else {
							break;
						}
						i++;
					}
					if (ch != '\\') {
						commentContinue = false;
					}
					if (!commentContinue) {
						createNode("Comment", temp1.toString().trim(), line, commentLines);
						commentLines = 0;
					}
				} else {

					// 以字母或者下划线开头或者数字开头,处理关键字或者标识符
					if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_'
							|| ch == '.' || ch == '-') {
						int start = i;
						StringBuffer temp11 = new StringBuffer("");
						while (i < sLong) {
							ch = S.charAt(i);
							if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')
									|| ch == '_' || ch == '.' || ch == '-') {
								temp11.append(ch);
							} else {
								break;
							}
							i++;
							if (debug)
								System.out.println(i + " " + temp11.toString().trim());
						}
						i--;
						int seekTemp = seekKey(map.keyWords, temp11.toString().trim());// 返回相应的key
						if (seekTemp != -1)// 关键字
						{
							createNode("keyword", temp11.toString().trim(), line, start);
						} else// 自定义变量或自定义函数
						{
							createNode("id", temp11.toString().trim(), line, start);
						}
					} else if (ch == '$') {
						int start = i;
						StringBuffer temp11 = new StringBuffer("");
						temp11.append(ch);
						ch = S.charAt(++i);
						if (ch != '(' && ch != '{') {
							if (ch == '$') {
								createNode("$$", "$$", line, start);
							} else {
								while (i < sLong) {
									ch = S.charAt(i);
									if (ch != ' ' && ch != '\n' && ch != '\r' && ch != '\0' && ch != '\\' && ch != ' '
											&& ch != '\t' && ch != ')' && ch != '}') {
										temp11.append(ch);
									} else {
										break;
									}
									i++;
									if (debug)
										System.out.println("in $" + i + " " + temp11.toString().trim());
								}
								int seekTemp = seekKey(map.keyWords, temp11.toString().trim());// 返回相应的key
								if (seekTemp != -1)// 自动变量
								{
									createNode("AutoVar", temp11.toString().trim(), line, start);
									i--;
								} else {
									createNode("$", "$", line, start);
									i = start;
								}
							}
						} else {
							if (ch == '(')
								createNode("$(", "$(", line, start);
							else
								createNode("${", "${", line, start);
						}
					} else if (ch == '#') {
						int start = i;
						commentLines = 1;
						temp1 = new StringBuffer("");
						while (i < sLong) {
							ch = S.charAt(i);
							if (ch != '\n' && ch != '\r' && ch != '\0' && ch != '\\') {
								temp1.append(ch);
							} else {
								break;
							}
							i++;

						}
						if (ch == '\\') {
							commentContinue = true;
						}
						if (!commentContinue) {
							createNode("Comment", temp1.toString().trim(), line, commentLines);
							commentLines = 0;
						}
					} else if (ch == '?')// 处理?开头的运算符
					{
						if(i< sLong-1){
							ch = S.charAt(++i);
							if (ch == '=')
								createNode("Assignment", "?=", line, i - 1);
							else {
								createNode("?", "?", line, i - 1);
								i--;
							}
						}
						else {
							createNode("?", "?", line, i);
						}
					} else if (ch == ':')// 处理:开头的运算符
					{
						if(i< sLong-1){
							ch = S.charAt(++i);
							if (ch == '=')
								createNode("Assignment", ":=", line, i - 1);
							else if (ch == ':')
								createNode("Separator", "::", line, i - 1);//双冒号规则
							else {
								createNode("Separator", ":", line, i - 1);
								i--;
							}
						}
						else{
							createNode("Separator", ":", line, i);//尾端的冒号
						}
						
					} else if (ch == '+')// 处理+开头的运算符
					{
						if(i< sLong-1){
							ch = S.charAt(++i);
							if (ch == '=')
								createNode("Assignment", "+=", line, i - 1);
							else {
								createNode("op", "+", line, i - 1);
								i--;
							}
						}
						else{
							createNode("op", "+", line, i);
						}
					} else if (ch == '=')
						createNode("Assignment", "=", line, i);
					else if (ch == ';'){
						createNode("nextline", ";", line, i);
					}
					else if(ch =='\\'){//省略\
						continue;
					}
					else if (ch == ' ')
						createNode("Whitespace", " ", line, i);
					else if (ch == '\t')
						createNode("Tabspace", "\t", line, i);
					else
						createNode(String.valueOf(ch), String.valueOf(ch), line, i);
				}
				
			}
			createNode("Separator", ENTERLinux, line, i);

		}

	}

	public int createNode(String type, String content, int line, int col) {
		Word newWord = new Word(type, content, line, col);
		lexical.addElement(newWord);
		return lexical.size();
	}

	public int seekKey(Map<Integer, String> keyWords, String value) {
		for (Map.Entry<Integer, String> k : keyWords.entrySet()) {
			if (k.getValue().equals(value))
				return k.getKey();

		}
		return -1;
	}

}

class MyMap {
	static Map<Integer, String>  keyWords = new HashMap<Integer, String>();
	public static void init() {
		// shell命令
		keyWords.put(10000,"cat");
		keyWords.put(10000,"chattr");
		keyWords.put(10000,"chgrp");
		keyWords.put(10000,"chmod");
		keyWords.put(10000,"chown");
		keyWords.put(10000,"cksum");
		keyWords.put(10000,"cmp");
		keyWords.put(10000,"diff");
		keyWords.put(10000,"diffstat");
		keyWords.put(10000,"file");
		keyWords.put(10000,"find");
		keyWords.put(10000,"git");
		keyWords.put(10000,"gitview");
		keyWords.put(10000,"indent");
		keyWords.put(10000,"cut");
		keyWords.put(10000,"ln");
		keyWords.put(10000,"less");
		keyWords.put(10000,"locate");
		keyWords.put(10000,"lsattr");
		keyWords.put(10000,"mattrib");
		keyWords.put(10000,"mc");
		keyWords.put(10000,"mdel");
		keyWords.put(10000,"mdir");
		keyWords.put(10000,"mktemp");
		keyWords.put(10000,"mmove");
		keyWords.put(10000,"mread");
		keyWords.put(10000,"mren");
		keyWords.put(10000,"mtools");
		keyWords.put(10000,"mtoolstest");
		keyWords.put(10000,"mv");
		keyWords.put(10000,"od");
		keyWords.put(10000,"paste");
		keyWords.put(10000,"patch");
		keyWords.put(10000,"rcp");
		keyWords.put(10000,"rm");
		keyWords.put(10000,"slocate");
		keyWords.put(10000,"split");
		keyWords.put(10000,"tee");
		keyWords.put(10000,"tmpwatch");
		keyWords.put(10000,"touch");
		keyWords.put(10000,"umask");
		keyWords.put(10000,"which");
		keyWords.put(10000,"cp");
		keyWords.put(10000,"whereis");
		keyWords.put(10000,"mcopy");
		keyWords.put(10000,"mshowfat");
		keyWords.put(10000,"rhmask");
		keyWords.put(10000,"scp");
		keyWords.put(10000,"awk");
		keyWords.put(10000,"read");
		keyWords.put(10000,"updatedb");
		keyWords.put(10000,"col");
		keyWords.put(10000,"colrm");
		keyWords.put(10000,"comm");
		keyWords.put(10000,"csplit");
		keyWords.put(10000,"ed");
		keyWords.put(10000,"egrep");
		keyWords.put(10000,"ex");
		keyWords.put(10000,"fgrep");
		keyWords.put(10000,"fmt");
		keyWords.put(10000,"fold");
		keyWords.put(10000,"grep");
		keyWords.put(10000,"ispell");
		keyWords.put(10000,"jed");
		keyWords.put(10000,"joe");
		keyWords.put(10000,"join");
		keyWords.put(10000,"look");
		keyWords.put(10000,"mtype");
		keyWords.put(10000,"pico");
		keyWords.put(10000,"rgrep");
		keyWords.put(10000,"sed");
		keyWords.put(10000,"sort");
		keyWords.put(10000,"spell");
		keyWords.put(10000,"tr");
		keyWords.put(10000,"expr");
		keyWords.put(10000,"uniq");
		keyWords.put(10000,"wc");
		keyWords.put(10000,"let");;
		keyWords.put(10000,"lprm");
		keyWords.put(10000,"lpr");
		keyWords.put(10000,"lpq");
		keyWords.put(10000,"lpd");
		keyWords.put(10000,"bye");
		keyWords.put(10000,"ftp");
		keyWords.put(10000,"uuto");
		keyWords.put(10000,"uupick");
		keyWords.put(10000,"uucp");
		keyWords.put(10000,"uucico");
		keyWords.put(10000,"tftp");
		keyWords.put(10000,"ncftp");
		keyWords.put(10000,"ftpshut");
		keyWords.put(10000,"ftpwho");
		keyWords.put(10000,"ftpcount");
		keyWords.put(10000,"cd");
		keyWords.put(10000,"df");
		keyWords.put(10000,"dirs");
		keyWords.put(10000,"du");
		keyWords.put(10000,"edquota");
		keyWords.put(10000,"eject");
		keyWords.put(10000,"mcd");
		keyWords.put(10000,"mdeltree");
		keyWords.put(10000,"mdu");
		keyWords.put(10000,"mkdir");
		keyWords.put(10000,"mlabel");
		keyWords.put(10000,"mmd");
		keyWords.put(10000,"mrd");
		keyWords.put(10000,"mzip");
		keyWords.put(10000,"pwd");
		keyWords.put(10000,"quota");
		keyWords.put(10000,"mount");
		keyWords.put(10000,"mmount");
		keyWords.put(10000,"rmdir");
		keyWords.put(10000,"rmt");
		keyWords.put(10000,"stat");
		keyWords.put(10000,"tree");
		keyWords.put(10000,"umount");
		keyWords.put(10000,"ls");
		keyWords.put(10000,"quotacheck");
		keyWords.put(10000,"quotaoff");
		keyWords.put(10000,"lndir");
		keyWords.put(10000,"repquota");
		keyWords.put(10000,"quotaon");
		keyWords.put(10000,"badblocks");
		keyWords.put(10000,"cfdisk");
		keyWords.put(10000,"dd");
		keyWords.put(10000,"e2fsck");
		keyWords.put(10000,"ext2ed");
		keyWords.put(10000,"fsck");
		keyWords.put(10000,"fsck.minix");
		keyWords.put(10000,"fsconf");
		keyWords.put(10000,"fdformat");
		keyWords.put(10000,"hdparm");
		keyWords.put(10000,"mformat");
		keyWords.put(10000,"mkbootdisk");
		keyWords.put(10000,"mkdosfs");
		keyWords.put(10000,"mke2fs");
		keyWords.put(10000,"mkfs.ext2");
		keyWords.put(10000,"mkfs.msdos");
		keyWords.put(10000,"mkinitrd");
		keyWords.put(10000,"mkisofs");
		keyWords.put(10000,"mkswap");
		keyWords.put(10000,"mpartition");
		keyWords.put(10000,"swapon");
		keyWords.put(10000,"symlinks");
		keyWords.put(10000,"sync");
		keyWords.put(10000,"mbadblocks");
		keyWords.put(10000,"mkfs.minix");
		keyWords.put(10000,"fsck.ext2");
		keyWords.put(10000,"fdisk");
		keyWords.put(10000,"losetup");
		keyWords.put(10000,"mkfs");
		keyWords.put(10000,"sfdisk");
		keyWords.put(10000,"swapoff");
		keyWords.put(10000,"apachectl");
		keyWords.put(10000,"arpwatch");
		keyWords.put(10000,"dip");
		keyWords.put(10000,"getty");
		keyWords.put(10000,"mingetty");
		keyWords.put(10000,"uux");
		keyWords.put(10000,"telnet");
		keyWords.put(10000,"uulog");
		keyWords.put(10000,"uustat");
		keyWords.put(10000,"ppp-off");
		keyWords.put(10000,"netconfig");
		keyWords.put(10000,"nc");
		keyWords.put(10000,"httpd");
		keyWords.put(10000,"ifconfig");
		keyWords.put(10000,"minicom");
		keyWords.put(10000,"mesg");
		keyWords.put(10000,"dnsconf");
		keyWords.put(10000,"wall");
		keyWords.put(10000,"netstat");
		keyWords.put(10000,"ping");
		keyWords.put(10000,"pppstats");
		keyWords.put(10000,"samba");
		keyWords.put(10000,"setserial");
		keyWords.put(10000,"talk");
		keyWords.put(10000,"traceroute");
		keyWords.put(10000,"tty");
		keyWords.put(10000,"newaliases");
		keyWords.put(10000,"uuname");
		keyWords.put(10000,"netconf");
		keyWords.put(10000,"write");
		keyWords.put(10000,"statserial");
		keyWords.put(10000,"efax");
		keyWords.put(10000,"pppsetup");
		keyWords.put(10000,"tcpdump");
		keyWords.put(10000,"ytalk");
		keyWords.put(10000,"cu");
		keyWords.put(10000,"smbd");
		keyWords.put(10000,"testparm");
		keyWords.put(10000,"smbclient");
		keyWords.put(10000,"shapecfg");
		keyWords.put(10000,"adduser");
		keyWords.put(10000,"chfn");
		keyWords.put(10000,"useradd");
		keyWords.put(10000,"date");
		keyWords.put(10000,"exit");
		keyWords.put(10000,"finger");
		keyWords.put(10000,"fwhios");
		keyWords.put(10000,"sleep");
		keyWords.put(10000,"suspend");
		keyWords.put(10000,"groupdel");
		keyWords.put(10000,"groupmod");
		keyWords.put(10000,"halt");
		keyWords.put(10000,"kill");
		keyWords.put(10000,"last");
		keyWords.put(10000,"lastb");
		keyWords.put(10000,"login");
		keyWords.put(10000,"logname");
		keyWords.put(10000,"logout");
		keyWords.put(10000,"ps");
		keyWords.put(10000,"nice");
		keyWords.put(10000,"procinfo");
		keyWords.put(10000,"top");
		keyWords.put(10000,"pstree");
		keyWords.put(10000,"reboot");
		keyWords.put(10000,"rlogin");
		keyWords.put(10000,"rsh");
		keyWords.put(10000,"sliplogin");
		keyWords.put(10000,"screen");
		keyWords.put(10000,"shutdown");
		keyWords.put(10000,"rwho");
		keyWords.put(10000,"sudo");
		keyWords.put(10000,"gitps");
		keyWords.put(10000,"swatch");
		keyWords.put(10000,"tload");
		keyWords.put(10000,"logrotate");
		keyWords.put(10000,"uname");
		keyWords.put(10000,"chsh");
		keyWords.put(10000,"userconf");
		keyWords.put(10000,"userdel");
		keyWords.put(10000,"usermod");
		keyWords.put(10000,"vlock");
		keyWords.put(10000,"who");
		keyWords.put(10000,"whoami");
		keyWords.put(10000,"whois");
		keyWords.put(10000,"newgrp");
		keyWords.put(10000,"renice");
		keyWords.put(10000,"su");
		keyWords.put(10000,"skill");
		keyWords.put(10000,"w");
		keyWords.put(10000,"id");
		keyWords.put(10000,"free");
		keyWords.put(10000,"reset");
		keyWords.put(10000,"clear");
		keyWords.put(10000,"alias");
		keyWords.put(10000,"dircolors");
		keyWords.put(10000,"aumix");
		keyWords.put(10000,"bind");
		keyWords.put(10000,"chroot");
		keyWords.put(10000,"clock");
		keyWords.put(10000,"crontab");
		keyWords.put(10000,"declare");
		keyWords.put(10000,"depmod");
		keyWords.put(10000,"dmesg");
		keyWords.put(10000,"enable");
		keyWords.put(10000,"eval");
		keyWords.put(10000,"export");
		keyWords.put(10000,"pwunconv");
		keyWords.put(10000,"grpconv");
		keyWords.put(10000,"rpm");
		keyWords.put(10000,"insmod");
		keyWords.put(10000,"kbdconfig");
		keyWords.put(10000,"lilo");
		keyWords.put(10000,"liloconfig");
		keyWords.put(10000,"lsmod");
		keyWords.put(10000,"minfo");
		keyWords.put(10000,"set");
		keyWords.put(10000,"modprobe");
		keyWords.put(10000,"ntsysv");
		keyWords.put(10000,"mouseconfig");
		keyWords.put(10000,"passwd");
		keyWords.put(10000,"pwconv");
		keyWords.put(10000,"rdate");
		keyWords.put(10000,"resize");
		keyWords.put(10000,"rmmod");
		keyWords.put(10000,"grpunconv");
		keyWords.put(10000,"modinfo");
		keyWords.put(10000,"time");
		keyWords.put(10000,"setup");
		keyWords.put(10000,"sndconfig");
		keyWords.put(10000,"setenv");
		keyWords.put(10000,"setconsole");
		keyWords.put(10000,"timeconfig");
		keyWords.put(10000,"ulimit");
		keyWords.put(10000,"unset");
		keyWords.put(10000,"chkconfig");
		keyWords.put(10000,"apmd");
		keyWords.put(10000,"hwclock");
		keyWords.put(10000,"mkkickstart");
		keyWords.put(10000,"fbset");
		keyWords.put(10000,"unalias");
		keyWords.put(10000,"SVGATextMode");
		keyWords.put(10000,"ar");
		keyWords.put(10000,"bunzip2");
		keyWords.put(10000,"bzip2");
		keyWords.put(10000,"bzip2recover");
		keyWords.put(10000,"gunzip");
		keyWords.put(10000,"unarj");
		keyWords.put(10000,"compress");
		keyWords.put(10000,"cpio");
		keyWords.put(10000,"dump");
		keyWords.put(10000,"uuencode");
		keyWords.put(10000,"gzexe");
		keyWords.put(10000,"gzip");
		keyWords.put(10000,"lha");
		keyWords.put(10000,"restore");
		keyWords.put(10000,"tar");
		keyWords.put(10000,"uudecode");
		keyWords.put(10000,"unzip");
		keyWords.put(10000,"zip");
		keyWords.put(10000,"zipinfo");
		keyWords.put(10000,"setleds");
		keyWords.put(10000,"loadkeys");
		keyWords.put(10000,"rdev");
		keyWords.put(10000,"dumpkeys");
		keyWords.put(10000,"MAKEDEV");
		keyWords.put(10000, "check");
		keyWords.put(10000, "clean");
		keyWords.put(10000, "disclean");
		keyWords.put(10000, "info");
		keyWords.put(10000, "test");
		keyWords.put(10000, "install");
		keyWords.put(10000, "install-strip");
		keyWords.put(10000, "installdirs");
		keyWords.put(10000, "echo");

		// 路径关键字
		keyWords.put(13, "exec_prefix");
		keyWords.put(14, "bindir");
		keyWords.put(15, "sbindir");
		keyWords.put(16, "libexecdir");
		keyWords.put(17, "datadir");
		keyWords.put(18, "sysconfdir");
		keyWords.put(19, "sharedstatedir");
		keyWords.put(20, "localstatedir");
		keyWords.put(21, "libdir");
		keyWords.put(22, "infodir");
		keyWords.put(23, "includedir");
		keyWords.put(24, "oldincludedir");
		keyWords.put(25, "mandir");
		keyWords.put(26, "srcdir");

		// 关键字
		keyWords.put(27, "-include");
		keyWords.put(28, "sinclude");
		keyWords.put(29, "include");
		keyWords.put(30, "define");
		keyWords.put(31, "override");
		keyWords.put(32, "endef");
		keyWords.put(33, "export");
		keyWords.put(34, "unexport");
		keyWords.put(35, "ifeq");
		keyWords.put(36, "ifneq");
		keyWords.put(37, "ifdef");
		keyWords.put(38, "ifndef");
		keyWords.put(39, "else");
		keyWords.put(40, "endif");

		keyWords.put(51, "=");
		keyWords.put(52, ":=");
		keyWords.put(53, "?=");
		keyWords.put(54, "+=");// 4个赋值运算符

		// 伪目标
		keyWords.put(53, ".PHONY");
		keyWords.put(53, ".INTERMEDIATE ");
		keyWords.put(53, ".SECONDARY ");
		keyWords.put(53, ".PRECIOUS");

		keyWords.put(101, "(");
		keyWords.put(102, ")");
		keyWords.put(103, "[");
		keyWords.put(104, "]");
		keyWords.put(105, "{");
		keyWords.put(106, "}");
		keyWords.put(107, ".");
		keyWords.put(108, "#");// 注释
		keyWords.put(109, ",");
		keyWords.put(110, ";");
		keyWords.put(111, "'");
		keyWords.put(112, "\\");
		keyWords.put(114, "$");
		keyWords.put(115, "@");
		keyWords.put(116, "%");// 模式
		keyWords.put(117, "$");// 变量
		keyWords.put(118, "~");
		keyWords.put(119, "?");
		keyWords.put(120, "*");
		keyWords.put(121, "space");
		keyWords.put(122, "tab");
		keyWords.put(123, "^");
		keyWords.put(123, ":");

		keyWords.put(150, "id");

		// 函数
		keyWords.put(201, "subst");
		keyWords.put(202, "patsubst");
		keyWords.put(203, "strip");
		keyWords.put(204, "findstring");
		keyWords.put(205, "filter");
		keyWords.put(206, "filter-out");
		keyWords.put(207, "sort");
		keyWords.put(208, "word");
		keyWords.put(209, "wordlist");
		keyWords.put(210, "firstword");
		keyWords.put(211, "dir");
		keyWords.put(212, "notdir");
		keyWords.put(213, "suffix");
		keyWords.put(214, "basename");
		keyWords.put(215, "addsuffix");
		keyWords.put(216, "addprefix");
		keyWords.put(217, "join");
		keyWords.put(218, "foreach");
		keyWords.put(219, "if");
		keyWords.put(220, "call");
		keyWords.put(221, "origin");
		keyWords.put(222, "shell");
		keyWords.put(223, "error");

		// 特殊变量
		// 命令变量
		keyWords.put(301, "AR");
		keyWords.put(601, "ar");
		keyWords.put(302, "AS");
		keyWords.put(602, "as");
		keyWords.put(303, "CC");
		keyWords.put(603, "cc");
		keyWords.put(304, "CXX");
		keyWords.put(604, "g++");
		keyWords.put(305, "CO");
		keyWords.put(605, "co");
		keyWords.put(306, "CPP");
		keyWords.put(606, "$(CC)");
		keyWords.put(607, "-E");
		keyWords.put(307, "FC");
		keyWords.put(607, "f77");
		keyWords.put(308, "GET");
		keyWords.put(608, "get");
		keyWords.put(309, "LEX");
		keyWords.put(609, "lex");
		keyWords.put(310, "PC");
		keyWords.put(610, "pc");
		keyWords.put(311, "YACC");
		keyWords.put(611, "yacc");
		keyWords.put(312, "YACCR");
		keyWords.put(612, "yacc");
		keyWords.put(613, "–r");
		keyWords.put(313, "MAKEINFO");
		keyWords.put(613, "makeinfo");
		keyWords.put(314, "TEX");
		keyWords.put(614, "tex");
		keyWords.put(315, "TEXI2DVI");
		keyWords.put(615, "texi2dvi");
		keyWords.put(316, "WEAVE");
		keyWords.put(616, "weave");
		keyWords.put(317, "CWEAVE");
		keyWords.put(617, "cweave");
		keyWords.put(318, "TANGLE");
		keyWords.put(618, "tangle");
		keyWords.put(319, "CTANGLE");
		keyWords.put(619, "ctangle");
		keyWords.put(320, "RM");
		keyWords.put(620, "rm");
		keyWords.put(621, "–f");
		// 自动变量
		keyWords.put(341, "$@");
		keyWords.put(342, "$%");
		keyWords.put(343, "$<");
		keyWords.put(344, "$?");
		keyWords.put(345, "$^");
		keyWords.put(346, "$+");
		keyWords.put(347, "$*");
		keyWords.put(348, "$(@D)");
		keyWords.put(349, "$(@F)");
		keyWords.put(350, "$(*D)");
		keyWords.put(351, "$(*F)");
		keyWords.put(352, "$(%D)");
		keyWords.put(353, "$(%F)");
		keyWords.put(354, "$(<D)");
		keyWords.put(355, "$(<F)");
		keyWords.put(356, "$(+D)");
		keyWords.put(357, "$(+F)");
		keyWords.put(358, "$(?D)");
		keyWords.put(359, "$(?F)");
		  
		// 其他变量
		keyWords.put(381, "VPATH");
		keyWords.put(382, "MAKEFILES");
		keyWords.put(383, "MAKEFILE_LIST");
		keyWords.put(384, "MAKECMDGOALS");
		// 命令参数
		keyWords.put(401, "ARFLAGS");
		keyWords.put(402, "ASFLAGS");
		keyWords.put(403, "CFLAGS");
		keyWords.put(404, "CXXFLAGS");
		keyWords.put(405, "COFLAGS");
		keyWords.put(406, "CPPFLAGS");
		keyWords.put(407, "FFLAGS");
		keyWords.put(408, "GFLAGS");
		keyWords.put(409, "LDFLAGS");
		keyWords.put(410, "LFLAGS");
		keyWords.put(411, "PFLAGS");
		keyWords.put(412, "RFLAGS");
		keyWords.put(413, "YFLAGS");

	}
}

class Word {
	private String type="";// 类型 key
	private String wordName="";// 内容
	private int line;// 行
	private int column;// 列

	public Word() {
	}

	public Word(String type, String wordName, int line, int column) {
		super();
		this.type = type+"";
		this.wordName = wordName+"";
		this.line = line;
		this.setColumn(column);
	}

	public Word(String type, String wordName, int line) {
		super();
		this.type = type+"";
		this.wordName = wordName+"";
		this.line = line;
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getWordName() {
		return wordName;
	}

	public void setWordName(String wordName) {
		this.wordName = wordName;
	}

	public int getColumn() {
		return column;
	}

	public void setColumn(int column) {
		this.column = column;
	}
}

class block{
	private int startLine=-1;
	private int endLine=-1;
	private Vector<Word> block=null;
	int type=0;
	/*target 1
	 * ifdef 2
	 * ifeq  3
	 * export 4
	 * define 5
	 * other 0
	*/
	public block(int start, int end, Vector<Word> from, int type){
		this.block = new Vector<Word>();
		this.startLine=from.elementAt(start).getLine();
		this.endLine=from.elementAt(end).getLine();
		for(int i=start; i<end; i++){
			block.addElement(from.elementAt(i));
		}
		this.type=type;
	}
}
