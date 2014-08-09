import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

public class Datapath {
	static HashMap<String, Integer> labels = new HashMap<String, Integer>();
	int programCounter;

	Integer[] instructionMemory, dataMemory;

	HashMap<Integer, Integer> registers;

	HashMap<String, Integer> IF_ID, ID_EXEC, EXEC_MEM, MEM_WB;

	HashMap<String, Integer> TIF_ID, TID_EXEC, TEXEC_MEM, TMEM_WB;

	final static int R = 0;

	final static int I = 1;

	final static int J = 2;

	final static int LW = 3;

	final static int SW = 4;

	final static int B = 5;

	public void init() {
		registers = fillReg(new File("reg.txt"));
		if(registers==null)
		registers= new HashMap<Integer,Integer>();
		instructionMemory = new Integer[2000];
		dataMemory = new Integer[2000];
		programCounter = 0;
		// TODO:addfile

	}

	public boolean clockCycle() {
		boolean executed = false;

		if (instructionMemory[programCounter] != null) {
			instructionFetch();
			executed = true;
		} else {
			TIF_ID = null;
		}
		if (IF_ID != null) {
			instructionDecode();
			executed = true;
		} else {
			TID_EXEC = null;
		}
		if (ID_EXEC != null) {
			execute();
			executed = true;
		} else {
			TEXEC_MEM = null;
		}
		if (EXEC_MEM != null) {
			memoryAccess();
			executed = true;
		} else {
			TMEM_WB = null;
		}

		if (MEM_WB != null) {
			writeBack();
			executed = true;
		}
		IF_ID = (TIF_ID != null) ? (HashMap<String, Integer>) (TIF_ID.clone())
				: null;
		ID_EXEC = (TID_EXEC != null) ? (HashMap<String, Integer>) (TID_EXEC
				.clone()) : null;
		EXEC_MEM = (TEXEC_MEM != null) ? (HashMap<String, Integer>) (TEXEC_MEM
				.clone()) : null;
		MEM_WB = (TMEM_WB != null) ? (HashMap<String, Integer>) (TMEM_WB
				.clone()) : null;

		return executed;
	}

	public void instructionFetch() {
		if (TIF_ID == null)
			TIF_ID = new HashMap<String, Integer>();

		Integer instruction = instructionMemory[programCounter];
		Integer opcode = Integer.parseInt(toNString(instruction, 32).substring(
				0, 6));
		Integer function = Integer.parseInt(toNString(instruction, 32)
				.substring(26, 32));
		programCounter++;

		switch (opcode) {
		case 0:
			if (function == 8)
				stall(2);
			break;// jr
		case 2: // j
		case 3:
			stall(2);
			break; // jal
		case 4: // beq
		case 5:
			stall(3);
			break;// bne
		}

		TIF_ID.put("instruction", instruction);
		TIF_ID.put("programCounter", programCounter);

	}

	public void instructionDecode() {

		if (TID_EXEC == null)
			TID_EXEC = new HashMap<String, Integer>();

		Integer instruction = IF_ID.get("instruction");
		Integer pc = IF_ID.get("programCounter");

		int type;
		int mask = 0xFC000000;// 0b11111100000000000000000000000000
		switch (instruction & mask) {
		case 0:
			type = R;
			break;
		case 0x8000000:
		case 0xC000000:
			type = J;
			break;
		case 0x38000000:
			type = LW;
			break;// 0b10001100000000000000000000000000
		case 0x3C000000:
			type = SW;
			break;// 0b10101100000000000000000000000000
		case 0x14000000:
		case 0x10000000:
			type = B;
			break;
		default:
			type = I;
		}

		switch (type) {
		case R:
			Rfill(instruction);
			break;
		case I:
			Ifill(instruction);
			break;
		case J:
			Jfill(instruction);
			break;
		case LW:
			LWfill(instruction);
			break;
		case SW:
			SWfill(instruction);
			break;
		case B:
			Bfill(instruction);
			break;
		default:
		}

		TID_EXEC.put("programCounter", pc);
		TID_EXEC.put("instruction", instruction);

	}

	private void Bfill(Integer instruction) {
		TID_EXEC.put("RegDst", 0);
		TID_EXEC.put("ALUOp1", 0);
		TID_EXEC.put("ALUOp2", 1);
		TID_EXEC.put("ALUSrc", 0);
		TID_EXEC.put("Branch", 1);
		TID_EXEC.put("MemRead", 0);
		TID_EXEC.put("MemWrite", 0);
		TID_EXEC.put("RegWrite", 0);
		TID_EXEC.put("MemtoReg", 1);
		TID_EXEC.put("type", B);
		int opcode = Integer.parseInt(toNString(instruction, 32)
				.substring(0, 6), 2);
		if (opcode == 5)
			TID_EXEC.put("equal", 0); // bne
		else
			TID_EXEC.put("equal", 1);// beq
		TID_EXEC.put("immediateField",
				handleNegative(toNString(instruction, 32).substring(16, 32)));
		TID_EXEC.put("rsContent", getRs(instruction));
		TID_EXEC.put("rtContent", getRt(instruction));

	}

	private int handleNegative(String number) {
		char bit = number.charAt(0);

		if (bit == '1') {
			String complement = "";
			for (int i = 0; i < number.length(); i++) {
				complement += number.charAt(i) == '0' ? '1' : '0';
			}
			int bc = Integer.parseInt(complement, 2) + 0b1;
			return -bc;
		} else
			return Integer.parseInt(number, 2);

		// while(number.length()<8)
		// number = bit + number;
		// return ((byte) Integer.parseInt(number,2));
	}

