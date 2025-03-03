package cas;

import java.math.BigInteger;
import java.util.ArrayList;

public class Factor extends Expr{
	
	private static final long serialVersionUID = -5448276275686292911L;
	
	static Rule sumOfCubes = new Rule("factor(a^3+b^3)=(a+b)*(a^2-a*b+b^2)","sum of cubes",Rule.UNCOMMON);
	static Rule differenceOfCubes = new Rule("factor(a^3-b^3)=(a-b)*(a^2+a*b+b^2)","difference of cubes",Rule.UNCOMMON);

	Factor(){}//
	public Factor(Expr expr) {
		add(expr);
	}
	
	static Rule reversePascalsTriangle = new Rule("reverse pascals triangle",Rule.DIFFICULT){
		private static final long serialVersionUID = 1L;

		@Override
		public Expr applyRuleToExpr(Expr e,Settings settings) {
			Factor factor = (Factor)e;
			Expr expr = factor.get();
			
			Var v = mostCommonVar(expr);
			if(expr instanceof Sum && v != null && isPolynomial(expr,v )) {
				
				ExprList coefs = polyExtract(expr,v,settings);
				if(coefs == null) return e;
				Num degree = num(coefs.size()-1);
				if(degree.realValue.compareTo(BigInteger.TWO) == -1) return e;
				
				Expr highestDegreeCoef = coefs.get(coefs.size()-1);
				Expr lowestDegreeCoef = coefs.get(0);
				
				Expr m = pow(highestDegreeCoef ,inv(degree)).simplify(settings);
				Expr b = pow(lowestDegreeCoef ,inv(degree)).simplify(settings);
				
				if(multinomial(sum(prod(m,v),b),degree,settings).equals(expr) ) {
					Expr result = pow(sum(prod(m,v),b),degree).simplify(settings);
					return result;
				}else if(multinomial(sum(prod(m,v),neg(b)),degree,settings).equals(expr)) {//try the negative variant
					Expr result = pow(sub(prod(m,v),b),degree).simplify(settings);
					return result;
				}
				
			}
			return e;
		}
	};
	
	static Rule power2Reduction = new Rule("power of 2 polynomial",Rule.UNCOMMON){
		private static final long serialVersionUID = 1L;

		@Override
		public Expr applyRuleToExpr(Expr e,Settings settings) {
			Factor factor = (Factor)e;
			Expr expr = factor.get();
			Var v = mostCommonVar(expr);
			if(expr instanceof Sum && v!=null && expr.size() == 2 && isPolynomial(expr,v)) {
				Power pow = null;
				Expr other = null;
				if(expr.get(0) instanceof Power) {
					pow = (Power)expr.get(0);
					other = expr.get(1);
				}else if(expr.get(1) instanceof Power) {
					pow = (Power)expr.get(1);
					other = expr.get(0);
				}
				
				
				if(pow != null && other != null && other.negative() && isPositiveRealNum(pow.getExpo()) && ((Num)pow.getExpo()).realValue.mod(BigInteger.TWO).equals(BigInteger.ZERO) ) {
					Expr newPow = sqrt(pow).simplify(settings);
					Expr newOther = sqrt(neg(other)).simplify(settings);
					
					if(!(newOther instanceof Power || newOther instanceof Prod)) {
						
						return prod(sum(newPow,newOther),sum(newPow,neg(newOther))).simplify(settings);
						
					}
					
				}
			}
			return e;
		}
	};
	
