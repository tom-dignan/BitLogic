package cas;

import java.io.Serializable;

public class ComplexFloat implements Serializable{
	private static final long serialVersionUID = 5035686221005313754L;
	
	public double real = 0.0,imag = 0.0;
	public static ComplexFloat ONE = new ComplexFloat(1,0);
	public static ComplexFloat ZERO = new ComplexFloat(0,0);
	public static ComplexFloat I = new ComplexFloat(0,1);
	
	public ComplexFloat(double real,double imag) {
		this.real = real;
		this.imag = imag;
	}
	public void set(ComplexFloat other) {
		this.real = other.real;
		this.imag = other.imag;
	}
	private static boolean closeToZero(double a){
		return Math.abs(a)<1.0E-12;
	}
	public boolean closeToZero(){
		return closeToZero(real) && closeToZero(imag);
	}
	@Override
	public String toString(){
		if(!closeToZero(real) && !closeToZero(imag)) {
			return Double.toString(real)+"+"+Double.toString(imag)+"*i";
		}else if(closeToZero(imag)) {
			return Double.toString(real);
		}else if(closeToZero(real)){
			return Double.toString(imag)+"*i";
		}
		return Double.toString(real);
	}
	
	public static ComplexFloat sin(ComplexFloat in) {
		return new ComplexFloat(Math.sin(in.real)*Math.cosh(in.imag),Math.cos(in.real)*Math.sinh(in.imag));
	}
	public static ComplexFloat cos(ComplexFloat in) {
		return new ComplexFloat(Math.cos(in.real)*Math.cosh(in.imag),-Math.sin(in.real)*Math.sinh(in.imag));
	}
	public static ComplexFloat tan(ComplexFloat in) {
		return div(sin(in),cos(in));
	}
	public static ComplexFloat add(ComplexFloat a,ComplexFloat b) {
		return new ComplexFloat(a.real+b.real,a.imag+b.imag);
	}
	public static ComplexFloat sub(ComplexFloat a, ComplexFloat b) {
		return new ComplexFloat(a.real-b.real,a.imag-b.imag);
	}
	public static ComplexFloat mult(ComplexFloat a,ComplexFloat b) {
		return new ComplexFloat(a.real*b.real-a.imag*b.imag,a.real*b.imag+a.imag*b.real);
	}
	public static ComplexFloat div(ComplexFloat a,ComplexFloat b) {
		double divisor = b.real*b.real+b.imag*b.imag;
		return new ComplexFloat( (a.imag*b.imag+b.real*a.real)/divisor ,(a.imag*b.real-b.imag*a.real)/divisor );
	}
	public static ComplexFloat ln(ComplexFloat in) {
		return new ComplexFloat(0.5*Math.log(in.real*in.real+in.imag*in.imag),Math.atan2(in.imag,in.real));
	}
	public static ComplexFloat exp(ComplexFloat in) {
		double scaler = Math.exp(in.real);
		return new ComplexFloat(Math.cos(in.imag)*scaler,Math.sin(in.imag)*scaler);
	}
	public static ComplexFloat pow(ComplexFloat base,ComplexFloat expo) {
		if(base.real == 0.0 && base.imag == 0.0) return ZERO;
		if(base.imag == 0.0 && expo.imag == 0.0 && base.real < 0 && 1.0/expo.real==Math.round(1.0/expo.real) && Math.round(1.0/expo.real)%2 == 1 ){
			System.out.println("ok");
			return new ComplexFloat(-Math.pow(-base.real, expo.real),0);
		}
		return exp(mult(ln(base),expo));
	}
	public static ComplexFloat atan(ComplexFloat in) {
		ComplexFloat iz = mult(I,in);
		return mult(new ComplexFloat(0,0.5),sub(ln(sub(ONE,iz)),ln(add(new ComplexFloat(1,0),iz)) ));
	}
	public static ComplexFloat asin(ComplexFloat in) {
		ComplexFloat oneMinusXSquared = sub(ONE,pow(in,new ComplexFloat(2,0)));
		
		return div(ln(add(mult(I,in), pow(oneMinusXSquared,new ComplexFloat(0.5,0)) )),I);
		
	}
	public static ComplexFloat acos(ComplexFloat in) {
		ComplexFloat oneMinusXSquared = sub(ONE,pow(in,new ComplexFloat(2,0)));
		
		return div(ln(add(in, mult(I,pow(oneMinusXSquared,new ComplexFloat(0.5,0)) ))),I);
		
	}
}
