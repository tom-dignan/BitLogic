package cas;

import java.math.BigInteger;

public class Sin extends Expr{
	
	private static final long serialVersionUID = -5759564792496416862L;
	
	static Rule sinOfArctan = new Rule("sin(atan(x))=x/sqrt(1+x^2)","sin of arctan",Rule.UNCOMMON);
	static Rule sinOfAsin = new Rule("sin(asin(x))=x","sin contains inverse",Rule.EASY);
	static Rule sinOfAcos = new Rule("sin(acos(x))=sqrt(1-x^2)","sin of arccos",Rule.UNCOMMON);

	Sin(){}//
	public Sin(Expr a) {
		add(a);
	}
	
	static Rule unitCircle = new Rule("unit circle for sine",Rule.TRICKY){
		private static final long serialVersionUID = 1L;
		
		@Override
		public Expr applyRuleToExpr(Expr e,Settings settings){
			Sin sin = (Sin)e;
			
			Var pi = pi();
			BigInteger three = BigInteger.valueOf(3),six = BigInteger.valueOf(6),four = BigInteger.valueOf(4);
			
			Expr innerExpr = sin.get();
			if(innerExpr.equals(num(0))) {
				return num(0);
			}else if(innerExpr.equals(pi))
				return num(0);
			if(innerExpr instanceof Div && innerExpr.contains(pi())){
				Div frac = ((Div)innerExpr).ratioOfUnitCircle();
				
				if(frac != null) {
					BigInteger numer = ((Num)frac.getNumer()).realValue,denom = ((Num)frac.getDenom()).realValue;//getting numerator and denominator
					
					numer = numer.mod(denom.multiply(BigInteger.TWO));//restrict to the main circle
					int negate = 1;
					
					if(numer.compareTo(denom) == 1) {//if the numerator is greater than the denominator
						negate = -negate;
						numer = numer.mod(denom);//if we go past the top part of the circle we can flip it back to the top and keep track of the negative
					}
					
					if(numer.compareTo(denom.divide(BigInteger.TWO)) == 1) {//if we are past the first part of the quarter circle, we can restrict it further
						numer = denom.subtract(numer);//basically reflecting across the y axis
					}
					
					if(numer.equals(BigInteger.ONE) && denom.equals(BigInteger.TWO)) return num(negate);
					else if(numer.equals(BigInteger.ONE) && denom.equals(three)) return div(sqrt(num(3)),num(2*negate));
					else if(numer.equals(BigInteger.ONE) && denom.equals(six)) return inv(num(2*negate));
					else if(numer.equals(BigInteger.ONE) && denom.equals(four)) return div(sqrt(num(2)),num(2*negate));
					else if(numer.equals(BigInteger.ZERO)) return num(0);
					else {
						if(negate == -1) {
							return neg(sin(div(prod(pi(),num(numer)),num(denom)).simplify(Settings.normal)));
						}
						return sin(div(prod(pi(),num(numer)),num(denom)).simplify(Settings.normal));
					}
					
					
				}
				
			}else if(innerExpr instanceof Sum) {//sin(x-pi/4) can be turned into sin(x+7*pi/4) because sin has symmetry
				for(int i = 0;i<innerExpr.size();i++) {
					if(innerExpr.get(i) instanceof Div && !innerExpr.get(i).containsVars() && innerExpr.get(i).contains(pi)) {
						
						Div frac = ((Div)innerExpr.get(i)).ratioOfUnitCircle();
						
						if(frac!=null) {
							System.out.println(frac);
							BigInteger numer = ((Num)frac.getNumer()).realValue,denom = ((Num)frac.getDenom()).realValue;
							
							if(denom.signum() == -1){
								denom = denom.negate();
								numer = numer.negate();
							}
							
							numer = numer.mod(denom.multiply(BigInteger.TWO));//to do this we take the mod
							
							if(numer.equals(BigInteger.ONE) && denom.equals(BigInteger.TWO)) {//sin(x+pi/2) = cos(x)
								innerExpr.remove(i);
								return cos(innerExpr).simplify(Settings.normal);
							}else if(numer.equals(three) && denom.equals(BigInteger.TWO)) {
								innerExpr.remove(i);
								return neg(cos(innerExpr.simplify(Settings.normal)));
							}
							
							innerExpr.set(i,  div(prod(num(numer),pi()),num(denom)) );
							sin.set(0, innerExpr.simplify(Settings.normal));
							
						}
						
					}
				}
			}
			return sin;
			
		}
		
	};

	@Override
	public ComplexFloat convertToFloat(ExprList varDefs) {
		return ComplexFloat.sin(get().convertToFloat(varDefs));
	}
	
	static ExprList ruleSequence = null;
	
	public static void loadRules(){
		ruleSequence = exprList(
				sinOfArctan,
				sinOfAsin,
				sinOfAcos,
				StandardRules.oddFunction,
				StandardRules.distrInner,
				unitCircle
		);
	}
	
	@Override
	ExprList getRuleSequence() {
		return ruleSequence;
	}

}
