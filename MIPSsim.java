
/* On my honor, I have neither given nor received unauthorized aid on this assignment */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MIPSsim {
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String strcode, code, instruction, dacode = "";// 保存从文件读到的数据
		String mipsCode = "";// 用来保存总的指令字符串
		int startaddress = 256;
		Scanner in =new Scanner(System.in);
		String fileName = in.nextLine();
		try {
			InputStream inp = new FileInputStream(fileName);// 输入sample.txt存放的位置
			InputStreamReader inps = new InputStreamReader(inp);
			BufferedReader buffd = new BufferedReader(inps);
			OutputStream oup = new FileOutputStream("dissamblely.txt");//输出文件位置
			OutputStreamWriter outpw = new OutputStreamWriter(oup);
			BufferedWriter bWriter = new BufferedWriter(outpw);
			// System.out.println("------------------");

			while ((code = buffd.readLine()) != null) {
				mipsCode += code;
			}
			int endpoint = mipsCode.length();
			int i = 0, davalue = 0;
			while (i < mipsCode.length()/32) {
				strcode = mipsCode.substring(i * 32, i * 32 + 32);
				instruction = MIPSdisassemble.disassemble(strcode);
				// System.out.println(instruction);
				if (!(instruction.contains("BREAK"))) {
					bWriter.write(strcode + "\t" + startaddress + "\t" + instruction);
					bWriter.newLine();
					global.codeIns.put(startaddress, strcode); //存放地址及机器码
					global.codeString.put(startaddress, instruction); //存放地址及指令
					startaddress += 4;
				} else {
					bWriter.write(strcode + "\t" + startaddress + "\t" + "BREAK");
					global.codeIns.put(startaddress, strcode);
					global.codeString.put(startaddress, instruction);
					endpoint = i;// 记录代码段的终止行号
					startaddress += 4;
					break;// 代码段完成
				}
				i++;
			}

			// 这里 i指向break所在行的位置,j为新的一行的位置编号
			for (int j = endpoint + 1; j < mipsCode.length() / 32; j++) {
				// 对数据段进行扫描
				dacode = mipsCode.substring(j * 32, j * 32 + 32);
				davalue = TransformValue.transform(dacode);
				bWriter.newLine();// 从新一行写入
				bWriter.write(dacode + "\t" + startaddress + "\t" + davalue);
				global.codeData.put(startaddress, davalue);
				startaddress += 4;
			}
			endpoint = 256 + endpoint * 4;// endpoint 为代码段的最后的行号

			OutputFile.output(); //输出文件
			bWriter.close();
			buffd.close();
		} catch (Exception ex) {
			System.out.print(ex.toString());
			StackTraceElement stackTraceElement = ex.getStackTrace()[0];
			System.out.println(stackTraceElement.getLineNumber());
		}
	}
}

class global {
	// 定义全局变量
	public static ArrayList<String> w = new ArrayList<String>();// 用来保存正在写的寄存器
	public static ArrayList<String[]> r = new ArrayList<>();// 用来保存正在读的寄存器
	public static Map<Integer, String> codeIns = new HashMap<Integer, String>();// 用来存放代码段的输出,这保存的是0-1串
	public static Map<Integer, String> codeString = new HashMap<Integer, String>();// 用来存放代码段的输出,这保存的是指令字符串
	public static Map<Integer, Integer> codeData = new HashMap<Integer, Integer>();// 用来存放数据段的输出
	public static int circle = 0;
	public static int[] preIssue = new int[4]; // 定义preIssue单元
	public static int[] preAlu1 = new int[2]; // 定义preAlu1单元
	public static int[] preAlu2 = new int[2]; // 定义preAlu2单元
	public static int readCount = 2; // 用来记录读取的个数，如果执行一条，那么只能读取一条
	public static int postAlu2 = 0;
	public static int postMem = 0;
	public static int preMem = 0;
	public static int[] ifUnit = new int[2];// 创建IF Unit
	public static int start = 256;// 当前PC
	public static int Register[] = new int[32];// 寄存器数组，用来保存不同寄存器的值
	public static boolean end = true;
	public static int wIssue = 0;// 保存当前发射LW和SW的指令
	public static int otherIssue = 0;// 保存当前发射除LW和SW以外的指令
	public static int lastIssue[] = new int[4];// 保存上个周期的preIssue
	public static int count;
}

