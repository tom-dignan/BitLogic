package cas;
import java.util.ArrayList;

public class Interpreter extends QuickMath{
	
	public static Expr SUCCESS = var("done!");
	
	public static Expr createExpr(String string,Defs defs,Settings settings){
		string = string.replaceAll(" ", "");//remove spaces
		try {
			ArrayList<String> tokens = generateTokens(string);
			return createExprFromTokens(tokens,defs,settings);
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Expr createExprWithThrow(String string,Defs defs,Settings settings) throws Exception{
		string = string.replaceAll(" ", "");//remove spaces
		ArrayList<String> tokens = generateTokens(string);
		return createExprFromTokens(tokens,defs,settings);
	}
	
	public static Expr createExprWithThrow(String string) throws Exception {
		return createExprWithThrow(string,Defs.blank,Settings.normal);
	}
	
	public static Expr createExpr(String string) {
		return createExpr(string,Defs.blank,Settings.normal);
	}
	
	static Expr createExprFromToken(String token,Defs defs,Settings settings) throws Exception {
		ArrayList<String> tokens = new ArrayList<String>();
		tokens.add(token);
		return createExprFromTokens(tokens,defs,settings);
	}
	
	static void errors(ArrayList<String> tokens) throws Exception {
		//System.out.println(tokens);
		if(tokens == null) throw new Exception("missing tokens");
		
		String initToken = tokens.get(0);
		if(initToken.equals("+") || initToken.equals("*") || initToken.equals("/") || initToken.equals("^") || initToken.equals(",")) throw new Exception("starting with invalid token");//should not start with any of these
		
		String lastToken = tokens.get(tokens.size()-1);
		if(lastToken.equals("+") || lastToken.equals("-") || lastToken.equals("*") || lastToken.equals("/") || lastToken.equals("^") || lastToken.equals(",")) throw new Exception("ending with invalid token");//should not end with any of these
		
	}
	
	public static boolean isOperator(String string) {
		return string.matches("[+\\-*/^,=><!;:\\~\\&\\|\\[\\]\\{\\}\\(\\)]")&&!string.contains(" ");
	}
	
	public static boolean isProbablyExpr(String string) {
		return string.matches("pi|i|e|.*[(0-9)(+\\-*/^,=><!;:\\~\\&\\|\\[\\]\\{\\}\\(\\))].*")&&!string.contains(" ");
	}
	
	public static boolean containsOperators(String string) {
		return string.matches(".*[+\\-*/^,=><!;:\\~\\&\\|\\[\\]\\{\\}\\(\\)].*")&&!string.contains(" ");
	}
	
	static void printTokens(ArrayList<String> tokens) {//for debugging
		for(int i = 0;i<tokens.size();i++) {
			System.out.print(tokens.get(i));
			System.out.print('\\');
		}
		System.out.println();
	}
	
	static Expr createExprFromTokens(ArrayList<String> tokens,Defs defs,Settings settings) throws Exception{
		
		errors(tokens);
		
		//System.out.println(tokens);
		
		if(tokens.size() == 1) {
			String string = tokens.get(0);
			if(string.isEmpty()) return null;
			Expr num = null;
			if(string.matches("[(0-9)(.)]+")){
				try {
					if(string.contains(".")) num = floatExpr(string);
					else num = num(string);
				}catch(Exception e) {}
			}
			
			if(num != null){
				return num;
			}else if(!containsOperators(string)){
				String lowered = string.toLowerCase();
				if(string.equals("i")) return num(0,1);
				else if(lowered.equals("true")) return bool(true);
				else if(lowered.equals("false")) return bool(false);
				else{//variables
					return var(string);
				}
			}else if(string.charAt(0) == '[') {
				if(string.equals("[]")) return new ExprList();
				return ExprList.cast(createExprFromToken(string.substring(1, string.length()-1),defs,settings));
			}else if(string.charAt(0) == '{') {
				if(string.equals("{}")) return new Script();
				Script script = Script.cast(createExprFromToken(string.substring(1, string.length()-1),defs,settings));
				return script;
			}else {
				return createExprFromTokens(generateTokens(tokens.get(0)),defs,settings);
			}
		}else if(tokens.size() == 2) {
			if(tokens.get(0).equals("-")) {
				Expr expr = createExpr(tokens.get(1));
				if(expr instanceof Num) return ((Num) expr).negate();
				return neg(createExpr(tokens.get(1),defs,settings));
			}else if(!tokens.get(1).equals("!")){
				String op = tokens.get(0);
				if(op.isBlank()) throw new Exception("confusing parenthesis");
				Expr params = ExprList.cast( createExprFromToken(tokens.get(1),defs,settings));
				
				if(op.equals("define")) {
					Equ def = (Equ)params.get(0);
					
					if(def.getLeftSide() instanceof Func) {
						Func f = new Func(def.getLeftSide().typeName());
						
						f.ruleSequence.add(new Rule(def,"function definition",Rule.EASY));
						
						defs.addFunc(f);
					}else {
						defs.addVar(def);
					}
					
					return var("done");
				}
				if(op.equals("defs")) {
					return var(defs.toString());
				}
				
				if(!op.equals("~")){
					Expr[] paramsArray = new Expr[params.size()];
					
					for(int i = 0;i<params.size();i++) {
						paramsArray[i] = params.get(i);
					}
					
					try {
						Expr f = SimpleFuncs.getFuncByName(op,defs,paramsArray);
						return f;
					}catch(Exception e) {
						//throw new Exception("error with parameters");
						if(e instanceof java.lang.ArrayIndexOutOfBoundsException) {
							throw new Exception("wrong number of parameters");
						}
						throw new Exception("wrong type in parameters");
					}
				}
				
			}
		}
		boolean isSum = false,isProd = false,isList = false,isFactorial = false,isScript = false,isAnd = false,isOr = false,isNot = false,isEqu = false;
		int indexOfPowToken = -1,equCount = 0;
		boolean lastWasOperator = false;
		for(int i = 0;i<tokens.size();i++) {
			String token = tokens.get(i);
			
			if(token.equals(",")) {
				isList = true;
				lastWasOperator = true;
			}else if(token.equals("=") || token.equals(">") || token.equals("<")) {
				isEqu = true;
				equCount++;
				lastWasOperator = true;
			}else if(token.equals("+") || (token.equals("-") && !lastWasOperator)) {//the last operator check avoids error of misinterpreting x*-2*5 as a sum
				isSum = true;
				lastWasOperator = true;
			}else if(token.equals("*") || token.equals("/")) {
				isProd = true;
				lastWasOperator = true;
			}else if(token.equals("^")) {
				if(indexOfPowToken == -1) indexOfPowToken = i;
				lastWasOperator = true;
			}else if(token.equals("!")) {
				isFactorial = true;
				lastWasOperator = true;
			}else if(token.equals(";")) {
				isScript = true;
				lastWasOperator = true;
			}else if(token.equals("&")){
				isAnd = true;
				lastWasOperator = true;
			}else if(token.equals("|")){
				isOr = true;
				lastWasOperator = true;
			}else if(token.equals("~")){
				isNot = true;
				lastWasOperator = true;
			}else {
				lastWasOperator = false;
			}
		}
		if(isScript) {
			
			Script scr = new Script();
			int indexOfLastComma = 0;
			
			for(int i = 0;i<tokens.size();i++) {
				String token = tokens.get(i);
				if(token.equals(";")) {
					
					ArrayList<String> tokenSet = new ArrayList<String>();
					for(int j = indexOfLastComma;j<i;j++)  tokenSet.add(tokens.get(j));
					scr.add(createExprFromTokens(tokenSet,defs,settings));
					indexOfLastComma = i+1;
				}
			}
			
			return scr;
			
		}else if(isList) {
			tokens.add(",");
			ExprList list = new ExprList();
			int indexOfLastComma = 0;
			
			for(int i = 0;i<tokens.size();i++) {
				String token = tokens.get(i);
				if(token.equals(",")) {
					
					ArrayList<String> tokenSet = new ArrayList<String>();
					for(int j = indexOfLastComma;j<i;j++)  tokenSet.add(tokens.get(j));
					list.add(createExprFromTokens(tokenSet,defs,settings));
					indexOfLastComma = i+1;
				}
			}
			
			return list;
			
		}else if(isEqu) {//is equation
			
			int indexOfEquToken = 0;
			int currentCount = 0;
			
			for(int i = 0; i < tokens.size();i++) {
				String token = tokens.get(i);
				if(token.equals("=") || token.equals(">") || token.equals("<")) {
					currentCount++;
				}
				if(currentCount == equCount/2+1 ) {
					indexOfEquToken = i;
					break;
				}
			}
			
			ArrayList<String> leftSideTokens = new ArrayList<String>();
			for(int i = 0;i<indexOfEquToken;i++) {
				leftSideTokens.add(tokens.get(i));
			}
			ArrayList<String> rightSideTokens = new ArrayList<String>();
			for(int i = indexOfEquToken+1;i<tokens.size();i++) {
				rightSideTokens.add(tokens.get(i));
			}
			char symbol = tokens.get(indexOfEquToken).charAt(0);
			
			Expr leftSide = createExprFromTokens(leftSideTokens,defs,settings);
			Expr rightSide = createExprFromTokens(rightSideTokens,defs,settings);
			
			if(symbol == '=') return equ(leftSide,rightSide);
			if(symbol == '>') return equGreater(leftSide,rightSide);
			if(symbol == '<') return equLess(leftSide,rightSide);
			
		}else if(isOr){
			tokens.add("|");
			Or or = new Or();
			int indexOfLastOr = 0;
			
			for(int i = 0;i<tokens.size();i++) {
				String token = tokens.get(i);
				if(token.equals("|")) {
					
					ArrayList<String> tokenSet = new ArrayList<String>();
					for(int j = indexOfLastOr;j<i;j++)  tokenSet.add(tokens.get(j));
					or.add(createExprFromTokens(tokenSet,defs,settings));
					indexOfLastOr = i+1;
				}
			}
			
			return or;
			
		}else if(isAnd){
			tokens.add("&");
			And and = new And();
			int indexOfLastAnd = 0;
			
			for(int i = 0;i<tokens.size();i++) {
				String token = tokens.get(i);
				if(token.equals("&")) {
					
					ArrayList<String> tokenSet = new ArrayList<String>();
					for(int j = indexOfLastAnd;j<i;j++)  tokenSet.add(tokens.get(j));
					and.add(createExprFromTokens(tokenSet,defs,settings));
					indexOfLastAnd = i+1;
				}
			}
			
			return and;
			
		}else if(isNot){
			tokens.remove(0);
			return not(createExprFromTokens(tokens,defs,settings));
		}else if(isSum) {
			tokens.add("+");
			Expr sum = new Sum();
			int indexOfLastAdd = 0;
			boolean nextIsSub = false;
			for(int i = 0;i<tokens.size();i++) {
				String token = tokens.get(i);
				if(token.equals("+") || token.equals("-")) {
					if(i != 0) {
						if(tokens.get(i-1).equals("^") || tokens.get(i-1).equals("/")) continue;//avoids error created by e^-x+x where the negative is actually in the exponent same things with x+y/-2
						ArrayList<String> tokenSet = new ArrayList<String>();
						for(int j = indexOfLastAdd;j<i;j++) {
							tokenSet.add(tokens.get(j));
						}
						if(nextIsSub) {
							Expr toBeAdded = createExprFromTokens(tokenSet,defs,settings);
							if(toBeAdded instanceof Prod) {
								boolean foundNum = false;
								for(int k = 0;k<toBeAdded.size();k++) {
									if(toBeAdded.get(k) instanceof Num) {
										Num casted = (Num)toBeAdded.get(k);
										casted.realValue = casted.realValue.negate();
										foundNum = true;
										break;
									}
								}
								if(!foundNum) {
									toBeAdded.add(num(-1));
								}
								sum.add(toBeAdded);
							}else if(toBeAdded instanceof Num) {
								sum.add( ((Num)toBeAdded).negate() );
							}else sum.add(neg(toBeAdded));
						}
						else {
							sum.add(createExprFromTokens(tokenSet,defs,settings));
						}
					}
					indexOfLastAdd = i;
					nextIsSub = false;
					boolean goingThroughOperators = true;
					for (int j = i;goingThroughOperators&&j<tokens.size();j++) {
						if(tokens.get(j).equals("-")) {
							nextIsSub=!nextIsSub;
						}
						if(j+1<tokens.size()) {
							if(!(tokens.get(j+1).equals("-") || tokens.get(j+1).equals("+"))) {
								goingThroughOperators = false;
							}
						}
						tokens.remove(j);
						j--;
					}
				}
			}
			if(sum.size() == 1) return sum.get();
			return sum;
		}else if(isProd) {
			tokens.add("*");
			Expr numerProd = new Prod();
			Expr denomProd = new Prod();
			int indexOfLastProd = 0;
			boolean nextDiv = false;
			for(int i = 0;i<tokens.size();i++) {
				String token = tokens.get(i);
				if(token.equals("*") || token.equals("/")) {
					ArrayList<String> tokenSet = new ArrayList<String>();
					for(int j = indexOfLastProd;j<i;j++) {
						tokenSet.add(tokens.get(j));
					}
					if(nextDiv) denomProd.add(createExprFromTokens(tokenSet,defs,settings));
					else numerProd.add(createExprFromTokens(tokenSet,defs,settings));
					if(token.equals("/")) nextDiv = true;
					else nextDiv = false;
					
					indexOfLastProd = i+1;
				}
			}
			
			
			if(numerProd.size() == 1) numerProd = numerProd.get();
			if(denomProd.size() == 1) denomProd = denomProd.get();
			
			if(denomProd instanceof Prod && denomProd.size() == 0) {
				return numerProd;
			}
			return div(numerProd,denomProd);
		}else if(indexOfPowToken != -1) {
			ArrayList<String> baseTokens = new ArrayList<String>();
			for(int i = 0;i<indexOfPowToken;i++) baseTokens.add(tokens.get(i));
			Expr base = createExprFromTokens(baseTokens,defs,settings);
			
			ArrayList<String> expoTokens = new ArrayList<String>();
			for(int i = indexOfPowToken+1;i<tokens.size();i++) expoTokens.add(tokens.get(i));
			Expr expo = createExprFromTokens(expoTokens,defs,settings);
			
			return pow(base,expo);
		}else if(isFactorial) {
			Expr out = createExpr(tokens.get(0),defs,settings);
			for(int i = tokens.size()-1;i>=1;i--){
				if(tokens.get(i).equals("!")){
					out = gamma(sum(out,num(1)));
				}
			}
			return out;
		}
		throw new Exception("unrecognized format:"+tokens);
	}
	
	
	static char[] basicOperators = new char[] {
		'*','+','-','^','/',',','=','!',':',';','&','|','~','<','>'
	};
	private static boolean isBasicOperator(char c) {
		for(char o:basicOperators) {
			if(o == c) return true;
		}
		return false;
	}
	
	static ArrayList<String> generateTokens(String string) throws Exception{//splits up a string into its relevant subsections and removes parentheses	
		ArrayList<String> tokens = new ArrayList<String>();
		int count = 0;
		int lastIndex = 0;
		for(int i = 0;i < string.length();i++) {
			if(string.charAt(i) == '(' || string.charAt(i) == '[' || string.charAt(i) == '{') count++;
			else if(string.charAt(i) == ')' || string.charAt(i) == ']' || string.charAt(i) == '}') {
				count--;
				if(i != string.length()-1  && !containsOperators(Character.toString(  string.charAt(i+1) ))) {
					//throw new Exception("expected operator after ')'");
				}
			}
			
			if(count == 0) {
				if(isBasicOperator(string.charAt(i))) {
					String subString = string.substring(lastIndex, i);
					if(!subString.isEmpty())tokens.add(subString);
					tokens.add(String.valueOf(string.charAt(i)));
					lastIndex = i+1;
				}else if(string.charAt(i) == ')') {
					tokens.add(string.substring(lastIndex+1, i));
					lastIndex = i+1;
				}
			}else if(count == 1 && ((string.charAt(i) == '(') || string.charAt(i) == '{')) {//this is important for detecting sin(x) into [sin,x]
				String subString = string.substring(lastIndex, i);
				if(!subString.isEmpty())tokens.add(subString);
				lastIndex = i;
			}
		}
		if(count != 0) throw new Exception("missing ')'");
		if(lastIndex < string.length()+1) {
			String subString = string.substring(lastIndex, string.length());
			if(!subString.isEmpty())tokens.add(subString);
		}
		//strange case of 6x meaning 6*x
		
		
		
		
		return tokens;
	}
}