	private void LWfill(Integer instruction) {
		String sti = toNString(instruction, 32);
		TID_EXEC.put("RegDst", 0);
		TID_EXEC.put("ALUOp1", 0);
		TID_EXEC.put("ALUOp2", 0);
		TID_EXEC.put("ALUSrc", 1);
		TID_EXEC.put("Branch", 0);
		TID_EXEC.put("MemRead", 1);
		TID_EXEC.put("MemWrite", 0);
		TID_EXEC.put("RegWrite", 1);
		TID_EXEC.put("MemtoReg", 0);
		TID_EXEC.put("type", LW);
		TID_EXEC.put("rsContent", getRs(instruction));
		TID_EXEC.put("rtContent", getRt(instruction));
		TID_EXEC.put("rtAdress", Integer.parseInt(sti.substring(11, 16), 2));
		TID_EXEC.put("immediateField", handleNegative(sti.substring(16, 32))); // Ehh
																				// elly
																				// gab
																				// el
																				// immediate
																				// f
																				// el
																				// R-Type
																				// w
																				// leh
																				// mesh
																				// 7atenha
																				// f
																				// el
																				// LW
																				// w
																				// el
																				// SW
																				// !!
		// 3andak 7a2 hmmm

	}

	private void SWfill(Integer instruction) {
		String sti = toNString(instruction, 32);
		TID_EXEC.put("RegDst", 0);
		TID_EXEC.put("ALUOp1", 0);
		TID_EXEC.put("ALUOp2", 0);
		TID_EXEC.put("ALUSrc", 1);
		TID_EXEC.put("Branch", 0);
		TID_EXEC.put("MemRead", 0);
		TID_EXEC.put("MemWrite", 1);
		TID_EXEC.put("RegWrite", 0);
		TID_EXEC.put("MemtoReg", 1);
		TID_EXEC.put("rsContent", getRs(instruction));
		TID_EXEC.put("rtContent", getRt(instruction));
		TID_EXEC.put("type", SW);
		TID_EXEC.put("immediateField", handleNegative(sti.substring(16, 32))); // Ehh
																				// elly
																				// gab
																				// el
																				// immediate
																				// f
																				// el
																				// R-Type
																				// w
																				// leh
																				// mesh
																				// 7atenha
																				// f
																				// el
																				// LW
																				// w
																				// el
																				// SW
																				// !!
		// 3andak 7a2 hmmm
	}

	private void Jfill(Integer instruction) {
		String sti = toNString(instruction, 32);
		TID_EXEC.put("type", J);
		TID_EXEC.put("RegWrite", 0);
		TID_EXEC.put("address", Integer.parseInt(sti.substring(6, 32), 2));
		// Ya Sakr shift it by 2 3and el execute stage !!!!!!

	}

	private void Ifill(Integer instruction) {
		String sti = toNString(instruction, 32);
		TID_EXEC.put("RegDst", 0);
		TID_EXEC.put("ALUOp1", 1);
		TID_EXEC.put("ALUOp2", 0);
		TID_EXEC.put("ALUSrc", 1);
		TID_EXEC.put("Branch", 0);
		TID_EXEC.put("MemRead", 0);
		TID_EXEC.put("MemWrite", 0);
		TID_EXEC.put("RegWrite", 1);
		TID_EXEC.put("MemtoReg", 1);
		TID_EXEC.put("rsContent", getRs(instruction));
		TID_EXEC.put("rtContent", getRt(instruction));
		TID_EXEC.put("rdAdress", Integer.parseInt(sti.substring(16, 21), 2));
		TID_EXEC.put("rtAdress", Integer.parseInt(sti.substring(11, 16), 2));
		TID_EXEC.put("immediateField", handleNegative(sti.substring(16, 32)));
		TID_EXEC.put("Opcode", Integer.parseInt(sti.substring(0, 6), 2));
		TID_EXEC.put("type", I);

	}

	private void Rfill(int instruction) {
		String sti = toNString(instruction, 32);
		Integer function = Integer.parseInt(toNString(instruction, 32)
				.substring(26, 32), 2);
		TID_EXEC.put("RegDst", 1);
		TID_EXEC.put("ALUOp1", 1);
		TID_EXEC.put("ALUOp2", 0);
		TID_EXEC.put("ALUSrc", 0);
		TID_EXEC.put("Branch", 0);
		TID_EXEC.put("MemRead", 0);
		TID_EXEC.put("MemWrite", 0);
		TID_EXEC.put("RegWrite", (function == 8) ? 0 : 1);
		TID_EXEC.put("MemtoReg", 1);
		TID_EXEC.put("rsContent", getRs(instruction));
		TID_EXEC.put("rtContent", getRt(instruction));
		TID_EXEC.put("rdAdress", Integer.parseInt(sti.substring(16, 21), 2));
		TID_EXEC.put("rtAdress", Integer.parseInt(sti.substring(11, 16), 2));
		TID_EXEC.put("type", R);

	}

	public int getRs(int ins) {

		String sti = toNString(ins, 32);
		return registers.get(Integer.parseInt(sti.substring(6, 11), 2));

	}