	static Rule quadraticFactor = new Rule("factor quadratics",Rule.TRICKY){
		private static final long serialVersionUID = 1L;

		@Override
		public Expr applyRuleToExpr(Expr e,Settings settings) {
			Factor factor = (Factor)e;
			Expr expr = factor.get();
			
			
			if(expr instanceof Sum) {
				ExprList coefs = null;
				Var x = mostCommonVar(expr);
				if(x!=null) {
					coefs = polyExtract(expr, x,settings);
				}
				
				if(coefs != null) {
					if(coefs.size() == 3) {//quadratic
						//check discriminant
						for(int i = 0;i<coefs.size();i++) {
							if(!(coefs.get(i) instanceof Num)) {
								return e;
							}
						}
						Num a =  (Num)coefs.get(2),b = (Num)coefs.get(1),c = (Num)coefs.get(0);
						
						if(a.isComplex() || b.isComplex() || c.isComplex()) return e;
						
						BigInteger discrNum = b.realValue.pow(2).subtract(BigInteger.valueOf(4).multiply(a.realValue).multiply(c.realValue));
					
						if(discrNum.signum() == -1) return e;
						
						BigInteger discrNumSqrt = discrNum.sqrt();
						
						if(discrNum.signum() != -1 && discrNumSqrt.pow(2).equals(discrNum)) {
						
							Expr discr = num(discrNumSqrt);
							
							
							Expr out = new Prod();
							
							Prod twoAX = prod(num(2),a,x);
							out.add( sum(twoAX,b.copy(),prod(num(-1),discr)) );
							out.add( sum(twoAX.copy(),b.copy(),discr.copy()) );
							out.add(inv(num(4)));
							out.add(inv(a.copy()));
							
							return out.simplify(settings);
						}
							
						
					}
				}
			}
			
			return e;
		}
	};
	
	static Rule generalFactor = new Rule("general factor",Rule.TRICKY){
		private static final long serialVersionUID = 1L;
		
		private Num getNumerOfPower(Power pow) {
			if(pow.getExpo() instanceof Num) {
				return (Num)pow.getExpo();
			}else if(pow.getExpo() instanceof Div && ((Div)pow.getExpo()).isNumericalAndReal() ) {
				return (Num)((Div)pow.getExpo()).getNumer();
			}else {
				return num(1);
			}
		}
		
		private Num getDenomOfPower(Power pow) {
			if(pow.getExpo() instanceof Div && ((Div)pow.getExpo()).isNumericalAndReal() ) {
				return (Num)((Div)pow.getExpo()).getDenom();
			}
			return num(1);
		}

		@Override
		public Expr applyRuleToExpr(Expr e,Settings settings) {
			Factor factor = (Factor)e;
			Expr expr = factor.get();
		
			if(expr instanceof Sum) {
				boolean sumHasDiv = false;
				for(int i = 0;i<expr.size();i++) {
					if(expr.get(i) instanceof Div) {
						sumHasDiv = true;
						break;
					}
				}
				if(sumHasDiv){//combine fractions
					Div sum = div(num(0),num(1));
					for(int i = 0;i<expr.size();i++) {
						Div current = Div.cast(expr.get(i));
						sum = Div.addFracs(sum, current);
					}
					return sum.simplify(settings);
				}
				Prod leadingTerm = Prod.cast(expr.get(0));
				Num leadingTermCoef = (Num)leadingTerm.getCoefficient();
				BigInteger gcd = leadingTermCoef.gcd();
				Expr factors = new Prod();
				//calculate gcd
				for(int i = 1;i<expr.size();i++) {
					gcd = ((Num)expr.get(i).getCoefficient()).gcd().gcd(gcd);
				}
				if(expr.negative()) gcd = gcd.negate();
				//add to factors product
				if(!gcd.equals(BigInteger.ONE)) factors.add(num(gcd));
				//common term
				for(int i = 0;i<leadingTerm.size();i++) {
					Expr subTerm = leadingTerm.get(i);
					if(subTerm instanceof Num) continue;
					
					Power termPower = Power.cast(subTerm);
					
					if(!Div.cast(termPower.getExpo()).isNumericalAndReal()) {
						termPower = pow(termPower,num(1));
					}
					
					Num minExpoNum = getNumerOfPower(termPower);
					Num minExpoDen = getDenomOfPower(termPower);
					
					
					for(int j = 1;j<expr.size();j++) {
						Prod otherTerm = Prod.cast(expr.get(j));
						
						boolean found = false;
						
						for(int k = 0;k<otherTerm.size();k++) {
							Expr otherSubTerm = otherTerm.get(k);
							if(otherSubTerm instanceof Num) continue;
							
							Power otherTermPower = Power.cast(otherSubTerm);
							
							if(!Div.cast(otherTermPower.getExpo()).isNumericalAndReal()) {
								otherTermPower = pow(otherTermPower,num(1));
							}
							
							if(otherTermPower.getBase().equals(termPower.getBase())) {
								found = true;
								
								Num expoNum = getNumerOfPower(otherTermPower);
								Num expoDen = getDenomOfPower(otherTermPower);
								
								BigInteger a = minExpoNum.realValue.multiply(expoDen.realValue);
								BigInteger b = expoNum.realValue.multiply(minExpoDen.realValue);
								
								if(b.compareTo(a) == -1) {
									minExpoNum = expoNum;
									minExpoDen = expoDen;
								}
								
								break;
							}
							
						}
						if(!found) {
							minExpoNum = Num.ZERO;
							break;
						}
					}
					if(!minExpoNum.equals(Num.ZERO)) {
						factors.add( Power.unCast( pow(termPower.getBase(), Div.unCast(div(minExpoNum,minExpoDen)))));
					}
				}
				
				//
				if(factors.size()>0) {
					//divide terms by factors
					for(int i = 0;i<expr.size();i++) {
						expr.set(i, div(expr.get(i),factors).simplify(settings));
					}
					//
					expr = Prod.combine(factors, factor(expr).simplify(settings));
					
					return expr;
				}
			}
			
			
			return e;
		}
	};
	