class MIPSdisassemble {
	// 解析字符串生成dissamblely.txt文件
	public static String disassemble(String strcode) {
		String op, rt, rs, rd, sa, immediate, offset, inStr = "", inStr_idx, base = "";
		String rsN, rtN, rdN, baseS = "";
		String read[] = new String[2];
		int offsetN, saN, immediateN = 0;
		if (strcode.substring(0, 2).equals("11")) {
			switch (strcode.substring(2, 6)) {
			case "0000":
				op = "ADD";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				rdN = "R" + Integer.parseInt(rd, 2);
				inStr = op + " " + rdN + ", " + rsN + ", " + rtN;
				read[0] = rsN;
				read[1] = rtN;
				global.w.add(rdN);// 正在写的寄存器放到寄存器数组里面
				global.r.add(read);// 正在读的寄存器放到寄存器数组里面
				break;
			case "0001":
				op = "SUB";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				rdN = "R" + Integer.parseInt(rd, 2);
				inStr = op + " " + rdN + ", " + rsN + ", " + rtN;
				read[0] = rsN;
				read[1] = rtN;
				global.w.add(rdN);
				global.r.add(read);
				break;
			case "0010":
				op = "MUL";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				rdN = "R" + Integer.parseInt(rd, 2);
				inStr = op + " " + rdN + ", " + rsN + ", " + rtN;
				read[0] = rsN;
				read[1] = rtN;
				global.w.add(rdN);
				global.r.add(read);
				break;
			case "0011":
				op = "AND";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				rdN = "R" + Integer.parseInt(rd, 2);
				inStr = op + " " + rdN + ", " + rsN + ", " + rtN;
				read[0] = rsN;
				read[1] = rtN;
				global.w.add(rdN);
				global.r.add(read);
				break;
			case "0100":
				op = "OR";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				rdN = "R" + Integer.parseInt(rd, 2);
				inStr = op + " " + rdN + ", " + rsN + ", " + rtN;
				read[0] = rsN;
				read[1] = rtN;
				global.w.add(rdN);
				global.r.add(read);
				break;
			case "0101":
				op = "XOR";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				rdN = "R" + Integer.parseInt(rd, 2);
				inStr = op + " " + rdN + ", " + rsN + ", " + rtN;
				read[0] = rsN;
				read[1] = rtN;
				global.w.add(rdN);
				global.r.add(read);
				break;
			case "0110":
				op = "NOR";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				rdN = "R" + Integer.parseInt(rd, 2);
				inStr = op + " " + rdN + ", " + rsN + ", " + rtN;
				read[0] = rsN;
				read[1] = rtN;
				global.w.add(rdN);
				global.r.add(read);
				break;
			case "0111":
				op = "SLT";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				rdN = "R" + Integer.parseInt(rd, 2);
				inStr = op + " " + rdN + ", " + rsN + ", " + rtN;
				read[0] = rsN;
				read[1] = rtN;
				global.w.add(rdN);
				global.r.add(read);
				break;
			case "1000":
				op = "ADDI";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				immediate = strcode.substring(16, 32);
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				immediateN = Integer.parseInt(immediate, 2);
				inStr = op + " " + rtN + ", " + rsN + ", #" + immediateN;
				read[0] = rsN;
				global.w.add(rtN);
				global.r.add(read);
				break;
			case "1001":
				op = "ANDI";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				immediate = strcode.substring(16, 32);
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				immediateN = Integer.parseInt(immediate, 2);
				inStr = op + " " + rtN + ", " + rsN + ", " + immediateN;
				read[0] = rsN;
				global.w.add(rtN);
				global.r.add(read);
				break;
			case "1010":
				op = "ORI";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				immediate = strcode.substring(16, 21);
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				immediateN = Integer.parseInt(immediate, 2);
				inStr = op + " " + rtN + ", " + rsN + ", " + immediateN;
				read[0] = rsN;
				global.w.add(rtN);
				global.r.add(read);
				break;
			}
		} else {
			switch (strcode.substring(2, 6)) {
			case "0000":
				op = "J";
				inStr_idx = strcode.substring(6, 32) + "00";
				inStr = op + " " + "#" + Integer.parseInt(inStr_idx, 2);
				read[0] = "";
				global.r.add(read);
				global.w.add("");
				break;
			case "0001":
				op = "JR";
				rs = strcode.substring(6, 11);
				rsN = "R" + Integer.parseInt(rs, 2);
				inStr = op + " " + rsN;
				read[0] = rsN;
				global.r.add(read);// 存放到读的指令里面
				global.w.add("");
				break;
			case "0010":
				op = "BEQ";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				offset = strcode.substring(16, 32) + "00";
				rsN = "R" + Integer.parseInt(rs, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				offsetN = Integer.parseInt(offset, 2);
				inStr = op + " " + rsN + ", " + rtN + ", #" + offsetN;
				read[0] = rsN;
				read[1] = rtN;
				global.r.add(read);
				global.w.add("");// 没有写的寄存器，就存空字符串，以免寻找的时候指针溢出
				break;
			case "0011":
				op = "BLTZ";
				rs = strcode.substring(6, 11);
				offset = strcode.substring(16, 32) + "00";
				rsN = "R" + Integer.parseInt(rs, 2);
				offsetN = Integer.parseInt(offset, 2);
				inStr = op + " " + rsN + ", #" + offsetN;
				read[0] = rsN;
				global.r.add(read);
				global.w.add("");
				break;
			case "0100":
				op = "BGTZ";
				rs = strcode.substring(6, 11);
				offset = strcode.substring(16, 32) + "00";
				rsN = "R" + Integer.parseInt(rs, 2);
				offsetN = Integer.parseInt(offset, 2);
				inStr = op + " " + rsN + ", #" + offsetN;
				read[0] = rsN;
				global.r.add(read);
				global.w.add("");
				break;
			case "0101":
				op = "BREAK";
				inStr = op;
				break;
			case "0110":
				op = "SW";
				base = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				offset = strcode.substring(16, 32);
				offsetN = Integer.parseInt(offset, 2);
				baseS = "R" + Integer.parseInt(base, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				inStr = op + " " + rtN + ", " + offsetN + "(" + baseS + ")";
				global.w.add(rtN);
				read[0] = baseS;
				global.r.add(read);
				break;
			case "0111":
				op = "LW";
				base = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				offset = strcode.substring(16, 32);
				baseS = "R" + Integer.parseInt(base, 2);
				offsetN = Integer.parseInt(offset, 2);
				rtN = "R" + Integer.parseInt(rt, 2);
				inStr = op + " " + rtN + ", " + offsetN + "(" + baseS + ")";
				global.w.add(rtN);
				read[0] = baseS;
				global.r.add(read);
				break;
			case "1000":
				op = "SLL";
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				sa = strcode.substring(21, 26);
				rtN = "R" + Integer.parseInt(rt, 2);
				rdN = "R" + Integer.parseInt(rd, 2);
				saN = Integer.parseInt(sa, 2);
				inStr = op + " " + rdN + ", " + rtN + ", #" + saN;
				global.w.add(rdN);
				read[0] = rtN;
				global.r.add(read);
				break;
			case "1001":
				op = "SRL";
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				sa = strcode.substring(21, 26);
				rtN = "R" + Integer.parseInt(rt, 2);
				rdN = "R" + Integer.parseInt(rd, 2);
				saN = +Integer.parseInt(sa, 2);
				inStr = op + " " + rdN + ", " + rtN + ", #" + saN;
				global.w.add(rdN);
				read[0] = rtN;
				global.r.add(read);
				break;
			case "1010":
				op = "SRA";
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				sa = strcode.substring(21, 26);
				rtN = "R" + Integer.parseInt(rt, 2);
				rdN = "R" + Integer.parseInt(rd, 2);
				saN = +Integer.parseInt(sa, 2);
				inStr = op + " " + rdN + ", " + rtN + ", #" + saN;
				global.w.add(rdN);
				read[0] = rtN;
				global.r.add(read);
				break;
			case "1011":
				op = "NOP";
				inStr = op;
				break;
			}
		}

		return inStr;
	}

}

class TransformValue {
	public static int transform(String str) {
		int temp;
		if (str.indexOf("0") == 0) {
			// 如果符号位为正
			return Integer.parseInt(str, 2);
		} else {
			// 如果符号位为负
			char[] reverse = str.toCharArray();
			int j = str.length();
			for (int i = 0; i < j; i++) {
				if (reverse[i] == '1') {
					reverse[i] = '0';
				} else {
					reverse[i] = '1';
				}
			} // 将字符串中的值按位取反
			temp = Integer.parseInt(String.valueOf(reverse), 2);
			temp += 1;
			return (-1) * temp;

		}
	}
}

// 取指令阶段
class fetchInstruction {
	public static boolean fetch() {
		for (int i = 0; i < global.readCount && i < global.count && global.ifUnit[0] == 0 && global.ifUnit[1] == 0
				&& global.preIssue[3] == 0; i++) {   // 取指令的条件
													// 每次读指令,最多读两条												
			// 取指令不光要满足执行一条语句后只能取一条的条件
			// 同时也得满足根据上个周期的情况来进行取指令的操作，因为这次周期取指令的数量 要以上个周期preIssue的空的数量决定
			if (global.codeString.get(global.start).contains("J")) { // 这里还需判断是否为跳转指令
				global.ifUnit[1] = global.start;
			} else if (global.codeString.get(global.start).contains("JR")) {
				global.ifUnit[1] = global.start;
			} else if (global.codeString.get(global.start).contains("BREAK")) {
				global.ifUnit[1] = global.start;
				return true;
			} else if (global.codeString.get(global.start).contains("BEQ")) {
				global.ifUnit[0] = global.start;
			} else if (global.codeString.get(global.start).contains("BLTZ")) {
				global.ifUnit[0] = global.start;
			}else if (global.codeString.get(global.start).contains("BGTZ")) {
				global.ifUnit[0] = global.start;
			} else {   //取指令时候的判断，若为满，则无法取指令
				if (global.preIssue[2] != 0) {
					global.preIssue[3] = global.start;
				} else if (global.preIssue[1] != 0) {
					global.preIssue[2] = global.start;
				} else if (global.preIssue[0] != 0) {
					global.preIssue[1] = global.start;
				} else {
					global.preIssue[0] = global.start;
				}
			}
			global.start = global.start + 4;
		}

		return false;
	}
}

// 执行指令
class MIPSPipeline {
	public static void simulator(String strcode) throws FileNotFoundException {

		String op, rt, rs, rd, sa, immediate, offset;
		String inStr_idx, base = "";
		int rss, rts, rds;// 存储寄存器的编号
		int offsetN, saN, immediateN, baseN = 0;
		if (strcode == "") {

		}
		if (strcode.substring(0, 2).equals("11")) {
			// 执行指令或进行跳转
			switch (strcode.substring(2, 6)) {
			case "0000":
				// op = "ADD";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				rds = Integer.parseInt(rd, 2);
				global.Register[rds] = global.Register[rss] + global.Register[rts];
				break;
			case "0001":
				// op = "SUB";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				rds = Integer.parseInt(rd, 2);
				global.Register[rds] = global.Register[rss] - global.Register[rts];
				break;
			case "0010":
				// op = "MUL";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				rds = Integer.parseInt(rd, 2);
				global.Register[rds] = global.Register[rss] * global.Register[rts];
				break;
			case "0011":
				// op = "AND";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				rds = Integer.parseInt(rd, 2);
				global.Register[rds] = global.Register[rss] & global.Register[rts];
				break;
			case "0100":
				// op = "OR";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				rds = Integer.parseInt(rd, 2);
				global.Register[rds] = global.Register[rss] | global.Register[rts];
				break;
			case "0101":
				// op = "XOR";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				rds = Integer.parseInt(rd, 2);
				global.Register[rds] = global.Register[rss] ^ global.Register[rts];
				break;
			case "0110":
				// op = "NOR";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				rds = Integer.parseInt(rd, 2);
				global.Register[rds] = ~(global.Register[rss] | global.Register[rts]);
				break;
			case "0111":
				// op = "SLT";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				rds = Integer.parseInt(rd, 2);
				if (rss < rts) {
					global.Register[rds] = 1;
				} else {
					global.Register[rds] = 0;
				}
				break;
			case "1000":
				// op = "ADDI";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				immediate = strcode.substring(16, 32);
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				immediateN = Integer.parseInt(immediate, 2);
				global.Register[rts] = global.Register[rss] + immediateN;
				break;
			case "1001":
				// op = "ANDI";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				immediate = strcode.substring(16, 32);
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				immediateN = Integer.parseInt(immediate, 2);
				global.Register[rts] = global.Register[rss] & immediateN;
				break;
			case "1010":
				// op = "ORI";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				immediate = strcode.substring(16, 21);
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				immediateN = Integer.parseInt(immediate, 2);
				global.Register[rts] = global.Register[rss] | immediateN;
				break;
			}
		} else {
			switch (strcode.substring(2, 6)) {
			case "0000":
				// op = "J";
				inStr_idx = strcode.substring(6, 32) + "00";
				global.start = Integer.parseInt(inStr_idx, 2);
				break;
			case "0001":
				// op="JR";
				rs = strcode.substring(6, 11);
				rss = Integer.parseInt(rs, 2);
				global.start = global.Register[rss];
				break;
			case "0010":
				// op="BEQ";
				rs = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				offset = strcode.substring(16, 32) + "00";
				rss = Integer.parseInt(rs, 2);
				rts = Integer.parseInt(rt, 2);
				offsetN = Integer.parseInt(offset, 2);
				if (global.Register[rss] == global.Register[rts]) {
					global.start = global.start + offsetN;
				}
				break;
			case "0011":
				// op="BLTZ";
				rs = strcode.substring(6, 11);
				offset = strcode.substring(16, 32) + "00";
				rss = Integer.parseInt(rs, 2);
				offsetN = Integer.parseInt(offset, 2);
				if (global.Register[rss] < 0) {
					global.start = global.start + offsetN;
				}
				break;
			case "0100":
				// op="BGTZ";
				rs = strcode.substring(6, 11);
				offset = strcode.substring(16, 32) + "00";
				rss = Integer.parseInt(rs, 2);
				offsetN = Integer.parseInt(offset, 2);
				if (global.Register[rss] > 0) {
					global.start = global.start + offsetN;// 需要注意的是这里偏移量得加上delay slot中地址得到目的地址
				}
				break;
			case "0101":
				op = "BREAK";
				// global.start+=4;//执行pc+4
				System.out.println("---------------");
				global.end = false;
				break;
			case "0110":
				// op="SW";
				base = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				offset = strcode.substring(16, 32);
				offsetN = Integer.parseInt(offset, 2);
				baseN = Integer.parseInt(base, 2);
				rts = Integer.parseInt(rt, 2);
				global.codeData.remove(offsetN + global.Register[baseN]);// 注意要修改内存中的值
				global.codeData.put(offsetN + global.Register[baseN], global.Register[rts]);
				break;
			case "0111":
				// op="LW";
				base = strcode.substring(6, 11);
				rt = strcode.substring(11, 16);
				offset = strcode.substring(16, 32);
				baseN = Integer.parseInt(base, 2);
				offsetN = Integer.parseInt(offset, 2);
				rts = Integer.parseInt(rt, 2);
				global.Register[rts] = global.codeData.get(offsetN + global.Register[baseN]);
				break;
			case "1000":
				// op="SLL";
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				sa = strcode.substring(21, 26);
				rts = Integer.parseInt(rt, 2);
				rds = Integer.parseInt(rd, 2);
				saN = Integer.parseInt(sa, 2);
				global.Register[rds] = global.Register[rts] << saN;
				break;
			case "1001":
				// op="SRL";
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				sa = strcode.substring(21, 26);
				rts = Integer.parseInt(rt, 2);
				rds = Integer.parseInt(rd, 2);
				saN = +Integer.parseInt(sa, 2);
				global.Register[rds] = global.Register[rts] >> saN;
				break;
			case "1010":
				// op="SRA";
				rt = strcode.substring(11, 16);
				rd = strcode.substring(16, 21);
				sa = strcode.substring(21, 26);
				rts = Integer.parseInt(rt, 2);
				rds = Integer.parseInt(rd, 2);
				saN = +Integer.parseInt(sa, 2);
				global.Register[rds] = (global.Register[rts] >> saN) | global.Register[rts];
				break;
			case "1011":
				// op="NOP";
				break;
			}
		}
	}

}

class HazardJudgment {// 判断依赖
	static boolean PreSig = true;
	static boolean AluSig = true;
	static boolean Temp = true;

	public static boolean judges(int pc, int index) {
		PreSig = true;
		AluSig = true;
		Temp = true;
		for (int i = 0; global.preIssue[i] != 0 && i < index; i++) { // 判断preIssue的依赖，如果preIssue里面为空，则不用比较是否有hazard
			PreSig = judge(pc, global.preIssue[i], true);
			if (!PreSig)
				return false;
		}
		for (int i = 0; i < 2; i++) {
			// 这里只比较entry0位置的就行
			AluSig = judge(pc, global.preAlu1[i], i == 0) && judge(pc, global.preAlu2[i], i == 0);// 判断两个Alu里面的依赖，这里不用判断ifUnit的依赖，
			                                                                                       //因为ifUnit是最后执行的指令
			if (!AluSig)
				return false;
		}
		Temp = judge(pc, global.postAlu2, false) && judge(pc, global.preMem, false) && judge(pc, global.postMem, false);//判断和post单元和preMem里面的指令的依赖,只有一行
		return PreSig && AluSig && Temp;

	}

	public static boolean judge(int pc, int compc, boolean isIssue) {
		if (compc == 0)
			return true;
		if (isIssue)
			return WAW(pc, compc) && RAW(pc, compc) && RAW(compc, pc);
		return WAW(pc, compc) && RAW(pc, compc);// 这里需要注意的就是，在比较RAW时，只和preIssue里面的比，因为外面
	} // 的指令之间不存在RAW依赖，一般都是先读完，然后才写的，不会发生读在写之后

	public static boolean WAW(int pc1, int pc2) {
		// 其中pc1为当前pc, pc2为被比较的pc
		if ((global.w.get((pc1 - 256) / 4)).equals(global.w.get((pc2 - 256) / 4))) {
			return false; // 存在WAW危害
		}
		return true;
	}

	public static boolean RAW(int pc1, int pc2) {
		// 其中pc1为当前pc, pc2为被比较的pc
		if (global.w.get((pc2 - 256) / 4) != null && global.r.get((pc1 - 256) / 4) != null) {
			if ((global.r.get((pc1 - 256) / 4)[0]).equals(global.w.get((pc2 - 256) / 4))) {
				return false; // 存在RAW危害
			} else if ((global.r.get((pc1 - 256) / 4)[1]) != null
					&& (global.r.get((pc1 - 256) / 4)[1]).equals(global.w.get((pc2 - 256) / 4))) {
				return false; // 由于是数组，因此需要判断当前读的，每一个寄存器是否发生RAW危害
			}
		}
		return true;
	}
	// WAR 和RAW 参数互换即可
}

class MoveInstruction {// 发射阶段
	public static void move() throws FileNotFoundException {
		global.readCount = 2;// 执行后，最大读取次数还原为2
		if (global.postMem != 0) {
			MIPSPipeline.simulator(global.codeIns.get(global.postMem));
			global.readCount--;// 执行一条指令后，则读取的指令减一
		} // 先执行指令的判断条件
		if (global.postAlu2 != 0) {
			MIPSPipeline.simulator(global.codeIns.get(global.postAlu2));
			global.readCount--;
		}
		if (global.ifUnit[1] != 0) {
			MIPSPipeline.simulator(global.codeIns.get(global.ifUnit[1]));
			global.ifUnit[1] = 0;
		}
		// 这里在执行ifUnit移动的时候，需要判断依赖
		if (global.ifUnit[0] != 0 && global.ifUnit[1] == 0 && HazardJudgment.judges(global.ifUnit[0], 4)) {
			global.ifUnit[1] = global.ifUnit[0];
			global.ifUnit[0] = 0;
		}
		global.preAlu2[1] = global.preAlu2[0];
		global.preAlu1[1] = global.preAlu1[0];
		global.preAlu1[0] = 0;
		global.preAlu2[0] = 0;
		if (global.preIssue[3] != 0) {
			global.count = 0;
		} // 计算上一个周期，preIssue的周期情况,只有存在空的指令槽，才能继续取指令，否则不能取指令。
		else if (global.preIssue[2] != 0) {
			global.count = 1;
		} else
			global.count = 2;
		for (int i = 0; i < 4 && global.preIssue[i] != 0; i++) {
			if (HazardJudgment.judges(global.preIssue[i], i)) {
				// 其他位置的寄存器进行移动
				// 这里存在同时发射两条指令的情况 一个发送到ALU1 一个发送到ALU2,
				// 那么开始选择
				if (global.codeString.get(global.preIssue[i]).contains("SW")
						|| global.codeString.get(global.preIssue[i]).contains("LW")) {
					if (global.wIssue == 0) {
						global.preAlu1[0] = global.preIssue[i];
						global.wIssue = global.preIssue[i]; // 保存到临时变量中,说明此时已经发射了一条
						for (int j = i + 1; j < 4; j++)
							global.preIssue[j - 1] = global.preIssue[j];
						i--;// 每次重新判断依赖都得从entry0到entry3来遍历判断
						global.preIssue[3] = 0;
					}
				} else {
					if (global.otherIssue == 0) {
						global.preAlu2[0] = global.preIssue[i];
						global.otherIssue = global.preIssue[i]; // 保存到储存其他发射指令的临时变量中,说明此时已经发射了一条其他指令
						for (int j = i + 1; j < 4; j++)
							global.preIssue[j - 1] = global.preIssue[j];
						i--;
						global.preIssue[3] = 0;
					}

				}
			}
		}
		global.wIssue = 0;
		global.otherIssue = 0;// 在每次发射完以后，重置保存发射的临时变量
		global.postAlu2 = global.preAlu2[1];
		global.preAlu2[1] = 0;
		global.postMem = global.preMem;
		global.preMem = global.preAlu1[1];
		global.preAlu1[1] = 0;
	}
}

class OutputFile {
	public static void output() throws IOException {
		OutputStream oup = new FileOutputStream("simulation.txt");
		OutputStreamWriter outpw = new OutputStreamWriter(oup);
		BufferedWriter bWriter = new BufferedWriter(outpw);// 输出simulator.txt
		// 将内容写出
		boolean isBreak = false;
		// while (!isBreak) {
		for (int i = 0; !isBreak; i++) {
//			if (i == 45)
//				System.out.print("ok");
			MoveInstruction.move(); // 先执行再发射再移动
			isBreak = fetchInstruction.fetch(); //然后再取指令
			writeCycle(bWriter);
		}
		bWriter.close();
	}
	// }

	public static String NotNull(String code) {
		if (code == null)
			return "";
		else
			return "[" + code + "]";
	}
	// 判断指令是否为空

	public static void writeCycle(BufferedWriter bWriter) {
		StringBuilder sBuilder = new StringBuilder();

		try {
			sBuilder.append("--------------------");
			sBuilder.append("\n");
			sBuilder.append("Cycle:" + (++global.circle));
			sBuilder.append("\n");
			sBuilder.append("\n");
			sBuilder.append("IF Unit: ");
			sBuilder.append("\n");
			sBuilder.append("\t" + "Waiting Instruction: " + NotNull(global.codeString.get(global.ifUnit[0])));
			sBuilder.append("\n");
			sBuilder.append("\t" + "Executed Instruction: " + NotNull(global.codeString.get(global.ifUnit[1])));
			sBuilder.append("\n");
			sBuilder.append("Pre-Issue Queue: ");
			sBuilder.append("\n");
			for (int i = 0; i < 4; i++) {
				sBuilder.append("\t" + "Entry " + i + ": " + NotNull(global.codeString.get(global.preIssue[i])));
				sBuilder.append("\n");
			}
			sBuilder.append("Pre-ALU1 Queue: ");
			sBuilder.append("\n");
			for (int i = 0; i < 2; i++) {
				sBuilder.append("\t" + "Entry " + i + ": " + NotNull(global.codeString.get(global.preAlu1[i])));
				sBuilder.append("\n");
			}
			sBuilder.append("Pre-MEM Queue: " + NotNull(global.codeString.get(global.preMem)));
			sBuilder.append("\n");
			sBuilder.append("Post-MEM Queue: " + NotNull(global.codeString.get(global.postMem)));
			sBuilder.append("\n");
			sBuilder.append("Pre-ALU2 Queue: ");
			sBuilder.append("\n");
			for (int i = 0; i < 2; i++) {
				sBuilder.append("\t" + "Entry " + i + ": " + NotNull(global.codeString.get(global.preAlu2[i])));
				sBuilder.append("\n");
			}
			sBuilder.append("Post-ALU2 Queue: " + NotNull(global.codeString.get(global.postAlu2)));
			sBuilder.append("\n");
			sBuilder.append("\n");
			sBuilder.append("Registers");
			sBuilder.append("\n");
			sBuilder.append("R00:");
			for (int i = 0; i < 8; ++i) {
				// System.out.print("\t"+global.Register[i]);
				sBuilder.append("\t" + global.Register[i]);
			}
			sBuilder.append("\n");
			sBuilder.append("R08:");
			for (int i = 8; i < 16; ++i) {
				// System.out.print("\t"+register[i]);
				sBuilder.append("\t" + global.Register[i]);
			}
			sBuilder.append("\n");
			sBuilder.append("R16:");
			for (int i = 16; i < 24; ++i) {
				// System.out.print("\t"+register[i]);
				sBuilder.append("\t" + global.Register[i]);
			}
			sBuilder.append("\n");
			sBuilder.append("R24:");
			for (int i = 24; i < 32; ++i) {
				// System.out.print("\t"+register[i]);
				sBuilder.append("\t" + global.Register[i]);
			}
			sBuilder.append("\n");
			sBuilder.append("\n");
			sBuilder.append("Data");
			sBuilder.append("\n");
			int initData = 256 + global.codeIns.size() * 4;
			for (int i = initData; i < initData + global.codeData.size() * 4; i += 4) {
				if ((i - initData) / 4 % 8 == 0) {

					sBuilder.append(i + ":");
				}
				sBuilder.append("\t" + global.codeData.get(i));
				if ((i - initData) / 4 % 8 == 7) {
					sBuilder.append("\n");
				}
			}
			bWriter.write(sBuilder.toString());
			System.out.print(sBuilder.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