	public int getRt(int ins) {
		String sti = toNString(ins, 32);
		return registers.get(Integer.parseInt(sti.substring(11, 16), 2));

	}

	public void writeInRd(int ins, int value) {
		String sti = toNString(ins, 32);
		// registers.put(sti.substring(16, 21), value);

	}

	public String toNString(int instruction, int N) {
		String sti = Integer.toBinaryString(instruction);

		while (sti.length() < N)
			sti = 0 + sti;

		return sti;

	}

	public void execute() {

		if (TEXEC_MEM == null)
			TEXEC_MEM = new HashMap<String, Integer>();

		TEXEC_MEM.put("zeroFlag", 0);
		TEXEC_MEM.put("Branch", 0);

		// use values from ID_EXEC and pass values to TEXEC_MEM
		int zeroFlag = 0;
		Integer aluResult = new Integer(0);
		if (ID_EXEC.get("type").equals(R)) {
			TEXEC_MEM.put("writeBackRegister", ID_EXEC.get("rdAdress"));
			Integer instruction = ID_EXEC.get("instruction");
			Integer function = Integer.parseInt(toNString(instruction, 32)
					.substring(26, 32), 2);
			Integer shiftAmount = Integer.parseInt(toNString(instruction, 32)
					.substring(21, 26), 2);
			switch (function) {
			case 32: // add
				aluResult = ID_EXEC.get("rsContent") + ID_EXEC.get("rtContent");
				break;
			case 34: // sub
				aluResult = ID_EXEC.get("rsContent") - ID_EXEC.get("rtContent");
				break;
			case 0: // sll
				aluResult = ID_EXEC.get("rtContent") << shiftAmount;
				break;
			case 2: // srl (division for unsigned numbers done, what about
					// signed numbers ?)
				aluResult = ID_EXEC.get("rtContent") >> shiftAmount;
				break;
			case 36: // and
				aluResult = ID_EXEC.get("rsContent") & ID_EXEC.get("rtContent");
				break;
			case 37: // or
				aluResult = ID_EXEC.get("rsContent") | ID_EXEC.get("rtContent");
				break;
			case 39: // nor
				aluResult = ~(ID_EXEC.get("rsContent") | ID_EXEC
						.get("rtContent"));
				break;
			case 42: // slt
				aluResult = ID_EXEC.get("rsContent") < ID_EXEC.get("rtContent") ? 1
						: 0;
				break;
			case 43: // sltu
				aluResult = Long.parseLong(
						Integer.toBinaryString(ID_EXEC.get("rsContent")), 2) < Long
						.parseLong(Integer.toBinaryString(ID_EXEC
								.get("rtContent")), 2) ? 1 : 0;
				break;
			case 8: // jr
				programCounter = ID_EXEC.get("rsContent");
				break;

			}
		} else if (ID_EXEC.get("type").equals(B)) {
			if (ID_EXEC.get("equal") == 0) { // bne
				if (ID_EXEC.get("rsContent") - ID_EXEC.get("rtContent") != 0)
					zeroFlag = 1;
				else
					zeroFlag = 0;
			} else {
				if (ID_EXEC.get("rsContent") - ID_EXEC.get("rtContent") == 0) // beq
					zeroFlag = 1;
				else
					zeroFlag = 0;
			}
			TEXEC_MEM.put("zeroFlag", zeroFlag);
			TEXEC_MEM.put("immediateField", ID_EXEC.get("immediateField"));

		} else if (ID_EXEC.get("type").equals(I)) {
			Integer instruction = ID_EXEC.get("instruction");
			Integer opcode = Integer.parseInt(toNString(instruction, 32)
					.substring(0, 6), 2);

			switch (opcode) {
			case 8: // addi
				aluResult = ID_EXEC.get("rsContent")
						+ ID_EXEC.get("immediateField");
				break;
			case 12: // andi
				aluResult = ID_EXEC.get("rsContent")
						& ID_EXEC.get("immediateField");
				break;
			case 13: // ori
				aluResult = ID_EXEC.get("rsContent")
						| ID_EXEC.get("immediateField");
				break;
			}
			TEXEC_MEM.put("writeBackRegister", ID_EXEC.get("rtAdress"));
		} else if (ID_EXEC.get("type").equals(SW)) {
			aluResult = ID_EXEC.get("immediateField")
					+ ID_EXEC.get("rsContent");
			TEXEC_MEM.put("writeData", ID_EXEC.get("rtContent"));
			TEXEC_MEM.put("aluResult", aluResult);
		} else if (ID_EXEC.get("type").equals(LW)) {
			aluResult = ID_EXEC.get("immediateField")
					+ ID_EXEC.get("rsContent");
			TEXEC_MEM.put("aluResult", aluResult);
			TEXEC_MEM.put("writeBackRegister", ID_EXEC.get("rtAdress"));
		} else if (ID_EXEC.get("type").equals(J)) {
			Integer instruction = ID_EXEC.get("instruction");
			Integer opcode = Integer.parseInt(toNString(instruction, 32)
					.substring(0, 6), 2);
			Integer jumpAddress = Integer.parseInt(toNString(instruction, 32)
					.substring(6, 32), 2);

			if (opcode == 3)
				registers.put(31, ID_EXEC.get("programCounter") + 1);

			// programCounter = decrypt(jumpAddress);
			programCounter = (jumpAddress);

		} else {
			TEXEC_MEM.put("writeBackRegister", ID_EXEC.get("rtAdress"));
		}

		// TEXEC_MEM.put("writeData", registers.get("rtContent"));
		// TEXEC_MEM.put("zeroFlag",zeroFlag);
		TEXEC_MEM.put("MemWrite", ID_EXEC.get("MemWrite"));
		TEXEC_MEM.put("MemRead", ID_EXEC.get("MemRead"));
		TEXEC_MEM.put("Branch", ID_EXEC.get("Branch"));
		TEXEC_MEM.put("MemtoReg", ID_EXEC.get("MemtoReg"));
		TEXEC_MEM.put("RegWrite", ID_EXEC.get("RegWrite"));
		TEXEC_MEM.put("aluResult", aluResult);
	}