	static Rule reExpandSubSums = new Rule("re distribute sums",Rule.EASY){
		private static final long serialVersionUID = 1L;
		
		@Override
		public Expr applyRuleToExpr(Expr e,Settings settings) {
			Factor factor = (Factor)e;
			Expr expr = factor.get();
			
			if(expr instanceof Prod) {

				for(int i = 0;i<expr.size();i++) {
					if(expr.get(i) instanceof Sum) {
						Expr subSum = expr.get(i);
						subSum.flags.simple = false;
						expr.set(i, subSum.simplify(settings));
					}
				}
			}
			
			return e;
		}
	};
	
	static Rule pullOutRoots = new Rule("pull out roots of polynomial",Rule.VERY_DIFFICULT){
		private static final long serialVersionUID = 1L;
		
		@Override
		public Expr applyRuleToExpr(Expr e,Settings settings) {
			Factor factor = (Factor)e;
			Expr expr = factor.get();
			
			Var v = mostCommonVar(expr);
			
			if(expr instanceof Sum && v != null && isPolynomial(expr,v) ) {
				int degree = degree(expr,v).intValue();
				if(degree < 2) return e;
				ExprList poly = polyExtract(expr,v,settings);
				if(poly == null) return e;
				for(int i = 0;i<poly.size();i++) if(!(poly.get(i) instanceof Num)) return e;//must be all nums
				ArrayList<Double> rootsAsFloat = Solve.polySolve(poly);
				Prod out = new Prod();
				//System.out.println(expr);
				//System.out.println(rootsAsFloat);
				for(double root:rootsAsFloat) {
					ExprList rootAsPoly = new ExprList();//the polynomial to be divided
					long[] frac =  toFraction(root);
					rootAsPoly.add(num(-frac[0]));
					
					ExprList[] divided = null;
					
					//System.out.println("r "+root+" "+degree);
					for(int i = 1;i<Math.min(8, degree);i++) {//this checks other factors like x^3-7, still integer root but a quadratic 
						//System.out.println(frac[0]+"/"+frac[1]);
						rootAsPoly.add(num(frac[1]));
						divided = polyDiv(poly, rootAsPoly, settings);//try polynomial division
						if(divided[1].size() == 0) break;//success
						rootAsPoly.set(i, num(0));//shifting to next degree
						double newApprox = Math.pow(root,i+1);
						//System.out.println(newApprox);
						frac =  toFraction(newApprox);//shift to  next degree
						rootAsPoly.set(0, num(-frac[0]));//shift to  next degree
					}
					
					
					if(divided != null && divided[1].size() == 0) {
						out.add( exprListToPoly(rootAsPoly, v, settings) );
						poly = divided[0];
						degree = poly.size()-1;
					}
					
				}
				out.add( exprListToPoly(poly, v, settings) );
				if(out.size() > 1) {
					return out.simplify(settings);
				}
				
			}
			return e;
			
		}
	};
	
	static ExprList ruleSequence = null;
	public static void loadRules(){
		ruleSequence = exprList(
				sumOfCubes,
				differenceOfCubes,
				power2Reduction,
				quadraticFactor,
				pullOutRoots,
				reversePascalsTriangle,
				generalFactor,
				reExpandSubSums,
				StandardRules.becomeInner
					
		);
	}
	@Override
	ExprList getRuleSequence() {
		return ruleSequence;
	}
	
	@Override
	public ComplexFloat convertToFloat(ExprList varDefs) {
		return get().convertToFloat(varDefs);
	}

}
