
public class test {

	public test() {
		// TODO Auto-generated constructor stub
	}
	public static void main (String [] args){
		String number = Integer.toBinaryString(-7).substring(32-8);
		
//		while(x.length()<8)
//			x='1'+x;
//		 int y = ((byte) Integer.parseInt(x, 2));
//		System.out.println(y);
		
		
char bit=number.charAt(0);
		System.out.println(Integer.toBinaryString(-1));
		if(bit=='1'){
			String complement="";
			for (int i = 0; i < number.length(); i++) {
				complement+=number.charAt(i)=='0'?'1':'0';
			}
			int bc=Integer.parseInt(complement, 2) + 0b1;
			System.out.println(-bc);
		}
		else System.out.println(Integer.parseInt(number,2));
		
		
	}

}
