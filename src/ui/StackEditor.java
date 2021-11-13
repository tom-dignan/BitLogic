package ui;

import javax.swing.*;

import cas.*;

public class StackEditor extends cas.QuickMath {

	private static Expr expr = null;

	void result() {
		if (last() == null)
			return;
		expr = last();
		Thread compute = new Thread() {
			@Override
			public void run() {
				long oldTime = System.nanoTime();
				expr = expr.replace(currentDefs.getVars()).simplify(currentSettings);// Substitutes the variables and}
				
				long delta = System.nanoTime() - oldTime;
				System.out.println("took " + delta / 1000000.0 + " ms to compute");
			}
		};
		compute.start();
		Thread stuckCheck = new Thread() {
			@Override
			public void run() {
				try {
					while (compute.isAlive()) {
						sleep(1000);
						if (compute.isAlive())
							System.out.println("thinking...");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		stuckCheck.start();
		try {
			compute.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		stack.set(size() - 1, expr);
	}

	Settings currentSettings = new Settings();
	public Defs currentDefs = new Defs();

	public DefaultListModel<Expr> stack = new DefaultListModel<Expr>();
	DefaultListModel<Expr> stackOld = new DefaultListModel<Expr>();

	public int size() {
		return stack.size();
	}

	public Expr last() {
		if (size() < 1) {
			UI.createMessege("the stack is empty");
			return null;
		}
		return stack.lastElement();
	}

	public Expr sLast() {
		if (size() < 2) {
			UI.createMessege("need more elements");
			return null;
		}
		return stack.get(size() - 2);
	}

	public void printStack() {
		System.out.println("**********************");
		for (int i = 0; i < size(); i++) {
			System.out.print((i + 1) + ": ");
			System.out.println(stack.get(i));
		}
		System.out.println("**********************");
		//if(stack.size()>0) last().printTree(0);
	}

	void clear() {
		stack.clear();
	}

	void negate() {
		if (last() == null)
			return;
		stack.set(size() - 1, neg(last()));
	}

	void add() {
		if (sLast() == null)
			return;
		if (sLast() instanceof Sum) {
			sLast().add(last());
			stack.remove(size() - 1);
		} else {
			Expr sum = sum(sLast(), last());
			stack.remove(size() - 1);
			stack.set(size() - 1, sum);
		}
	}

	void subtract() {
		if (sLast() == null)
			return;
		if (sLast() instanceof Sum) {
			sLast().add(neg(last()));
			stack.remove(size() - 1);
		} else {
			Expr sub = sub(sLast(), last());
			stack.remove(size() - 1);
			stack.set(size() - 1, sub);
		}
	}

	void multiply() {
		if (sLast() == null)
			return;
		if (sLast() instanceof Prod) {
			sLast().add(last());
			stack.remove(size() - 1);
		} else {
			Expr prod = prod(sLast(), last());
			stack.remove(size() - 1);
			stack.set(size() - 1, prod);
		}
	}

	void exponent() {
		if (sLast() == null)
			return;
		Expr pow = pow(sLast(), last());
		stack.remove(size() - 1);
		stack.set(size() - 1, pow);
	}

	void pop() {
		if (last() == null)
			return;
		stack.remove(size() - 1);
	}

	void ln() {
		if (last() == null)
			return;
		stack.set(size() - 1, ln(last()));
	}

	void log() {
		if (sLast() == null)
			return;
		stack.set(size() - 1, div(ln(sLast()), ln(last())));
	}

	void factor() {
		if (last() == null)
			return;
		stack.set(size() - 1, factor(last()));
	}

	void distr() {
		if (last() == null)
			return;
		stack.set(size() - 1, distr(last()));
	}

	void sqrt() {
		if (last() == null)
			return;
		stack.set(size() - 1, sqrt(last()));
	}

	void similar() {
		if (sLast() == null)
			return;
		stack.addElement(bool(sLast().strictSimilarStruct(last())));
	}

	void divide() {
		if (sLast() == null)
			return;

		stack.set(size() - 2, div(sLast(), stack.get(size() - 1)));
		stack.remove(size() - 1);
	}

	void inverse() {
		if (last() == null)
			return;
		stack.set(size() - 1, inv(last()));
	}

	void swap() {
		if (sLast() == null)
			return;
		Expr temp = last();
		stack.set(size() - 1, sLast());
		stack.set(size() - 2, temp);
	}

	void diff() {
		if (sLast() == null)
			return;
		if (!(last() instanceof Var)) {
			UI.createMessege("the second element needs to be a variable");
			return;
		}
		stack.set(size() - 2, diff(sLast(), (Var) last()));
		stack.remove(size() - 1);
	}

	void integrate() {
		if (sLast() == null)
			return;
		if (!(last() instanceof Var)) {
			UI.createMessege("the second element needs to be a variable");
			return;
		}
		stack.set(size() - 2, integrate(sLast(), (Var) last()));
		stack.remove(size() - 1);
	}

	void integrateOver() {
		if (stack.size() < 4) {
			UI.createMessege("need more elements");
			return;
		}
		if (!(last() instanceof Var)) {
			UI.createMessege("the last element needs to be a variable");
			return;
		}
		stack.set(size() - 4, integrateOver(stack.get(size() - 4), stack.get(size() - 3), sLast(), (Var) last()));
		stack.removeRange(size() - 3, size() - 1);
	}

	void solve() {
		if (sLast() == null)
			return;
		if (!(last() instanceof Var)) {
			UI.createMessege("the second element needs to be a variable");
			return;
		} else if (!(sLast() instanceof Equ)) {
			UI.createMessege("the first element needs to be an equation");
			return;
		}
		stack.set(size() - 2, solve((Equ) sLast(), (Var) last()));
		stack.remove(size() - 1);
	}

	void equ() {
		if (sLast() == null)
			return;
		stack.set(size() - 2, equ(sLast(), last()));
		stack.remove(size() - 1);
	}

	void createList() {
		stack.addElement(new ExprList());
	}

	void addToList() {
		if (sLast() == null)
			return;
		sLast().add(last());
		stack.remove(size() - 1);
	}

	void breakApart() {
		if (last() == null)
			return;
		Expr temp = last();
		stack.remove(size() - 1);
		for (int i = 0; i < temp.size(); i++)
			stack.addElement(temp.get(i));
	}

	void duplicate() {
		if (last() == null)
			return;
		stack.addElement(last().copy());
	}

	void roll() {
		if (last() == null)
			return;
		Expr temp = last();
		for (int i = size() - 1; i > 0; i--) {
			stack.set(i, stack.get(i - 1));
		}
		stack.set(0, temp);
	}

	void sin() {
		if (last() == null)
			return;
		stack.set(size() - 1, sin(last()));
	}

	void cos() {
		if (last() == null)
			return;
		stack.set(size() - 1, cos(last()));
	}

	void tan() {
		if (last() == null)
			return;
		stack.set(size() - 1, tan(last()));
	}

	void sinh() {
		if (last() == null)
			return;
		stack.set(size() - 1, sinh(last()));
	}

	void cosh() {
		if (last() == null)
			return;
		stack.set(size() - 1, cosh(last()));
	}

	void tanh() {
		if (last() == null)
			return;
		stack.set(size() - 1, tanh(last()));
	}

	void atan() {
		if (last() == null)
			return;
		stack.set(size() - 1, atan(last()));
	}

	void approx() {
		if (sLast() == null)
			return;
		stack.set(size() - 2, approx(sLast(), (ExprList) last()));
		stack.remove(stack.size() - 1);
	}

	public void command(String command) {

		if (command.isEmpty()) {
			UI.createMessege("you typed nothing");
			return;
		}
		try {
			if (!command.equals("undo")) {
				stackOld.clear();
				for (int i = 0; i < size(); i++) {
					stackOld.addElement(stack.get(i).copy());
				}
			}
			if (command.equals("+")) {
				add();
			} else if (command.equals("--")) {
				negate();
			} else if (command.equals("-")) {
				subtract();
			} else if (command.equals("*")) {
				multiply();
			} else if (command.equals("^")) {
				exponent();
			} else if (command.equals("r") || (command.equals("result"))) {// get result
				result();
			} else if (command.equals("p") || command.equals("pop")) {// pop element
				pop();
			} else if (command.equals("ln") || command.equals("log")) {
				log();
			} else if (command.equals("sqrt")) {
				sqrt();
			} else if (command.equals("similar")) {
				similar();
			} else if (command.equals("swap")) {
				swap();
			} else if (command.equals("inv")) {
				inverse();
			} else if (command.equals("/")) {
				divide();
			} else if (command.equals("diff")) {
				diff();
			} else if (command.equals("integrate")) {
				integrate();
			} else if (command.equals("perfect-power")) {
				stack.addElement(perfectPower((Num) last()));
			} else if (command.equals("prime-factor")) {
				stack.addElement(primeFactor((Num) last()));
			} else if (command.equals("distr")) {
				distr();
			} else if (command.equals("solve")) {
				solve();
			} else if (command.equals("=")) {
				equ();
			} else if (command.equals("[")) {
				createList();
			} else if (command.equals("]")) {
				addToList();
			} else if (command.equals("factor")) {
				factor();
			} else if (command.equals("break")) {
				breakApart();
			} else if (command.equals("save")) {
				Expr.saveExpr(sLast(), last().toString());
				stack.remove(size() - 1);
			} else if (command.equals("open")) {
				stack.set(size() - 1, Expr.openExpr(((Var) last()).name));
			} else if (command.equals("clear")) {
				clear();
			} else if (command.equals("sort")) {
				last().sort();
				stack.set(size() - 1, last());// update visuals
			} else if (command.equals("dup")) {
				duplicate();
			} else if (command.equals("undo")) {
				stack.clear();
				for (int i = 0; i < stackOld.size(); i++) {
					stack.addElement(stackOld.get(i));
				}
			} else if (command.equals("roll")) {
				roll();
			} else if (command.equals("sin")) {
				sin();
			} else if (command.equals("cos")) {
				cos();
			} else if (command.equals("tan")) {
				tan();
			} else if (command.equals("sinh")) {
				sinh();
			} else if (command.equals("cosh")) {
				cosh();
			} else if (command.equals("tanh")) {
				tanh();
			} else if (command.equals("atan")) {
				atan();
			} else if (command.equals("quit") || command.equals("exit") || command.equals("close")) {
				System.exit(0);
			} else if (command.equals("plot")) {
				new graphics.Plot(this);
			} else if (command.equals("approx")) {
				approx();
			} else if (command.equals("integrateOver")) {
				integrateOver();
			} else if (command.equals("poly")) {
				Var v = var("x");
				partialFrac(last(), v, currentSettings);
			} else if (command.equals("hash")) {
				System.out.println(last().generateHash());
			} else if (command.equals("equal-struct")) {
				stack.addElement(new BoolState(last().equalStruct(sLast())));
			} else if (command.equals("define")) {
				currentDefs.addVar(((Var) sLast()).name, last());
				stack.removeRange(size() - 2, size() - 1);
			} else if (command.equals("defineFunc")) {
				String name = ((Var) stack.get(size() - 3)).name;
				currentDefs.addFunc(name, func(name, (ExprList) sLast(), last()));
				stack.removeRange(size() - 3, size() - 1);
			} else if (command.contains(":")) {
				String[] parts = command.split(":");
				command = parts[0];

				if (command.equals("dup")) {
					int index = Integer.parseInt(parts[1]) - 1;
					if (index > -1 && index < size()) {
						stack.addElement(stack.get(index).copy());
					} else
						UI.createMessege("invalid index");
				} else if (command.equals("remove")) {
					int index = Integer.parseInt(parts[1]) - 1;
					if (index > -1 && index < size()) {
						stack.remove(index);
					} else
						UI.createMessege("invalid index");
				} else if (command.equals("swap")) {
					int index = Integer.parseInt(parts[1]) - 1;
					if (index > -1 && index < size()) {
						Expr temp = stack.get(index);
						stack.set(index, last());
						stack.set(size() - 1, temp);
					} else
						UI.createMessege("invalid index");
				}
			} else {
				long oldTime = System.nanoTime();
				Expr convertedQ = Ask.ask(command, currentDefs, currentSettings);

				long delta = System.nanoTime() - oldTime;
				System.out.println("took " + delta / 1000000.0 + " ms to understand");

				System.out.println("meaning: " + convertedQ);
				if (convertedQ != null) {
					stack.addElement(convertedQ);
				}

			}
		} catch (Exception e) {
			UI.createMessege("An error has occured\nreason: " + e.getMessage());
			e.printStackTrace();
		}

	}
}