	private int decrypt(Integer integer) {
		return integer;
		// TODO Auto-generated method stub

	}

	public void memoryAccess() {

		if (TMEM_WB == null)
			TMEM_WB = new HashMap<String, Integer>();

		// use values from EXEC_MEM and pass values to TMEM_WB

		// Howa el ALU Address da mafrood ne7awelo l type mo3ayan ?!
		// Eh nezam el types kolaha 3amatan :D

		Integer aluResult = EXEC_MEM.get("aluResult"); // Address to write or
														// load from memory
		Integer writeData = EXEC_MEM.get("writeData"); // Data to be written
		Integer MemRead = EXEC_MEM.get("MemRead");
		Integer MemWrite = EXEC_MEM.get("MemWrite");
		Integer branch = EXEC_MEM.get("Branch");
		Integer zeroFlag = EXEC_MEM.get("zeroFlag");

		if (zeroFlag == 1 && branch == 1) {
			programCounter += EXEC_MEM.get("immediateField");
		}
		if (MemRead.intValue() == 1) {
			TMEM_WB.put("memoryValue", dataMemory[aluResult]);
		}
		if (MemWrite.intValue() == 1) {
			dataMemory[aluResult] = writeData;
		}

		TMEM_WB.put("aluResult", aluResult);
		TMEM_WB.put("writeBackRegister", EXEC_MEM.get("writeBackRegister"));
		TMEM_WB.put("RegWrite", EXEC_MEM.get("RegWrite"));
		TMEM_WB.put("MemtoReg", EXEC_MEM.get("MemtoReg"));

	}

	public void writeBack() {
		// handle writing in zero register

		if (MEM_WB.get("RegWrite") == 1) {

			if (MEM_WB.get("writeBackRegister") == 0) {
				try {
					throw new Exception("Can not write data into register ZERO");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			else {
				if (MEM_WB.get("MemtoReg") == 0)
					registers.put(MEM_WB.get("writeBackRegister"),
							MEM_WB.get("memoryValue"));
				else
					registers.put(MEM_WB.get("writeBackRegister"),
							MEM_WB.get("aluResult"));
			}

		}
	}

	public void stall(int shiftamount) {

		int beginindex;
		beginindex = programCounter;// the newly incremented one
		boolean found = false;

		for (int i = instructionMemory.length - 1; i >= beginindex; i--) {
			if (!found) {
				if (instructionMemory[i] != null) {
					found = true;
				}
			}

			if (found) {

				instructionMemory[i + shiftamount] = instructionMemory[i];
				instructionMemory[i] = null;
			}

		}

	}

	public static void main(String[] args) {
		/*
		 * Datapath d = new Datapath(); d.init(); while(d.clockCycle());
		 * 
		 * 
		 * Integer x = 21; int y = x&0b00111;
		 * System.out.println(Integer.toBinaryString(y));
		 * System.out.println(21==0b10101?"yes":"no");
		 * 
		 * 
		 * for (Integer i = 0; i < 32; i++) { String
		 * acc=Integer.toBinaryString(i); while(acc.length()<5) acc="0"+acc;
		 * System.out.println("registers.put(\""+acc+"\", 0);"); }
		 * 
		 * 
		 * String x = "1001"; System.out.println(Integer.parseInt(x, 2));
		 * 
		 * 
		 * for (int i = 0; i < 10; i++) { if(i==7) break; }
		 * System.out.println(i);
		 * 
		 * 
		 * Integer[] instructionMemory = new Integer[16];
		 * instructionMemory[0]=5; instructionMemory[1]=5;
		 * instructionMemory[2]=5; instructionMemory[3]=5;
		 * instructionMemory[4]=5; instructionMemory[5]=5;
		 * instructionMemory[6]=5; instructionMemory[7]=5;
		 * instructionMemory[8]=5;
		 * instructionMemory[9]=5;instructionMemory[10]=5;
		 * 
		 * int programCounter = 5; int shiftamount=2; int beginindex,endindex;
		 * beginindex=programCounter;//the newly incremented one boolean
		 * found=false; for (int i = 0; i < instructionMemory.length; i++)
		 * System.out.print(instructionMemory[i]+" ");
		 * 
		 * 
		 * 
		 * for (int i = instructionMemory.length-1; i >= beginindex; i--) {
		 * if(!found) if(instructionMemory[i]!=null){ endindex=i;found=true; }
		 * 
		 * 
		 * if(found){
		 * 
		 * instructionMemory[i+shiftamount]=instructionMemory[i];
		 * instructionMemory[i]=null; }
		 * 
		 * }
		 * 
		 * 
		 * 
		 * 
		 * System.out.println(); for (int i = 0; i < instructionMemory.length;
		 * i++)
		 * 
		 * System.out.print(instructionMemory[i]+" "); //String x =
		 * Integer.toBinaryString(-7);
		 */// System.out.println(Integer.parseInt(x,2));
			// System.out.println(Long.parseLong(Integer.toBinaryString(-7),2));

		Datapath dt = new Datapath();
		dt.init();
		dt.registers.put(5, 4);
		// dt.registers.put(5, 2000);
		// dt.registers.put(9, 1);
		// dt.registers.put(10, 1);
		// dt.registers.put(11, 1);
		// dt.registers.put(12, 1);
		// dt.registers.put(13, 1);
		// dt.registers.put(31, 9);
		dt.dataMemory[4] = 3030;
		try {
			compile(new File("code.txt"));
			BufferedReader br = new BufferedReader(new FileReader(new File(
					"binary.txt")));
			int i = 0;
			while (br.ready()) {
				dt.instructionMemory[i] = Integer.parseInt(br.readLine(), 2);
				i++;
			}
		} catch (CompileError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		dt.Display();

		for (int i = 0; i < dt.registers.size(); i++)
			try {
				System.out.print(reg(i) + " " + dt.registers.get(i) + " || ");
			} catch (CompileError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		System.out.println("\n" + dt.dataMemory[4]);

	}

	public void Display() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input;
		try {

			boring: while (!(input = br.readLine()).equals("q")) {

				if (input.equals("ff")) {
					while (clockCycle())
						;
					break boring;
				}

				if (input.equals("n")) {

					clockCycle();

					DisplayRegisterContent(IF_ID, "IF_ID");
					DisplayRegisterContent(ID_EXEC, "ID_EXEC");
					DisplayRegisterContent(EXEC_MEM, "EXEC_MEM");
					DisplayRegisterContent(MEM_WB, "MEM_WB");
					DisplayRegisters();
				}

			}
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	private void DisplayRegisters() {
		System.out.println("Registers");
		for (int i = 0; i < 32; i++) {
			try {
				System.out.println(reg(i) + " : " + registers.get(i));
			} catch (CompileError e) {

				e.printStackTrace();
			}
		}

	}

	private void DisplayRegisterContent(HashMap<String, Integer> register,
			String name) {
		if (register == null)
			System.out.println(name + " : Empty \n");
		else {
			System.out.println(name);
			Iterator<String> i = register.keySet().iterator();

			while (i.hasNext()) {
				String key = i.next();
				System.out.println(key + " : " + register.get(key));
			}
			System.out.println();
		}
	}

	public static File compile(File file) throws CompileError {
		try {
			String label = "";
			int count;
			int org = 1;
			boolean change = false;
			BufferedReader brLabel = new BufferedReader(new FileReader(file));
			BufferedWriter bwLabel = new BufferedWriter(new FileWriter(
					"withoutLabels.txt"));
			if (brLabel.ready()) {
				label = brLabel.readLine();
				String[] values = label.split(":");
				if (values[0].toLowerCase().equals("org")) {
					org = Integer.parseInt(values[1]);
					change = true;
				}

			}
			count = org;
			while (brLabel.ready()) {
				if (change)
					label = brLabel.readLine();
				String lineWithoutLabel = label;
				if (label.contains(":")) {
					lineWithoutLabel = label.substring(label.indexOf(":") + 2);
					label = label.split(":")[0];
					labels.put(label, count);
				}
				bwLabel.write(lineWithoutLabel);
				bwLabel.newLine();
				// flush position ?
				bwLabel.flush();
				count++;
			}
			bwLabel.close();
			brLabel.close();
			BufferedReader br = new BufferedReader(new FileReader(
					"withoutLabels.txt"));
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					"binary.txt")));
			count = 1;
			while (br.ready()) {
				boolean branch = false;
				String write = "";
				int function = 0;
				int shamt = 0;
				int imm = 0;
				String line[] = br.readLine().split(" ");
				char format;
				int opcode;
				switch (line[0]) {
				case "add":
					opcode = 0b0;
					format = 'r';
					function = 0x20;
					break;
				case "addi":
					opcode = 0x08;
					format = 'i';
					break;
				case "sub":
					opcode = 0b0;
					format = 'r';
					function = 0x22;
					break;
				case "sw":
					opcode = 0x0F;
					format = 'i';
					imm = Integer.parseInt(line[2].split("\\(")[0]);
					line[2] = line[2].split("\\(")[1].substring(0,
							line[2].split("\\(")[1].length() - 1);
					branch = true;
					break;
				case "lw":
					opcode = 0x0E;
					format = 'i';
					imm = Integer.parseInt(line[2].split("\\(")[0]);
					line[2] = line[2].split("\\(")[1].substring(0,
							line[2].split("\\(")[1].length() - 1);
					branch = true;
					break;
				case "sll":
					opcode = 0;
					format = 'r';
					function = 0x0;
					shamt = Integer.parseInt(line[3]);
					break;
				case "srl":
					opcode = 0x0;
					format = 'r';
					function = 0x2;
					shamt = Integer.parseInt(line[3]);
					break;
				case "and":
					opcode = 0x0;
					format = 'r';
					function = 0x24;
					break;
				case "andi":
					opcode = 0x0C;
					format = 'i';
					break;
				case "or":
					opcode = 0x0;
					format = 'r';
					function = 0x25;
					break;
				case "ori":
					opcode = 0x0D;
					format = 'i';
					break;
				case "nor":
					opcode = 0x0;
					format = 'r';
					function = 0x27;
					break;
				case "beq":
					opcode = 0x04;
					format = 'i';
					imm = labels.get(line[3]) - count;
					branch = true;
					break;
				case "bne":
					opcode = 0x05;
					format = 'i';
					imm = labels.get(line[3]) - count;
					branch = true;
					break;
				case "j":
					opcode = 0x02;
					format = 'j';
					break;
				case "jal":
					opcode = 0x03;
					format = 'j';
					break;
				case "jr":
					opcode = 0x0;
					format = 'r';
					function = 0x08;
					break;
				case "slt":
					opcode = 0x0;
					format = 'r';
					function = 0x2A;
					break;
				case "sltu":
					opcode = 0x0;
					format = 'r';
					function = 0x2B;
					break;
				default:
					throw new CompileError("unknown instruction : " + line[0]);
				}
				write += complete(opcode, 6);
				if (format != 'j') {
					int rt;
					int rs;
					if (function == 8) {
						switch (line[1]) {
						case "$zero":
							rs = 0;
							break;
						case "$at":
							rs = 1;
							break;
						case "$v0":
							rs = 2;
							break;
						case "$v1":
							rs = 3;
							break;
						case "$a0":
							rs = 4;
							break;
						case "$a1":
							rs = 5;
							break;
						case "$a2":
							rs = 6;
							break;
						case "$a3":
							rs = 7;
							break;
						case "$t0":
							rs = 8;
							break;
						case "$t1":
							rs = 9;
							break;
						case "$t2":
							rs = 10;
							break;
						case "$t3":
							rs = 11;
							break;
						case "$t4":
							rs = 12;
							break;
						case "$t5":
							rs = 13;
							break;
						case "$t6":
							rs = 14;
							break;
						case "$t7":
							rs = 15;
							break;
						case "$s0":
							rs = 16;
							break;
						case "$s1":
							rs = 17;
							break;
						case "$s2":
							rs = 18;
							break;
						case "$s3":
							rs = 19;
							break;
						case "$s4":
							rs = 20;
							break;
						case "$s5":
							rs = 21;
							break;
						case "$s6":
							rs = 22;
							break;
						case "$s7":
							rs = 23;
							break;
						case "$t8":
							rs = 24;
							break;
						case "$t9":
							rs = 25;
							break;
						case "$k0":
							rs = 26;
							break;
						case "$k1":
							rs = 27;
							break;
						case "$gp":
							rs = 28;
							break;
						case "$sp":
							rs = 29;
							break;
						case "$fp":
							rs = 30;
							break;
						case "$ra":
							rs = 31;
							break;
						default:
							throw new CompileError("unvalid register : "
									+ line[1]);
						}
						write += complete(rs, 5) + complete(0, 5)
								+ complete(0, 5) + complete(0, 5)
								+ complete(function, 6);
					} else {
						switch (line[2]) {
						case "$zero":
							rs = 0;
							break;
						case "$at":
							rs = 1;
							break;
						case "$v0":
							rs = 2;
							break;
						case "$v1":
							rs = 3;
							break;
						case "$a0":
							rs = 4;
							break;
						case "$a1":
							rs = 5;
							break;
						case "$a2":
							rs = 6;
							break;
						case "$a3":
							rs = 7;
							break;
						case "$t0":
							rs = 8;
							break;
						case "$t1":
							rs = 9;
							break;
						case "$t2":
							rs = 10;
							break;
						case "$t3":
							rs = 11;
							break;
						case "$t4":
							rs = 12;
							break;
						case "$t5":
							rs = 13;
							break;
						case "$t6":
							rs = 14;
							break;
						case "$t7":
							rs = 15;
							break;
						case "$s0":
							rs = 16;
							break;
						case "$s1":
							rs = 17;
							break;
						case "$s2":
							rs = 18;
							break;
						case "$s3":
							rs = 19;
							break;
						case "$s4":
							rs = 20;
							break;
						case "$s5":
							rs = 21;
							break;
						case "$s6":
							rs = 22;
							break;
						case "$s7":
							rs = 23;
							break;
						case "$t8":
							rs = 24;
							break;
						case "$t9":
							rs = 25;
							break;
						case "$k0":
							rs = 26;
							break;
						case "$k1":
							rs = 27;
							break;
						case "$gp":
							rs = 28;
							break;
						case "$sp":
							rs = 29;
							break;
						case "$fp":
							rs = 30;
							break;
						case "$ra":
							rs = 31;
							break;
						default:
							throw new CompileError("unvalid register : "
									+ line[2]);
						}
						if (format == 'r') {
							if (line[3].charAt(0) == '$') {
								switch (line[3]) {
								case "$zero":
									rt = 0;
									break;
								case "$at":
									rt = 1;
									break;
								case "$v0":
									rt = 2;
									break;
								case "$v1":
									rt = 3;
									break;
								case "$a0":
									rt = 4;
									break;
								case "$a1":
									rt = 5;
									break;
								case "$a2":
									rt = 6;
									break;
								case "$a3":
									rt = 7;
									break;
								case "$t0":
									rt = 8;
									break;
								case "$t1":
									rt = 9;
									break;
								case "$t2":
									rt = 10;
									break;
								case "$t3":
									rt = 11;
									break;
								case "$t4":
									rt = 12;
									break;
								case "$t5":
									rt = 13;
									break;
								case "$t6":
									rt = 14;
									break;
								case "$t7":
									rt = 15;
									break;
								case "$s0":
									rt = 16;
									break;
								case "$s1":
									rt = 17;
									break;
								case "$s2":
									rt = 18;
									break;
								case "$s3":
									rt = 19;
									break;
								case "$s4":
									rt = 20;
									break;
								case "$s5":
									rt = 21;
									break;
								case "$s6":
									rt = 22;
									break;
								case "$s7":
									rt = 23;
									break;
								case "$t8":
									rt = 24;
									break;
								case "$t9":
									rt = 25;
									break;
								case "$k0":
									rt = 26;
									break;
								case "$k1":
									rt = 27;
									break;
								case "$gp":
									rt = 28;
									break;
								case "$sp":
									rt = 29;
									break;
								case "$fp":
									rt = 30;
									break;
								case "$ra":
									rt = 31;
									break;
								default:
									throw new CompileError(
											"unvalid register : " + line[3]);
								}
							} else {
								rt = Integer.parseInt(line[3]);
							}
							int rd;
							switch (line[1]) {
							case "$zero":
								rd = 0;
								break;
							case "$at":
								rd = 1;
								break;
							case "$v0":
								rd = 2;
								break;
							case "$v1":
								rd = 3;
								break;
							case "$a0":
								rd = 4;
								break;
							case "$a1":
								rd = 5;
								break;
							case "$a2":
								rd = 6;
								break;
							case "$a3":
								rd = 7;
								break;
							case "$t0":
								rd = 8;
								break;
							case "$t1":
								rd = 9;
								break;
							case "$t2":
								rd = 10;
								break;
							case "$t3":
								rd = 11;
								break;
							case "$t4":
								rd = 12;
								break;
							case "$t5":
								rd = 13;
								break;
							case "$t6":
								rd = 14;
								break;
							case "$t7":
								rd = 15;
								break;
							case "$s0":
								rd = 16;
								break;
							case "$s1":
								rd = 17;
								break;
							case "$s2":
								rd = 18;
								break;
							case "$s3":
								rd = 19;
								break;
							case "$s4":
								rd = 20;
								break;
							case "$s5":
								rd = 21;
								break;
							case "$s6":
								rd = 22;
								break;
							case "$s7":
								rd = 23;
								break;
							case "$t8":
								rd = 24;
								break;
							case "$t9":
								rd = 25;
								break;
							case "$k0":
								rd = 26;
								break;
							case "$k1":
								rd = 27;
								break;
							case "$gp":
								rd = 28;
								break;
							case "$sp":
								rd = 29;
								break;
							case "$fp":
								rd = 30;
								break;
							case "$ra":
								rd = 31;
								break;
							default:
								throw new CompileError("unvalid register : "
										+ line[1]);
							}

							if (line[0].equals("sll") || line[0].equals("srl")) {
								rt = rs;
								rs = 0;
							}
							write += complete(rs, 5) + complete(rt, 5)
									+ complete(rd, 5) + complete(shamt, 5)
									+ complete(function, 6);
						} else {
							if (format == 'i') {
								switch (line[1]) {
								case "$zero":
									rt = 0;
									break;
								case "$at":
									rt = 1;
									break;
								case "$v0":
									rt = 2;
									break;
								case "$v1":
									rt = 3;
									break;
								case "$a0":
									rt = 4;
									break;
								case "$a1":
									rt = 5;
									break;
								case "$a2":
									rt = 6;
									break;
								case "$a3":
									rt = 7;
									break;
								case "$t0":
									rt = 8;
									break;
								case "$t1":
									rt = 9;
									break;
								case "$t2":
									rt = 10;
									break;
								case "$t3":
									rt = 11;
									break;
								case "$t4":
									rt = 12;
									break;
								case "$t5":
									rt = 13;
									break;
								case "$t6":
									rt = 14;
									break;
								case "$t7":
									rt = 15;
									break;
								case "$s0":
									rt = 16;
									break;
								case "$s1":
									rt = 17;
									break;
								case "$s2":
									rt = 18;
									break;
								case "$s3":
									rt = 19;
									break;
								case "$s4":
									rt = 20;
									break;
								case "$s5":
									rt = 21;
									break;
								case "$s6":
									rt = 22;
									break;
								case "$s7":
									rt = 23;
									break;
								case "$t8":
									rt = 24;
									break;
								case "$t9":
									rt = 25;
									break;
								case "$k0":
									rt = 26;
									break;
								case "$k1":
									rt = 27;
									break;
								case "$gp":
									rt = 28;
									break;
								case "$sp":
									rt = 29;
									break;
								case "$fp":
									rt = 30;
									break;
								case "$ra":
									rt = 31;
									break;
								default:
									throw new CompileError(
											"unvalid register : " + line[1]);

								}
								if (!branch)
									imm = Integer.parseInt(line[3]);
								write += complete(rs, 5) + complete(rt, 5)
										+ complete(imm, 16);
							}
						}
					}
				} else {
					write += complete(labels.get(line[1]), 26);
				}
				bw.write(write);
				bw.newLine();
				write = "";
				bw.flush();
				count++;
			}
			br.close();
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static String reg(int i) throws CompileError {
		switch (i) {
		case 0:
			return "$zero";
		case 1:
			return "$at";
		case 2:
			return "$v0";
		case 3:
			return "$v1";
		case 4:
			return "$a0";
		case 5:
			return "$a1";
		case 6:
			return "$a2";
		case 7:
			return "$a3";
		case 8:
			return "$t0";
		case 9:
			return "$t1";
		case 10:
			return "$t2";
		case 11:
			return "$t3";
		case 12:
			return "$t4";
		case 13:
			return "$t5";
		case 14:
			return "$t6";
		case 15:
			return "$t7";
		case 16:
			return "$s0";
		case 17:
			return "$s1";
		case 18:
			return "$s2";
		case 19:
			return "$s3";
		case 20:
			return "$s4";
		case 21:
			return "$s5";
		case 22:
			return "$s6";
		case 23:
			return "$s7";
		case 24:
			return "$t8";
		case 25:
			return "$t9";
		case 26:
			return "$k0";
		case 27:
			return "$k1";
		case 28:
			return "$gp";
		case 29:
			return "$sp";
		case 30:
			return "$fp";
		case 31:
			return "$ra";
		default:
			throw new CompileError("unvalid register : " + i);
		}
	}

	public static String complete(int binary, int numberOfBitsReq) {
		String result = Integer.toBinaryString(binary);
		if (binary >= 0)
			while (result.length() < numberOfBitsReq)
				result = "0" + result;
		else
			result = result.substring(32 - numberOfBitsReq);
		return result;
	}

	public HashMap<Integer, Integer> fillReg(File file) {
		HashMap<Integer, Integer> regs = new HashMap<Integer, Integer>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			regs.put(0, 0);
			regs.put(1, 0);
			regs.put(2, 0);
			regs.put(3, 0);
			regs.put(4, 0);
			regs.put(5, 0);
			regs.put(6, 0);
			regs.put(7, 0);
			regs.put(8, 0);
			regs.put(9, 0);
			regs.put(10, 0);
			regs.put(11, 0);
			regs.put(12, 0);
			regs.put(13, 0);
			regs.put(14, 0);
			regs.put(15, 0);
			regs.put(16, 0);
			regs.put(17, 0);
			regs.put(18, 0);
			regs.put(19, 0);
			regs.put(20, 0);
			regs.put(21, 0);
			regs.put(22, 0);
			regs.put(23, 0);
			regs.put(24, 0);
			regs.put(25, 0);
			regs.put(26, 0);
			regs.put(27, 0);
			regs.put(28, 0);
			regs.put(29, 0);
			regs.put(30, 0);
			regs.put(31, 0);
			while (br.ready()) {
				String[] values = br.readLine().split(":");
				switch (values[0]) {
				case "$at":
					regs.put(1, Integer.parseInt(values[1]));
					break;
				case "$v0":
					regs.put(2, Integer.parseInt(values[1]));
					break;
				case "$v1":
					regs.put(3, Integer.parseInt(values[1]));
					break;
				case "$a0":
					regs.put(4, Integer.parseInt(values[1]));
					break;
				case "$a1":
					regs.put(5, Integer.parseInt(values[1]));
					break;
				case "$a2":
					regs.put(6, Integer.parseInt(values[1]));
					break;
				case "$a3":
					regs.put(7, Integer.parseInt(values[1]));
					break;
				case "$t0":
					regs.put(8, Integer.parseInt(values[1]));
					break;
				case "$t1":
					regs.put(9, Integer.parseInt(values[1]));
					break;
				case "$t2":
					regs.put(10, Integer.parseInt(values[1]));
					break;
				case "$t3":
					regs.put(11, Integer.parseInt(values[1]));
					break;
				case "$t4":
					regs.put(12, Integer.parseInt(values[1]));
					break;
				case "$t5":
					regs.put(13, Integer.parseInt(values[1]));
					break;
				case "$t6":
					regs.put(14, Integer.parseInt(values[1]));
					break;
				case "$t7":
					regs.put(15, Integer.parseInt(values[1]));
					break;
				case "$s0":
					regs.put(16, Integer.parseInt(values[1]));
					break;
				case "$s1":
					regs.put(17, Integer.parseInt(values[1]));
					break;
				case "$s2":
					regs.put(18, Integer.parseInt(values[1]));
					break;
				case "$s3":
					regs.put(19, Integer.parseInt(values[1]));
					break;
				case "$s4":
					regs.put(20, Integer.parseInt(values[1]));
					break;
				case "$s5":
					regs.put(21, Integer.parseInt(values[1]));
					break;
				case "$s6":
					regs.put(22, Integer.parseInt(values[1]));
					break;
				case "$s7":
					regs.put(23, Integer.parseInt(values[1]));
					break;
				case "$t8":
					regs.put(24, Integer.parseInt(values[1]));
					break;
				case "$t9":
					regs.put(25, Integer.parseInt(values[1]));
					break;
				case "$k0":
					regs.put(26, Integer.parseInt(values[1]));
					break;
				case "$k1":
					regs.put(27, Integer.parseInt(values[1]));
					break;
				case "$gp":
					regs.put(28, Integer.parseInt(values[1]));
					break;
				case "$sp":
					regs.put(29, Integer.parseInt(values[1]));
					break;
				case "$fp":
					regs.put(30, Integer.parseInt(values[1]));
					break;
				case "$ra":
					regs.put(31, Integer.parseInt(values[1]));
					break;
				default:
					throw new CompileError("unvalid register : " + values[0]);
				}
			}
			return regs;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CompileError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return regs;
	}
}
